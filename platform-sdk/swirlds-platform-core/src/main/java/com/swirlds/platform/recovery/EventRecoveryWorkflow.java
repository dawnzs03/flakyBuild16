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

package com.swirlds.platform.recovery;

import static com.swirlds.logging.LogMarker.EXCEPTION;
import static com.swirlds.logging.LogMarker.STARTUP;
import static com.swirlds.platform.util.BootstrapUtils.loadAppMain;
import static com.swirlds.platform.util.BootstrapUtils.setupConstructableRegistry;

import com.swirlds.common.config.ConsensusConfig;
import com.swirlds.common.config.StateConfig;
import com.swirlds.common.context.PlatformContext;
import com.swirlds.common.crypto.Hash;
import com.swirlds.common.io.IOIterator;
import com.swirlds.common.merkle.crypto.MerkleCryptoFactory;
import com.swirlds.common.notification.NotificationEngine;
import com.swirlds.common.stream.RunningHashCalculatorForStream;
import com.swirlds.common.system.InitTrigger;
import com.swirlds.common.system.NodeId;
import com.swirlds.common.system.Round;
import com.swirlds.common.system.SwirldDualState;
import com.swirlds.common.system.SwirldMain;
import com.swirlds.common.system.SwirldState;
import com.swirlds.common.system.events.ConsensusEvent;
import com.swirlds.common.system.state.notifications.NewRecoveredStateListener;
import com.swirlds.common.system.state.notifications.NewRecoveredStateNotification;
import com.swirlds.common.utility.CompareTo;
import com.swirlds.config.api.Configuration;
import com.swirlds.platform.internal.EventImpl;
import com.swirlds.platform.recovery.emergencyfile.EmergencyRecoveryFile;
import com.swirlds.platform.recovery.internal.EventStreamRoundIterator;
import com.swirlds.platform.recovery.internal.RecoveryPlatform;
import com.swirlds.platform.state.MinGenInfo;
import com.swirlds.platform.state.State;
import com.swirlds.platform.state.signed.ReservedSignedState;
import com.swirlds.platform.state.signed.SignedState;
import com.swirlds.platform.state.signed.SignedStateFileReader;
import com.swirlds.platform.state.signed.SignedStateFileWriter;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles the event stream recovery workflow.
 */
public final class EventRecoveryWorkflow {

    private static final Logger logger = LogManager.getLogger(EventRecoveryWorkflow.class);

    public static final long NO_FINAL_ROUND = Long.MAX_VALUE;

    private EventRecoveryWorkflow() {}

