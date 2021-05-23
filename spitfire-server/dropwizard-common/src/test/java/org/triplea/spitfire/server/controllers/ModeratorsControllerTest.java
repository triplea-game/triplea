package org.triplea.spitfire.server.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.http.client.lobby.moderator.toolbox.management.ModeratorInfo;
import org.triplea.modules.moderation.moderators.ModeratorsService;
import org.triplea.spitfire.server.TestData;
import org.triplea.spitfire.server.access.authentication.AuthenticatedUser;
import org.triplea.spitfire.server.controllers.lobby.moderation.ModeratorsController;

@SuppressWarnings("UnmatchedTest")
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

    ControllerTestUtil.verifyResponse(response, true);
  }

  @Test
  void getModerators() {
    when(moderatorsService.fetchModerators()).thenReturn(List.of(MODERATOR_INFO));

    final Response response = moderatorsController.getModerators();

    ControllerTestUtil.verifyResponse(response, List.of(MODERATOR_INFO));
  }

  @Test
  void isAdminPositiveCase() {
    when(authenticatedUser.getUserRole()).thenReturn(UserRole.ADMIN);

    final Response response = moderatorsController.isAdmin(authenticatedUser);

    ControllerTestUtil.verifyResponse(response, true);
  }

  @Test
  void isAdminNegativeCase() {
    when(authenticatedUser.getUserRole()).thenReturn(UserRole.MODERATOR);

    final Response response = moderatorsController.isAdmin(authenticatedUser);

    ControllerTestUtil.verifyResponse(response, false);
  }

  @Test
  void removeMod() {
    final Response response = moderatorsController.removeMod(AUTHENTICATED_USER, MODERATOR_NAME);

    assertThat(response.getStatus(), is(200));

    verify(moderatorsService).removeMod(AUTHENTICATED_USER.getUserIdOrThrow(), MODERATOR_NAME);
  }

  @Test
  void setAdmin() {
    final Response response = moderatorsController.setAdmin(AUTHENTICATED_USER, MODERATOR_NAME);

    assertThat(response.getStatus(), is(200));
    verify(moderatorsService).addAdmin(AUTHENTICATED_USER.getUserIdOrThrow(), MODERATOR_NAME);
  }

  @Test
  void addModerator() {
    final Response response = moderatorsController.addModerator(AUTHENTICATED_USER, MODERATOR_NAME);

    assertThat(response.getStatus(), is(200));
    verify(moderatorsService).addModerator(AUTHENTICATED_USER.getUserIdOrThrow(), MODERATOR_NAME);
  }
}
