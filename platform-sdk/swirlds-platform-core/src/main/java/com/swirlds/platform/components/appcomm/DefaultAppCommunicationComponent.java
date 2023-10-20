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

package com.swirlds.platform.components.appcomm;

import com.swirlds.common.notification.NotificationEngine;
import com.swirlds.common.notification.listeners.StateWriteToDiskCompleteListener;
import com.swirlds.common.notification.listeners.StateWriteToDiskCompleteNotification;
import com.swirlds.common.system.NodeId;
import com.swirlds.common.system.state.notifications.IssListener;
import com.swirlds.common.system.state.notifications.IssNotification;
import com.swirlds.common.system.state.notifications.NewSignedStateListener;
import com.swirlds.common.system.state.notifications.NewSignedStateNotification;
import com.swirlds.platform.state.signed.ReservedSignedState;
import com.swirlds.platform.state.signed.SignedState;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.nio.file.Path;

/**
 * Default implementation of the {@link AppCommunicationComponent}
 */
public class DefaultAppCommunicationComponent implements AppCommunicationComponent {

    private final NotificationEngine notificationEngine;

    public DefaultAppCommunicationComponent(final NotificationEngine notificationEngine) {
        this.notificationEngine = notificationEngine;
    }

    @Override
    public void stateToDiskAttempt(
            @NonNull final SignedState signedState, @NonNull final Path directory, final boolean success) {
        if (success) {
            // Synchronous notification, no need to take an extra reservation
            notificationEngine.dispatch(
                    StateWriteToDiskCompleteListener.class,
                    new StateWriteToDiskCompleteNotification(
                            signedState.getRound(),
                            signedState.getConsensusTimestamp(),
                            signedState.getSwirldState(),
                            directory,
                            signedState.isFreezeState()));
        }
    }

    @Override
    public void newLatestCompleteStateEvent(@NonNull final SignedState signedState) {
        final ReservedSignedState reservedSignedState =
                signedState.reserve("DefaultAppCommunicationComponent.newLatestCompleteStateEvent()");

        final NewSignedStateNotification notification = new NewSignedStateNotification(
                signedState.getSwirldState(),
                signedState.getState().getSwirldDualState(),
                signedState.getRound(),
                signedState.getConsensusTimestamp());

        notificationEngine.dispatch(NewSignedStateListener.class, notification, r -> reservedSignedState.close());
    }

    @Override
    public void iss(
            final long round, @NonNull final IssNotification.IssType issType, @Nullable final NodeId otherNodeId) {
        final IssNotification notification = new IssNotification(round, issType, otherNodeId);
        notificationEngine.dispatch(IssListener.class, notification);
    }
}
