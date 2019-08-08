package org.triplea.server.moderator.toolbox.banned.names;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.banned.name.UsernameBanData;
import org.triplea.server.moderator.toolbox.ControllerTestUtil;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationService;

@ExtendWith(MockitoExtension.class)
class BannedUsernamesControllerTest {

  private static final int MODERATOR_ID = 8878;
  private static final String USERNAME = "Ho-ho-ho! halitosis of treasure.";
  @Mock private UsernameBanService bannedNamesService;
  @Mock private ApiKeyValidationService apiKeyValidationService;

  @InjectMocks private UsernameBanController bannedUsernamesController;

  @Mock private HttpServletRequest request;

  @Mock private UsernameBanData bannedUsernameData;

  @Nested
  final class RemoveBannedUsername {
    @Test
    void failureCase() {
      givenRemoveBanResult(false);

      final Response response = bannedUsernamesController.removeBannedUsername(request, USERNAME);

      assertThat(response.getStatus(), is(400));
    }

    @Test
    void successCase() {
      givenRemoveBanResult(true);

      final Response response = bannedUsernamesController.removeBannedUsername(request, USERNAME);

      assertThat(response.getStatus(), is(200));
    }

    private void givenRemoveBanResult(final boolean result) {
      when(apiKeyValidationService.lookupModeratorIdByApiKey(request)).thenReturn(MODERATOR_ID);
      when(bannedNamesService.removeUsernameBan(MODERATOR_ID, USERNAME)).thenReturn(result);
    }
  }

  @Nested
  final class AddBannedUsername {
    @Test
    void failureCase() {
      givenAddBanResult(false);

      final Response response = bannedUsernamesController.addBannedUsername(request, USERNAME);

      assertThat(response.getStatus(), is(400));
    }

    @Test
    void addBannedUserName() {
      givenAddBanResult(true);

      final Response response = bannedUsernamesController.addBannedUsername(request, USERNAME);

      assertThat(response.getStatus(), is(200));
    }

    private void givenAddBanResult(final boolean result) {
      when(apiKeyValidationService.lookupModeratorIdByApiKey(request)).thenReturn(MODERATOR_ID);
      when(bannedNamesService.addBannedUserName(MODERATOR_ID, USERNAME)).thenReturn(result);
    }
  }

  @Test
  void getBannedUserNames() {
    when(bannedNamesService.getBannedUserNames())
        .thenReturn(Collections.singletonList(bannedUsernameData));

    final Response response = bannedUsernamesController.getBannedUsernames(request);

    ControllerTestUtil.verifyResponse(response, Collections.singletonList(bannedUsernameData));
    verify(apiKeyValidationService).verifyApiKey(request);
  }
}
