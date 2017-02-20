package games.strategy.triplea.ai.proAI.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.proAI.ProData;
import games.strategy.triplea.ai.proAI.util.ProMatches;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.util.Match;

public class ProTerritory {

  private Territory territory;
  private List<Unit> maxUnits;
  private List<Unit> units;
  private List<Unit> bombers;
  private ProBattleResult maxBattleResult;
  private double value;
  private double seaValue;
  private boolean canHold;
  private boolean canAttack;
  private double strengthEstimate;

  // Amphib variables
  private List<Unit> maxAmphibUnits;
  private Map<Unit, List<Unit>> amphibAttackMap;
  private final Map<Unit, Territory> transportTerritoryMap;
  private boolean needAmphibUnits;
  private boolean strafing;
  private Map<Unit, Boolean> isTransportingMap;
  private Set<Unit> maxBombardUnits;
  private Map<Unit, Set<Territory>> bombardOptionsMap;
  private final Map<Unit, Territory> bombardTerritoryMap;

  // Determine territory to attack variables
  private boolean currentlyWins;
  private ProBattleResult battleResult;

  // Non-combat move variables
  private List<Unit> cantMoveUnits;
  private List<Unit> maxEnemyUnits;
  private Set<Unit> maxEnemyBombardUnits;
  private ProBattleResult minBattleResult;
  private final List<Unit> tempUnits;
  private final Map<Unit, List<Unit>> tempAmphibAttackMap;
  private double loadValue;

  // Scramble variables
  private List<Unit> maxScrambleUnits;

  public ProTerritory(final Territory territory) {
    this.territory = territory;
    maxUnits = new ArrayList<>();
    units = new ArrayList<>();
    bombers = new ArrayList<>();
    cantMoveUnits = new ArrayList<>();
    maxEnemyUnits = new ArrayList<>();
    maxEnemyBombardUnits = new HashSet<>();
    maxBattleResult = new ProBattleResult();
    canHold = true;
    canAttack = false;
    strengthEstimate = Double.POSITIVE_INFINITY;
    maxAmphibUnits = new ArrayList<>();
    maxBombardUnits = new HashSet<>();
    needAmphibUnits = false;
    strafing = false;
    amphibAttackMap = new HashMap<>();
    isTransportingMap = new HashMap<>();
    transportTerritoryMap = new HashMap<>();
    bombardOptionsMap = new HashMap<>();
    bombardTerritoryMap = new HashMap<>();
    currentlyWins = false;
    battleResult = null;
    minBattleResult = new ProBattleResult();
    tempUnits = new ArrayList<>();
    tempAmphibAttackMap = new HashMap<>();
    loadValue = 0;
    value = 0;
    seaValue = 0;
    maxScrambleUnits = new ArrayList<>();
  }

  public ProTerritory(final ProTerritory patd) {
    this.territory = patd.getTerritory();
    maxUnits = new ArrayList<>(patd.getMaxUnits());
    units = new ArrayList<>(patd.getUnits());
    bombers = new ArrayList<>(patd.getBombers());
    cantMoveUnits = new ArrayList<>(patd.getCantMoveUnits());
    maxEnemyUnits = new ArrayList<>(patd.getMaxEnemyUnits());
    maxEnemyBombardUnits = new HashSet<>(patd.getMaxEnemyBombardUnits());
    minBattleResult = patd.getMaxBattleResult();
    canHold = patd.isCanHold();
    canAttack = patd.isCanAttack();
    strengthEstimate = patd.getStrengthEstimate();
    maxAmphibUnits = new ArrayList<>(patd.getMaxAmphibUnits());
    maxBombardUnits = new HashSet<>(patd.getMaxBombardUnits());
    needAmphibUnits = patd.isNeedAmphibUnits();
    strafing = patd.isStrafing();
    amphibAttackMap = new HashMap<>(patd.getAmphibAttackMap());
    isTransportingMap = new HashMap<>(patd.getIsTransportingMap());
    transportTerritoryMap = new HashMap<>(patd.getTransportTerritoryMap());
    bombardOptionsMap = new HashMap<>(patd.getBombardOptionsMap());
    bombardTerritoryMap = new HashMap<>(patd.getBombardTerritoryMap());
    currentlyWins = patd.isCurrentlyWins();
    battleResult = patd.getBattleResult();
    minBattleResult = patd.getMinBattleResult();
    tempUnits = new ArrayList<>(patd.getTempUnits());
    tempAmphibAttackMap = new HashMap<>(patd.getTempAmphibAttackMap());
    loadValue = patd.getLoadValue();
    value = patd.getValue();
    seaValue = patd.getSeaValue();
    maxScrambleUnits = new ArrayList<>(patd.getMaxScrambleUnits());
  }

