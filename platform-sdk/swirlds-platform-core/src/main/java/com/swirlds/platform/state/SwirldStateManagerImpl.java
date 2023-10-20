/*
 * Copyright (C) 2016-2023 Hedera Hashgraph, LLC
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

package com.swirlds.platform.state;

import static com.swirlds.logging.LogMarker.RECONNECT;
import static com.swirlds.platform.state.SwirldStateManagerUtils.fastCopy;

import com.swirlds.base.time.Time;
import com.swirlds.common.context.PlatformContext;
import com.swirlds.common.system.NodeId;
import com.swirlds.common.system.SoftwareVersion;
import com.swirlds.common.system.SwirldState;
import com.swirlds.common.system.address.AddressBook;
import com.swirlds.common.system.status.StatusActionSubmitter;
import com.swirlds.common.system.transaction.internal.ConsensusTransactionImpl;
import com.swirlds.platform.components.transaction.system.ConsensusSystemTransactionManager;
import com.swirlds.platform.components.transaction.system.PreconsensusSystemTransactionManager;
import com.swirlds.platform.eventhandling.TransactionPool;
import com.swirlds.platform.internal.ConsensusRound;
import com.swirlds.platform.internal.EventImpl;
import com.swirlds.platform.metrics.SwirldStateMetrics;
import com.swirlds.platform.state.signed.SignedState;
import com.swirlds.platform.uptime.UptimeTracker;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>Manages all interactions with the state object required by {@link SwirldState}.</p>
 *
 * <p>Two threads interact with states in this class: pre-consensus event handler and consensus event handler.
 * Transactions are submitted by a different thread. Other threads can access the states by calling
 * {@link #getCurrentSwirldState()} and {@link #getConsensusState()}. Sync threads access state to check if there is an
 * active freeze period. Careful attention must be paid to changes in this class regarding locking and synchronization
 * in this class and its utility classes.</p>
 */
public class SwirldStateManagerImpl implements SwirldStateManager {

    /**
     * use this for all logging, as controlled by the optional data/log4j2.xml file
     */
    private static final Logger logger = LogManager.getLogger(SwirldStateManagerImpl.class);

    /**
     * Stats relevant to SwirldState operations.
     */
    private final SwirldStateMetrics stats;

    /**
     * reference to the state that reflects all known consensus transactions
     */
    private final AtomicReference<State> stateRef = new AtomicReference<>();

    /**
     * The most recent immutable state. No value until the first fast copy is created.
     */
    private final AtomicReference<State> latestImmutableState = new AtomicReference<>();

    /**
     * Contains self transactions to be included in the next event.
     */
    private final TransactionPool transactionPool;

    /**
     * Handle transactions by applying them to a state
     */
    private final TransactionHandler transactionHandler;

    /**
     * Tracks and reports node uptime.
     */
    private final UptimeTracker uptimeTracker;

    /**
     * Handles system transactions pre-consensus
     */
    private final PreconsensusSystemTransactionManager preconsensusSystemTransactionManager;

    /**
     * Handles system transactions post-consensus
     */
    private final ConsensusSystemTransactionManager consensusSystemTransactionManager;

    /**
     * The current software version.
     */
    private final SoftwareVersion softwareVersion;

    // Used for creating mock instances in unit testing
    public SwirldStateManagerImpl() {
        stats = null;
        transactionPool = null;
        preconsensusSystemTransactionManager = null;
        consensusSystemTransactionManager = null;
        transactionHandler = null;
        uptimeTracker = null;
        softwareVersion = null;
    }

