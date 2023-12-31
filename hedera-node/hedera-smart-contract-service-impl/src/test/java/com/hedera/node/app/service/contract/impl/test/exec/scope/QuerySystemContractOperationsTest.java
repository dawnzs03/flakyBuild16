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

package com.hedera.node.app.service.contract.impl.test.exec.scope;

import static com.hedera.node.app.service.contract.impl.test.TestHelpers.MOCK_VERIFICATION_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hedera.hapi.node.base.NftID;
import com.hedera.hapi.node.base.TokenRelationship;
import com.hedera.hapi.node.contract.ContractFunctionResult;
import com.hedera.hapi.node.state.token.Account;
import com.hedera.hapi.node.state.token.Nft;
import com.hedera.hapi.node.state.token.Token;
import com.hedera.hapi.node.transaction.TransactionBody;
import com.hedera.node.app.service.contract.impl.exec.scope.QuerySystemContractOperations;
import com.hedera.node.app.service.contract.impl.exec.scope.ResultTranslator;
import com.hedera.node.app.service.contract.impl.utils.SystemContractUtils.ResultStatus;
import com.hedera.node.app.spi.workflows.QueryContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuerySystemContractOperationsTest {
    @Mock
    private QueryContext context;

    @Mock
    private NftID nftID;

    @Mock
    private ResultTranslator<Nft> nftResultTranslator;

    @Mock
    private ResultTranslator<Token> tokenResultTranslator;

    @Mock
    private ResultTranslator<Account> accountResultTranslator;

    @Mock
    private ResultTranslator<TokenRelationship> tokenRelResultTranslator;

    private QuerySystemContractOperations subject;

    @BeforeEach
    void setUp() {
        subject = new QuerySystemContractOperations();
    }

    @Test
    void doesNotSupportAnyMutations() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> subject.getNftAndExternalizeResult(nftID, 1L, nftResultTranslator));
        assertThrows(
                UnsupportedOperationException.class,
                () -> subject.getTokenAndExternalizeResult(1L, 2L, tokenResultTranslator));
        assertThrows(
                UnsupportedOperationException.class,
                () -> subject.getAccountAndExternalizeResult(1L, 2L, accountResultTranslator));
        assertThrows(
                UnsupportedOperationException.class,
                () -> subject.getRelationshipAndExternalizeResult(1L, 2L, 3L, tokenRelResultTranslator));
        assertThrows(
                UnsupportedOperationException.class,
                () -> subject.dispatch(TransactionBody.DEFAULT, MOCK_VERIFICATION_STRATEGY));
        assertThrows(
                UnsupportedOperationException.class,
                () -> subject.externalizeResult(ContractFunctionResult.DEFAULT, ResultStatus.IS_SUCCESS));
    }
}
