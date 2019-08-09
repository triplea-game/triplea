package games.strategy.triplea.ui;

import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.Collection;

/** A collection of units placed in a single territory. */
public class PlaceData {
  private final Collection<Unit> units;
  private final Territory at;

  public PlaceData(final Collection<Unit> units, final Territory at) {
    this.units = units;
    this.at = at;
  }

  public Territory getAt() {
    return at;
  }

  public Collection<Unit> getUnits() {
    return units;
  }
}
