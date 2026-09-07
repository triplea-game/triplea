package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.destroyer;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.submarine;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.odds.calculator.adapter.EngineRandomSource;
import games.strategy.triplea.odds.calculator.adapter.GameDataBattleAdapter;
import games.strategy.triplea.odds.calculator.context.model.BattleOptions;
import games.strategy.triplea.odds.calculator.context.model.BattleResult;
import games.strategy.triplea.odds.calculator.context.model.BattleScenario;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.reference.ReferenceBattleSimulator;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The differential harness (design §9, the permanent CI gate) — the acceptance bar that keeps the
 * new {@code GameData}-free calc bit-honest against the engine it replaces. The oracle is the
 * <em>existing</em> {@link BattleCalculator} (a real {@code MustFightBattle} over a cloned {@code
 * GameData}); it is never reimplemented, only called. The same units and the same {@code
 * IRandomSource} feed both the oracle and the new {@link ReferenceBattleSimulator} (via the {@link
 * GameDataBattleAdapter}, the one acceptance test allowed to touch {@code GameData} because it
 * drives the oracle side).
 *
 * <p>Two oracles by regime: under {@code alwaysHits} luck is removed and the outcome is
 * rules-determined, so survivor counts must be <em>identical</em>; under a seeded source only the
 * distribution is comparable, so win% is asserted within a tolerance.
 *
 * <p><b>RED until Phase 2.</b> Both {@code GameDataBattleAdapter.toScenario} and {@code
 * ReferenceBattleSimulator.simulate} are throwing stubs, so every case here fails at runtime on the
 * new-path call. Writing them now fixes the bar. The starter matrix below seeds the seams the model
 * reshapes most; WW2V2 both-sneak and V2-vs-V3 bombard need other map XMLs and are deferred to the
 * full Phase-3 fuzzing gate.
 */
class BattleCalcDifferentialTest extends AbstractClientSettingTestCase {

  /** Fixed so the seeded comparison is reproducible build to build. */
  private static final long SEED = 20260906L;

  @Test
  void alwaysHitsGivesIdenticalSurvivorsForAnInfantryBrawl() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final Territory germany = territory("Germany", gameData);

    assertIdenticalSurvivorsUnderAlwaysHits(
        gameData,
        russians(gameData),
        germans(gameData),
        germany,
        infantry(gameData).create(3, russians(gameData)),
        infantry(gameData).create(2, germans(gameData)));
  }

  @Test
  void seededRunsAgreeOnAttackerWinPercentWithinTolerance() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final Territory germany = territory("Germany", gameData);
    final GamePlayer attacker = russians(gameData);
    final GamePlayer defender = germans(gameData);
    final Collection<Unit> attacking = infantry(gameData).create(5, attacker);
    final Collection<Unit> defending = infantry(gameData).create(5, defender);

    final BattleCalculator oracle = new BattleCalculator(gameData);
    oracle.setRandomSource(new PlainRandomSource(SEED));
    final double oracleWinPercent =
        oracle
            .calculate(
                attacker,
                defender,
                germany,
                attacking,
                defending,
                List.of(),
                TerritoryEffectHelper.getEffects(germany),
                false,
                500)
            .getAttackerWinPercent();

    final BattleScenario scenario =
        new GameDataBattleAdapter()
            .toScenario(
                attacker,
                defender,
                germany,
                attacking,
                defending,
                List.of(),
                TerritoryEffectHelper.getEffects(germany),
                new BattleOptions(false, List.of(), List.of()));
    // Separate same-seed source per side: the oracle exhausts its 500 runs before the new path
    // starts, so a shared instance would diverge — same seed keeps both sequences identical.
    final double newWinPercent =
        new ReferenceBattleSimulator()
            .simulate(scenario, 500, new EngineRandomSource(new PlainRandomSource(SEED)))
            .attackerWinPercent();

    assertThat(newWinPercent).isCloseTo(oracleWinPercent, within(0.1));
  }

  /**
   * The scenarios the vector model reshapes hardest — each one a place where casualty timing,
   * targeting, or damage migration diverges from naive per-unit iteration. All go green only after
   * Phase-2 integration wires the adapter and simulator.
   */
  @Nested
  class ReshapingMatrix {
    @Test
    void destroyerVersusSubmarineSurvivorsMatchTheEngine() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory seaZone = territory("1 Sea Zone", gameData);

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          destroyer(gameData).create(2, americans(gameData)),
          submarine(gameData).create(2, germans(gameData)));
    }

    @Test
    void transportCasualtyRestrictionMatchesTheEngine() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory seaZone = territory("1 Sea Zone", gameData);

      final Collection<Unit> defenders = transport(gameData).create(1, germans(gameData));
      defenders.addAll(submarine(gameData).create(1, germans(gameData)));

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          submarine(gameData).create(2, americans(gameData)),
          defenders);
    }
  }

  /**
   * The exact-equality oracle: one {@code alwaysHits} run through both paths must leave the same
   * per-unit-type survivor counts on each side. Single-type or engine-default-order fights are used
   * so the casualty <em>order</em> is not itself a variable — see the class note on OOL injection.
   *
   * <pre>
   * (1) oracle: real BattleCalculator, alwaysHits, one run -> remaining units per side
   * (2) new: adapter bakes a scenario, reference simulator runs it alwaysHits, one run
   * (3) validate: attacker survivors by type equal; defender survivors by type equal
   * </pre>
   */
  private static void assertIdenticalSurvivorsUnderAlwaysHits(
      final GameData gameData,
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending) {
    // One shared alwaysHits source feeds both sides through the bridge, so the oracle and the new
    // path draw from identical dice; safe to share because every roll is a deterministic 0.
    final ScriptedRandomSource dice = ScriptedRandomSource.alwaysHits();
    final BattleCalculator oracle = new BattleCalculator(gameData);
    oracle.setRandomSource(dice);
    final var oracleResult =
        oracle
            .calculate(
                attacker,
                defender,
                location,
                attacking,
                defending,
                List.of(),
                TerritoryEffectHelper.getEffects(location),
                false,
                1)
            .getResults()
            .get(0);

    final BattleScenario scenario =
        new GameDataBattleAdapter()
            .toScenario(
                attacker,
                defender,
                location,
                attacking,
                defending,
                List.of(),
                TerritoryEffectHelper.getEffects(location),
                new BattleOptions(false, List.of(), List.of()));
    final BattleResult newResult =
        new ReferenceBattleSimulator()
            .simulate(scenario, 1, new EngineRandomSource(dice))
            .results()
            .get(0);

    assertThat(countByType(newResult.attackerSurvivors()))
        .isEqualTo(countByType(oracleResult.getRemainingAttackingUnits()));
    assertThat(countByType(newResult.defenderSurvivors()))
        .isEqualTo(countByType(oracleResult.getRemainingDefendingUnits()));
  }

  private static Map<String, Integer> countByType(final Collection<Unit> units) {
    final Map<String, Integer> counts = new HashMap<>();
    for (final Unit unit : units) {
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
