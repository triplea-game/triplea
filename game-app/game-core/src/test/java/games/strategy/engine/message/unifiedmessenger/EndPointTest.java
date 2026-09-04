package games.strategy.engine.message.unifiedmessenger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import games.strategy.engine.message.TypedInvocation;
import games.strategy.engine.message.TypedInvocationResult;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

class EndPointTest {

  interface TestInterface {
    int dummy();
  }

  static final class DummyRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    static final MessageType<DummyRequest> TYPE = MessageType.of(DummyRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  static final class DummyResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    static final MessageType<DummyResponse> TYPE = MessageType.of(DummyResponse.class);
    final int value;

    DummyResponse(final int value) {
      this.value = value;
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  @Test
  void invokesTypedHandlerAgainstTheImplementor() {
    final TypedMessageRegistry registry = new TypedMessageRegistry();
    registry.register(
        DummyRequest.TYPE,
        (message, implementor) -> new DummyResponse(((TestInterface) implementor).dummy()));
    final EndPoint endPoint =
        new EndPoint("", TestInterface.class, false, registry, InvocationExecutionGate.NONE);
    endPoint.addImplementor((TestInterface) () -> 2);

    final TypedInvocation call = new TypedInvocation("", new DummyRequest());
    final List<TypedInvocationResult> results =
        endPoint.invokeLocal(call, endPoint.takeANumber(), null);

    assertEquals(1, results.size());
    assertEquals(2, ((DummyResponse) results.iterator().next().getReturnValue()).value);
  }
}
