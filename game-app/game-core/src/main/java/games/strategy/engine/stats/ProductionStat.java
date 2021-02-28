package games.strategy.engine.stats;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.mapdata.MapData;

public class ProductionStat implements IStat {
  @Override
  public String getName() {
    return "Production";
  }

  @Override
  public double getValue(final GamePlayer player, final GameData data, final MapData mapData) {
    final int production =
        data.getMap().getTerritories().stream()
            .filter(place -> place.getOwner().equals(player))
            .filter(
                Matches.territoryCanCollectIncomeFrom(
                    player, data.getProperties(), data.getRelationshipTracker()))
            .mapToInt(TerritoryAttachment::getProduction)
            .sum();
    /*
     * Match will Check if terr is a Land Convoy Route and check ownership
     * of neighboring Sea Zone, or if contested
     */
    return (double) production * Properties.getPuMultiplier(data.getProperties());
  }
}
