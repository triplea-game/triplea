package org.triplea.modules.moderation.moderators;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.management.ToolboxModeratorManagementClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.ProtectedEndpointTest;

class ModeratorsControllerIntegrationTest
    extends ProtectedEndpointTest<ToolboxModeratorManagementClient> {
  ModeratorsControllerIntegrationTest() {
    super(AllowedUserRole.MODERATOR, ToolboxModeratorManagementClient::newClient);
  }

  @Test
  void isSuperMod() {
    verifyEndpoint(ToolboxModeratorManagementClient::isCurrentUserSuperMod);
  }

  @Test
  void removeMod() {
    verifyEndpoint(AllowedUserRole.ADMIN, client -> client.removeMod("mod"));
  }

  @Test
  void setSuperMod() {
    verifyEndpoint(AllowedUserRole.ADMIN, client -> client.addSuperMod("mod3"));
  }
}
