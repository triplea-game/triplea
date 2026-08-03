package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GamePlayer;
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
import java.util.stream.Collectors;

/** Add units. */
public class AddUnits extends Change {
  private static final long serialVersionUID = 2694342784633196289L;

  private final String name;
  private final Collection<Unit> units;
  @Deprecated private final String type = null;
  private UnitHolderType unitHolderType;

  /**
   * The unit's owner can be modified sometime after this Change is created but before it is
   * performed. To ensure that the newly created units have the correct ownership, their original
   * owners are stored in this separate map.
   */
  private final Map<UUID, String> unitOwnerMap;

  AddUnits(UnitCollection collection, Collection<Unit> units) {
    this(collection.getHolder().getName(), collection.getHolder().getType(), units);
  }

  AddUnits(String name, UnitHolderType unitHolderType, Collection<Unit> units) {
    this(name, unitHolderType, units, AddUnits.buildUnitOwnerMap(units));
  }

  AddUnits(
      String name,
      UnitHolderType unitHolderType,
      Collection<Unit> units,
      Map<UUID, String> unitOwnerMap) {
    this.name = name;
    this.unitHolderType = unitHolderType;
    this.units = List.copyOf(units);
    this.unitOwnerMap = unitOwnerMap;
  }

  /** Returns an unmodifiable map of unit UUIDs to player names. */
  public static Map<UUID, String> buildUnitOwnerMap(final Collection<Unit> units) {
    // Tolerate duplicate UUIDs: if a unit appears more than once (e.g. a corrupted save where
    // two Unit objects share an id), keep the first owner. Without a merge function, toMap
    // throws IllegalStateException and blocks edit-mode removal of the offending units.
    return units.stream()
        .collect(
            Collectors.toMap(
                Unit::getId, u -> u.getOwner().getName(), (existing, duplicate) -> existing));
  }

  @Override
  protected void perform(final GameState data) {
    final UnitHolder holder = data.getUnitHolder(name, unitHolderType);
    final Collection<Unit> unitsWithCorrectOwner =
        // old saved games will have a null unitOwnerMap
        unitOwnerMap == null ? units : buildUnitsWithOwner(data);
    holder.getUnitCollection().addAll(unitsWithCorrectOwner);
  }

  @Override
  public Change invert() {
    // Note: We pass in unitOwnerMap so that invert() doesn't rely on the current game state.
    return new RemoveUnits(name, unitHolderType, units, unitOwnerMap);
  }

  private Collection<Unit> buildUnitsWithOwner(final GameState data) {
    final Map<UUID, Unit> uuidToUnits =
        units.stream().collect(Collectors.toMap(Unit::getId, unit -> unit));
    return unitOwnerMap.entrySet().stream()
        .map(
            entry -> {
              Unit unit = data.getUnits().get(entry.getKey());
              if (unit == null) {
                unit = uuidToUnits.get(entry.getKey());
              }
              if (entry.getValue() != null) {
                final GamePlayer player = data.getPlayerList().getPlayerId(entry.getValue());
                unit.setOwner(player);
              }
              return unit;
            })
        .collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return "Add unit change.  Add to: " + name + " units: " + units;
  }

  @Serial
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();

    if (unitHolderType == null && type != null) {
      unitHolderType = UnitHolderType.fromId(type);
    }
  }
}
