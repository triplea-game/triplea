package games.strategy.engine.lobby.server.db;

import com.google.common.base.Strings;

import games.strategy.util.MD5Crypt;

/**
 * A Wrapper class for salted password hashes.
 * If the given String is not matching the format
 * of MD5Crypt or BCrypt hashes isValidSyntax returns false.
 */
public class HashedPassword {
  public final String value;

  public HashedPassword(final String hashedPassword) {
    this.value = Strings.nullToEmpty(hashedPassword);
  }

  /**
   * Returns true if the hashed password looks like it could be a hash.
   */
  public boolean isValidSyntax() {
    return isMd5Crypted() || isBcrypted();
  }

  public boolean isBcrypted() {
    return value != null && value.matches("^\\$2a\\$.{56}$");
  }

  private boolean isMd5Crypted() {
    return value != null && value.startsWith(MD5Crypt.MAGIC);
  }

  /**
   * Returns the value of this HashedPassword object with all characters replaces with asterisks.
   */
  public String mask() {
    return Strings.repeat("*", value.length());
  }
}

