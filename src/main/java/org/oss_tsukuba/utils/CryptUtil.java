package org.oss_tsukuba.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptUtil {

	private static final String ALGORITHM = "AES_256/GCM/NoPadding";

	private static MessageDigest md;

	private static final int GCM_IV_LENGTH = 12; // 96bit is commonly used

	private static final int GCM_TAG_BITS = 128;

	static {
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			LogUtils.error(e.toString(), e);
		}
	}

	public static byte[] generateIV() {
		byte[] iv = new byte[GCM_IV_LENGTH];
		SecureRandom random = new SecureRandom();
		random.nextBytes(iv);

		return iv;
	}
	
	private static byte[] getSHA256(String pass) {
		byte[] cipher_byte;
		md.update(pass.getBytes());
		cipher_byte = md.digest();

		return cipher_byte;
	}

	public static byte[] encrypt(byte[] plainText, String pass, byte[] iv) throws Exception {
		SecretKeySpec key = new SecretKeySpec(getSHA256(pass), "AES");
		GCMParameterSpec params = new GCMParameterSpec(GCM_TAG_BITS, iv);
		Cipher encrypter = Cipher.getInstance(ALGORITHM);
		encrypter.init(Cipher.ENCRYPT_MODE, key, params);

		byte[] cipherText = encrypter.doFinal(plainText);

		return cipherText;
	}

	public static byte[] decrypt(byte[] cipherText, String pass, byte[] iv) throws Exception {
		SecretKeySpec key = new SecretKeySpec(getSHA256(pass), "AES");
		GCMParameterSpec params = new GCMParameterSpec(GCM_TAG_BITS, iv);
		Cipher decrypter = Cipher.getInstance(ALGORITHM);
		decrypter.init(Cipher.DECRYPT_MODE, key, params);

		byte[] plainText = decrypter.doFinal(cipherText);

		return plainText;
	}
}
