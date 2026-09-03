package games.strategy.engine.message.unifiedmessenger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * Routes an inbound typed message to the handler registered for its type, keyed by the same
 * discriminator the message carries on the wire ({@link MessageType#getMessageTypeId()}). This is
 * the typed replacement for the op-code table: a converted method registers one handler here and
 * the endpoint dispatches to it instead of reflecting a method from an op-code.
 */
public final class TypedMessageRegistry {
  private final Map<String, TypedMessageHandler<? extends WebSocketMessage>> handlers =
      new ConcurrentHashMap<>();

  /**
   * Registers the handler that a converted method's message type dispatches to. Throws on a
   * duplicate type so the fan-out of registrations surfaces a collision during development instead
   * of silently overwriting an earlier handler.
   */
  public <T extends WebSocketMessage> void register(
      final MessageType<T> messageType, final TypedMessageHandler<T> handler) {
    final TypedMessageHandler<? extends WebSocketMessage> existing =
        handlers.putIfAbsent(messageType.getMessageTypeId(), handler);
    if (existing != null) {
      throw new IllegalStateException(
          "A handler is already registered for " + messageType.getMessageTypeId());
    }
  }

  @SuppressWarnings("unchecked")
  Optional<TypedMessageHandler<WebSocketMessage>> handlerFor(final WebSocketMessage message) {
    return Optional.ofNullable(
        (TypedMessageHandler<WebSocketMessage>) handlers.get(message.getClass().getName()));
  }
}
