package games.strategy.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Replacement for MD5Crypt
 * using  SHA256 instead
 */
public class SHA256Crypt {
  /**
   * Returns the SHA256-Hash of the given String
   */
  public static String crypt(String text) throws UnsupportedEncodingException, NoSuchAlgorithmException{
    MessageDigest md = MessageDigest.getInstance("SHA-256");

    md.update(text.getBytes("UTF-8"));
    byte[] digest = md.digest();
    
    return String.format("%064x", new java.math.BigInteger(1, digest));
  }
}
