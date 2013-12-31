package pw.caple.bolt.user;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

public final class Hashing {

	public static byte[] generateSalt() {
		final Random r = new SecureRandom();
		byte[] salt = new byte[32];
		r.nextBytes(salt);
		return salt;
	}

	public static byte[] computeHash(String data, byte[] salt) {
		try {
			return computeHash(data.getBytes("UTF-8"), salt);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static byte[] computeHash(byte[] data, byte[] salt) {
		try {
			byte[] bytes = new byte[data.length + salt.length];
			System.arraycopy(data, 0, bytes, 0, data.length);
			System.arraycopy(salt, 0, bytes, data.length, salt.length);
			MessageDigest md;
			md = MessageDigest.getInstance("SHA-512");
			md.update(bytes);
			return md.digest();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}
}
