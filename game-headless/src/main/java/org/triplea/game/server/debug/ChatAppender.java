package org.triplea.game.server.debug;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Handler;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.triplea.game.server.HeadlessGameServer;

/**
 * A {@link Handler} that publishes log records to the chat subsystem. This allows a headless game
 * server to report its own logs to other game clients via the chat window.
 *
 * <p><strong>Configuration:</strong> This handler does not currently support configuration through
 * the {@link LogManager}. It always uses the following default configuration:
 *
 * <ul>
 *   <li>Level: {@code Level.WARNING}
 *   <li>Filter: No {@code Filter}
 *   <li>Formatter: {@code java.util.logging.SimpleFormatter}
 *   <li>Encoding: default platform encoding
 * </ul>
 */
@ThreadSafe
public final class ChatAppender extends AppenderBase<ILoggingEvent> {
  @GuardedBy("this")
  private boolean enabled = true;

  @Override
  protected void append(final ILoggingEvent loggingEvent) {
    publish(loggingEvent, ChatAppender::sendChatMessage);
  }

  @GuardedBy("this")
  @VisibleForTesting
  void publish(final ILoggingEvent record, final Consumer<String> sendChatMessage) {
    assert Thread.holdsLock(this);

    // guard against infinite recursion if sendChatMessage also logs
    if (enabled) {
      enabled = false;
      try {
        formatChatMessage(record).stream()
            .map(message -> "[" + record.getLevel() + "] " + message)
            .forEach(sendChatMessage);
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
