package org.triplea.server.moderator.toolbox.banned.names;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.server.http.ProtectedEndpointTest;

class UsernameBanControllerIntegrationTest extends ProtectedEndpointTest<ToolboxUsernameBanClient> {

  UsernameBanControllerIntegrationTest() {
    super(ToolboxUsernameBanClient::newClient);
  }

  @Test
  void removeBannedUsername() {
    verifyEndpointReturningVoid(client -> client.removeUsernameBan("not nice"));
  }

  @Test
  void addBannedUsername() {
    verifyEndpointReturningVoid(client -> client.addUsernameBan("new bad name"));
  }

  @Test
  void getBannedUsernames() {
    verifyEndpointReturningCollection(ToolboxUsernameBanClient::getUsernameBans);
  }
}
