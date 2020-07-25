package games.strategy.engine.stats;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitHolder;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.mapdata.MapData;
import java.util.Collection;
import java.util.function.Predicate;

public class UnitsStat extends AbstractStat {
  @Override
  public String getName() {
    return "Units";
  }

  @Override
  public double getValue(final GamePlayer player, final GameData data, final MapData mapData) {
    final Predicate<Unit> ownedBy = Matches.unitIsOwnedBy(player);
    final Predicate<Unit> visible = u -> mapData.shouldDrawUnit(u.getType().getName());

    // sum the total match count
    return data.getMap().getTerritories().stream()
        .map(UnitHolder::getUnits)
        .flatMap(Collection::stream)
        .filter(ownedBy)
        .filter(visible)
        .count();
  }
}
