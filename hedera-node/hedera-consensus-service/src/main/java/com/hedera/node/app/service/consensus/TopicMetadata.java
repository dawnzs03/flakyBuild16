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

package com.hedera.node.app.service.consensus;

import com.hedera.hapi.node.base.Key;
import com.hedera.hapi.node.base.Timestamp;
import java.util.Optional;
import java.util.OptionalLong;

// TODO : Remove use of TopicMetadata and change to use Topic instead
/**
 * Topic metadata
 *
 * @param memo topic's memo
 * @param adminKey topic's admin key
 * @param submitKey topic's submit key
 * @param autoRenewDurationSeconds topic's auto-renew duration in seconds
 * @param autoRenewAccountId topic's auto-renew account id
 * @param expirationTimestamp topic's expiration timestamp
 * @param sequenceNumber topic's sequence number
 * @param runningHash topic's running hash
 * @param key topic's key
 * @param isDeleted topic's deleted flag
 */
public record TopicMetadata(
        Optional<String> memo,
        Key adminKey,
        Key submitKey,
        long autoRenewDurationSeconds,
        OptionalLong autoRenewAccountId,
        Timestamp expirationTimestamp,
        long sequenceNumber,
        byte[] runningHash,
        long key,
        boolean isDeleted) {}
