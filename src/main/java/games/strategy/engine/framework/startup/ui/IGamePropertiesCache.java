package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.data.GameData;

/**
 * Interface that identifies a class that can cache game options
 * the GameProperties can't be replaced, only modified, so the cache works by modifying the GameProperties inside
 * GameData
 */
public interface IGamePropertiesCache {
  /**
   * Caches the gameOptions stored in the game data, and associates with this game
   *
   * @param gameData
   *        the game which options you want to cache
   */
  void cacheGameProperties(GameData gameData);

  /**
   * Loads cached game options into the gameData
   *
   * @param gameData
   *        the game to load the cached game options into
   */
  void loadCachedGamePropertiesInto(GameData gameData);
}
