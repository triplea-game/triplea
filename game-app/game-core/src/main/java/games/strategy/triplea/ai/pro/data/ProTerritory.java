package games.strategy.triplea.ai.pro.data;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.pro.util.ProMatches;
import games.strategy.triplea.ai.pro.util.ProOddsCalculator;
import games.strategy.triplea.delegate.Matches;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;

/** The result of an AI territory analysis. */
public class ProTerritory {

  private final ProData proData;
  @Getter private final Territory territory;
  @Getter private final Set<Unit> maxUnits;
  @Getter private final List<Unit> units;
  @Getter private final List<Unit> bombers;
  @Getter private ProBattleResult maxBattleResult;
  @Getter private double value;
  @Getter private double seaValue;
  @Getter private boolean canHold;
  @Getter private boolean canAttack;
  @Getter private double strengthEstimate;

  // Amphib variables
  @Getter private final List<Unit> maxAmphibUnits;
  @Getter private final Map<Unit, List<Unit>> amphibAttackMap;
  @Getter private final Map<Unit, Territory> transportTerritoryMap;
  @Getter private boolean needAmphibUnits;
  @Getter private boolean strafing;
  @Getter private final Map<Unit, Boolean> isTransportingMap;
  @Getter private final Set<Unit> maxBombardUnits;
  @Getter private final Map<Unit, Set<Territory>> bombardOptionsMap;
  @Getter private final Map<Unit, Territory> bombardTerritoryMap;

  // Determine territory to attack variables
  @Getter private boolean currentlyWins;
  @Getter private ProBattleResult battleResult;

  // Non-combat move variables
  private final Set<Unit> cantMoveUnits;
  @Getter private List<Unit> maxEnemyUnits;
  @Getter private Set<Unit> maxEnemyBombardUnits;
  @Getter private ProBattleResult minBattleResult;
  @Getter private final List<Unit> tempUnits;
  @Getter private final Map<Unit, List<Unit>> tempAmphibAttackMap;
  @Getter private double loadValue;

  // Scramble variables
  @Getter private final List<Unit> maxScrambleUnits;

  public ProTerritory(final Territory territory, final ProData proData) {
    this.territory = territory;
    this.proData = proData;
    maxUnits = new HashSet<>();
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

    cantMoveUnits = new HashSet<>();
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
    maxUnits = new HashSet<>(patd.getMaxUnits());
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

    cantMoveUnits = new HashSet<>(patd.getCantMoveUnits());
    maxEnemyUnits = new ArrayList<>(patd.getMaxEnemyUnits());
    maxEnemyBombardUnits = new HashSet<>(patd.getMaxEnemyBombardUnits());
    minBattleResult = patd.getMinBattleResult();
    tempUnits = new ArrayList<>(patd.getTempUnits());
    tempAmphibAttackMap = new HashMap<>(patd.getTempAmphibAttackMap());
    loadValue = patd.getLoadValue();

    maxScrambleUnits = new ArrayList<>(patd.getMaxScrambleUnits());
  }

  public Collection<Unit> getAllDefenders() {
    final Set<Unit> defenders = new HashSet<>(units);
    defenders.addAll(cantMoveUnits);
    // tempUnits can already be in the units/cantMoveUnits collection
    defenders.addAll(tempUnits);
    return defenders;
  }

  public Collection<Unit> getEligibleDefenders(GamePlayer player) {
    Collection<Unit> defendingUnits = getAllDefenders();
    if (getTerritory().isWater()) {
      return defendingUnits;
    }
    return CollectionUtils.getMatches(
        defendingUnits, ProMatches.unitIsAlliedNotOwnedAir(player).negate());
  }

  public Collection<Unit> getAllDefendersForCarrierCalcs(
      final GameState data, final GamePlayer player) {
    if (Properties.getProduceNewFightersOnOldCarriers(data.getProperties())) {
      return getAllDefenders();
    }

    final Set<Unit> defenders =
        new HashSet<>(
            CollectionUtils.getMatches(
                cantMoveUnits, ProMatches.unitIsOwnedCarrier(player).negate()));
    defenders.addAll(units);
    // tempUnits can already be in the units/cantMoveUnits collection
    defenders.addAll(tempUnits);
    return defenders;
  }

  public List<Unit> getMaxDefenders() {
    final List<Unit> defenders = new ArrayList<>(maxUnits);
    defenders.addAll(cantMoveUnits);
    return defenders;
  }

  public List<Unit> getMaxEnemyDefenders(final GamePlayer player) {
    final List<Unit> defenders =
        territory.getUnitCollection().getMatches(Matches.enemyUnit(player));
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

  public void setValue(final double value) {
    this.value = value;
  }

  public void setCanHold(final boolean canHold) {
    this.canHold = canHold;
  }

  public void setNeedAmphibUnits(final boolean needAmphibUnits) {
    this.needAmphibUnits = needAmphibUnits;
  }

  public void setStrafing(final boolean strafing) {
    this.strafing = strafing;
  }

  public void putAllAmphibAttackMap(final Map<Unit, List<Unit>> amphibAttackMap) {
    amphibAttackMap.forEach(this::putAmphibAttackMap);
  }

  public void putAmphibAttackMap(final Unit transport, final List<Unit> amphibUnits) {
    this.amphibAttackMap.put(transport, amphibUnits);
    this.isTransportingMap.put(
        transport, transport.isTransporting(proData.getUnitTerritory(transport)));
  }

  public void setCanAttack(final boolean canAttack) {
    this.canAttack = canAttack;
  }

  public void setStrengthEstimate(final double strengthEstimate) {
    this.strengthEstimate = strengthEstimate;
  }

  public void estimateBattleResult(final ProOddsCalculator calc, final GamePlayer player) {
    setBattleResult(
        calc.estimateAttackBattleResults(
            proData,
            territory,
            getUnits(),
            getMaxEnemyDefenders(player),
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

  public void setBattleResultIfNull(final Supplier<ProBattleResult> supplier) {
    if (battleResult == null) {
      setBattleResult(supplier.get());
    }
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

  public Collection<Unit> getCantMoveUnits() {
    return Collections.unmodifiableCollection(cantMoveUnits);
  }

  public void addCantMoveUnit(final Unit unit) {
    this.cantMoveUnits.add(unit);
  }

  public void addCantMoveUnits(final Collection<Unit> units) {
    this.cantMoveUnits.addAll(units);
  }

  public void setMaxEnemyUnits(final Collection<Unit> maxEnemyUnits) {
    this.maxEnemyUnits = new ArrayList<>(maxEnemyUnits);
  }

  public void setMinBattleResult(final ProBattleResult minBattleResult) {
    this.minBattleResult = minBattleResult;
  }

  public void addTempUnit(final Unit unit) {
    this.tempUnits.add(unit);
  }

  public void addTempUnits(final List<Unit> units) {
    this.tempUnits.addAll(units);
  }

  public void putTempAmphibAttackMap(final Unit transport, final List<Unit> amphibUnits) {
    this.tempAmphibAttackMap.put(transport, amphibUnits);
  }

  public void setLoadValue(final double loadValue) {
    this.loadValue = loadValue;
  }

  public void setSeaValue(final double seaValue) {
    this.seaValue = seaValue;
  }

  void addMaxBombardUnit(final Unit unit) {
    this.maxBombardUnits.add(unit);
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

  public void setMaxBattleResult(final ProBattleResult maxBattleResult) {
    this.maxBattleResult = maxBattleResult;
  }

  public Set<Territory> getNeighbors(Predicate<Territory> predicate) {
    return proData.getData().getMap().getNeighbors(territory, predicate);
  }
}
