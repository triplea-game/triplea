package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitHolder;
import games.strategy.engine.data.UnitHolderType;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Change type that indicates units have been removed from the map. */
public class RemoveUnits extends Change {
  private static final long serialVersionUID = -6410444472951010568L;

  private final String name;
  private final Collection<Unit> units;
  // replaced with unitHolderType
  @Deprecated private final String type = null;
  private UnitHolderType unitHolderType;

  /**
   * The unit's owner can be modified sometime after this Change is created but before it is
   * performed. To ensure that the newly created units have the correct ownership, their original
   * owners are stored in this separate map.
   */
  private final Map<UUID, String> unitOwnerMap;

  RemoveUnits(UnitCollection collection, Collection<Unit> units) {
    this(collection.getHolder().getName(), collection.getHolder().getType(), units);
  }

  RemoveUnits(String name, UnitHolderType unitHolderType, Collection<Unit> units) {
    this(name, unitHolderType, units, AddUnits.buildUnitOwnerMap(units));
  }

  RemoveUnits(
      String name,
      UnitHolderType unitHolderType,
      Collection<Unit> units,
      Map<UUID, String> unitOwnerMap) {
    this.name = name;
    this.unitHolderType = unitHolderType;
    this.units = List.copyOf(units);
    this.unitOwnerMap = unitOwnerMap;
  }

  @Override
  protected void perform(final GameState data) {
    final UnitHolder holder = data.getUnitHolder(name, unitHolderType);
    holder.getUnitCollection().removeAll(units);
  }

  @Override
  public Change invert() {
    // Note: We pass in unitOwnerMap so that invert() doesn't rely on the current game state.
    return new AddUnits(name, unitHolderType, units, unitOwnerMap);
  }

  @Override
  public String toString() {
    return "Remove unit change. Remove from: " + name + " units: " + units;
  }

  @Serial
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();

    if (unitHolderType == null && type != null) {
      unitHolderType = UnitHolderType.fromId(type);
    }
  }
}
