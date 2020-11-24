package games.strategy.engine.data.changefactory.units;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import org.triplea.java.collections.IntegerMap;

/**
 * A game data change that captures the damage caused to a collection of units by a bombing attack.
 */
public class BombingUnitDamageChange extends Change {
  private static final long serialVersionUID = 7093658184880237574L;
  private final IntegerMap<String> hits;
  private final IntegerMap<String> undoHits;
  private final Collection<String> territoriesToNotify;

  private BombingUnitDamageChange(
      final IntegerMap<String> hits,
      final IntegerMap<String> undoHits,
      final Collection<String> territoriesToNotify) {
    this.hits = hits;
    this.undoHits = undoHits;
    this.territoriesToNotify = territoriesToNotify;
  }

  public BombingUnitDamageChange(
      final IntegerMap<Unit> hits, final Collection<Territory> territoriesToNotify) {
    this.hits = new IntegerMap<>();
    this.undoHits = new IntegerMap<>();
    hits.entrySet()
        .forEach(
            entry -> {
              this.hits.put(entry.getKey().getId().toString(), entry.getValue());
              this.undoHits.put(entry.getKey().getId().toString(), entry.getKey().getUnitDamage());
            });
    this.territoriesToNotify =
        territoriesToNotify.stream().map(Territory::getName).collect(Collectors.toList());
  }

  @Override
  protected void perform(final GameData data) {
    hits.keySet()
        .forEach(
            unitId -> {
              data.getUnits().get(UUID.fromString(unitId)).setUnitDamage(hits.getInt(unitId));
            });
    this.territoriesToNotify.forEach(
        territory -> {
          data.getMap().getTerritory(territory).notifyChanged();
        });
  }

  @Override
  public Change invert() {
    return new BombingUnitDamageChange(undoHits, hits, territoriesToNotify);
  }

  @Override
  public String toString() {
    return "Bombing unit damage change. Hits:"
        + hits
        + " undoHits:"
        + undoHits
        + " territories:"
        + territoriesToNotify;
  }
}
