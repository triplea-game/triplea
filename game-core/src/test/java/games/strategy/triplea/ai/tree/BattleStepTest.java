package games.strategy.triplea.ai.tree;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.junit.jupiter.api.Assertions.assertEquals;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.math3.distribution.BinomialDistribution;
import org.junit.jupiter.api.Test;

class BattleStepTest {
  private static final GameData data = TestMapGameData.GLOBAL1940.getGameData();
  private static final GamePlayer BRITISH =
      checkNotNull(data.getPlayerList().getPlayerId("British"));
  private static final GamePlayer GERMAN =
      checkNotNull(data.getPlayerList().getPlayerId("Germans"));
  private static final Territory FRANCE = checkNotNull(territory("France", data));
  private static final Collection<TerritoryEffect> FRANCE_TERRITORY_EFFECTS =
      TerritoryEffectHelper.getEffects(FRANCE);
  private static final Territory SEA_ZONE = checkNotNull(territory("110 Sea Zone", data));
  private static final Collection<TerritoryEffect> SEA_ZONE_TERRITORY_EFFECTS =
      TerritoryEffectHelper.getEffects(SEA_ZONE);
  private static final UnitType INFANTRY =
      checkNotNull(data.getUnitTypeList().getUnitType("infantry"));
  private static final UnitType ARTILLERY =
      checkNotNull(data.getUnitTypeList().getUnitType("artillery"));
  private static final UnitType ARMOUR = checkNotNull(data.getUnitTypeList().getUnitType("armour"));
  private static final UnitType FIGHTER =
      checkNotNull(data.getUnitTypeList().getUnitType("fighter"));
  private static final UnitType TACTICAL_BOMBER =
      checkNotNull(data.getUnitTypeList().getUnitType("tactical_bomber"));
  private static final UnitType BOMBER = checkNotNull(data.getUnitTypeList().getUnitType("bomber"));
  private static final UnitType AAGUN = checkNotNull(data.getUnitTypeList().getUnitType("aaGun"));
  private static final UnitType CRUISER =
      checkNotNull(data.getUnitTypeList().getUnitType("cruiser"));
  private static final UnitType BATTLESHIP =
      checkNotNull(data.getUnitTypeList().getUnitType("battleship"));
  private static final UnitType SUBMARINE =
      checkNotNull(data.getUnitTypeList().getUnitType("submarine"));
  private static final UnitType DESTROYER =
      checkNotNull(data.getUnitTypeList().getUnitType("destroyer"));

  private static final GameData dataTww = TestMapGameData.TWW.getGameData();
  private static final GamePlayer BRITAIN = checkNotNull(GameDataTestUtil.britain(dataTww));
  private static final GamePlayer USA = checkNotNull(GameDataTestUtil.usa(dataTww));
  private static final GamePlayer GERMANY = checkNotNull(GameDataTestUtil.germany(dataTww));
  private static final Territory LAND_NO_ATTACHMENTS_TWW =
      checkNotNull(GameDataTestUtil.territory("Alberta", dataTww));
  private static final Collection<TerritoryEffect> LAND_NO_ATTACHMENTS_EFFECTS_TWW =
      TerritoryEffectHelper.getEffects(LAND_NO_ATTACHMENTS_TWW);
  private static final Territory SEA_NO_ATTACHMENTS_TWW =
      checkNotNull(GameDataTestUtil.territory("100 Sea Zone", dataTww));
  private static final Collection<TerritoryEffect> SEA_NO_ATTACHMENTS_EFFECTS_TWW =
      TerritoryEffectHelper.getEffects(SEA_NO_ATTACHMENTS_TWW);

  private BattleStep.Parameters createLandParameters() {
    return BattleStep.Parameters.builder()
        .data(data)
        .location(FRANCE)
        .territoryEffects(FRANCE_TERRITORY_EFFECTS)
        .build();
  }

  private BattleStep.Parameters createSeaParameters() {
    return BattleStep.Parameters.builder()
        .data(data)
        .location(SEA_ZONE)
        .territoryEffects(SEA_ZONE_TERRITORY_EFFECTS)
        .build();
  }

