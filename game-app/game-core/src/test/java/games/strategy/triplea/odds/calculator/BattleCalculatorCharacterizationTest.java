package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.delegate.GameDataTestUtil.aaGun;
import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.artillery;
import static games.strategy.triplea.delegate.GameDataTestUtil.bomber;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
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
import games.strategy.engine.data.UnitType;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.util.TuvCostsCalculator;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Black-box characterization of the odds {@link BattleCalculator}: it pins the observable behavior
 * of the {@code calculate(...) -> AggregateResults} seam so a future overhaul of how units are
 * represented and battles are computed can be checked against the current engine.
 *
 * <p>The suite is a deliberate blend:
 *
 * <ul>
 *   <li><b>Forced outcomes</b> use a {@link ScriptedRandomSource} so the result is determined by the
 *       rules, not the dice, and can be asserted exactly (win/lose/draw, survivors, rounds).
 *   <li><b>Probabilistic characterization</b> uses a seeded {@link PlainRandomSource} so win-rate
 *       bands are reproducible rather than flaky. These assert the odds are <em>about</em> right,
 *       not to the last digit, so they survive the dice-draw-order changes an overhaul will bring.
 *   <li><b>Invariants</b> (probabilities summing to one, monotonicity, reproducibility) must hold
 *       for any correct implementation and make strong regression/mutation detectors.
 * </ul>
 */
class BattleCalculatorCharacterizationTest extends AbstractClientSettingTestCase {

  /** Fixed so every seeded run in this suite is reproducible from build to build. */
  private static final long SEED = 20260904L;

  private static BattleCalculator seededCalculator(final GameData data) {
    final BattleCalculator calculator = new BattleCalculator(data);
    calculator.setRandomSource(new PlainRandomSource(SEED));
    return calculator;
  }

  private static BattleCalculator scriptedCalculator(
      final GameData data, final IRandomSource diceSource) {
    final BattleCalculator calculator = new BattleCalculator(data);
    calculator.setRandomSource(diceSource);
    return calculator;
  }

  /**
   * Runs the simulation with the ceremony every production caller shares: no bombarding units, the
   * location's own territory effects, and no "retreat when only air is left". The units, players,
   * location, run count, and the calculator's own configuration stay visible in each test.
   */
  private static AggregateResults fight(
      final BattleCalculator calculator,
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final int runCount) {
    return calculator.calculate(
        attacker,
        defender,
        location,
        attacking,
        defending,
        List.of(),
        TerritoryEffectHelper.getEffects(location),
        false,
        runCount);
  }

  private static AggregateResults fight(
      final BattleCalculator calculator,
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final boolean retreatWhenOnlyAirLeft,
      final int runCount) {
    return calculator.calculate(
        attacker,
        defender,
        location,
        attacking,
        defending,
        List.of(),
        TerritoryEffectHelper.getEffects(location),
        retreatWhenOnlyAirLeft,
        runCount);
  }

  private static List<String> unitTypeNames(final Collection<Unit> units) {
    return units.stream().map(Unit::getType).map(UnitType::getName).toList();
  }

  @Nested
  class ForcedOutcomes {
    @Test
    void equalForcesAnnihilateEachOtherIntoADrawWhenEveryShotHits() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final List<Unit> attackers = infantry(gameData).create(1, russians(gameData));
      final List<Unit> defenders = infantry(gameData).create(1, germans(gameData));

      final AggregateResults results =
          fight(
              scriptedCalculator(gameData, ScriptedRandomSource.alwaysHits()),
              russians(gameData),
              germans(gameData),
              germany,
              attackers,
              defenders,
              1);

      assertThat(results.getDrawPercent()).isEqualTo(1.0);
      assertThat(results.getAttackerWinPercent()).isEqualTo(0.0);
      assertThat(results.getDefenderWinPercent()).isEqualTo(0.0);
      assertThat(results.getAverageAttackingUnitsLeft()).isEqualTo(0.0);
      assertThat(results.getAverageDefendingUnitsLeft()).isEqualTo(0.0);
      assertThat(results.getAverageBattleRoundsFought()).isEqualTo(1.0);
    }

    @Test
    void attackerOutnumberingThreeToTwoWinsWithOneSurvivorWhenEveryShotHits() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final List<Unit> attackers = infantry(gameData).create(3, russians(gameData));
      final List<Unit> defenders = infantry(gameData).create(2, germans(gameData));

