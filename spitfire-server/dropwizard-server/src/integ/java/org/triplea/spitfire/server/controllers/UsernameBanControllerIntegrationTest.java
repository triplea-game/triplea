package org.triplea.spitfire.server.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.triplea.test.common.matchers.CollectionMatchers.containsMappedItem;
import static org.triplea.test.common.matchers.CollectionMatchers.doesNotContainMappedItem;

import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.login.LobbyLoginClient;
import org.triplea.http.client.lobby.login.LobbyLoginResponse;
import org.triplea.http.client.lobby.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.name.UsernameBanData;
import org.triplea.spitfire.server.ControllerIntegrationTest;

class UsernameBanControllerIntegrationTest extends ControllerIntegrationTest {
  private final URI localhost;
  private final ToolboxUsernameBanClient client;

  UsernameBanControllerIntegrationTest(final URI localhost) {
    this.localhost = localhost;
    this.client =
        ToolboxUsernameBanClient.newClient(localhost, ControllerIntegrationTest.MODERATOR);
  }

  @SuppressWarnings("unchecked")
  @Test
  void mustBeAuthorized() {
    assertNotAuthorized(
        ControllerIntegrationTest.NOT_MODERATORS,
        apiKey -> ToolboxUsernameBanClient.newClient(localhost, apiKey),
        ToolboxUsernameBanClient::getUsernameBans,
        client -> client.addUsernameBan("some-username"),
        client -> client.removeUsernameBan("some-username"));
  }

  @Test
  void listBans() {
    final List<UsernameBanData> nameBans = client.getUsernameBans();
    assertThat(nameBans, is(not(empty())));
  }

  @Test
  void removeBan() {
    final List<UsernameBanData> nameBans = client.getUsernameBans();

    // remember the first item
    final UsernameBanData firstItem = nameBans.get(0);

    // remove the first item
    client.removeUsernameBan(firstItem.getBannedName());

    // verify first item is removed
    assertThat(client.getUsernameBans(), is(not(hasItem(firstItem))));
  }

  @Test
  void addBan() {
    assertThat(
        "Make sure bans does not contain the item we will add",
        client.getUsernameBans(),
        doesNotContainMappedItem(
            UsernameBanData::getBannedName, "username-that-is-now-banned".toUpperCase()));

    client.addUsernameBan("username-that-is-now-banned");

    assertThat(
        "Bans should now contain the newly added item",
        client.getUsernameBans(),
        containsMappedItem(
            UsernameBanData::getBannedName, "username-that-is-now-banned".toUpperCase()));
  }

  /**
   * Do a login to verify we can login. Ban the name we used for login, then repeat the login and
   * verify the login is not successful.
   */
  @Test
  void usernameBanDisallowsLogin() {
    LobbyLoginResponse loginResponse =
        LobbyLoginClient.newClient(localhost).login("random-user", null);
    assertThat("Verify our anonymous login worked", loginResponse.isSuccess(), is(true));

    client.addUsernameBan("random-user");

    loginResponse = LobbyLoginClient.newClient(localhost).login("random-user", null);
    assertThat("Verify our anonymous login worked", loginResponse.isSuccess(), is(false));
  }
}
