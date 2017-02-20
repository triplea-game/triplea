package games.strategy.engine.display;

import games.strategy.engine.data.GameData;

/**
 * A bridge between the IDisplay and the game.
 */
public interface IDisplayBridge {
  GameData getGameData();
}
