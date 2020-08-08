package org.triplea.game.server.debug;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
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
public final class ChatHandler extends Handler {
  @GuardedBy("this")
  private boolean enabled = true;

  public ChatHandler() {
    setLevel(Level.WARNING);
    setFormatter(new SimpleFormatter());
  }

  @Override
  public void close() {}

  @Override
  public void flush() {}

  @Override
  public synchronized void publish(final LogRecord record) {
    publish(record, ChatHandler::sendChatMessage);
  }

  @GuardedBy("this")
  @VisibleForTesting
  void publish(final LogRecord record, final Consumer<String> sendChatMessage) {
    assert Thread.holdsLock(this);

    // guard against infinite recursion if sendChatMessage also logs
    if (isLoggable(record) && enabled) {
      enabled = false;
      try {
        formatChatMessage(record).forEach(sendChatMessage);
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

  private List<String> formatChatMessage(final LogRecord record) {
    return List.of(getFormatter().format(record).trim().split("\\n"));
  }
}
