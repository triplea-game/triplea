package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.triplea.delegate.GameDelegateBridge;
import games.strategy.triplea.delegate.battle.BattleResults;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

class BattleCalculator implements IBattleCalculator, Callable<AggregateResults> {
  private GameData gameData;
  private PlayerId attacker = null;
  private PlayerId defender = null;
  private Territory location = null;
  private Collection<Unit> attackingUnits = new ArrayList<>();
  private Collection<Unit> defendingUnits = new ArrayList<>();
  private Collection<Unit> bombardingUnits = new ArrayList<>();
  private Collection<TerritoryEffect> territoryEffects = new ArrayList<>();
  private boolean keepOneAttackingLandUnit = false;
  private boolean amphibious = false;
  private int retreatAfterRound = -1;
  private int retreatAfterXUnitsLeft = -1;
  private boolean retreatWhenOnlyAirLeft = false;
  private String attackerOrderOfLosses = null;
  private String defenderOrderOfLosses = null;
  private int runCount = 0;
  private volatile boolean cancelled = false;
  private volatile boolean isDataSet = false;
  private volatile boolean isCalcSet = false;
  private volatile boolean isRunning = false;

  BattleCalculator(final GameData data) {
    this(data, false);
  }

  BattleCalculator(final GameData data, final boolean dataHasAlreadyBeenCloned) {
    gameData =
        data == null
            ? null
            : (dataHasAlreadyBeenCloned ? data : GameDataUtils.cloneGameData(data, false));
    if (data != null) {
      isDataSet = true;
    }
  }

  @Override
  public void setGameData(final GameData data) {
    if (isRunning) {
      return;
    }
    isDataSet = false;
    isCalcSet = false;
    gameData = (data == null ? null : GameDataUtils.cloneGameData(data, false));
    // reset old data
    attacker = null;
    defender = null;
    location = null;
    attackingUnits = new ArrayList<>();
    defendingUnits = new ArrayList<>();
    bombardingUnits = new ArrayList<>();
    territoryEffects = new ArrayList<>();
    runCount = 0;
    isDataSet = data != null;
  }

  /** Calculates odds using the stored game data. */
  @Override
  public void setCalculateData(
      final PlayerId attacker,
      final PlayerId defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final Collection<Unit> bombarding,
      final Collection<TerritoryEffect> territoryEffects,
      final int runCount)
      throws IllegalStateException {
    if (isRunning) {
      return;
    }
    isCalcSet = false;
    if (!isDataSet) {
      throw new IllegalStateException("Called set calculation before setting game data!");
    }
    this.attacker =
        gameData
            .getPlayerList()
            .getPlayerId(attacker == null ? PlayerId.NULL_PLAYERID.getName() : attacker.getName());
    this.defender =
        gameData
            .getPlayerList()
            .getPlayerId(defender == null ? PlayerId.NULL_PLAYERID.getName() : defender.getName());
    this.location = gameData.getMap().getTerritory(location.getName());
    attackingUnits = GameDataUtils.translateIntoOtherGameData(attacking, gameData);
    defendingUnits = GameDataUtils.translateIntoOtherGameData(defending, gameData);
    bombardingUnits = GameDataUtils.translateIntoOtherGameData(bombarding, gameData);
    this.territoryEffects = GameDataUtils.translateIntoOtherGameData(territoryEffects, gameData);
    gameData.performChange(ChangeFactory.removeUnits(this.location, this.location.getUnits()));
    gameData.performChange(ChangeFactory.addUnits(this.location, attackingUnits));
    gameData.performChange(ChangeFactory.addUnits(this.location, defendingUnits));
    this.runCount = runCount;
    isCalcSet = true;
  }

  @Override
  public AggregateResults setCalculateDataAndCalculate(
      final PlayerId attacker,
      final PlayerId defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final Collection<Unit> bombarding,
      final Collection<TerritoryEffect> territoryEffects,
      final int runCount) {
    setCalculateData(
        attacker, defender, location, attacking, defending, bombarding, territoryEffects, runCount);
    return calculate();
  }

