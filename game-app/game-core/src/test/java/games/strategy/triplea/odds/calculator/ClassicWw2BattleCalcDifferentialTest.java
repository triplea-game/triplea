package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.artillery;
import static games.strategy.triplea.delegate.GameDataTestUtil.battleship;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.unitType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.battle.BattleResults;
import games.strategy.triplea.odds.calculator.adapter.BoundedContextAggregateResults;
import games.strategy.triplea.odds.calculator.adapter.EngineRandomSource;
import games.strategy.triplea.odds.calculator.adapter.GameDataBattleAdapter;
import games.strategy.triplea.odds.calculator.context.model.BattleOptions;
import games.strategy.triplea.odds.calculator.context.model.BattleResult;
import games.strategy.triplea.odds.calculator.context.model.BattleScenario;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Outcome;
import games.strategy.triplea.odds.calculator.context.model.SimulationResults;
import games.strategy.triplea.odds.calculator.context.reference.ReferenceBattleSimulator;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Differential gates on the rules World War II Revised (WW2V2, Low Luck as the AI harness plays it)
 * and World War II v3 (1941 and 1942, dice) actually play, so the bounded calculator can be trusted
 * to drive ProAI on those maps. Same oracle as {@link BigWorldBattleCalcDifferentialTest}: the real
 * {@link BattleCalculator} fed the same units and dice — identical survivors and outcome under
 * {@code alwaysHits}, win% and TUV swing within a tolerance under a seeded source.
 */
class ClassicWw2BattleCalcDifferentialTest extends AbstractClientSettingTestCase {

  private static final long SEED = 20261026L;
  private static final int SEEDED_RUNS = 2000;
  private static final BattleOptions NO_RETREAT = new BattleOptions(false, List.of(), List.of());

  /**
   * v3 sets {@code Naval Bombard Casualties Return Fire}: a defender killed by bombardment is not
   * removed before general combat ({@code ClearBombardmentCasualties}), so it still fires back in
   * round 1.
   */
  @Nested
  class V3BombardReturnFire {
    @Test
    void aBombardedDefenderStillFiresBackInTheFirstRound() {
      final GameData gameData = TestMapGameData.WW2V3_1941.getGameData();

      assertIdenticalUnderAlwaysHits(
          gameData,
          russians(gameData),
          germans(gameData),
          territory("France", gameData),
          infantry(gameData).create(2, russians(gameData)),
          infantry(gameData).create(2, germans(gameData)),
          battleship(gameData).create(1, russians(gameData)));
    }

    @Test
    void seededAmphibiousAssaultWithCruiserAndBattleshipBombardMatchesTheEngine() {
      final GameData gameData = TestMapGameData.WW2V3_1942.getGameData();
      final Collection<Unit> attacking = infantry(gameData).create(3, russians(gameData));
      attacking.addAll(armour(gameData).create(1, russians(gameData)));
      final Collection<Unit> bombarding = battleship(gameData).create(1, russians(gameData));
      bombarding.addAll(unitType("cruiser", gameData).create(1, russians(gameData)));

      assertSeededBattleAgreement(
          gameData,
          russians(gameData),
          germans(gameData),
          territory("France", gameData),
          attacking,
          infantry(gameData).create(4, germans(gameData)),
          bombarding);
    }

    @Test
    void seededLargeAssaultWithBombardMatchesTheEngine() {
      final GameData gameData = TestMapGameData.WW2V3_1941.getGameData();
      final Collection<Unit> attacking = infantry(gameData).create(6, russians(gameData));
      attacking.addAll(artillery(gameData).create(3, russians(gameData)));
      attacking.addAll(armour(gameData).create(3, russians(gameData)));
      attacking.addAll(fighter(gameData).create(3, russians(gameData)));
      final Collection<Unit> defending = infantry(gameData).create(8, germans(gameData));
      defending.addAll(artillery(gameData).create(2, germans(gameData)));
      defending.addAll(armour(gameData).create(2, germans(gameData)));
      defending.addAll(fighter(gameData).create(2, germans(gameData)));

      assertSeededBattleAgreement(
          gameData,
          russians(gameData),
          germans(gameData),
          territory("France", gameData),
          attacking,
          defending,
          battleship(gameData).create(2, russians(gameData)));
    }
  }

