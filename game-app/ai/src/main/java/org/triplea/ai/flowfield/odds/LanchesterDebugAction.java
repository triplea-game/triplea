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
import games.strategy.triplea.ui.menubar.debug.AiPlayerDebugOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.triplea.ai.flowfield.FlowFieldAi;
import org.triplea.ai.flowfield.influence.TerritoryDebugAction;

@Slf4j
@RequiredArgsConstructor
public class LanchesterDebugAction implements Consumer<AiPlayerDebugAction> {

  private final FlowFieldAi ai;
  private final RelationshipTracker relationshipTracker;

  public static List<AiPlayerDebugOption> buildDebugOptions(FlowFieldAi ai) {
    return List.of(
        AiPlayerDebugOption.builder().title("HeatMap").subOptions(buildHeatmapOptions(ai)).build(),
        AiPlayerDebugOption.builder()
            .title("Calculate Attrition Factor")
            .actionListener(
                new LanchesterDebugAction(ai, ai.getGameData().getRelationshipTracker()))
            .build());
  }

  private static List<AiPlayerDebugOption> buildHeatmapOptions(FlowFieldAi ai) {
    final List<AiPlayerDebugOption> options = new ArrayList<>();

    options.add(
        AiPlayerDebugOption.builder()
            .title("None")
            .optionType(AiPlayerDebugOption.OptionType.ON_OFF_EXCLUSIVE)
            .exclusiveGroup("heatmap")
            .build());

    options.addAll(
        ai.getDiffusions().stream()
            .map(
                diffusion ->
                    AiPlayerDebugOption.builder()
                        .title(diffusion.getName())
                        .optionType(AiPlayerDebugOption.OptionType.ON_OFF_EXCLUSIVE)
                        .exclusiveGroup("heatmap")
                        .actionListener(
                            new TerritoryDebugAction(diffusion, ai.getGameData().getMap()))
                        .build())
            .collect(Collectors.toList()));

    return options;
  }

  @Override
  public void accept(final AiPlayerDebugAction aiPlayerDebugAction) {
    final GameData gameData = ai.getGameData();
    final Territory territory =
        gameData.getMap().getTerritories().stream()
            .filter(Predicate.not(Territory::isWater))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Land territory is required."));
    log.info("Using territory {}", territory.getName());
    final GamePlayer offender = ai.getGamePlayer();
    final GamePlayer defender =
        relationshipTracker.getEnemies(ai.getGamePlayer()).stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("An enemy is required"));
    log.info("Defender is {}", defender.getName());

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
    final ThreadLocalRandom localRandom = ThreadLocalRandom.current();
    for (int i = 0; i < 4; i++) {
      getRandomUnitType(localRandom, offenseUnitTypes)
          .ifPresent(
              unitType ->
                  attackingUnits.addAll(unitType.create(localRandom.nextInt(10), offender)));
      getRandomUnitType(localRandom, defenseUnitTypes)
          .ifPresent(
              unitType ->
                  defendingUnits.addAll(unitType.create(localRandom.nextInt(10), defender)));
    }

    log.info("Attack Units: {}", MyFormatter.unitsToText(attackingUnits));
    log.info("Defending Units: {}", MyFormatter.unitsToText(defendingUnits));

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

    log.info("Hard AI Results");
    log.info("Win percentage: {}", hardAiResults.getAttackerWinPercent());
    log.info(
        "Avg attacking units remaining: {} - {}",
        hardAiResults.getAverageAttackingUnitsRemaining().size(),
        hardAiResults.getAverageAttackingUnitsRemaining());
    log.info(
        "Avg defending units remaining: {} - {}",
        hardAiResults.getAverageDefendingUnitsRemaining().size(),
        hardAiResults.getAverageDefendingUnitsRemaining());

    log.info("Lanchester Results");
    log.info("Winner: {}", lanchesterCalculator.getWon());
    log.info("Units Remaining: {}", lanchesterCalculator.getRemainingUnits());
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

  private Optional<UnitType> getRandomUnitType(
      final ThreadLocalRandom localRandom, final Collection<UnitType> unitTypes) {
    return unitTypes.stream().skip((localRandom.nextInt(unitTypes.size()))).findFirst();
  }
}
