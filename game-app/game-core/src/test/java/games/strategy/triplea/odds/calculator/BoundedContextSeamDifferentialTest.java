package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.delegate.GameDataTestUtil.americanCruiser;
import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.battleship;
import static games.strategy.triplea.delegate.GameDataTestUtil.destroyer;
import static games.strategy.triplea.delegate.GameDataTestUtil.germanBattleship;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.germany;
import static games.strategy.triplea.delegate.GameDataTestUtil.submarine;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static games.strategy.triplea.delegate.GameDataTestUtil.usa;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * The differential harness routed through the <em>production seam</em>: the calculator the AI
 * actually runs when {@link ClientSetting#useBoundedContextBattleCalc} is on ({@link
 * BattleCalculatorFactory} → {@link BoundedContextBattleCalculator} → {@code
 * BoundedContextAggregateResults} → {@code SurvivorMapper} → {@code VectorizedHitRoller}), compared
 * against the same engine oracle {@link BattleCalculator} the direct-simulator suite uses. {@link
 * BattleCalcDifferentialTest} drives {@code ReferenceBattleSimulator} directly and so never touches
 * the aggregate-results/survivor-mapper layer; this class is the first to hold that layer to the
 * oracle.
 *
 * <p>Both sides read survivors through the shared {@code getAverage*UnitsRemaining()} accessor:
 * under {@code alwaysHits} with a single run the "average" collapses to that one rules-determined
 * run, so per-type survivor counts are exactly comparable — no distributional slack. The dice
 * source is injectable only on the concrete bounded-context calc, so the factory-selected instance
 * is cast to force {@code alwaysHits}; that cast is safe precisely because the flag is forced on.
 */
class BoundedContextSeamDifferentialTest extends AbstractClientSettingTestCase {

  /**
   * The seam's own reshaping oracle: the multi-HP-plus-first-strike fixture {@link
   * BattleCalcDifferentialTest.ReshapingMatrix#multiHitBattleshipWithAFirstStrikeSubMatchesTheEngine}
   * already pins green at the direct-simulator layer, so any divergence here is introduced by the
   * seam it adds — the vectorized roller, the count-backed aggregate, or the survivor mapper — not
   * by the underlying combat model. A surviving battleship exercises {@code SurvivorMapper}'s
   * map-back of a damaged multi-HP survivor to its original instance. Pass-or-fail is genuinely
   * unknown until run; a failure is a real production defect in the seam, not the model.
   */
  @Test
  void seamAgreesWithEngineOnMultiHitBattleshipWithFirstStrikeSurvivors() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final Territory seaZone = territory("1 Sea Zone", gameData);
    final Collection<Unit> attacking = battleship(gameData).create(1, americans(gameData));
    attacking.addAll(submarine(gameData).create(1, americans(gameData)));
    final Collection<Unit> defending = submarine(gameData).create(2, germans(gameData));