  @Override
  public AggregateResults calculate() {
    if (!getIsReady()) {
      throw new IllegalStateException("Called calculate before setting calculate data!");
    }
    return calculate(runCount);
  }

  private AggregateResults calculate(final int count) {
    isRunning = true;
    final long start = System.currentTimeMillis();
    final AggregateResults aggregateResults = new AggregateResults(count);
    final BattleTracker battleTracker = new BattleTracker();
    // CasualtySortingCaching can cause issues if there is more than 1 one battle being calced at
    // the same time (like if
    // the AI and a human are both using the calc)
    // TODO: first, see how much it actually speeds stuff up by, and if it does make a difference
    // then convert it to a
    // per-thread, per-calc caching
    final List<Unit> attackerOrderOfLosses =
        OrderOfLossesInputPanel.getUnitListByOrderOfLoss(
            this.attackerOrderOfLosses, attackingUnits, gameData);
    final List<Unit> defenderOrderOfLosses =
        OrderOfLossesInputPanel.getUnitListByOrderOfLoss(
            this.defenderOrderOfLosses, defendingUnits, gameData);
    for (int i = 0; i < count && !cancelled; i++) {
      final CompositeChange allChanges = new CompositeChange();
      final DummyDelegateBridge bridge1 =
          new DummyDelegateBridge(
              attacker,
              gameData,
              allChanges,
              attackerOrderOfLosses,
              defenderOrderOfLosses,
              keepOneAttackingLandUnit,
              retreatAfterRound,
              retreatAfterXUnitsLeft,
              retreatWhenOnlyAirLeft);
      final GameDelegateBridge bridge = new GameDelegateBridge(bridge1);
      final MustFightBattle battle =
          new MustFightBattle(location, attacker, gameData, battleTracker);
      battle.setHeadless(true);
      battle.setUnits(
          defendingUnits,
          attackingUnits,
          bombardingUnits,
          (amphibious ? attackingUnits : new ArrayList<>()),
          defender,
          territoryEffects);
      bridge1.setBattle(battle);
      battle.fight(bridge);
      aggregateResults.addResult(new BattleResults(battle, gameData));
      // restore the game to its original state
      gameData.performChange(allChanges.invert());
      battleTracker.clear();
      battleTracker.clearBattleRecords();
    }
    aggregateResults.setTime(System.currentTimeMillis() - start);
    isRunning = false;
    cancelled = false;
    return aggregateResults;
  }

  @Override
  public AggregateResults call() {
    return calculate();
  }

  @Override
  public boolean getIsReady() {
    return isDataSet && isCalcSet;
  }

  @Override
  public int getRunCount() {
    return runCount;
  }

  @Override
  public void setKeepOneAttackingLandUnit(final boolean bool) {
    keepOneAttackingLandUnit = bool;
  }

  @Override
  public void setAmphibious(final boolean bool) {
    amphibious = bool;
  }

  @Override
  public void setRetreatAfterRound(final int value) {
    retreatAfterRound = value;
  }

  @Override
  public void setRetreatAfterXUnitsLeft(final int value) {
    retreatAfterXUnitsLeft = value;
  }

  @Override
  public void setRetreatWhenOnlyAirLeft(final boolean value) {
    retreatWhenOnlyAirLeft = value;
  }

  @Override
  public void setAttackerOrderOfLosses(final String attackerOrderOfLosses) {
    this.attackerOrderOfLosses = attackerOrderOfLosses;
  }

  @Override
  public void setDefenderOrderOfLosses(final String defenderOrderOfLosses) {
    this.defenderOrderOfLosses = defenderOrderOfLosses;
  }

  @Override
  public void cancel() {
    cancelled = true;
  }

  @Override
  public void shutdown() {
    cancel();
  }

  @Override
  public int getThreadCount() {
    return 1;
  }
}
