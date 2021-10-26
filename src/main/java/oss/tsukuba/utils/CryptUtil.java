package oss.tsukuba.utils;

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

	private static final byte[] IV;

	private static MessageDigest md;

	public static final int AES_KEY_SIZE = 256;

	public static final int GCM_IV_LENGTH = 128;

	static {
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			LogUtils.error(e.toString(), e);
		}

		IV = new byte[GCM_IV_LENGTH];

		// Generate Key
		SecureRandom random = new SecureRandom();
		random.nextBytes(IV);
	}

	private static byte[] getSHA256(String pass) {
		byte[] cipher_byte;
		md.update(pass.getBytes());
		cipher_byte = md.digest();

		return cipher_byte;
	}

	public static String encrypt(String text, String pass) throws Exception {
		byte[] plainText = text.getBytes();

		SecretKeySpec key = new SecretKeySpec(getSHA256(pass), "AES");
		GCMParameterSpec params = new GCMParameterSpec(IV.length, IV);
		Cipher encrypter = Cipher.getInstance(ALGORITHM);
		encrypter.init(Cipher.ENCRYPT_MODE, key, params);

		byte[] cipherText = new byte[encrypter.getOutputSize(plainText.length)];
		encrypter.doFinal(plainText, 0, plainText.length, cipherText);

		return new String(Base64.getEncoder().encode(cipherText));
	}

	public static String decrypt(String text, String pass) throws Exception {
		byte[] cipherText = text.getBytes();

		SecretKeySpec key = new SecretKeySpec(getSHA256(pass), "AES");
		GCMParameterSpec params = new GCMParameterSpec(IV.length, IV);
		Cipher decrypter = Cipher.getInstance(ALGORITHM);
		decrypter.init(Cipher.DECRYPT_MODE, key, params);

		return new String(decrypter.doFinal(cipherText, 0, cipherText.length));
	}
}
