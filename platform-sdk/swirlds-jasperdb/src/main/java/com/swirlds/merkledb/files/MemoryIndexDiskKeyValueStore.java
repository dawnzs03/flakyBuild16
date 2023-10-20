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

package com.swirlds.merkledb.files;

import static com.swirlds.logging.LogMarker.EXCEPTION;
import static com.swirlds.logging.LogMarker.MERKLE_DB;
import static com.swirlds.merkledb.files.DataFileCommon.dataLocationToString;
import static com.swirlds.merkledb.files.DataFileCommon.fileIndexFromDataLocation;
import static com.swirlds.merkledb.files.DataFileCommon.formatSizeBytes;
import static com.swirlds.merkledb.files.DataFileCommon.getSizeOfFiles;
import static com.swirlds.merkledb.files.DataFileCommon.getSizeOfFilesByPath;
import static com.swirlds.merkledb.files.DataFileCommon.logMergeStats;
import static com.swirlds.merkledb.files.DataFileCommon.printDataLinkValidation;

import com.swirlds.common.units.UnitConstants;
import com.swirlds.merkledb.KeyRange;
import com.swirlds.merkledb.Snapshotable;
import com.swirlds.merkledb.collections.CASableLongIndex;
import com.swirlds.merkledb.collections.LongList;
import com.swirlds.merkledb.files.DataFileCollection.LoadedDataCallback;
import com.swirlds.merkledb.serialize.DataItemSerializer;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.LongConsumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.collections.impl.map.mutable.primitive.LongLongHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;

/**
 * A specialized map like disk based data store with long keys. It is assumed the keys are a single
 * sequential block of numbers that does not need to start at zero. The index from long key to disk
 * location for value is in RAM and the value data is stored in a set of files on disk.
 * <p>
 * There is an assumption that keys are a contiguous range of incrementing numbers. This allows
 * easy deletion during merging by accepting any key/value with a key outside this range is not
 * needed any more. This design comes from being used where keys are leaf paths in a binary tree.
 *
 * @param <D> type for data items
 */
@SuppressWarnings({"DuplicatedCode"})
public class MemoryIndexDiskKeyValueStore<D> implements AutoCloseable, Snapshotable {
    private static final Logger logger = LogManager.getLogger(MemoryIndexDiskKeyValueStore.class);

    /** This is useful for debugging and validating but is too expensive to enable in production. */
    protected static boolean enableDeepValidation = logger.isTraceEnabled();
    /**
     * Index mapping, it uses our key as the index within the list and the value is the dataLocation
     * in fileCollection where the key/value pair is stored.
     */
    private final LongList index;
    /** On disk set of DataFiles that contain our key/value pairs */
    private final DataFileCollection<D> fileCollection;
    /**
     * The name for the data store, this allows more than one data store in a single directory.
     * Also, useful for identifying what files are used by what part of the code.
     */
    private final String storeName;

    /** The minimum key that is valid for this store. Set in {@link MemoryIndexDiskKeyValueStore#startWriting} to
     * be reused in {@link MemoryIndexDiskKeyValueStore#endWriting()}
     */
    private final AtomicLong minValidKey;
    /** The maximum key that is valid for this store.  Set in {@link MemoryIndexDiskKeyValueStore#startWriting} to
     * be reused in {@link MemoryIndexDiskKeyValueStore#endWriting()}
     */
    private final AtomicLong maxValidKey;