  public List<Unit> getAllDefenders() {
    final List<Unit> defenders = new ArrayList<>(units);
    defenders.addAll(cantMoveUnits);
    defenders.addAll(tempUnits);
    return defenders;
  }

  public List<Unit> getAllDefendersForCarrierCalcs(final GameData data, final PlayerID player) {
    if (Properties.getProduceNewFightersOnOldCarriers(data)) {
      return getAllDefenders();
    } else {
      final List<Unit> defenders = Match.getMatches(cantMoveUnits, ProMatches.UnitIsOwnedCarrier(player).invert());
      defenders.addAll(units);
      defenders.addAll(tempUnits);
      return defenders;
    }
  }

  public List<Unit> getMaxDefenders() {
    final List<Unit> defenders = new ArrayList<>(maxUnits);
    defenders.addAll(cantMoveUnits);
    return defenders;
  }

  public List<Unit> getMaxEnemyDefenders(final PlayerID player, final GameData data) {
    final List<Unit> defenders = territory.getUnits().getMatches(Matches.enemyUnit(player, data));
    defenders.addAll(maxScrambleUnits);
    return defenders;
  }

  @Override
  public String toString() {
    return territory.getName();
  }

  public void addUnit(final Unit unit) {
    this.units.add(unit);
  }

  public void addUnits(final List<Unit> units) {
    this.units.addAll(units);
  }

  public void addMaxUnits(final List<Unit> units) {
    this.maxUnits.addAll(units);
  }

  public void addMaxAmphibUnits(final List<Unit> amphibUnits) {
    this.maxAmphibUnits.addAll(amphibUnits);
  }

  public void addMaxUnit(final Unit unit) {
    this.maxUnits.add(unit);
  }

  public void setTerritory(final Territory territory) {
    this.territory = territory;
  }

  public Territory getTerritory() {
    return territory;
  }

  public void setMaxUnits(final List<Unit> units) {
    this.maxUnits = units;
  }

  public List<Unit> getMaxUnits() {
    return maxUnits;
  }

  public void setValue(final double value) {
    this.value = value;
  }

  public double getValue() {
    return value;
  }

  public void setUnits(final List<Unit> units) {
    this.units = units;
  }

  public List<Unit> getUnits() {
    return units;
  }

  public void setCanHold(final boolean canHold) {
    this.canHold = canHold;
  }

  public boolean isCanHold() {
    return canHold;
  }

  public void setMaxAmphibUnits(final List<Unit> maxAmphibUnits) {
    this.maxAmphibUnits = maxAmphibUnits;
  }

  public List<Unit> getMaxAmphibUnits() {
    return maxAmphibUnits;
  }

  public void setNeedAmphibUnits(final boolean needAmphibUnits) {
    this.needAmphibUnits = needAmphibUnits;
  }

  public boolean isNeedAmphibUnits() {
    return needAmphibUnits;
  }

  public boolean isStrafing() {
    return strafing;
  }

  public void setStrafing(final boolean strafing) {
    this.strafing = strafing;
  }

  public Map<Unit, List<Unit>> getAmphibAttackMap() {
    return amphibAttackMap;
  }

  public void setAmphibAttackMap(final Map<Unit, List<Unit>> amphibAttackMap) {
    this.amphibAttackMap = amphibAttackMap;
  }

  public void putAllAmphibAttackMap(final Map<Unit, List<Unit>> amphibAttackMap) {
    for (final Unit u : amphibAttackMap.keySet()) {
      putAmphibAttackMap(u, amphibAttackMap.get(u));
    }
  }

  public void putAmphibAttackMap(final Unit transport, final List<Unit> amphibUnits) {
    this.amphibAttackMap.put(transport, amphibUnits);
    this.isTransportingMap.put(transport, TransportTracker.isTransporting(transport));
  }

  public void setCanAttack(final boolean canAttack) {
    this.canAttack = canAttack;
  }

  public boolean isCanAttack() {
    return canAttack;
  }

  public void setStrengthEstimate(final double strengthEstimate) {
    this.strengthEstimate = strengthEstimate;
  }

  public double getStrengthEstimate() {
    return strengthEstimate;
  }

  public boolean isCurrentlyWins() {
    return currentlyWins;
  }

  public void setBattleResult(final ProBattleResult battleResult) {
    this.battleResult = battleResult;
    if (battleResult == null) {
      currentlyWins = false;
    } else if (battleResult.getWinPercentage() >= ProData.winPercentage && battleResult.isHasLandUnitRemaining()) {
      currentlyWins = true;
    }
  }

