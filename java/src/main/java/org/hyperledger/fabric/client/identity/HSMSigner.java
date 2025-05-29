/*
 * Copyright 2025 IBM All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.client.identity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.*;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.CertificateException;
import java.util.Map;

final class HSMSigner implements Signer {
    private final Provider provider;
    private final Signer signer;

    public HSMSigner(final Path library, final String slot, final String pin, final Map<String, String> attributes)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
            UnrecoverableEntryException {
        StringBuilder config = new StringBuilder();
        config.append("name=\n");
        config.append("library=" + library.toAbsolutePath().toString() + "\n");
        config.append("slot=" + slot + "\n");
        attributes.forEach((key, value) -> config.append(key + "=" + value + "\n"));

        ByteArrayInputStream configStream = new ByteArrayInputStream(
                config.toString().getBytes(StandardCharsets.UTF_8));
        provider = new sun.security.pkcs11.SunPKCS11(configStream);

        KeyStore keyStore = KeyStore.getInstance(null, provider);
        keyStore.load(null, pin.toCharArray());
        Entry keyEntry = keyStore.getEntry(null, new PasswordProtection(pin.toCharArray()));
        if (!(keyEntry instanceof PrivateKeyEntry)) {
            throw new IllegalArgumentException(
                    "Invalid key type: " + keyEntry != null ? keyEntry.getClass().getSimpleName()
                            : String.valueOf(keyEntry));
        }

        PrivateKey privateKey = ((PrivateKeyEntry) keyEntry).getPrivateKey();
        signer = Signers.newPrivateKeySigner(privateKey);
    }

    @Override
    public byte[] sign(byte[] digest) throws GeneralSecurityException {
        return signer.sign(digest);
    }
}
