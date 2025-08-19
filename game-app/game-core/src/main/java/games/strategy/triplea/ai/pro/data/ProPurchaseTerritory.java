package games.strategy.triplea.ai.pro.data;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.pro.util.ProMatches;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.triplea.java.collections.CollectionUtils;

/** The result of an AI purchase analysis for a single territory. */
@Getter
@ToString
public class ProPurchaseTerritory {

  private final @NotNull Territory territory;
  @Setter private int unitProduction;
  private final List<ProPlaceTerritory> canPlaceTerritories;

  public ProPurchaseTerritory(
      final Territory territory,
      final GameData data,
      final GamePlayer player,
      final int unitProduction) {
    this(territory, data, player, unitProduction, false);
  }

  /**
   * Create data structure for tracking unit purchase and list of place territories.
   *
   * @param territory - production territory
   * @param data - current game data
   * @param player - AI player who is purchasing
   * @param unitProduction - max unit production for territory
   * @param isBid - true when bid phase, false when normal purchase phase
   */
  public ProPurchaseTerritory(
      final @NotNull Territory territory,
      final GameData data,
      final GamePlayer player,
      final int unitProduction,
      final boolean isBid) {
    this.territory = territory;
    this.unitProduction = unitProduction;
    canPlaceTerritories = new ArrayList<>();
    canPlaceTerritories.add(new ProPlaceTerritory(territory));
    if (!isBid
        && ProMatches.territoryHasFactoryAndIsNotConqueredOwnedLand(player).test(territory)) {
      for (final Territory t : data.getMap().getNeighbors(territory, Matches.territoryIsWater())) {
        if (Properties.getWW2V2(data.getProperties())
            || Properties.getUnitPlacementInEnemySeas(data.getProperties())
            || !t.anyUnitsMatch(Matches.enemyUnit(player))) {
          canPlaceTerritories.add(new ProPlaceTerritory(t));
        }
      }
    }
  }

  public int getRemainingUnitProduction() {
    int remainingUnitProduction = unitProduction;
    for (final ProPlaceTerritory ppt : canPlaceTerritories) {
      remainingUnitProduction -=
          CollectionUtils.countMatches(ppt.getPlaceUnits(), Matches.unitIsNotConstruction());
    }
    return remainingUnitProduction;
  }
}
