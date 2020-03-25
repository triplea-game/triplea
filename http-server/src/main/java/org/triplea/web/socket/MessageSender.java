package org.triplea.web.socket;

import com.google.gson.Gson;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import javax.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;
import org.triplea.java.Interruptibles;

/** Sends a server message (encoded as a JSON string) to a specific connected websocket sessions. */
@Slf4j
public class MessageSender implements BiConsumer<Session, ServerMessageEnvelope> {
  private final Gson gson = new Gson();

  @Override
  public void accept(final Session session, final ServerMessageEnvelope message) {
    if (session.isOpen()) {
      new Thread(() -> Interruptibles.await(() -> sendMessage(session, message))).start();
    }
  }

  private void sendMessage(final Session session, final ServerMessageEnvelope message)
      throws InterruptedException {
    try {
      if (session.isOpen()) {
        session.getAsyncRemote().sendText(gson.toJson(message)).get();
      }
    } catch (final ExecutionException e) {
      log.warn("Failed to send message: " + message, e);
    }
  }
}