    /**
     * Read a signed state from disk and apply events from an event stream on disk. Write the resulting signed state to
     * disk.
     *
     * @param platformContext         the platform context
     * @param signedStateFile         the bootstrap signed state file
     * @param configurationFiles      files containing configuration
     * @param eventStreamDirectory    a directory containing the event stream
     * @param mainClassName           the fully qualified class name of the {@link SwirldMain} for the app
     * @param finalRound              if not {@link #NO_FINAL_ROUND} then stop reapplying events after this round has
     *                                been generated
     * @param resultingStateDirectory the location where the resulting state will be written
     * @param selfId                  the self ID of the node
     * @param allowPartialRounds      if true then allow the last round to be missing events, if false then ignore the
     *                                last round if it does not have all of its events
     * @param loadSigningKeys         if true then load the signing keys
     */
    public static void recoverState(
            @NonNull final PlatformContext platformContext,
            @NonNull final Path signedStateFile,
            @NonNull final List<Path> configurationFiles,
            @NonNull final Path eventStreamDirectory,
            @NonNull final String mainClassName,
            @NonNull final Boolean allowPartialRounds,
            @NonNull final Long finalRound,
            @NonNull final Path resultingStateDirectory,
            @NonNull final NodeId selfId,
            final boolean loadSigningKeys)
            throws IOException {
        Objects.requireNonNull(platformContext);
        Objects.requireNonNull(signedStateFile, "signedStateFile must not be null");
        Objects.requireNonNull(configurationFiles, "configurationFiles must not be null");
        Objects.requireNonNull(eventStreamDirectory, "eventStreamDirectory must not be null");
        Objects.requireNonNull(mainClassName, "mainClassName must not be null");
        Objects.requireNonNull(allowPartialRounds, "allowPartialRounds must not be null");
        Objects.requireNonNull(finalRound, "finalRound must not be null");
        Objects.requireNonNull(resultingStateDirectory, "resultingStateDirectory must not be null");
        Objects.requireNonNull(selfId, "selfId must not be null");

        setupConstructableRegistry();

        final SwirldMain appMain = loadAppMain(mainClassName);

        if (!Files.exists(resultingStateDirectory)) {
            Files.createDirectories(resultingStateDirectory);
        }

        logger.info(STARTUP.getMarker(), "Loading state from {}", signedStateFile);

        try (final ReservedSignedState initialState = SignedStateFileReader.readStateFile(
                        platformContext, signedStateFile)
                .reservedSignedState()) {

            logger.info(
                    STARTUP.getMarker(),
                    "State from round {} loaded.",
                    initialState.get().getRound());
            logger.info(STARTUP.getMarker(), "Loading event stream at {}", eventStreamDirectory);

            final IOIterator<Round> roundIterator = new EventStreamRoundIterator(
                    eventStreamDirectory, initialState.get().getRound() + 1, allowPartialRounds);

            logger.info(STARTUP.getMarker(), "Reapplying transactions");

            final ReservedSignedState resultingState = reapplyTransactions(
                    platformContext, initialState, appMain, roundIterator, finalRound, selfId, loadSigningKeys);

            logger.info(
                    STARTUP.getMarker(),
                    "Finished reapplying transactions, writing state to {}",
                    resultingStateDirectory);

            SignedStateFileWriter.writeSignedStateFilesToDirectory(
                    selfId, resultingStateDirectory, resultingState.get(), platformContext.getConfiguration());
            final StateConfig stateConfig = platformContext.getConfiguration().getConfigData(StateConfig.class);
            updateEmergencyRecoveryFile(
                    stateConfig, resultingStateDirectory, initialState.get().getConsensusTimestamp());

            logger.info(STARTUP.getMarker(), "Recovery process completed");

            resultingState.close();
        }
    }

