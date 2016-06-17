package games.strategy.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Replacement for MD5Crypt
 * using SHA512 instead
 * 
 * Do not use for Passwords!
 * Use the BCrypt library instead!
 */
public class SHA512Crypt {

  public static final String SHA_512 = "SHA-512";

  /**
   * Returns the SHA256-Hash of the given String
   */
  public static String crypt(String text) {
    return crypt(text, "");
  }
  
  /**
   * Returns the SHA256-Hash of the given String using the specified Salt
   */
  public static String crypt(String text, String salt){
    try {
      MessageDigest md = MessageDigest.getInstance(SHA_512);

      md.update(salt.getBytes(StandardCharsets.UTF_8));
      byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));

      return String.format("%064x", new BigInteger(1, digest));
    } catch (NoSuchAlgorithmException e) {
      // This Code shouldn't be executed under any circumstances
      throw new IllegalStateException("The SHA512Crypt class uses an invalid algorithm", e);
    }
  }
  
  /**
   * 
   * Same as {@link SHA512Crypt.crypt(text, salt)}, but passing the salt as well
   */
  public static String cryptPassSalt(String text, String salt){
    return "$" + salt + crypt(text, salt);
  }
}
