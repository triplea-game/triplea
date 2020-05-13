package org.triplea.modules.game;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.HttpInteractionException;
import org.triplea.http.client.lobby.game.ConnectivityCheckClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.ProtectedEndpointTest;

class ConnectivityControllerIntegrationTest extends ProtectedEndpointTest<ConnectivityCheckClient> {
  ConnectivityControllerIntegrationTest() {
    super(AllowedUserRole.HOST, ConnectivityCheckClient::newClient);
  }

  @Test
  void checkConnectivityPositiveCase() {
    final var httpInteractionException =
        assertThrows(
            HttpInteractionException.class,
            () -> verifyEndpoint(client -> client.checkConnectivity("Some game id")));

    assertThat(
        "Expect bad request to be served, the given game-id is not found",
        httpInteractionException.status(),
        is(400));
  }
}
