package games.strategy.engine.lobby.server.db;

import com.google.common.base.Strings;

import games.strategy.util.MD5Crypt;

/**
 * A Wrapper class for password hashes in the form of the legacy MD5 with salt system,
 * and the new BCrypt system.
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
    return value != null && (value.startsWith(MD5Crypt.MAGIC) || value.matches("^\\$2a\\$.{56}$"));
  }

  public boolean isBcrypted() {
    return value != null && value.matches("^\\$2a\\$.{56}$");
  }
}

