package games.strategy.triplea.ai.tree;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class BattleStepTWWTest {
  private static final GameData data = TestMapGameData.TWW.getGameData();
  private static final GamePlayer BRITAIN = checkNotNull(GameDataTestUtil.britain(data));
  private static final GamePlayer AMERICAN = checkNotNull(GameDataTestUtil.usa(data));
  private static final GamePlayer GERMANS = checkNotNull(GameDataTestUtil.germany(data));
  private static final Territory LAND_NO_ATTACHMENTS =
      checkNotNull(GameDataTestUtil.territory("Alberta", data));
  private static final Collection<TerritoryEffect> LAND_NO_ATTACHMENTS_EFFECTS =
      TerritoryEffectHelper.getEffects(LAND_NO_ATTACHMENTS);
  private static final Territory SEA_NO_ATTACHMENTS =
      checkNotNull(GameDataTestUtil.territory("100 Sea Zone", data));
  private static final Collection<TerritoryEffect> SEA_NO_ATTACHMENTS_EFFECTS =
      TerritoryEffectHelper.getEffects(SEA_NO_ATTACHMENTS);

  private BattleStep.Parameters createParameters() {
    return BattleStep.Parameters.builder()
        .data(data)
        .location(LAND_NO_ATTACHMENTS)
        .territoryEffects(LAND_NO_ATTACHMENTS_EFFECTS)
        .build();
  }

  private BattleStep.Parameters createSeaParameters() {
    return BattleStep.Parameters.builder()
        .data(data)
        .location(SEA_NO_ATTACHMENTS)
        .territoryEffects(SEA_NO_ATTACHMENTS_EFFECTS)
        .build();
  }

  @Test
  void attackAaWithMultipleTargetGroups() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(GameDataTestUtil.britishInfantry(data).create(1, BRITAIN));
    attackingOrderOfLoss.addAll(GameDataTestUtil.britishFighter(data).create(1, BRITAIN));
    attackingOrderOfLoss.addAll(GameDataTestUtil.britishTank(data).create(1, BRITAIN));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(GameDataTestUtil.germanAntiAirGun(data).create(1, GERMANS));
    defendingOrderOfLoss.addAll(GameDataTestUtil.germanAntiTankGun(data).create(1, GERMANS));
    defendingOrderOfLoss.addAll(GameDataTestUtil.germanMobileArtillery(data).create(1, GERMANS));

    final StepUnits attackingUnits =
        new StepUnits(attackingOrderOfLoss, BRITAIN, defendingOrderOfLoss, GERMANS);

    final BattleStep root = new BattleStep(attackingUnits, BRITAIN, 0, createParameters());
    root.calculateBattle(attackingUnits, GERMANS);
    final BattleStepGlobal1940Test.CalculateResult expected =
        new BattleStepGlobal1940Test.CalculateResult();
    expected.winProbability = 0.431;
    expected.loseProbability = 0.524;
    expected.tieProbability = 0.042;
    expected.badProbability = 0.002;
    final BattleStepGlobal1940Test.CalculateResult actual =
        new BattleStepGlobal1940Test.CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);
  }

  @Test
  void attackSuicideAa() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(GameDataTestUtil.americanCruiser(data).create(1, AMERICAN));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(GameDataTestUtil.germanMine(data).create(1, GERMANS));

    final StepUnits attackingUnits =
        new StepUnits(attackingOrderOfLoss, AMERICAN, defendingOrderOfLoss, GERMANS);

    final BattleStep root = new BattleStep(attackingUnits, AMERICAN, 0, createSeaParameters());
    root.calculateBattle(attackingUnits, GERMANS);
    final BattleStepGlobal1940Test.CalculateResult expected =
        new BattleStepGlobal1940Test.CalculateResult();
    expected.winProbability = 0.671;
    expected.loseProbability = 0.0;
    expected.tieProbability = 0.322;
    expected.badProbability = 0.006;
    final BattleStepGlobal1940Test.CalculateResult actual =
        new BattleStepGlobal1940Test.CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);
  }

  private Map<Integer, Integer> getDiceGrouped(
      final List<Unit> attackers, final List<Unit> defenders) {
    final BattleStep.Parameters parameters =
        BattleStep.Parameters.builder()
            .data(data)
            .location(LAND_NO_ATTACHMENTS)
            .territoryEffects(LAND_NO_ATTACHMENTS_EFFECTS)
            .build();
    final StepUnits attackingUnits = new StepUnits(attackers, BRITAIN, List.of(), GERMANS);
    final BattleStep root = new BattleStep(attackingUnits, BRITAIN, 0, createParameters());
    return root.getRegularDiceGrouped(parameters, false, attackers, defenders);
  }

  @Test
  void calculateComplexProbabilities() throws InterruptedException {
    final int quantity = 7;
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(
        data.getUnitTypeList().getUnitType("britishInfantry").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        data.getUnitTypeList().getUnitType("britishAlpineInfantry").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        data.getUnitTypeList().getUnitType("britishMarine").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        data.getUnitTypeList().getUnitType("britishArtillery").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        data.getUnitTypeList().getUnitType("britishHeavyArtillery").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        data.getUnitTypeList().getUnitType("britishMobileArtillery").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        data.getUnitTypeList().getUnitType("britishMech.Infantry").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        data.getUnitTypeList().getUnitType("britishTank").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        data.getUnitTypeList().getUnitType("britishHeavyTank").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        data.getUnitTypeList().getUnitType("britishFighter").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        data.getUnitTypeList().getUnitType("britishAdvancedFighter").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        data.getUnitTypeList().getUnitType("britishTacticalBomber").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        data.getUnitTypeList()
            .getUnitType("britishAdvancedTacticalBomber")
            .create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        data.getUnitTypeList().getUnitType("britishAntiAirGun").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        data.getUnitTypeList().getUnitType("britishAntiTankGun").create(quantity, BRITAIN));

    final int diceSides = data.getDiceSides();
    final Map<Integer, Integer> diceGrouped = getDiceGrouped(attackingOrderOfLoss, List.of());

    final StepUnits attackingUnits =
        new StepUnits(attackingOrderOfLoss, BRITAIN, List.of(), GERMANS);

    final BattleStep root = new BattleStep(attackingUnits, BRITAIN, 0, createParameters());
    final List<Double> hitProbability =
        root.calculateHitProbabilities(
            BattleStep.RollData.of(15 * quantity, diceGrouped, diceSides));

    assertEquals(0.0314, hitProbability.get(35), 0.0001);
  }
}
