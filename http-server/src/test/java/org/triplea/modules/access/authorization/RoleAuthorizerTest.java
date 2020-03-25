package org.triplea.modules.access.authorization;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.data.UserRole;
import org.triplea.modules.access.authentication.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class RoleAuthorizerTest {

  @Mock private AuthenticatedUser authenticatedUser;

  private final RoleAuthorizer roleAuthorizer = new RoleAuthorizer();

  @Test
  void authenticateAsAdmin() {
    givenUserAssumingEachRoleInSequence();

    // first iteration, user is admin
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.ADMIN), is(true));
    // second iteration, user is moderator
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.ADMIN), is(false));
    // third iteration, user is player
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.ADMIN), is(false));
    // fourth iteration, user is anonymous
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.ADMIN), is(false));
    // fifth iteration, user is host
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.ADMIN), is(false));
  }

  /** Rotates user role on each authorization iteration. */
  private void givenUserAssumingEachRoleInSequence() {
    when(authenticatedUser.getUserRole())
        .thenReturn(UserRole.ADMIN)
        .thenReturn(UserRole.MODERATOR)
        .thenReturn(UserRole.PLAYER)
        .thenReturn(UserRole.ANONYMOUS)
        .thenReturn(UserRole.HOST);
  }

  @Test
  void authenticateAsModerator() {
    givenUserAssumingEachRoleInSequence();

    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.MODERATOR), is(true));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.MODERATOR), is(true));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.MODERATOR), is(false));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.MODERATOR), is(false));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.MODERATOR), is(false));
  }

  @Test
  void authenticateAsPlayer() {
    givenUserAssumingEachRoleInSequence();

    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.PLAYER), is(true));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.PLAYER), is(true));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.PLAYER), is(true));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.PLAYER), is(false));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.PLAYER), is(false));
  }

  @Test
  void authenticateAsAnonymous() {
    givenUserAssumingEachRoleInSequence();

    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.ANONYMOUS), is(true));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.ANONYMOUS), is(true));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.ANONYMOUS), is(true));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.ANONYMOUS), is(true));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.ANONYMOUS), is(false));
  }

  @Test
  void authenticateAsHost() {
    givenUserAssumingEachRoleInSequence();

    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.HOST), is(false));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.HOST), is(false));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.HOST), is(false));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.HOST), is(false));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.HOST), is(true));
  }
}