      final AggregateResults results =
          fight(
              scriptedCalculator(gameData, ScriptedRandomSource.alwaysHits()),
              russians(gameData),
              germans(gameData),
              germany,
              attackers,
              defenders,
              1);

      assertThat(results.getAttackerWinPercent()).isEqualTo(1.0);
      assertThat(results.getAverageAttackingUnitsLeft()).isEqualTo(1.0);
      assertThat(results.getAverageDefendingUnitsLeft()).isEqualTo(0.0);
      assertThat(results.getAverageBattleRoundsFought()).isEqualTo(1.0);
    }

    @Test
    void defenderOutnumberingThreeToTwoWinsWithOneSurvivorWhenEveryShotHits() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final List<Unit> attackers = infantry(gameData).create(2, russians(gameData));
      final List<Unit> defenders = infantry(gameData).create(3, germans(gameData));

      final AggregateResults results =
          fight(
              scriptedCalculator(gameData, ScriptedRandomSource.alwaysHits()),
              russians(gameData),
              germans(gameData),
              germany,
              attackers,
              defenders,
              1);

      assertThat(results.getDefenderWinPercent()).isEqualTo(1.0);
      assertThat(results.getAverageDefendingUnitsLeft()).isEqualTo(1.0);
      assertThat(results.getAverageAttackingUnitsLeft()).isEqualTo(0.0);
    }

    @Test
    void attackingTransportsCannotFireSoTheAttackerAlwaysLoses() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory seaZone = territory("1 Sea Zone", gameData);
      final List<Unit> attackers = transport(gameData).create(2, americans(gameData));
      final List<Unit> defenders = submarine(gameData).create(2, germans(gameData));

      final AggregateResults results =
          fight(
              scriptedCalculator(gameData, ScriptedRandomSource.alwaysHits()),
              americans(gameData),
              germans(gameData),
              seaZone,
              attackers,
              defenders,
              1);

      assertThat(results.getAttackerWinPercent()).isEqualTo(0.0);
      assertThat(results.getDefenderWinPercent()).isEqualTo(1.0);
    }

    @Test
    void defenselessTransportIsDestroyedSoTheAttackerAlwaysWins() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory seaZone = territory("1 Sea Zone", gameData);
      final List<Unit> attackers = submarine(gameData).create(1, americans(gameData));
      final List<Unit> defenders = transport(gameData).create(1, germans(gameData));

      final AggregateResults results =
          fight(
              scriptedCalculator(gameData, ScriptedRandomSource.alwaysHits()),
              americans(gameData),
              germans(gameData),
              seaZone,
              attackers,
              defenders,
              1);

      assertThat(results.getAttackerWinPercent()).isEqualTo(1.0);
      assertThat(results.getAverageAttackingUnitsLeft()).isEqualTo(1.0);
      assertThat(results.getAverageDefendingUnitsLeft()).isEqualTo(0.0);
    }
  }

  @Nested
  class OrderOfLossesRespected {
    @Test
    void cheapUnitIsTakenAsCasualtyFirstWhenOrderOfLossListsItFirst() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final List<Unit> attackers = infantry(gameData).create(1, russians(gameData));
      attackers.addAll(armour(gameData).create(1, russians(gameData)));
      final List<Unit> defenders = infantry(gameData).create(1, germans(gameData));

      final BattleCalculator calculator =
          scriptedCalculator(gameData, ScriptedRandomSource.alwaysHits());
      calculator.setAttackerOrderOfLosses("1^infantry");
      final AggregateResults results =
          fight(calculator, russians(gameData), germans(gameData), germany, attackers, defenders, 1);

      // Every shot hits: the lone defender dies and inflicts exactly one attacker casualty, which
      // the order of losses directs onto the infantry, leaving the armour standing.
      assertThat(results.getAttackerWinPercent()).isEqualTo(1.0);
      assertThat(unitTypeNames(results.getResults().get(0).getRemainingAttackingUnits()))
          .containsExactly("armour");
    }

    @Test
    void expensiveUnitIsTakenAsCasualtyFirstWhenOrderOfLossListsItFirst() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final List<Unit> attackers = infantry(gameData).create(1, russians(gameData));
      attackers.addAll(armour(gameData).create(1, russians(gameData)));
      final List<Unit> defenders = infantry(gameData).create(1, germans(gameData));

      final BattleCalculator calculator =
          scriptedCalculator(gameData, ScriptedRandomSource.alwaysHits());
      calculator.setAttackerOrderOfLosses("1^armour");
      final AggregateResults results =
          fight(calculator, russians(gameData), germans(gameData), germany, attackers, defenders, 1);

      assertThat(results.getAttackerWinPercent()).isEqualTo(1.0);
      assertThat(unitTypeNames(results.getResults().get(0).getRemainingAttackingUnits()))
          .containsExactly("infantry");
    }

    @Test
    void firstListedTypeAcrossMultipleSectionsIsTakenFirst() {
      // Two sections with a single casualty: only the *first* listed type should die, so the
      // relative order of the sections is what is being pinned here.
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final List<Unit> attackers = armour(gameData).create(1, russians(gameData));
      attackers.addAll(infantry(gameData).create(1, russians(gameData)));
      attackers.addAll(artillery(gameData).create(1, russians(gameData)));
      final List<Unit> defenders = infantry(gameData).create(1, germans(gameData));

      final BattleCalculator calculator =
          scriptedCalculator(gameData, ScriptedRandomSource.alwaysHits());
      calculator.setAttackerOrderOfLosses("1^armour;1^infantry;1^artillery");
      final AggregateResults results =
          fight(calculator, russians(gameData), germans(gameData), germany, attackers, defenders, 1);

      assertThat(results.getAttackerWinPercent()).isEqualTo(1.0);
      assertThat(unitTypeNames(results.getResults().get(0).getRemainingAttackingUnits()))
          .containsExactlyInAnyOrder("infantry", "artillery");
    }

    @Test
    void casualtiesBeyondTheOrderOfLossListFallBackToDefaultSelection() {
      // The order of losses names only one unit but two casualties are taken; the second must fall
      // back to a default pick, so two hits still remove two units (not one).
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final List<Unit> attackers = infantry(gameData).create(1, russians(gameData));
      attackers.addAll(armour(gameData).create(1, russians(gameData)));
      attackers.addAll(artillery(gameData).create(1, russians(gameData)));
      final List<Unit> defenders = infantry(gameData).create(2, germans(gameData));

      final BattleCalculator calculator =
          scriptedCalculator(gameData, ScriptedRandomSource.alwaysHits());
      calculator.setAttackerOrderOfLosses("1^infantry");
      final AggregateResults results =
          fight(calculator, russians(gameData), germans(gameData), germany, attackers, defenders, 1);

      assertThat(results.getAttackerWinPercent()).isEqualTo(1.0);
      assertThat(results.getResults().get(0).getRemainingAttackingUnits()).hasSize(1);
      assertThat(unitTypeNames(results.getResults().get(0).getRemainingAttackingUnits()))
          .doesNotContain("infantry");
    }
  }

  @Nested
  class WinProbabilities {
    @Test
    void overwhelmingAttackerWinsNearlyAlways() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final List<Unit> attackers = infantry(gameData).create(100, russians(gameData));
      final List<Unit> defenders = new ArrayList<>(territory("Germany", gameData).getUnits());

      final AggregateResults results =
          fight(
              seededCalculator(gameData),
              russians(gameData),
              germans(gameData),
              germany,
              attackers,
              defenders,
              200);

      assertThat(results.getAttackerWinPercent()).isGreaterThan(0.99);
      assertThat(results.getDefenderWinPercent()).isLessThan(0.01);
    }

    @Test
    void probabilitiesFormAValidDistributionSummingToOne() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final List<Unit> attackers = infantry(gameData).create(5, russians(gameData));
      final List<Unit> defenders = infantry(gameData).create(5, germans(gameData));

      final AggregateResults results =
          fight(
              seededCalculator(gameData),
              russians(gameData),
              germans(gameData),
              germany,
              attackers,
              defenders,
              500);

      assertThat(
              results.getAttackerWinPercent()
                  + results.getDefenderWinPercent()
                  + results.getDrawPercent())
          .isEqualTo(1.0, within(1.0e-9));
      assertThat(results.getRollCount()).isEqualTo(500);
    }
  }

  @Nested
  class Invariants {
    @Test
    void keepingOneAttackingLandUnitLowersTheAttackerWinPercent() {
      // Air cannot capture territory, so requiring a surviving land unit costs the attacker the
      // games where the infantry dies but the bomber would otherwise have carried the day. The
      // flag only bites when air and land attack together, hence the mixed force.
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory easternCanada = territory("Eastern Canada", gameData);
      final List<Unit> defenders = fighter(gameData).create(1, british(gameData));

      final AggregateResults freeToLose =
          fight(
              seededCalculator(gameData),
              germans(gameData),
              british(gameData),
              easternCanada,
              landAndAir(gameData),
              new ArrayList<>(defenders),
              1000);

      final BattleCalculator mustKeepOne = seededCalculator(gameData);
      mustKeepOne.setKeepOneAttackingLandUnit(true);
      final AggregateResults keepingOne =
          fight(
              mustKeepOne,
              germans(gameData),
              british(gameData),
              easternCanada,
              landAndAir(gameData),
              new ArrayList<>(defenders),
              1000);

      assertThat(keepingOne.getAttackerWinPercent())
          .isLessThan(freeToLose.getAttackerWinPercent());
    }

    private List<Unit> landAndAir(final GameData gameData) {
      final List<Unit> attackers = infantry(gameData).create(1, germans(gameData));
      attackers.addAll(bomber(gameData).create(1, germans(gameData)));
      return attackers;
    }

    @Test
    void addingAttackersNeverLowersTheAttackerWinPercent() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final List<Unit> defenders = infantry(gameData).create(5, germans(gameData));

      final AggregateResults fourAttackers =
          fight(
              seededCalculator(gameData),
              russians(gameData),
              germans(gameData),
              germany,
              infantry(gameData).create(4, russians(gameData)),
              new ArrayList<>(defenders),
              500);
      final AggregateResults eightAttackers =
          fight(
              seededCalculator(gameData),
              russians(gameData),
              germans(gameData),
              germany,
              infantry(gameData).create(8, russians(gameData)),
              new ArrayList<>(defenders),
              500);

      assertThat(eightAttackers.getAttackerWinPercent())
          .isGreaterThan(fourAttackers.getAttackerWinPercent());
    }

    @Test
    void averageTuvSwingIsStronglyPositiveWhenTheAttackerDominates() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final List<Unit> attackers = infantry(gameData).create(100, russians(gameData));
      final List<Unit> defenders = new ArrayList<>(territory("Germany", gameData).getUnits());

      final AggregateResults results =
          fight(
              seededCalculator(gameData),
              russians(gameData),
              germans(gameData),
              germany,
              attackers,
              defenders,
              200);

      // The attacker crushes the defender, so on average the defender loses far more unit value.
      assertThat(
              results.getAverageTuvSwing(
                  russians(gameData), attackers, germans(gameData), defenders, gameData))
          .isGreaterThan(0.0);
    }

    @Test
    void averageTuvSwingEqualsTheValueDestroyedInAOneSidedBattle() {
      // A submarine wipes out a defenseless transport and survives untouched, so the swing is
      // exactly the transport's unit value: no attacker value was lost.
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory seaZone = territory("1 Sea Zone", gameData);
      final List<Unit> attackers = submarine(gameData).create(1, americans(gameData));
      final List<Unit> defenders = transport(gameData).create(1, germans(gameData));

      final AggregateResults results =
          fight(
              scriptedCalculator(gameData, ScriptedRandomSource.alwaysHits()),
              americans(gameData),
              germans(gameData),
              seaZone,
              attackers,
              defenders,
              1);

      final int destroyedTransportValue =
          TuvUtils.getTuv(
              defenders, new TuvCostsCalculator().getCostsForTuv(germans(gameData)));
      assertThat(
              results.getAverageTuvSwing(
                  americans(gameData), attackers, germans(gameData), defenders, gameData))
          .isEqualTo((double) destroyedTransportValue);
    }

    @Test
    void retreatingWhenReducedToEightUnitsLowersTheAttackerWinPercent() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final List<Unit> defenders = infantry(gameData).create(10, germans(gameData));

      final AggregateResults fightToTheDeath =
          fight(
              seededCalculator(gameData),
              russians(gameData),
              germans(gameData),
              germany,
              infantry(gameData).create(10, russians(gameData)),
              new ArrayList<>(defenders),
              300);

      final BattleCalculator retreater = seededCalculator(gameData);
      retreater.setRetreatAfterXUnitsLeft(8);
      final AggregateResults retreatingEarly =
          fight(
              retreater,
              russians(gameData),
              germans(gameData),
              germany,
              infantry(gameData).create(10, russians(gameData)),
              new ArrayList<>(defenders),
              300);

      assertThat(retreatingEarly.getAttackerWinPercent())
          .isLessThan(fightToTheDeath.getAttackerWinPercent());
    }

    @Test
    void retreatingWhenOnlyAirIsLeftChangesTheAttackerOutcome() {
      // A lone infantry backed by fighters against a strong defender: once the infantry dies the
      // attacker has only air left, which cannot capture, so "retreat when only air is left" pulls
      // the fighters out instead of throwing them away.
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final List<Unit> defenders = infantry(gameData).create(6, germans(gameData));

      final AggregateResults noRetreat =
          fight(
              seededCalculator(gameData),
              russians(gameData),
              germans(gameData),
              germany,
              airHeavyAttack(gameData),
              new ArrayList<>(defenders),
              false,
              300);

      final AggregateResults retreatAir =
          fight(
              seededCalculator(gameData),
              russians(gameData),
              germans(gameData),
              germany,
              airHeavyAttack(gameData),
              new ArrayList<>(defenders),
              true,
              300);

      assertThat(retreatAir.getAverageTuvSwing(
              russians(gameData), airHeavyAttack(gameData), germans(gameData), defenders, gameData))
          .isGreaterThan(
              noRetreat.getAverageTuvSwing(
                  russians(gameData),
                  airHeavyAttack(gameData),
                  germans(gameData),
                  defenders,
                  gameData));
    }

    private List<Unit> airHeavyAttack(final GameData gameData) {
      final List<Unit> attackers = infantry(gameData).create(1, russians(gameData));
      attackers.addAll(fighter(gameData).create(3, russians(gameData)));
      return attackers;
    }

    @Test
    void retreatingAfterTheFirstRoundBoundsTheRoundsFought() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final List<Unit> attackers = infantry(gameData).create(10, russians(gameData));
      final List<Unit> defenders = infantry(gameData).create(10, germans(gameData));

      final BattleCalculator calculator = seededCalculator(gameData);
      calculator.setRetreatAfterRound(1);
      final AggregateResults results =
          fight(
              calculator, russians(gameData), germans(gameData), germany, attackers, defenders, 200);

      assertThat(results.getAverageBattleRoundsFought()).isLessThanOrEqualTo(1.0);
    }

    @Test
    void sameSeedProducesIdenticalAggregateResults() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final List<Unit> defenders = infantry(gameData).create(5, germans(gameData));

      final AggregateResults first =
          fight(
              seededCalculator(gameData),
              russians(gameData),
              germans(gameData),
              germany,
              infantry(gameData).create(5, russians(gameData)),
              new ArrayList<>(defenders),
              300);
      final AggregateResults second =
          fight(
              seededCalculator(gameData),
              russians(gameData),
              germans(gameData),
              germany,
              infantry(gameData).create(5, russians(gameData)),
              new ArrayList<>(defenders),
              300);

      assertThat(second.getAttackerWinPercent()).isEqualTo(first.getAttackerWinPercent());
      assertThat(second.getDefenderWinPercent()).isEqualTo(first.getDefenderWinPercent());
      assertThat(second.getAverageAttackingUnitsLeft())
          .isEqualTo(first.getAverageAttackingUnitsLeft());
    }
  }

  @Nested
  class SpecialUnits {
    @Test
    void antiAircraftFireCanDestroyAttackingAircraft() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final List<Unit> attackers = fighter(gameData).create(3, russians(gameData));
      final List<Unit> defenders = aaGun(gameData).create(1, germans(gameData));
      defenders.addAll(infantry(gameData).create(1, germans(gameData)));

      final AggregateResults results =
          fight(
              seededCalculator(gameData),
              russians(gameData),
              germans(gameData),
              germany,
              attackers,
              defenders,
              200);

      // Some fighters are shot down before the general engagement, so on average fewer than all
      // three survive even across won and lost battles.
      assertThat(results.getAverageAttackingUnitsLeft()).isLessThan(3.0);
    }
  }
}
