package games.strategy.engine.lobby.server.userDB;

import com.google.common.base.Strings;

import games.strategy.util.MD5Crypt;

public class HashedPassword {
  public final String value;

  public HashedPassword(String hashedPassword) {
    this.value = Strings.nullToEmpty(hashedPassword);
  }

  /**
   * Returns true if the hashed password looks like it could be a hash.
   */
  public boolean isValidSyntax() {
    return value != null && value.startsWith(MD5Crypt.MAGIC);
  }
}

