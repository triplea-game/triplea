package org.triplea.server.user.account;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.lobby.user.account.FetchEmailResponse;
import org.triplea.lobby.server.db.data.UserRole;
import org.triplea.server.access.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class UserAccountControllerTest {

  private static final AuthenticatedUser AUTHENTICATED_USER =
      AuthenticatedUser.builder().userId(10).userRole(UserRole.PLAYER).build();
  private static final String NEW_PASSWORD = "new-password-value";

  private static final String EMAIL = "email-value";

  @Mock private UserAccountService userAccountService;

  private UserAccountController userAccountController;

  @BeforeEach
  void setup() {
    userAccountController =
        UserAccountController.builder().userAccountService(userAccountService).build();
  }

  @Test
  void changePassword() {
    userAccountController.changePassword(AUTHENTICATED_USER, NEW_PASSWORD);

    verify(userAccountService).changePassword(AUTHENTICATED_USER.getUserIdOrThrow(), NEW_PASSWORD);
  }

  @Test
  void fetchEmail() {
    when(userAccountService.fetchEmail(AUTHENTICATED_USER.getUserIdOrThrow())).thenReturn(EMAIL);

    final FetchEmailResponse response = userAccountController.fetchEmail(AUTHENTICATED_USER);

    assertThat(response, is(new FetchEmailResponse(EMAIL)));
  }

  @Test
  void changeEmail() {
    userAccountController.changeEmail(AUTHENTICATED_USER, EMAIL);

    verify(userAccountService).changeEmail(AUTHENTICATED_USER.getUserIdOrThrow(), EMAIL);
  }
}
