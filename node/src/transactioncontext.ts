/*
 * Copyright 2020 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import { common } from '@hyperledger/fabric-protos';
import { randomBytes } from 'node:crypto';
import { sha256 } from './hash/hashes';
import { SigningIdentity } from './signingidentity';

export class TransactionContext {
    readonly #transactionId: string;
    readonly #signatureHeader: common.SignatureHeader;

    constructor(signingIdentity: SigningIdentity) {
        const nonce = randomBytes(24);
        const creator = signingIdentity.getCreator();

        const saltedCreator = Buffer.concat([nonce, creator]);
        const rawTransactionId = sha256(saltedCreator);
        this.#transactionId = Buffer.from(rawTransactionId).toString('hex');

        this.#signatureHeader = new common.SignatureHeader();
        this.#signatureHeader.setCreator(creator);
        this.#signatureHeader.setNonce(nonce);
    }

    getTransactionId(): string {
        return this.#transactionId;
    }

    getSignatureHeader(): common.SignatureHeader {
        return this.#signatureHeader;
    }
}
