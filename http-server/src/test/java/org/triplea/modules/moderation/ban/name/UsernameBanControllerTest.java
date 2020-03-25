package org.triplea.modules.moderation.ban.name;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.ControllerTestUtil;
import org.triplea.http.client.lobby.moderator.toolbox.banned.name.UsernameBanData;
import org.triplea.modules.TestData;
import org.triplea.modules.access.authentication.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class UsernameBanControllerTest {
  private static final AuthenticatedUser AUTHENTICATED_USER = TestData.AUTHENTICATED_USER;
  private static final UsernameBanData USERNAME_BAN_DATA =
      UsernameBanData.builder().banDate(Instant.now()).bannedName("banned name").build();
  private static final String USERNAME = "Ho-ho-ho! halitosis of treasure.";

  @Mock private UsernameBanService bannedNamesService;

  @InjectMocks private UsernameBanController bannedUsernamesController;

  @Nested
  final class RemoveBannedUsername {
    @Test
    void failureCase() {
      givenRemoveBanResult(false);

      final Response response =
          bannedUsernamesController.removeBannedUsername(AUTHENTICATED_USER, USERNAME);

      assertThat(response.getStatus(), is(400));
    }

    @Test
    void successCase() {
      givenRemoveBanResult(true);

      final Response response =
          bannedUsernamesController.removeBannedUsername(AUTHENTICATED_USER, USERNAME);

      assertThat(response.getStatus(), is(200));
    }

    private void givenRemoveBanResult(final boolean result) {
      when(bannedNamesService.removeUsernameBan(AUTHENTICATED_USER.getUserId(), USERNAME))
          .thenReturn(result);
    }
  }

  @Nested
  final class AddBannedUsername {
    @Test
    void failureCase() {
      givenAddBanResult(false);

      final Response response =
          bannedUsernamesController.addBannedUsername(AUTHENTICATED_USER, USERNAME);

      assertThat(response.getStatus(), is(400));
    }

    @Test
    void addBannedUserName() {
      givenAddBanResult(true);

      final Response response =
          bannedUsernamesController.addBannedUsername(AUTHENTICATED_USER, USERNAME);

      assertThat(response.getStatus(), is(200));
    }

    private void givenAddBanResult(final boolean result) {
      when(bannedNamesService.addBannedUserName(AUTHENTICATED_USER.getUserId(), USERNAME))
          .thenReturn(result);
    }
  }

  @Test
  void getBannedUserNames() {
    when(bannedNamesService.getBannedUserNames()).thenReturn(List.of(USERNAME_BAN_DATA));

    final Response response = bannedUsernamesController.getBannedUsernames();

    ControllerTestUtil.verifyResponse(response, List.of(USERNAME_BAN_DATA));
  }
}
