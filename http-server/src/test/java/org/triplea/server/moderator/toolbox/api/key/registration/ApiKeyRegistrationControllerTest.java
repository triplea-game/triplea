package org.triplea.server.moderator.toolbox.api.key.registration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.ToolboxHttpHeaders;
import org.triplea.http.client.moderator.toolbox.register.key.RegisterApiKeyParam;
import org.triplea.http.client.moderator.toolbox.register.key.RegisterApiKeyResult;
import org.triplea.server.moderator.toolbox.api.key.exception.IncorrectApiKeyException;


@ExtendWith(MockitoExtension.class)
class ApiKeyRegistrationControllerTest {

  private static final String NEW_KEY = "Blow the galley until it hobbles.";

  @Mock
  private ApiKeyRegistrationService apiKeyRegistrationService;

  @InjectMocks
  private ApiKeyRegistrationController apiKeyRegistrationController;

  @Mock
  private HttpServletRequest httpServletRequest;

  private RegisterApiKeyParam registerApiKeyParam = RegisterApiKeyParam.builder()
      .singleUseKey("Parrot of a misty fortune, trade the riddle!")
      .newPassword("Seashell of a real life, fire the yellow fever!")
      .build();

  @Test
  void registerApiKeySuccessCase() {
    when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER))
        .thenReturn(registerApiKeyParam.getNewPassword());
    when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_HEADER))
        .thenReturn(registerApiKeyParam.getSingleUseKey());
    when(apiKeyRegistrationService.registerKey(
        httpServletRequest, registerApiKeyParam.getSingleUseKey(), registerApiKeyParam.getNewPassword()))
            .thenReturn(NEW_KEY);

    final Response response =
        apiKeyRegistrationController.registerApiKey(httpServletRequest);

    assertThat(response.getStatus(), is(200));
    assertThat(response.getEntity(), is(RegisterApiKeyResult.newApiKeyResult(NEW_KEY)));
  }

  @Test
  void registerApiKeyFailureCase() {
    when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_PASSWORD_HEADER))
        .thenReturn(registerApiKeyParam.getNewPassword());
    when(httpServletRequest.getHeader(ToolboxHttpHeaders.API_KEY_HEADER))
        .thenReturn(registerApiKeyParam.getSingleUseKey());
    when(apiKeyRegistrationService.registerKey(
        httpServletRequest, registerApiKeyParam.getSingleUseKey(), registerApiKeyParam.getNewPassword()))
            .thenThrow(new IncorrectApiKeyException());

    final Response response =
        apiKeyRegistrationController.registerApiKey(httpServletRequest);

    assertThat(response.getStatus(), is(400));
    assertThat(
        response.getEntity(),
        is(RegisterApiKeyResult.newErrorResult(new IncorrectApiKeyException().getMessage())));
  }
}
