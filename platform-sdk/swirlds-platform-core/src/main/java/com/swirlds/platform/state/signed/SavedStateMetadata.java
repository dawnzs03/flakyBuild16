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

package com.swirlds.platform.state.signed;

import static com.swirlds.common.formatting.StringFormattingUtils.formattedList;
import static com.swirlds.common.utility.CommonUtils.unhex;
import static com.swirlds.logging.LogMarker.STARTUP;
import static com.swirlds.platform.state.signed.SavedStateMetadataField.CONSENSUS_TIMESTAMP;
import static com.swirlds.platform.state.signed.SavedStateMetadataField.MINIMUM_GENERATION_NON_ANCIENT;
import static com.swirlds.platform.state.signed.SavedStateMetadataField.NODE_ID;
import static com.swirlds.platform.state.signed.SavedStateMetadataField.NUMBER_OF_CONSENSUS_EVENTS;
import static com.swirlds.platform.state.signed.SavedStateMetadataField.ROUND;
import static com.swirlds.platform.state.signed.SavedStateMetadataField.RUNNING_EVENT_HASH;
import static com.swirlds.platform.state.signed.SavedStateMetadataField.SIGNING_NODES;
import static com.swirlds.platform.state.signed.SavedStateMetadataField.SIGNING_WEIGHT_SUM;
import static com.swirlds.platform.state.signed.SavedStateMetadataField.SOFTWARE_VERSION;
import static com.swirlds.platform.state.signed.SavedStateMetadataField.TOTAL_WEIGHT;
import static com.swirlds.platform.state.signed.SavedStateMetadataField.WALL_CLOCK_TIME;

