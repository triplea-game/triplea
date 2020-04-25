package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
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
import lombok.Setter;

class BattleCalculator implements IBattleCalculator {
  private GameData gameData;
  private GamePlayer attacker = null;
  private GamePlayer defender = null;
  private Territory location = null;
  private Collection<Unit> attackingUnits = new ArrayList<>();
  private Collection<Unit> defendingUnits = new ArrayList<>();
  private Collection<Unit> bombardingUnits = new ArrayList<>();
  private Collection<TerritoryEffect> territoryEffects = new ArrayList<>();
  @Setter private boolean keepOneAttackingLandUnit = false;
  @Setter private boolean amphibious = false;
  @Setter private int retreatAfterRound = -1;
  @Setter private int retreatAfterXUnitsLeft = -1;

  @Setter(onMethod_ = {@Override})
  private boolean retreatWhenOnlyAirLeft = false;

  @Setter private String attackerOrderOfLosses = null;
  @Setter private String defenderOrderOfLosses = null;
  private volatile boolean cancelled = false;
  private volatile boolean isDataSet = false;
  private volatile boolean isCalcSet = false;
  private volatile boolean isRunning = false;

  public void setGameData(final GameData data) {
    if (isRunning) {
      return;
    }
    isDataSet = data != null;
    isCalcSet = false;
    gameData = data;
    // reset old data
    attacker = null;
    defender = null;
    location = null;
    attackingUnits = new ArrayList<>();
    defendingUnits = new ArrayList<>();
    bombardingUnits = new ArrayList<>();
    territoryEffects = new ArrayList<>();
  }

  /** Calculates odds using the stored game data. */
  private void setCalculateData(
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final Collection<Unit> bombarding,
      final Collection<TerritoryEffect> territoryEffects)
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
            .getPlayerId(
                attacker == null ? GamePlayer.NULL_PLAYERID.getName() : attacker.getName());
    this.defender =
        gameData
            .getPlayerList()
            .getPlayerId(
                defender == null ? GamePlayer.NULL_PLAYERID.getName() : defender.getName());
    this.location = gameData.getMap().getTerritory(location.getName());
    attackingUnits = GameDataUtils.translateIntoOtherGameData(attacking, gameData);
    defendingUnits = GameDataUtils.translateIntoOtherGameData(defending, gameData);
    bombardingUnits = GameDataUtils.translateIntoOtherGameData(bombarding, gameData);
    this.territoryEffects = GameDataUtils.translateIntoOtherGameData(territoryEffects, gameData);
    gameData.performChange(ChangeFactory.removeUnits(this.location, this.location.getUnits()));
    gameData.performChange(ChangeFactory.addUnits(this.location, attackingUnits));
    gameData.performChange(ChangeFactory.addUnits(this.location, defendingUnits));
    isCalcSet = true;
  }

  @Override
  public AggregateResults calculate(
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final Collection<Unit> bombarding,
      final Collection<TerritoryEffect> territoryEffects,
      final int runCount) {
    setCalculateData(
        attacker, defender, location, attacking, defending, bombarding, territoryEffects);
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
    // CasualtySortingCaching can cause issues if there is more than 1 one battle being calculated
    // at the same time (like if the AI and a human are both using the calc)
    // TODO: first, see how much it actually speeds stuff up by, and if it does make a difference
    // then convert it to a per-thread, per-calc caching
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

  public boolean getIsReady() {
    return isDataSet && isCalcSet;
  }

  public void cancel() {
    cancelled = true;
  }

  @Override
  public int getThreadCount() {
    return 1;
  }
}
