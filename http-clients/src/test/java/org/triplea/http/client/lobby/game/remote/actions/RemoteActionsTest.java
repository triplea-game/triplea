package org.triplea.http.client.lobby.game.remote.actions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.IpAddressParser;

@SuppressWarnings("InnerClassMayBeStatic")
class RemoteActionsTest {

  private static final String IPV4 = "99.99.99.99";

  @Nested
  class ServerShutdownIpAddressMatching {
    @Test
    @DisplayName(
        "IsRequestedToShutDown returns *true* "
            + "when a given IP address *is* in the set of IPs to shut down")
    void ipAddressIsPresent() {
      final RemoteActions remoteActions =
          RemoteActions.builder().serversToShutdown(Set.of("1.1.1.1", IPV4)).build();

      final boolean result =
          remoteActions.serverIsRequestedToShutdown(IpAddressParser.fromString(IPV4));

      assertThat(result, is(true));
    }

    @Test
    @DisplayName(
        "IsRequestedToShutDown returns *false* "
            + "when a given IP address is *not* in the set of IPs to shut down")
    void ipAddressIsNotPresent() {
      final RemoteActions remoteActions =
          RemoteActions.builder().serversToShutdown(Set.of("1.1.1.1")).build();

      final boolean result =
          remoteActions.serverIsRequestedToShutdown(IpAddressParser.fromString(IPV4));

      assertThat(result, is(false));
    }

    @Test
    @DisplayName("Verify null systems to shutdown collection does not cause any errors")
    void canHandleNullDataStructure() {
      final var remoteActions = RemoteActions.builder().build();

      assertDoesNotThrow(
          () -> remoteActions.serverIsRequestedToShutdown(IpAddressParser.fromString(IPV4)));
    }
  }

  @Nested
  class BannedPlayers {
    @Test
    @DisplayName("Verify null banned player collection is converted to an empty collection")
    void bannedPlayersCollectionIsNeverReturnedNull() {
      final var bannedPlayers = RemoteActions.builder().build().getBannedPlayers();
      assertThat(bannedPlayers, empty());
    }
  }
}
