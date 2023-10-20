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

package com.swirlds.platform.internal;

import com.swirlds.base.utility.ToStringBuilder;
import com.swirlds.common.system.Round;
import com.swirlds.common.system.events.ConsensusEvent;
import com.swirlds.platform.consensus.GraphGenerations;
import com.swirlds.platform.event.EventUtils;
import com.swirlds.platform.util.iterator.TypedIterator;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * A consensus round with all its events.
 */
public class ConsensusRound implements Round {

    /** an unmodifiable list of consensus events in this round, in consensus order */
    private final List<EventImpl> consensusEvents;

    /** the consensus generations when this round reached consensus */
    private final GraphGenerations generations;

    /** this round's number */
    private final long roundNum;

    /** the last event in the round */
    private EventImpl lastEvent;

    /** The number of application transactions in this round */
    private int numAppTransactions = 0;

    /**
     * The event that, when added to the hashgraph, caused this round to reach consensus.
     */
    private final EventImpl keystoneEvent;

    /**
     * The consensus timestamp of this round.
     */
    private final Instant consensusTimestamp;

    /**
     * Create a new instance with the provided consensus events.
     *
     * @param consensusEvents the events in the round, in consensus order
     * @param keystoneEvent   the event that, when added to the hashgraph, caused this round to reach consensus
     * @param generations     the consensus generations for this round
     */
    public ConsensusRound(
            @NonNull final List<EventImpl> consensusEvents,
            @NonNull final EventImpl keystoneEvent,
            @NonNull final GraphGenerations generations) {

        Objects.requireNonNull(consensusEvents, "consensusEvents must not be null");
        Objects.requireNonNull(keystoneEvent, "keystoneEvent must not be null");
        Objects.requireNonNull(generations, "generations must not be null");

        this.consensusEvents = Collections.unmodifiableList(consensusEvents);
        this.keystoneEvent = keystoneEvent;
        this.generations = generations;

        for (final EventImpl e : consensusEvents) {
            numAppTransactions += e.getNumAppTransactions();
        }

        final EventImpl lastInList = consensusEvents.get(consensusEvents.size() - 1);
        if (lastInList.isLastInRoundReceived()) {
            lastEvent = lastInList;
        }

        this.roundNum = consensusEvents.get(0).getRoundReceived();

        // FUTURE WORK: once we properly handle rounds with no events, we need to define the consensus timestamp of a
        // round with no events as 1 nanosecond later than the previous round.
        consensusTimestamp = consensusEvents.get(consensusEvents.size() - 1).getLastTransTime();
    }

    /**
     * Returns the number of application transactions in this round
     *
     * @return the number of application transactions
     */
    public int getNumAppTransactions() {
        return numAppTransactions;
    }

    /**
     * Provides an unmodifiable list of the consensus event in this round.
     *
     * @return the list of events in this round
     */
    public List<EventImpl> getConsensusEvents() {
        return consensusEvents;
    }

    /**
     * @return the consensus generations when this round reached consensus
     */
    public GraphGenerations getGenerations() {
        return generations;
    }

    /**
     * @return the number of events in this round
     */
    public int getNumEvents() {
        return consensusEvents.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<ConsensusEvent> iterator() {
        return new TypedIterator<>(consensusEvents.iterator());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getRoundNum() {
        return roundNum;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return consensusEvents.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getEventCount() {
        return consensusEvents.size();
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Instant getConsensusTimestamp() {
        return consensusTimestamp;
    }

    /**
     * @return the last event of this round, or null if this round is not complete
     */
    public EventImpl getLastEvent() {
        return lastEvent;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final ConsensusRound that = (ConsensusRound) other;
        return Objects.equals(consensusEvents, that.consensusEvents);
    }

    /**
     * @return the event that, when added to the hashgraph, caused this round to reach consensus
     */
    public @NonNull EventImpl getKeystoneEvent() {
        return keystoneEvent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(consensusEvents);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("round", roundNum)
                .append("consensus events", EventUtils.toShortStrings(consensusEvents))
                .toString();
    }
}