  private BattleStep.Parameters createTwwLandParameters() {
    return BattleStep.Parameters.builder()
        .data(dataTww)
        .location(LAND_NO_ATTACHMENTS_TWW)
        .territoryEffects(LAND_NO_ATTACHMENTS_EFFECTS_TWW)
        .build();
  }

  private BattleStep.Parameters createTwwSeaParameters() {
    return BattleStep.Parameters.builder()
        .data(dataTww)
        .location(SEA_NO_ATTACHMENTS_TWW)
        .territoryEffects(SEA_NO_ATTACHMENTS_EFFECTS_TWW)
        .build();
  }

  private Map<Integer, Integer> getDiceGrouped(
      final List<Unit> attackers, final List<Unit> defenders) {
    final BattleStep.Parameters parameters =
        BattleStep.Parameters.builder()
            .data(data)
            .location(FRANCE)
            .territoryEffects(FRANCE_TERRITORY_EFFECTS)
            .build();

    final StepUnits attackingUnits = new StepUnits(attackers, BRITISH, List.of(), GERMAN);
    final BattleStep root = new BattleStep(attackingUnits, BRITISH, 0, createLandParameters());
    return root.getRegularDiceGrouped(parameters, false, attackers, defenders);
  }

  private Map<Integer, Integer> getTwwDiceGrouped(
      final List<Unit> attackers, final List<Unit> defenders) {
    final BattleStep.Parameters parameters =
        BattleStep.Parameters.builder()
            .data(dataTww)
            .location(LAND_NO_ATTACHMENTS_TWW)
            .territoryEffects(LAND_NO_ATTACHMENTS_EFFECTS_TWW)
            .build();
    final StepUnits attackingUnits = new StepUnits(attackers, BRITAIN, List.of(), GERMANY);
    final BattleStep root = new BattleStep(attackingUnits, BRITAIN, 0, createTwwLandParameters());
    return root.getRegularDiceGrouped(parameters, false, attackers, defenders);
  }

  @Test
  void getDiceGroupedWithSupport() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.add(INFANTRY.create(BRITISH));
    attackingOrderOfLoss.add(ARTILLERY.create(BRITISH));