    /**
     * Update the resulting emergency recovery file to contain the bootstrap timestamp.
     *
     * @param stateConfig     the state configuration for the platform
     * @param recoveryFileDir the directory containing the emergency recovery file
     * @param bootstrapTime   the consensus timestamp of the bootstrap state
     */
    public static void updateEmergencyRecoveryFile(
            @NonNull final StateConfig stateConfig,
            @NonNull final Path recoveryFileDir,
            @NonNull final Instant bootstrapTime) {
        try {
            // Read the existing recovery file and write it to a backup directory
            final EmergencyRecoveryFile oldRecoveryFile = EmergencyRecoveryFile.read(stateConfig, recoveryFileDir);
            if (oldRecoveryFile == null) {
                logger.error(EXCEPTION.getMarker(), "Recovery file does not exist at {}", recoveryFileDir);
                return;
            }
            final Path backupDir = recoveryFileDir.resolve("backup");
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }
            oldRecoveryFile.write(backupDir);

            // Create a new recovery file with the bootstrap time, overwriting the original
            final EmergencyRecoveryFile newRecoveryFile =
                    new EmergencyRecoveryFile(oldRecoveryFile.recovery().state(), bootstrapTime);
            newRecoveryFile.write(recoveryFileDir);
        } catch (final IOException e) {
            logger.error(
                    EXCEPTION.getMarker(),
                    "Exception occurred when updating the emergency recovery file with the bootstrap time");
        }
    }

    /**
     * Send a notification that the recovered state has been calculated.
     *
     * @param notificationEngine the notification engine used to dispatch the notification
     * @param recoveredState     the recovered state
     */
    private static void notifyStateRecovered(
            final NotificationEngine notificationEngine, final SignedState recoveredState) {
        final NewRecoveredStateNotification notification = new NewRecoveredStateNotification(
                recoveredState.getSwirldState(), recoveredState.getRound(), recoveredState.getConsensusTimestamp());
        notificationEngine.dispatch(NewRecoveredStateListener.class, notification);
    }

    /**
     * Apply transactions on top of a state to produce a new state
     *
     * @param platformContext the platform context
     * @param initialState    the starting signed state
     * @param appMain         the {@link SwirldMain} for the app. Ignored if null.
     * @param roundIterator   an iterator that walks over transactions
     * @param finalRound      the last round to apply to the state (inclusive), will stop earlier if the event stream
     *                        does not have events from the final round
     * @param selfId          the self ID of the node
     * @param loadSigningKeys if true then load the signing keys
     * @return the resulting signed state
     * @throws IOException if there is a problem reading from the event stream file
     */
    @NonNull
    public static ReservedSignedState reapplyTransactions(
            @NonNull final PlatformContext platformContext,
            @NonNull final ReservedSignedState initialState,
            @NonNull final SwirldMain appMain,
            @NonNull final IOIterator<Round> roundIterator,
            final long finalRound,
            @NonNull final NodeId selfId,
            final boolean loadSigningKeys)
            throws IOException {

        Objects.requireNonNull(platformContext, "platformContext must not be null");
        Objects.requireNonNull(initialState, "initialState must not be null");
        Objects.requireNonNull(appMain, "appMain must not be null");
        Objects.requireNonNull(roundIterator, "roundIterator must not be null");
        Objects.requireNonNull(selfId, "selfId must not be null");

        final Configuration configuration = platformContext.getConfiguration();

        final long roundsNonAncient =
                configuration.getConfigData(ConsensusConfig.class).roundsNonAncient();

        initialState.get().getState().throwIfImmutable("initial state must be mutable");

        logger.info(STARTUP.getMarker(), "Initializing application state");

        final RecoveryPlatform platform =
                new RecoveryPlatform(configuration, initialState.get(), selfId, loadSigningKeys);

        initialState
                .get()
                .getSwirldState()
                .init(
                        platform,
                        initialState.get().getState().getSwirldDualState(),
                        InitTrigger.EVENT_STREAM_RECOVERY,
                        initialState
                                .get()
                                .getState()
                                .getPlatformState()
                                .getPlatformData()
                                .getCreationSoftwareVersion());

        appMain.init(platform, platform.getSelfId());

        ReservedSignedState signedState = initialState;

        // Apply events to the state
        while (roundIterator.hasNext()
                && (finalRound == -1 || roundIterator.peek().getRoundNum() <= finalRound)) {
            final Round round = roundIterator.next();

            logger.info(
                    STARTUP.getMarker(),
                    "Applying {} events from round {}",
                    round.getEventCount(),
                    round.getRoundNum());

            signedState = handleNextRound(platformContext, signedState, round, roundsNonAncient);
            platform.setLatestState(signedState.get());
        }

        logger.info(STARTUP.getMarker(), "Hashing resulting signed state");
        try {
            MerkleCryptoFactory.getInstance()
                    .digestTreeAsync(signedState.get().getState())
                    .get();
        } catch (final InterruptedException e) {
            throw new RuntimeException("interrupted while attempting to hash the state", e);
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        }
        logger.info(STARTUP.getMarker(), "Hashing complete");

        // Let the application know about the recovered state
        notifyStateRecovered(platform.getNotificationEngine(), signedState.get());

        platform.close();

        return signedState;
    }

    /**
     * Apply a single round and generate a new state. The previous state is released.
     *
     * @param platformContext  the current context
     * @param previousState    the previous round's signed state
     * @param round            the next round
     * @param roundsNonAncient the number of rounds until an event becomes ancient
     * @return the resulting signed state
     */
    private static ReservedSignedState handleNextRound(
            @NonNull final PlatformContext platformContext,
            @NonNull final ReservedSignedState previousState,
            @NonNull final Round round,
            final long roundsNonAncient) {

        final Instant currentRoundTimestamp = getRoundTimestamp(round);

        previousState.get().getState().throwIfImmutable();
        final State newState = previousState.get().getState().copy();
        newState.getPlatformState()
                .getPlatformData()
                .setRound(round.getRoundNum())
                .setNumEventsCons(previousState.get().getNumEventsCons() + round.getEventCount())
                .setHashEventsCons(getHashEventsCons(previousState.get().getHashEventsCons(), round))
                .setEvents(collectEventsForRound(
                        roundsNonAncient, previousState.get().getEvents(), round))
                .setConsensusTimestamp(currentRoundTimestamp)
                .setMinGenInfo(
                        getMinGenInfo(roundsNonAncient, previousState.get().getMinGenInfo(), round))
                .setCreationSoftwareVersion(previousState
                        .get()
                        .getState()
                        .getPlatformState()
                        .getPlatformData()
                        .getCreationSoftwareVersion());

        applyTransactions(
                previousState.get().getSwirldState().cast(),
                newState.getSwirldState().cast(),
                newState.getSwirldDualState(),
                round);

        final boolean isFreezeState = isFreezeState(
                previousState.get().getConsensusTimestamp(),
                currentRoundTimestamp,
                newState.getPlatformDualState().getFreezeTime());
        if (isFreezeState) {
            newState.getPlatformDualState().setLastFrozenTimeToBeCurrentFreezeTime();
        }

        final ReservedSignedState signedState = new SignedState(
                        platformContext, newState, "EventRecoveryWorkflow.handleNextRound()", isFreezeState)
                .reserve("recovery");
        previousState.close();

        return signedState;
    }

    /**
     * Calculate the running hash at the end of a round.
     *
     * @param previousRunningHash the previous running hash
     * @param round               the current round
     * @return the running event hash at the end of the current round
     */
    static Hash getHashEventsCons(final Hash previousRunningHash, final Round round) {
        final RunningHashCalculatorForStream<EventImpl> hashCalculator = new RunningHashCalculatorForStream<>();
        hashCalculator.setRunningHash(previousRunningHash);

        for (final ConsensusEvent event : round) {
            hashCalculator.addObject((EventImpl) event);
        }

        final Hash runningHash = hashCalculator.getRunningHash();
        hashCalculator.close();

        return runningHash;
    }

    /**
     * Get the timestamp for a round (equivalent to the timestamp of the last event).
     *
     * @param round the current round
     * @return the round's timestamp
     */
    static Instant getRoundTimestamp(final Round round) {
        final Iterator<ConsensusEvent> iterator = round.iterator();

        while (iterator.hasNext()) {
            final ConsensusEvent event = iterator.next();

            if (!iterator.hasNext()) {
                return event.getConsensusTimestamp();
            }
        }

        throw new IllegalStateException("round has no events");
    }

    /**
     * Apply the next round of transactions and produce a new swirld state.
     *
     * @param immutableState the immutable swirld state for the previous round
     * @param mutableState   the swirld state for the current round
     * @param dualState      the dual state for the current round
     * @param round          the current round
     */
    static void applyTransactions(
            final SwirldState immutableState,
            final SwirldState mutableState,
            final SwirldDualState dualState,
            final Round round) {

        mutableState.throwIfImmutable();

        for (final ConsensusEvent event : round) {
            immutableState.preHandle(event);
        }

        mutableState.handleConsensusRound(round, dualState);

        // FUTURE WORK: there are currently no system transactions that are capable of modifying
        //  the state. If/when system transactions capable of modifying state are added, this workflow
        //  must be updated to reflect that behavior.
    }

    /**
     * Check if this state should be a freeze state.
     *
     * @param previousRoundTimestamp the timestamp of the previous round
     * @param currentRoundTimestamp  the timestamp of the current round
     * @param freezeTime             the freeze time in the state
     * @return true if this round will create a freeze state
     */
    static boolean isFreezeState(
            final Instant previousRoundTimestamp, final Instant currentRoundTimestamp, final Instant freezeTime) {

        if (freezeTime == null) {
            return false;
        }

        return CompareTo.isLessThan(previousRoundTimestamp, freezeTime)
                && CompareTo.isGreaterThanOrEqualTo(currentRoundTimestamp, freezeTime);
    }

    /**
     * Collect the events that need to go into a signed state using the previous round's state and the new round.
     *
     * @param roundsNonAncient the number of rounds until an event becomes ancient
     * @param previousEvents   the previous round's state
     * @param round            the current round
     * @return an array of all non-ancient events
     */
    static EventImpl[] collectEventsForRound(
            final long roundsNonAncient, final EventImpl[] previousEvents, final Round round) {

        final long firstRoundToKeep = round.getRoundNum() - roundsNonAncient;

        final List<EventImpl> eventList = new ArrayList<>();
        for (final EventImpl event : previousEvents) {
            if (event.getRoundReceived() >= firstRoundToKeep) {
                eventList.add(event);
            }
        }

        for (final ConsensusEvent event : round) {
            eventList.add((EventImpl) event);
        }

        return eventList.toArray(new EventImpl[0]);
    }

    /**
     * <p>
     * Get the minimum generation info for all non-ancient rounds.
     * </p>
     *
     * <p>
     * This implementation differs from what happens in a real platform because the event stream contains insufficient
     * information to fully reconstruct all consensus data. This implementation will reuse the minimum generation info
     * from the previous rounds, and will set the minimum generation for the current round to be equal to the minimum
     * generation of all events in the current round.
     * </p>
     *
     * @param roundsNonAncient   the number of rounds until an event becomes ancient
     * @param previousMinGenInfo the previous round's minimum generation info
     * @param round              the current round
     * @return minimum generation info for the rounds described by the events
     */
    static List<MinGenInfo> getMinGenInfo(
            final long roundsNonAncient, final List<MinGenInfo> previousMinGenInfo, final Round round) {

        final long firstRoundToKeep = round.getRoundNum() - roundsNonAncient;
        final List<MinGenInfo> minGenInfos = new ArrayList<>();

        for (final MinGenInfo minGenInfo : previousMinGenInfo) {
            if (minGenInfo.round() >= firstRoundToKeep) {
                minGenInfos.add(minGenInfo);
            }
        }

        if (!round.isEmpty()) {
            long minimumGeneration = Integer.MAX_VALUE;
            for (final ConsensusEvent event : round) {
                final EventImpl eventImpl = (EventImpl) event;
                minimumGeneration = Math.min(minimumGeneration, eventImpl.getGeneration());
            }
            minGenInfos.add(new MinGenInfo(round.getRoundNum(), minimumGeneration));
        }

        return minGenInfos;
    }

    /**
     * Repair an event stream. A damaged event stream might be created when a network is abruptly shut down in the
     * middle of writing an event stream file. After the repair has been completed, the new event stream will contain
     * only events up to the specified event, and the fill will be terminated with a hash that matches the data that
     * ends up in the resulting stream.
     *
     * @param eventStreamDirectory         the original event stream directory
     * @param repairedEventStreamDirectory the location where the repaired event stream will be written
     * @param finalEventRound              the round of the final event that should be included in the event stream
     * @param finalEventHash               the hash of the final event that should be included in the event stream
     */
    public static void repairEventStream(
            final Path eventStreamDirectory,
            final Path repairedEventStreamDirectory,
            final long finalEventRound,
            final Hash finalEventHash) {

        // FUTURE WORK https://github.com/swirlds/swirlds-platform/issues/6235

    }
}
