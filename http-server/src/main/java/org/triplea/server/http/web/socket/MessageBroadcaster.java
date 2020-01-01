package org.triplea.server.http.web.socket;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
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

  @Override
  public void accept(
      final Collection<Session> sessions, final ServerMessageEnvelope serverEventEnvelope) {
    log.info("Broadcasting: {}", serverEventEnvelope);
    Set.copyOf(sessions)
        .parallelStream()
        .filter(Session::isOpen)
        .forEach(s -> messageSender.accept(s, serverEventEnvelope));
  }
}
