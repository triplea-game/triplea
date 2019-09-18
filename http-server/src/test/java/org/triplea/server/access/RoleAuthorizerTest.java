package org.triplea.server.access;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.lobby.server.db.data.UserRole;

@ExtendWith(MockitoExtension.class)
class RoleAuthorizerTest {

  @Mock private AuthenticatedUser authenticatedUser;

  private final RoleAuthorizer roleAuthorizer = new RoleAuthorizer();

  @Test
  void authenticateAdmin() {
    when(authenticatedUser.getUserRole())
        .thenReturn(UserRole.ADMIN)
        .thenReturn(UserRole.MODERATOR)
        .thenReturn(UserRole.PLAYER)
        .thenReturn(UserRole.ANONYMOUS);

    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.ADMIN), is(true));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.ADMIN), is(false));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.ADMIN), is(false));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.ADMIN), is(false));
  }

  @Test
  void authenticateModerator() {
    when(authenticatedUser.getUserRole())
        .thenReturn(UserRole.ADMIN)
        .thenReturn(UserRole.MODERATOR)
        .thenReturn(UserRole.PLAYER)
        .thenReturn(UserRole.ANONYMOUS);

    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.MODERATOR), is(true));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.MODERATOR), is(true));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.MODERATOR), is(false));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.MODERATOR), is(false));
  }

  @Test
  void authenticatePlayer() {
    when(authenticatedUser.getUserRole())
        .thenReturn(UserRole.ADMIN)
        .thenReturn(UserRole.MODERATOR)
        .thenReturn(UserRole.PLAYER)
        .thenReturn(UserRole.ANONYMOUS);

    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.PLAYER), is(true));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.PLAYER), is(true));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.PLAYER), is(true));
    // on fourth iteration, role is anonymous, not authorized
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.PLAYER), is(false));
  }

  @Test
  void authenticateAnonymous() {
    when(authenticatedUser.getUserRole())
        .thenReturn(UserRole.ADMIN)
        .thenReturn(UserRole.MODERATOR)
        .thenReturn(UserRole.PLAYER)
        .thenReturn(UserRole.ANONYMOUS);

    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.ANONYMOUS), is(true));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.ANONYMOUS), is(true));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.ANONYMOUS), is(true));
    assertThat(roleAuthorizer.authorize(authenticatedUser, UserRole.ANONYMOUS), is(true));
  }
}
