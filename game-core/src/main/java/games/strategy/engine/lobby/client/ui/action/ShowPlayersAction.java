package games.strategy.engine.lobby.client.ui.action;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import lombok.Builder;
import lombok.extern.java.Log;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyGameListing;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.java.concurrency.AsyncRunner;
import org.triplea.swing.JDialogBuilder;
import org.triplea.swing.JTextAreaBuilder;
import org.triplea.swing.SwingAction;

@Builder
@Log
public class ShowPlayersAction {
  private final Supplier<LobbyGameListing> gameIdSelection;
  private final PlayerToLobbyConnection playerToLobbyConnection;
  private final JFrame parentWindow;

  public Action buildSwingAction() {
    return SwingAction.of(
        "Show Players",
        () -> {
          AsyncRunner.runAsync(() -> showPlayersInGame(gameIdSelection.get()))
              .exceptionally(e -> log.log(Level.INFO, "Failed to fetch players in game", e));
        });
  }

  private void showPlayersInGame(final LobbyGameListing lobbyGameListing) {
    final Collection<String> playersInGame =
        playerToLobbyConnection.fetchPlayersInGame(lobbyGameListing.getGameId());

    SwingUtilities.invokeLater(
        () ->
            new JDialogBuilder()
                .parent(parentWindow)
                .title(
                    "Players In Game Hosted By: " + lobbyGameListing.getLobbyGame().getHostName())
                .add(
                    new JTextAreaBuilder()
                        .readOnly()
                        .text(playersInGame.stream().sorted().collect(Collectors.joining("\n")))
                        .build())
                .buildAndShow());
  }
}
