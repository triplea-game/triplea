package org.triplea.spitfire.server.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

import java.net.URI;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.PlayerSummary;
import org.triplea.http.client.lobby.player.PlayerLobbyActionsClient;
import org.triplea.spitfire.server.ControllerIntegrationTest;

class PlayerInfoControllerIntegrationTest extends ControllerIntegrationTest {

  private final PlayerLobbyActionsClient client;
  private final PlayerLobbyActionsClient moderatorClient;

  PlayerInfoControllerIntegrationTest(final URI localhost) {
    client = PlayerLobbyActionsClient.newClient(localhost, ControllerIntegrationTest.ANONYMOUS);
    moderatorClient =
        PlayerLobbyActionsClient.newClient(localhost, ControllerIntegrationTest.MODERATOR);
  }

  @Test
  @Disabled
  /*
  Disabled: non-deterministic test, needs work.
  Failure message:
  Reason: Bad Request
  Http 400 - Bad Request: Player could not be found, have they left chat?
  */
  void fetchPlayerInfo() {
    final PlayerSummary playerSummary = client.fetchPlayerInformation("chatter-chat-id2");

    assertThat(playerSummary.getCurrentGames(), is(notNullValue()));
    assertThat(playerSummary.getIp(), is(nullValue()));
    assertThat(playerSummary.getSystemId(), is(nullValue()));
  }

  @Test
  @Disabled
  /*
  Disabled: non-deterministic test, needs work.
  Failure message:
  Reason: Bad Request
  Http 400 - Bad Request: Player could not be found, have they left chat?
  */
  void fetchPlayerInfoAsModerator() {
    final PlayerSummary playerSummary = moderatorClient.fetchPlayerInformation("chatter-chat-id2");

    assertThat(playerSummary.getIp(), is(notNullValue()));
    assertThat(playerSummary.getRegistrationDateEpochMillis(), is(notNullValue()));
    assertThat(playerSummary.getAliases(), is(notNullValue()));
    assertThat(playerSummary.getBans(), is(notNullValue()));
    assertThat(playerSummary.getSystemId(), is(notNullValue()));
  }
}
