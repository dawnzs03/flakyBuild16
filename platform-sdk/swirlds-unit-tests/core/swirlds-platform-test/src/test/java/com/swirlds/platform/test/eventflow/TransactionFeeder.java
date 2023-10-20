/*
 * Copyright (C) 2022-2023 Hedera Hashgraph, LLC
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

package com.swirlds.platform.test.eventflow;

import static com.swirlds.common.threading.manager.AdHocThreadManager.getStaticThreadManager;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.swirlds.common.system.NodeId;
import com.swirlds.common.system.transaction.internal.ConsensusTransactionImpl;
import com.swirlds.common.test.fixtures.TransactionUtils;
import com.swirlds.common.threading.framework.Stoppable;
import com.swirlds.common.threading.framework.StoppableThread;
import com.swirlds.common.threading.framework.config.StoppableThreadConfiguration;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Feeds transactions to a consumer in a worker thread.
 */
public class TransactionFeeder {
    /** The average number of transactions per event. */
    private static final double TX_PER_EVENT_AVG = 3;

    /** The standard deviation for the number of transactions per event. */
    private static final double TX_PER_EVENT_STD_DEV = 3;

    private final StoppableThread worker;
    private final Random random;
    private final Consumer<ConsensusTransactionImpl> transactionConsumer;
    private final Duration timeBetweenSubmissions;
    private int numSystemTransactions = 0;

    public TransactionFeeder(
            @NonNull final Random random,
            @NonNull final NodeId selfId,
            @NonNull final Consumer<ConsensusTransactionImpl> transactionConsumer,
            @NonNull final Duration timeBetweenSubmissions) {
        this.random = Objects.requireNonNull(random, "random must not be null");
        Objects.requireNonNull(selfId, "selfId must not be null");
        this.transactionConsumer = Objects.requireNonNull(transactionConsumer, "transactionConsumer must not be null");
        this.timeBetweenSubmissions =
                Objects.requireNonNull(timeBetweenSubmissions, "timeBetweenSubmissions must not be null");
        worker = new StoppableThreadConfiguration<>(getStaticThreadManager())
                .setNodeId(selfId)
                .setThreadName("transaction-submitter")
                .setWork(this::feedTransactions)
                .setStopBehavior(Stoppable.StopBehavior.INTERRUPTABLE)
                .build();
    }

    public void start() {
        worker.start();
    }

    public void stop() {
        worker.stop();
    }

    public int getNumSystemTransactions() {
        return numSystemTransactions;
    }

    /**
     * Sleep for a period of time, then feed some transactions to the consumer.
     */
    private void feedTransactions() {
        try {
            MILLISECONDS.sleep(timeBetweenSubmissions.toMillis());
        } catch (final InterruptedException e) {
            // ignored
        }
        final ConsensusTransactionImpl[] txns =
                TransactionUtils.incrementingMixedTransactions(random, TX_PER_EVENT_AVG, TX_PER_EVENT_STD_DEV, 0.5);
        Arrays.stream(txns).forEach(tx -> {
            transactionConsumer.accept(tx);
            if (tx.isSystem()) {
                numSystemTransactions++;
            }
        });
    }
}
