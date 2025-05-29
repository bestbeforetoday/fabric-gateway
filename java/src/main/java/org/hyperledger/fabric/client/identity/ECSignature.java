/*
 * Copyright 2021 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.client.identity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;

final class ECSignature {
    private final ASN1Integer r;
    private final ASN1Integer s;

    static ECSignature fromBytes(final byte[] derSignature) throws GeneralSecurityException {
        ASN1Primitive asn1 = readASN1Primitive(derSignature);
        if (!(asn1 instanceof ASN1Sequence)) {
            throw new GeneralSecurityException(
                    "Invalid signature type: " + asn1.getClass().getTypeName());
        }

        ASN1Sequence asn1Sequence = (ASN1Sequence) asn1;
        List<ASN1Integer> signatureParts = StreamSupport.stream(asn1Sequence.spliterator(), false)
                .map(ASN1Encodable::toASN1Primitive)
                .filter(asn1Primitive -> asn1Primitive instanceof ASN1Integer)
                .map(asn1Primitive -> (ASN1Integer) asn1Primitive)
                .collect(Collectors.toList());
        if (signatureParts.size() != 2) {
            throw new GeneralSecurityException("Invalid signature. Expected 2 values but got " + signatureParts.size());
        }

        return new ECSignature(signatureParts.get(0), signatureParts.get(1));
    }

    private static ASN1Primitive readASN1Primitive(final byte[] asn1Bytes) throws GeneralSecurityException {
        try (ASN1InputStream asnInputStream = new ASN1InputStream(asn1Bytes)) {
            return asnInputStream.readObject();
        } catch (IOException e) {
            throw new GeneralSecurityException(e);
        }
    }

    ECSignature(final ASN1Integer r, final ASN1Integer s) {
        this.r = r;
        this.s = s;
    }

    public ECSignature toLowS(final BigInteger curveN) {
        BigInteger sValue = s.getValue();
        BigInteger halfCurveN = curveN.divide(BigInteger.valueOf(2));

        if (sValue.compareTo(halfCurveN) <= 0) {
            return this;
        }

        BigInteger lowS = curveN.subtract(sValue);
        return new ECSignature(r, new ASN1Integer(lowS));
    }

    public byte[] getBytes() {
        DERSequence sequence = new DERSequence(new ASN1Integer[] {r, s});
        try {
            return sequence.getEncoded();
        } catch (IOException e) {
            // Should never happen
            throw new UncheckedIOException(e);
        }
    }
}
