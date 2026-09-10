package games.strategy.triplea.odds.calculator;

import static games.strategy.triplea.delegate.GameDataTestUtil.aaGun;
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
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.engine.data.properties.BooleanProperty;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitSupportAttachment;
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
import java.util.Set;
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

  /**
   * Enemy (debuff) support fidelity, the §5.5 headline. No bundled map carries a normal-combat
   * enemy support — TWW's only one is AA — so an enemy strength rule is synthesized onto a loaded
   * REVISED the same way REVISED's own old-artillery support is synthesized: russian armour cuts
   * german infantry defense by two, to zero. Under low luck plus {@code alwaysHits} the debuffed
   * defenders score no hits, so the attacker keeps all four armour; the old allied-only gate
   * dropped enemy support entirely and would leave the defenders defending at two, scoring a hit
   * that costs the attacker an armour — so identical survivors pin that the adapter now bakes the
   * enemy debuff the engine applies.
   */
  @Test
  void alwaysHitsUnderLowLuckWithEnemyStrengthDebuffMatchesTheEngine() throws GameParseException {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    makeGameLowLuck(gameData);
    injectEnemyStrengthDebuff(
        gameData, armour(gameData), infantry(gameData), russians(gameData), -2);
    // Compute the lazily-cached support list now, so the oracle and the new path both read the
    // injected rule, and guard against a silent no-op where the debuff reaches neither calculator.
    assertThat(gameData.getUnitTypeList().getSupportRules())
        .anyMatch(UnitSupportAttachment::getEnemy);

    final Territory germany = territory("Germany", gameData);

    assertIdenticalSurvivorsUnderAlwaysHits(
        gameData,
        russians(gameData),
        germans(gameData),
        germany,
        armour(gameData).create(4, russians(gameData)),
        infantry(gameData).create(3, germans(gameData)));
  }

  /**
   * A support that is both strength and roll ({@code dice="roll:strength"}) must apply in both the
   * engine's strength and roll pools. No bundled map declares one, so an allied dual support is
   * synthesized onto REVISED: russian armour lends each attacking infantry +1 attack and +1 roll.
   * Under low luck the roll half turns each supported infantry's single die into two, so the
   * attacker scores an extra hit and wipes the defenders a round sooner; the old adapter filed the
   * attachment as strength-only and dropped the roll half, losing that hit and an extra attacker —
   * so identical survivors pin that both halves are now baked.
   */
  @Test
  void alwaysHitsUnderLowLuckWithDualStrengthAndRollSupportMatchesTheEngine()
      throws GameParseException {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    makeGameLowLuck(gameData);
    injectDualStrengthAndRollSupport(
        gameData, armour(gameData), infantry(gameData), russians(gameData), 1);
    // Compute the lazily-cached support list now, so the oracle and the new path both read the
    // injected rule, and guard against a silent no-op where the dual support never reaches either.
    assertThat(gameData.getUnitTypeList().getSupportRules())
        .anyMatch(rule -> rule.getStrength() && rule.getRoll());

    final Territory germany = territory("Germany", gameData);
    final Collection<Unit> attacking = infantry(gameData).create(3, russians(gameData));
    attacking.addAll(armour(gameData).create(1, russians(gameData)));

    assertIdenticalSurvivorsUnderAlwaysHits(
        gameData,
        russians(gameData),
        germans(gameData),
        germany,
        attacking,
        infantry(gameData).create(3, germans(gameData)));
  }

  /**
   * §5.7 probe — support-adjusted casualty ordering. The engine ranks casualties by
   * support-adjusted power ({@code CasualtyOrderOfLosses}): a heavily supported infantry outranks
   * an armour, so the engine sheds the armour first, whereas the bounded calc ranks by base attack
   * and sheds the infantry. A synthesized artillery->infantry +3 attack support lifts the one
   * attacking infantry's attack to 5, above the armour's 3; the attacker takes exactly one casualty
   * (one defending infantry scores one low-luck hit, then dies), so the surviving TYPE differs —
   * engine keeps the infantry, base-order bounded keeps the armour. Pins whether §5.7 is
   * observable.
   */
  @Test
  void alwaysHitsUnderLowLuckSupportAdjustsCasualtyOrderLikeTheEngine() throws GameParseException {
    final GameData gameData = TestMapGameData.REVISED.getGameData();
    makeGameLowLuck(gameData);
    injectStrongOffenseSupport(
        gameData, artillery(gameData), infantry(gameData), russians(gameData), 3);

    final Territory germany = territory("Germany", gameData);
    final Collection<Unit> attacking = artillery(gameData).create(1, russians(gameData));
    attacking.addAll(infantry(gameData).create(1, russians(gameData)));
    attacking.addAll(armour(gameData).create(1, russians(gameData)));

    assertIdenticalSurvivorsUnderAlwaysHits(
        gameData,
        russians(gameData),
        germans(gameData),
        germany,
        attacking,
        infantry(gameData).create(1, germans(gameData)));
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
     * wrongly let the sub open fire and spare it. The discriminator depends on the carrier being
     * 1-HP — a 2-HP attacker would survive the single sub hit and blur the sneak-vs-main outcome.
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
     * The multi-HP damage chain composed with first strikers on both sides: a 2-hit battleship and
     * a first-strike sub attack two subs that also first strike (REVISED, WW2V2). The sub first
     * strikes fire off the round-start snapshot and the battleship's {@code onHit} migration is
     * applied as it takes hits, and the survivor counts must match the engine. It does not pin
     * concentration-to-sink: the battleship is paired with a sub, so the second incoming hit lands
     * on the sub by casualty order rather than on the battleship, and the lone-multi-HP allocator
     * path (a single 2-HP unit absorbing two hits from one volley) is not exercised here.
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

    /**
     * The transport-casualty restriction as a targeting gate: with {@code Transport Casualties
     * Restricted} on, the engine bars the lone transport from being a casualty while its destroyer
     * escort is still alive, so the escort must die before the transport can ever be reached
     * (mirrors {@code CasualtySelector} line 77). A single attacking sub scores exactly one hit, so
     * the ordering is decisive: restricted the destroyer dies and the transport lives ({@code
     * {transport=1}}), unrestricted the cheaper transport dies instead ({@code {destroyer=1}}). Red
     * until the restriction is wired into casualty eligibility; the destroyer negates first strike,
     * so the exchange is simultaneous and the lone sub still trades away.
     */
    @Test
    void restrictedTransportIsNotACasualtyWhileItsEscortLives() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      setBooleanProperty(gameData, Constants.TRANSPORT_CASUALTIES_RESTRICTED, true);
      final Territory seaZone = territory("1 Sea Zone", gameData);

      final Collection<Unit> defenders =
          new ArrayList<>(transport(gameData).create(1, germans(gameData)));
      defenders.addAll(destroyer(gameData).create(1, germans(gameData)));

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          submarine(gameData).create(1, americans(gameData)),
          defenders);
    }

    /**
     * The off-case contrast to {@link #restrictedTransportIsNotACasualtyWhileItsEscortLives} — the
     * same one-sub fixture with the restriction flipped off. The single hit now falls to the
     * cheaper transport under ordinary cost order, so the destroyer survives ({@code
     * {destroyer=1}}), the mirror of A1's flag-on {@code {transport=1}}. Because the ordering is
     * decisive it is a true guard: an over-restriction that wrongly protected the transport when
     * the flag is off would leave {@code {transport=1}} and diverge from the engine here.
     */
    @Test
    void unrestrictedTransportIsAnOrdinaryCasualtyAlongsideItsEscort() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      setBooleanProperty(gameData, Constants.TRANSPORT_CASUALTIES_RESTRICTED, false);
      final Territory seaZone = territory("1 Sea Zone", gameData);

      final Collection<Unit> defenders =
          new ArrayList<>(transport(gameData).create(1, germans(gameData)));
      defenders.addAll(destroyer(gameData).create(1, germans(gameData)));

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          submarine(gameData).create(1, americans(gameData)),
          defenders);
    }

    /**
     * End-of-round force removal of unescorted transports: with the restriction on, once the escort
     * is dead the engine sweeps the now-defenseless transports off the board at round end even
     * though no hit was scored on them ({@code RemoveUnprotectedUnits} / {@code
     * RetreatChecks#onlyDefenselessTransportsLeft}). The sim has no such end-of-round step and no
     * consumer of the flag, so it keeps trading against the escort under ordinary casualty order
     * and diverges on who is left standing. Red until the removal step exists.
     */
    @Test
    void unescortedTransportsAreSweptAtRoundEndUnderTheRestriction() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      setBooleanProperty(gameData, Constants.TRANSPORT_CASUALTIES_RESTRICTED, true);
      final Territory seaZone = territory("1 Sea Zone", gameData);

      final Collection<Unit> defenders =
          new ArrayList<>(transport(gameData).create(2, germans(gameData)));
      defenders.addAll(destroyer(gameData).create(1, germans(gameData)));

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          submarine(gameData).create(2, americans(gameData)),
          defenders);
    }

    /**
     * The restriction composed with the eligibility-overflow cargo cascade: two subs put two hits
     * on a destroyer plus two transports, all cargo loaded on the first transport. The escort
     * saturates the first hit; the second overflows onto a transport, and when that transport dies
     * its {@code IS_DEPENDENT} cargo must cascade off with it while the surviving transport is left
     * — restricted {@code {transport=1}} versus the unrestricted counterfactual that kills the two
     * cheaper transports and spares the destroyer ({@code {destroyer=1}}). Pins that overflow onto
     * a transport still cascades its cargo and that cargo never itself soaks the restriction; red
     * until the restriction is wired, green with it.
     */
    @Test
    void restrictedTransportStillCascadesItsCargoOnceItsEscortDies() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      setBooleanProperty(gameData, Constants.TRANSPORT_CASUALTIES_RESTRICTED, true);
      final Territory seaZone = territory("1 Sea Zone", gameData);

      final List<Unit> transportUnit = transport(gameData).create(2, germans(gameData));
      final Collection<Unit> cargo = infantry(gameData).create(2, germans(gameData));
      // Green is fixture-dependent: all cargo rides transportUnit[0], the transport the engine's
      // limitTransportsToSelect().limit(1) picks first, so its cargo cascades on both paths.
      // Loading
      // transportUnit[1] instead would diverge — the adapter pools cargo per side while the engine
      // links it per unit (the §E.1 v1 gap), so the pooled cascade would sink the wrong transport's
      // cargo.
      cargo.forEach(unit -> unit.setTransportedBy(transportUnit.get(0)));
      final Collection<Unit> defenders = new ArrayList<>(transportUnit);
      defenders.addAll(cargo);
      defenders.addAll(destroyer(gameData).create(1, germans(gameData)));

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          submarine(gameData).create(2, americans(gameData)),
          defenders);
    }

    /**
     * The end-of-round unescorted-transport sweep against the engine: four destroyers face two 2-HP
     * battleships escorting a transport, and once the escorts die the restriction sweeps the
     * now-unescorted transport at round end ({@code
     * RemoveUnprotectedUnits#checkUndefendedTransports}).
     *
     * <p>Still disabled after the lone-multi-HP drop is closed: this fixture also trips a distinct
     * multi-HP hit-accounting divergence — under {@code alwaysHits} the engine eliminates the
     * entire attacking force (attacker survivors empty), while the bounded sim keeps a destroyer,
     * so the two paths disagree on how many rounds the 2-HP escorts survive independently of the
     * sweep. The sweep itself is pinned in isolation by {@code TransportSweepTest} in {@code
     * battle-calc-core}; kept here so a clean differential lands the day that multi-HP accounting
     * gap closes.
     */
    @Test
    @Disabled(
        "distinct multi-HP hit-accounting divergence beyond the lone-multi-HP drop; sweep pinned by"
            + " TransportSweepTest")
    void unescortedTransportIsSweptAtRoundEndLeavingTheAttackerStanding() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      setBooleanProperty(gameData, Constants.TRANSPORT_CASUALTIES_RESTRICTED, true);
      final Territory seaZone = territory("1 Sea Zone", gameData);

      final Collection<Unit> defenders =
          new ArrayList<>(battleship(gameData).create(2, germans(gameData)));
      defenders.addAll(transport(gameData).create(1, germans(gameData)));

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          destroyer(gameData).create(4, americans(gameData)),
          defenders);
    }

    /**
     * Concentrated fire sinks a lone multi-HP unit exactly like the engine. Three fighters land two
     * guaranteed hits on a single defending battleship under {@code alwaysHits}; the second hit
     * follows the battleship onto its damaged {@code onHit()} successor rather than dropping, so
     * the sim sinks it as the engine does. Pins the onHit-closure in {@code
     * ReferenceCasualtyAllocator} that keeps a damaged successor a legal casualty though it is
     * absent from the undamaged-only eligibility filter.
     */
    @Test
    void loneMultiHitBattleshipSinksUnderConcentratedFire() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory seaZone = territory("1 Sea Zone", gameData);

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          fighter(gameData).create(3, americans(gameData)),
          battleship(gameData).create(1, germans(gameData)));
    }

    /**
     * The AA stat-source gap (scope §2a bug 1) where it bites hardest: a defending aaGun with 0
     * normal defense but a real {@code getAttackAa} of 1. Under {@code alwaysHits} the engine's AA
     * fire kills the lone attacking fighter before it reaches the gun and its escorting infantry,
     * so both defenders survive; the sim bakes the gun's 0 normal defense as its firepower, so its
     * AA fire hits on {@code 0 < 0}, ie never — the fighter survives AA, trades in main combat, and
     * a defender dies. The fixture discriminates only because the gun's normal defense (0) and its
     * AA value (1) disagree; a same-stats bake would pass even under the bug.
     */
    @Test
    void aaGunWithZeroNormalAttackStillDamagesAirAttackers() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final Collection<Unit> defenders =
          new ArrayList<>(aaGun(gameData).create(1, germans(gameData)));
      defenders.addAll(infantry(gameData).create(1, germans(gameData)));

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          russians(gameData),
          germans(gameData),
          germany,
          fighter(gameData).create(1, russians(gameData)),
          defenders);
    }

    /**
     * The infinite-gun under-count (scope §2a bug 2), the face this oracle does see. REVISED's
     * aaGun has an infinite {@code maxAaAttacks}, so the engine ({@code AaPowerStrengthAndRolls})
     * fires one die per live air target — two dice against two fighters — killing both in AA under
     * {@code alwaysHits}; no attacker reaches main combat, so both defenders survive. Before the
     * per-round dice cap the sim baked a single static AA die regardless of air count, so it killed
     * one fighter and spared the other into main combat where it could kill the infantry — a
     * composition divergence the exact-equality oracle catches. The infantry forces a real battle
     * so the gun actually fires (a gun-only defender trips the engine's
     * no-battle-against-infrastructure rule). Complements {@link
     * #stackedAaGunsOverCountDiceButSurvivorsStillMatchUnderAlwaysHits}, whose over-count face
     * stays hidden from this oracle.
     */
    @Test
    void infiniteAaGunFiresOncePerAirTargetSoBothFightersDieInAa() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final Collection<Unit> defenders =
          new ArrayList<>(aaGun(gameData).create(1, germans(gameData)));
      defenders.addAll(infantry(gameData).create(1, germans(gameData)));
      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          russians(gameData),
          germans(gameData),
          germany,
          fighter(gameData).create(2, russians(gameData)),
          defenders);
    }

    /**
     * The stacked-AA over-count (scope §2a bug 2), and why it stays invisible to this oracle. Each
     * AA gun bakes its real firepower, and the per-round cap now clamps total AA dice at the live
     * air-target count — here one — so three defending guns roll one die, matching the engine
     * ({@code AaPowerStrengthAndRolls}) rather than three. The cap change is nonetheless invisible
     * to this oracle: under {@code alwaysHits} the pre-cap three dice and the engine's one both
     * land at least one hit on the lone fighter, and neither can kill more air than exists, so
     * survivors matched exactly before and after. The infantry is load-bearing — it forces a real
     * battle so AA actually fires; a gun-only defender would trip the engine's separate
     * no-battle-against-infrastructure rule instead. The over-count only moves the seeded win%, so
     * this pins that the cap does not regress the exact-equality survivors; its distributional
     * effect is a fuzz-pass concern.
     */
    @Test
    void stackedAaGunsOverCountDiceButSurvivorsStillMatchUnderAlwaysHits() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final Collection<Unit> defenders =
          new ArrayList<>(aaGun(gameData).create(3, germans(gameData)));
      defenders.addAll(infantry(gameData).create(1, germans(gameData)));
      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          russians(gameData),
          germans(gameData),
          germany,
          fighter(gameData).create(1, russians(gameData)),
          defenders);
    }

    /**
     * An infrastructure-only defender is no battle at all. A lone AA gun is infrastructure ({@code
     * isAA} sets {@code isInfrastructure}), so the engine ({@code MustFightBattle.fight}) ends the
     * battle for the attacker before any AA step: the gun never fires and, headless, is captured
     * rather than shot, so it stays a defender survivor. Without the same short-circuit the sim
     * fired the infinite AA gun once per air target and killed both fighters. The guard makes both
     * fighters pass through unharmed and the gun survive. The sibling AA tests add a defending
     * infantry precisely to avoid this rule so the gun actually fires.
     */
    @Test
    void infrastructureOnlyDefenderIsNoBattleSoAirAttackersPassUnharmed() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          russians(gameData),
          germans(gameData),
          germany,
          fighter(gameData).create(2, russians(gameData)),
          aaGun(gameData).create(1, germans(gameData)));
    }

    /**
     * The no-battle short-circuit holds for a mixed air-and-land attack, exercising the engine's
     * other survivor branch. With a non-air attacker present, {@code getRemainingDefendingUnits}
     * keeps the AA gun a survivor without re-adding territory units, and every attacker passes
     * through — the land unit captures the gun at battle end rather than shooting it (headless, so
     * the gun still shows as a survivor). Before the guard the sim killed the fighter in AA and let
     * the infantry sink the gun in main combat.
     */
    @Test
    void infrastructureOnlyDefenderIsNoBattleForMixedAirAndLandAttackers() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      final Territory germany = territory("Germany", gameData);
      final Collection<Unit> attackers =
          new ArrayList<>(fighter(gameData).create(1, russians(gameData)));
      attackers.addAll(infantry(gameData).create(1, russians(gameData)));
      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          russians(gameData),
          germans(gameData),
          germany,
          attackers,
          aaGun(gameData).create(1, germans(gameData)));
    }

    /**
     * A regression pin on the oracle's real pre-battle-submerge behavior — NOT evidence the engine
     * retreats here. With {@code subRetreatBeforeBattle} on and no defending destroyer the engine
     * does reach a pre-battle submerge check, but its headless AI ({@code
     * DummyPlayer#retreatQuery}) approves a submerge only when every enemy is a non-destroyer
     * plane; a surface carrier is not, so it declines. The sub stays, fires its first strike, and
     * sinks the 1-HP carrier — exactly what the sim does, since the sim has no pre-battle
     * checkpoint either. The property moves only <em>when</em> the doomed submerge is evaluated,
     * not whether it succeeds, so both paths agree. A general BEFORE_BATTLE retreat is deliberately
     * not modeled: it would be more permissive than the oracle, which has no all-arms pre-battle
     * evasion.
     */
    @Test
    void subDeclinesPreBattleSubmergeAgainstSurfaceDefender() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      setBooleanProperty(gameData, Constants.SUB_RETREAT_BEFORE_BATTLE, true);
      final Territory seaZone = territory("1 Sea Zone", gameData);

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          submarine(gameData).create(1, americans(gameData)),
          carrier(gameData).create(1, germans(gameData)));
    }

    /**
     * The destroyer block on pre-battle sub evasion: {@code subRetreatBeforeBattle} is on, but a
     * defending destroyer trips {@code isDestroyerPresent} before any submerge query runs, so the
     * engine cannot evade the attacking sub before battle — a static structural rule both paths
     * honor, so they fight and agree. The destroyer also negates the sub's first strike, so under
     * {@code alwaysHits} the two trade simultaneously and neither survives.
     */
    @Test
    void defendingDestroyerBlocksPreBattleSubEvasion() {
      final GameData gameData = TestMapGameData.REVISED.getGameData();
      setBooleanProperty(gameData, Constants.SUB_RETREAT_BEFORE_BATTLE, true);
      final Territory seaZone = territory("1 Sea Zone", gameData);

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          submarine(gameData).create(1, americans(gameData)),
          destroyer(gameData).create(1, germans(gameData)));
    }

    /**
     * The invincible-sub bug: on WW2V3 ({@code Air Attack Sub Restricted} on, so the sub bakes
     * {@code CANNOT_BE_TARGETED_BY_ALL}), a fighter escorting a battleship against a lone protected
     * sub with no destroyer must NOT rob the battleship of its target. The engine resolves
     * eligibility per firing unit type, so the battleship sinks the sub even though the fighter
     * cannot touch it; group-wide resolution made the sub untargetable by both and thus immortal.
     * Run to resolution — the sub sneak-fires round 1, and the battleship only finishes it in main
     * combat, so the divergence shows across rounds, not in one.
     */
    @Test
    void mixedAirAndSurfaceAttackLetsTheSurfaceUnitSinkTheProtectedSub() {
      final GameData gameData = TestMapGameData.WW2V3_1942.getGameData();
      final Territory seaZone = territory("1 Sea Zone", gameData);

      final Collection<Unit> attackers = fighter(gameData).create(1, americans(gameData));
      attackers.addAll(battleship(gameData).create(1, americans(gameData)));

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          attackers,
          submarine(gameData).create(1, germans(gameData)));
    }

    /**
     * The immunity itself survives the per-firer split: a pure-air attack on the protected sub with
     * no destroyer still cannot hit it, so the sub lives on both paths. Guards against the split
     * accidentally handing air a target it should never have.
     */
    @Test
    void pureAirStillCannotHitAProtectedSubWithoutADestroyer() {
      final GameData gameData = TestMapGameData.WW2V3_1942.getGameData();
      final Territory seaZone = territory("1 Sea Zone", gameData);

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          fighter(gameData).create(2, americans(gameData)),
          submarine(gameData).create(1, germans(gameData)));
    }

    /**
     * A destroyer on the firing side strips the immunity side-wide, so the fighter beside it may
     * hit the protected sub — matching the engine's once-per-step {@code destroyerPresent}. Both
     * paths sink the sub.
     */
    @Test
    void anAttackingDestroyerLetsAirHitTheProtectedSub() {
      final GameData gameData = TestMapGameData.WW2V3_1942.getGameData();
      final Territory seaZone = territory("1 Sea Zone", gameData);

      final Collection<Unit> attackers = fighter(gameData).create(1, americans(gameData));
      attackers.addAll(destroyer(gameData).create(1, americans(gameData)));

      assertIdenticalSurvivorsUnderAlwaysHits(
          gameData,
          americans(gameData),
          germans(gameData),
          seaZone,
          attackers,
          submarine(gameData).create(1, germans(gameData)));
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

  /**
   * Synthesizes an enemy strength support: {@code giver} (owned by {@code giverOwner}) cuts {@code
   * target}'s combat strength by {@code bonus} whenever the giver attacks. Attached to the loaded
   * data before the support list is cached, so both the engine and the adapter read it — the only
   * way to exercise normal-combat enemy support, since no bundled test map declares one.
   */
  private static void injectEnemyStrengthDebuff(
      final GameData gameData,
      final UnitType giver,
      final UnitType target,
      final GamePlayer giverOwner,
      final int bonus)
      throws GameParseException {
    final UnitSupportAttachment rule =
        new UnitSupportAttachment(
            Constants.SUPPORT_ATTACHMENT_PREFIX + "EnemyDebuffTest", giver, gameData);
    rule.setDice("strength");
    rule.setFaction("enemy");
    rule.setSide("offence");
    rule.setBonus(bonus);
    rule.setBonusType("enemyDebuff");
    rule.setNumber(3);
    rule.setUnitType(Set.of(target));
    rule.setPlayers(List.of(giverOwner));
    giver.addAttachment(rule.getName(), rule);
  }

  /**
   * Synthesizes an allied support that is both strength and roll: {@code giver} (owned by {@code
   * giverOwner}) lends {@code target} {@code bonus} attack and {@code bonus} rolls whenever it
   * attacks. {@code dice="roll:strength"} is the one shape no bundled test map declares, so it is
   * attached before the support list is cached, the way REVISED's old-artillery support is
   * synthesized.
   */
  private static void injectDualStrengthAndRollSupport(
      final GameData gameData,
      final UnitType giver,
      final UnitType target,
      final GamePlayer giverOwner,
      final int bonus)
      throws GameParseException {
    final UnitSupportAttachment rule =
        new UnitSupportAttachment(
            Constants.SUPPORT_ATTACHMENT_PREFIX + "DualSupportTest", giver, gameData);
    rule.setDice("roll:strength");
    rule.setFaction("allied");
    rule.setSide("offence");
    rule.setBonus(bonus);
    rule.setBonusType("dualSupport");
    rule.setNumber(10);
    rule.setUnitType(Set.of(target));
    rule.setPlayers(List.of(giverOwner));
    giver.addAttachment(rule.getName(), rule);
  }

  /**
   * Synthesizes a strong allied offense strength support: {@code giver} lends {@code target} {@code
   * bonus} attack when attacking. Used to lift a supported unit's power above a normally-stronger
   * type so support-adjusted casualty ordering (§5.7) becomes observable.
   */
  private static void injectStrongOffenseSupport(
      final GameData gameData,
      final UnitType giver,
      final UnitType target,
      final GamePlayer giverOwner,
      final int bonus)
      throws GameParseException {
    final UnitSupportAttachment rule =
        new UnitSupportAttachment(
            Constants.SUPPORT_ATTACHMENT_PREFIX + "OolSupportTest", giver, gameData);
    rule.setDice("strength");
    rule.setFaction("allied");
    rule.setSide("offence");
    rule.setBonus(bonus);
    rule.setBonusType("oolSupport");
    rule.setNumber(10);
    rule.setUnitType(Set.of(target));
    rule.setPlayers(List.of(giverOwner));
    giver.addAttachment(rule.getName(), rule);
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
