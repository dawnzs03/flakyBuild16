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

package com.hedera.node.app.workflows.prehandle;

import com.hedera.hapi.node.base.AccountID;
import com.hedera.node.app.service.token.ReadableAccountStore;
import com.hedera.node.app.workflows.dispatcher.ReadableStoreFactory;
import com.swirlds.common.system.events.Event;
import com.swirlds.common.system.transaction.Transaction;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.stream.Stream;

/** A workflow to pre-handle transactions. */
public interface PreHandleWorkflow {

    /**
     * Starts the pre-handle transaction workflow of the {@link Event}
     *
     * @param readableStoreFactory the {@link ReadableStoreFactory} that is used for looking up stores
     * @param creator The {@link AccountID} of the node that created these transactions
     * @param transactions An {@link Stream} over all transactions to pre-handle
     * @throws NullPointerException if one of the arguments is {@code null}
     */
    void preHandle(
            @NonNull final ReadableStoreFactory readableStoreFactory,
            @NonNull final AccountID creator,
            @NonNull final Stream<Transaction> transactions);

    /**
     * Starts the pre-handle transaction workflow for a single event.
     *
     * <p>If this method is called directly, pre-handle is done on the current thread.
     *
     * @param creator The {@link AccountID} of the node that created these transactions
     * @param storeFactory The {@link ReadableStoreFactory} based on the current state
     * @param accountStore The {@link ReadableAccountStore} based on the current state
     * @param platformTx The {@link Transaction} to pre-handle
     * @return The {@link PreHandleResult} of running pre-handle
     */
    @NonNull
    PreHandleResult preHandleTransaction(
            @NonNull AccountID creator,
            @NonNull ReadableStoreFactory storeFactory,
            @NonNull ReadableAccountStore accountStore,
            @NonNull Transaction platformTx);
}
