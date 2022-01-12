/*
 * Copyright 2021 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { ServiceError } from '@grpc/grpc-js';
import { ErrorDetail as ErrorDetailProto } from './protos/gateway/gateway_pb';
import { Status } from './protos/google/rpc/status_pb';

/**
 * ErrorDetail contains the details of an error generated by an endorsing peer or ordering node.
 * It contains the address of the node as well as the error message it generated.
 */
export interface ErrorDetail {
    /**
     * Fabric node endpoint address.
     */
    address: string;
    /**
     * Error message returned by the node.
     */
    message: string;
    /**
     * Member services provider to which the node is associated.
     */
    mspId: string;
}

/**
 * A GatewayError is thrown if an error is encountered while processing a transaction through the gateway.
 * Since the gateway delegates much of the processing to other nodes (endorsing peers and orderers), then
 * the error could have originated from one or more of those nodes.  In that case, the details field will
 * contain an array of ErrorDetail objects.
 */
export class GatewayError extends Error {
    /**
     * gRPC status code.
     * @see {@link https://grpc.github.io/grpc/core/md_doc_statuscodes.html} for descriptions of status codes.
     */
    code: number;

    /**
     * gRPC error details.
     */
    details: ErrorDetail[];

    /**
     * Raw underlying gRPC error.
     */
    cause: ServiceError;

    constructor(properties: Readonly<Omit<GatewayError, keyof Error> & Partial<Pick<Error, 'message'>>>) {
        super(properties.message);

        this.name = GatewayError.name;
        this.code = properties.code;
        this.details = properties.details;
        this.cause = properties.cause;
    }
}

export function newGatewayError(err: ServiceError): GatewayError {
    const metadata = err.metadata?.get('grpc-status-details-bin') || [];
    const details = metadata
        .flatMap(metadataValue => Status.deserializeBinary(Buffer.from(metadataValue)).getDetailsList())
        .map(statusDetail => {
            const endpointError = ErrorDetailProto.deserializeBinary(statusDetail.getValue_asU8());
            const detail: ErrorDetail = {
                address: endpointError.getAddress(),
                message: endpointError.getMessage(),
                mspId: endpointError.getMspId(),
            };
            return detail;
        });

    return new GatewayError({
        message: err.message,
        code: err.code,
        details,
        cause: err,
    });
}