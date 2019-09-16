package org.triplea.server.access;

import io.dropwizard.auth.Authorizer;
import org.triplea.lobby.server.db.data.UserRole;

/**
 * Performs authorization. Authorization happens after authentication and answers the question of:
 * given an authenticated user, do they have permissions for a given role?
 */
public class RoleAuthorizer implements Authorizer<AuthenticatedUser> {

  @Override
  public boolean authorize(final AuthenticatedUser user, final String role) {
    switch (role) {
      case UserRole.ADMIN:
        return user.isAdmin();
      case UserRole.MODERATOR:
        return user.isAdmin() || user.isModerator();
      default:
        return true;
    }
  }
}
