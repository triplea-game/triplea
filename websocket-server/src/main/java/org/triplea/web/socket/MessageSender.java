package org.triplea.web.socket;

import com.google.gson.Gson;
import java.util.function.BiConsumer;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.java.Interruptibles;

/** Sends a server message (encoded as a JSON string) to a specific connected websocket sessions. */
public class MessageSender implements BiConsumer<WebSocketSession, MessageEnvelope> {
  private static final Gson GSON = new Gson();

  @Override
  public void accept(final WebSocketSession session, final MessageEnvelope message) {
    if (session.isOpen()) {
      new Thread(() -> Interruptibles.await(() -> sendMessage(session, message))).start();
    }
  }

  private void sendMessage(final WebSocketSession session, final MessageEnvelope message) {
    if (session.isOpen()) {
      session.sendText(GSON.toJson(message));
    }
  }
}
