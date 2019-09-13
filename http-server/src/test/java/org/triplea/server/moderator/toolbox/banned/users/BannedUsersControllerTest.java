package org.triplea.server.moderator.toolbox.banned.users;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.triplea.server.moderator.toolbox.ControllerTestUtil.verifyResponse;

import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.banned.user.UserBanData;
import org.triplea.http.client.moderator.toolbox.banned.user.UserBanParams;

@ExtendWith(MockitoExtension.class)
class BannedUsersControllerTest {

  // TODO: Project#12 fix test expectation of MODERATOR_ID being hardcoded as zero
  private static final int MODERATOR_ID = 0;
  private static final String BAN_ID = "Aw, salty death!";

  @Mock private UserBanService bannedUsersService;

  @InjectMocks private UserBanController bannedUsersController;

  @Mock private HttpServletRequest request;

  @Mock private UserBanData bannedUserData;

  private final UserBanParams banUserParams =
      UserBanParams.builder()
          .hashedMac("Mainlands scream with urchin at the dead jamaica!")
          .hoursToBan(50)
          .ip("Hoist me ale, ye shiny sea-dog!")
          .username("Where is the mighty tobacco?")
          .build();

  @Test
  void getUserBans() {
    when(bannedUsersService.getBannedUsers()).thenReturn(Collections.singletonList(bannedUserData));

    final Response response = bannedUsersController.getUserBans(request);

    verifyResponse(response, Collections.singletonList(bannedUserData));
  }

  @Nested
  final class RemoveBanTest {

    @Test
    void removeUserBanFailureCase() {
      givenBanServiceRemoveBanResult(false);

      final Response response = bannedUsersController.removeUserBan(request, BAN_ID);

      assertThat(response.getStatus(), is(400));
    }

    @Test
    void removeUserBanSuccessCase() {
      givenBanServiceRemoveBanResult(true);

      final Response response = bannedUsersController.removeUserBan(request, BAN_ID);

      assertThat(response.getStatus(), is(200));
    }

    private void givenBanServiceRemoveBanResult(final boolean result) {
      when(bannedUsersService.removeUserBan(MODERATOR_ID, BAN_ID)).thenReturn(result);
    }
  }

  @Nested
  final class AddBanTest {

    @Test
    void addBanFailureCase() {
      givenBanServiceAddBanResult(false);

      final Response response = bannedUsersController.banUser(request, banUserParams);

      assertThat(response.getStatus(), is(400));
    }

    @Test
    void addBanSuccessCase() {
      givenBanServiceAddBanResult(true);

      final Response response = bannedUsersController.banUser(request, banUserParams);

      assertThat(response.getStatus(), is(200));
    }

    private void givenBanServiceAddBanResult(final boolean result) {
      when(bannedUsersService.banUser(MODERATOR_ID, banUserParams)).thenReturn(result);
    }
  }
}
