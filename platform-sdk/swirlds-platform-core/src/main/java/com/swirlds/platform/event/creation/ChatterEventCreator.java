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

package com.swirlds.platform.event.creation;

import static com.swirlds.logging.LogMarker.CREATE_EVENT;

import com.swirlds.base.time.Time;
import com.swirlds.common.context.PlatformContext;
import com.swirlds.common.crypto.Cryptography;
import com.swirlds.common.stream.Signer;
import com.swirlds.common.system.EventCreationRuleResponse;
import com.swirlds.common.system.NodeId;
import com.swirlds.common.system.SoftwareVersion;
import com.swirlds.common.system.events.BaseEvent;
import com.swirlds.common.system.events.BaseEventHashedData;
import com.swirlds.common.system.events.BaseEventUnhashedData;
import com.swirlds.platform.components.EventCreationRules;
import com.swirlds.platform.components.EventMapper;
import com.swirlds.platform.components.transaction.TransactionSupplier;
import com.swirlds.platform.event.EventUtils;
import com.swirlds.platform.event.GossipEvent;
import com.swirlds.platform.event.tipset.EventCreationConfig;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class encapsulates the workflow required to create new events.
 */
public class ChatterEventCreator {
    private static final Logger logger = LogManager.getLogger(ChatterEventCreator.class);

    /** The software version of the node. */
    private final SoftwareVersion softwareVersion;
    /** This node's address book ID */
    private final NodeId selfId;
    /** An implementor of {@link Signer} */
    private final Signer signer;
    /** An implementor of {@link TransactionSupplier} */
    private final TransactionSupplier transactionSupplier;
    /** Consumes the events that are created */
    private final Consumer<GossipEvent> newEventHandler;
    /** This hashgraph's {@link EventMapper} */
    private final Function<NodeId, GossipEvent> mostRecentEventById;
    /** This object is used for checking whether this node should create an event or not */
    private final EventCreationRules eventCreationRules;
    /** Used for hashing the event when created */
    private final Cryptography hasher;

    private final Time time;

    /**
     * If true, event creation is being handled by the tipset algorithm and this class should not create any events.
     */
    private final boolean disabled;

    public ChatterEventCreator(
            @NonNull final PlatformContext platformContext,
            @NonNull final SoftwareVersion softwareVersion,
            @NonNull final NodeId selfId,
            @NonNull final Signer signer,
            @NonNull final TransactionSupplier transactionSupplier,
            @NonNull final Consumer<GossipEvent> newEventHandler,
            @NonNull final Function<NodeId, GossipEvent> mostRecentEventById,
            @NonNull final EventCreationRules eventCreationRules,
            @NonNull final Cryptography hasher,
            @NonNull final Time time) {
        this.softwareVersion = Objects.requireNonNull(softwareVersion, "the softwareVersion is null");
        this.selfId = Objects.requireNonNull(selfId, "the selfId is null");
        this.signer = Objects.requireNonNull(signer, "the signer is null");
        this.transactionSupplier = Objects.requireNonNull(transactionSupplier, "the transactionSupplier is null");
        this.newEventHandler = Objects.requireNonNull(newEventHandler, "the newEventHandler is null");
        this.mostRecentEventById = Objects.requireNonNull(mostRecentEventById, "the mostRecentEventById is null");
        this.eventCreationRules = Objects.requireNonNull(eventCreationRules, "the eventCreationRules is null");
        this.hasher = Objects.requireNonNull(hasher, "the hasher is null");
        this.time = Objects.requireNonNull(time, "the time is null");
        this.disabled = platformContext
                .getConfiguration()
                .getConfigData(EventCreationConfig.class)
                .useTipsetAlgorithm();
    }

    /**
     * Create a genesis event with no parents
     */
    public void createGenesisEvent() {
        handleNewEvent(buildEvent(null, null));
    }

    /**
     * Create a new event and push it into the gossip/consensus pipeline.
     *
     * @param otherId
     * 		the node ID that will supply the other parent for this event
     * @return true if the event was created, false if not
     */
    public boolean createEvent(@NonNull final NodeId otherId) {
        Objects.requireNonNull(otherId, "the otherId must not be null");

        if (disabled) {
            return false;
        }

        final EventCreationRuleResponse basicRulesResponse = eventCreationRules.shouldCreateEvent();
        if (basicRulesResponse == EventCreationRuleResponse.DONT_CREATE) {
            return false;
        }
        final GossipEvent selfParent = mostRecentEventById.apply(selfId);
        final GossipEvent otherParent = mostRecentEventById.apply(otherId);
        // if the basic rules returned a CREATE, this overrides all subsequent rules, so we don't check the parent based
        // rules
        if (basicRulesResponse != EventCreationRuleResponse.CREATE
                && eventCreationRules.shouldCreateEvent(selfParent, otherParent)
                        == EventCreationRuleResponse.DONT_CREATE) {
            return false;
        }

        handleNewEvent(buildEvent(selfParent, otherParent));
        return true;
    }

    private void handleNewEvent(final GossipEvent event) {
        logEventCreation(event);
        newEventHandler.accept(event);
    }

    /**
     * Construct an event object.
     */
    private GossipEvent buildEvent(final BaseEvent selfParent, final BaseEvent otherParent) {

        final BaseEventHashedData hashedData = new BaseEventHashedData(
                softwareVersion,
                selfId,
                EventUtils.getEventGeneration(selfParent),
                EventUtils.getEventGeneration(otherParent),
                EventUtils.getEventHash(selfParent),
                EventUtils.getEventHash(otherParent),
                EventUtils.getChildTimeCreated(time.now(), selfParent),
                transactionSupplier.getTransactions());
        hasher.digestSync(hashedData);

        final BaseEventUnhashedData unhashedData = new BaseEventUnhashedData(
                EventUtils.getCreatorId(otherParent),
                signer.sign(hashedData.getHash().getValue()).getSignatureBytes());
        final GossipEvent gossipEvent = new GossipEvent(hashedData, unhashedData);
        gossipEvent.buildDescriptor();
        return gossipEvent;
    }

    /**
     * Write to the log (if configured) every time an event is created.
     *
     * @param event
     * 		the created event to be logged
     */
    protected void logEventCreation(final GossipEvent event) {
        logger.debug(CREATE_EVENT.getMarker(), "Creating {}", event::toString);
    }
}
