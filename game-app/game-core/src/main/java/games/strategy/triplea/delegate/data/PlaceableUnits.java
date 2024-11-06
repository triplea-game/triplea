package games.strategy.triplea.delegate.data;

import games.strategy.engine.data.Unit;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;

/** The result of validating a unit placement action. */
public class PlaceableUnits implements Serializable {
  private static final long serialVersionUID = 6572719978603199091L;
  @Nullable private final String errorMessage;
  @Getter private final Collection<Unit> units;

  /** -- GETTER -- Returns the maximum number of units that can be placed or -1 if no limit. */
  @Getter private final int maxUnits;

  public PlaceableUnits(final String errorMessage) {
    this.errorMessage = errorMessage;
    this.units = List.of();
    this.maxUnits = 0;
  }

  public PlaceableUnits(final Collection<Unit> units, final int maxUnits) {
    this.errorMessage = null;
    this.units = units;
    this.maxUnits = maxUnits;
  }

  public PlaceableUnits() {
    this(null);
  }

  @Nullable
  public String getErrorMessage() {
    return errorMessage;
  }

  public boolean isError() {
    return errorMessage != null;
  }

  @Override
  public String toString() {
    return "ProductionResponseMessage units: " + units;
  }
}
