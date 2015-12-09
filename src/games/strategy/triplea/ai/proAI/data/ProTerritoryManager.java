package games.strategy.triplea.ai.proAI.data;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.proAI.ProData;
import games.strategy.triplea.ai.proAI.util.ProMoveOptionsUtils;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProTerritoryManager {

  private final GameData data;
  private final PlayerID player;
  private final ProMoveOptionsUtils attackOptionsUtils;

  private final Map<Territory, ProTerritory> territoryMap;
  private final Map<Unit, Set<Territory>> unitMoveMap;
  private final Map<Unit, Set<Territory>> transportMoveMap;
  private final Map<Unit, Set<Territory>> bombardMap;
  private final List<ProTransport> transportList;
  private ProMoveOptions alliedAttackOptions;
  private ProMoveOptions enemyDefendOptions;
  private ProMoveOptions enemyAttackOptions;

  public ProTerritoryManager(final ProMoveOptionsUtils attackOptionsUtils) {
    data = ProData.getData();
    player = ProData.getPlayer();
    this.attackOptionsUtils = attackOptionsUtils;
    territoryMap = new HashMap<Territory, ProTerritory>();
    unitMoveMap = new HashMap<Unit, Set<Territory>>();
    transportMoveMap = new HashMap<Unit, Set<Territory>>();
    bombardMap = new HashMap<Unit, Set<Territory>>();
    transportList = new ArrayList<ProTransport>();
    alliedAttackOptions = new ProMoveOptions();
    enemyDefendOptions = new ProMoveOptions();
    enemyAttackOptions = new ProMoveOptions();
  }

  public ProTerritoryManager(final ProMoveOptionsUtils attackOptionsUtils, final ProTerritoryManager territoryManager) {
    this(attackOptionsUtils);
    for (final Territory t : territoryManager.getTerritoryMap().keySet()) {
      territoryMap.put(t, new ProTerritory(territoryManager.getTerritoryMap().get(t)));
    }
    unitMoveMap.putAll(territoryManager.getUnitMoveMap());
    transportMoveMap.putAll(territoryManager.getTransportMoveMap());
    bombardMap.putAll(territoryManager.getBombardMap());
    transportList.addAll(territoryManager.getTransportList());
    alliedAttackOptions = territoryManager.getAlliedAttackOptions();
    enemyDefendOptions = territoryManager.getEnemyDefendOptions();
    enemyAttackOptions = territoryManager.getEnemyAttackOptions();
  }

  public void populateAttackOptions() {
    attackOptionsUtils.findAttackOptions(player, ProData.myUnitTerritories, territoryMap, unitMoveMap,
        transportMoveMap, bombardMap, transportList, new ArrayList<Territory>(), new ArrayList<Territory>(),
        new ArrayList<Territory>(), false, false);
    alliedAttackOptions = attackOptionsUtils.findAlliedAttackOptions(player);
  }

  public void populatePotentialAttackOptions() {
    attackOptionsUtils.findPotentialAttackOptions(player, ProData.myUnitTerritories, territoryMap, unitMoveMap,
        transportMoveMap, bombardMap, transportList);
  }

  public void populateDefenseOptions() {
    attackOptionsUtils.findDefendOptions(player, ProData.myUnitTerritories, territoryMap, unitMoveMap,
        transportMoveMap, transportList, new ArrayList<Territory>(), false);
  }

  public void populateEnemyAttackOptions(final List<Territory> clearedTerritories,
      final List<Territory> territoriesToCheck) {
    enemyAttackOptions = attackOptionsUtils.findEnemyAttackOptions(player, clearedTerritories, territoriesToCheck);
  }

  public void populateEnemyDefenseOptions() {
    attackOptionsUtils.findScrambleOptions(player, territoryMap);
    enemyDefendOptions = attackOptionsUtils.findEnemyDefendOptions(player);
  }

  public List<ProTerritory> removeTerritoriesThatCantBeConquered() {
    return attackOptionsUtils.removeTerritoriesThatCantBeConquered(player, territoryMap, unitMoveMap, transportMoveMap,
        alliedAttackOptions, enemyDefendOptions, false);
  }

  public List<ProTerritory> removePotentialTerritoriesThatCantBeConquered() {
    return attackOptionsUtils.removeTerritoriesThatCantBeConquered(player, territoryMap, unitMoveMap, transportMoveMap,
        alliedAttackOptions, enemyDefendOptions, true);
  }

  public ProTerritory get(final Territory t) {
    return territoryMap.get(t);
  }

  public Map<Territory, ProTerritory> getTerritoryMap() {
    return territoryMap;
  }

  public Map<Unit, Set<Territory>> getUnitMoveMap() {
    return unitMoveMap;
  }

  public Map<Unit, Set<Territory>> getTransportMoveMap() {
    return transportMoveMap;
  }

  public Map<Unit, Set<Territory>> getBombardMap() {
    return bombardMap;
  }

  public List<ProTransport> getTransportList() {
    return transportList;
  }

  public ProMoveOptions getAlliedAttackOptions() {
    return alliedAttackOptions;
  }

  public ProMoveOptions getEnemyDefendOptions() {
    return enemyDefendOptions;
  }

  public ProMoveOptions getEnemyAttackOptions() {
    return enemyAttackOptions;
  }

  public List<Territory> getTerritories() {
    return new ArrayList<Territory>(territoryMap.keySet());
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

  public List<Territory> getCantHoldTerritories() {
    final List<Territory> territoriesThatCantBeHeld = new ArrayList<Territory>();
    for (final Territory t : territoryMap.keySet()) {
      if (!territoryMap.get(t).isCanHold()) {
        territoriesThatCantBeHeld.add(t);
      }
    }
    return territoriesThatCantBeHeld;
  }

  public boolean haveUsedAllTransports() {
    final Set<Unit> movedTransports = new HashSet<Unit>();
    for (final ProTerritory patd : territoryMap.values()) {
      movedTransports.addAll(patd.getAmphibAttackMap().keySet());
      movedTransports.addAll(Match.getMatches(patd.getUnits(), Matches.UnitIsTransport));
    }
    return movedTransports.size() >= transportList.size();
  }

}
