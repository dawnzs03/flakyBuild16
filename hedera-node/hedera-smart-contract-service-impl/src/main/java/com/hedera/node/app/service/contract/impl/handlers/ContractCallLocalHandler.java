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

package com.hedera.node.app.service.contract.impl.handlers;

import static com.hedera.hapi.node.base.ResponseCodeEnum.INVALID_CONTRACT_ID;
import static com.hedera.node.app.spi.validation.Validations.mustExist;
import static java.util.Objects.requireNonNull;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.hapi.node.base.QueryHeader;
import com.hedera.hapi.node.base.ResponseHeader;
import com.hedera.hapi.node.contract.ContractCallLocalQuery;
import com.hedera.hapi.node.contract.ContractCallLocalResponse;
import com.hedera.hapi.node.transaction.Query;
import com.hedera.hapi.node.transaction.Response;
import com.hedera.node.app.service.contract.impl.exec.QueryComponent;
import com.hedera.node.app.service.contract.impl.exec.QueryComponent.Factory;
import com.hedera.node.app.service.token.ReadableAccountStore;
import com.hedera.node.app.spi.workflows.PaidQueryHandler;
import com.hedera.node.app.spi.workflows.PreCheckException;
import com.hedera.node.app.spi.workflows.QueryContext;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * This class contains all workflow-related functionality regarding {@link HederaFunctionality#CONTRACT_CALL_LOCAL}.
 */
@Singleton
public class ContractCallLocalHandler extends PaidQueryHandler {
    private final Provider<QueryComponent.Factory> provider;

    @Inject
    public ContractCallLocalHandler(@NonNull final Provider<Factory> provider) {
        this.provider = provider;
    }

    @Override
    public QueryHeader extractHeader(@NonNull final Query query) {
        requireNonNull(query);
        return query.contractCallLocalOrThrow().header();
    }

    @Override
    public Response createEmptyResponse(@NonNull final ResponseHeader header) {
        requireNonNull(header);
        final var response = ContractCallLocalResponse.newBuilder().header(header);
        return Response.newBuilder().contractCallLocal(response).build();
    }

    @Override
    public void validate(@NonNull final QueryContext context) throws PreCheckException {
        requireNonNull(context);
        final var query = context.query();
        final ContractCallLocalQuery op = query.contractCallLocalOrThrow();
        final var contractID = op.contractID();
        mustExist(contractID, INVALID_CONTRACT_ID);
        // A contract corresponding to that contract ID must exist in state (otherwise we have nothing to call)
        final var contract = context.createStore(ReadableAccountStore.class).getContractById(contractID);
        mustExist(contract, INVALID_CONTRACT_ID);
        if (contract.deleted()) {
            throw new PreCheckException(INVALID_CONTRACT_ID);
        }
    }

    @Override
    public Response findResponse(@NonNull final QueryContext context, @NonNull final ResponseHeader header) {
        requireNonNull(context);
        requireNonNull(header);

        var component = provider.get().create(context, Instant.now());
        final var outcome = component.contextQueryProcessor().call();

        var response = ContractCallLocalResponse.newBuilder();
        response.header(header);
        response.functionResult(outcome.result());

        return Response.newBuilder().contractCallLocal(response).build();
    }
}
