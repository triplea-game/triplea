package games.strategy.engine.lobby.client.ui.action.player.info;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.swing.Action;
import javax.swing.JFrame;
import lombok.Builder;
import lombok.extern.java.Log;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.lobby.moderator.PlayerSummaryForModerator;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.swing.SwingAction;

/**
 * When this action is taken (by a moderator), we will fetch from server information about the
 * clicked on player and display it in a tabbed dialog.
 */
@Builder
@Log
public class ShowPlayerInformationAction {
  @Nonnull private final JFrame parent;
  @Nonnull private final PlayerToLobbyConnection playerToLobbyConnection;
  @Nonnull private final PlayerChatId playerChatId;
  private Function<PlayerSummaryForModerator, String> dataFormatter;

  public Action toSwingAction() {
    return SwingAction.of(
        "Show Player Information",
        () ->
            CompletableFuture.runAsync(this::fetchPlayerInfoAndShowDisplay)
                .exceptionally(this::logFetchError));
  }

  private void fetchPlayerInfoAndShowDisplay() {
    final var playerSummaryForModerator =
        playerToLobbyConnection.fetchPlayerInformation(playerChatId);
    PlayerInformationPopup.showPopup(parent, playerSummaryForModerator);
  }

  private Void logFetchError(final Throwable throwable) {
    log.log(Level.SEVERE, "Error fetching player information", throwable);
    return null;
  }
}
