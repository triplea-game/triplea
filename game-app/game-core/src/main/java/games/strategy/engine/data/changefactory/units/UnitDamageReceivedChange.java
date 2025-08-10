package games.strategy.engine.data.changefactory.units;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.java.collections.IntegerMap;

/** A game data change that captures the damage done to a collection of units. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UnitDamageReceivedChange extends Change {
  private static final long serialVersionUID = 4151013799362826142L;

  /** Maps unit ids to the total damage they have */
  private final Map<String, Integer> newTotalDamage;

  /** Maps unit ids to the total damage they used to have */
  private final Map<String, Integer> oldTotalDamage;

  private final Collection<String> territoriesToNotify;

  /**
   * @param totalDamage The total amount of damage that the unit should have. It is NOT the
   *     difference of the existing hits and the new hits.
   * @param territoriesToNotify The territories that contain all the units
   */
  public UnitDamageReceivedChange(
      final IntegerMap<Unit> totalDamage, final Collection<Territory> territoriesToNotify) {
    this.newTotalDamage = new HashMap<>(totalDamage.size());
    this.oldTotalDamage = new HashMap<>(totalDamage.size());
    for (Map.Entry<Unit, Integer> entry : totalDamage.entrySet()) {
      String key = entry.getKey().getId().toString();
      newTotalDamage.put(key, entry.getValue());
      oldTotalDamage.put(key, entry.getKey().getHits());
    }
    this.territoriesToNotify =
        territoriesToNotify.stream().map(Territory::getName).collect(Collectors.toList());
  }

  @Override
  protected void perform(final GameState data) {
    // update units damage
    newTotalDamage.forEach(
        (unitId, damage) -> {
          final Unit unit = data.getUnits().get(UUID.fromString(unitId));
          if (unit != null) {
            unit.setHits(damage);
          }
        });
    // invoke territory change listeners
    for (final String territory : territoriesToNotify) {
      data.getMap().getTerritoryOrThrow(territory).notifyChanged();
    }
  }

  @Override
  public Change invert() {
    return new UnitDamageReceivedChange(oldTotalDamage, newTotalDamage, territoriesToNotify);
  }
}
