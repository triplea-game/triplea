package org.triplea.server.moderator.toolbox.moderators;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.management.ToolboxModeratorManagementClient;
import org.triplea.server.http.AuthenticatedEndpointTest;

class ModeratorsControllerIntegrationTest
    extends AuthenticatedEndpointTest<ToolboxModeratorManagementClient> {
  ModeratorsControllerIntegrationTest() {
    super(ToolboxModeratorManagementClient::newClient);
  }

  @Test
  void isSuperMod() {
    verifyEndpointReturningObject(ToolboxModeratorManagementClient::isCurrentUserSuperMod);
  }

  @Test
  void removeMod() {
    verifyEndpointReturningVoid(client -> client.removeMod("mod"));
  }

  @Test
  void setSuperMod() {
    verifyEndpointReturningVoid(client -> client.addSuperMod("mod3"));
  }
}
