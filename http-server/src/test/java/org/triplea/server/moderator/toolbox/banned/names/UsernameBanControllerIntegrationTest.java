package org.triplea.server.moderator.toolbox.banned.names;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.server.http.AllowedUserRole;
import org.triplea.server.http.ProtectedEndpointTest;

class UsernameBanControllerIntegrationTest extends ProtectedEndpointTest<ToolboxUsernameBanClient> {

  UsernameBanControllerIntegrationTest() {
    super(AllowedUserRole.MODERATOR, ToolboxUsernameBanClient::newClient);
  }

  @Test
  void removeBannedUsername() {
    verifyEndpoint(client -> client.removeUsernameBan("not nice"));
  }

  @Test
  void addBannedUsername() {
    verifyEndpoint(client -> client.addUsernameBan("new bad name " + Math.random()));
  }

  @Test
  void getBannedUsernames() {
    verifyEndpointReturningCollection(ToolboxUsernameBanClient::getUsernameBans);
  }
}
