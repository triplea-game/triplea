package org.triplea.server.moderator.toolbox.api.key.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;

@ExtendWith(MockitoExtension.class)
class ApiKeyValidationControllerTest {

  @Mock
  private ApiKeySecurityService apiKeySecurityService;
  @Mock
  private ApiKeyValidationService apiKeyValidationService;
  @InjectMocks
  private ApiKeyValidationController apiKeyValidationController;

  @Mock
  private HttpServletRequest request;

  @Test
  void validateApiKeyLockOutCase() {
    when(apiKeySecurityService.allowValidation(request)).thenReturn(false);

    final Response response = apiKeyValidationController.validateApiKey(request);

    assertThat(response.getStatus(), is(ApiKeyValidationService.LOCK_OUT_RESPONSE.getStatus()));
    assertThat(response.getEntity(), is(ApiKeyValidationService.LOCK_OUT_RESPONSE.getEntity()));

    verify(apiKeyValidationService, never()).lookupModeratorIdByApiKey(any());
  }

  @Test
  void validateApiKeySuccessCase() {
    when(apiKeySecurityService.allowValidation(request)).thenReturn(true);
    when(apiKeyValidationService.lookupModeratorIdByApiKey(request)).thenReturn(Optional.of(1));

    final Response response = apiKeyValidationController.validateApiKey(request);

    assertThat(response.getStatus(), is(200));
    assertThat(response.getEntity(), is(ModeratorToolboxClient.SUCCESS));
  }

  @Test
  void validateApiKeyFailureCase() {
    when(apiKeySecurityService.allowValidation(request)).thenReturn(true);
    when(apiKeyValidationService.lookupModeratorIdByApiKey(request)).thenReturn(Optional.empty());

    final Response response = apiKeyValidationController.validateApiKey(request);

    assertThat(response.getStatus(), is(ApiKeyValidationService.API_KEY_NOT_FOUND_RESPONSE.getStatus()));
    assertThat(response.getEntity(), is(ApiKeyValidationService.API_KEY_NOT_FOUND_RESPONSE.getEntity()));
  }


}
