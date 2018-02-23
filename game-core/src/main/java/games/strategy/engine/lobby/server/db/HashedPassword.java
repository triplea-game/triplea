package games.strategy.engine.lobby.server.db;

import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

/**
 * A Wrapper class for salted password hashes.
 * If the given String is not matching the format
 * of Md5Crypt or BCrypt hashes isValidSyntax returns false.
 */
public final class HashedPassword {
  public final String value;

  public HashedPassword(final String hashedPassword) {
    this.value = Strings.nullToEmpty(hashedPassword);
  }

  /**
   * Returns true if the hashed password looks like it could be a hash.
   */
  public boolean isHashedWithSalt() {
    return isMd5Crypted() || isBcrypted();
  }

  public boolean isBcrypted() {
    return (value != null) && value.matches("^\\$2a\\$.{56}$");
  }

  public boolean isMd5Crypted() {
    return games.strategy.util.Md5Crypt.isLegalEncryptedPassword(value);
  }

  /**
   * Returns the value of this HashedPassword object with all characters replaces with asterisks.
   */
  public String mask() {
    return Strings.repeat("*", value.length());
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    } else if (!(obj instanceof HashedPassword)) {
      return false;
    }

    final HashedPassword other = (HashedPassword) obj;
    return Objects.equals(value, other.value);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("value", mask())
        .toString();
  }
}
