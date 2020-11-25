package games.strategy.engine.data.changefactory.units;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.java.collections.IntegerMap;

/** A game data change that captures the damage done to a collection of units. */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UnitHitsChange extends Change {
  private static final long serialVersionUID = 2862726651812142713L;

  private final IntegerMap<String> newHits;
  private final IntegerMap<String> oldHits;
  private final Collection<String> territoriesToNotify;

  /**
   * @param hits The amount of hits that the unit should have. It is NOT the difference of the
   *     existing hits and the new hits.
   * @param territoriesToNotify The territories that contain all the units
   */
  public UnitHitsChange(
      final IntegerMap<Unit> hits, final Collection<Territory> territoriesToNotify) {
    this.newHits = new IntegerMap<>();
    this.oldHits = new IntegerMap<>();
    hits.entrySet()
        .forEach(
            entry -> {
              this.newHits.add(entry.getKey().getId().toString(), entry.getValue());
              this.oldHits.add(entry.getKey().getId().toString(), entry.getKey().getHits());
            });
    this.territoriesToNotify =
        territoriesToNotify.stream().map(Territory::getName).collect(Collectors.toList());
  }

  @Override
  protected void perform(final GameData data) {
    for (final String unitId : newHits.keySet()) {
      final Unit unit = data.getUnits().get(UUID.fromString(unitId));
      unit.setHits(newHits.getInt(unitId));
    }
    for (final String territory : territoriesToNotify) {
      data.getMap().getTerritory(territory).notifyChanged();
    }
  }

  @Override
  public Change invert() {
    return new UnitHitsChange(oldHits, newHits, territoriesToNotify);
  }
}
