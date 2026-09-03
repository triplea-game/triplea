package games.strategy.engine.message.unifiedmessenger;

import javax.annotation.Nullable;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * Applies a typed message to a local endpoint implementor, the statically-checked replacement for
 * the reflective {@code method.invoke} dispatch. A fire-and-forget notification returns {@code
 * null}; a request/response handler returns the reply message that travels back to the caller.
 *
 * @param <T> the request message type this handler consumes.
 */
@FunctionalInterface
public interface TypedMessageHandler<T extends WebSocketMessage> {
  @Nullable
  WebSocketMessage handle(T message, Object implementor);
}
