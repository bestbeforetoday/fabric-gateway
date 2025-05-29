/*
 * Copyright 2020 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.client.identity;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.interfaces.ECPrivateKey;

final class ECPrivateKeySigner implements Signer {
    private static final String ALGORITHM_NAME = "NONEwithECDSA";

    private final Signer signer;
    private final BigInteger curveN;

    ECPrivateKeySigner(final ECPrivateKey privateKey) {
        signer = new PrivateKeySigner(privateKey, ALGORITHM_NAME);
        curveN = privateKey.getParams().getOrder();
    }

    @Override
    public byte[] sign(final byte[] digest) throws GeneralSecurityException {
        byte[] rawSignature = signer.sign(digest);
        ECSignature signature = ECSignature.fromBytes(rawSignature).toLowS(curveN);
        return signature.getBytes();
    }
}
