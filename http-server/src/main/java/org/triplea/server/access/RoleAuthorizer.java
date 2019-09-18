package org.triplea.server.access;

import io.dropwizard.auth.Authorizer;
import org.triplea.lobby.server.db.data.UserRole;

/**
 * Performs authorization. Authorization happens after authentication and answers the question of:
 * given an authenticated user, do they have permissions for a given role?
 */
public class RoleAuthorizer implements Authorizer<AuthenticatedUser> {

  /**
   * Verifies a user is authorized to assume a requestedRole. <br>
   * Rules are:<br>
   * - admin is authorized all roles<br>
   * - moderator is authorized all roles except admin<br>
   * - player can access player or anonymous resources<br>
   * - anonymous has lowest privilege and can access only the anonymous role
   */
  @Override
  public boolean authorize(final AuthenticatedUser user, final String requestedRole) {
    switch (user.getUserRole()) {
      case UserRole.ADMIN:
        return true;
      case UserRole.MODERATOR:
        return !requestedRole.equals(UserRole.ADMIN);
      case UserRole.PLAYER:
        return requestedRole.equals(UserRole.PLAYER) || requestedRole.equals(UserRole.ANONYMOUS);
      case UserRole.ANONYMOUS:
        return requestedRole.equals(UserRole.ANONYMOUS);
      default:
        throw new AssertionError("Unrecognized user role: " + user.getUserRole());
    }
  }
}
