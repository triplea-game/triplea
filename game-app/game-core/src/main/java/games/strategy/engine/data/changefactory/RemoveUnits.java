package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitHolder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Change type that indicates units have been removed from the map. */
public class RemoveUnits extends Change {
  private static final long serialVersionUID = -6410444472951010568L;

  private final String name;
  private final Collection<Unit> units;
  private final String type;

  /**
   * The unit's owner can be modified sometime after this Change is created but before it is
   * performed. To ensure that the newly created units have the correct ownership, their original
   * owners are stored in this separate map.
   */
  private final Map<UUID, String> unitOwnerMap;

  RemoveUnits(UnitCollection collection, Collection<Unit> units) {
    this(collection.getHolder().getName(), collection.getHolder().getType(), units);
  }

  RemoveUnits(String name, String type, Collection<Unit> units) {
    this(name, type, units, AddUnits.buildUnitOwnerMap(units));
  }

  RemoveUnits(String name, String type, Collection<Unit> units, Map<UUID, String> unitOwnerMap) {
    this.name = name;
    this.type = type;
    this.units = List.copyOf(units);
    this.unitOwnerMap = unitOwnerMap;
  }

  @Override
  public Change invert() {
    // Note: We pass in unitOwnerMap so that invert() doesn't rely on the current game state.
    return new AddUnits(name, type, units, unitOwnerMap);
  }

  @Override
  protected void perform(final GameState data) {
    final UnitHolder holder = data.getUnitHolder(name, type);
    holder.getUnitCollection().removeAll(units);
  }

  @Override
  public String toString() {
    return "Remove unit change. Remove from: " + name + " units: " + units;
  }
}
