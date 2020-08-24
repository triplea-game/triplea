package org.triplea.modules.moderation.ban.name;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.ProtectedEndpointTest;

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
