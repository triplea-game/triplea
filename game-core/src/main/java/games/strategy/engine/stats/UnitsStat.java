package games.strategy.engine.stats;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.Matches;
import java.util.function.Predicate;

public class UnitsStat extends AbstractStat {
  @Override
  public String getName() {
    return "Units";
  }

  @Override
  public double getValue(final GamePlayer player, final GameData data) {
    final Predicate<Unit> ownedBy = Matches.unitIsOwnedBy(player);
    // sum the total match count
    return data.getMap().getTerritories().stream()
        .map(Territory::getUnitCollection)
        .mapToInt(units -> units.countMatches(ownedBy))
        .sum();
  }
}
