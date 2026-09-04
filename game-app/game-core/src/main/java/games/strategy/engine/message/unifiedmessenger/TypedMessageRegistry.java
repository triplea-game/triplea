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
   * Registers the handler that a converted method's message type dispatches to, overwriting any
   * handler already registered for that type. Message type ids are unique per class, so a
   * re-registration is the same logical handler being refreshed (for example a new game's game data
   * captured in the handler's closure); overwriting is correct and prevents a stale per-game
   * capture from surviving into a later game on the session-scoped registry.
   */
  public <T extends WebSocketMessage> void register(
      final MessageType<T> messageType, final TypedMessageHandler<T> handler) {
    handlers.put(messageType.getMessageTypeId(), handler);
  }

  /** Reports whether a handler is already registered for the given message type. */
  public boolean hasHandler(final MessageType<?> messageType) {
    return handlers.containsKey(messageType.getMessageTypeId());
  }

  @SuppressWarnings("unchecked")
  Optional<TypedMessageHandler<WebSocketMessage>> handlerFor(final WebSocketMessage message) {
    return Optional.ofNullable(
        (TypedMessageHandler<WebSocketMessage>) handlers.get(message.getClass().getName()));
  }
}
