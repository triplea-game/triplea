package org.triplea.modules.moderation.ban.user;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.ProtectedEndpointTest;

class UserBanControllerIntegrationTest extends ProtectedEndpointTest<ToolboxUserBanClient> {

  UserBanControllerIntegrationTest(final URI localhost) {
    super(localhost, AllowedUserRole.MODERATOR, ToolboxUserBanClient::newClient);
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
                    .minutesToBan(10)
                    .systemId("$1$AA$AA7qDBliIofq8jOm4nMBB/")
                    .ip("2.2.2.2")
                    .username("name")
                    .build()));
  }
}
