package org.triplea.server.moderator.toolbox.banned.names;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
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

@ExtendWith(MockitoExtension.class)
class UsernameBanControllerTest {
  // TODO: Project#12 fix test expectation of MODERATOR_ID being hardcoded as zero
  private static final int MODERATOR_ID = 0;
  private static final String USERNAME = "Well, sail me lubber, ye misty sun!";
  private static final List<UsernameBanData> banData = new ArrayList<>();

  @Mock private UsernameBanService bannedNamesService;

  @InjectMocks private UsernameBanController usernameBanController;

  @Mock private HttpServletRequest request;

  @Nested
  final class RemoveBannedUsernameTest {

    @Test
    void removeBannedUsername() {
      when(bannedNamesService.removeUsernameBan(MODERATOR_ID, USERNAME)).thenReturn(true);

      final Response response = usernameBanController.removeBannedUsername(request, USERNAME);
      assertThat(response.getStatus(), is(200));
    }

    @Test
    void removeBannedUsernameFailCase() {
      when(bannedNamesService.removeUsernameBan(MODERATOR_ID, USERNAME)).thenReturn(false);
      final Response response = usernameBanController.removeBannedUsername(request, USERNAME);
      assertThat(response.getStatus(), is(400));
    }
  }

  @Nested
  final class AddBannedUsernameTest {
    @Test
    void addBannedUsername() {
      when(bannedNamesService.addBannedUserName(MODERATOR_ID, USERNAME)).thenReturn(true);

      final Response response = usernameBanController.addBannedUsername(request, USERNAME);
      assertThat(response.getStatus(), is(200));
    }

    @Test
    void addBannedUsernameFailCase() {
      when(bannedNamesService.addBannedUserName(MODERATOR_ID, USERNAME)).thenReturn(false);

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
  }
}
