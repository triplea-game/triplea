package games.strategy.engine.stats;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.util.TuvUtils;
import java.util.Collection;
import java.util.function.Predicate;
import org.triplea.java.collections.IntegerMap;

public class TuvStat implements IStat {
  @Override
  public String getName() {
    return "TUV";
  }

  @Override
  public double getValue(final GamePlayer player, final GameData data, final MapData mapData) {
    final IntegerMap<UnitType> costs = TuvUtils.getCostsForTuv(player, data);
    final Predicate<Unit> unitIsOwnedBy = Matches.unitIsOwnedBy(player);
    return data.getMap().getTerritories().stream()
        .map(Territory::getUnitCollection)
        .map(units -> units.getMatches(unitIsOwnedBy))
        .flatMap(Collection::stream)
        .filter(unit -> mapData.shouldDrawUnit(unit.getType().getName()))
        .mapToInt(unit -> costs.getInt(unit.getType()))
        .sum();
  }
}
