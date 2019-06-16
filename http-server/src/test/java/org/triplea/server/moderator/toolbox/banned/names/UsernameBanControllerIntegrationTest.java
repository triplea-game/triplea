package org.triplea.server.moderator.toolbox.banned.names;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.HttpInteractionException;
import org.triplea.http.client.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.server.http.AbstractDropwizardTest;

class UsernameBanControllerIntegrationTest extends AbstractDropwizardTest {

  private static final ToolboxUsernameBanClient client =
      AbstractDropwizardTest.newClient(ToolboxUsernameBanClient::newClient);

  private static final ToolboxUsernameBanClient clientWithBadKey =
      AbstractDropwizardTest.newClientWithInvalidCreds(ToolboxUsernameBanClient::newClient);

  @Test
  void removeBannedUsername() {
    client.removeUsernameBan("not nice");
  }

  @Test
  void removeBannedUsernameNotAuthorized() {
    // username ban exists, but because keys are bad we'll get an exception
    assertThrows(HttpInteractionException.class, () -> clientWithBadKey.removeUsernameBan("bad"));
  }

  @Test
  void addBannedUsername() {
    client.addUsernameBan("new bad name");
  }

  @Test
  void addBannedUsernameNotAuthorized() {
    assertThrows(HttpInteractionException.class, () -> clientWithBadKey.addUsernameBan("name to ban"));
  }

  @Test
  void getBannedUsernames() {
    assertThat(client.getUsernameBans(), not(empty()));
  }

  @Test
  void getBannedUsernamesNotAuthorized() {
    assertThrows(HttpInteractionException.class, clientWithBadKey::getUsernameBans);
  }
}
