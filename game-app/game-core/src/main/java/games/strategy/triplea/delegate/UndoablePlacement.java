package games.strategy.triplea.delegate;

import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.data.PlacementDescription;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/** Contains all the data to describe a placement and to undo it. */
@Getter
public class UndoablePlacement extends AbstractUndoableMove {
  private static final long serialVersionUID = -1493488646587233451L;

  private final Territory placeTerritory;
  private Territory producerTerritory;

  public UndoablePlacement(
      final CompositeChange change,
      final Territory producerTerritory,
      final Territory placeTerritory,
      final Collection<Unit> units) {
    super(change, units);
    this.placeTerritory = placeTerritory;
    this.producerTerritory = producerTerritory;
  }

  public void setProducerTerritory(final Territory producerTerritory) {
    this.producerTerritory = producerTerritory;
  }

  @Override
  protected final void undoSpecific(final IDelegateBridge bridge) {
    final GameState data = bridge.getData();
    final AbstractPlaceDelegate currentDelegate =
        (AbstractPlaceDelegate) data.getSequence().getStep().getDelegate();
    final Map<Territory, Collection<Unit>> produced = currentDelegate.getProduced();
    final Collection<Unit> units = produced.get(producerTerritory);
    units.removeAll(getUnits());
    if (units.isEmpty()) {
      produced.remove(producerTerritory);
    }
    currentDelegate.setProduced(new HashMap<>(produced));
  }

  @Override
  public final String getMoveLabel() {
    return getMoveLabel(" -> ");
  }

  private String getMoveLabel(final String separator) {
    return producerTerritory.equals(placeTerritory)
        ? placeTerritory.getName()
        : producerTerritory.getName() + separator + placeTerritory.getName();
  }

  @Override
  public final Territory getEnd() {
    return placeTerritory;
  }

  @Override
  protected final PlacementDescription getDescriptionObject() {
    return new PlacementDescription(units, placeTerritory);
  }

  @Override
  public String toString() {
    return getMoveLabel(" produces in ") + ": " + MyFormatter.unitsToTextNoOwner(units);
  }
}
