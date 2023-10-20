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

package com.hedera.node.app.service.contract.impl.test.exec;

import static com.hedera.hapi.node.base.ResponseCodeEnum.INVALID_CONTRACT_ID;
import static com.hedera.hapi.node.base.ResponseCodeEnum.SUCCESS;
import static com.hedera.node.app.service.contract.impl.test.TestHelpers.CALLED_CONTRACT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;

import com.hedera.node.app.service.contract.impl.exec.CallOutcome;
import com.hedera.node.app.service.contract.impl.state.RootProxyWorldUpdater;
import com.hedera.node.app.service.contract.impl.test.TestHelpers;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CallOutcomeTest {
    @Mock
    private RootProxyWorldUpdater updater;

    @Test
    void recognizesCreatedIdWhenEvmAddressIsSet() {
        given(updater.getCreatedContractIds()).willReturn(List.of(CALLED_CONTRACT_ID));
        final var outcome = new CallOutcome(TestHelpers.SUCCESS_RESULT.asProtoResultOf(updater), SUCCESS);
        assertEquals(CALLED_CONTRACT_ID, outcome.recipientIdIfCreated());
    }

    @Test
    void recognizesNoCreatedIdWhenEvmAddressNotSet() {
        final var outcome = new CallOutcome(TestHelpers.SUCCESS_RESULT.asProtoResultOf(updater), SUCCESS);
        assertNull(outcome.recipientIdIfCreated());
    }

    @Test
    void calledIdIsNullIfNoResult() {
        final var outcome = new CallOutcome(null, INVALID_CONTRACT_ID);
        assertNull(outcome.recipientIdIfCalled());
    }

    @Test
    void calledIdIsFromResultIfExtant() {
        final var outcome = new CallOutcome(TestHelpers.SUCCESS_RESULT.asProtoResultOf(updater), INVALID_CONTRACT_ID);
        assertEquals(CALLED_CONTRACT_ID, outcome.recipientIdIfCalled());
    }
}
