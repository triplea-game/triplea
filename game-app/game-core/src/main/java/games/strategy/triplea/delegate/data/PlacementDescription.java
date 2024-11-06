package games.strategy.triplea.delegate.data;

import games.strategy.engine.data.AbstractMoveDescription;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.Collection;
import lombok.Getter;

/** Describes an action that places one or more units within a territory. */
@Getter
public class PlacementDescription extends AbstractMoveDescription {
  private static final long serialVersionUID = -3141153168992624631L;
  private final Territory territory;

  public PlacementDescription(final Collection<Unit> units, final Territory territory) {
    super(units);
    this.territory = territory;
  }

  @Override
  public String toString() {
    return "Placement message territory: " + territory + " units: " + getUnits();
  }
}
