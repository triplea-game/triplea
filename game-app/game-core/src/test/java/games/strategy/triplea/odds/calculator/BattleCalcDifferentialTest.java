package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.artillery;
import static games.strategy.triplea.delegate.GameDataTestUtil.battleship;
import static games.strategy.triplea.delegate.GameDataTestUtil.carrier;
import static games.strategy.triplea.delegate.GameDataTestUtil.destroyer;
import static games.strategy.triplea.delegate.GameDataTestUtil.fighter;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.makeGameLowLuck;
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
import games.strategy.engine.data.properties.BooleanProperty;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.Constants;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
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
 * <p>The adapter and reference simulator are wired, so these cases run green and hold the bar. The
 * matrix seeds the seams the model reshapes most — casualty timing, targeting, damage migration,
 * and the per-flag rules baked from map Properties; V2-vs-V3 bombard and the broad random sweep are
 * deferred to the Phase-3 fuzzing gate.
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

  /**
   * The oracle for default-casualty-order fidelity: a mixed-unit fight with NO order-of-losses set,
   * so both paths take casualties by the engine default ({@code CasualtyOrderOfLosses}, a power/TUV
   * sort — NOT cost-ascending). Under {@code alwaysHits} the survivors on the winning side are
   * exactly whatever that order spared, so identical survivor counts by type pin that the new path
   * reproduces the default order rather than guessing. This, not {@code OolCasualtyOrderTest}'s
   * fallback case, is what owns default-order correctness.
   */
  @Test
  void alwaysHitsWithMixedUnitTypesAndNoOolAgreesOnSurvivorsByType() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    final Territory germany = territory("Germany", gameData);
    final Collection<Unit> attacking = infantry(gameData).create(4, russians(gameData));
    attacking.addAll(armour(gameData).create(2, russians(gameData)));

    assertIdenticalSurvivorsUnderAlwaysHits(
        gameData,
        russians(gameData),
        germans(gameData),
        germany,
        attacking,
        infantry(gameData).create(3, germans(gameData)));
  }

  /**
   * The low-luck + non-standard-dice oracle: with low luck on and eight-sided dice, {@code
   * alwaysHits} still exercises the low-luck arithmetic (guaranteed hits are {@code power /
   * diceSides} plus a remainder die that the always-0 source resolves deterministically), so
   * survivors stay exactly comparable. A one-round wipe isolates the dice model — the attacker
   * removes all four defenders whatever the dice, so the attacker's survivor count is driven purely
   * by the defender's low-luck return fire (power 8 → one guaranteed hit at eight sides, versus two
   * at six sides, versus four under all-hit normal dice). Identical survivors therefore pin that
   * the new path reads both {@code lowLuck} and {@code diceSides} off the scenario rather than
   * baking in normal six-sided dice.
   */
  @Test
  void alwaysHitsUnderLowLuckAndEightSidedDiceMatchesTheEngine() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    makeGameLowLuck(gameData);
    gameData.setDiceSides(8);
    final Territory germany = territory("Germany", gameData);

    assertIdenticalSurvivorsUnderAlwaysHits(
        gameData,
        russians(gameData),
        germans(gameData),
        germany,
        infantry(gameData).create(30, russians(gameData)),
        infantry(gameData).create(4, germans(gameData)));
  }

  /**
   * Support fidelity in the one regime where a strength bonus is observable: low luck plus {@code
   * alwaysHits}. Normal all-hit dice saturate strength (everything hits), but low luck derives its
   * guaranteed hits from summed power, so artillery support raising {@code floor(power /
   * diceSides)} changes the result. Four artillery lend +1 attack to four infantry; with the bonus
   * the attacker clears the three defenders in one round, without it the fight runs a second round
   * and costs an extra attacker — so identical survivors pin that the adapter bakes the artillery
   * support the engine applies. If the support list were still empty, the new path would lose the
   * extra hit and diverge here.
   */
  @Test
  void alwaysHitsUnderLowLuckWithArtillerySupportMatchesTheEngine() {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    makeGameLowLuck(gameData);
    final Territory germany = territory("Germany", gameData);
    final Collection<Unit> attacking = artillery(gameData).create(4, russians(gameData));
    attacking.addAll(infantry(gameData).create(4, russians(gameData)));

    assertIdenticalSurvivorsUnderAlwaysHits(
        gameData,
        russians(gameData),
        germans(gameData),
        germany,
        attacking,
        infantry(gameData).create(3, germans(gameData)));
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
                2000)
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
    // Separate same-seed source per side: the oracle exhausts its 2000 runs before the new path
    // starts, so a shared instance would diverge — same seed keeps both sequences identical.
    final double newWinPercent =
        new ReferenceBattleSimulator()
            .simulate(scenario, 2000, new EngineRandomSource(new PlainRandomSource(SEED)))
            .attackerWinPercent();

    // Distributional guard, not an exact oracle — exact fidelity is owned by the alwaysHits cases.
    // within(0.1) (10 percentage points) was slack enough to pass a badly-wrong impl; 2000 runs
    // shrink the sampling spread enough to hold ~3 points, at the cost of 2000 engine clones here.
    assertThat(newWinPercent).isCloseTo(oracleWinPercent, within(0.03));
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

    /**
     * Submerge as a retreat migration: defending subs facing pure air with no destroyer evade under
     * water and survive, where naive per-unit iteration would let the planes grind them out. The
     * submerged subs must show up as defender survivors, matching the engine.
     */
    @Test
    void subsSubmergeAgainstPureAirMatchesTheEngine() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory seaZone = territory("1 Sea Zone", gameData);

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          fighter(gameData).create(2, americans(gameData)),
          submarine(gameData).create(2, germans(gameData)));
    }

    /**
     * The submerge property gate: with {@code Submersible Subs} off (and no defending-submerge
     * variant), defending subs facing pure air cannot dive — they neither submerge nor can be hit
     * by the air, so both sides remain exactly as the engine leaves them. Contrast {@link
     * #subsSubmergeAgainstPureAirMatchesTheEngine}, which runs with the Revised default on.
     */
    @Test
    void subsCannotSubmergeAgainstPureAirWhenSubmersibleSubsIsOff() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      setBooleanProperty(gameData, Constants.SUBMERSIBLE_SUBS, false);
      final Territory seaZone = territory("1 Sea Zone", gameData);

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          fighter(gameData).create(2, americans(gameData)),
          submarine(gameData).create(2, germans(gameData)));
    }

    /**
     * WW2V2 gives a defending sub a sneak attack: with no attacking destroyer, the defending sub
     * fires in the sub phase and sinks the lone attacking carrier before it returns fire, so the
     * sub survives untouched. REVISED runs WW2V2 on by default.
     */
    @Test
    void ww2v2GivesADefendingSubASneakAttack() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory seaZone = territory("1 Sea Zone", gameData);

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          carrier(gameData).create(1, americans(gameData)),
          submarine(gameData).create(1, germans(gameData)));
    }

    /**
     * The defendingSubsSneakAttack rule grants the same sneak with WW2V2 off: the defending sub
     * still opens fire in the sub phase and survives. Contrast {@link
     * #withoutTheSneakRulesADefendingSubFiresInMainCombat}, the identical fight with neither rule.
     */
    @Test
    void defendingSubsSneakAttackGivesADefendingSubASneakWithoutWw2v2() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      setBooleanProperty(gameData, Constants.WW2V2, false);
      setBooleanProperty(gameData, Constants.DEFENDING_SUBS_SNEAK_ATTACK, true);
      final Territory seaZone = territory("1 Sea Zone", gameData);

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          carrier(gameData).create(1, americans(gameData)),
          submarine(gameData).create(1, germans(gameData)));
    }

    /**
     * With neither WW2V2 nor defendingSubsSneakAttack, a defending sub has no sneak — it fires in
     * main combat, trading simultaneously with the carrier, so the carrier takes the sub down with
     * it and neither survives. This is the live-bug pin: a defender-blind first-strike rule would
     * wrongly let the sub open fire and spare it.
     */
    @Test
    void withoutTheSneakRulesADefendingSubFiresInMainCombat() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      setBooleanProperty(gameData, Constants.WW2V2, false);
      setBooleanProperty(gameData, Constants.DEFENDING_SUBS_SNEAK_ATTACK, false);
      final Territory seaZone = territory("1 Sea Zone", gameData);

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          carrier(gameData).create(1, americans(gameData)),
          submarine(gameData).create(1, germans(gameData)));
    }

    /**
     * The known ww2v2 waiting-to-die gap ({@code ReferenceCombatRelations#firstStrikeNegated}):
     * under ww2v2 a destroyer-pinned first striker still fires in the sub phase and trades before
     * dying, but the sim defers it to main. Attacker one sub, defender one sub plus a destroyer —
     * the engine leaves the defender its destroyer alone (the defending sub traded with the
     * attacking sub in the sub phase), while the sim spares both defenders because the attacker
     * sub, pinned to main, is killed before it fires. Disabled until phase-2b models waiting-to-die
     * casualties in {@code fightRound}; kept so the divergence cannot silently change.
     */
    @Test
    @Disabled("phase-2b: ww2v2 waiting-to-die, see ReferenceCombatRelations")
    void ww2v2DestroyerPinnedFirstStrikeStillTradesInTheSubPhase() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory seaZone = territory("1 Sea Zone", gameData);
      final Collection<Unit> defending =
          new ArrayList<>(submarine(gameData).create(1, germans(gameData)));
      defending.addAll(destroyer(gameData).create(1, germans(gameData)));

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          submarine(gameData).create(1, americans(gameData)),
          defending);
    }

    /**
     * The named §9 multi-HP + first-strike seam, composed in one fight and exercised nowhere else
     * in the differential: a 2-hit battleship alongside a first-strike submarine, against subs that
     * also fire first strike. Multi-hit concentration (which hit damages vs sinks the battleship)
     * and first-strike timing (opening fire off live counts) must both match the engine survivor
     * counts.
     */
    @Test
    void multiHitBattleshipWithAFirstStrikeSubMatchesTheEngine() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory seaZone = territory("1 Sea Zone", gameData);

      final Collection<Unit> attacking = battleship(gameData).create(1, americans(gameData));
      attacking.addAll(submarine(gameData).create(1, americans(gameData)));

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          attacking,
          submarine(gameData).create(2, germans(gameData)));
    }

    /**
     * The transport cargo cascade under alwaysHits: two subs sink a transport carrying two
     * infantry, so the cargo — a non-combatant in the sea battle — must be gone from the defender
     * exactly as the engine drops it. Were the cargo left unflagged it would fire back and change
     * the survivors, so identical counts pin that the adapter marks it dependent and the allocator
     * sinks it with its transport.
     */
    @Test
    void transportCargoSinksWithItsTransportMatchingTheEngine() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory seaZone = territory("1 Sea Zone", gameData);
      final List<Unit> transportUnit = transport(gameData).create(1, germans(gameData));
      final Collection<Unit> cargo = infantry(gameData).create(2, germans(gameData));
      cargo.forEach(unit -> unit.setTransportedBy(transportUnit.get(0)));
      final Collection<Unit> defending = new ArrayList<>(transportUnit);
      defending.addAll(cargo);

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          submarine(gameData).create(2, americans(gameData)),
          defending);
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

  // An editable rule flag is read from the editable-property store before the map that set(String,
  // Object) writes, so it must be flipped in place; a non-editable flag lives only in that map, so
  // it falls through to set(). This override handles either kind.
  private static void setBooleanProperty(
      final GameData gameData, final String propertyName, final boolean value) {
    for (final IEditableProperty<?> property : gameData.getProperties().getEditableProperties()) {
      if (property.getName().equals(propertyName)) {
        ((BooleanProperty) property).setValue(value);
        return;
      }
    }
    gameData.getProperties().set(propertyName, value);
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
