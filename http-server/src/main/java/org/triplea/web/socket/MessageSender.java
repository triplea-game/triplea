package org.triplea.web.socket;

import com.google.gson.Gson;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import javax.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.java.Interruptibles;

/** Sends a server message (encoded as a JSON string) to a specific connected websocket sessions. */
@Slf4j
class MessageSender implements BiConsumer<Session, MessageEnvelope> {
  private static final Gson GSON = new Gson();

  @Override
  public void accept(final Session session, final MessageEnvelope message) {
    if (session.isOpen()) {
      new Thread(() -> Interruptibles.await(() -> sendMessage(session, message))).start();
    }
  }

  private void sendMessage(final Session session, final MessageEnvelope message)
      throws InterruptedException {
    try {
      if (session.isOpen()) {
        session.getAsyncRemote().sendText(GSON.toJson(message)).get();
      }
    } catch (final ExecutionException e) {
      log.warn("Failed to send message: " + message, e);
    }
  }
}
