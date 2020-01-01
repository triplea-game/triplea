package games.strategy.engine.framework;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.message.IChannelSubscriber;

/** All changes to game data (Changes and History events) can be tracked through this channel. */
public interface IGameModifiedChannel extends IChannelSubscriber {
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
}
