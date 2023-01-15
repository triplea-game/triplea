package org.triplea.spitfire.server.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.remote.actions.RemoteActionsClient;
import org.triplea.java.IpAddressParser;
import org.triplea.spitfire.server.ControllerIntegrationTest;

class RemoteActionsControllerIntegrationTest extends ControllerIntegrationTest {
  final URI localhost;
  final RemoteActionsClient client;
  final RemoteActionsClient hostClient;

  RemoteActionsControllerIntegrationTest(final URI localhost) {
    this.localhost = localhost;
    client = RemoteActionsClient.newClient(localhost, ControllerIntegrationTest.MODERATOR);
    hostClient = RemoteActionsClient.newClient(localhost, ControllerIntegrationTest.HOST);
  }

  @SuppressWarnings("unchecked")
  @Test
  void mustBeAuthorized() {
    assertNotAuthorized(
        ControllerIntegrationTest.NOT_MODERATORS,
        apiKey -> RemoteActionsClient.newClient(localhost, apiKey),
        client -> client.sendShutdownRequest("game-id"));

    assertNotAuthorized(
        ControllerIntegrationTest.NOT_HOST,
        apiKey -> RemoteActionsClient.newClient(localhost, apiKey),
        client -> client.checkIfPlayerIsBanned(IpAddressParser.fromString("3.3.3.3")));
  }

  @Test
  void sendShutdownSignal() {
    client.sendShutdownRequest("game-id");
  }

  @Test
  @DisplayName("IP address is banned")
  void userIsBanned() {
    final boolean result = hostClient.checkIfPlayerIsBanned(IpAddressParser.fromString("1.1.1.1"));

    assertThat(result, is(true));
  }

  @Test
  @DisplayName("IP address has an expired ban")
  void userWasBanned() {
    final boolean result = hostClient.checkIfPlayerIsBanned(IpAddressParser.fromString("1.1.1.2"));

    assertThat(result, is(false));
  }

  @Test
  @DisplayName("IP address is not in ban table at all")
  void userWasNeverBanned() {
    final boolean result = hostClient.checkIfPlayerIsBanned(IpAddressParser.fromString("1.1.1.3"));

    assertThat(result, is(false));
  }
}
