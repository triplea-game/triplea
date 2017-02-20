package games.strategy.engine.display;

import games.strategy.engine.message.IChannelSubscribor;

/**
 * A Display is a view of the game.
 * Displays listen on the display channel for game events. There may be many displays
 * on a single vm, and conversly a display may interact with many IGamePlayers
 */
public interface IDisplay extends IChannelSubscribor {
  /**
   * before recieving messages, this method will be called by the game engine.
   *
   * @param bridge
   */
  void initialize(IDisplayBridge bridge);

  void shutDown();
}
