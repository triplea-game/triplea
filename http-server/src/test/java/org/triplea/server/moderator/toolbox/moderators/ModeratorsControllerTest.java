package org.triplea.server.moderator.toolbox.moderators;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.triplea.server.moderator.toolbox.ControllerTestUtil.verifyResponse;

import java.util.Collections;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.NewApiKey;
import org.triplea.http.client.moderator.toolbox.moderator.management.ModeratorInfo;
import org.triplea.server.moderator.toolbox.api.key.GenerateSingleUseKeyService;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationService;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class ModeratorsControllerTest {

  private static final String USERNAME = "The gibbet screams faith like an old cannibal.";
  private static final String MODERATOR_NAME = "Where is the lively cannibal?";
  private static final String API_KEY = "Parrots grow with fight!";
  private static final int USER_ID = 1235;

  @Mock private ModeratorsService moderatorsService;
  @Mock private GenerateSingleUseKeyService generateSingleUseKeyService;
  @Mock private ApiKeyValidationService apiKeyValidationService;

  @InjectMocks private ModeratorsController moderatorsController;

  @Mock private HttpServletRequest request;

  @Mock private ModeratorInfo moderatorInfo;

  @Test
  void checkUserExists() {
    when(moderatorsService.userExistsByName(USERNAME)).thenReturn(true);

    final Response response = moderatorsController.checkUserExists(request, USERNAME);

    verifyResponse(response, true);
    verify(apiKeyValidationService).verifyApiKey(request);
  }

  @Test
  void getModerators() {
    when(moderatorsService.fetchModerators()).thenReturn(Collections.singletonList(moderatorInfo));

    final Response response = moderatorsController.getModerators(request);

    verifyResponse(response, Collections.singletonList(moderatorInfo));
    verify(apiKeyValidationService).verifyApiKey(request);
  }

  @Test
  void isSuperModPositiveCase() {
    when(apiKeyValidationService.lookupSuperModByApiKey(request)).thenReturn(Optional.of(USER_ID));

    final Response response = moderatorsController.isSuperMod(request);

    verifyResponse(response, true);
    verify(apiKeyValidationService).verifyApiKey(request);
  }

  @Test
  void isSuperModNegativeCase() {
    when(apiKeyValidationService.lookupSuperModByApiKey(request)).thenReturn(Optional.empty());

    final Response response = moderatorsController.isSuperMod(request);

    verifyResponse(response, false);
    verify(apiKeyValidationService).verifyApiKey(request);
  }

  @Test
  void generateSingleUseKey() {
    when(generateSingleUseKeyService.generateSingleUseKey(MODERATOR_NAME)).thenReturn(API_KEY);

    final Response response = moderatorsController.generateSingleUseKey(request, MODERATOR_NAME);

    verifyResponse(response, new NewApiKey(API_KEY));
    verify(apiKeyValidationService).verifySuperMod(request);
  }

  @Test
  void removeMod() {
    when(apiKeyValidationService.verifySuperMod(request)).thenReturn(USER_ID);

    final Response response = moderatorsController.removeMod(request, MODERATOR_NAME);

    assertThat(response.getStatus(), is(200));
    verify(moderatorsService).removeMod(USER_ID, MODERATOR_NAME);
  }

  @Test
  void setSuperMod() {
    when(apiKeyValidationService.verifySuperMod(request)).thenReturn(USER_ID);

    final Response response = moderatorsController.setSuperMod(request, MODERATOR_NAME);

    assertThat(response.getStatus(), is(200));
    verify(moderatorsService).addSuperMod(USER_ID, MODERATOR_NAME);
  }

  @Test
  void addModerator() {
    when(apiKeyValidationService.verifySuperMod(request)).thenReturn(USER_ID);

    final Response response = moderatorsController.addModerator(request, MODERATOR_NAME);

    assertThat(response.getStatus(), is(200));
    verify(moderatorsService).addModerator(USER_ID, MODERATOR_NAME);
  }
}
