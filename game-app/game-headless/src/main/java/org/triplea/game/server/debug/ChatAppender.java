package org.triplea.game.server.debug;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.List;
import java.util.Optional;
import org.triplea.game.server.HeadlessGameServer;

/**
 * A {@link AppenderBase} that publishes log records to the chat subsystem. This allows a headless
 * game server to report its own logs to other game clients via the chat window.
 */
public final class ChatAppender extends AppenderBase<ILoggingEvent> {
  private boolean enabled = true;

  @Override
  protected void append(final ILoggingEvent record) {
    // guard against infinite recursion if sendChatMessage also logs
    if (enabled) {
      enabled = false;
      try {
        // format log message and send it to the chat window
        formatChatMessage(record).stream()
            .map(message -> "[" + record.getLevel() + "] " + message)
            .forEach(ChatAppender::sendChatMessage);
      } finally {
        enabled = true;
      }
    }
  }

  private static void sendChatMessage(final String message) {
    Optional.ofNullable(HeadlessGameServer.getInstance())
        .map(HeadlessGameServer::getChat)
        .ifPresent(chat -> chat.sendMessage(message));
  }

  private List<String> formatChatMessage(final ILoggingEvent record) {
    return List.of(record.getFormattedMessage().trim().split("\\n"));
  }
}
