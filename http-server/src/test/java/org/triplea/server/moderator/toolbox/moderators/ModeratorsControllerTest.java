package org.triplea.server.moderator.toolbox.moderators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.triplea.server.moderator.toolbox.ControllerTestUtil.verifyResponse;

import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.moderator.management.ModeratorInfo;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class ModeratorsControllerTest {

  private static final String USERNAME = "The gibbet screams faith like an old cannibal.";
  private static final String MODERATOR_NAME = "Where is the lively cannibal?";

  // TODO: Project#12 Fix hardcoded user id
  private static final int USER_ID = 0;

  @Mock private ModeratorsService moderatorsService;

  @InjectMocks private ModeratorsController moderatorsController;

  @Mock private HttpServletRequest request;

  @Mock private ModeratorInfo moderatorInfo;

  @Test
  void checkUserExists() {
    when(moderatorsService.userExistsByName(USERNAME)).thenReturn(true);

    final Response response = moderatorsController.checkUserExists(request, USERNAME);

    verifyResponse(response, true);
  }

  @Test
  void getModerators() {
    when(moderatorsService.fetchModerators()).thenReturn(Collections.singletonList(moderatorInfo));

    final Response response = moderatorsController.getModerators(request);

    verifyResponse(response, Collections.singletonList(moderatorInfo));
  }

  // TODO: Project#12 re-enable test
  @Disabled
  @Test
  void isSuperModPositiveCase() {
    final Response response = moderatorsController.isSuperMod(request);

    verifyResponse(response, true);
  }

  @Test
  void isSuperModNegativeCase() {
    final Response response = moderatorsController.isSuperMod(request);

    verifyResponse(response, false);
  }

  @Test
  void removeMod() {
    final Response response = moderatorsController.removeMod(request, MODERATOR_NAME);

    assertThat(response.getStatus(), is(200));
    verify(moderatorsService).removeMod(USER_ID, MODERATOR_NAME);
  }

  @Test
  void setSuperMod() {
    final Response response = moderatorsController.setSuperMod(request, MODERATOR_NAME);

    assertThat(response.getStatus(), is(200));
    verify(moderatorsService).addSuperMod(USER_ID, MODERATOR_NAME);
  }

  @Test
  void addModerator() {
    final Response response = moderatorsController.addModerator(request, MODERATOR_NAME);

    assertThat(response.getStatus(), is(200));
    verify(moderatorsService).addModerator(USER_ID, MODERATOR_NAME);
  }
}
