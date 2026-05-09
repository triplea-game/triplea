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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;

/** Contains all the data to describe a placement and to undo it. */
@Getter
public class UndoablePlacement extends AbstractUndoableMove {
  private static final long serialVersionUID = -1493488646587233451L;

  private final Territory placeTerritory;
  private Territory producerTerritory;

  // placements this one depends on (must remain undone-able only after these are undone first;
  // these can't be undone while we're around)
  private final Set<UndoablePlacement> dependencies = new HashSet<>();
  // placements that depend on this one — they must be undone before us
  private final Set<UndoablePlacement> dependents = new HashSet<>();

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

  /** Adds a placement that depends on this one (must be undone before this can be undone). */
  public void addDependent(final UndoablePlacement dependent) {
    dependents.add(dependent);
    dependent.dependencies.add(this);
  }

  public boolean getCanUndo() {
    return dependents.isEmpty();
  }

  public String getReasonCantUndo() {
    if (dependents.isEmpty()) {
      throw new IllegalStateException("no reason");
    }
    return "Placement "
        + (CollectionUtils.getAny(dependents).getIndex() + 1)
        + " must be undone first";
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
    for (final UndoablePlacement dep : dependencies) {
      dep.dependents.remove(this);
    }
    dependencies.clear();
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
