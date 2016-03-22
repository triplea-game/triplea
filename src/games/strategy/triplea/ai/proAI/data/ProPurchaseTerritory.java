package games.strategy.triplea.ai.proAI.data;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.proAI.util.ProMatches;
import games.strategy.triplea.delegate.Matches;

import java.util.ArrayList;
import java.util.List;

public class ProPurchaseTerritory {

  private Territory territory;
  private int unitProduction;
  private List<ProPlaceTerritory> canPlaceTerritories;

  public ProPurchaseTerritory(final Territory territory, final GameData data, final PlayerID player,
      final int unitProduction) {
    this.territory = territory;
    this.unitProduction = unitProduction;
    canPlaceTerritories = new ArrayList<>();
    canPlaceTerritories.add(new ProPlaceTerritory(territory));
    if (ProMatches.territoryHasInfraFactoryAndIsNotConqueredOwnedLand(player, data).match(territory)) {
      for (final Territory t : data.getMap().getNeighbors(territory, Matches.TerritoryIsWater)) {
        if (Properties.getWW2V2(data) || Properties.getUnitPlacementInEnemySeas(data)
            || !t.getUnits().someMatch(Matches.enemyUnit(player, data))) {
          canPlaceTerritories.add(new ProPlaceTerritory(t));
        }
      }
    }
  }

  public int getRemainingUnitProduction() {
    int remainingUnitProduction = unitProduction;
    for (final ProPlaceTerritory ppt : canPlaceTerritories) {
      remainingUnitProduction -= ppt.getPlaceUnits().size();
    }
    return remainingUnitProduction;
  }

  public Territory getTerritory() {
    return territory;
  }

  @Override
  public String toString() {
    return territory + " | unitProduction=" + unitProduction + " | placeTerritories=" + canPlaceTerritories;
  }

  public void setTerritory(final Territory territory) {
    this.territory = territory;
  }

  public int getUnitProduction() {
    return unitProduction;
  }

  public void setUnitProduction(final int unitProduction) {
    this.unitProduction = unitProduction;
  }

  public List<ProPlaceTerritory> getCanPlaceTerritories() {
    return canPlaceTerritories;
  }

  public void setCanPlaceTerritories(final List<ProPlaceTerritory> canPlaceTerritories) {
    this.canPlaceTerritories = canPlaceTerritories;
  }
}