    /**
     * Creates a new instance with the provided state.
     *
     * @param platformContext                      the platform context
     * @param addressBook                          the address book
     * @param selfId                               this node's id
     * @param preconsensusSystemTransactionManager the manager for pre-consensus system transactions
     * @param consensusSystemTransactionManager    the manager for post-consensus system transactions
     * @param swirldStateMetrics                   metrics related to SwirldState
     * @param statusActionSubmitter                enables submitting platform status actions
     * @param inFreeze                             indicates if the system is currently in a freeze
     * @param state                                the genesis state
     * @param softwareVersion                      the current software version
     */
    public SwirldStateManagerImpl(
            @NonNull final PlatformContext platformContext,
            @NonNull final AddressBook addressBook,
            @NonNull final NodeId selfId,
            @NonNull final PreconsensusSystemTransactionManager preconsensusSystemTransactionManager,
            @NonNull final ConsensusSystemTransactionManager consensusSystemTransactionManager,
            @NonNull final SwirldStateMetrics swirldStateMetrics,
            @NonNull final StatusActionSubmitter statusActionSubmitter,
            @NonNull final BooleanSupplier inFreeze,
            @NonNull final State state,
            @NonNull final SoftwareVersion softwareVersion) {

        Objects.requireNonNull(platformContext);
        Objects.requireNonNull(addressBook);
        Objects.requireNonNull(selfId);
        this.preconsensusSystemTransactionManager = Objects.requireNonNull(preconsensusSystemTransactionManager);
        this.consensusSystemTransactionManager = Objects.requireNonNull(consensusSystemTransactionManager);
        this.stats = Objects.requireNonNull(swirldStateMetrics);
        Objects.requireNonNull(statusActionSubmitter);
        Objects.requireNonNull(inFreeze);
        Objects.requireNonNull(state);
        this.softwareVersion = Objects.requireNonNull(softwareVersion);

        this.transactionPool = new TransactionPool(platformContext, inFreeze);
        this.transactionHandler = new TransactionHandler(selfId, stats);
        this.uptimeTracker =
                new UptimeTracker(platformContext, addressBook, statusActionSubmitter, selfId, Time.getCurrent());
        initialState(state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean submitTransaction(final ConsensusTransactionImpl transaction, final boolean priority) {
        return transactionPool.submitTransaction(transaction, priority);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void preHandle(final EventImpl event) {
        final long startTime = System.nanoTime();

        State immutableState = latestImmutableState.get();
        while (!immutableState.tryReserve()) {
            immutableState = latestImmutableState.get();
        }
        transactionHandler.preHandle(event, immutableState.getSwirldState());
        immutableState.release();

        stats.preHandleTime(startTime, System.nanoTime());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handlePreConsensusEvent(final EventImpl event) {
        final long startTime = System.nanoTime();

        preconsensusSystemTransactionManager.handleEvent(event);

        stats.preConsensusHandleTime(startTime, System.nanoTime());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleConsensusRound(final ConsensusRound round) {
        final State state = stateRef.get();

        uptimeTracker.handleRound(
                round,
                state.getPlatformDualState().getMutableUptimeData(),
                state.getPlatformState().getAddressBook());
        transactionHandler.handleRound(round, state);
        consensusSystemTransactionManager.handleRound(state, round);
        updateEpoch();
    }

    /**
     * {@inheritDoc}
     */
    public SwirldState getCurrentSwirldState() {
        return stateRef.get().getSwirldState();
    }

    /**
     * IMPORTANT: this method is for unit testing purposes only. The returned state may be deleted at any time while the
     * caller is using it.
     * <p>
     * Returns the most recent immutable state.
     *
     * @return latest immutable state
     */
    public State getLatestImmutableState() {
        return latestImmutableState.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public State getConsensusState() {
        return stateRef.get();
    }

    /**
     * {@inheritDoc}
     * <p>
     * NOTE: This will only clear current transactions, it will not prevent new transactions from being added while
     * clear is being called
     */
    @Override
    public void clear() {
        // clear the transactions
        logger.info(RECONNECT.getMarker(), "SwirldStateManager: clearing transactionPool");
        transactionPool.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void savedStateInFreezePeriod() {
        // set current DualState's lastFrozenTime to be current freezeTime
        stateRef.get().getPlatformDualState().setLastFrozenTimeToBeCurrentFreezeTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadFromSignedState(final SignedState signedState) {
        final State state = signedState.getState();

        state.throwIfDestroyed("state must not be destroyed");
        state.throwIfImmutable("state must be mutable");

        fastCopyAndUpdateRefs(state);
    }

    private void initialState(final State state) {
        state.throwIfDestroyed("state must not be destroyed");
        state.throwIfImmutable("state must be mutable");

        if (stateRef.get() != null) {
            throw new IllegalStateException("Attempt to set initial state when there is already a state reference.");
        }

        // Create a fast copy so there is always an immutable state to
        // invoke handleTransaction on for pre-consensus transactions
        fastCopyAndUpdateRefs(state);
    }

    private void fastCopyAndUpdateRefs(final State state) {
        final State consState = fastCopy(state, stats, softwareVersion);

        // Set latest immutable first to prevent the newly immutable state from being deleted between setting the
        // stateRef and the latestImmutableState
        setLatestImmutableState(state);
        setState(consState);
    }

    /**
     * Sets the consensus state to the state provided. Must be mutable and have a reference count of at least 1.
     *
     * @param state the new mutable state
     */
    private void setState(final State state) {
        final State currVal = stateRef.get();
        if (currVal != null) {
            currVal.release();
        }
        // Do not increment the reference count because the state provided already has a reference count of at least
        // one to represent this reference and to prevent it from being deleted before this reference is set.
        stateRef.set(state);
    }

    private void setLatestImmutableState(final State immutableState) {
        final State currVal = latestImmutableState.get();
        if (currVal != null) {
            currVal.release();
        }
        immutableState.reserve();
        latestImmutableState.set(immutableState);
    }

    private void updateEpoch() {
        final PlatformState platformState = stateRef.get().getPlatformState();
        if (platformState != null) {
            platformState.getPlatformData().updateEpochHash();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInFreezePeriod(final Instant timestamp) {
        return SwirldStateManagerUtils.isInFreezePeriod(timestamp, getConsensusState());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Only invoked by the consensus handler thread
     */
    @Override
    public State getStateForSigning() {
        fastCopyAndUpdateRefs(stateRef.get());
        return latestImmutableState.get();
    }

    /**
     * {@inheritDoc}
     */
    public TransactionPool getTransactionPool() {
        return transactionPool;
    }
}
