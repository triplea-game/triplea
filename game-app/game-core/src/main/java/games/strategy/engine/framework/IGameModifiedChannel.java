package games.strategy.engine.framework;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.engine.message.wire.EntityRef;
import games.strategy.net.Messengers;
import java.io.Serial;
import java.io.Serializable;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import org.triplea.http.client.web.socket.MessageEnvelope;
import org.triplea.http.client.web.socket.messages.MessageType;
import org.triplea.http.client.web.socket.messages.WebSocketMessage;

/** All changes to game data (Changes and History events) can be tracked through this channel. */
public interface IGameModifiedChannel extends IChannelSubscriber {
  /**
   * Registers the typed handlers for the light channel messages, guarded so per-game subscribers
   * stay idempotent on the session-scoped registry. Each handler dispatches to whichever subscriber
   * the channel endpoint currently holds; the entity reference in a step change resolves by name,
   * so the game data captured here works for any subscriber's data.
   */
  static void registerHandlers(final Messengers messengers, final GameData gameData) {
    if (!messengers.hasTypedMessageHandler(ShutDownMessage.TYPE)) {
      messengers.registerMessageHandler(
          ShutDownMessage.TYPE,
          (message, implementor) -> {
            message.invokeCallback((IGameModifiedChannel) implementor);
            return null;
          });
    }
    if (!messengers.hasTypedMessageHandler(StartHistoryEventMessage.TYPE)) {
      messengers.registerMessageHandler(
          StartHistoryEventMessage.TYPE,
          (message, implementor) -> {
            message.invokeCallback((IGameModifiedChannel) implementor);
            return null;
          });
    }
    if (!messengers.hasTypedMessageHandler(StepChangedMessage.TYPE)) {
      messengers.registerMessageHandler(
          StepChangedMessage.TYPE,
          (message, implementor) -> {
            message.invokeCallback((IGameModifiedChannel) implementor, gameData);
            return null;
          });
    }
  }

  @RemoteActionCode(1)
  void gameDataChanged(Change change);

  @RemoteActionCode(4)
  void startHistoryEvent(String event, Object renderingData);

  @RemoteActionCode(3)
  void startHistoryEvent(String event);

  @RemoteActionCode(0)
  void addChildToEvent(String text, Object renderingData);

  /**
   * Invoked when a game step has changed.
   *
   * @param loadedFromSavedGame - true if the game step has changed because we were loaded from a
   *     saved game.
   */
  @RemoteActionCode(5)
  void stepChanged(
      String stepName,
      String delegateName,
      GamePlayer player,
      int round,
      String displayName,
      boolean loadedFromSavedGame);

  @RemoteActionCode(2)
  void shutDown();

  /** Typed broadcast that a history event has started. */
  @AllArgsConstructor
  class StartHistoryEventMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 4471200938810029921L;

    public static final MessageType<StartHistoryEventMessage> TYPE =
        MessageType.of(StartHistoryEventMessage.class);

    private final String event;

    public void invokeCallback(final IGameModifiedChannel channel) {
      channel.startHistoryEvent(event);
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /**
   * Typed broadcast that the game step changed. The player rides as an {@link EntityRef} resolved
   * against the receiver's game data; it is absent for steps that have no player.
   */
  @AllArgsConstructor
  class StepChangedMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 2290109384712000219L;

    public static final MessageType<StepChangedMessage> TYPE =
        MessageType.of(StepChangedMessage.class);

    private final String stepName;
    private final String delegateName;
    @Nullable private final EntityRef player;
    private final int round;
    private final String displayName;
    private final boolean loadedFromSavedGame;

    public void invokeCallback(final IGameModifiedChannel channel, final GameData gameData) {
      channel.stepChanged(
          stepName,
          delegateName,
          player == null ? null : player.resolvePlayer(gameData),
          round,
          displayName,
          loadedFromSavedGame);
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /** Typed broadcast that the game is shutting down. */
  class ShutDownMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 8812009348871200219L;

    public static final MessageType<ShutDownMessage> TYPE = MessageType.of(ShutDownMessage.class);

    public void invokeCallback(final IGameModifiedChannel channel) {
      channel.shutDown();
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }
}