    /**
     * Construct a new MemoryIndexDiskKeyValueStore
     *
     * @param storeDir The directory to store data files in
     * @param storeName The name for the data store, this allows more than one data store in a
     *     single directory.
     * @param legacyStoreName Base name for the data store. If not null, the store will process
     *     files with this prefix at startup. New files in the store will be prefixed with {@code
     *     storeName}
     * @param dataItemSerializer Serializer for converting raw data to/from data items
     * @param loadedDataCallback call back for handing loaded data from existing files on startup.
     *     Can be null if not needed.
     * @param keyToDiskLocationIndex The index to use for keys to disk locations. Having this passed
     *     in allows multiple MemoryIndexDiskKeyValueStore stores to share the same index if there
     *     key ranges do not overlap. For example with internal node and leaf paths in a virtual map
     *     tree. It also lets the caller decide the LongList implementation to use. This does mean
     *     the caller is responsible for snapshot of the index.
     * @throws IOException If there was a problem opening data files
     */
    public MemoryIndexDiskKeyValueStore(
            final Path storeDir,
            final String storeName,
            final String legacyStoreName,
            final DataItemSerializer<D> dataItemSerializer,
            final LoadedDataCallback loadedDataCallback,
            final LongList keyToDiskLocationIndex)
            throws IOException {
        this.storeName = storeName;
        index = keyToDiskLocationIndex;
        final boolean indexIsEmpty = keyToDiskLocationIndex.size() == 0;
        // create store dir
        Files.createDirectories(storeDir);
        // rebuild index as well as calling user's loadedDataCallback if needed
        final LoadedDataCallback combinedLoadedDataCallback;
        if (!indexIsEmpty && loadedDataCallback == null) {
            combinedLoadedDataCallback = null;
        } else {
            combinedLoadedDataCallback = (key, dataLocation, dataValue) -> {
                if (indexIsEmpty) {
                    index.put(key, dataLocation);
                }
                if (loadedDataCallback != null) {
                    loadedDataCallback.newIndexEntry(key, dataLocation, dataValue);
                }
            };
        }
        // create file collection
        fileCollection = new DataFileCollection<>(
                storeDir, storeName, legacyStoreName, dataItemSerializer, combinedLoadedDataCallback);
        // no limits for the keys on init
        minValidKey = new AtomicLong(0);
        maxValidKey = new AtomicLong(Long.MAX_VALUE);
    }

    /**
     * Compact (merge) all files that match the given filter.
     *
     * @param filterForFilesToMerge filter to choose which subset of files to merge
     * @param minNumberOfFilesToMerge the minimum number of files to consider for a merge
     * @param reportDurationMetricFunction function to report how long compaction took, in ms
     * @param reportSavedSpaceMetricFunction function to report how much space was compacted, in Mb
     * @throws IOException if there was a problem merging
     * @throws InterruptedException if the merge thread was interupted
     */
    public void merge(
            final Function<List<DataFileReader<D>>, List<DataFileReader<D>>> filterForFilesToMerge,
            final int minNumberOfFilesToMerge,
            @Nullable final LongConsumer reportDurationMetricFunction,
            @Nullable final DoubleConsumer reportSavedSpaceMetricFunction)
            throws IOException, InterruptedException {
        final List<DataFileReader<D>> allMergeableFiles = fileCollection.getAllCompletedFiles();
        final List<DataFileReader<D>> filesToMerge = filterForFilesToMerge.apply(allMergeableFiles);
        if (filesToMerge == null) {
            // nothing to do
            return;
        }
        final int filesCount = filesToMerge.size();
        if (filesCount < minNumberOfFilesToMerge) {
            logger.debug(
                    MERKLE_DB.getMarker(),
                    "[{}] No need to merge as {} is less than the minimum {} files to merge.",
                    storeName,
                    filesCount,
                    minNumberOfFilesToMerge);
            return;
        }

        final long start = System.currentTimeMillis();

        final long filesToMergeSize = getSizeOfFiles(filesToMerge);
        logger.debug(
                MERKLE_DB.getMarker(),
                "[{}] Starting merging {} files / {}",
                storeName,
                filesCount,
                formatSizeBytes(filesToMergeSize));

        CASableLongIndex indexUpdater = index;
        if (enableDeepValidation) {
            startChecking();
            indexUpdater = new CASableLongIndex() {
                @Override
                public long get(final long key) {
                    return index.get(key);
                }

                @Override
                public boolean putIfEqual(final long key, final long oldValue, final long newValue) {
                    final boolean casSuccessful = index.putIfEqual(key, oldValue, newValue);
                    checkItem(casSuccessful, key, oldValue, newValue);
                    return casSuccessful;
                }

                @Override
                @SuppressWarnings({"rawtypes", "unchecked"})
                public void forEach(final LongAction action) throws InterruptedException {
                    index.forEach(action);
                }
            };
        }
        final List<Path> newFilesCreated = fileCollection.compactFiles(indexUpdater, filesToMerge);
        if (enableDeepValidation) {
            endChecking(filesToMerge);
        }

        final long end = System.currentTimeMillis();
        final long tookMillis = end - start;
        if (reportDurationMetricFunction != null) {
            reportDurationMetricFunction.accept(tookMillis);
        }

        final long mergedFilesSize = getSizeOfFilesByPath(newFilesCreated);
        if (reportSavedSpaceMetricFunction != null) {
            reportSavedSpaceMetricFunction.accept(
                    (filesToMergeSize - mergedFilesSize) * UnitConstants.BYTES_TO_MEBIBYTES);
        }

        logMergeStats(storeName, tookMillis, filesToMerge, filesToMergeSize, newFilesCreated, fileCollection);
        logger.debug(
                MERKLE_DB.getMarker(),
                "[{}] Finished merging {} files / {} in {} ms",
                storeName,
                filesCount,
                formatSizeBytes(filesToMergeSize),
                tookMillis);
    }

