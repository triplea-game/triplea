package org.triplea.server.lobby.chat;

import java.util.function.BiConsumer;
import javax.websocket.Session;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.lobby.chat.events.server.ServerEventEnvelope;

@Slf4j
@AllArgsConstructor
public class MessageBroadcaster implements BiConsumer<Session, ServerEventEnvelope> {

  private final BiConsumer<Session, ServerEventEnvelope> messageSender;

  @Override
  public void accept(final Session session, final ServerEventEnvelope serverEventEnvelope) {
    log.info("Broadcasting: {}", serverEventEnvelope);
    session
        .getOpenSessions()
        .parallelStream()
        .filter(Session::isOpen)
        .forEach(s -> messageSender.accept(s, serverEventEnvelope));
  }
}
