package org.triplea.spitfire.server.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.management.ToolboxModeratorManagementClient;
import org.triplea.spitfire.server.ControllerIntegrationTest;

class ModeratorsControllerIntegrationTest extends ControllerIntegrationTest {
  private final URI localhost;
  private final ToolboxModeratorManagementClient playerClient;
  private final ToolboxModeratorManagementClient moderatorClient;
  private final ToolboxModeratorManagementClient adminClient;

  ModeratorsControllerIntegrationTest(final URI localhost) {
    this.localhost = localhost;
    this.playerClient =
        ToolboxModeratorManagementClient.newClient(localhost, ControllerIntegrationTest.PLAYER);
    this.moderatorClient =
        ToolboxModeratorManagementClient.newClient(localhost, ControllerIntegrationTest.MODERATOR);
    this.adminClient =
        ToolboxModeratorManagementClient.newClient(localhost, ControllerIntegrationTest.ADMIN);
  }

  @SuppressWarnings("unchecked")
  @Test
  void mustBeAuthorized() {
    assertNotAuthorized(
        List.of(ControllerIntegrationTest.PLAYER, ControllerIntegrationTest.MODERATOR),
        apiKey -> ToolboxModeratorManagementClient.newClient(localhost, apiKey),
        client -> client.addAdmin("admin"),
        client -> client.addModerator("mod"),
        client -> client.removeMod("mod"));
  }

  @Test
  void isAdmin() {
    assertThat(playerClient.isCurrentUserAdmin(), is(false));
    assertThat(moderatorClient.isCurrentUserAdmin(), is(false));
    assertThat(adminClient.isCurrentUserAdmin(), is(true));
  }

  @Test
  void removeMod() {
    adminClient.removeMod("mod");
  }

  @Test
  void addMod() {
    adminClient.addModerator("mod");
  }

  @Test
  void setAdmin() {
    adminClient.addAdmin("admin");
  }
}
