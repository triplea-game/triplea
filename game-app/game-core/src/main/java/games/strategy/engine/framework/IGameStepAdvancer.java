package games.strategy.engine.framework;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.wire.EntityRef;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

interface IGameStepAdvancer extends IRemote {
  /**
   * A server calls this methods on client game when a player starts a certain step. The method
   * should not return until the player has finished the step.
   */
  void startPlayerStep(String stepName, GamePlayer player);

  /**
   * Typed request advancing the client to a step for a player. The player rides as an {@link
   * EntityRef} resolved against the receiver's game data. Blocks until the step is finished.
   */
  @AllArgsConstructor
  class StartPlayerStepRequest implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 4420098831200910021L;

    public static final MessageType<StartPlayerStepRequest> TYPE =
        MessageType.of(StartPlayerStepRequest.class);

    @Getter private final String stepName;
    private final EntityRef player;

    public GamePlayer resolvePlayer(final GameData gameData) {
      return player.resolvePlayer(gameData);
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed acknowledgement that the step was finished. */
  class StartPlayerStepResponse implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 2231009488471200021L;

    public static final MessageType<StartPlayerStepResponse> TYPE =
        MessageType.of(StartPlayerStepResponse.class);

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }
}
