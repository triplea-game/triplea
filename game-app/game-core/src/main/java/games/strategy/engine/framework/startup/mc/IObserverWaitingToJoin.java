package games.strategy.engine.framework.startup.mc;

import games.strategy.engine.message.IRemote;
import games.strategy.net.INode;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/**
 * A callback remote. Allows the server to add the player as an observer when the game is in
 * progress.
 */
public interface IObserverWaitingToJoin extends IRemote {
  /**
   * This method should not return until the client is ready to start the game. This includes the
   * display running, with all remote and channel listeners set up.
   */
  void joinGame(byte[] gameData, Map<String, INode> players);

  /** You could not join the game, usually this is due to an error. */
  void cannotJoinGame(String reason);

  /**
   * Typed request asking the observer to join the running game. Blocks until the client is ready.
   * The {@code players} map is omitted from Gson wire fixtures because {@code INode} cannot be
   * instantiated by Gson; it still rides the Java wire.
   */
  @AllArgsConstructor
  class JoinGameRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 7781200934810029921L;

    public static final MessageType<JoinGameRequest> TYPE = MessageType.of(JoinGameRequest.class);

    private final byte[] gameData;
    private final Map<String, INode> players;

    public void invokeCallback(final IObserverWaitingToJoin observer) {
      observer.joinGame(gameData, players);
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed acknowledgement that the observer finished joining. */
  class JoinGameResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 3310928471200109921L;

    public static final MessageType<JoinGameResponse> TYPE = MessageType.of(JoinGameResponse.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed request telling the observer it could not join. */
  @AllArgsConstructor
  class CannotJoinGameRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 1912003488712000219L;

    public static final MessageType<CannotJoinGameRequest> TYPE =
        MessageType.of(CannotJoinGameRequest.class);

    private final String reason;

    public void invokeCallback(final IObserverWaitingToJoin observer) {
      observer.cannotJoinGame(reason);
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed acknowledgement of a cannot-join notice. */
  class CannotJoinGameResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 1120934887120000219L;

    public static final MessageType<CannotJoinGameResponse> TYPE =
        MessageType.of(CannotJoinGameResponse.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }
}
