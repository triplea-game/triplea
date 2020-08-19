package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitHolder;
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
    this.units = units;
    unitOwnerMap = buildUnitOwnerMap(units);
    name = collection.getHolder().getName();
    type = collection.getHolder().getType();
  }

  AddUnits(final String name, final String type, final Collection<Unit> units) {
    this.units = units;
    unitOwnerMap = buildUnitOwnerMap(units);
    this.type = type;
    this.name = name;
  }

  private Map<UUID, String> buildUnitOwnerMap(final Collection<Unit> units) {
    return units.stream()
        .collect(
            Collectors.toMap(
                Unit::getId,
                unit -> {
                  if (unit.getOwner() == null || unit.getOwner().getName() == null) {
                    return GamePlayer.NULL_PLAYERID.getName();
                  }
                  return unit.getOwner().getName();
                }));
  }

  @Override
  public Change invert() {
    return new RemoveUnits(name, type, units);
  }

  @Override
  protected void perform(final GameData data) {
    final UnitHolder holder = data.getUnitHolder(name, type);
    final Collection<Unit> unitsWithCorrectOwner =
        // old saved games will have a null unitOwnerMap
        unitOwnerMap == null ? units : buildUnitsWithOwner(data);
    holder.getUnitCollection().addAll(unitsWithCorrectOwner);
  }

  private Collection<Unit> buildUnitsWithOwner(final GameData data) {
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
