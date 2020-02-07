package games.strategy.engine.stats;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.util.TuvUtils;
import java.util.function.Predicate;
import org.triplea.java.collections.IntegerMap;

public class TuvStat extends AbstractStat {
  @Override
  public String getName() {
    return "TUV";
  }

  @Override
  public double getValue(final GamePlayer player, final GameData data) {
    final IntegerMap<UnitType> costs = TuvUtils.getCostsForTuv(player, data);
    final Predicate<Unit> unitIsOwnedBy = Matches.unitIsOwnedBy(player);
    return data.getMap().getTerritories().stream()
        .map(Territory::getUnitCollection)
        .map(units -> units.getMatches(unitIsOwnedBy))
        .mapToInt(owned -> TuvUtils.getTuv(owned, costs))
        .sum();
  }
}
