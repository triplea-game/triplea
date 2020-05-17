package games.strategy.engine.lobby.client.ui.action.player.info;

import java.util.function.Function;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.swing.Action;
import javax.swing.JFrame;
import lombok.Builder;
import lombok.extern.java.Log;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.http.client.lobby.moderator.PlayerSummary;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.java.concurrency.AsyncRunner;
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
  private Function<PlayerSummary, String> dataFormatter;

  public Action toSwingAction() {
    return SwingAction.of(
        "Show Player Info",
        () ->
            AsyncRunner.runAsync(this::fetchPlayerInfoAndShowDisplay)
                .exceptionally(this::logFetchError));
  }

  private void fetchPlayerInfoAndShowDisplay() {
    final var playerSummary =
        playerToLobbyConnection.fetchPlayerInformation(playerChatId);
    PlayerInformationPopup.showPopup(parent, playerSummary);
  }

  private void logFetchError(final Throwable throwable) {
    log.log(Level.SEVERE, "Error fetching player information", throwable);
  }
}
