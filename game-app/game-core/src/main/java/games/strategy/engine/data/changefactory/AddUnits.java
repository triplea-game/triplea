package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitHolder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Add units. */
public class AddUnits extends Change {
  private static final long serialVersionUID = 2694342784633196289L;

  private final String name;
  private final Collection<Unit> units;
  private final String type;
  /**
   * The unit's owner can be modified sometime after this Change is created but before it is
   * performed. To ensure that the newly created units have the correct ownership, their original
   * owners are stored in this separate map.
   */
  private final Map<UUID, String> unitOwnerMap;

  AddUnits(final UnitCollection collection, final Collection<Unit> units) {
    this(collection.getHolder().getName(), collection.getHolder().getType(), units);
  }

  AddUnits(final String name, final String type, final Collection<Unit> units) {
    this(name, type, units, AddUnits.buildUnitOwnerMap(units));
  }

  AddUnits(String name, String type, Collection<Unit> units, Map<UUID, String> unitOwnerMap) {
    this.name = name;
    this.type = type;
    this.units = new ArrayList<>(units);
    this.unitOwnerMap = unitOwnerMap;
  }

  /** Returns an unmodifiable map of unit UUIDs to player names. */
  public static Map<UUID, String> buildUnitOwnerMap(final Collection<Unit> units) {
    return units.stream()
        .collect(
            Collectors.toUnmodifiableMap(
                Unit::getId,
                unit -> {
                  if (unit.getOwner() == null || unit.getOwner().getName() == null) {
                    return unit.getData().getPlayerList().getNullPlayer().getName();
                  }
                  return unit.getOwner().getName();
                }));
  }

  @Override
  public Change invert() {
    // Note: We pass in unitOwnerMap so that invert() doesn't rely on the current game state.
    return new RemoveUnits(name, type, units, unitOwnerMap);
  }

  @Override
  protected void perform(final GameState data) {
    final UnitHolder holder = data.getUnitHolder(name, type);
    final Collection<Unit> unitsWithCorrectOwner =
        // old saved games will have a null unitOwnerMap
        unitOwnerMap == null ? units : buildUnitsWithOwner(data);
    holder.getUnitCollection().addAll(unitsWithCorrectOwner);
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
    return "Add unit change.  Add to:" + name + " units:" + units;
  }
}
