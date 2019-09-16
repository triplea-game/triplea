package org.triplea.server.access;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.lobby.server.db.data.UserRole;

@ExtendWith(MockitoExtension.class)
class RoleAuthorizerTest {

  @Mock private AuthenticatedUser authenticatedUser;

  private final RoleAuthorizer roleAuthorizer = new RoleAuthorizer();

  @Nested
  final class AdminRole {
    @Test
    void authenticateAdmin() {
      when(authenticatedUser.isAdmin()).thenReturn(true);

      final boolean result = roleAuthorizer.authorize(authenticatedUser, UserRole.ADMIN);

      assertThat(result, is(true));
    }

    @Test
    void authenticateAdminNegativeCase() {
      when(authenticatedUser.isAdmin()).thenReturn(false);

      final boolean result = roleAuthorizer.authorize(authenticatedUser, UserRole.ADMIN);

      assertThat(result, is(false));
    }
  }

  @Nested
  final class AuthenticateModerator {
    @Test
    void authenticateModerator() {
      when(authenticatedUser.isModerator()).thenReturn(true);

      final boolean result = roleAuthorizer.authorize(authenticatedUser, UserRole.MODERATOR);

      assertThat(result, is(true));
    }

    @Test
    void authenticateModeratorNegativeCase() {
      when(authenticatedUser.isModerator()).thenReturn(false);

      final boolean result = roleAuthorizer.authorize(authenticatedUser, UserRole.MODERATOR);

      assertThat(result, is(false));
    }
  }
}
