package org.triplea.server.moderator.toolbox.api.key.registration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.moderator.toolbox.ApiKeyPassword;
import org.triplea.http.client.moderator.toolbox.api.key.ToolboxApiKeyClient;
import org.triplea.http.client.moderator.toolbox.register.key.RegisterApiKeyResult;
import org.triplea.http.client.moderator.toolbox.register.key.ToolboxRegisterNewKeyClient;
import org.triplea.server.http.AbstractDropwizardTest;

class ApiKeyRegistrationControllerIntegrationTest extends AbstractDropwizardTest {

  private static final ToolboxRegisterNewKeyClient client =
      AbstractDropwizardTest.newClient(ToolboxRegisterNewKeyClient::newClient);

  private static final ToolboxApiKeyClient apiKeyClient =
      AbstractDropwizardTest.newClient(ToolboxApiKeyClient::newClient);

  @Test
  void registerApiKeySuccess() {
    apiKeyClient.clearLockouts();
    final RegisterApiKeyResult newApiKey =
        client.registerNewKey(ApiKeyPassword.builder().apiKey("test").password("test").build());

    assertThat(newApiKey.getNewApiKey(), notNullValue());
    assertThat(newApiKey.getErrorMessage(), nullValue());
  }

  @Test
  void registerApiKeySuccessFailure() {
    apiKeyClient.clearLockouts();
    final RegisterApiKeyResult newApiKey =
        client.registerNewKey(
            ApiKeyPassword.builder()
                .apiKey("invalid single use key")
                .password(
                    "this password is used to salt a new key, does not matter "
                        + "if single-use key is invalid")
                .build());

    assertThat(newApiKey.getNewApiKey(), nullValue());
    assertThat(newApiKey.getErrorMessage(), notNullValue());
  }
}
