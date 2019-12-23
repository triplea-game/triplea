package games.strategy.triplea.ai.pro.data;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.util.ProMatches;
import games.strategy.triplea.ai.pro.util.ProOddsCalculator;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TransportTracker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.triplea.java.collections.CollectionUtils;

/** The result of an AI territory analysis. */
public class ProTerritory {

  private final ProData proData;
  private final Territory territory;
  private final List<Unit> maxUnits;
  private final List<Unit> units;
  private final List<Unit> bombers;
  private ProBattleResult maxBattleResult;
  private double value;
  private double seaValue;
  private boolean canHold;
  private boolean canAttack;
  private double strengthEstimate;

  // Amphib variables
  private final List<Unit> maxAmphibUnits;
  private final Map<Unit, List<Unit>> amphibAttackMap;
  private final Map<Unit, Territory> transportTerritoryMap;
  private boolean needAmphibUnits;
  private boolean strafing;
  private final Map<Unit, Boolean> isTransportingMap;
  private final Set<Unit> maxBombardUnits;
  private final Map<Unit, Set<Territory>> bombardOptionsMap;
  private final Map<Unit, Territory> bombardTerritoryMap;

  // Determine territory to attack variables
  private boolean currentlyWins;
  private ProBattleResult battleResult;

  // Non-combat move variables
  private final List<Unit> cantMoveUnits;
  private List<Unit> maxEnemyUnits;
  private Set<Unit> maxEnemyBombardUnits;
  private ProBattleResult minBattleResult;
  private final List<Unit> tempUnits;
  private final Map<Unit, List<Unit>> tempAmphibAttackMap;
  private double loadValue;

  // Scramble variables
  private final List<Unit> maxScrambleUnits;

  public ProTerritory(final Territory territory, final ProData proData) {
    this.territory = territory;
    this.proData = proData;
    maxUnits = new ArrayList<>();
    units = new ArrayList<>();
    bombers = new ArrayList<>();
    maxBattleResult = new ProBattleResult();
    value = 0;
    seaValue = 0;
    canHold = true;
    canAttack = false;
    strengthEstimate = Double.POSITIVE_INFINITY;

    maxAmphibUnits = new ArrayList<>();
    amphibAttackMap = new HashMap<>();
    transportTerritoryMap = new HashMap<>();
    needAmphibUnits = false;
    strafing = false;
    isTransportingMap = new HashMap<>();
    maxBombardUnits = new HashSet<>();
    bombardOptionsMap = new HashMap<>();
    bombardTerritoryMap = new HashMap<>();

    currentlyWins = false;
    battleResult = null;

    cantMoveUnits = new ArrayList<>();
    maxEnemyUnits = new ArrayList<>();
    maxEnemyBombardUnits = new HashSet<>();
    minBattleResult = new ProBattleResult();
    tempUnits = new ArrayList<>();
    tempAmphibAttackMap = new HashMap<>();
    loadValue = 0;

    maxScrambleUnits = new ArrayList<>();
  }

  ProTerritory(final ProTerritory patd, final ProData proData) {
    this.territory = patd.getTerritory();
    this.proData = proData;
    maxUnits = new ArrayList<>(patd.getMaxUnits());
    units = new ArrayList<>(patd.getUnits());
    bombers = new ArrayList<>(patd.getBombers());
    maxBattleResult = patd.getMaxBattleResult();
    value = patd.getValue();
    seaValue = patd.getSeaValue();
    canHold = patd.isCanHold();
    canAttack = patd.isCanAttack();
    strengthEstimate = patd.getStrengthEstimate();

    maxAmphibUnits = new ArrayList<>(patd.getMaxAmphibUnits());
    amphibAttackMap = new HashMap<>(patd.getAmphibAttackMap());
    transportTerritoryMap = new HashMap<>(patd.getTransportTerritoryMap());
    needAmphibUnits = patd.isNeedAmphibUnits();
    strafing = patd.isStrafing();
    isTransportingMap = new HashMap<>(patd.getIsTransportingMap());
    maxBombardUnits = new HashSet<>(patd.getMaxBombardUnits());
    bombardOptionsMap = new HashMap<>(patd.getBombardOptionsMap());
    bombardTerritoryMap = new HashMap<>(patd.getBombardTerritoryMap());

    currentlyWins = patd.isCurrentlyWins();
    battleResult = patd.getBattleResult();

    cantMoveUnits = new ArrayList<>(patd.getCantMoveUnits());
    maxEnemyUnits = new ArrayList<>(patd.getMaxEnemyUnits());
    maxEnemyBombardUnits = new HashSet<>(patd.getMaxEnemyBombardUnits());
    minBattleResult = patd.getMinBattleResult();
    tempUnits = new ArrayList<>(patd.getTempUnits());
    tempAmphibAttackMap = new HashMap<>(patd.getTempAmphibAttackMap());
    loadValue = patd.getLoadValue();

    maxScrambleUnits = new ArrayList<>(patd.getMaxScrambleUnits());
  }

