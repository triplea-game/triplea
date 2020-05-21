package games.strategy.triplea.ai.tree;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.UnitBattleComparator;
import games.strategy.triplea.delegate.battle.casualty.CasualtyOrderOfLosses;
import games.strategy.triplea.odds.calculator.AggregateResults;
import games.strategy.triplea.odds.calculator.IBattleCalculator;
import games.strategy.triplea.util.TuvUtils;
import java.util.Collection;
import java.util.List;

public class BattleTreeCalculator implements IBattleCalculator {

  private GameData data;

  public BattleTreeCalculator() {}

  public void setGameData(final GameData data) {
    this.data = data;
  }

  @Override
  public AggregateResults calculate(
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final Collection<Unit> bombardingUnits,
      final Collection<TerritoryEffect> territoryEffects,
      final boolean retreatWhenOnlyAirLeft, // TODO: handle this flag
      final int runCount) {
    final long startTime = System.currentTimeMillis();
    final int maxRounds =
        location.isWater()
            ? Properties.getSeaBattleRounds(data)
            : Properties.getLandBattleRounds(data);
    if (maxRounds > 0) {
      BattleStep.MAX_ROUNDS = maxRounds;
    }

    // remove all of the non combatants
    final MustFightBattle battle = new MustFightBattle(location, attacker, data, null);
    final List<Unit> attackingUnitsCleaned = battle.removeNonCombatants(attackingUnits, true, true);
    final List<Unit> defendingUnitsCleaned =
        battle.removeNonCombatants(defendingUnits, false, true);

    final boolean amphibious = false;
    final List<Unit> amphibiousLandAttackers = null;

    final List<Unit> attackingOrderOfLoss =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(
            CasualtyOrderOfLosses.Parameters.builder()
                .targetsToPickFrom(attackingUnitsCleaned)
                .player(attacker)
                .enemyUnits(defendingUnitsCleaned)
                .combatModifiers(
                    UnitBattleComparator.CombatModifiers.builder()
                        .territoryEffects(territoryEffects)
                        .amphibious(amphibious)
                        .defending(false)
                        .build())
                .amphibiousLandAttackers(
                    amphibiousLandAttackers == null ? List.of() : amphibiousLandAttackers)
                .battlesite(location)
                .costs(TuvUtils.getCostsForTuv(attacker, data))
                .data(data)
                .build());

    final List<Unit> defendingOrderOfLoss =
        CasualtyOrderOfLosses.sortUnitsForCasualtiesWithSupport(
            CasualtyOrderOfLosses.Parameters.builder()
                .targetsToPickFrom(defendingUnitsCleaned)
                .player(defender)
                .enemyUnits(attackingUnitsCleaned)
                .combatModifiers(
                    UnitBattleComparator.CombatModifiers.builder()
                        .territoryEffects(territoryEffects)
                        .amphibious(amphibious)
                        .defending(true)
                        .build())
                .amphibiousLandAttackers(
                    amphibiousLandAttackers == null ? List.of() : amphibiousLandAttackers)
                .battlesite(location)
                .costs(TuvUtils.getCostsForTuv(defender, data))
                .data(data)
                .build());

    if (Properties.getTransportCasualtiesRestricted(data)) {
      // move all transports to the end so that they are picked last
      attackingOrderOfLoss.sort(
          (unit1, unit2) ->
              Boolean.compare(
                  Matches.unitIsTransport().and(Matches.unitIsSea()).test(unit1),
                  Matches.unitIsTransport().and(Matches.unitIsSea()).test(unit2)));
      defendingOrderOfLoss.sort(
          (unit1, unit2) ->
              Boolean.compare(
                  Matches.unitIsTransport().and(Matches.unitIsSea()).test(unit1),
                  Matches.unitIsTransport().and(Matches.unitIsSea()).test(unit2)));
    }

    final StepUnits attackingUnitsObject =
        new StepUnits(attackingOrderOfLoss, attacker, defendingOrderOfLoss, defender);

    final BattleStep root =
        new BattleStep(
            attackingUnitsObject,
            attacker,
            0,
            BattleStep.Parameters.builder()
                .data(data)
                .location(location)
                .territoryEffects(territoryEffects)
                .build());
    root.calculateBattle(attackingUnitsObject, defender);

    final BattleTreeResults results = new BattleTreeResults(root);

    results.setTime(System.currentTimeMillis() - startTime);

    return results;
  }
}
