/*
 * Copyright (C) 2023 Hedera Hashgraph, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.swirlds.common.threading;

import static com.swirlds.common.test.fixtures.AssertionUtils.assertEventuallyTrue;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.swirlds.common.threading.locks.locked.MaybeLocked;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SyncPermitProviderTest {
    @Test
    @DisplayName("Permits are acquired and released properly")
    void testPermitRelease() {
        final int numPermits = 3;
        final SyncPermitProvider syncPermitProvider = new SyncPermitProvider(numPermits);

        assertEquals(numPermits, syncPermitProvider.getNumAvailable(), "all permits should be available");

        try (final MaybeLocked maybeLocked = syncPermitProvider.tryAcquire()) {
            assertTrue(maybeLocked.isLockAcquired(), "first acquire should succeed");
            assertEquals(
                    numPermits - 1,
                    syncPermitProvider.getNumAvailable(),
                    "one less permit should be available when a permit is acquired");
        }

        assertEquals(
                numPermits,
                syncPermitProvider.getNumAvailable(),
                "all permits should be available after the acquired permit is released");
    }

    @Test
    @DisplayName("Once all permits are acquired, further attempts to acquire fail")
    void testAllPermitsAcquired() {
        final int numPermits = 9;
        final SyncPermitProvider syncPermitProvider = new SyncPermitProvider(numPermits);

        assertEquals(numPermits, syncPermitProvider.getNumAvailable(), "all permits should be available");

        final List<MaybeLocked> permits = new ArrayList<>(numPermits);

        // Acquire all the permits
        for (int i = 0; i < numPermits; i++) {
            final MaybeLocked maybeLocked = syncPermitProvider.tryAcquire();
            permits.add(maybeLocked);
            assertTrue(maybeLocked.isLockAcquired(), "first acquire should succeed");
            assertEquals(
                    numPermits - i - 1,
                    syncPermitProvider.getNumAvailable(),
                    "one less permit should be available when a permit is acquired");
        }

        // Attempts to acquire more permits should fail
        final MaybeLocked shouldNotAcquire = syncPermitProvider.tryAcquire();
        assertFalse(shouldNotAcquire.isLockAcquired(), "no further permits should be able to be acquired");

        // Releasing permits should result in more permits being available
        for (int i = 0; i < numPermits; i++) {
            final MaybeLocked maybeLocked = permits.get(i);
            maybeLocked.close();
            assertEquals(
                    i + 1,
                    syncPermitProvider.getNumAvailable(),
                    "one more permit should be available when a permit is released");
        }
    }

    @Test
    @DisplayName("waitForAllSyncsToFinish blocks until all permits are released")
    void testWaitForAllSyncsToFinish() {
        final int numPermits = 3;
        final SyncPermitProvider syncPermitProvider = new SyncPermitProvider(numPermits);

        final List<MaybeLocked> permits = new ArrayList<>(numPermits);
        // Acquire all the permits
        for (int i = 0; i < numPermits; i++) {
            final MaybeLocked maybeLocked = syncPermitProvider.tryAcquire();
            permits.add(maybeLocked);
            assertTrue(maybeLocked.isLockAcquired());
        }

        // Attempts to acquire more permits should fail
        final MaybeLocked shouldNotAcquire = syncPermitProvider.tryAcquire();
        assertFalse(shouldNotAcquire.isLockAcquired(), "no further permits should be able to be acquired");

        final AtomicBoolean waitComplete = new AtomicBoolean(false);

        // Have a separate thread wait for syncs to finish
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        final Future<Void> future = executorService.submit(() -> {
            syncPermitProvider.waitForAllSyncsToFinish();
            waitComplete.set(true);
            return null;
        });

        try {
            // wait a bit, to give waitForAllSyncsToFinish time to potentially misbehave
            MILLISECONDS.sleep(50);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertFalse(waitComplete.get(), "waitForAllSyncsToFinish should not return until all permits are released");

        // close the permits that have already been acquired, so waitForAllSyncsToFinish will return
        permits.forEach(MaybeLocked::close);

        assertEventuallyTrue(
                waitComplete::get,
                Duration.ofMillis(1000),
                "waitForAllSyncsToFinish should return after all permits are released");
    }
}
