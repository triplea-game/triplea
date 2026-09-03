package games.strategy.engine.message.wire;

import games.strategy.net.websocket.ClientNetworkBridge;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * Thin typed send/request layer over {@link ClientNetworkBridge}. Fire-and-forget messages go out
 * through {@link #send}; {@link #request} sends a message and completes once a reply of the
 * expected type arrives, so callers receive a typed response without wiring up the underlying
 * listeners themselves.
 */
public final class MessageBus {
  private final ClientNetworkBridge networkBridge;

  public MessageBus(final ClientNetworkBridge networkBridge) {
    this.networkBridge = networkBridge;
  }

  public void send(final WebSocketMessage message) {
    networkBridge.sendMessage(message);
  }

  /** Registers a handler for a message type; the returned action removes it again. */
  public <T extends WebSocketMessage> Runnable subscribe(
      final MessageType<T> messageType, final Consumer<T> handler) {
    networkBridge.addListener(messageType, handler);
    return () -> networkBridge.removeListener(messageType, handler);
  }

  public <T extends WebSocketMessage> CompletableFuture<T> request(
      final WebSocketMessage request, final MessageType<T> responseType) {
    final CompletableFuture<T> future = new CompletableFuture<>();
    final Consumer<T> oneShotListener =
        new Consumer<>() {
          @Override
          public void accept(final T response) {
            networkBridge.removeListener(responseType, this);
            future.complete(response);
          }
        };
    networkBridge.addListener(responseType, oneShotListener);
    networkBridge.sendMessage(request);
    return future;
  }
}
