package org.triplea.spitfire.server.controllers;

import com.github.database.rider.core.api.dataset.DataSet;
import java.net.URI;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.management.ToolboxModeratorManagementClient;
import org.triplea.spitfire.server.AllowedUserRole;
import org.triplea.spitfire.server.ProtectedEndpointTest;
import org.triplea.spitfire.server.SpitfireServerTestExtension;

@SuppressWarnings("UnmatchedTest")
@Disabled
@DataSet(value = SpitfireServerTestExtension.LOBBY_USER_DATASET, useSequenceFiltering = false)
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
