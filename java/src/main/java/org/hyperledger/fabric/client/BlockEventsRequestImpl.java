/*
 * Copyright 2022 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.client;

import io.grpc.CallOptions;
import java.util.NoSuchElementException;
import java.util.function.UnaryOperator;
import org.hyperledger.fabric.protos.common.Block;
import org.hyperledger.fabric.protos.common.Envelope;
import org.hyperledger.fabric.protos.peer.DeliverResponse;

final class BlockEventsRequestImpl extends SignableBlockEventsRequest implements BlockEventsRequest {
    private final GatewayClient client;

    BlockEventsRequestImpl(final GatewayClient client, final SigningIdentity signingIdentity, final Envelope request) {
        super(signingIdentity, request);
        this.client = client;
    }

    @Override
    public CloseableIterator<Block> getEvents(final UnaryOperator<CallOptions> options) {
        Envelope request = getSignedRequest();
        CloseableIterator<DeliverResponse> responseIter = client.blockEvents(request, options);

        return new MappingCloseableIterator<>(responseIter, response -> {
            DeliverResponse.TypeCase responseType = response.getTypeCase();
            if (responseType == DeliverResponse.TypeCase.STATUS) {
                throw new NoSuchElementException("Unexpected status response: " + response.getStatus());
            }

            return response.getBlock();
        });
    }
}
