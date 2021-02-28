package games.strategy.engine.framework;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.message.IChannelSubscriber;
import games.strategy.engine.message.RemoteActionCode;

/** All changes to game data (Changes and History events) can be tracked through this channel. */
public interface IGameModifiedChannel extends IChannelSubscriber {
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
}
