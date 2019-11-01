package org.triplea.server.moderator.toolbox.banned.users;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.server.http.ProtectedEndpointTest;

class UserBanControllerIntegrationTest extends ProtectedEndpointTest<ToolboxUserBanClient> {

  UserBanControllerIntegrationTest() {
    super(ToolboxUserBanClient::newClient);
  }

  @Test
  void getUserBans() {
    verifyEndpointReturningCollection(ToolboxUserBanClient::getUserBans);
  }

  @Test
  void removeUserBan() {
    verifyEndpointReturningVoid(client -> client.removeUserBan("xyz"));
  }

  @Test
  void banUser() {
    verifyEndpointReturningVoid(
        client ->
            client.banUser(
                UserBanParams.builder()
                    .hoursToBan(10)
                    .systemId("$1$AA$AA7qDBliIofq8jOm4nMBB/")
                    .ip("2.2.2.2")
                    .username("name")
                    .build()));
  }
}
