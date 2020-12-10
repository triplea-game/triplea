package org.triplea.web.socket;

import java.util.Collection;
import java.util.function.BiConsumer;
import lombok.AllArgsConstructor;
import org.triplea.http.client.web.socket.MessageEnvelope;

/**
 * Sends a server message (encoded as a JSON string) to all open and connected websocket sessions.
 */
@AllArgsConstructor
public class MessageBroadcaster
    implements BiConsumer<Collection<WebSocketSession>, MessageEnvelope> {

  private final BiConsumer<WebSocketSession, MessageEnvelope> messageSender;

  public static MessageBroadcaster build() {
    return new MessageBroadcaster(new MessageSender());
  }

  /**
   * Sends a message to sessions.
   *
   * <p>Warning: use a concurrent collection for {@param sessions}, the broadcast could take a
   * significant amount of time, any modifications during that time could trigger a
   * ConcurrentModificationException.
   *
   * @param sessions Sessions to receive message.
   * @param messageEnvelope The message to send.
   */
  @Override
  public void accept(
      final Collection<WebSocketSession> sessions, final MessageEnvelope messageEnvelope) {
    sessions.parallelStream()
        .filter(WebSocketSession::isOpen)
        .forEach(s -> messageSender.accept(s, messageEnvelope));
  }
}
