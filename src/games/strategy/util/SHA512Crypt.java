package games.strategy.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Replacement for MD5Crypt
 * using SHA512 instead
 */
public class SHA512Crypt {

  public static final String SHA_512 = "SHA-512";

  /**
   * Returns the SHA256-Hash of the given String
   * is used non-static for testing purposes
   */
  public static String crypt(String text) {
    try {
      MessageDigest md = MessageDigest.getInstance(SHA_512);

      md.update(text.getBytes(StandardCharsets.UTF_8));
      byte[] digest = md.digest();

      return String.format("%064x", new BigInteger(1, digest));
    } catch (NoSuchAlgorithmException e) {
      // This Code shouldn't be executed under any circumstances
      throw new IllegalStateException("The SHA512Crypt class uses an invalid algorithm", e);
    }
  }
}
