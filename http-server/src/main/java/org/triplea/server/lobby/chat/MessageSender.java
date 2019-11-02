package org.triplea.server.lobby.chat;

import com.google.gson.Gson;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import javax.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.lobby.chat.events.server.ServerMessageEnvelope;
import org.triplea.java.Interruptibles;

@Slf4j
public class MessageSender implements BiConsumer<Session, ServerMessageEnvelope> {
  private final Gson gson = new Gson();

  @Override
  public void accept(final Session session, final ServerMessageEnvelope message) {
    if (session.isOpen()) {
      new Thread(
              () -> {
                Interruptibles.await(
                    () -> {
                      try {
                        session.getAsyncRemote().sendText(gson.toJson(message)).get();
                      } catch (final ExecutionException e) {
                        log.warn("Failed to send message: " + message.getMessageType(), e);
                      }
                    });
              })
          .start();
    }
  }
}
