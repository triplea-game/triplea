package games.strategy.triplea.ai.proAI.data;

import games.strategy.engine.data.Territory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProTerritoryManager {

  final Map<Territory, ProTerritory> territoryMap;

  public ProTerritoryManager(final Map<Territory, ProTerritory> territoryMap) {
    this.territoryMap = territoryMap;
  }

  public ProTerritory get(final Territory t) {
    return territoryMap.get(t);
  }

  public List<Territory> getStrafingTerritories() {
    final List<Territory> strafingTerritories = new ArrayList<Territory>();
    for (final Territory t : territoryMap.keySet()) {
      if (territoryMap.get(t).isStrafing()) {
        strafingTerritories.add(t);
      }
    }
    return strafingTerritories;
  }

}
