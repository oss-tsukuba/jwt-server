/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.jose.jws.crypto;


import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.spec.ECPublicKeySpec;

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.keycloak.common.util.PemUtils;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSInput;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ECDSAProvider implements SignatureProvider {
    public static String getJavaAlgorithm(Algorithm alg) {
        switch (alg) {
            case ES256:
                return "SHA256withECDSAinP1363Format";
            case ES384:
                return "SHA384withECDSAinP1363Format";
            case ES512:
                return "SHA512withECDSAinP1363Format";
            default:
                throw new IllegalArgumentException("Not an ECDSA Algorithm");
        }
    }

    public static Signature getSignature(Algorithm alg) {
        try {
            return Signature.getInstance(getJavaAlgorithm(alg));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] sign(byte[] data, Algorithm algorithm, PrivateKey privateKey) {
        try {
            Signature signature = getSignature(algorithm);
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verifyViaCertificate(JWSInput input, String cert) {
        X509Certificate certificate = null;
        try {
            certificate = PemUtils.decodeCertificate(cert);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return verify(input, certificate.getPublicKey());
    }

    public static boolean verify(JWSInput input, PublicKey publicKey) {
        try {
            PublicKey pubKey = publicKey;

            if (publicKey instanceof BCECPublicKey) {
                ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(((BCECPublicKey)publicKey).getW(), ((BCECPublicKey)publicKey).getParams());
                KeyFactory kf = KeyFactory.getInstance("EC");
                pubKey = kf.generatePublic(publicKeySpec);
            }

            Signature verifier = getSignature(input.getHeader().getAlgorithm());
            verifier.initVerify(pubKey);
            verifier.update(input.getEncodedSignatureInput().getBytes(StandardCharsets.UTF_8));
            return verifier.verify(input.getSignature());
        } catch (Exception e) {
            return false;
        }

    }

    @Override
    public boolean verify(JWSInput input, String key) {
        return verifyViaCertificate(input, key);
    }


}
