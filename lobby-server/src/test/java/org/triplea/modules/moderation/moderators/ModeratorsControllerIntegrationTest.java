package org.triplea.modules.moderation.moderators;

import com.github.database.rider.core.api.dataset.DataSet;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.management.ToolboxModeratorManagementClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.LobbyServerTest;
import org.triplea.modules.http.ProtectedEndpointTest;

@DataSet(value = LobbyServerTest.LOBBY_USER_DATASET, useSequenceFiltering = false)
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