  public ProBattleResult getBattleResult() {
    return battleResult;
  }

  public String getResultString() {
    if (battleResult == null) {
      return "territory=" + territory.getName();
    } else {
      return "territory=" + territory.getName() + ", win%=" + battleResult.getWinPercentage() + ", TUVSwing="
          + battleResult.getTUVSwing() + ", hasRemainingLandUnit=" + battleResult.isHasLandUnitRemaining();
    }
  }

  public void setCantMoveUnits(final List<Unit> cantMoveUnits) {
    this.cantMoveUnits = cantMoveUnits;
  }

  public List<Unit> getCantMoveUnits() {
    return cantMoveUnits;
  }

  public void addCantMoveUnit(final Unit unit) {
    this.cantMoveUnits.add(unit);
  }

  public void setMaxEnemyUnits(final List<Unit> maxEnemyUnits) {
    this.maxEnemyUnits = maxEnemyUnits;
  }

  public List<Unit> getMaxEnemyUnits() {
    return maxEnemyUnits;
  }

  public void setMinBattleResult(final ProBattleResult minBattleResult) {
    this.minBattleResult = minBattleResult;
  }

  public ProBattleResult getMinBattleResult() {
    return minBattleResult;
  }

  public List<Unit> getTempUnits() {
    return tempUnits;
  }

  public void addTempUnit(final Unit unit) {
    this.tempUnits.add(unit);
  }

  public void addTempUnits(final List<Unit> units) {
    this.tempUnits.addAll(units);
  }

  public Map<Unit, List<Unit>> getTempAmphibAttackMap() {
    return tempAmphibAttackMap;
  }

  public void putTempAmphibAttackMap(final Unit transport, final List<Unit> amphibUnits) {
    this.tempAmphibAttackMap.put(transport, amphibUnits);
  }

  public Map<Unit, Territory> getTransportTerritoryMap() {
    return transportTerritoryMap;
  }

  public void setLoadValue(final double loadValue) {
    this.loadValue = loadValue;
  }

  public double getLoadValue() {
    return loadValue;
  }

  public void setIsTransportingMap(final Map<Unit, Boolean> isTransportingMap) {
    this.isTransportingMap = isTransportingMap;
  }

  public Map<Unit, Boolean> getIsTransportingMap() {
    return isTransportingMap;
  }

  public void setSeaValue(final double seaValue) {
    this.seaValue = seaValue;
  }

  public double getSeaValue() {
    return seaValue;
  }

  public Map<Unit, Territory> getBombardTerritoryMap() {
    return bombardTerritoryMap;
  }

  public void setMaxBombardUnits(final Set<Unit> maxBombardUnits) {
    this.maxBombardUnits = maxBombardUnits;
  }

  public Set<Unit> getMaxBombardUnits() {
    return maxBombardUnits;
  }

  public void addMaxBombardUnit(final Unit unit) {
    this.maxBombardUnits.add(unit);
  }

  public void setBombardOptionsMap(final Map<Unit, Set<Territory>> bombardOptionsMap) {
    this.bombardOptionsMap = bombardOptionsMap;
  }

  public Map<Unit, Set<Territory>> getBombardOptionsMap() {
    return bombardOptionsMap;
  }

  public void addBombardOptionsMap(final Unit unit, final Territory t) {
    if (bombardOptionsMap.containsKey(unit)) {
      bombardOptionsMap.get(unit).add(t);
    } else {
      final Set<Territory> territories = new HashSet<>();
      territories.add(t);
      bombardOptionsMap.put(unit, territories);
    }
  }

  public void setMaxEnemyBombardUnits(final Set<Unit> maxEnemyBombardUnits) {
    this.maxEnemyBombardUnits = maxEnemyBombardUnits;
  }

  public Set<Unit> getMaxEnemyBombardUnits() {
    return maxEnemyBombardUnits;
  }

  public void setMaxBattleResult(final ProBattleResult maxBattleResult) {
    this.maxBattleResult = maxBattleResult;
  }

  public ProBattleResult getMaxBattleResult() {
    return maxBattleResult;
  }

  public void setMaxScrambleUnits(final List<Unit> maxScrambleUnits) {
    this.maxScrambleUnits = maxScrambleUnits;
  }

  public List<Unit> getMaxScrambleUnits() {
    return maxScrambleUnits;
  }

  public List<Unit> getBombers() {
    return bombers;
  }

  public void setBombers(final List<Unit> bombers) {
    this.bombers = bombers;
  }
}
