package org.triplea.modules.moderation.moderators;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.management.ToolboxModeratorManagementClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.ProtectedEndpointTest;

class ModeratorsControllerIntegrationTest
    extends ProtectedEndpointTest<ToolboxModeratorManagementClient> {

  ModeratorsControllerIntegrationTest(final URI localhost) {
    super(localhost, AllowedUserRole.MODERATOR, ToolboxModeratorManagementClient::newClient);
  }

  @Test
  void isAdmin() {
    verifyEndpoint(ToolboxModeratorManagementClient::isCurrentUserAdmin);
  }

  @Test
  void removeMod() {
    verifyEndpoint(AllowedUserRole.ADMIN, client -> client.removeMod("mod"));
  }

  @Test
  void setAdmin() {
    verifyEndpoint(AllowedUserRole.ADMIN, client -> client.addAdmin("mod3"));
  }
}
