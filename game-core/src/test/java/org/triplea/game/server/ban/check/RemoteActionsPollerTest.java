package org.triplea.game.server.ban.check;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.IpAddressParser;
import org.triplea.http.client.lobby.game.remote.actions.BannedPlayer;
import org.triplea.http.client.lobby.game.remote.actions.RemoteActions;
import org.triplea.http.client.lobby.game.remote.actions.RemoteActionsClient;

@ExtendWith(MockitoExtension.class)
class RemoteActionsPollerTest {
  private static final String IPV4 = "99.9.0.0";
  private static final BannedPlayer BANNED_PLAYER_1 =
      BannedPlayer.builder().playerName("player1").ipAddress("127.0.0.1").build();
  private static final BannedPlayer BANNED_PLAYER_2 =
      BannedPlayer.builder().playerName("player2").ipAddress("127.0.0.2").build();

  @Mock private RemoteActionsClient remoteActionsClient;
  @Mock private BiConsumer<PlayerName, String> playerDisconnectAction;
  @Mock private Runnable shutdownAction;

  private RemoteActionsPoller bannedUserPoller;

  @BeforeEach
  void setup() {
    bannedUserPoller =
        new RemoteActionsPoller(
            remoteActionsClient,
            IpAddressParser.fromString(IPV4),
            playerDisconnectAction,
            shutdownAction);
  }

  @Nested
  class PlayerDisconnectAction {
    @Test
    @DisplayName("If no players are recently banend, we do not call the player disconnect action")
    void emptyCase() {
      givenBannedPlayers();

      bannedUserPoller.queryForRemoteActions();

      verify(playerDisconnectAction, never()).accept(any(), any());
    }

    @Test
    @DisplayName("For each recently banned player, we call the player disconnect action")
    void disconnectBannedPlayers() {
      givenBannedPlayers(BANNED_PLAYER_1, BANNED_PLAYER_2);

      bannedUserPoller.queryForRemoteActions();

      verifyPlayerDisconnected(BANNED_PLAYER_1);
      verifyPlayerDisconnected(BANNED_PLAYER_2);
    }

    private void givenBannedPlayers(final BannedPlayer... bannedPlayers) {
      when(remoteActionsClient.queryForRemoteActions())
          .thenReturn(RemoteActions.builder().bannedPlayers(List.of(bannedPlayers)).build());
    }

    private void verifyPlayerDisconnected(final BannedPlayer bannedPlayer) {
      verify(playerDisconnectAction)
          .accept(bannedPlayer.getPlayerName(), bannedPlayer.getIpAddress());
    }
  }

  @Nested
  class ShutdownAction {
    @Test
    @DisplayName("No servers requested for shutdown should be a no-op")
    void emptyCase() {
      givenServersToShutdown();

      bannedUserPoller.queryForRemoteActions();

      verify(shutdownAction, never()).run();
    }

    @Test
    @DisplayName(
        "If we see other IP address requested to shut down, but not ours, should be a no-op")
    void otherServersRequestedToShutdownButNotThisOne() {
      givenServersToShutdown("5.5.5.5");

      bannedUserPoller.queryForRemoteActions();

      verify(shutdownAction, never()).run();
    }

    @Test
    @DisplayName(
        "If we see our IP address in the list requested to shutdown, "
            + "then we should execute the shutdown action")
    void ourServerIsRequestedToShutdown() {
      givenServersToShutdown("5.5.5.5", IPV4);

      bannedUserPoller.queryForRemoteActions();

      verify(shutdownAction).run();
    }

    private void givenServersToShutdown(final String... ipAddresses) {
      when(remoteActionsClient.queryForRemoteActions())
          .thenReturn(RemoteActions.builder().serversToShutdown(List.of(ipAddresses)).build());
    }
  }
}
