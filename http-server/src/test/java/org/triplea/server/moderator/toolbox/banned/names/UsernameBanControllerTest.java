package org.triplea.server.moderator.toolbox.banned.names;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.banned.name.UsernameBanData;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationService;

@ExtendWith(MockitoExtension.class)
class UsernameBanControllerTest {
  private static final int MODERATOR_ID = 123;
  private static final String USERNAME = "Well, sail me lubber, ye misty sun!";
  private static final List<UsernameBanData> banData = new ArrayList<>();

  @Mock
  private UsernameBanService bannedNamesService;
  @Mock
  private ApiKeyValidationService apiKeyValidationService;

  @InjectMocks
  private UsernameBanController usernameBanController;

  @Mock
  private HttpServletRequest request;

  @Nested
  final class RemoveBannedUsernameTest {

    @Test
    void removeBannedUsername() {
      when(apiKeyValidationService.lookupModeratorIdByApiKey(request))
          .thenReturn(MODERATOR_ID);
      when(bannedNamesService.removeUsernameBan(MODERATOR_ID, USERNAME))
          .thenReturn(true);

      final Response response = usernameBanController.removeBannedUsername(request, USERNAME);
      assertThat(response.getStatus(), is(200));
    }

    @Test
    void removeBannedUsernameFailCase() {
      when(apiKeyValidationService.lookupModeratorIdByApiKey(request))
          .thenReturn(MODERATOR_ID);
      when(bannedNamesService.removeUsernameBan(MODERATOR_ID, USERNAME))
          .thenReturn(false);
      final Response response = usernameBanController.removeBannedUsername(request, USERNAME);
      assertThat(response.getStatus(), is(400));
    }
  }

  @Nested
  final class AddBannedUsernameTest {
    @Test
    void addBannedUsername() {
      when(apiKeyValidationService.lookupModeratorIdByApiKey(request))
          .thenReturn(MODERATOR_ID);
      when(bannedNamesService.addBannedUserName(MODERATOR_ID, USERNAME))
          .thenReturn(true);

      final Response response = usernameBanController.addBannedUsername(request, USERNAME);
      assertThat(response.getStatus(), is(200));
    }

    @Test
    void addBannedUsernameFailCase() {
      when(apiKeyValidationService.lookupModeratorIdByApiKey(request))
          .thenReturn(MODERATOR_ID);
      when(bannedNamesService.addBannedUserName(MODERATOR_ID, USERNAME))
          .thenReturn(false);

      final Response response = usernameBanController.addBannedUsername(request, USERNAME);
      assertThat(response.getStatus(), is(400));
    }
  }

  @Test
  void getBannedUsernames() {
    when(bannedNamesService.getBannedUserNames()).thenReturn(banData);

    final Response response = usernameBanController.getBannedUsernames(request);

    assertThat(response.getStatus(), is(200));
    assertThat(response.getEntity(), is(banData));
    verify(apiKeyValidationService).verifyApiKey(request);
  }
}
