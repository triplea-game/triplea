package games.strategy.engine.lobby.client.ui.action;

import java.awt.Component;
import java.util.Collection;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import lombok.Builder;
import lombok.extern.java.Log;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyGameListing;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.java.concurrency.AsyncRunner;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JDialogBuilder;
import org.triplea.swing.JTextAreaBuilder;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;

/**
 * Opens a dialog window listing the players that are present in a game (both playing and
 * observing).
 */
@Builder
@Log
public class ShowPlayersAction {
  @Nonnull private final Supplier<LobbyGameListing> gameIdSelection;
  @Nonnull private final PlayerToLobbyConnection playerToLobbyConnection;
  @Nonnull private final JFrame parentWindow;

  public Action buildSwingAction() {
    return SwingAction.of(
        "Show Players",
        () ->
            AsyncRunner.runAsync(() -> showPlayersInGame(gameIdSelection.get()))
                .exceptionally(e -> log.log(Level.INFO, "Failed to fetch players in game", e)));
  }

  private void showPlayersInGame(final LobbyGameListing lobbyGameListing) {
    final Collection<String> playersInGame =
        playerToLobbyConnection.fetchPlayersInGame(lobbyGameListing.getGameId());

    SwingUtilities.invokeLater(
        () ->
            new JDialogBuilder()
                .parent(parentWindow)
                .title("Players In Game: " + lobbyGameListing.getLobbyGame().getHostName())
                .size(300, 275)
                .add(dialog -> buildDialogContents(dialog, playersInGame))
                .escapeKeyCloses()
                .buildAndShow());
  }

  private static Component buildDialogContents(
      final JDialog dialog, final Collection<String> playersInGame) {
    return new JPanelBuilder()
        .borderLayout()
        .addCenter(SwingComponents.newJScrollPane(buildTextArea(dialog, playersInGame)))
        .addSouth(new JButtonBuilder("Close").actionListener(dialog::dispose).build())
        .build();
  }

  private static Component buildTextArea(
      final JDialog dialog, final Collection<String> playersInGame) {
    return new JTextAreaBuilder()
        .readOnly()
        .text(playersInGame.stream().sorted().collect(Collectors.joining("\n")))
        .keyListener(SwingComponents.escapeKeyListener(dialog::dispose))
        .build();
  }
}
