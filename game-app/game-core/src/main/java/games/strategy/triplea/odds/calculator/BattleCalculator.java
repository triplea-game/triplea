package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.Constants.EDIT_MODE;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.triplea.delegate.battle.BattleResults;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.odds.calculator.adapter.AggregateResultsBridge;
import games.strategy.triplea.odds.calculator.adapter.EngineRandomSource;
import games.strategy.triplea.odds.calculator.adapter.GameDataBattleAdapter;
import games.strategy.triplea.odds.calculator.context.model.BattleOptions;
import games.strategy.triplea.odds.calculator.context.model.BattleScenario;
import games.strategy.triplea.odds.calculator.context.model.SimulationResults;
import games.strategy.triplea.odds.calculator.context.model.UnitTypeId;
import games.strategy.triplea.odds.calculator.context.reference.ReferenceBattleSimulator;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.util.TuvCostsCalculator;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Setter;

class BattleCalculator implements IBattleCalculator {
  @Nonnull private final GameData gameData;
  // Use a single TuvCostsCalculator so its computations are cached.
  private final TuvCostsCalculator tuvCalculator = new TuvCostsCalculator();
  @Setter private boolean keepOneAttackingLandUnit = false;
  @Setter private boolean amphibious = false;
  @Setter private int retreatAfterRound = -1;
  @Setter private int retreatAfterXUnitsLeft = -1;
  @Setter private String attackerOrderOfLosses = null;
  @Setter private String defenderOrderOfLosses = null;
  // Injectable so tests can supply a seeded or scripted source for reproducible simulations.
  @Setter private IRandomSource randomSource = new PlainRandomSource();
  private volatile boolean cancelled = false;
  private final AtomicBoolean isRunning = new AtomicBoolean(false);

  BattleCalculator(GameData data) {
    gameData =
        GameDataUtils.cloneGameData(data, GameDataManager.Options.forBattleCalculator())
            .orElseThrow();
  }

  BattleCalculator(byte[] data) {
    gameData = GameDataUtils.createGameDataFromBytes(data).orElseThrow();
    gameData.getProperties().set(EDIT_MODE, false);
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
      final boolean retreatWhenOnlyAirLeft,
      final int runCount) {
    Preconditions.checkState(
        !isRunning.getAndSet(true), "Can't calculate while operation is still running!");
    try {
      final GamePlayer attacker2 =
          attacker == null
              ? gameData.getPlayerList().getNullPlayer()
              : gameData.getPlayerList().getPlayerId(attacker.getName());
      final GamePlayer defender2 =
          defender == null
              ? gameData.getPlayerList().getNullPlayer()
              : gameData.getPlayerList().getPlayerId(defender.getName());
      final Territory location2 = gameData.getMap().getTerritoryOrNull(location.getName());
      final Collection<Unit> attackingUnits =
          translateCollectionIntoOtherGameData(attacking, gameData);
      final Collection<Unit> defendingUnits =
          translateCollectionIntoOtherGameData(defending, gameData);
      final Collection<Unit> bombardingUnits =
          translateCollectionIntoOtherGameData(bombarding, gameData);
      final Collection<TerritoryEffect> territoryEffects2 =
          translateCollectionIntoOtherGameData(territoryEffects, gameData);
      if (ClientSetting.useBoundedContextBattleCalc.getSetting()) {
        return calculateWithBoundedContext(
            attacker2,
            defender2,
            location2,
            attackingUnits,
            defendingUnits,
            bombardingUnits,
            territoryEffects2,
            retreatWhenOnlyAirLeft,
            runCount);
      }
      gameData.performChange(ChangeFactory.removeUnits(location2, location2.getUnits()));
      gameData.performChange(
          ChangeFactory.addUnits(location2, mergeUnitCollections(attackingUnits, defendingUnits)));
      final long start = System.currentTimeMillis();
      final AggregateResults aggregateResults = new AggregateResults(runCount);
      final BattleTracker battleTracker = new BattleTracker();
      final List<Unit> attackerOrderOfLosses =
          OrderOfLossesInputPanel.getUnitListByOrderOfLoss(
              this.attackerOrderOfLosses, attackingUnits, gameData);
      final List<Unit> defenderOrderOfLosses =
          OrderOfLossesInputPanel.getUnitListByOrderOfLoss(
              this.defenderOrderOfLosses, defendingUnits, gameData);
      for (int i = 0; i < runCount && !cancelled; i++) {
        final CompositeChange allChanges = new CompositeChange();
        final DummyDelegateBridge bridge =
            new DummyDelegateBridge(
                attacker2,
                gameData,
                allChanges,
                attackerOrderOfLosses,
                defenderOrderOfLosses,
                keepOneAttackingLandUnit,
                retreatAfterRound,
                retreatAfterXUnitsLeft,
                retreatWhenOnlyAirLeft,
                tuvCalculator,
                randomSource);
        final MustFightBattle battle =
            new MustFightBattle(location2, attacker2, gameData, battleTracker);
        battle.setHeadless(true);
        if (amphibious) {
          attackingUnits.forEach(unit -> unit.setWasAmphibious(true));
        }
        battle.setUnits(
            defendingUnits, attackingUnits, bombardingUnits, defender2, territoryEffects2);
        bridge.setBattle(battle);
        battle.fight(bridge);
        aggregateResults.addResult(new BattleResults(battle, gameData));
        // restore the game to its original state
        gameData.performChange(allChanges.invert());
        battleTracker.clear();
        battleTracker.clearBattleRecords();
      }
      aggregateResults.setTime(System.currentTimeMillis() - start);
      cancelled = false;
      return aggregateResults;
    } finally {
      isRunning.set(false);
    }
  }

