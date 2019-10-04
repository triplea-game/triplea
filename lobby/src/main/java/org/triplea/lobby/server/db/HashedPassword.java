package org.triplea.lobby.server.db;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import lombok.EqualsAndHashCode;
import org.triplea.util.Md5Crypt;

/**
 * A Wrapper class for salted password hashes. If the given String is not matching the format of
 * Md5Crypt or BCrypt hashes isValidSyntax returns false.
 */
@EqualsAndHashCode
public final class HashedPassword {
  public final String value;

  public HashedPassword(final String hashedPassword) {
    this.value = Strings.nullToEmpty(hashedPassword);
  }

  /** Returns true if the hashed password looks like it could be a hash. */
  public boolean isHashedWithSalt() {
    return isMd5Crypted() || isBcrypted();
  }

  public boolean isBcrypted() {
    return value.matches("^\\$2a\\$.{56}$");
  }

  public boolean isMd5Crypted() {
    return Md5Crypt.isLegalHashedValue(value);
  }

  /**
   * Returns the value of this HashedPassword object with all characters replaces with asterisks.
   */
  public String mask() {
    return Strings.repeat("*", value.length());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("value", mask()).toString();
  }
}
