package org.triplea.server.access;

import io.dropwizard.auth.Authorizer;
import org.triplea.lobby.server.db.data.UserRole;

/**
 * Performs authorization. Authorization happens after authentication and answers the question of:
 * given an authenticated user, do they have permissions for a given role?
 */
public class RoleAuthorizer implements Authorizer<AuthenticatedUser> {

  /** Verifies a user is authorized to assume a requestedRole. <br> */
  @Override
  public boolean authorize(final AuthenticatedUser user, final String requestedRole) {
    switch (user.getUserRole()) {
      case UserRole.ADMIN:
        return adminPermissions(requestedRole);
      case UserRole.MODERATOR:
        return moderatorPermission(requestedRole);
      case UserRole.PLAYER:
        return playerPermissions(requestedRole);
      case UserRole.ANONYMOUS:
        return anonymousPermissions(requestedRole);
      case UserRole.HOST:
        return requestedRole.equals(UserRole.HOST);
      default:
        throw new AssertionError("Unrecognized user role: " + user.getUserRole());
    }
  }

  private static boolean adminPermissions(final String requestedRole) {
    return requestedRole.equals(UserRole.ADMIN) || moderatorPermission(requestedRole);
  }

  private static boolean moderatorPermission(final String requestedRole) {
    return requestedRole.equals(UserRole.MODERATOR) || playerPermissions(requestedRole);
  }

  private static boolean playerPermissions(final String requestedRole) {
    return requestedRole.equals(UserRole.PLAYER) || anonymousPermissions(requestedRole);
  }

  private static boolean anonymousPermissions(final String requestedRole) {
    return requestedRole.equals(UserRole.ANONYMOUS);
  }
}
