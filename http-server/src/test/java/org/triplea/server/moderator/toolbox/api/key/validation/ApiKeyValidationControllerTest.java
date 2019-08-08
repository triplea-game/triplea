package org.triplea.server.moderator.toolbox.api.key.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.server.moderator.toolbox.api.key.exception.ApiKeyLockOutException;

@ExtendWith(MockitoExtension.class)
class ApiKeyValidationControllerTest {

  @Mock private ApiKeyValidationService apiKeyValidationService;
  @InjectMocks private ApiKeyValidationController apiKeyValidationController;

  @Mock private HttpServletRequest request;

  @Test
  void validateApiKeySuccessCase() {
    final Response response = apiKeyValidationController.validateApiKey(request);

    assertThat(response.getStatus(), is(200));

    verify(apiKeyValidationService).verifyApiKey(request);
  }

  @Test
  void validateApiKeyFailureCase() {
    doThrow(new ApiKeyLockOutException()).when(apiKeyValidationService).verifyApiKey(request);

    assertThrows(
        ApiKeyLockOutException.class, () -> apiKeyValidationController.validateApiKey(request));
  }

  @Test
  void clearLockouts() {
    apiKeyValidationController.clearLockouts(request);
    verify(apiKeyValidationService).clearLockoutCache(request);
  }
}
