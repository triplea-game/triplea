package org.triplea.server.moderator.toolbox.api.key.validation;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.moderator.toolbox.api.key.ToolboxApiKeyClient;
import org.triplea.server.http.AbstractDropwizardTest;

class ApiKeyValidationControllerIntegrationTest extends AbstractDropwizardTest {

  @Test
  void validateKeySuccess() {
    final ToolboxApiKeyClient client = AbstractDropwizardTest.newClient(ToolboxApiKeyClient::newClient);

    client.clearLockouts();
    assertThat(
        "Expecting no error message on success case",
        client.validateApiKey(),
        isEmpty());
  }

  @Test
  void validKeyBadKeyFailure() {
    final ToolboxApiKeyClient client = AbstractDropwizardTest.newClientWithInvalidCreds(ToolboxApiKeyClient::newClient);

    assertThat(
        "Expecting an error message on failure case",
        client.validateApiKey(),
        isPresent());
  }
}
