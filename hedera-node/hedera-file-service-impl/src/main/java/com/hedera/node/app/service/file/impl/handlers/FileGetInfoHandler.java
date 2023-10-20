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

package com.hedera.node.app.service.file.impl.handlers;

import static com.hedera.hapi.node.base.ResponseCodeEnum.INVALID_FILE_ID;
import static com.hedera.hapi.node.base.ResponseCodeEnum.OK;
import static com.hedera.hapi.node.base.ResponseType.COST_ANSWER;
import static com.swirlds.common.utility.CommonUtils.hex;
import static java.util.Objects.requireNonNull;

import com.hedera.hapi.node.base.FileID;
import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.hapi.node.base.QueryHeader;
import com.hedera.hapi.node.base.ResponseHeader;
import com.hedera.hapi.node.base.Timestamp;
import com.hedera.hapi.node.file.FileGetInfoQuery;
import com.hedera.hapi.node.file.FileGetInfoResponse;
import com.hedera.hapi.node.file.FileInfo;
import com.hedera.hapi.node.transaction.Query;
import com.hedera.hapi.node.transaction.Response;
import com.hedera.node.app.service.file.FileMetadata;
import com.hedera.node.app.service.file.ReadableFileStore;
import com.hedera.node.app.service.file.ReadableUpgradeFileStore;
import com.hedera.node.app.service.file.impl.base.FileQueryBase;
import com.hedera.node.app.spi.workflows.PreCheckException;
import com.hedera.node.app.spi.workflows.QueryContext;
import com.hedera.node.config.data.FilesConfig;
import com.hedera.node.config.data.LedgerConfig;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import com.swirlds.common.crypto.CryptographyHolder;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * This class contains all workflow-related functionality regarding {@link HederaFunctionality#FILE_GET_INFO}.
 */
@Singleton
public class FileGetInfoHandler extends FileQueryBase {

    @Inject
    public FileGetInfoHandler() {
        // Exists for injection
    }

    @Override
    public @NonNull QueryHeader extractHeader(@NonNull final Query query) {
        requireNonNull(query);
        return query.fileGetInfoOrThrow().header();
    }

    @Override
    public @NonNull Response createEmptyResponse(@NonNull final ResponseHeader header) {
        requireNonNull(header);
        final var response = FileGetInfoResponse.newBuilder().header(header);
        return Response.newBuilder().fileGetInfo(response).build();
    }

    @Override
    public void validate(@NonNull final QueryContext context) throws PreCheckException {
        final var query = context.query();
        final FileGetInfoQuery op = query.fileGetInfoOrThrow();
        if (!op.hasFileID()) {
            throw new PreCheckException(INVALID_FILE_ID);
        }
    }

    @Override
    public @NonNull Response findResponse(@NonNull final QueryContext context, @NonNull final ResponseHeader header) {
        requireNonNull(header);
        final var query = context.query();
        final var fileStore = context.createStore(ReadableFileStore.class);
        final var upgradeFileStore = context.createStore(ReadableUpgradeFileStore.class);
        final var ledgerConfig = context.configuration().getConfigData(LedgerConfig.class);
        final var fileServiceConfig = context.configuration().getConfigData(FilesConfig.class);
        final var op = query.fileGetInfoOrThrow();
        final var responseBuilder = FileGetInfoResponse.newBuilder();
        final var file = op.fileIDOrThrow();

        final var responseType = op.headerOrElse(QueryHeader.DEFAULT).responseType();
        responseBuilder.header(header);
        if (header.nodeTransactionPrecheckCode() == OK && responseType != COST_ANSWER) {
            final Optional<FileInfo> optionalInfo;
            try {
                optionalInfo = infoForFile(file, fileStore, ledgerConfig, upgradeFileStore, fileServiceConfig);
            } catch (IOException e) {
                throw new RuntimeException("Unable to read file contents", e);
            }

            if (optionalInfo.isEmpty()) {
                responseBuilder.header(header.copyBuilder()
                        .nodeTransactionPrecheckCode(INVALID_FILE_ID)
                        .build());
            } else {
                responseBuilder.fileInfo(optionalInfo.get());
            }
        }

        return Response.newBuilder().fileGetInfo(responseBuilder).build();
    }

    /**
     * Provides information about a file.
     * @param fileID the file to get information about
     * @param fileStore the file store
     * @param ledgerConfig
     * @return the information about the file
     */
    private Optional<FileInfo> infoForFile(
            @NonNull final FileID fileID,
            @NonNull final ReadableFileStore fileStore,
            @NonNull final LedgerConfig ledgerConfig,
            @NonNull final ReadableUpgradeFileStore upgradeFileStore,
            @NonNull final FilesConfig fileServiceConfig)
            throws IOException {

        FileMetadata meta = null;
        long contentSize = 0L;
        // upgrade is for the entire network, not a node. It's across shards and realms, however, which is why we ignore
        // the shard and realm values.
        if (fileID.fileNum() == fileServiceConfig.upgradeFileNumber()) {
            final var file = upgradeFileStore.peek();
            if (file != null) {
                // The "memo" of a special upgrade file is its hexed SHA-384 hash for DevOps convenience
                final var contents = upgradeFileStore.getFull().toByteArray();
                contentSize = contents.length;
                final var upgradeHash =
                        hex(CryptographyHolder.get().digestSync(contents).getValue());
                meta = new FileMetadata(
                        file.fileId(),
                        Timestamp.newBuilder().seconds(file.expirationSecond()).build(),
                        file.keys(),
                        Bytes.EMPTY,
                        upgradeHash,
                        file.deleted());
            }
        } else {
            meta = fileStore.getFileMetadata(fileID);
        }

        if (meta == null) {
            return Optional.empty();
        } else {
            final var info = FileInfo.newBuilder();
            info.memo(meta.memo() == null ? "" : meta.memo());
            info.fileID(fileID);
            info.size((contentSize > 0L ? contentSize : meta.contents().length()));
            info.expirationTime(meta.expirationTimestamp());
            info.deleted(meta.deleted());
            info.keys(meta.keys());
            info.ledgerId(ledgerConfig.id());
            return Optional.of(info.build());
        }
    }
}
