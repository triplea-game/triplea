package games.strategy.triplea.delegate.dataObjects;

import java.io.Serializable;
import java.util.Collection;

import games.strategy.engine.data.Unit;

public class PlaceableUnits implements Serializable {
  private static final long serialVersionUID = 6572719978603199091L;
  private String errorMessage;
  private Collection<Unit> units;
  private int maxUnits;

  /**
   * Creates new PlaceableUnits.
   *
   * @param errorMessage error message
   */
  public PlaceableUnits(final String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public PlaceableUnits(final Collection<Unit> units, final int maxUnits) {
    this.units = units;
    this.maxUnits = maxUnits;
  }

  public Collection<Unit> getUnits() {
    return units;
  }

  /**
   * Returns the maximum number of units that can be placed or -1 if no limit.
   */
  public int getMaxUnits() {
    return maxUnits;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public boolean isError() {
    return errorMessage != null;
  }

  @Override
  public String toString() {
    return "ProductionResponseMessage units:" + units;
  }
}
