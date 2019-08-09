package org.triplea.server.moderator.toolbox.api.key;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.triplea.server.moderator.toolbox.ControllerTestUtil.verifyResponse;

import java.util.Collections;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.moderator.toolbox.NewApiKey;
import org.triplea.http.client.moderator.toolbox.api.key.ApiKeyData;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationService;

@ExtendWith(MockitoExtension.class)
class ApiKeyControllerTest {

  private static final int MODERATOR_ID = 123;
  private static final NewApiKey NEW_KEY = new NewApiKey("new key value");
  private static final String PUBLIC_KEY_ID = "public key id";

  @Mock private ApiKeyValidationService apiKeyValidationService;
  @Mock private GenerateSingleUseKeyService generateSingleUseKeyService;
  @Mock private ApiKeyService apiKeyService;

  @InjectMocks private ApiKeyController apiKeyController;

  @Mock private HttpServletRequest request;
  @Mock private ApiKeyData apiKeyData;

  @Test
  void generateSingleUseKey() {
    when(apiKeyValidationService.lookupModeratorIdByApiKey(request)).thenReturn(MODERATOR_ID);
    when(generateSingleUseKeyService.generateSingleUseKey(MODERATOR_ID))
        .thenReturn(NEW_KEY.getApiKey());

    final Response response = apiKeyController.generateSingleUseKey(request);

    verifyResponse(response, NEW_KEY);
  }

  @Test
  void getApiKeys() {
    when(apiKeyValidationService.lookupModeratorIdByApiKey(request)).thenReturn(MODERATOR_ID);
    when(apiKeyService.getKeys(MODERATOR_ID)).thenReturn(Collections.singletonList(apiKeyData));

    final Response response = apiKeyController.getApiKeys(request);

    verifyResponse(response, Collections.singletonList(apiKeyData));
  }

  @Test
  void deleteApiKey() {
    final Response response = apiKeyController.deleteApiKey(request, PUBLIC_KEY_ID);

    assertThat(response.getStatus(), is(200));
    verify(apiKeyService).deleteKey(PUBLIC_KEY_ID);
    verify(apiKeyValidationService).verifyApiKey(request);
  }
}
