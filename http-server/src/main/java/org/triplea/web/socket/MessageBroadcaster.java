package org.triplea.web.socket;

import java.util.Collection;
import java.util.function.BiConsumer;
import javax.websocket.Session;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

/**
 * Sends a server message (encoded as a JSON string) to all open and connected websocket sessions.
 */
@Slf4j
@AllArgsConstructor
public class MessageBroadcaster implements BiConsumer<Collection<Session>, ServerMessageEnvelope> {

  private final BiConsumer<Session, ServerMessageEnvelope> messageSender;

  /**
   * Sends a message to sessions.
   *
   * <p>Warning: use a concurrent collection for {@param sessions}, the broadcast could take a
   * significant amount of time, any modifications during that time could trigger a
   * ConcurrentModificationException.
   *
   * @param sessions Sessions to receive message.
   * @param serverEventEnvelope The message to send.
   */
  @Override
  public void accept(
      final Collection<Session> sessions, final ServerMessageEnvelope serverEventEnvelope) {
    log.info("Broadcasting: {}", serverEventEnvelope);
    sessions
        .parallelStream()
        .filter(Session::isOpen)
        .forEach(s -> messageSender.accept(s, serverEventEnvelope));
  }
}
