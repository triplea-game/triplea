package games.strategy.engine.display;

import games.strategy.engine.data.GameData;

/**
 * A bridge between the IDisplay and the game.
 *
 * @deprecated Unused. Kept for backwards compatibility.
 */
@Deprecated
public interface IDisplayBridge {
  GameData getGameData();
}
