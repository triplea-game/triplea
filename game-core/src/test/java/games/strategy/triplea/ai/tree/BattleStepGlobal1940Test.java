package games.strategy.triplea.ai.tree;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.junit.jupiter.api.Assertions.*;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class BattleStepGlobal1940Test {
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
  private static final UnitType ARMOUR =
      checkNotNull(data.getUnitTypeList().getUnitType("armour"));
  private static final UnitType FIGHTER =
      checkNotNull(data.getUnitTypeList().getUnitType("fighter"));
  private static final UnitType TACTICAL_BOMBER =
      checkNotNull(data.getUnitTypeList().getUnitType("tactical_bomber"));
  private static final UnitType BOMBER =
      checkNotNull(data.getUnitTypeList().getUnitType("bomber"));
  private static final UnitType AAGUN =
      checkNotNull(data.getUnitTypeList().getUnitType("aaGun"));
  private static final UnitType CRUISER =
      checkNotNull(data.getUnitTypeList().getUnitType("cruiser"));
  private static final UnitType BATTLESHIP =
      checkNotNull(data.getUnitTypeList().getUnitType("battleship"));
  private static final UnitType SUBMARINE =
      checkNotNull(data.getUnitTypeList().getUnitType("submarine"));
  private static final UnitType DESTROYER =
      checkNotNull(data.getUnitTypeList().getUnitType("destroyer"));
  private static final UnitType TRANSPORT =
      checkNotNull(data.getUnitTypeList().getUnitType("transport"));

  private Map<Integer, Integer> getDiceGrouped(final List<Unit> attackers, final List<Unit> defenders) {
    final BattleStep.Parameters parameters = BattleStep.Parameters.builder()
        .data(data)
        .location(FRANCE)
        .territoryEffects(FRANCE_TERRITORY_EFFECTS)
        .build();

    final StepUnits attackingUnits = new StepUnits(attackers, BRITISH, List.of(), GERMAN);
    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    return root.getRegularDiceGrouped(
        parameters,
        false,
        attackers,
        defenders
    );
  }

  @Test
  void getDiceGroupedWithSupport() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.add(new Unit(INFANTRY, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(ARTILLERY, BRITISH, data));

    final Map<Integer, Integer> diceGrouped = getDiceGrouped(attackingOrderOfLoss, List.of());

    final Map<Integer, Integer> expected = new HashMap<>();
    expected.put(2, 2);

    assertEquals(expected, diceGrouped);
  }

  @Test
  void getDiceGroupedWithSupportExtraNonSupport() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.add(new Unit(INFANTRY, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(INFANTRY, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(ARTILLERY, BRITISH, data));

    final Map<Integer, Integer> diceGrouped = getDiceGrouped(attackingOrderOfLoss, List.of());

    final Map<Integer, Integer> expected = new HashMap<>();
    expected.put(2, 2);
    expected.put(1, 1);

    assertEquals(expected, diceGrouped);
  }

  @Test
  void calculateSingleHitProbability() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.add(new Unit(FIGHTER, BRITISH, data));

    final int diceSides = data.getDiceSides();
    final Map<Integer, Integer> diceGrouped = getDiceGrouped(attackingOrderOfLoss, List.of());
    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, List.of(), GERMAN);
    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    final List<Double> hitProbability = root.calculateHitProbabilities(
        BattleStep.RollData.of(1, diceGrouped, diceSides)
    );

    assertEquals(0.5, hitProbability.get(0));
    assertEquals(0.5, hitProbability.get(1));
  }

  @Test
  void calculateDoubleHitProbability() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.add(new Unit(INFANTRY, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(ARMOUR, BRITISH, data));

    final int diceSides = data.getDiceSides();
    final Map<Integer, Integer> diceGrouped = getDiceGrouped(attackingOrderOfLoss, List.of());
    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, List.of(), GERMAN);
    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    final List<Double> hitProbability = root.calculateHitProbabilities(
        BattleStep.RollData.of(2, diceGrouped, diceSides)
    );

    assertEquals(0.4166, hitProbability.get(0), 0.0001);
    assertEquals(0.5, hitProbability.get(1), 0.0001);
    assertEquals(0.0833, hitProbability.get(2), 0.0001);
  }

  @Test
  void calculateSingleHitProbabilityWithTwoDifferentUnits() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.add(new Unit(INFANTRY, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(ARMOUR, BRITISH, data));

    final int diceSides = data.getDiceSides();
    final Map<Integer, Integer> diceGrouped = getDiceGrouped(attackingOrderOfLoss, List.of());

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, List.of(), GERMAN);
    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    final List<Double> hitProbability = root.calculateHitProbabilities(
        BattleStep.RollData.of(2, diceGrouped, diceSides)
    );

    assertEquals(0.5, hitProbability.get(1), 0.0001);
  }

  @Test
  void calculate3HitProbabilityWithTwoOfTwoDifferentUnits() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.add(new Unit(INFANTRY, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(INFANTRY, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(ARMOUR, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(ARMOUR, BRITISH, data));

    final int diceSides = data.getDiceSides();
    final Map<Integer, Integer> diceGrouped = getDiceGrouped(attackingOrderOfLoss, List.of());

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, List.of(), GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    final List<Double> hitProbability = root.calculateHitProbabilities(
        BattleStep.RollData.of(4, diceGrouped, diceSides)
    );

    assertEquals(0.0833, hitProbability.get(3), 0.0001);
  }

  @Test
  void calculateHitProbabilityWith2_3_2_units() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.add(new Unit(INFANTRY, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(INFANTRY, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(ARMOUR, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(ARMOUR, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(ARMOUR, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(TACTICAL_BOMBER, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(TACTICAL_BOMBER, BRITISH, data));

    final int diceSides = data.getDiceSides();
    final Map<Integer, Integer> diceGrouped = getDiceGrouped(attackingOrderOfLoss, List.of());

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, List.of(), GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    final List<Double> hitProbability = root.calculateHitProbabilities(
        BattleStep.RollData.of(7, diceGrouped, diceSides)
    );

    assertEquals(0.3171, hitProbability.get(3), 0.0001);
  }

  @Test
  void calculateAllHitProbabilitiesWith2_3_2_units() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.add(new Unit(INFANTRY, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(INFANTRY, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(ARMOUR, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(ARMOUR, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(ARMOUR, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(TACTICAL_BOMBER, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(TACTICAL_BOMBER, BRITISH, data));

    final int diceSides = data.getDiceSides();
    final Map<Integer, Integer> diceGrouped = getDiceGrouped(attackingOrderOfLoss, List.of());

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, List.of(), GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    final List<Double> hitProbability = root.calculateHitProbabilities(
        BattleStep.RollData.of(7, diceGrouped, diceSides)
    );

    assertEquals(0.0096, hitProbability.get(0), 0.0001);
    assertEquals(0.0713, hitProbability.get(1), 0.0001);
    assertEquals(0.2106, hitProbability.get(2), 0.0001);
    assertEquals(0.3171, hitProbability.get(3), 0.0001);
    assertEquals(0.2581, hitProbability.get(4), 0.0001);
    assertEquals(0.1099, hitProbability.get(5), 0.0001);
    assertEquals(0.0216, hitProbability.get(6), 0.0001);
    assertEquals(0.0015, hitProbability.get(7), 0.0001);
  }

  private BattleStep.Parameters createParameters() {
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

  final static class CalculateResult {
    final double THRESHOLD = .001;
    double winProbability = 0;
    double loseProbability = 0;
    double tieProbability = 0;
    double badProbability = 0;

    @Override
    public String toString() {
      return "CalculateResult{" +
          "winProbability=" + winProbability +
          ", loseProbability=" + loseProbability +
          ", tieProbability=" + tieProbability +
          ", badProbability=" + badProbability +
          '}';
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final CalculateResult that = (CalculateResult) o;
      return Math.abs(that.winProbability - winProbability) < THRESHOLD &&
          Math.abs(that.loseProbability - loseProbability) < THRESHOLD &&
          Math.abs(that.tieProbability - tieProbability) < THRESHOLD &&
          Math.abs(that.badProbability - badProbability) < THRESHOLD;
    }

    @Override
    public int hashCode() {
      return Objects.hash(winProbability, loseProbability, tieProbability, badProbability);
    }
  }

  @Test
  void simpleFightCalculation() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.add(ARMOUR.create(BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.add(INFANTRY.create(GERMAN));

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, defendingOrderOfLoss, GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    root.calculateBattle(attackingUnits, GERMAN);
    final CalculateResult expected = new CalculateResult();
    expected.winProbability = 0.499;
    expected.loseProbability = 0.249;
    expected.tieProbability = 0.249;
    expected.badProbability = 0.0;
    final CalculateResult actual = new CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);
  }

  @Test
  void attackerWithMultUnitsVsSingleUnit() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.add(new Unit(INFANTRY, BRITISH, data));
    attackingOrderOfLoss.add(new Unit(INFANTRY, BRITISH, data));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.add(new Unit(INFANTRY, GERMAN, data));

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, defendingOrderOfLoss, GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    root.calculateBattle(attackingUnits, GERMAN);
    final CalculateResult expected = new CalculateResult();
    expected.winProbability = 0.671;
    expected.loseProbability = 0.263;
    expected.tieProbability = 0.052;
    expected.badProbability = 0.011;
    final CalculateResult actual = new CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);
  }

  @Test
  void attackerWithInf1v1() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(INFANTRY.create(1, GERMAN));

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, defendingOrderOfLoss, GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    root.calculateBattle(attackingUnits, GERMAN);
    final CalculateResult expected = new CalculateResult();
    expected.winProbability = 0.245;
    expected.loseProbability = 0.614;
    expected.tieProbability = 0.122;
    expected.badProbability = 0.016;
    final CalculateResult actual = new CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);
  }

  @Test
  void attackerWithInf2v2() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(2, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(INFANTRY.create(2, GERMAN));

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, defendingOrderOfLoss, GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    root.calculateBattle(attackingUnits, GERMAN);
    final CalculateResult expected = new CalculateResult();
    expected.winProbability = 0.212;
    expected.loseProbability = 0.737;
    expected.tieProbability = 0.042;
    expected.badProbability = 0.008;
    final CalculateResult actual = new CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);
  }

  // The low probability tests are using IGNORE_BRANCH_PROBABILITY = 0.005
  // if that changes, these results will need to be updated
  @Test
  void fightWithLowProbabilityBranch1() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(5, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(INFANTRY.create(5, GERMAN));
    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, defendingOrderOfLoss, GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    root.calculateBattle(attackingUnits, GERMAN);
    final CalculateResult expected = new CalculateResult();
    expected.winProbability = 0.116;
    expected.loseProbability = 0.861;
    expected.tieProbability = 0.008;
    expected.badProbability = 0.013;
    final CalculateResult actual = new CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);
  }

  @Test
  void fightWithLowProbabilityBranch2() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(10, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(INFANTRY.create(10, GERMAN));

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, defendingOrderOfLoss, GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    root.calculateBattle(attackingUnits, GERMAN);
    final CalculateResult expected = new CalculateResult();
    expected.winProbability = 0.049;
    expected.loseProbability = 0.935;
    expected.tieProbability = 0.001;
    expected.badProbability = 0.013;
    final CalculateResult actual = new CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);
  }

  @Test
  void fightWithLowProbabilityBranch3() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(15, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(INFANTRY.create(15, GERMAN));

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, defendingOrderOfLoss, GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    root.calculateBattle(attackingUnits, GERMAN);
    final CalculateResult expected = new CalculateResult();
    expected.winProbability = 0.021;
    expected.loseProbability = 0.961;
    expected.tieProbability = 0.000;
    expected.badProbability = 0.015;
    final CalculateResult actual = new CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);
  }

  @Test
  void fightWithLowProbabilityBranch4() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(20, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(INFANTRY.create(20, GERMAN));

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, defendingOrderOfLoss, GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    root.calculateBattle(attackingUnits, GERMAN);
    final CalculateResult expected = new CalculateResult();
    expected.winProbability = 0.009;
    expected.loseProbability = 0.966;
    expected.tieProbability = 0.000;
    expected.badProbability = 0.024;
    final CalculateResult actual = new CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);
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

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, defendingOrderOfLoss, GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    root.calculateBattle(attackingUnits, GERMAN);
    final CalculateResult expected = new CalculateResult();
    expected.winProbability = 0.453;
    expected.loseProbability = 0.453;
    expected.tieProbability = 0.093;
    expected.badProbability = 0.0;
    final CalculateResult actual = new CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);

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

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, defendingOrderOfLoss, GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    root.calculateBattle(attackingUnits, GERMAN);
    final CalculateResult expected = new CalculateResult();
    expected.winProbability = 0.985;
    expected.loseProbability = 0.011;
    expected.tieProbability = 0.003;
    expected.badProbability = 0.0;
    final CalculateResult actual = new CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);

    assertEquals(attackingOrderOfLoss.subList(2, attackingOrderOfLoss.size()), root.getAverageUnits().getFriendlyWithChance(0.5));
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

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, defendingOrderOfLoss, GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    root.calculateBattle(attackingUnits, GERMAN);
    final CalculateResult expected = new CalculateResult();
    expected.winProbability = 0.018;
    expected.loseProbability = 0.976;
    expected.tieProbability = 0.005;
    expected.badProbability = 0.0;
    final CalculateResult actual = new CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);

    assertEquals(List.of(), root.getAverageUnits().getFriendlyWithChance(0.5));
    assertEquals(defendingOrderOfLoss.subList(2, defendingOrderOfLoss.size()), root.getAverageUnits().getEnemyWithChance(0.5));
  }

  @Test
  void attackFighterVsAa() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(FIGHTER.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(AAGUN.create(1, GERMAN));

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, defendingOrderOfLoss, GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    root.calculateBattle(attackingUnits, GERMAN);
    final CalculateResult expected = new CalculateResult();
    expected.winProbability = 0.833;
    expected.loseProbability = 0.166;
    expected.tieProbability = 0.0;
    expected.badProbability = 0.0;
    final CalculateResult actual = new CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);
  }

  @Test
  void attackFighterInfantryVsAaInfantry() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(INFANTRY.create(1, BRITISH));
    attackingOrderOfLoss.addAll(FIGHTER.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(AAGUN.create(1, GERMAN));
    defendingOrderOfLoss.addAll(INFANTRY.create(1, GERMAN));

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, defendingOrderOfLoss, GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createParameters()
    );
    root.calculateBattle(attackingUnits, GERMAN);
    final CalculateResult expected = new CalculateResult();
    expected.winProbability = 0.549;
    expected.loseProbability = 0.363;
    expected.tieProbability = 0.083;
    expected.badProbability = 0.003;
    final CalculateResult actual = new CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);
  }

  @Test
  void attackBattleshipVsCruiser() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(BATTLESHIP.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(CRUISER.create(1, GERMAN));

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, defendingOrderOfLoss, GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createSeaParameters()
    );
    root.calculateBattle(attackingUnits, GERMAN);
    final CalculateResult expected = new CalculateResult();
    expected.winProbability = 0.879;
    expected.loseProbability = 0.039;
    expected.tieProbability = 0.079;
    expected.badProbability = 0.000;
    final CalculateResult actual = new CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);
  }

  @Test
  void attackFighterVsSubmarine() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(FIGHTER.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(SUBMARINE.create(1, GERMAN));

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, defendingOrderOfLoss, GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createSeaParameters()
    );
    root.calculateBattle(attackingUnits, GERMAN);
    final CalculateResult expected = new CalculateResult();
    expected.winProbability = 1.0;
    expected.loseProbability = 0.0;
    expected.tieProbability = 0.0;
    expected.badProbability = 0.0;
    final CalculateResult actual = new CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);
  }

  @Test
  void attackSubmarineVsFighterDestroy() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(SUBMARINE.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(FIGHTER.create(1, GERMAN));
    defendingOrderOfLoss.addAll(DESTROYER.create(1, GERMAN));

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, defendingOrderOfLoss, GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createSeaParameters()
    );
    root.calculateBattle(attackingUnits, GERMAN);
    final CalculateResult expected = new CalculateResult();
    expected.winProbability = 0.0;
    expected.loseProbability = 0.999;
    expected.tieProbability = 0.0;
    expected.badProbability = 0.0;
    final CalculateResult actual = new CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);
  }

  @Test
  void attackSubmarineBomberVsFighterDestroy() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(SUBMARINE.create(1, BRITISH));
    attackingOrderOfLoss.addAll(BOMBER.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(DESTROYER.create(1, GERMAN));
    defendingOrderOfLoss.addAll(FIGHTER.create(1, GERMAN));

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, defendingOrderOfLoss, GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createSeaParameters()
    );
    root.calculateBattle(attackingUnits, GERMAN);
    final CalculateResult expected = new CalculateResult();
    expected.winProbability = 0.300;
    expected.loseProbability = 0.467;
    expected.tieProbability = 0.231;
    expected.badProbability = 0.0;
    final CalculateResult actual = new CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);
  }

  @Test
  void attackSubmarineVsFighterCruiser() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(SUBMARINE.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(CRUISER.create(1, GERMAN));
    defendingOrderOfLoss.addAll(FIGHTER.create(1, GERMAN));

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, defendingOrderOfLoss, GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createSeaParameters()
    );
    root.calculateBattle(attackingUnits, GERMAN);
    final CalculateResult expected = new CalculateResult();
    expected.winProbability = 0.0;
    expected.loseProbability = 0.999;
    expected.tieProbability = 0.0;
    expected.badProbability = 0.0;
    final CalculateResult actual = new CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);
  }

  @Test
  void attackSubmarineVsBattleship() {
    final List<Unit> attackingOrderOfLoss = new ArrayList<>();
    attackingOrderOfLoss.addAll(SUBMARINE.create(1, BRITISH));
    final List<Unit> defendingOrderOfLoss = new ArrayList<>();
    defendingOrderOfLoss.addAll(BATTLESHIP.create(1, GERMAN));

    final StepUnits attackingUnits = new StepUnits(attackingOrderOfLoss, BRITISH, defendingOrderOfLoss, GERMAN);

    final BattleStep root = new BattleStep(
        attackingUnits,
        BRITISH,
        0,
        createSeaParameters()
    );
    root.calculateBattle(attackingUnits, GERMAN);
    final CalculateResult expected = new CalculateResult();
    expected.winProbability = 0.061;
    expected.loseProbability = 0.938;
    expected.tieProbability = 0.0;
    expected.badProbability = 0.0;
    final CalculateResult actual = new CalculateResult();
    actual.winProbability = root.getWinProbability();
    actual.loseProbability = root.getLoseProbability();
    actual.tieProbability = root.getTieProbability();
    actual.badProbability = root.getBadProbability();
    assertEquals(expected, actual);
  }
}