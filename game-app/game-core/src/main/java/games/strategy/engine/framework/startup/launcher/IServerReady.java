package games.strategy.engine.framework.startup.launcher;

import games.strategy.engine.message.IRemote;
import java.io.Serial;
import java.io.Serializable;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * Allows for the server to wait for all clients to finish initialization before starting the game.
 */
public interface IServerReady extends IRemote {
  void clientReady();

  /** Typed request signalling that the client has finished initialization. */
  class ClientReadyRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6621409928834451220L;

    public static final MessageType<ClientReadyRequest> TYPE =
        MessageType.of(ClientReadyRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed acknowledgement that the client-ready signal was recorded. */
  class ClientReadyResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 3312048873201995540L;

    public static final MessageType<ClientReadyResponse> TYPE =
        MessageType.of(ClientReadyResponse.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }
}
