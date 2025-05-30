package games.strategy.engine.stats;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.mapdata.MapData;
import java.util.Optional;

public class VictoryCityStat implements IStat {
  @Override
  public String getName() {
    return "VC";
  }

  @Override
  public double getValue(final GamePlayer player, final GameData data, final MapData mapData) {
    // return sum of victory cities
    return data.getMap().getTerritories().stream()
        .filter(Matches.isTerritoryOwnedBy(player))
        .map(TerritoryAttachment::get)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .mapToInt(TerritoryAttachment::getVictoryCity)
        .sum();
  }
}
