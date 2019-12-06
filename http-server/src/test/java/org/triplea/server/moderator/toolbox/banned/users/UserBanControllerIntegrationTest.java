package org.triplea.server.moderator.toolbox.banned.users;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.server.http.AllowedUserRole;
import org.triplea.server.http.ProtectedEndpointTest;

class UserBanControllerIntegrationTest extends ProtectedEndpointTest<ToolboxUserBanClient> {

  UserBanControllerIntegrationTest() {
    super(AllowedUserRole.MODERATOR, ToolboxUserBanClient::newClient);
  }

  @Test
  void getUserBans() {
    verifyEndpointReturningCollection(ToolboxUserBanClient::getUserBans);
  }

  @Test
  void removeUserBan() {
    verifyEndpoint(client -> client.removeUserBan("xyz"));
  }

  @Test
  void banUser() {
    verifyEndpoint(
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
