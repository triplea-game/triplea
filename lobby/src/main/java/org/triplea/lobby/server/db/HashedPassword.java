package org.triplea.lobby.server.db;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/**
 * A Wrapper class for salted password hashes.
 * If the given String is not matching the format of Md5Crypt or BCrypt hashes isValidSyntax returns false.
 */
@AllArgsConstructor
@EqualsAndHashCode
public final class HashedPassword {
  public final String value;

  /**
   * Returns true if the hashed password looks like it could be a hash.
   */
  public boolean isHashedWithSalt() {
    return isBcrypted();
  }

  boolean isBcrypted() {
    return value != null && value.matches("^\\$2a\\$.{56}$");
  }
}
