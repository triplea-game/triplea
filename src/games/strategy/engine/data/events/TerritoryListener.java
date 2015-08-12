package games.strategy.engine.data.events;

import games.strategy.engine.data.Territory;

/**
 * A TerritoryListener will be notified of events that affect a Territory.
 */
public interface TerritoryListener {
  public void unitsChanged(Territory territory);

  public void ownerChanged(Territory territory);

  public void attachmentChanged(Territory territory);
}
