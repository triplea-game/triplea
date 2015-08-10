package games.strategy.engine.framework;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.message.IChannelSubscribor;

/**
 * All changes to game data (Changes and History events) can be tracked through this channel.
 *
 */
public interface IGameModifiedChannel extends IChannelSubscribor {
  public void gameDataChanged(final Change aChange);

  public void startHistoryEvent(final String event, final Object renderingData);

  public void startHistoryEvent(final String event);

  // public void setRenderingData(final Object renderingData);

  public void addChildToEvent(final String text, final Object renderingData);

  /**
   *
   * @param stepName
   * @param delegateName
   * @param player
   * @param round
   * @param displayName
   * @param loadedFromSavedGame
   *        - true if the game step has changed because we were loaded from a saved game
   */
  public void stepChanged(final String stepName, final String delegateName, final PlayerID player, final int round,
      final String displayName, final boolean loadedFromSavedGame);

  public void shutDown();
}
