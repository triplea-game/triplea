package games.strategy.engine.framework;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.message.IChannelSubscriber;
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
   * Registers the typed handlers for the light channel messages. Registration is unconditional: the
   * registry overwrites any prior handler, so re-registering on a later game refreshes the {@code
   * gameData} captured below. Each handler dispatches to whichever subscriber the channel endpoint
   * currently holds; the entity reference in a step change resolves by name, so the game data
   * captured here works for any subscriber's data.
   */
  static void registerHandlers(final Messengers messengers, final GameData gameData) {
    messengers.registerMessageHandler(
        ShutDownMessage.TYPE,
        (message, implementor) -> {
          message.invokeCallback((IGameModifiedChannel) implementor);
          return null;
        });
    messengers.registerMessageHandler(
        StartHistoryEventMessage.TYPE,
        (message, implementor) -> {
          message.invokeCallback((IGameModifiedChannel) implementor);
          return null;
        });
    messengers.registerMessageHandler(
        StepChangedMessage.TYPE,
        (message, implementor) -> {
          message.invokeCallback((IGameModifiedChannel) implementor, gameData);
          return null;
        });
    messengers.registerMessageHandler(
        GameDataChangedMessage.TYPE,
        (message, implementor) -> {
          message.invokeCallback((IGameModifiedChannel) implementor);
          return null;
        });
    messengers.registerMessageHandler(
        StartHistoryEventWithRenderingMessage.TYPE,
        (message, implementor) -> {
          message.invokeCallback((IGameModifiedChannel) implementor);
          return null;
        });
    messengers.registerMessageHandler(
        AddChildToEventMessage.TYPE,
        (message, implementor) -> {
          message.invokeCallback((IGameModifiedChannel) implementor);
          return null;
        });
  }

  void gameDataChanged(Change change);

  void startHistoryEvent(String event, Object renderingData);

  void startHistoryEvent(String event);

  void addChildToEvent(String text, Object renderingData);

  /**
   * Invoked when a game step has changed.
   *
   * @param loadedFromSavedGame - true if the game step has changed because we were loaded from a
   *     saved game.
   */
  void stepChanged(
      String stepName,
      String delegateName,
      GamePlayer player,
      int round,
      String displayName,
      boolean loadedFromSavedGame);

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

  /**
   * Typed broadcast of a game data change. The {@link Change} rides RAW as a Serializable field:
   * production messengers serialize with GameObjectStreamFactory, so the change's unit/territory/
   * player references keep identity (resolve-or-create) exactly as the reflective wire did — no
   * whole GameData graph is sent. It is delivered on the single-threaded game-modification channel
   * endpoint under the same number gate as {@code stepChanged}, so changes apply strictly in send
   * order.
   */
  @AllArgsConstructor
  class GameDataChangedMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5590109384712000301L;

    public static final MessageType<GameDataChangedMessage> TYPE =
        MessageType.of(GameDataChangedMessage.class);

    private final Change change;

    public void invokeCallback(final IGameModifiedChannel channel) {
      channel.gameDataChanged(change);
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /**
   * Typed broadcast that a history event has started with rendering data. The {@code renderingData}
   * rides RAW as a Serializable field (it is a game object such as a {@link
   * games.strategy.triplea.delegate.DiceRoll} or a unit collection, consumed downstream by history
   * rendering and dice-stat import), matching the reflective wire; it is not dropped.
   */
  @AllArgsConstructor
  class StartHistoryEventWithRenderingMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5590109384712000302L;

    public static final MessageType<StartHistoryEventWithRenderingMessage> TYPE =
        MessageType.of(StartHistoryEventWithRenderingMessage.class);

    private final String event;
    @Nullable private final Object renderingData;

    public void invokeCallback(final IGameModifiedChannel channel) {
      channel.startHistoryEvent(event, renderingData);
    }

    @Override
    public MessageEnvelope toEnvelope() {
      return MessageEnvelope.packageMessage(TYPE, this);
    }
  }

  /**
   * Typed broadcast adding a child to the current history event. The {@code renderingData} rides
   * RAW as a Serializable field (wrapped into an EventChild and read downstream), matching the
   * reflective wire; it is not dropped.
   */
  @AllArgsConstructor
  class AddChildToEventMessage implements WebSocketMessage, Serializable {
    @Serial private static final long serialVersionUID = 5590109384712000303L;

    public static final MessageType<AddChildToEventMessage> TYPE =
        MessageType.of(AddChildToEventMessage.class);

    private final String text;
    @Nullable private final Object renderingData;

    public void invokeCallback(final IGameModifiedChannel channel) {
      channel.addChildToEvent(text, renderingData);
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
