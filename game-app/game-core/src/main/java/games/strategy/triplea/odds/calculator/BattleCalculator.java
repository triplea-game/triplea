package games.strategy.triplea.odds.calculator;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.triplea.delegate.GameDelegateBridge;
import games.strategy.triplea.delegate.battle.BattleResults;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Setter;
import org.triplea.util.Version;

class BattleCalculator implements IBattleCalculator {
  @Nonnull private final GameData gameData;
  @Setter private boolean keepOneAttackingLandUnit = false;
  @Setter private boolean amphibious = false;
  @Setter private int retreatAfterRound = -1;
  @Setter private int retreatAfterXUnitsLeft = -1;
  @Setter private String attackerOrderOfLosses = null;
  @Setter private String defenderOrderOfLosses = null;
  private volatile boolean cancelled = false;
  private final AtomicBoolean isRunning = new AtomicBoolean(false);

  private BattleCalculator(@Nullable GameData data) {
    gameData = Preconditions.checkNotNull(data, "Error cloning game data (low memory?)");
  }

  BattleCalculator(GameData data, Version engineVersion) {
    this(
        GameDataUtils.cloneGameData(
                data, GameDataManager.Options.forBattleCalculator(), engineVersion)
            .orElse(null));
  }

  BattleCalculator(byte[] data, Version engineVersion) {
    this(GameDataUtils.createGameDataFromBytes(data, engineVersion).orElse(null));
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
      final Territory location2 = gameData.getMap().getTerritory(location.getName());
      final Collection<Unit> attackingUnits =
          GameDataUtils.translateIntoOtherGameData(attacking, gameData);
      final Collection<Unit> defendingUnits =
          GameDataUtils.translateIntoOtherGameData(defending, gameData);
      final Collection<Unit> bombardingUnits =
          GameDataUtils.translateIntoOtherGameData(bombarding, gameData);
      final Collection<TerritoryEffect> territoryEffects2 =
          GameDataUtils.translateIntoOtherGameData(territoryEffects, gameData);
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
        final DummyDelegateBridge bridge1 =
            new DummyDelegateBridge(
                attacker2,
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
            new MustFightBattle(location2, attacker2, gameData, battleTracker);
        battle.setHeadless(true);
        if (amphibious) {
          attackingUnits.forEach(
              unit ->
                  unit.getProperty(Unit.UNLOADED_AMPHIBIOUS)
                      .ifPresent(
                          property -> {
                            try {
                              property.setValue(true);
                            } catch (final MutableProperty.InvalidValueException e) {
                              // ignore
                            }
                          }));
        }
        battle.setUnits(
            defendingUnits, attackingUnits, bombardingUnits, defender2, territoryEffects2);
        bridge1.setBattle(battle);
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
