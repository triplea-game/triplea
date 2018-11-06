package games.strategy.engine.framework;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.message.IChannelSubscribor;

/**
 * All changes to game data (Changes and History events) can be tracked through this channel.
 */
public interface IGameModifiedChannel extends IChannelSubscribor {
  void gameDataChanged(final Change change);

  void startHistoryEvent(final String event, final Object renderingData);

  void startHistoryEvent(final String event);

  // public void setRenderingData(final Object renderingData);
  void addChildToEvent(final String text, final Object renderingData);

  /**
   * Invoked when a game step has changed.
   *
   * @param loadedFromSavedGame - true if the game step has changed because we were loaded from a saved game.
   */
  void stepChanged(final String stepName, final String delegateName, final PlayerId player, final int round,
      final String displayName, final boolean loadedFromSavedGame);

  void shutDown();
}
