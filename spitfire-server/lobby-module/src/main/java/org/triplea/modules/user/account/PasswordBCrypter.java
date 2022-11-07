package org.triplea.modules.user.account;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PasswordBCrypter {
  /**
   * This is a helper method designed to simplify the bcrypt API and hide some of the constants
   * involved. This method generates a hash with 10 rounds. This number is arbitrary and might
   * increase at a later time.
   *
   * @param password The string to apply the bcrypt algorithm to.
   * @return A hashed password using a randomly generated bcrypt salt.
   */
  public static String hashPassword(final String password) {
    return BCrypt.with(LongPasswordStrategies.none()).hashToString(10, password.toCharArray());
  }

  /**
   * Checks of the provided password does match the existing hash. NOTE: Any passwords longer than
   * 72 bytes (UTF-8) will result in the same hash as the version trimmed to 72 bytes.
   *
   * @param password The password to check.
   * @param hash The hash to verify the password against.
   * @return True if the password matches the hash, false otherwise.
   */
  public static boolean verifyHash(final String password, final String hash) {
    return BCrypt.verifyer(null, LongPasswordStrategies.none())
        .verify(password.toCharArray(), hash.toCharArray())
        .verified;
  }
}
