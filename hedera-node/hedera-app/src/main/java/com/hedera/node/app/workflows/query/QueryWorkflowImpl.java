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

package com.hedera.node.app.workflows.query;

import static com.hedera.hapi.node.base.HederaFunctionality.GET_ACCOUNT_DETAILS;
import static com.hedera.hapi.node.base.HederaFunctionality.NETWORK_GET_EXECUTION_TIME;
import static com.hedera.hapi.node.base.ResponseCodeEnum.BUSY;
import static com.hedera.hapi.node.base.ResponseCodeEnum.NOT_SUPPORTED;
import static com.hedera.hapi.node.base.ResponseCodeEnum.OK;
import static com.hedera.hapi.node.base.ResponseType.ANSWER_STATE_PROOF;
import static com.hedera.hapi.node.base.ResponseType.COST_ANSWER_STATE_PROOF;
import static java.util.Objects.requireNonNull;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.hapi.node.base.ResponseCodeEnum;
import com.hedera.hapi.node.base.ResponseHeader;
import com.hedera.hapi.node.base.ResponseType;
import com.hedera.hapi.node.base.Transaction;
import com.hedera.hapi.node.transaction.Query;
import com.hedera.hapi.node.transaction.Response;
import com.hedera.hapi.node.transaction.TransactionBody;
import com.hedera.node.app.service.mono.pbj.PbjConverter;
import com.hedera.node.app.spi.HapiUtils;
import com.hedera.node.app.spi.UnknownHederaFunctionality;
import com.hedera.node.app.spi.records.RecordCache;
import com.hedera.node.app.spi.workflows.InsufficientBalanceException;
import com.hedera.node.app.spi.workflows.PreCheckException;
import com.hedera.node.app.spi.workflows.QueryContext;
import com.hedera.node.app.state.HederaState;
import com.hedera.node.app.throttle.ThrottleAccumulator;
import com.hedera.node.app.workflows.dispatcher.ReadableStoreFactory;
import com.hedera.node.app.workflows.ingest.IngestChecker;
import com.hedera.node.app.workflows.ingest.SubmissionManager;
import com.hedera.node.config.ConfigProvider;
import com.hedera.pbj.runtime.Codec;
import com.hedera.pbj.runtime.MalformedProtobufException;
import com.hedera.pbj.runtime.UnknownFieldException;
import com.hedera.pbj.runtime.io.buffer.BufferedData;
import com.hedera.pbj.runtime.io.buffer.Bytes;
import com.swirlds.common.utility.AutoCloseableWrapper;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;
import javax.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Implementation of {@link QueryWorkflow} */
public final class QueryWorkflowImpl implements QueryWorkflow {

    private static final Logger logger = LogManager.getLogger(QueryWorkflowImpl.class);

    private static final EnumSet<ResponseType> UNSUPPORTED_RESPONSE_TYPES =
            EnumSet.of(ANSWER_STATE_PROOF, COST_ANSWER_STATE_PROOF);
    private static final List<HederaFunctionality> RESTRICTED_FUNCTIONALITIES =
            List.of(NETWORK_GET_EXECUTION_TIME, GET_ACCOUNT_DETAILS);

    private final Function<ResponseType, AutoCloseableWrapper<HederaState>> stateAccessor;
    private final ThrottleAccumulator throttleAccumulator;
    private final SubmissionManager submissionManager;
    private final QueryChecker queryChecker;
    private final IngestChecker ingestChecker;
    private final QueryDispatcher dispatcher;

    private final Codec<Query> queryParser;
    private final ConfigProvider configProvider;
    private final RecordCache recordCache;

    /**
     * Constructor of {@code QueryWorkflowImpl}
     *
     * @param stateAccessor a {@link Function} that returns the latest immutable or latest signed
     *     state depending on the {@link ResponseType}
     * @param throttleAccumulator the {@link ThrottleAccumulator} for throttling
     * @param submissionManager the {@link SubmissionManager} to submit transactions to the platform
     * @param queryChecker the {@link QueryChecker} with specific checks of an ingest-workflow
     * @param ingestChecker the {@link IngestChecker} to handle the crypto transfer
     * @param dispatcher the {@link QueryDispatcher} that will call query-specific methods
     * @throws NullPointerException if one of the arguments is {@code null}
     */
    @Inject
    public QueryWorkflowImpl(
            @NonNull final Function<ResponseType, AutoCloseableWrapper<HederaState>> stateAccessor,
            @NonNull final ThrottleAccumulator throttleAccumulator,
            @NonNull final SubmissionManager submissionManager,
            @NonNull final QueryChecker queryChecker,
            @NonNull final IngestChecker ingestChecker,
            @NonNull final QueryDispatcher dispatcher,
            @NonNull final Codec<Query> queryParser,
            @NonNull final ConfigProvider configProvider,
            @NonNull final RecordCache recordCache) {
        this.stateAccessor = requireNonNull(stateAccessor);
        this.throttleAccumulator = requireNonNull(throttleAccumulator);
        this.submissionManager = requireNonNull(submissionManager);
        this.ingestChecker = requireNonNull(ingestChecker);
        this.queryChecker = requireNonNull(queryChecker);
        this.dispatcher = requireNonNull(dispatcher);
        this.queryParser = requireNonNull(queryParser);
        this.configProvider = requireNonNull(configProvider);
        this.recordCache = requireNonNull(recordCache);
    }

