package org.triplea.ai.flowfield.odds;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.BattleState.Side.DEFENSE;
import static games.strategy.triplea.delegate.battle.BattleState.Side.OFFENSE;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import games.strategy.triplea.delegate.power.calculator.PowerStrengthAndRolls;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.odds.calculator.AggregateResults;
import games.strategy.triplea.odds.calculator.ConcurrentBattleCalculator;
import games.strategy.triplea.ui.menubar.debug.AiPlayerDebugAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.triplea.ai.flowfield.FlowFieldAi;

@RequiredArgsConstructor
public class LanchesterDebugAction implements Consumer<AiPlayerDebugAction> {

  private final FlowFieldAi ai;
  private final RelationshipTracker relationshipTracker;

  @Override
  public void accept(final AiPlayerDebugAction aiPlayerDebugAction) {
    final GameData gameData = ai.getGameData();
    final Territory territory =
        gameData.getMap().getTerritories().stream()
            .filter(Predicate.not(Territory::isWater))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Land territory is required."));
    System.out.println("Using territory " + territory.getName());
    final GamePlayer offender = ai.getGamePlayer();
    final GamePlayer defender =
        relationshipTracker.getEnemies(ai.getGamePlayer()).stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("An enemy is required"));
    System.out.println("Defender is " + defender.getName());

    final List<Unit> attackingUnits = new ArrayList<>();
    final List<Unit> defendingUnits = new ArrayList<>();

    final Collection<UnitType> offenseUnitTypes =
        getUnitTypes(
            offender,
            unitAttachment ->
                unitAttachment.getAttack(offender) > 0
                    && Matches.unitTypeIsLand().test((UnitType) unitAttachment.getAttachedTo()));
    final Collection<UnitType> defenseUnitTypes =
        getUnitTypes(
            defender,
            unitAttachment ->
                unitAttachment.getDefense(defender) > 0
                    && Matches.unitTypeIsLand().test((UnitType) unitAttachment.getAttachedTo()));
    for (int i = 0; i < 4; i++) {
      getRandomUnitType(offenseUnitTypes)
          .ifPresent(
              unitType ->
                  attackingUnits.addAll(unitType.create(new Random().nextInt(10), offender)));
      getRandomUnitType(defenseUnitTypes)
          .ifPresent(
              unitType ->
                  defendingUnits.addAll(unitType.create(new Random().nextInt(10), defender)));
    }

    System.out.println("Attack Units: " + MyFormatter.unitsToText(attackingUnits));
    System.out.println("Defending Units: " + MyFormatter.unitsToText(defendingUnits));

    final ConcurrentBattleCalculator hardAiCalculator = new ConcurrentBattleCalculator();
    hardAiCalculator.setGameData(ai.getGameData());
    final AggregateResults hardAiResults =
        hardAiCalculator.calculate(
            offender,
            defender,
            territory,
            attackingUnits,
            defendingUnits,
            List.of(),
            List.of(),
            false,
            200);

    final LanchesterBattleCalculator lanchesterCalculator =
        new LanchesterBattleCalculator(
            PowerStrengthAndRolls.build(
                attackingUnits,
                CombatValueBuilder.mainCombatValue()
                    .friendlyUnits(attackingUnits)
                    .enemyUnits(defendingUnits)
                    .side(OFFENSE)
                    .territoryEffects(List.of())
                    .gameDiceSides(gameData.getDiceSides())
                    .gameSequence(gameData.getSequence())
                    .lhtrHeavyBombers(false)
                    .supportAttachments(gameData.getUnitTypeList().getSupportRules())
                    .build()),
            PowerStrengthAndRolls.build(
                defendingUnits,
                CombatValueBuilder.mainCombatValue()
                    .friendlyUnits(defendingUnits)
                    .enemyUnits(attackingUnits)
                    .side(DEFENSE)
                    .territoryEffects(List.of())
                    .gameDiceSides(gameData.getDiceSides())
                    .gameSequence(gameData.getSequence())
                    .lhtrHeavyBombers(false)
                    .supportAttachments(gameData.getUnitTypeList().getSupportRules())
                    .build()),
            1.45);

    System.out.println("Hard AI Results");
    System.out.println("Win percentage: " + hardAiResults.getAttackerWinPercent());
    System.out.println(
        "Avg attacking units remaining: "
            + hardAiResults.getAverageAttackingUnitsRemaining().size()
            + " - "
            + hardAiResults.getAverageAttackingUnitsRemaining());
    System.out.println(
        "Avg defending units remaining: "
            + hardAiResults.getAverageDefendingUnitsRemaining().size()
            + " - "
            + hardAiResults.getAverageDefendingUnitsRemaining());

    System.out.println("Lanchester Results");
    System.out.println("Winner: " + lanchesterCalculator.getWon());
    System.out.println("Units Remaining: " + lanchesterCalculator.getRemainingUnits());
  }

  private Collection<UnitType> getUnitTypes(
      final GamePlayer player, final Predicate<UnitAttachment> filter) {
    return player.getProductionFrontier().getRules().stream()
        .map(rule -> rule.getResults().entrySet())
        .flatMap(Collection::stream)
        .map(Map.Entry::getKey)
        .filter(UnitType.class::isInstance)
        .map(UnitType.class::cast)
        .filter(
            unitType -> filter.test((UnitAttachment) unitType.getAttachment(UNIT_ATTACHMENT_NAME)))
        .collect(Collectors.toSet());
  }

  private Optional<UnitType> getRandomUnitType(final Collection<UnitType> unitTypes) {
    return unitTypes.stream().skip((int) (unitTypes.size() * Math.random())).findFirst();
  }
}
