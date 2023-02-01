package org;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class SignatureTest {

	public static void main(String[] args) {
        try {
    		Security.addProvider(new BouncyCastleProvider());

        	KeyFactory kf = KeyFactory.getInstance("EC");
        	
        	String publicPem = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE0o3Pj71mTpIWlHBn4rdfK/YSu0iftngmLNdNkA65+xzcwJkAd4ronyFr6Wjxl/ZUkx0nzZBQ8htfAKa1/wK0YQ==";
        	byte[] publicDer = Base64.getDecoder().decode(publicPem);
        	EncodedKeySpec	publicKeySpec = new X509EncodedKeySpec(publicDer);
        	
        	String header = "eyJhbGciOiJFUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJNRmV6amg5TGxPbnA4RTg5WU5hLWRmSlBISHppeXVOblZJSEx5TFVCb0U0In0";
        	String content = "eyJleHAiOjE2Njg0MDk1MTksImlhdCI6MTY2ODQwNTkxOSwianRpIjoiZmNmYTQzZjQtYTljMC00ZmMxLWJiY2EtNjVhMTIxNTEzMWRjIiwiaXNzIjoiaHR0cHM6Ly9jYW4zLmNhbmFseS5jby5qcDo4NDQzL2F1dGgvcmVhbG1zL2hwY2kiLCJhdWQiOiJocGNpIiwic3ViIjoiMTkyMzc1NTQtZTZhYS00MDg3LWIzM2MtOWNmMDY4MWQzOGVlIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiand0LXNlcnZlci10ZXN0Iiwic2Vzc2lvbl9zdGF0ZSI6IjcyMzA4YzUzLThmZTYtNDMzNi1iZWQ3LTVkNTlkNWUxYzMwOSIsImFjciI6IjEiLCJzY29wZSI6Im9mZmxpbmVfYWNjZXNzIHNjaXRva2VucyBvcGVuaWQgaHBjaSIsInNpZCI6IjcyMzA4YzUzLThmZTYtNDMzNi1iZWQ3LTVkNTlkNWUxYzMwOSIsImhwY2kuaWQiOiJrdW1hIiwiaHBjaS52ZXIiOiIxLjAiLCJ2ZXIiOiJzY2l0b2tlbnM6Mi4wIiwibmJmIjowfQ";
        	String signature = "j1fPHyBO75jGUQ_F32n1PXOO-6ognOfAf3_fnwYtfT92BHVPtPi_BHuYCW6U8CdyKTRMm7egl3sE06GKWn-m5g-4fDJYyODMbZTh4bKFxWTKJd-mhxhTZNz46ff45Q";
        	
        	PublicKey publicKey = kf.generatePublic(publicKeySpec);
        	
            Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
            verifier.initVerify(publicKey);
            verifier.update((header + "." + content).getBytes(StandardCharsets.UTF_8));
            verifier.verify(signature.getBytes(StandardCharsets.UTF_8));
            System.out.println("Verify OK");
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}

}
