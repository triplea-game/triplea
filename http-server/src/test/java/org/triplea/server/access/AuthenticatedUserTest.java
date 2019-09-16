package org.triplea.server.access;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;
import org.triplea.lobby.server.db.data.UserRole;

class AuthenticatedUserTest {

  @Test
  void isAdmin() {
    final AuthenticatedUser user = givenUserWithRole(UserRole.ADMIN);

    assertThat(user.isAdmin(), is(true));
  }

  @Test
  void notAdmin() {
    asList(UserRole.MODERATOR, UserRole.ANONYMOUS, UserRole.PLAYER)
        .forEach(
            notAdmin -> {
              final AuthenticatedUser user = givenUserWithRole(notAdmin);
              assertThat(user.isAdmin(), is(false));
            });
  }

  @Test
  void isModerator() {
    final AuthenticatedUser user = givenUserWithRole(UserRole.MODERATOR);

    assertThat(user.isModerator(), is(true));
  }

  @Test
  void notModerator() {
    asList(UserRole.ADMIN, UserRole.ANONYMOUS, UserRole.PLAYER)
        .forEach(
            notModerator -> {
              final AuthenticatedUser user = givenUserWithRole(notModerator);
              assertThat(user.isModerator(), is(false));
            });
  }

  private static AuthenticatedUser givenUserWithRole(final String role) {
    return AuthenticatedUser.builder().userRole(role).build();
  }
}
