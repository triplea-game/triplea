package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.delegate.GameDataTestUtil.aaGun;
import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.battleship;
import static games.strategy.triplea.delegate.GameDataTestUtil.carrier;
import static games.strategy.triplea.delegate.GameDataTestUtil.destroyer;
import static games.strategy.triplea.delegate.GameDataTestUtil.factory;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.russians;
import static games.strategy.triplea.delegate.GameDataTestUtil.submarine;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
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
 * Differential gates on the rules Big World 1942 actually plays (WW2V2, two-hit battleships, LHTR
 * heavy bombers, battleship bombard, factories and AA guns in defended territories), so the bounded
 * calculator can be trusted to drive ProAI on that map. Same oracle as {@link
 * BattleCalcDifferentialTest}: the real {@link BattleCalculator} fed the same units and dice —
 * identical survivors and outcome under {@code alwaysHits}, attacker win% within a tolerance under
 * a seeded source.
 */
class BigWorldBattleCalcDifferentialTest extends AbstractClientSettingTestCase {

  private static final long SEED = 20261025L;
  private static final int SEEDED_RUNS = 2000;

  /**
   * Infrastructure that cannot fight (a factory; an AA gun past its firing round) leaves the battle
   * before main combat ({@code RemoveNonCombatants}), so it can never soak a hit, and it does not
   * keep a beaten defender alive. Weakest-first ordering used to shed it ahead of infantry.
   */
  @Nested
  class Infrastructure {
    @Test
    void aFactoryCannotSoakTheHitThatKillsTheLastDefender() {
      final GameData gameData = bigWorld();
      final Collection<Unit> defending = infantry(gameData).create(1, germans(gameData));
      defending.addAll(factory(gameData).create(1, germans(gameData)));

      assertIdenticalUnderAlwaysHits(
          gameData,
          russians(gameData),
          germans(gameData),
          territory("France", gameData),
          infantry(gameData).create(1, russians(gameData)),
          defending);
    }

    @Test
    void factoryAndAaGunCannotSoakHitsMeantForInfantry() {
      final GameData gameData = bigWorld();
      final Collection<Unit> attacking = infantry(gameData).create(2, russians(gameData));
      attacking.addAll(armour(gameData).create(1, russians(gameData)));
      final Collection<Unit> defending = infantry(gameData).create(3, germans(gameData));
      defending.addAll(aaGun(gameData).create(1, germans(gameData)));
      defending.addAll(factory(gameData).create(1, germans(gameData)));

      assertIdenticalUnderAlwaysHits(
          gameData,
          russians(gameData),
          germans(gameData),
          territory("France", gameData),
          attacking,
          defending);
    }

    @Test
    void anAaGunFiresAtAirThenLeavesTheBattle() {
      final GameData gameData = bigWorld();
      final Collection<Unit> attacking = infantry(gameData).create(2, russians(gameData));
      attacking.addAll(fighter(gameData).create(2, russians(gameData)));
      final Collection<Unit> defending = infantry(gameData).create(2, germans(gameData));
      defending.addAll(aaGun(gameData).create(1, germans(gameData)));
      defending.addAll(factory(gameData).create(1, germans(gameData)));

      assertIdenticalUnderAlwaysHits(
          gameData,
          russians(gameData),
          germans(gameData),
          territory("France", gameData),
          attacking,
          defending);
    }

    @Test
    void seededAttackOnADefendedFactoryTerritoryMatchesTheEngine() {
      final GameData gameData = bigWorld();
      final Collection<Unit> attacking = infantry(gameData).create(3, russians(gameData));
      attacking.addAll(armour(gameData).create(2, russians(gameData)));
      attacking.addAll(fighter(gameData).create(1, russians(gameData)));
      final Collection<Unit> defending = infantry(gameData).create(4, germans(gameData));
      defending.addAll(aaGun(gameData).create(1, germans(gameData)));
      defending.addAll(factory(gameData).create(1, germans(gameData)));

      assertSeededAgreement(
          gameData,
          russians(gameData),
          germans(gameData),
          territory("France", gameData),
          attacking,
          defending,
          List.of());
    }
  }

