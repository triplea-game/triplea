package org.triplea.server.lobby.chat;

import java.util.Collection;
import java.util.function.BiConsumer;
import javax.websocket.Session;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.lobby.chat.events.server.ServerMessageEnvelope;

@Slf4j
@AllArgsConstructor
public class MessageBroadcaster implements BiConsumer<Collection<Session>, ServerMessageEnvelope> {

  private final BiConsumer<Session, ServerMessageEnvelope> messageSender;

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