    @Override
    public void handleQuery(@NonNull final Bytes requestBuffer, @NonNull final BufferedData responseBuffer) {
        requireNonNull(requestBuffer);
        requireNonNull(responseBuffer);

        // 1. Parse and check header
        final Query query = parseQuery(requestBuffer);
        logger.debug("Received query: {}", query);

        final var function = functionOf(query);

        final var handler = dispatcher.getHandler(query);
        final var queryHeader = handler.extractHeader(query);
        if (queryHeader == null) {
            throw new StatusRuntimeException(Status.INVALID_ARGUMENT);
        }
        final ResponseType responseType = queryHeader.responseType();
        logger.debug("Started answering a {} query of type {}", function, responseType);

        Response response;
        long fee = 0L;
        try (final var wrappedState = stateAccessor.apply(responseType)) {
            // 2. Do some general pre-checks
            ingestChecker.checkNodeState();
            if (UNSUPPORTED_RESPONSE_TYPES.contains(responseType)) {
                throw new PreCheckException(NOT_SUPPORTED);
            }

            // 3. Check query throttles
            if (throttleAccumulator.shouldThrottleQuery(function, query)) {
                throw new PreCheckException(BUSY);
            }

            final var state = wrappedState.get();
            final var storeFactory = new ReadableStoreFactory(state);
            final var paymentRequired = handler.requiresNodePayment(responseType);
            final QueryContext context;
            Transaction allegedPayment = null;
            TransactionBody txBody = null;
            if (paymentRequired) {
                allegedPayment = queryHeader.paymentOrThrow();

                // 4.i Ingest checks
                final var transactionInfo = ingestChecker.runAllChecks(state, allegedPayment);

                // 4.ii Validate CryptoTransfer
                queryChecker.validateCryptoTransfer(transactionInfo);

                txBody = transactionInfo.txBody();
                final var payer = txBody.transactionIDOrThrow().accountIDOrThrow();
                context = new QueryContextImpl(
                        state, storeFactory, query, configProvider.getConfiguration(), recordCache, payer);

                // 4.iii Check permissions
                queryChecker.checkPermissions(payer, function);

                // 4.iv Calculate costs
                fee = handler.computeFees(context).totalFee();

                // 4.v Check account balances
                queryChecker.validateAccountBalances(payer, transactionInfo, fee);

                // 4.vi Submit payment to platform
                final var txBytes = PbjConverter.asWrappedBytes(Transaction.PROTOBUF, allegedPayment);
                submissionManager.submit(txBody, txBytes);
            } else {
                if (RESTRICTED_FUNCTIONALITIES.contains(function)) {
                    throw new PreCheckException(NOT_SUPPORTED);
                }
                context = new QueryContextImpl(
                        state, storeFactory, query, configProvider.getConfiguration(), recordCache, null);
            }

            // 5. Check validity of query
            handler.validate(context);

            if (handler.needsAnswerOnlyCost(responseType)) {
                // 6.i Estimate costs
                fee = handler.computeFees(context).totalFee();

                final var header = createResponseHeader(responseType, OK, fee);
                response = handler.createEmptyResponse(header);
            } else {
                // 6.ii Find response
                final var header = createResponseHeader(responseType, OK, fee);
                response = handler.findResponse(context, header);
            }
        } catch (InsufficientBalanceException e) {
            final var header = createResponseHeader(responseType, e.responseCode(), e.getEstimatedFee());
            response = handler.createEmptyResponse(header);
        } catch (PreCheckException e) {
            final var header = createResponseHeader(responseType, e.responseCode(), fee);
            response = handler.createEmptyResponse(header);
        }

        try {
            Response.PROTOBUF.write(response, responseBuffer);
            logger.debug("Finished handling a query request in Query workflow");
        } catch (IOException e) {
            logger.warn("Unexpected IO exception while writing protobuf", e);
            throw new StatusRuntimeException(Status.INTERNAL);
        }
    }

    private Query parseQuery(Bytes requestBuffer) {
        try {
            return queryParser.parseStrict(requestBuffer.toReadableSequentialData());
        } catch (MalformedProtobufException | UnknownFieldException e) {
            throw new StatusRuntimeException(Status.INVALID_ARGUMENT);
        } catch (IOException e) {
            // This should technically not be possible. The data buffer supplied
            // is either based on a byte[] or a byte buffer, in both cases all data
            // is available and a generic IO exception shouldn't happen. If it does,
            // it indicates the data could not be parsed, but for a reason other than
            // those causing an MalformedProtobufException or UnknownFieldException.
            logger.warn("Unexpected IO exception while parsing protobuf", e);
            throw new StatusRuntimeException(Status.INVALID_ARGUMENT);
        }
    }

    private static ResponseHeader createResponseHeader(
            @NonNull final ResponseType type, @NonNull final ResponseCodeEnum responseCode, final long fee) {
        return ResponseHeader.newBuilder()
                .responseType(type)
                .nodeTransactionPrecheckCode(responseCode)
                .cost(fee)
                .build();
    }

    private static HederaFunctionality functionOf(@NonNull final Query query) {
        try {
            return HapiUtils.functionOf(query);
        } catch (UnknownHederaFunctionality e) {
            throw new StatusRuntimeException(Status.INVALID_ARGUMENT);
        }
    }
}