  /**
   * Bombarding battleships fire once, in round 1, before general combat ({@code NavalBombardment});
   * with Big World's default (no bombard return fire) their casualties are cleared before they can
   * shoot back. Bombarders are not in the battle, so they are never hit and never survivors.
   */
  @Nested
  class Bombard {
    @Test
    void aBombardingBattleshipKillsADefenderBeforeItCanFire() {
      final GameData gameData = bigWorld();

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
    void bombardFiresOnlyInTheFirstRoundAndNeverHitsInfrastructure() {
      final GameData gameData = bigWorld();
      final Collection<Unit> defending = infantry(gameData).create(4, germans(gameData));
      defending.addAll(factory(gameData).create(1, germans(gameData)));
      defending.addAll(aaGun(gameData).create(1, germans(gameData)));

      assertIdenticalUnderAlwaysHits(
          gameData,
          russians(gameData),
          germans(gameData),
          territory("France", gameData),
          infantry(gameData).create(2, russians(gameData)),
          defending,
          battleship(gameData).create(1, russians(gameData)));
    }

    @Test
    void seededAmphibiousAssaultWithBombardMatchesTheEngine() {
      final GameData gameData = bigWorld();
      final Collection<Unit> attacking = infantry(gameData).create(3, russians(gameData));
      attacking.addAll(armour(gameData).create(1, russians(gameData)));
      final Collection<Unit> defending = infantry(gameData).create(4, germans(gameData));
      defending.addAll(factory(gameData).create(1, germans(gameData)));

      assertSeededAgreement(
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
   * The engine's default casualty pick spends every spare hit point of a multi-HP unit (a Big World
   * battleship) before it kills anything ({@code CasualtySelector#getDefaultCasualties}), so a
   * battleship soaks a hit that would otherwise sink an escort.
   */
  @Nested
  class TwoHitBattleships {
    @Test
    void aBattleshipTakesDamageBeforeAnEscortDies() {
      final GameData gameData = bigWorld();
      final Collection<Unit> defending = battleship(gameData).create(1, germans(gameData));
      defending.addAll(destroyer(gameData).create(2, germans(gameData)));

      assertIdenticalUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          territory("SZ 17 English Channel", gameData),
          destroyer(gameData).create(2, americans(gameData)),
          defending);
    }

    @Test
    void anAttackingBattleshipSoaksTheFirstHit() {
      final GameData gameData = bigWorld();
      final Collection<Unit> attacking = battleship(gameData).create(1, americans(gameData));
      attacking.addAll(destroyer(gameData).create(1, americans(gameData)));

      assertIdenticalUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          territory("SZ 17 English Channel", gameData),
          attacking,
          submarine(gameData).create(1, germans(gameData)));
    }

    @Test
    void seededMixedFleetBattleMatchesTheEngine() {
      final GameData gameData = bigWorld();
      final Collection<Unit> attacking = battleship(gameData).create(1, americans(gameData));
      attacking.addAll(destroyer(gameData).create(2, americans(gameData)));
      attacking.addAll(fighter(gameData).create(2, americans(gameData)));
      final Collection<Unit> defending = battleship(gameData).create(1, germans(gameData));
      defending.addAll(carrier(gameData).create(1, germans(gameData)));
      defending.addAll(fighter(gameData).create(2, germans(gameData)));
      defending.addAll(submarine(gameData).create(2, germans(gameData)));

      assertSeededAgreement(
          gameData,
          americans(gameData),
          germans(gameData),
          territory("SZ 17 English Channel", gameData),
          attacking,
          defending,
          List.of());
    }
  }

  /**
   * Big World as a running game sees it: the map sets "Two hit battleship", which the
   * initialization delegate (not run for a bare fixture) applies by giving battleships 2 HP.
   */
  private static GameData bigWorld() {
    final GameData gameData = TestMapGameData.BIG_WORLD_1942.getGameData();
    battleship(gameData).getUnitAttachment().setHitPoints(2);
    return gameData;
  }

  private static void assertIdenticalUnderAlwaysHits(
      final GameData gameData,
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending) {
    assertIdenticalUnderAlwaysHits(
        gameData, attacker, defender, location, attacking, defending, List.of());
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
   * Same-seed runs through both paths must agree on attacker win% within 4 points. A distributional
   * guard only: the two paths consume dice in different orders, so it bounds drift, not exactness.
   */
  private static void assertSeededAgreement(
      final GameData gameData,
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final Collection<Unit> bombarding) {
    final double engineWin =
        engine(
                gameData,
                attacker,
                defender,
                location,
                attacking,
                defending,
                bombarding,
                new PlainRandomSource(SEED),
                SEEDED_RUNS)
            .getAttackerWinPercent();
    final double boundedWin =
        bounded(
                attacker,
                defender,
                location,
                attacking,
                defending,
                bombarding,
                new PlainRandomSource(SEED),
                SEEDED_RUNS)
            .attackerWinPercent();

    assertThat(boundedWin).as("attacker win%").isCloseTo(engineWin, within(0.04));
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
        false,
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
                new BattleOptions(false, List.of(), List.of()));
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
