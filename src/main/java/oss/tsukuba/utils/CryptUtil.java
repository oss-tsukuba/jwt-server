package oss.tsukuba.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptUtil {

	private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

	private static final String INIT_VECTOR = "yourInitVector01";

	private static final IvParameterSpec IV = new IvParameterSpec(INIT_VECTOR.getBytes());

	private static MessageDigest md;

	static {
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			LogUtils.error(e.toString(), e);
		}
	}
	
	private static byte[] getSHA256(String pass) {
        byte[] cipher_byte;
        md.update(pass.getBytes());
        cipher_byte = md.digest();
        
        return cipher_byte;
	}
	
	public static String encrypt(String text, String pass) throws Exception {
		SecretKeySpec key = new SecretKeySpec(getSHA256(pass), "AES");
		
	    Cipher encrypter = Cipher.getInstance(ALGORITHM);
	    encrypter.init(Cipher.ENCRYPT_MODE, key, IV);
	    byte[] byteToken = encrypter.doFinal(text.getBytes());

	    return new String(Base64.getEncoder().encode(byteToken));
	}
	
	public static String decrypt(String text, String pass) throws Exception {
		SecretKeySpec key = new SecretKeySpec(getSHA256(pass), "AES");

	    Cipher decrypter = Cipher.getInstance(ALGORITHM);
	    decrypter.init(Cipher.DECRYPT_MODE, key, IV);
	    byte[] byteToken = Base64.getDecoder().decode(text);

	    return new String(decrypter.doFinal(byteToken));
	}
}
