package org.triplea.web.socket;

import java.util.Collection;
import java.util.function.BiConsumer;
import javax.websocket.Session;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.web.socket.MessageEnvelope;

/**
 * Sends a server message (encoded as a JSON string) to all open and connected websocket sessions.
 */
@Slf4j
@AllArgsConstructor
public class MessageBroadcaster implements BiConsumer<Collection<Session>, MessageEnvelope> {

  private final BiConsumer<Session, MessageEnvelope> messageSender;

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
  public void accept(final Collection<Session> sessions, final MessageEnvelope messageEnvelope) {
    log.info("Broadcasting: {}", messageEnvelope);
    sessions
        .parallelStream()
        .filter(Session::isOpen)
        .forEach(s -> messageSender.accept(s, messageEnvelope));
  }
}