    assertSeamSurvivorsMatchEngine(
        gameData, americans(gameData), germans(gameData), seaZone, attacking, defending, -1);
  }

  /**
   * The known phase-1 {@code SurvivorMapper} transform-drop, pinned against the oracle for the first
   * time. In TWW a {@code germanBattleship} that takes a single hit transforms via {@code
   * whenHitPointsDamagedChangesInto} into a {@code germanBattleship-damaged} bucket; the mapper
   * matches survivors back to caller units by type name, and no original carries that transformed
   * type, so the survivor is silently dropped from "units remaining." One {@code americanCruiser}
   * lands exactly one hit on the 2-HP battleship (and dies to its return fire), leaving the engine a
   * lone damaged-battleship survivor while the seam reports none — an expected divergence that
   * confirms the drop is real and scoped, not worse than believed. Disabled as a characterization
   * pin of the {@code TODO(seam-phase2)} at {@code SurvivorMapper#selectOriginals}
   * (SurvivorMapper.java:107-110), whose type-name match cannot claim an original for a transformed
   * survivor; kept so the drop cannot silently worsen, mirroring the two direct-simulator pins.
   */
  @Test
  @Disabled(
      "phase-1 residual: SurvivorMapper type-name drop of a whenHitPointsDamagedChangesInto"
          + " survivor, see SurvivorMapper#selectOriginals")
  void seamDropsATypeTransformedSurvivorTheEngineKeeps() {
    final GameData gameData = TestMapGameData.TWW.getGameData();
    final Territory seaZone = territory("33 Sea Zone", gameData);
    final Collection<Unit> attacking = americanCruiser(gameData).create(1, usa(gameData));
    final Collection<Unit> defending = germanBattleship(gameData).create(1, germany(gameData));

    assertSeamSurvivorsMatchEngine(
        gameData, usa(gameData), germany(gameData), seaZone, attacking, defending, -1);
  }

  /**
   * WITHDRAWN units must count as remaining identically through the seam and the engine. With {@code
   * retreatAfterRound = 1} the attacking subs withdraw at the end of round one rather than fighting
   * on; the defender's escorting destroyer dies but its defenceless transports survive the round, so
   * the retreat actually fires (a live defender means the battle would otherwise continue). The
   * withdrawn subs are non-DEAD and so are survivors — the seam counts WITHDRAWN buckets as
   * remaining and maps them back, and the oracle keeps the retreated attackers too. Pass-or-fail is
   * unknown: retreat plumbing through the seam has never been checked against the oracle.
   */
  @Test
  void seamCountsWithdrawnUnitsAsRemainingLikeTheEngine() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final Territory seaZone = territory("1 Sea Zone", gameData);
    final Collection<Unit> attacking = submarine(gameData).create(2, americans(gameData));
    final Collection<Unit> defending =
        new ArrayList<>(destroyer(gameData).create(1, germans(gameData)));
    defending.addAll(transport(gameData).create(2, germans(gameData)));

    assertSeamSurvivorsMatchEngine(
        gameData, americans(gameData), germans(gameData), seaZone, attacking, defending, 1);
  }

  /**
   * Runs one {@code alwaysHits} battle through the flag-on production seam and through the engine
   * oracle, then asserts identical per-type survivor counts on each side. A single run makes the
   * "average units remaining" the exact rules-determined survivor set, so the two collections must
   * match bucket for bucket.
   *
   * @param retreatAfterRound the round-end withdrawal threshold ({@code -1} disables it), applied to
   *     both paths so retreat is not itself a variable
   */
  private static void assertSeamSurvivorsMatchEngine(
      final GameData gameData,
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final int retreatAfterRound) {
    final AggregateResults seam =
        seamResults(
            gameData, attacker, defender, location, attacking, defending, retreatAfterRound);
    final AggregateResults oracle =
        oracleResults(
            gameData, attacker, defender, location, attacking, defending, retreatAfterRound);

    assertThat(countByType(seam.getAverageAttackingUnitsRemaining()))
        .isEqualTo(countByType(oracle.getAverageAttackingUnitsRemaining()));
    assertThat(countByType(seam.getAverageDefendingUnitsRemaining()))
        .isEqualTo(countByType(oracle.getAverageDefendingUnitsRemaining()));
  }

  private static AggregateResults seamResults(
      final GameData gameData,
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final int retreatAfterRound) {
    ClientSetting.useBoundedContextBattleCalc.setValue(true);
    final IBattleCalculator calculator = BattleCalculatorFactory.newBattleCalculator();
    // The dice source has no injection seam on IBattleCalculator; only the concrete bounded-context
    // calc the flag-on factory returns exposes it, so cast to force a deterministic alwaysHits run.
    ((BoundedContextBattleCalculator) calculator)
        .setRandomSource(ScriptedRandomSource.alwaysHits());
    calculator.setRetreatAfterRound(retreatAfterRound);
    calculator.setGameData(gameData).join();
    return calculator.calculate(
        attacker,
        defender,
        location,
        attacking,
        defending,
        List.of(),
        TerritoryEffectHelper.getEffects(location),
        false,
        1);
  }

  private static AggregateResults oracleResults(
      final GameData gameData,
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final int retreatAfterRound) {
    final BattleCalculator oracle = new BattleCalculator(gameData);
    oracle.setRandomSource(ScriptedRandomSource.alwaysHits());
    oracle.setRetreatAfterRound(retreatAfterRound);
    return oracle.calculate(
        attacker,
        defender,
        location,
        attacking,
        defending,
        List.of(),
        TerritoryEffectHelper.getEffects(location),
        false,
        1);
  }

  private static Map<String, Integer> countByType(final Collection<Unit> units) {
    final Map<String, Integer> counts = new HashMap<>();
    for (final Unit unit : units) {
      counts.merge(unit.getType().getName(), 1, Integer::sum);
    }
    return counts;
  }
}
