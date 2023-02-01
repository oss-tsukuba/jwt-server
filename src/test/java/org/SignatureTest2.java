package org;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Base64Url;
import org.keycloak.jose.jwk.ECPublicJWK;

public class SignatureTest2 {

	public static void main(String[] args) {
        try {
    		Security.addProvider(new BouncyCastleProvider());

    		CryptoIntegration.init(SignatureTest2.class.getClassLoader());
    		
            BigInteger x = new BigInteger(1, Base64Url.decode("0o3Pj71mTpIWlHBn4rdfK_YSu0iftngmLNdNkA65-xw"));
            BigInteger y = new BigInteger(1, Base64Url.decode("3MCZAHeK6J8ha-lo8Zf2VJMdJ82QUPIbXwCmtf8CtGE"));

            ECPoint point = new ECPoint(x, y);
            ECParameterSpec params = CryptoIntegration.getProvider().createECParams("secp256r1");
            ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(point, params);
            
        	KeyFactory kf = KeyFactory.getInstance("ECDSA");
        	
        	String header = "eyJhbGciOiJFUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJNRmV6amg5TGxPbnA4RTg5WU5hLWRmSlBISHppeXVOblZJSEx5TFVCb0U0In0";
        	String content = "eyJleHAiOjE2Njg0MDk1MTksImlhdCI6MTY2ODQwNTkxOSwianRpIjoiZmNmYTQzZjQtYTljMC00ZmMxLWJiY2EtNjVhMTIxNTEzMWRjIiwiaXNzIjoiaHR0cHM6Ly9jYW4zLmNhbmFseS5jby5qcDo4NDQzL2F1dGgvcmVhbG1zL2hwY2kiLCJhdWQiOiJocGNpIiwic3ViIjoiMTkyMzc1NTQtZTZhYS00MDg3LWIzM2MtOWNmMDY4MWQzOGVlIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiand0LXNlcnZlci10ZXN0Iiwic2Vzc2lvbl9zdGF0ZSI6IjcyMzA4YzUzLThmZTYtNDMzNi1iZWQ3LTVkNTlkNWUxYzMwOSIsImFjciI6IjEiLCJzY29wZSI6Im9mZmxpbmVfYWNjZXNzIHNjaXRva2VucyBvcGVuaWQgaHBjaSIsInNpZCI6IjcyMzA4YzUzLThmZTYtNDMzNi1iZWQ3LTVkNTlkNWUxYzMwOSIsImhwY2kuaWQiOiJrdW1hIiwiaHBjaS52ZXIiOiIxLjAiLCJ2ZXIiOiJzY2l0b2tlbnM6Mi4wIiwibmJmIjowfQ";
        	String signature = "j1fPHyBO75jGUQ_F32n1PXOO-6ognOfAf3_fnwYtfT92BHVPtPi_BHuYCW6U8CdyKTRMm7egl3sE06GKWn-m5g-4fDJYyODMbZTh4bKFxWTKJd-mhxhTZNz46ff45Q";
        	
        	PublicKey publicKey = kf.generatePublic(publicKeySpec);
        	
        	ECPublicKeySpec publicKeySpec2 = new ECPublicKeySpec(((BCECPublicKey)publicKey).getW(), ((BCECPublicKey)publicKey).getParams());
        	KeyFactory kf2 = KeyFactory.getInstance("EC");
        	PublicKey publickey2 = kf2.generatePublic(publicKeySpec2);

            Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
            verifier.initVerify(publickey2);
            verifier.update((header + "." + content).getBytes(StandardCharsets.UTF_8));
            verifier.verify(signature.getBytes(StandardCharsets.UTF_8));
            System.out.println("Verify OK");
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}

}