import com.swirlds.common.crypto.Hash;
import com.swirlds.common.formatting.TextTable;
import com.swirlds.common.system.NodeId;
import com.swirlds.platform.state.PlatformData;
import com.swirlds.platform.state.PlatformState;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Metadata about a saved state. Fields in this record may be null if they are not present in the metadata file. All
 * fields in this record will be null if the metadata file is missing.
 *
 * @param round                       the round of the signed state, corresponds to
 *                                    {@link SavedStateMetadataField#ROUND}
 * @param numberOfConsensusEvents     the number of consensus events, starting from genesis, that have been handled to
 *                                    create this state, corresponds to
 *                                    {@link SavedStateMetadataField#NUMBER_OF_CONSENSUS_EVENTS}
 * @param consensusTimestamp          the consensus timestamp of this state, corresponds to
 *                                    {@link SavedStateMetadataField#CONSENSUS_TIMESTAMP}
 * @param runningEventHash            the running hash of all events, starting from genesis, that have been handled to
 *                                    create this state, corresponds to
 *                                    {@link SavedStateMetadataField#RUNNING_EVENT_HASH}
 * @param minimumGenerationNonAncient the minimum generation of non-ancient events after this state reached consensus,
 *                                    corresponds to {@link SavedStateMetadataField#MINIMUM_GENERATION_NON_ANCIENT}
 * @param softwareVersion             the application software version that created this state, corresponds to
 *                                    {@link SavedStateMetadataField#SOFTWARE_VERSION}
 * @param wallClockTime               the wall clock time when this state was written to disk, corresponds to
 *                                    {@link SavedStateMetadataField#WALL_CLOCK_TIME}
 * @param nodeId                      the ID of the node that wrote this state to disk, corresponds to
 *                                    {@link SavedStateMetadataField#NODE_ID}
 * @param signingNodes                a comma separated list of node IDs that signed this state, corresponds to
 *                                    {@link SavedStateMetadataField#SIGNING_NODES}
 * @param signingWeightSum            the sum of all signing nodes' weights, corresponds to
 *                                    {@link SavedStateMetadataField#SIGNING_WEIGHT_SUM}
 * @param totalWeight                 the total weight of all nodes in the network, corresponds to
 *                                    {@link SavedStateMetadataField#TOTAL_WEIGHT}
 */
public record SavedStateMetadata(
        @Nullable Long round,
        @Nullable Long numberOfConsensusEvents,
        @Nullable Instant consensusTimestamp,
        @Nullable Hash runningEventHash,
        @Nullable Long minimumGenerationNonAncient,
        @Nullable String softwareVersion,
        @Nullable Instant wallClockTime,
        @Nullable NodeId nodeId,
        @Nullable List<NodeId> signingNodes,
        @Nullable Long signingWeightSum,
        @Nullable Long totalWeight) {

    /**
     * The standard file name for the saved state metadata file.
     */
    public static final String FILE_NAME = "stateMetadata.txt";

    /**
     * Use this constant for the node ID if the thing writing the state is not a node.
     */
    public static final NodeId NO_NODE_ID = null;

    private static final Logger logger = LogManager.getLogger(SavedStateMetadata.class);

    /**
     * Parse the saved state metadata from the given file.
     *
     * @param metadataFile the file to parse
     * @return the signed state metadata
     */
    public static SavedStateMetadata parse(final Path metadataFile) {
        final Map<SavedStateMetadataField, String> data = parseStringMap(metadataFile);
        return new SavedStateMetadata(
                parseLong(data, ROUND),
                parseLong(data, NUMBER_OF_CONSENSUS_EVENTS),
                parseInstant(data, CONSENSUS_TIMESTAMP),
                parseHash(data, RUNNING_EVENT_HASH),
                parseLong(data, MINIMUM_GENERATION_NON_ANCIENT),
                parseString(data, SOFTWARE_VERSION),
                parseInstant(data, WALL_CLOCK_TIME),
                parseNodeId(data),
                parseNodeIdList(data, SIGNING_NODES),
                parseLong(data, SIGNING_WEIGHT_SUM),
                parseLong(data, TOTAL_WEIGHT));
    }

    /**
     * Create a new saved state metadata object from the given signed state.
     *
     * @param signedState the signed state
     * @param selfId      the ID of the node that created the signed state
     * @param now         the current time
     * @return the signed state metadata
     */
    public static SavedStateMetadata create(
            @NonNull final SignedState signedState, @Nullable final NodeId selfId, @NonNull final Instant now) {
        Objects.requireNonNull(signedState, "signedState must not be null");
        Objects.requireNonNull(now, "now must not be null");

        final PlatformState platformState = signedState.getState().getPlatformState();
        final PlatformData platformData = platformState.getPlatformData();

        final List<NodeId> signingNodes = signedState.getSigSet().getSigningNodes();
        Collections.sort(signingNodes);

        return new SavedStateMetadata(
                signedState.getRound(),
                platformData.getNumEventsCons(),
                signedState.getConsensusTimestamp(),
                platformData.getHashEventsCons(),
                platformData.getMinimumGenerationNonAncient(),
                convertToString(platformData.getCreationSoftwareVersion()),
                now,
                selfId,
                signingNodes,
                signedState.getSigningWeight(),
                platformState.getAddressBook().getTotalWeight());
    }

    /**
     * Convert an object to a string, throw if the string has newlines.
     *
     * @param value the object to convert
     * @return the string representation of the object
     */
    private static String convertToString(final Object value) {
        final String string = value == null ? "null" : value.toString();

        if (string.contains("\n")) {
            throw new IllegalArgumentException("Value cannot contain newlines: " + value);
        }
        return string;
    }

    /**
     * Parse the key/value pairs written to disk. The inverse of {@link #buildStringMap()}.
     */
    @NonNull
    private static Map<SavedStateMetadataField, String> parseStringMap(final Path metadataFile) {

        if (!Files.exists(metadataFile)) {
            // We must elegantly handle the case where the metadata file does not exist
            // until we have fully migrated all state snapshots in production environments.
            logger.warn(STARTUP.getMarker(), "Signed state does not have a metadata file at {}", metadataFile);
            return new EnumMap<>(SavedStateMetadataField.class);
        }

        try {
            final Map<SavedStateMetadataField, String> map = new EnumMap<>(SavedStateMetadataField.class);

            try (final BufferedReader reader = new BufferedReader(new FileReader(metadataFile.toFile()))) {
                String line;
                while ((line = reader.readLine()) != null) {

                    final int colonIndex = line.indexOf(":");
                    if (colonIndex == -1) {
                        logger.warn(STARTUP.getMarker(), "Invalid line in metadata file: {}", line);
                        continue;
                    }

                    final String keyString = line.substring(0, colonIndex).strip();
                    final String valueString = line.substring(colonIndex + 1).strip();

                    try {
                        final SavedStateMetadataField key = SavedStateMetadataField.valueOf(keyString);
                        map.put(key, valueString);
                    } catch (final IllegalArgumentException e) {
                        logger.warn(STARTUP.getMarker(), "Invalid key in metadata file: {}", keyString, e);
                    }
                }
            }

            return map;
        } catch (final IOException e) {
            logger.warn(STARTUP.getMarker(), "Failed to parse signed state metadata file: {}", metadataFile, e);
            return new EnumMap<>(SavedStateMetadataField.class);
        }
    }

    /**
     * Write a log message for a missing field.
     *
     * @param field the missing field
     */
    private static void logMissingField(final SavedStateMetadataField field) {
        logger.warn(STARTUP.getMarker(), "Signed state metadata file is missing field: {}", field);
    }

    /**
     * Write a log message for an invalid field.
     *
     * @param field the invalid field
     * @param value the invalid value
     * @param e     the exception
     */
    private static void logInvalidField(final SavedStateMetadataField field, final String value, final Exception e) {
        logger.warn(
                STARTUP.getMarker(), "Signed state metadata file has invalid value for field {}: {}", field, value, e);
    }

    /**
     * Attempt to parse a long from the data map.
     *
     * @param data  the data map
     * @param field the field to parse
     * @return the parsed long, or null if the field is not present or the value is not a valid long
     */
    private static Long parseLong(
            final Map<SavedStateMetadataField, String> data, final SavedStateMetadataField field) {

        if (!data.containsKey(field)) {
            logMissingField(field);
            return null;
        }

        final String value = data.get(field);
        try {
            return Long.parseLong(value);
        } catch (final NumberFormatException e) {
            logInvalidField(field, value, e);
            return null;
        }
    }

    /**
     * Attempt to parse a string from the data map.
     *
     * @param data  the data map
     * @param field the field to parse
     * @return the parsed string, or null if the field is not present or the value is not a valid hash
     */
    @SuppressWarnings("SameParameterValue")
    private static String parseString(
            final Map<SavedStateMetadataField, String> data, final SavedStateMetadataField field) {

        if (!data.containsKey(field)) {
            logMissingField(field);
            return null;
        }

        return data.get(field);
    }

    /**
     * Attempt to parse an instant from the data map.
     *
     * @param data  the data map
     * @param field the field to parse
     * @return the parsed instant, or null if the field is not present or the value is not a valid instant
     */
    private static Instant parseInstant(
            final Map<SavedStateMetadataField, String> data, final SavedStateMetadataField field) {

        if (!data.containsKey(field)) {
            logMissingField(field);
            return null;
        }

        final String value = data.get(field);
        try {
            return Instant.parse(value);
        } catch (final DateTimeParseException e) {
            logInvalidField(field, value, e);
            return null;
        }
    }

    /**
     * Attempt to parse a NodeId from the data map.
     *
     * @param data the data map
     * @return the parsed NodeId, or null if the field is not present or the value is not a valid Long
     */
    @Nullable
    private static NodeId parseNodeId(@NonNull final Map<SavedStateMetadataField, String> data) {
        Objects.requireNonNull(data, "data must not be null");
        final Long longValue = parseLong(data, SavedStateMetadataField.NODE_ID);
        if (longValue == null) {
            return null;
        }
        return new NodeId(longValue);
    }

    /**
     * Attempt to parse a list of NodeIds from the data map.
     *
     * @param data  the data map
     * @param field the field to parse
     * @return the parsed list of longs, or null if the field is not present or the value is not a valid list of longs
     */
    @SuppressWarnings("SameParameterValue")
    @Nullable
    private static List<NodeId> parseNodeIdList(
            @NonNull final Map<SavedStateMetadataField, String> data, @NonNull final SavedStateMetadataField field) {

        if (!data.containsKey(field)) {
            logMissingField(field);
            return null;
        }

        final String value = data.get(field);
        final String[] parts = value.split(",");
        final List<NodeId> list = new ArrayList<>();

        if (parts.length == 1 && parts[0].isBlank()) {
            // List is empty.
            return list;
        }

        for (final String part : parts) {
            try {
                list.add(new NodeId(Long.parseLong(part.strip())));
            } catch (final NumberFormatException e) {
                logInvalidField(field, value, e);
                return null;
            }
        }
        return list;
    }

    /**
     * Attempt to parse a hash from the data map.
     *
     * @param data  the data map
     * @param field the field to parse
     * @return the parsed hash, or null if the field is not present or the value is not a valid hash
     */
    @SuppressWarnings("SameParameterValue")
    private static Hash parseHash(
            final Map<SavedStateMetadataField, String> data, final SavedStateMetadataField field) {

        if (!data.containsKey(field)) {
            logMissingField(field);
            return null;
        }

        final String value = data.get(field);
        try {
            return new Hash(unhex(value));
        } catch (final IllegalArgumentException e) {
            logInvalidField(field, value, e);
            return null;
        }
    }

    /**
     * Put a value into the data map if it is not null.
     */
    private static void putIfNotNull(
            final Map<SavedStateMetadataField, String> map, final SavedStateMetadataField field, final Object value) {
        if (value != null) {
            map.put(field, value.toString().replace("\n", "//"));
        }
    }

    /**
     * Build a map of key/value pairs to be written to disk.
     */
    private Map<SavedStateMetadataField, String> buildStringMap() {
        final Map<SavedStateMetadataField, String> map = new EnumMap<>(SavedStateMetadataField.class);

        putIfNotNull(map, ROUND, round);
        putIfNotNull(map, NUMBER_OF_CONSENSUS_EVENTS, numberOfConsensusEvents);
        putIfNotNull(map, CONSENSUS_TIMESTAMP, consensusTimestamp);
        putIfNotNull(map, RUNNING_EVENT_HASH, runningEventHash);
        putIfNotNull(map, MINIMUM_GENERATION_NON_ANCIENT, minimumGenerationNonAncient);
        putIfNotNull(map, SOFTWARE_VERSION, softwareVersion);
        putIfNotNull(map, WALL_CLOCK_TIME, wallClockTime);
        putIfNotNull(map, NODE_ID, nodeId);
        final String signingNodesString = signingNodes == null ? null : formattedList(signingNodes.iterator());
        putIfNotNull(map, SIGNING_NODES, signingNodesString);
        putIfNotNull(map, SIGNING_WEIGHT_SUM, signingWeightSum);
        putIfNotNull(map, TOTAL_WEIGHT, totalWeight);

        return map;
    }

    /**
     * Write the saved state metadata to the given file.
     *
     * @param metadataFile the file to write to
     * @throws IOException if an error occurs while writing
     */
    public void write(final Path metadataFile) throws IOException {

        final Map<SavedStateMetadataField, String> map = buildStringMap();
        final List<SavedStateMetadataField> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);

        final TextTable table = new TextTable().setBordersEnabled(false);

        for (final SavedStateMetadataField key : keys) {
            final String keyString = key.toString() + ": ";
            final String valueString = map.get(key);
            table.addRow(keyString, valueString);
        }

        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(metadataFile.toFile()))) {
            writer.write(table.render());
        }
    }
}