  public List<Unit> getAllDefenders() {
    final List<Unit> defenders = new ArrayList<>(units);
    defenders.addAll(cantMoveUnits);
    defenders.addAll(tempUnits);
    return defenders;
  }

  public List<Unit> getAllDefendersForCarrierCalcs(final GameData data, final PlayerId player) {
    if (Properties.getProduceNewFightersOnOldCarriers(data)) {
      return getAllDefenders();
    }

    final List<Unit> defenders =
        CollectionUtils.getMatches(cantMoveUnits, ProMatches.unitIsOwnedCarrier(player).negate());
    defenders.addAll(units);
    defenders.addAll(tempUnits);
    return defenders;
  }

  public List<Unit> getMaxDefenders() {
    final List<Unit> defenders = new ArrayList<>(maxUnits);
    defenders.addAll(cantMoveUnits);
    return defenders;
  }

  public List<Unit> getMaxEnemyDefenders(final PlayerId player, final GameData data) {
    final List<Unit> defenders =
        territory.getUnitCollection().getMatches(Matches.enemyUnit(player, data));
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

  void addMaxAmphibUnits(final List<Unit> amphibUnits) {
    this.maxAmphibUnits.addAll(amphibUnits);
  }

  void addMaxUnit(final Unit unit) {
    this.maxUnits.add(unit);
  }

  void addMaxUnits(final List<Unit> units) {
    this.maxUnits.addAll(units);
  }

  public Territory getTerritory() {
    return territory;
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

  public List<Unit> getUnits() {
    return units;
  }

  public void setCanHold(final boolean canHold) {
    this.canHold = canHold;
  }

  public boolean isCanHold() {
    return canHold;
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

  public void estimateBattleResult(final ProOddsCalculator calc, final PlayerId player) {
    setBattleResult(
        calc.estimateAttackBattleResults(
            proData,
            territory,
            getUnits(),
            getMaxEnemyDefenders(player, player.getData()),
            getBombardTerritoryMap().keySet()));
  }

  public void setBattleResult(final ProBattleResult battleResult) {
    this.battleResult = battleResult;
    if (battleResult == null) {
      currentlyWins = false;
    } else if (battleResult.getWinPercentage() >= proData.getWinPercentage()
        && battleResult.isHasLandUnitRemaining()) {
      currentlyWins = true;
    }
  }

  public ProBattleResult getBattleResult() {
    return battleResult;
  }

  /** Returns a description of the battle result in this territory. */
  public String getResultString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("territory=").append(territory.getName());
    if (battleResult != null) {
      sb.append(", win%=").append(battleResult.getWinPercentage());
      sb.append(", TUVSwing=").append(battleResult.getTuvSwing());
      sb.append(", hasRemainingLandUnit=").append(battleResult.isHasLandUnitRemaining());
    }
    return sb.toString();
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

  public Set<Unit> getMaxBombardUnits() {
    return maxBombardUnits;
  }

  void addMaxBombardUnit(final Unit unit) {
    this.maxBombardUnits.add(unit);
  }

  public Map<Unit, Set<Territory>> getBombardOptionsMap() {
    return bombardOptionsMap;
  }

  void addBombardOptionsMap(final Unit unit, final Territory t) {
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

  public List<Unit> getMaxScrambleUnits() {
    return maxScrambleUnits;
  }

  public List<Unit> getBombers() {
    return bombers;
  }
}
