package games.strategy.engine.lobby.client.ui.action.player.info;

import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.swing.Action;
import javax.swing.JFrame;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.moderator.PlayerSummary;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.java.concurrency.AsyncRunner;
import org.triplea.swing.SwingAction;

/**
 * When this action is taken (by a moderator), we will fetch from server information about the
 * clicked on player and display it in a tabbed dialog.
 */
@Builder
@Slf4j
public class ShowPlayerInformationAction {
  @Nonnull private final JFrame parent;
  @Nonnull private final PlayerToLobbyConnection playerToLobbyConnection;
  @Nonnull private final PlayerChatId playerChatId;
  @Nonnull private final UserName playerName;
  private Function<PlayerSummary, String> dataFormatter;

  public Action toSwingAction() {
    return SwingAction.of(
        "Show Player Info",
        () ->
            AsyncRunner.runAsync(this::fetchPlayerInfoAndShowDisplay)
                .exceptionally(this::logFetchError));
  }

  private void fetchPlayerInfoAndShowDisplay() {
    final var playerSummary = playerToLobbyConnection.fetchPlayerInformation(playerChatId);
    PlayerInformationPopup.showPopup(parent, playerName, playerSummary);
  }

  private void logFetchError(final Throwable throwable) {
    log.error("Error fetching player information", throwable);
  }
}
