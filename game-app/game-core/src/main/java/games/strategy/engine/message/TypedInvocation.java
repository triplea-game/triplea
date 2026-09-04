package games.strategy.engine.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import lombok.Getter;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * The payload of a remote invocation: the target endpoint name plus the typed message to dispatch
 * through the {@link games.strategy.engine.message.unifiedmessenger.TypedMessageRegistry}. This
 * replaces the old reflective method-call carrier; routing still keys off {@link #getRemoteName()}
 * exactly as before, only the payload is now an explicit typed message rather than a reflected
 * method + args.
 */
public class TypedInvocation implements Externalizable {
  private static final long serialVersionUID = 4630825927685836208L;
  @Getter private String remoteName;
  @Getter private WebSocketMessage message;

  public TypedInvocation() {}

  public TypedInvocation(final String remoteName, final WebSocketMessage message) {
    this.remoteName = remoteName;
    this.message = message;
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    out.writeUTF(remoteName);
    out.writeObject(message);
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    remoteName = in.readUTF();
    message = (WebSocketMessage) in.readObject();
  }

  @Override
  public String toString() {
    return "TypedInvocation{remoteName="
        + remoteName
        + ", message="
        + (message == null ? "null" : message.getClass().getName())
        + "}";
  }
}