    /**
     * Puts this store compaction on hold, if in progress, until {@link #resumeMerging()} is called.
     * If compaction is not in progress, calling this method will prevent new compactions from
     * starting until resumed.
     *
     * @throws IOException If an I/O error occurs.
     */
    public void pauseMerging() throws IOException {
        fileCollection.pauseCompaction();
    }

    /**
     * Resumes this store compaction if it was in progress, or unblocks a new compaction if it was
     * blocked to start because of {@link #pauseMerging()}.
     *
     * @throws IOException If an I/O error occurs.
     */
    public void resumeMerging() throws IOException {
        fileCollection.resumeCompaction();
    }

    /**
     * Start a writing session ready for calls to put()
     *
     * @throws IOException If there was a problem opening a writing session
     */
    public void startWriting(final long minimumValidKey, final long maxValidIndex) throws IOException {
        this.minValidKey.set(minimumValidKey);
        this.maxValidKey.set(maxValidIndex);
        // By calling `updateMinValidIndex` we compact the index if it's applicable.
        // We need to do this before we start putting values into the index, otherwise we could put a value by
        // index that is not yet valid.
        index.updateValidRange(minimumValidKey, maxValidIndex);
        fileCollection.startWriting();
    }

    /**
     * Put a value into this store, you must be in a writing session started with startWriting()
     *
     * @param key The key to store value for
     * @param dataItem Buffer containing the data's value, it should have its position and limit set
     *     correctly
     * @throws IOException If there was a problem write key/value to the store
     */
    public void put(final long key, final D dataItem) throws IOException {
        final long dataLocation = fileCollection.storeDataItem(dataItem);
        // store data location in index
        index.put(key, dataLocation);
    }

    /**
     * End a session of writing
     *
     * @return Data file reader for the file written
     * @throws IOException If there was a problem closing the writing session
     */
    @Nullable
    public DataFileReader<D> endWriting() throws IOException {
        final long currentMinValidKey = minValidKey.get();
        final long currentMaxValidKey = maxValidKey.get();
        final DataFileReader<D> dataFileReader = fileCollection.endWriting(currentMinValidKey, currentMaxValidKey);
        dataFileReader.setFileCompleted();
        logger.info(
                MERKLE_DB.getMarker(),
                "{} Ended writing, newFile={}, numOfFiles={}, minimumValidKey={}, maximumValidKey={}",
                storeName,
                dataFileReader.getIndex(),
                fileCollection.getNumOfFiles(),
                currentMinValidKey,
                currentMaxValidKey);
        return dataFileReader;
    }

    /**
     * Get a value by reading it from disk.
     *
     * @param key The key to find and read value for
     * @return Array of serialization version for data if the value was read or null if not found
     * @throws IOException If there was a problem reading the value from file
     */
    public D get(final long key) throws IOException {
        // Check if out of range
        final KeyRange keyRange = fileCollection.getValidKeyRange();
        if (!keyRange.withinRange(key)) {
            // Key 0 is the root node and always supported, but if it doesn't exist, just return
            // null,
            // even when no data has yet been written.
            if (key != 0) {
                logger.trace(MERKLE_DB.getMarker(), "Key [{}] is outside valid key range of {}", key, keyRange);
            }
            return null;
        }
        // read from files via index lookup
        return fileCollection.readDataItemUsingIndex(index, key);
    }

