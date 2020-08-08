package org.triplea.modules.access.authorization;

import io.dropwizard.auth.Authorizer;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.modules.access.authentication.AuthenticatedUser;

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
        return adminAuthorizedFor(requestedRole);
      case UserRole.MODERATOR:
        return moderatorAuthorizedFor(requestedRole);
      case UserRole.PLAYER:
        return playerAuthorizedFor(requestedRole);
      case UserRole.ANONYMOUS:
        return anonymousAuthorizedFor(requestedRole);
      case UserRole.HOST:
        return requestedRole.equals(UserRole.HOST);
      default:
        throw new AssertionError("Unrecognized user role: " + user.getUserRole());
    }
  }

  private static boolean adminAuthorizedFor(final String requestedRole) {
    return requestedRole.equals(UserRole.ADMIN) || moderatorAuthorizedFor(requestedRole);
  }

  private static boolean moderatorAuthorizedFor(final String requestedRole) {
    return requestedRole.equals(UserRole.MODERATOR) || playerAuthorizedFor(requestedRole);
  }

  private static boolean playerAuthorizedFor(final String requestedRole) {
    return requestedRole.equals(UserRole.PLAYER) || anonymousAuthorizedFor(requestedRole);
  }

  private static boolean anonymousAuthorizedFor(final String requestedRole) {
    return requestedRole.equals(UserRole.ANONYMOUS);
  }
}
