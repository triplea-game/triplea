package games.strategy.engine.data.changefactory.units;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.java.collections.IntegerMap;

/**
 * A game data change that captures the damage caused to a collection of units by a bombing attack.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BombingUnitDamageChange extends Change {
  private static final long serialVersionUID = 7093658184880237574L;
  private final IntegerMap<String> newDamage;
  private final IntegerMap<String> oldDamage;
  private final Collection<String> territoriesToNotify;

  /**
   * @param damage The amount of damage that the unit should have. It is NOT the difference of the
   *     existing damage and the new damage.
   * @param territoriesToNotify The territories that contain all the units
   */
  public BombingUnitDamageChange(
      final IntegerMap<Unit> damage, final Collection<Territory> territoriesToNotify) {
    this.newDamage = new IntegerMap<>();
    this.oldDamage = new IntegerMap<>();
    damage
        .entrySet()
        .forEach(
            entry -> {
              this.newDamage.put(entry.getKey().getId().toString(), entry.getValue());
              this.oldDamage.put(entry.getKey().getId().toString(), entry.getKey().getUnitDamage());
            });
    this.territoriesToNotify =
        territoriesToNotify.stream().map(Territory::getName).collect(Collectors.toList());
  }

  @Override
  protected void perform(final GameState data) {
    newDamage
        .keySet()
        .forEach(
            unitId ->
                data.getUnits()
                    .get(UUID.fromString(unitId))
                    .setUnitDamage(newDamage.getInt(unitId)));
    this.territoriesToNotify.forEach(
        territory -> data.getMap().getTerritoryOrThrow(territory).notifyChanged());
  }

  @Override
  public Change invert() {
    return new BombingUnitDamageChange(oldDamage, newDamage, territoriesToNotify);
  }
}