    /**
     * Close all files being used
     *
     * @throws IOException If there was a problem closing files
     */
    @Override
    public void close() throws IOException {
        fileCollection.close();
    }

    /** {@inheritDoc} */
    @Override
    public void snapshot(final Path snapshotDirectory) throws IOException {
        fileCollection.snapshot(snapshotDirectory);
    }

    /**
     * Get statistics for sizes of all files
     *
     * @return statistics for sizes of all fully written files, in bytes
     */
    public LongSummaryStatistics getFilesSizeStatistics() {
        return fileCollection.getAllCompletedFilesSizeStatistics();
    }

    // =================================================================================================================
    // Debugging Tools, these can be enabled with the ENABLE_DEEP_VALIDATION flag above

    /** Debugging store of compare and swap operations. Enabled in trace level logging only. */
    private LongObjectHashMap<CasRecord> casRecords;
    /** Debugging store of how many keys were checked */
    private LongLongHashMap keyCount;

    /** Start collecting data for debugging integrity checking */
    private void startChecking() {
        casRecords = new LongObjectHashMap<>();
        keyCount = new LongLongHashMap();
    }

    /** Check an item for debugging integrity checking */
    private void checkItem(final boolean casSuccessful, final long key, final long oldValue, final long newValue) {
        casRecords.put(key, new CasRecord(casSuccessful, oldValue, newValue, index.get(key, 0)));
    }

    /** End debugging integrity checking and print results */
    private void endChecking(final List<DataFileReader<D>> filesToMerge) {
        // set of merged files
        final SortedSet<Integer> mergedFileIds = new TreeSet<>();
        for (final DataFileReader<D> file : filesToMerge) {
            mergedFileIds.add(file.getMetadata().getIndex());
        }

        final KeyRange validKeyRange = fileCollection.getValidKeyRange();
        final long currentMinValidKey = validKeyRange.getMinValidKey();
        final long currentMaxValidKey = validKeyRange.getMaxValidKey();

        for (long key = currentMinValidKey; key <= currentMaxValidKey; key++) {
            final long location = index.get(key, -1);
            if (mergedFileIds.contains(fileIndexFromDataLocation(location))) { // only entries for deleted files
                // If we enter this "if", it means we have a corrupt index. Either we should have
                // updated the
                // index but didn't, or shouldn't have updated it but did, or we didn't even try to
                // update it
                // when we should have updated it.
                final CasRecord miss = casRecords.get(key);
                if (miss == null) {
                    // We found a value in the index that refers to a file we've merged and deleted,
                    // but was never
                    // in the moves list. We never attempted to update the index, when we should
                    // have!
                    logger.trace(
                            MERKLE_DB.getMarker(),
                            "MISSING CAS RECORD for current = {}",
                            dataLocationToString(location));
                } else {
                    logger.trace(
                            MERKLE_DB.getMarker(),
                            "CAS {} " + "key = {}, value = {}, from = {}, to = {}, current = {}",
                            (miss.missed ? "miss" : "hit"),
                            key,
                            dataLocationToString(location),
                            dataLocationToString(miss.fileMovingFrom),
                            dataLocationToString(miss.fileMovingTo),
                            dataLocationToString(miss.currentLocation));
                }
            }
        }
        keyCount.forEachKeyValue((key, count) -> {
            if (count > 1) {
                logger.trace(EXCEPTION.getMarker(), "Key [{}] has invalid count {}", key, count);
            }
        });
        printDataLinkValidation(
                storeName,
                index,
                fileCollection.getSetOfNewFileIndexes(),
                fileCollection.getAllCompletedFiles(),
                validKeyRange);
    }

    /** POJO for storing a compare and swap operations */
    private static class CasRecord {
        private final boolean missed;
        private final long fileMovingFrom;
        private final long fileMovingTo;
        private final long currentLocation;

        public CasRecord(
                final boolean missed, final long fileMovingFrom, final long fileMovingTo, final long currentLocation) {
            this.missed = missed;
            this.fileMovingFrom = fileMovingFrom;
            this.fileMovingTo = fileMovingTo;
            this.currentLocation = currentLocation;
        }
    }
}
