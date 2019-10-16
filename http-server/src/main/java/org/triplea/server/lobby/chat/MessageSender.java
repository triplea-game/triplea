package org.triplea.server.lobby.chat;

import com.google.gson.Gson;
import java.util.function.BiConsumer;
import javax.websocket.Session;
import org.triplea.http.client.lobby.chat.events.server.ServerEventEnvelope;
import org.triplea.java.concurrency.CompletableFutureUtils;

public class MessageSender implements BiConsumer<Session, ServerEventEnvelope> {
  private final Gson gson = new Gson();

  @Override
  public void accept(final Session session, final ServerEventEnvelope message) {
    CompletableFutureUtils.logExceptionWhenComplete(
        CompletableFutureUtils.toCompletableFuture(
            session.getAsyncRemote().sendText(gson.toJson(message))),
        "Failed to send player listing");
  }
}