    final Map<Integer, Integer> diceGrouped = getDiceGrouped(attackingOrderOfLoss, List.of());
    assertEquals(Map.of(2, 2), diceGrouped);
  }

  @Test
  void getDiceGroupedWithSupportExtraNonSupport() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(2, BRITISH));
    attackingOrderOfLoss.add(ARTILLERY.create(BRITISH));

    final Map<Integer, Integer> diceGrouped = getDiceGrouped(attackingOrderOfLoss, List.of());
    assertEquals(Map.of(2, 2, 1, 1), diceGrouped);
  }

  @Test
  void calculateSingleHitProbability() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.add(FIGHTER.create(BRITISH));

    final int diceSides = data.getDiceSides();
    final Map<Integer, Integer> diceGrouped = getDiceGrouped(attackingOrderOfLoss, List.of());
    final StepUnits attackingUnits =
        new StepUnits(attackingOrderOfLoss, BRITISH, List.of(), GERMAN);
    final BattleStep root = new BattleStep(attackingUnits, BRITISH, 0, createLandParameters());
    final List<Double> hitProbability =
        root.calculateHitProbabilities(BattleStep.RollData.of(1, diceGrouped, diceSides));

    final BinomialDistribution distribution = new BinomialDistribution(null, 1, 3.0 / 6.0);
    assertEquals(distribution.probability(0), hitProbability.get(0));
    assertEquals(distribution.probability(1), hitProbability.get(1));
  }

  @Test
  void calculateDoubleHitProbability() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.add(INFANTRY.create(BRITISH));
    attackingOrderOfLoss.add(ARMOUR.create(BRITISH));

    final int diceSides = data.getDiceSides();
    final Map<Integer, Integer> diceGrouped = getDiceGrouped(attackingOrderOfLoss, List.of());
    final StepUnits attackingUnits =
        new StepUnits(attackingOrderOfLoss, BRITISH, List.of(), GERMAN);
    final BattleStep root = new BattleStep(attackingUnits, BRITISH, 0, createLandParameters());
    final List<Double> hitProbability =
        root.calculateHitProbabilities(BattleStep.RollData.of(2, diceGrouped, diceSides));

    final BinomialDistribution infantryDistribution = new BinomialDistribution(null, 1, 1.0 / 6.0);
    final BinomialDistribution armourDistribution = new BinomialDistribution(null, 1, 3.0 / 6.0);
    assertEquals(
        infantryDistribution.probability(0) * armourDistribution.probability(0),
        hitProbability.get(0),
        0.0001);
    assertEquals(
        infantryDistribution.probability(1) * armourDistribution.probability(0)
            + infantryDistribution.probability(0) * armourDistribution.probability(0),
        hitProbability.get(1),
        0.0001);
    assertEquals(
        infantryDistribution.probability(1) * armourDistribution.probability(1),
        hitProbability.get(2),
        0.0001);
  }

  @Test
  void calculate3HitProbabilityWithTwoOfTwoDifferentUnits() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(2, BRITISH));
    attackingOrderOfLoss.addAll(ARMOUR.create(2, BRITISH));

    final int diceSides = data.getDiceSides();
    final Map<Integer, Integer> diceGrouped = getDiceGrouped(attackingOrderOfLoss, List.of());

    final StepUnits attackingUnits =
        new StepUnits(attackingOrderOfLoss, BRITISH, List.of(), GERMAN);

    final BattleStep root = new BattleStep(attackingUnits, BRITISH, 0, createLandParameters());
    final List<Double> hitProbability =
        root.calculateHitProbabilities(BattleStep.RollData.of(4, diceGrouped, diceSides));

    final BinomialDistribution infantryDistribution = new BinomialDistribution(null, 2, 1.0 / 6.0);
    final BinomialDistribution armourDistribution = new BinomialDistribution(null, 2, 3.0 / 6.0);
    assertEquals(
        infantryDistribution.probability(1) * armourDistribution.probability(2)
            + infantryDistribution.probability(2) * armourDistribution.probability(1),
        hitProbability.get(3),
        0.0001);
  }

  @Test
  void calculateAllHitProbabilitiesWith2_3_2_units() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(2, BRITISH));
    attackingOrderOfLoss.addAll(ARMOUR.create(3, BRITISH));
    attackingOrderOfLoss.addAll(TACTICAL_BOMBER.create(2, BRITISH));

    final int diceSides = data.getDiceSides();
    final Map<Integer, Integer> diceGrouped = getDiceGrouped(attackingOrderOfLoss, List.of());

    final StepUnits attackingUnits =
        new StepUnits(attackingOrderOfLoss, BRITISH, List.of(), GERMAN);

    final BattleStep root = new BattleStep(attackingUnits, BRITISH, 0, createLandParameters());
    final List<Double> hitProbability =
        root.calculateHitProbabilities(BattleStep.RollData.of(7, diceGrouped, diceSides));

    final BinomialDistribution infantryDistribution = new BinomialDistribution(null, 2, 1.0 / 6.0);
    final BinomialDistribution armourDistribution = new BinomialDistribution(null, 3, 3.0 / 6.0);
    final BinomialDistribution bomberDistribution = new BinomialDistribution(null, 2, 4.0 / 6.0);
    assertEquals(
        infantryDistribution.probability(0)
            * armourDistribution.probability(0)
            * bomberDistribution.probability(0),
        hitProbability.get(0),
        0.0001);
    assertEquals(0.0713, hitProbability.get(1), 0.0001);
    assertEquals(0.2106, hitProbability.get(2), 0.0001);
    assertEquals(0.3171, hitProbability.get(3), 0.0001);
    assertEquals(0.2581, hitProbability.get(4), 0.0001);
    assertEquals(0.1099, hitProbability.get(5), 0.0001);
    assertEquals(0.0216, hitProbability.get(6), 0.0001);
    assertEquals(
        infantryDistribution.probability(2)
            * armourDistribution.probability(3)
            * bomberDistribution.probability(2),
        hitProbability.get(7),
        0.0001);
  }

  @Test
  void calculateComplexProbabilities() {
    final int quantity = 7;
    final UnitTypeList unitTypeList = dataTww.getUnitTypeList();
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(
        unitTypeList.getUnitType("britishInfantry").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        unitTypeList.getUnitType("britishAlpineInfantry").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        unitTypeList.getUnitType("britishMarine").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        unitTypeList.getUnitType("britishArtillery").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        unitTypeList.getUnitType("britishHeavyArtillery").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        unitTypeList.getUnitType("britishMobileArtillery").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        unitTypeList.getUnitType("britishMech.Infantry").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(unitTypeList.getUnitType("britishTank").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        unitTypeList.getUnitType("britishHeavyTank").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        unitTypeList.getUnitType("britishFighter").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        unitTypeList.getUnitType("britishAdvancedFighter").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        unitTypeList.getUnitType("britishTacticalBomber").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        unitTypeList.getUnitType("britishAdvancedTacticalBomber").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        unitTypeList.getUnitType("britishAntiAirGun").create(quantity, BRITAIN));
    attackingOrderOfLoss.addAll(
        unitTypeList.getUnitType("britishAntiTankGun").create(quantity, BRITAIN));

    final int diceSides = dataTww.getDiceSides();
    final Map<Integer, Integer> diceGrouped = getTwwDiceGrouped(attackingOrderOfLoss, List.of());

    final StepUnits attackingUnits =
        new StepUnits(attackingOrderOfLoss, BRITAIN, List.of(), GERMANY);

    final BattleStep root = new BattleStep(attackingUnits, BRITAIN, 0, createTwwLandParameters());
    final List<Double> hitProbability =
        root.calculateHitProbabilities(
            BattleStep.RollData.of(15 * quantity, diceGrouped, diceSides));

    assertEquals(0.0314, hitProbability.get(35), 0.0001);
  }

  static final class CalculateResult {
    private static final double THRESHOLD = .001;
    double winProbability = 0;
    double loseProbability = 0;
    double tieProbability = 0;
    double badProbability = 0;

    static CalculateResult of(
        final double winProbability,
        final double loseProbability,
        final double tieProbability,
        final double badProbability) {
      final CalculateResult result = new CalculateResult();
      result.winProbability = winProbability;
      result.loseProbability = loseProbability;
      result.tieProbability = tieProbability;
      result.badProbability = badProbability;
      return result;
    }

    static CalculateResult of(final BattleStep root) {
      final CalculateResult result = new CalculateResult();
      result.winProbability = root.getWinProbability();
      result.loseProbability = root.getLoseProbability();
      result.tieProbability = root.getTieProbability();
      result.badProbability = root.getBadProbability();
      return result;
    }

    @Override
    public String toString() {
      return "CalculateResult{"
          + "winProbability="
          + winProbability
          + ", loseProbability="
          + loseProbability
          + ", tieProbability="
          + tieProbability
          + ", badProbability="
          + badProbability
          + '}';
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      final CalculateResult that = (CalculateResult) o;
      return Math.abs(that.winProbability - winProbability) < THRESHOLD
          && Math.abs(that.loseProbability - loseProbability) < THRESHOLD
          && Math.abs(that.tieProbability - tieProbability) < THRESHOLD
          && Math.abs(that.badProbability - badProbability) < THRESHOLD;
    }

    @Override
    public int hashCode() {
      return Objects.hash(winProbability, loseProbability, tieProbability, badProbability);
    }
  }

  BattleStep runFight(
      final List<Unit> attackers,
      final List<Unit> defenders,
      final CalculateResult expected,
      final BattleStep.Parameters parameters) {
    final GamePlayer attacker = attackers.get(0).getOwner();
    final GamePlayer defender = defenders.get(0).getOwner();

    final StepUnits attackingUnits = new StepUnits(attackers, attacker, defenders, defender);

    final BattleStep root = new BattleStep(attackingUnits, attacker, 0, parameters);
    root.calculateBattle(attackingUnits, defender);
    assertEquals(expected, CalculateResult.of(root));
    return root;
  }

  @Test
  void attackerWith1ArmourVs1Inf() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.add(ARMOUR.create(BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.add(INFANTRY.create(GERMAN));

    final CalculateResult expected = CalculateResult.of(0.5, 0.25, 0.25, 0.0);
    runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createLandParameters());
  }

  @Test
  void attackerWithMultiUnitsVsSingleUnit() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(2, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.add(INFANTRY.create(GERMAN));

    final CalculateResult expected = CalculateResult.of(0.676, 0.269, 0.053, 0.0);
    runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createLandParameters());
  }

  @Test
  void attackerWithInf1v1() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(INFANTRY.create(1, GERMAN));

    final CalculateResult expected = CalculateResult.of(0.25, 0.625, 0.125, 0.0);
    runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createLandParameters());
  }

  @Test
  void attackerWithInf2v2() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(2, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(INFANTRY.create(2, GERMAN));

    final CalculateResult expected = CalculateResult.of(0.214, 0.742, 0.042, 0.0);
    runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createLandParameters());
  }

  // The low probability tests are using IGNORE_BRANCH_PROBABILITY = 0.005
  // if that changes, these results will need to be updated
  @Test
  void fightWithLowProbabilityBranch1() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(5, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(INFANTRY.create(5, GERMAN));
    final CalculateResult expected = CalculateResult.of(0.117, 0.863, 0.008, 0.009);
    runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createLandParameters());
  }

  @Test
  void fightWithLowProbabilityBranch2() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(10, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(INFANTRY.create(10, GERMAN));

    final CalculateResult expected = CalculateResult.of(0.049, 0.936, 0.001, 0.008);
    runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createLandParameters());
  }

  @Test
  void fightWithLowProbabilityBranch3() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(15, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(INFANTRY.create(15, GERMAN));

    final CalculateResult expected = CalculateResult.of(0.021, 0.961, 0.000, 0.011);
    runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createLandParameters());
  }

  @Test
  void fightWithLowProbabilityBranch4() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(20, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(INFANTRY.create(20, GERMAN));

    final CalculateResult expected = CalculateResult.of(0.009, 0.966, 0.000, 0.021);
    runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createLandParameters());
  }

  @Test
  void averageUnitsFromInfVsInf() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(INFANTRY.create(1, GERMAN));

    final CalculateResult expected = CalculateResult.of(0.25, 0.625, 0.125, 0.0);
    final BattleStep root =
        runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createLandParameters());

    final Map<Unit, Double> friendlyChances = root.getAverageUnits().getFriendlyUnitsChances();
    final Map<Unit, Double> enemyChances = root.getAverageUnits().getEnemyUnitsChances();

    assertEquals(.625, enemyChances.get(defendingOrderOfLoss.get(0)), 0.001);
    assertEquals(.25, friendlyChances.get(attackingOrderOfLoss.get(0)), 0.001);
  }

  @Test
  void averageUnitsFromEvenFight() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(1, BRITISH));
    attackingOrderOfLoss.addAll(ARTILLERY.create(1, BRITISH));
    attackingOrderOfLoss.addAll(FIGHTER.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(INFANTRY.create(2, GERMAN));
    defendingOrderOfLoss.addAll(ARMOUR.create(1, GERMAN));

    final CalculateResult expected = CalculateResult.of(0.453, 0.453, 0.093, 0.0);
    final BattleStep root =
        runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createLandParameters());

    assertEquals(List.of(), root.getAverageUnits().getFriendlyWithChance(0.5));
    assertEquals(List.of(), root.getAverageUnits().getEnemyWithChance(0.5));
  }

  @Test
  void averageUnitsFromAttackerWin() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(2, BRITISH));
    attackingOrderOfLoss.addAll(ARTILLERY.create(3, BRITISH));
    attackingOrderOfLoss.addAll(FIGHTER.create(2, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(INFANTRY.create(3, GERMAN));
    defendingOrderOfLoss.addAll(ARMOUR.create(1, GERMAN));

    final CalculateResult expected = CalculateResult.of(0.985, 0.011, 0.003, 0.0);
    final BattleStep root =
        runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createLandParameters());

    assertEquals(
        attackingOrderOfLoss.subList(2, attackingOrderOfLoss.size()),
        root.getAverageUnits().getFriendlyWithChance(0.5));
    assertEquals(List.of(), root.getAverageUnits().getEnemyWithChance(0.5));
  }

  @Test
  void averageUnitsFromDefenderWin() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(2, BRITISH));
    attackingOrderOfLoss.addAll(ARTILLERY.create(2, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(INFANTRY.create(3, GERMAN));
    defendingOrderOfLoss.addAll(ARMOUR.create(3, GERMAN));

    final CalculateResult expected = CalculateResult.of(0.018, 0.976, 0.005, 0.0);
    final BattleStep root =
        runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createLandParameters());

    assertEquals(List.of(), root.getAverageUnits().getFriendlyWithChance(0.5));
    assertEquals(
        defendingOrderOfLoss.subList(2, defendingOrderOfLoss.size()),
        root.getAverageUnits().getEnemyWithChance(0.5));
  }

  @Test
  void attackFighterVsAa() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(FIGHTER.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(AAGUN.create(1, GERMAN));

    final CalculateResult expected = CalculateResult.of(0.833, 0.166, 0.0, 0.0);
    runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createLandParameters());
  }

  @Test
  void attackFighterInfantryVsAaInfantry() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(1, BRITISH));
    attackingOrderOfLoss.addAll(FIGHTER.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(AAGUN.create(1, GERMAN));
    defendingOrderOfLoss.addAll(INFANTRY.create(1, GERMAN));

    final CalculateResult expected = CalculateResult.of(0.549, 0.366, 0.083, 0.0);
    runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createLandParameters());
  }

  @Test
  void attackBattleshipVsCruiser() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(BATTLESHIP.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(CRUISER.create(1, GERMAN));

    final CalculateResult expected = CalculateResult.of(0.88, 0.04, 0.08, 0.0);
    runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createSeaParameters());
  }

  @Test
  void attackFighterVsSubmarine() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(FIGHTER.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(SUBMARINE.create(1, GERMAN));

    final CalculateResult expected = CalculateResult.of(1.0, 0.0, 0.0, 0.0);
    runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createSeaParameters());
  }

  @Test
  void attackSubmarineVsFighterDestroy() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(SUBMARINE.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(FIGHTER.create(1, GERMAN));
    defendingOrderOfLoss.addAll(DESTROYER.create(1, GERMAN));

    final CalculateResult expected = CalculateResult.of(0.0, 0.999, 0.0, 0.0);
    runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createSeaParameters());
  }

  @Test
  void attackSubmarineBomberVsFighterDestroy() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(SUBMARINE.create(1, BRITISH));
    attackingOrderOfLoss.addAll(BOMBER.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(DESTROYER.create(1, GERMAN));
    defendingOrderOfLoss.addAll(FIGHTER.create(1, GERMAN));

    final CalculateResult expected = CalculateResult.of(0.300, 0.467, 0.231, 0.0);
    runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createSeaParameters());
  }

  @Test
  void attackSubmarineVsFighterCruiser() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(SUBMARINE.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(CRUISER.create(1, GERMAN));
    defendingOrderOfLoss.addAll(FIGHTER.create(1, GERMAN));

    final CalculateResult expected = CalculateResult.of(0.0, 1.0, 0.0, 0.0);
    runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createSeaParameters());
  }

  @Test
  void attackSubmarineVsBattleship() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(SUBMARINE.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(BATTLESHIP.create(1, GERMAN));

    final CalculateResult expected = CalculateResult.of(0.061, 0.938, 0.0, 0.0);
    runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createSeaParameters());
  }

  @Test
  void attackAaWithMultipleTargetGroups() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(GameDataTestUtil.britishInfantry(dataTww).create(1, BRITAIN));
    attackingOrderOfLoss.addAll(GameDataTestUtil.britishFighter(dataTww).create(1, BRITAIN));
    attackingOrderOfLoss.addAll(GameDataTestUtil.britishTank(dataTww).create(1, BRITAIN));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(GameDataTestUtil.germanAntiAirGun(dataTww).create(1, GERMANY));
    defendingOrderOfLoss.addAll(GameDataTestUtil.germanAntiTankGun(dataTww).create(1, GERMANY));
    defendingOrderOfLoss.addAll(GameDataTestUtil.germanMobileArtillery(dataTww).create(1, GERMANY));

    final CalculateResult expected = CalculateResult.of(0.432, 0.525, 0.042, 0.000);
    runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createTwwLandParameters());
  }

  @Test
  void attackSuicideAa() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(GameDataTestUtil.americanCruiser(dataTww).create(1, USA));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(GameDataTestUtil.germanMine(dataTww).create(1, GERMANY));

    final CalculateResult expected = CalculateResult.of(0.675, 0.0, 0.324, 0.0);
    runFight(attackingOrderOfLoss, defendingOrderOfLoss, expected, createTwwSeaParameters());
  }
}
