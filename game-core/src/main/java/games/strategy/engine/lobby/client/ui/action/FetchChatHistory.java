package games.strategy.engine.lobby.client.ui.action;

import java.awt.Component;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import lombok.Builder;
import lombok.extern.java.Log;
import org.triplea.http.client.lobby.moderator.ChatHistoryMessage;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.java.DateTimeFormatterUtil;
import org.triplea.swing.JDialogBuilder;
import org.triplea.swing.JTextAreaBuilder;
import org.triplea.swing.SwingComponents;

/**
 * Does an async call to server to fetch chat history for a given game and then renders the returned
 * results in a dialog box.
 */
@Builder
@Log
public class FetchChatHistory {
  @Nonnull private final String gameHostName;
  @Nonnull private final String gameId;
  @Nonnull private final JFrame parentWindow;
  @Nonnull private final PlayerToLobbyConnection playerToLobbyConnection;

  public void fetchAndShowChatHistory() {
    CompletableFuture.runAsync(
            () -> {
              final List<ChatHistoryMessage> chatHistory =
                  playerToLobbyConnection.fetchChatHistoryForGame(gameId);
              SwingUtilities.invokeLater(() -> showChatHistory(chatHistory));
            })
        .exceptionally(
            e -> {
              log.log(Level.SEVERE, "Error fetching game chat history", e);
              return null;
            });
  }

  private void showChatHistory(final List<ChatHistoryMessage> chatHistory) {
    new JDialogBuilder()
        .parent(parentWindow)
        .size(700, 500)
        .title(
            String.format(
                "Game Chat History for: %s, as of %s",
                gameHostName, DateTimeFormatterUtil.formatInstant(Instant.now())))
        .add(SwingComponents.newJScrollPane(buildChatHistoryTextArea(chatHistory)))
        .buildAndShow();
  }

  private Component buildChatHistoryTextArea(final List<ChatHistoryMessage> chatHistory) {
    final String text =
        chatHistory.stream() //
            .map(FetchChatHistory::toChatLine)
            .collect(Collectors.joining("\n"));

    return new JTextAreaBuilder().text(text).readOnly().build();
  }

  private static String toChatLine(final ChatHistoryMessage chatHistoryMessage) {
    return String.format(
        "(%s) %s: %s",
        DateTimeFormatterUtil.formatEpochMilli(chatHistoryMessage.getEpochMilliDate()),
        chatHistoryMessage.getUsername(),
        chatHistoryMessage.getMessage());
  }
}
