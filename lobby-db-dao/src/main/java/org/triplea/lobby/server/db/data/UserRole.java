package org.triplea.lobby.server.db.data;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Class to hold constants representing the possible set of user role values. <br>
 * Note: this is not an enum to allow these values to be referenced from annotations.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserRole {
  public static final String ADMIN = "ADMIN";
  public static final String MODERATOR = "MODERATOR";
  public static final String PLAYER = "PLAYER";
  public static final String ANONYMOUS = "ANONYMOUS";
}
