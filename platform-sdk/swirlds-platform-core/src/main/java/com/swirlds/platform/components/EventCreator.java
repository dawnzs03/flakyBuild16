/*
 * Copyright (C) 2021-2023 Hedera Hashgraph, LLC
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

package com.swirlds.platform.components;

import static com.swirlds.logging.LogMarker.CREATE_EVENT;

import com.swirlds.common.context.PlatformContext;
import com.swirlds.common.crypto.CryptographyHolder;
import com.swirlds.common.stream.Signer;
import com.swirlds.common.system.EventCreationRuleResponse;
import com.swirlds.common.system.NodeId;
import com.swirlds.common.system.SoftwareVersion;
import com.swirlds.common.system.events.BaseEventHashedData;
import com.swirlds.common.system.events.BaseEventUnhashedData;
import com.swirlds.platform.components.transaction.TransactionSupplier;
import com.swirlds.platform.consensus.GraphGenerations;
import com.swirlds.platform.event.EventUtils;
import com.swirlds.platform.event.SelfEventStorage;
import com.swirlds.platform.event.creation.AncientParentsRule;
import com.swirlds.platform.event.tipset.EventCreationConfig;
import com.swirlds.platform.eventhandling.TransactionPool;
import com.swirlds.platform.internal.EventImpl;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.time.Instant;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class encapsulates the workflow required to create new events.
 */
public class EventCreator {
    private static final Logger logger = LogManager.getLogger(EventCreator.class);

    /** The software version of the node. */
    private final SoftwareVersion softwareVersion;

    /** This node's address book ID */
    private final NodeId selfId;

    /** An implementor of {@link Signer} */
    private final Signer signer;

    /** Checks for ancient parents */
    private final AncientParentsRule ancientParentsCheck;

    /** An implementor of {@link TransactionSupplier} */
    private final TransactionSupplier transactionSupplier;

    /** An implementor of {@link EventHandler} */
    private final EventHandler newEventHandler;

    /** This hashgraph's {@link EventMapper} */
    private final EventMapper eventMapper;

    /** Stores the most recent event created by me */
    private final SelfEventStorage selfEventStorage;

    /** An implementor of {@link TransactionPool} */
    private final TransactionPool transactionPool;

    /** Indicates if the system is currently in a freeze. */
    private final BooleanSupplier inFreeze;

    /** This object is used for checking whether this node should create an event or not */
    private final EventCreationRules eventCreationRules;

    /**
     * If true, event creation is being handled by the tipset algorithm and this class should not create any events.
     */
    private final boolean disabled;

    /**
     * Construct a new EventCreator.
     *
     * @param platformContext          the platform context for this node
     * @param softwareVersion          the software version of the node
     * @param selfId                   the ID of this node
     * @param signer                   responsible for signing new events
     * @param graphGenerationsSupplier supplies the key generation number from the hashgraph
     * @param transactionSupplier      this method supplies transactions that should be inserted into newly created
     *                                 events
     * @param newEventHandler          this method is passed all newly created events
     * @param selfEventStorage         stores the most recent event created by me
     * @param eventMapper              the object that tracks the most recent events from each node
     * @param transactionPool          the TransactionPool
     * @param inFreeze                 indicates if the system is currently in a freeze
     * @param eventCreationRules       the object used for checking if we should create an event or not
     */
    public EventCreator(
            @NonNull final PlatformContext platformContext,
            @NonNull final SoftwareVersion softwareVersion,
            @NonNull final NodeId selfId,
            @NonNull final Signer signer,
            @NonNull final Supplier<GraphGenerations> graphGenerationsSupplier,
            @NonNull final TransactionSupplier transactionSupplier,
            @NonNull final EventHandler newEventHandler,
            @NonNull final EventMapper eventMapper,
            @NonNull final SelfEventStorage selfEventStorage,
            @NonNull final TransactionPool transactionPool,
            @NonNull final BooleanSupplier inFreeze,
            @NonNull final EventCreationRules eventCreationRules) {
        this.softwareVersion = Objects.requireNonNull(softwareVersion, "the software version is null");
        this.selfId = Objects.requireNonNull(selfId, "the self ID is null");
        this.signer = Objects.requireNonNull(signer, "the signer is null");
        this.ancientParentsCheck = new AncientParentsRule(
                Objects.requireNonNull(graphGenerationsSupplier, "the graph generations supplier is null"));
        this.transactionSupplier = Objects.requireNonNull(transactionSupplier, "the transaction supplier is null");
        this.newEventHandler = Objects.requireNonNull(newEventHandler, "the new event handler is null");
        this.eventMapper = Objects.requireNonNull(eventMapper, "the event mapper is null");
        this.selfEventStorage = Objects.requireNonNull(selfEventStorage, "the self event storage is null");
        this.transactionPool = Objects.requireNonNull(transactionPool, "the transaction pool is null");
        this.inFreeze = Objects.requireNonNull(inFreeze, "the in freeze is null");
        this.eventCreationRules = Objects.requireNonNull(eventCreationRules, "the event creation rules is null");
        this.disabled = platformContext
                .getConfiguration()
                .getConfigData(EventCreationConfig.class)
                .useTipsetAlgorithm();
    }

