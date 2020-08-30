package org.triplea.modules.remote.actions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.github.database.rider.core.api.dataset.DataSet;
import java.net.URI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.IpAddressParser;
import org.triplea.http.client.remote.actions.RemoteActionsClient;
import org.triplea.modules.http.AllowedUserRole;
import org.triplea.modules.http.LobbyServerTest;
import org.triplea.modules.http.ProtectedEndpointTest;

@DataSet(
    value =
        LobbyServerTest.LOBBY_USER_DATASET
            + ", integration/banned_user.yml, integration/game_hosting_api_key.yml",
    useSequenceFiltering = false)
class RemoteActionsControllerIntegrationTest extends ProtectedEndpointTest<RemoteActionsClient> {

  RemoteActionsControllerIntegrationTest(final URI localhost) {
    super(localhost, RemoteActionsClient::new);
  }

  @Test
  void sendShutdownSignal() {
    verifyEndpoint(AllowedUserRole.MODERATOR, client -> client.sendShutdownRequest("game-id"));
  }

  @Test
  @DisplayName("IP address is banned")
  void userIsBanned() {
    final boolean result =
        verifyEndpointReturningObject(
            AllowedUserRole.HOST,
            client -> client.checkIfPlayerIsBanned(IpAddressParser.fromString("1.1.1.1")));

    assertThat(result, is(true));
  }

  @Test
  @DisplayName("IP address has an expired ban")
  void userWasBanned() {
    final boolean result =
        verifyEndpointReturningObject(
            AllowedUserRole.HOST,
            client -> client.checkIfPlayerIsBanned(IpAddressParser.fromString("1.1.1.2")));

    assertThat(result, is(false));
  }

  @Test
  @DisplayName("IP address is not in ban table at all")
  void userWasNeverBanned() {
    final boolean result =
        verifyEndpointReturningObject(
            AllowedUserRole.HOST,
            client -> client.checkIfPlayerIsBanned(IpAddressParser.fromString("1.1.1.3")));

    assertThat(result, is(false));
  }
}
