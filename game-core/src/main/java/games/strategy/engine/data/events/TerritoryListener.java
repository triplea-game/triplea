package games.strategy.engine.data.events;

import games.strategy.engine.data.Territory;

/**
 * A TerritoryListener will be notified of events that affect a Territory.
 */
public interface TerritoryListener {
  void unitsChanged(Territory territory);

  void ownerChanged(Territory territory);

  void attachmentChanged(Territory territory);
}
