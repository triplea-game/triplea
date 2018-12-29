package org.triplea.game.server.debug;

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

import com.google.common.annotations.VisibleForTesting;

/**
 * A {@link Handler} that publishes log records to the chat subsystem. This allows a headless game server to report its
 * own logs to other game clients via the chat window.
 *
 * <p>
 * <strong>Configuration:</strong> This handler does not currently support configuration through the {@link LogManager}.
 * It always uses the following default configuration:
 * </p>
 * <ul>
 * <li>Level: {@code Level.WARNING}</li>
 * <li>Filter: No {@code Filter}</li>
 * <li>Formatter: {@code java.util.logging.SimpleFormatter}</li>
 * <li>Encoding: default platform encoding</li>
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
        sendChatMessage.accept(formatChatMessage(record));
      } finally {
        enabled = true;
      }
    }
  }

  private static void sendChatMessage(final String message) {
    Optional.ofNullable(HeadlessGameServer.getInstance())
        .map(HeadlessGameServer::getChat)
        .ifPresent(chat -> chat.sendMessage(message, false));
  }

  private String formatChatMessage(final LogRecord record) {
    return getFormatter()
        .format(record)
        .trim();
  }
}
