package org.triplea.modules.moderation.ban.user;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.triplea.http.ControllerTestUtil.verifyResponse;

import java.util.List;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanData;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.modules.TestData;
import org.triplea.modules.access.authentication.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class UserBanControllerTest {

  private static final AuthenticatedUser AUTHENTICATED_USER = TestData.AUTHENTICATED_USER;
  private static final String BAN_ID = "Aw, salty death!";

  @Mock private UserBanService bannedUsersService;

  @InjectMocks private UserBanController bannedUsersController;

  @Mock private UserBanData bannedUserData;

  private final UserBanParams banUserParams =
      UserBanParams.builder()
          .systemId("Mainlands scream with urchin at the dead jamaica!")
          .minutesToBan(50)
          .ip("Hoist me ale, ye shiny sea-dog!")
          .username("Where is the mighty tobacco?")
          .build();

  @Test
  void getUserBans() {
    when(bannedUsersService.getBannedUsers()).thenReturn(List.of(bannedUserData));

    final Response response = bannedUsersController.getUserBans();

    verifyResponse(response, List.of(bannedUserData));
  }

  @Nested
  final class RemoveBanTest {

    @Test
    void removeUserBanFailureCase() {
      givenBanServiceRemoveBanResult(false);

      final Response response = bannedUsersController.removeUserBan(AUTHENTICATED_USER, BAN_ID);

      assertThat(response.getStatus(), is(400));
    }

    @Test
    void removeUserBanSuccessCase() {
      givenBanServiceRemoveBanResult(true);

      final Response response = bannedUsersController.removeUserBan(AUTHENTICATED_USER, BAN_ID);

      assertThat(response.getStatus(), is(200));
    }

    private void givenBanServiceRemoveBanResult(final boolean result) {
      when(bannedUsersService.removeUserBan(AUTHENTICATED_USER.getUserId(), BAN_ID))
          .thenReturn(result);
    }
  }

  @Nested
  final class AddBanTest {
    @Test
    void addBan() {
      final Response response = bannedUsersController.banUser(AUTHENTICATED_USER, banUserParams);

      assertThat(response.getStatus(), is(200));
    }
  }
}