    /**
     * Create a new event and push it into the gossip/consensus pipeline.
     *
     * @param otherId the node ID that will supply the other parent for this event
     */
    public boolean createEvent(final NodeId otherId) {

        if (disabled) {
            return false;
        }

        if (eventCreationRules.shouldCreateEvent() == EventCreationRuleResponse.DONT_CREATE) {
            return false;
        }

        // We don't want to create multiple events with the same other parent, so we have to check if we
        // already created an event with this particular other parent.
        //
        // We still want to create an event if there are state signature transactions when we are frozen.
        if (hasOtherParentAlreadyBeenUsed(otherId) && !hasSignatureTransactionsWhileFrozen()) {
            return false;
        }

        final EventImpl otherParent = eventMapper.getMostRecentEvent(otherId);
        final EventImpl selfParent = selfEventStorage.getMostRecentSelfEvent();

        if (eventCreationRules.shouldCreateEvent(selfParent, otherParent) == EventCreationRuleResponse.DONT_CREATE) {
            return false;
        }

        // Don't create an event if both parents are old.
        if (ancientParentsCheck.areBothParentsAncient(selfParent, otherParent)) {
            logger.debug(
                    CREATE_EVENT.getMarker(),
                    "Both parents are ancient, selfParent: {}, otherParent: {}",
                    () -> EventUtils.toShortString(selfParent),
                    () -> EventUtils.toShortString(otherParent));
            return false;
        }

        handleNewEvent(buildEvent(selfParent, otherParent));
        return true;
    }

    private void handleNewEvent(final EventImpl event) {
        logEventCreation(event);
        selfEventStorage.setMostRecentSelfEvent(event);
        newEventHandler.handleEvent(event);
    }

    /**
     * Construct an event object.
     */
    protected EventImpl buildEvent(final EventImpl selfParent, final EventImpl otherParent) {

        final BaseEventHashedData hashedData = new BaseEventHashedData(
                softwareVersion,
                selfId,
                EventUtils.getEventGeneration(selfParent),
                EventUtils.getEventGeneration(otherParent),
                EventUtils.getEventHash(selfParent),
                EventUtils.getEventHash(otherParent),
                EventUtils.getChildTimeCreated(Instant.now(), selfParent),
                transactionSupplier.getTransactions());
        CryptographyHolder.get().digestSync(hashedData);

        final BaseEventUnhashedData unhashedData = new BaseEventUnhashedData(
                EventUtils.getCreatorId(otherParent),
                signer.sign(hashedData.getHash().getValue()).getSignatureBytes());

        return new EventImpl(hashedData, unhashedData, selfParent, otherParent);
    }

    /**
     * Check if the most recent event from the given node has been used as an other parent by an event created by the
     * current node.
     *
     * @param otherId the ID of the node supplying the other parent
     */
    protected boolean hasOtherParentAlreadyBeenUsed(final NodeId otherId) {
        return !Objects.equals(selfId, otherId) && eventMapper.hasMostRecentEventBeenUsedAsOtherParent(otherId);
    }

    /**
     * Check if there are signature transactions waiting to be inserted into an event during a freeze
     */
    protected boolean hasSignatureTransactionsWhileFrozen() {
        return transactionPool.hasBufferedSignatureTransactions() && inFreeze.getAsBoolean();
    }

    /**
     * Write to the log (if configured) every time an event is created.
     *
     * @param event the created event to be logged
     */
    protected void logEventCreation(final EventImpl event) {
        logger.debug(CREATE_EVENT.getMarker(), "Creating {}", event::toMediumString);
    }
}
