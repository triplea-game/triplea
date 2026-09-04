package games.strategy.engine.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import javax.annotation.Nullable;
import lombok.Getter;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * The result of a remote invocation. Exactly one of {@code returnValue} or {@code exception} is
 * non-null: the handler either returned a typed response message (or null for fire-and-forget) or
 * threw. This replaces the old reflective method-call result; the correlation/latch path carries it
 * unchanged.
 */
public class TypedInvocationResult implements Externalizable {
  private static final long serialVersionUID = 4562274411264858614L;
  @Nullable private WebSocketMessage returnValue;
  // throwable implements Serializable
  @Getter @Nullable private Throwable exception;

  public TypedInvocationResult() {}

  public TypedInvocationResult(final @Nullable WebSocketMessage returnValue) {
    this.returnValue = returnValue;
    exception = null;
  }

  public TypedInvocationResult(final Throwable exception) {
    returnValue = null;
    this.exception = exception;
  }

  @Nullable
  public WebSocketMessage getReturnValue() {
    return returnValue;
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    if (exception != null) {
      out.write(0);
      out.writeObject(exception);
    } else {
      out.write(1);
      out.writeObject(returnValue);
    }
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    final boolean hasReturnValue = in.read() == 1;
    if (hasReturnValue) {
      returnValue = (WebSocketMessage) in.readObject();
    } else {
      exception = (Throwable) in.readObject();
    }
  }

  @Override
  public String toString() {
    final String exceptionMsg = (exception == null) ? "none" : exception.toString();
    return "Return value: '" + returnValue + "', exception: " + exceptionMsg;
  }
}
