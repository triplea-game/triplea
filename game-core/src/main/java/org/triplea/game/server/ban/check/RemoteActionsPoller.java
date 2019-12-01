package org.triplea.game.server.ban.check;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.net.InetAddress;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.game.remote.actions.BannedPlayer;
import org.triplea.http.client.lobby.game.remote.actions.RemoteActions;
import org.triplea.http.client.lobby.game.remote.actions.RemoteActionsClient;
import org.triplea.java.timer.ScheduledTimer;
import org.triplea.java.timer.Timers;

/**
 * Polls the lobby for any moderator commands, eg: users banned or disconnected, or host shutdown.
 */
// TODO: Project#12 Instantiate and start this poller with LobbyWatcher-Thread
@RequiredArgsConstructor
public class RemoteActionsPoller {
  @Nonnull private final RemoteActionsClient remoteActionsClient;
  @Nonnull private final InetAddress myPublicIpAddress;
  @Nonnull private final BiConsumer<PlayerName, String> playerDisconnectAction;
  @Nonnull private final Runnable shutdownServerAction;

  private ScheduledTimer scheduledTimer;

  /**
   * Starts the remote actions poller, runs until cancelled.
   *
   * <p>On each poller iterations fetches a list of remote actions from the lobby server and
   * processes them.
   */
  public void start() {
    scheduledTimer =
        Timers.fixedRateTimer("banned-user-poller")
            .period(20, TimeUnit.SECONDS)
            .delay(3, TimeUnit.SECONDS)
            .task(this::queryForRemoteActions)
            .start();
  }

  /** Stops the remote actions poller thread. */
  public void cancel() {
    Preconditions.checkNotNull(scheduledTimer, "Stop method called before start method");
    scheduledTimer.cancel();
  }

  @VisibleForTesting
  void queryForRemoteActions() {
    final RemoteActions remoteActions = remoteActionsClient.queryForRemoteActions();
    if (remoteActions.serverIsRequestedToShutdown(myPublicIpAddress)) {
      shutdownServerAction.run();
    }
    disconnectBannedPlayers(remoteActions.getBannedPlayers());
  }

  private void disconnectBannedPlayers(final Collection<BannedPlayer> recentlyBannedPlayers) {
    recentlyBannedPlayers.forEach(
        bannedPlayer ->
            playerDisconnectAction.accept(
                bannedPlayer.getPlayerName(), bannedPlayer.getIpAddress()));
  }
}
