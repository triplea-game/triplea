package org.triplea.game.server.debug;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.common.base.Preconditions;
import games.strategy.engine.chat.Chat;
import java.util.List;

/**
 * A {@link AppenderBase} that publishes log records to the chat subsystem. This allows a headless
 * game server to report its own logs to other game clients via the chat window.
 */
public final class ChatAppender extends AppenderBase<ILoggingEvent> {
  private final Chat chat;

  public ChatAppender(final Chat chat) {
    setName("chatMessage");
    this.chat = Preconditions.checkNotNull(chat);
  }

  static void attach(Chat chat) {


  }

  @Override
  protected void append(final ILoggingEvent record) {
    // format log message and send it to the chat window
    formatChatMessage(record).stream()
        .map(message -> "[" + record.getLevel() + "] " + message)
        .forEach(chat::sendMessage);
  }

  private List<String> formatChatMessage(final ILoggingEvent record) {
    return List.of(record.getFormattedMessage().trim().split("\\n"));
  }
}
