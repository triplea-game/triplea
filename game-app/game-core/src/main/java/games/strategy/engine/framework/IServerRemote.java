package games.strategy.engine.framework;

import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

interface IServerRemote extends IRemote {
  @RemoteActionCode(0)
  byte[] getSavedGame();

  /** Typed request for the current game serialized to bytes. */
  class GetSavedGameRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 6178901320559920011L;

    public static final MessageType<GetSavedGameRequest> TYPE =
        MessageType.of(GetSavedGameRequest.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed response carrying the serialized game. */
  @AllArgsConstructor
  class GetSavedGameResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 1200985409942100021L;

    public static final MessageType<GetSavedGameResponse> TYPE =
        MessageType.of(GetSavedGameResponse.class);

    @Getter private final byte[] savedGame;

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }
}
