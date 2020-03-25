package org.triplea.modules.moderation.moderators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.triplea.http.ControllerTestUtil.verifyResponse;

import java.util.List;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.data.UserRole;
import org.triplea.http.client.lobby.moderator.toolbox.management.ModeratorInfo;
import org.triplea.modules.TestData;
import org.triplea.modules.access.authentication.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class ModeratorsControllerTest {

  private static final String USERNAME = "The gibbet screams faith like an old cannibal.";
  private static final String MODERATOR_NAME = "Where is the lively cannibal?";
  private static final ModeratorInfo MODERATOR_INFO = ModeratorInfo.builder().name("name").build();

  private static final AuthenticatedUser AUTHENTICATED_USER = TestData.AUTHENTICATED_USER;

  @Mock private ModeratorsService moderatorsService;

  @InjectMocks private ModeratorsController moderatorsController;

  @Mock private AuthenticatedUser authenticatedUser;

  @Test
  void checkUserExists() {
    when(moderatorsService.userExistsByName(USERNAME)).thenReturn(true);

    final Response response = moderatorsController.checkUserExists(USERNAME);

    verifyResponse(response, true);
  }

  @Test
  void getModerators() {
    when(moderatorsService.fetchModerators()).thenReturn(List.of(MODERATOR_INFO));

    final Response response = moderatorsController.getModerators();

    verifyResponse(response, List.of(MODERATOR_INFO));
  }

  @Test
  void isSuperModPositiveCase() {
    when(authenticatedUser.getUserRole()).thenReturn(UserRole.ADMIN);

    final Response response = moderatorsController.isSuperMod(authenticatedUser);

    verifyResponse(response, true);
  }

  @Test
  void isSuperModNegativeCase() {
    when(authenticatedUser.getUserRole()).thenReturn(UserRole.MODERATOR);

    final Response response = moderatorsController.isSuperMod(authenticatedUser);

    verifyResponse(response, false);
  }

  @Test
  void removeMod() {
    final Response response = moderatorsController.removeMod(AUTHENTICATED_USER, MODERATOR_NAME);

    assertThat(response.getStatus(), is(200));

    verify(moderatorsService).removeMod(AUTHENTICATED_USER.getUserIdOrThrow(), MODERATOR_NAME);
  }

  @Test
  void setSuperMod() {
    final Response response = moderatorsController.setSuperMod(AUTHENTICATED_USER, MODERATOR_NAME);

    assertThat(response.getStatus(), is(200));
    verify(moderatorsService).addSuperMod(AUTHENTICATED_USER.getUserIdOrThrow(), MODERATOR_NAME);
  }

  @Test
  void addModerator() {
    final Response response = moderatorsController.addModerator(AUTHENTICATED_USER, MODERATOR_NAME);

    assertThat(response.getStatus(), is(200));
    verify(moderatorsService).addModerator(AUTHENTICATED_USER.getUserIdOrThrow(), MODERATOR_NAME);
  }
}