  /**
   * The experimental bounded-context path (flag-gated): bakes a self-contained {@link
   * BattleScenario} from the cloned game data, runs the engine-free {@link
   * ReferenceBattleSimulator} over the caller's real {@link IRandomSource}, and bridges the
   * survivor forces back into an {@link AggregateResults} the existing accessors understand. No map
   * mutation happens here — the adapter only reads — so this returns before the {@code
   * MustFightBattle} path's setup.
   */
  private AggregateResults calculateWithBoundedContext(
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final Collection<Unit> bombarding,
      final Collection<TerritoryEffect> territoryEffects,
      final boolean retreatWhenOnlyAirLeft,
      final int runCount) {
    final long start = System.currentTimeMillis();
    if (amphibious) {
      attacking.forEach(unit -> unit.setWasAmphibious(true));
    }
    final BattleOptions options =
        new BattleOptions(
            retreatWhenOnlyAirLeft,
            orderOfLossTypes(attackerOrderOfLosses, attacking),
            orderOfLossTypes(defenderOrderOfLosses, defending));
    final GameDataBattleAdapter adapter = new GameDataBattleAdapter();
    final BattleScenario scenario =
        adapter.toScenario(
            attacker,
            defender,
            location,
            attacking,
            defending,
            bombarding,
            territoryEffects,
            options);
    final SimulationResults results =
        new ReferenceBattleSimulator()
            .simulate(scenario, runCount, new EngineRandomSource(randomSource));
    final AggregateResults aggregateResults =
        new AggregateResultsBridge(adapter).toAggregateResults(results, gameData);
    aggregateResults.setTime(System.currentTimeMillis() - start);
    return aggregateResults;
  }

  /**
   * Collapses a parsed order-of-losses (a unit list, casualty-first) into the distinct type
   * sequence {@link BattleOptions} carries; a blank OOL yields an empty list and the reference
   * default order.
   */
  private List<UnitTypeId> orderOfLossTypes(final String ool, final Collection<Unit> units) {
    final List<Unit> ordered =
        OrderOfLossesInputPanel.getUnitListByOrderOfLoss(ool, units, gameData);
    if (ordered == null) {
      return List.of();
    }
    return ordered.stream()
        .map(unit -> new UnitTypeId(unit.getType().getName()))
        .distinct()
        .collect(Collectors.toList());
  }

  private <T> Collection<T> translateCollectionIntoOtherGameData(
      Collection<T> collection, GameData otherData) {
    // translateIntoOtherGameData() uses serialization, so if the collection is not serializable,
    // copy it into one that is. In particular, HashMap.keySet() and similar are not serializable.
    if (!(collection instanceof Serializable)) {
      collection = new ArrayList<>(collection);
    }
    return GameDataUtils.translateIntoOtherGameData(collection, otherData);
  }

  private Collection<Unit> mergeUnitCollections(Collection<Unit> c1, Collection<Unit> c2) {
    var combined = new HashSet<>(c1);
    combined.addAll(c2);
    Preconditions.checkState(
        combined.size() == c1.size() + c2.size(),
        "Attackers and defenders collections must be distinct with no duplicates. "
            + "This helps catch logic errors in AI code that would otherwise be hard to debug.");
    return combined;
  }

  public void cancel() {
    cancelled = true;
  }
}
