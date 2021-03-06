package org.triplea.spitfire.server.controllers;

import com.github.database.rider.core.api.dataset.DataSet;
import java.net.URI;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.spitfire.server.AllowedUserRole;
import org.triplea.spitfire.server.ProtectedEndpointTest;
import org.triplea.spitfire.server.SpitfireServerTestExtension;

@SuppressWarnings("UnmatchedTest")
@Disabled
@DataSet(
    value = SpitfireServerTestExtension.LOBBY_USER_DATASET + ", integration/banned_user.yml",
    useSequenceFiltering = false)
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
