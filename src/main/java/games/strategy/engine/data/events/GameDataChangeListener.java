package games.strategy.engine.data.events;

import games.strategy.engine.data.Change;

/**
 * A GameDataChangeListener will be notified on changes to the GameData.
 */
public interface GameDataChangeListener {
  void gameDataChanged(Change change);
}