  /**
   * One {@code alwaysHits} run through the engine and the bounded simulator must leave the same
   * survivors by type on each side and the same winner.
   */
  private static void assertIdenticalUnderAlwaysHits(
      final GameData gameData,
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final Collection<Unit> bombarding) {
    final ScriptedRandomSource dice = ScriptedRandomSource.alwaysHits();
    final BattleResults engine =
        engine(gameData, attacker, defender, location, attacking, defending, bombarding, dice, 1)
            .getResults()
            .get(0);
    final BattleResult bounded =
        bounded(attacker, defender, location, attacking, defending, bombarding, dice, 1)
            .results()
            .get(0);

    assertThat(countByType(bounded.attackerSurvivors()))
        .as("attacker survivors")
        .isEqualTo(countByType(engine.getRemainingAttackingUnits()));
    assertThat(countByType(bounded.defenderSurvivors()))
        .as("defender survivors")
        .isEqualTo(countByType(engine.getRemainingDefendingUnits()));
    assertThat(bounded.outcome()).as("outcome").isEqualTo(outcomeOf(engine));
  }

  /**
   * Same-seed runs through both paths must agree on attacker and defender win% within 4 points, and
   * on the mean TUV swing ProAI scores attacks by within 2.5 TUV or 10% of the engine's, whichever
   * is larger. A distributional guard: the two paths consume dice in different orders.
   */
  private static void assertSeededBattleAgreement(
      final GameData gameData,
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final Collection<Unit> bombarding) {
    final AggregateResults engine =
        engine(
            gameData,
            attacker,
            defender,
            location,
            attacking,
            defending,
            bombarding,
            new PlainRandomSource(SEED),
            SEEDED_RUNS);
    final AggregateResults bounded =
        new BoundedContextAggregateResults(
            bounded(
                attacker,
                defender,
                location,
                attacking,
                defending,
                bombarding,
                new PlainRandomSource(SEED),
                SEEDED_RUNS),
            attacking,
            defending);

    assertThat(bounded.getAttackerWinPercent())
        .as("attacker win%")
        .isCloseTo(engine.getAttackerWinPercent(), within(0.04));
    assertThat(bounded.getDefenderWinPercent())
        .as("defender win%")
        .isCloseTo(engine.getDefenderWinPercent(), within(0.04));
    final double engineSwing =
        engine.getAverageTuvSwing(attacker, attacking, defender, defending, gameData);
    assertThat(bounded.getAverageTuvSwing(attacker, attacking, defender, defending, gameData))
        .as("mean TUV swing")
        .isCloseTo(engineSwing, within(Math.max(2.5, Math.abs(engineSwing) * 0.1)));
  }

  private static AggregateResults engine(
      final GameData gameData,
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final Collection<Unit> bombarding,
      final IRandomSource dice,
      final int runs) {
    final BattleCalculator oracle = new BattleCalculator(gameData);
    oracle.setRandomSource(dice);
    return oracle.calculate(
        attacker,
        defender,
        location,
        attacking,
        defending,
        bombarding,
        TerritoryEffectHelper.getEffects(location),
        NO_RETREAT.retreatWhenOnlyAirLeft(),
        runs);
  }

  private static SimulationResults bounded(
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final Collection<Unit> bombarding,
      final IRandomSource dice,
      final int runs) {
    final BattleScenario scenario =
        new GameDataBattleAdapter()
            .toScenario(
                attacker,
                defender,
                location,
                attacking,
                defending,
                bombarding,
                TerritoryEffectHelper.getEffects(location),
                NO_RETREAT);
    return new ReferenceBattleSimulator().simulate(scenario, runs, new EngineRandomSource(dice));
  }

  private static Outcome outcomeOf(final BattleResults engine) {
    if (engine.attackerWon()) {
      return Outcome.ATTACKER_WINS;
    }
    return engine.defenderWon() ? Outcome.DEFENDER_WINS : Outcome.DRAW;
  }

  private static Map<String, Integer> countByType(final Collection<Unit> units) {
    final Map<String, Integer> counts = new HashMap<>();
    for (final Unit unit : new ArrayList<>(units)) {
      counts.merge(unit.getType().getName(), 1, Integer::sum);
    }
    return counts;
  }

  private static Map<String, Integer> countByType(final Force survivors) {
    final Map<String, Integer> counts = new HashMap<>();
    survivors
        .counts()
        .forEach((key, count) -> counts.merge(key.profile().type().name(), count, Integer::sum));
    return counts;
  }
}
