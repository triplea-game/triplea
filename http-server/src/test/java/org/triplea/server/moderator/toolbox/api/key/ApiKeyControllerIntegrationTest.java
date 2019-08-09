package org.triplea.server.moderator.toolbox.api.key;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.HttpInteractionException;
import org.triplea.http.client.moderator.toolbox.api.key.ToolboxApiKeyClient;
import org.triplea.server.http.AbstractDropwizardTest;

class ApiKeyControllerIntegrationTest extends AbstractDropwizardTest {

  private static final ToolboxApiKeyClient client =
      AbstractDropwizardTest.newClient(ToolboxApiKeyClient::newClient);

  private static final ToolboxApiKeyClient clientWithBadKey =
      AbstractDropwizardTest.newClientWithInvalidCreds(ToolboxApiKeyClient::newClient);

  @Test
  void generateSingleUseKey() {
    assertThat(client.generateSingleUseKey().getApiKey(), notNullValue());
  }

  @Test
  void generateSingleUseKeyNotAuthorized() {
    assertThrows(HttpInteractionException.class, clientWithBadKey::generateSingleUseKey);
  }

  @Test
  void getApiKeys() {
    assertThat(client.getApiKeys(), not(empty()));
  }

  @Test
  void getApiKeysNotAuthorized() {
    assertThrows(HttpInteractionException.class, clientWithBadKey::getApiKeys);
  }

  @Test
  void deleteApiKey() {
    client.deleteApiKey("123-xyz");
  }

  @Test
  void deleteApiKeyNotAuthorized() {
    assertThrows(HttpInteractionException.class, () -> clientWithBadKey.deleteApiKey("abc"));
  }
}
