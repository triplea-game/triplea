package org.triplea.spitfire.server.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.triplea.test.common.matchers.CollectionMatchers.containsMappedItem;
import static org.triplea.test.common.matchers.CollectionMatchers.doesNotContainMappedItem;

import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanData;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.spitfire.server.ControllerIntegrationTest;

class UserBanControllerIntegrationTest extends ControllerIntegrationTest {

  private final URI localhost;
  private final ToolboxUserBanClient client;

  UserBanControllerIntegrationTest(final URI localhost) {
    this.localhost = localhost;
    client = ToolboxUserBanClient.newClient(localhost, ControllerIntegrationTest.MODERATOR);
  }

  @SuppressWarnings("unchecked")
  @Test
  void mustBeAuthorized() {
    assertNotAuthorized(
        ControllerIntegrationTest.NOT_MODERATORS,
        apiKey -> ToolboxUserBanClient.newClient(localhost, apiKey),
        ToolboxUserBanClient::getUserBans,
        client ->
            client.banUser(
                UserBanParams.builder()
                    .ip("ip")
                    .minutesToBan(10)
                    .systemId("system-id")
                    .username("username")
                    .build()),
        client -> client.removeUserBan("some-username"));
  }

  @Test
  void listUserBans() {
    assertThat(client.getUserBans(), is(not(empty())));
  }

  /** Get list of banned users. Unban the first item. */
  @Test
  void removeUserNameBan() {
    final UserBanData firstItem = client.getUserBans().get(0);

    assertThat(
        client.getUserBans(), containsMappedItem(UserBanData::getBanId, firstItem.getBanId()));

    client.removeUserBan(firstItem.getBanId());

    assertThat(
        client.getUserBans(),
        doesNotContainMappedItem(UserBanData::getBanId, firstItem.getBanId()));
  }

  /**
   * Generate a mostly unique user name. <br>
   * Ensure user name is not already banned. <br>
   * Add user name to banned users. <br>
   * Verify banned users contains the new ban. <br>
   */
  @Test
  void addUserNameBan() {
    final String userNameToBan = "user-name-to-ban-" + UUID.randomUUID().toString().substring(0, 5);
    assertThat(
        client.getUserBans(), doesNotContainMappedItem(UserBanData::getUsername, userNameToBan));

    client.banUser(
        UserBanParams.builder()
            .username(userNameToBan)
            .systemId("system-id")
            .minutesToBan(10)
            .ip("55.55.55.55")
            .build());

    assertThat(client.getUserBans(), containsMappedItem(UserBanData::getUsername, userNameToBan));
  }
}
