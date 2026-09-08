package games.strategy.triplea.odds.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.BooleanProperty;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * The map-XML-fuzzed differential harness (design §9, Phase 3): the honest drift meter for the
 * {@code GameData}-free calc. Where {@link BattleCalcDifferentialTest} pins seven hand-picked seams
 * to <em>exact</em> equality, this one draws thousands of random forces from real map XML, runs
 * both the engine oracle ({@link BattleCalculator}, flag OFF → a real {@code MustFightBattle}) and
 * the new bounded-context calc ({@link GameDataBattleAdapter} → {@link ReferenceBattleSimulator})
 * under a shared {@code alwaysHits} source, and <em>reports</em> where their survivor compositions
 * diverge.
 *
 * <p><b>This suite does not assert a low drift bar.</b> The adapter is known-incomplete — support
 * rules, transport/carrier dependents, non-six-sided dice, and low-luck are all stubbed — so drift
 * is expected and measuring it is the deliverable. The one hard assertion is a self-check that the
 * harness itself ran (it produced comparisons and didn't silently no-op); everything else is
 * printed to stdout for a human to read. It is therefore {@code @Tag("fuzz")} and excluded from the
 * {@code check}/{@code ./verify} gate — run it on demand with the {@code fuzzTest} Gradle task.
 *
 * <p>The alwaysHits regime is the whole point: with every die a deterministic hit, any
 * survivor-count difference is a <em>modeling</em> drift, never dice noise, so each scenario needs
 * only one run and the comparison is exact. Scenarios are tagged by which stubbed feature they
 * exercise, and the match rate is aggregated by tag so the report shows whether drift tracks the
 * incompleteness (it should) or leaks into vanilla fights (which would be a real surprise).
 */
@Tag("fuzz")
class FuzzedBattleCalcDifferentialTest extends AbstractClientSettingTestCase {

  /** Printed on every run so a drift report can be reproduced exactly. */
  private static final long SEED = 20260906L;

  /** Per map; total scenarios is this times the map count below. */
  private static final int SCENARIOS_PER_MAP = 800;

  private static final int MAX_TYPES_PER_SIDE = 3;
  private static final int MAX_COUNT_PER_TYPE = 6;
  private static final int MAX_FORCE_PER_SIDE = 8;

  /** The seeded distributional pass is heavier per scenario, so it runs fewer of them. */
  private static final int SEEDED_SCENARIOS_PER_MAP = 120;

  private static final int SEEDED_RUNS = 200;

  /**
   * Win% within 5 points counts as agreement; 200 runs keep sampling spread comfortably under it.
   */
  private static final double SEEDED_TOLERANCE = 0.05;

  /**
   * The maps to fuzz. REVISED is the proven baseline the hand-picked differential already uses;
   * WW2V3_1942 and LHTR add support-giving artillery and richer unit rosters so the by-tag
   * breakdown has something to concentrate in. TWW ({@code Total_World_War_Dec1941.xml}) carries a
   * negative support {@code bonus} and capped {@code bonusType}s, so it is the only map here that
   * exercises the negative-bonus / bonusType-cap support paths against a real oracle.
   */
  private static final List<TestMapGameData> MAPS =
      List.of(
          TestMapGameData.REVISED,
          TestMapGameData.WW2V3_1942,
          TestMapGameData.LHTR,
          TestMapGameData.TWW);

  @Test
  void fuzzRealMapsAndReportDriftAgainstTheEngineOracle() {
    final Report report = new Report(SEED);
    final Random rng = new Random(SEED);
    for (final TestMapGameData map : MAPS) {
      fuzzMap(map, rng, report);
    }
    report.print();

    // The harness actually exercised both calcs.
    assertThat(report.total()).isPositive();
    // Per-flag drift stays reported-not-gated (it carries known capability residuals), but the
    // vanilla bucket — no stubbed feature engaged — must match the engine exactly under alwaysHits,
    // where every modeled rule is exact. A vanilla mismatch is a real core regression, not drift, so
    // it is the one invariant worth failing on. Deterministic under the fixed seed, so it cannot
    // flake.
    final int[] vanilla = report.tagCell("vanilla");
    assertThat(vanilla[1])
        .as("fuzz must produce vanilla scenarios to hold to the bar")
        .isPositive();
    assertThat(vanilla[0])
        .as("vanilla (no stubbed feature) scenarios must match the engine exactly under alwaysHits")
        .isEqualTo(vanilla[1]);
  }

  /**
   * The distributional companion to the alwaysHits pass, and the reason it exists: alwaysHits makes
   * every die a hit regardless of strength, so any feature that only shifts a die's
   * <em>strength</em> — support bonuses above all — is invisible to the exact-equality pass (it
   * reports 100% for support not because support is modeled but because it cannot matter there).
   * This pass runs both calcs over a seeded {@code PlainRandomSource} for many runs and compares
   * attacker win% within a tolerance, so strength-driven drift finally shows. Fewer scenarios and a
   * bounded run count keep it inside the runtime budget. Drift stays diagnostic here — unlike the
   * alwaysHits pass, which additionally holds the vanilla bucket to an exact match, this
   * distributional pass has no exact invariant and too small a vanilla sample to gate on tolerance.
   */
  @Test
  void seededFuzzRealMapsAndReportWinPercentDrift() {
    final SeededReport report = new SeededReport(SEED, SEEDED_TOLERANCE);
    final Random rng = new Random(SEED);
    for (final TestMapGameData map : MAPS) {
      seededFuzzMap(map, rng, report);
    }
    report.print();

    // Distributional pass: exact-match has no meaning under a seeded source, and the vanilla sample
    // is too small to gate on a win% tolerance without risking flake, so drift stays diagnostic here
    // — the alwaysHits pass owns the vanilla-exact invariant.
    assertThat(report.total()).isPositive();
  }

  /**
   * The flag sampler is reproducible: two samplers seeded alike draw an identical combination, so a
   * flaky fuzz report can never be blamed on the sampler injecting different flags between runs.
   * Uses the run's frozen {@code SEED} rather than a fresh {@code Random} because that is the exact
   * seed the fuzz passes draw from.
   */
  @Test
  void ruleFlagSampleIsReproducibleForAGivenSeed() {
    final RuleFlagSample first = RuleFlagSample.sample(new Random(SEED));
    final RuleFlagSample second = RuleFlagSample.sample(new Random(SEED));

    org.assertj.core.api.Assertions.assertThat(second).isEqualTo(first);
  }

  /**
   * Each of the six sampled flags maps to its own distinct by-tag label — {@code
   * transportCasualtiesRestricted} included as a first-class member, not folded anonymously into
   * "the six flags" — so the by-tag report attributes drift to the exact flag value that caused it.
   * Turning one flag on at a time pins the {@code onTags()} mapping and guards a copy-paste that
   * would give two flags the same label.
   */
  @Test
  void eachRuleFlagMapsToItsOwnByTagLabel() {
    assertThat(new RuleFlagSample(true, false, false, false, false, false).onTags())
        .containsExactly("ww2v2-on");
    assertThat(new RuleFlagSample(false, true, false, false, false, false).onTags())
        .containsExactly("defending-subs-sneak-on");
    assertThat(new RuleFlagSample(false, false, true, false, false, false).onTags())
        .containsExactly("transport-restricted");
    assertThat(new RuleFlagSample(false, false, false, true, false, false).onTags())
        .containsExactly("submersible-subs-on");
    assertThat(new RuleFlagSample(false, false, false, false, true, false).onTags())
        .containsExactly("def-subs-submerge-on");
    assertThat(new RuleFlagSample(false, false, false, false, false, true).onTags())
        .containsExactly("lhtr-heavy-bombers-on");
  }

  private void seededFuzzMap(
      final TestMapGameData map, final Random rng, final SeededReport report) {
    final GameData gameData = map.getGameData();
    final List<GamePlayer> players =
        gameData.getPlayerList().getPlayers().stream()
            .filter(player -> !player.isNull())
            .collect(Collectors.toList());
    final List<Territory> landTerritories =
        gameData.getMap().getTerritories().stream()
            .filter(territory -> !territory.isWater())
            .collect(Collectors.toList());
    final List<Territory> seaZones =
        gameData.getMap().getTerritories().stream()
            .filter(Territory::isWater)
            .collect(Collectors.toList());
    if (players.size() < 2 || landTerritories.isEmpty()) {
      return;
    }

    for (int i = 0; i < SEEDED_SCENARIOS_PER_MAP; i++) {
      final boolean sea = !seaZones.isEmpty() && rng.nextBoolean();
      final Territory location = sea ? pick(seaZones, rng) : pick(landTerritories, rng);
      final GamePlayer attacker = pick(players, rng);
      final GamePlayer defender = pickDistinct(players, attacker, rng);
      final List<UnitType> pool = combatPool(gameData, location.isWater());
      if (pool.isEmpty()) {
        continue;
      }
      final Collection<Unit> attacking = randomForce(pool, attacker, rng);
      final Collection<Unit> defending = randomForce(pool, defender, rng);
      if (attacking.isEmpty() || defending.isEmpty()) {
        continue;
      }
      // Set the sampled flags before building the oracle: BattleCalculator clones the
      // GameData in its constructor, so a later write never reaches the oracle's copy.
      // The adapter reads them live when it bakes the scenario, so one write feeds both.
      final RuleFlagSample flags = RuleFlagSample.sample(rng);
      flags.applyTo(gameData);
      final BattleCalculator oracle = new BattleCalculator(gameData);
      final Set<String> tags = tagsFor(attacking, defending, flags);
      final ScenarioKey key =
          new ScenarioKey(
              map.name(),
              location.getName(),
              attacker.getName(),
              defender.getName(),
              countByType(attacking),
              countByType(defending),
              tags);
      try {
        oracle.setRandomSource(new PlainRandomSource(SEED));
        final double oracleWin =
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
                    SEEDED_RUNS)
                .getAttackerWinPercent();
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
        final double newWin =
            new ReferenceBattleSimulator()
                .simulate(
                    scenario, SEEDED_RUNS, new EngineRandomSource(new PlainRandomSource(SEED)))
                .attackerWinPercent();
        report.record(key, oracleWin, newWin);
      } catch (final RuntimeException e) {
        report.noteError();
      }
    }
  }

  private void fuzzMap(final TestMapGameData map, final Random rng, final Report report) {
    final GameData gameData = map.getGameData();
    final List<GamePlayer> players =
        gameData.getPlayerList().getPlayers().stream()
            .filter(player -> !player.isNull())
            .collect(Collectors.toList());
    final List<Territory> landTerritories =
        gameData.getMap().getTerritories().stream()
            .filter(territory -> !territory.isWater())
            .collect(Collectors.toList());
    final List<Territory> seaZones =
        gameData.getMap().getTerritories().stream()
            .filter(Territory::isWater)
            .collect(Collectors.toList());
    if (players.size() < 2 || landTerritories.isEmpty()) {
      report.noteSkippedMap(map, "needs two players and a land territory");
      return;
    }

    for (int i = 0; i < SCENARIOS_PER_MAP; i++) {
      final boolean sea = !seaZones.isEmpty() && rng.nextBoolean();
      final Territory location = sea ? pick(seaZones, rng) : pick(landTerritories, rng);
      final GamePlayer attacker = pick(players, rng);
      final GamePlayer defender = pickDistinct(players, attacker, rng);
      final List<UnitType> pool = combatPool(gameData, location.isWater());
      if (pool.isEmpty()) {
        continue;
      }
      final Collection<Unit> attacking = randomForce(pool, attacker, rng);
      final Collection<Unit> defending = randomForce(pool, defender, rng);
      if (attacking.isEmpty() || defending.isEmpty()) {
        continue;
      }
      // Set the sampled flags before building the oracle: BattleCalculator clones the
      // GameData in its constructor, so a later write never reaches the oracle's copy.
      // The adapter reads them live when it bakes the scenario, so one write feeds both.
      // A fresh oracle per scenario is the cost of a per-scenario flag combination.
      final RuleFlagSample flags = RuleFlagSample.sample(rng);
      flags.applyTo(gameData);
      final BattleCalculator oracle = new BattleCalculator(gameData);
      runScenario(
          map, gameData, oracle, flags, attacker, defender, location, attacking, defending, report);
    }
  }

  /**
   * One fuzzed fight through both paths. Both sides draw from the same {@code alwaysHits} source (a
   * deterministic 0), so a survivor-count difference is a pure modeling drift. An exception on the
   * new path is itself a reported drift category (a stub that throws); an exception on the oracle
   * side is treated as the harness having picked an invalid composition and is counted separately.
   */
  private void runScenario(
      final TestMapGameData map,
      final GameData gameData,
      final BattleCalculator oracle,
      final RuleFlagSample flags,
      final GamePlayer attacker,
      final GamePlayer defender,
      final Territory location,
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final Report report) {
    final Set<String> tags = tagsFor(attacking, defending, flags);
    final ScenarioKey key =
        new ScenarioKey(
            map.name(),
            location.getName(),
            attacker.getName(),
            defender.getName(),
            countByType(attacking),
            countByType(defending),
            tags);

    final Map<String, Integer> oracleAttSurvivors;
    final Map<String, Integer> oracleDefSurvivors;
    try {
      final ScriptedRandomSource dice = ScriptedRandomSource.alwaysHits();
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
      oracleAttSurvivors = countByType(oracleResult.getRemainingAttackingUnits());
      oracleDefSurvivors = countByType(oracleResult.getRemainingDefendingUnits());
    } catch (final RuntimeException e) {
      report.noteOracleError(key, e);
      return;
    }

    final Map<String, Integer> newAttSurvivors;
    final Map<String, Integer> newDefSurvivors;
    try {
      final ScriptedRandomSource dice = ScriptedRandomSource.alwaysHits();
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
      newAttSurvivors = countByType(newResult.attackerSurvivors());
      newDefSurvivors = countByType(newResult.defenderSurvivors());
    } catch (final RuntimeException e) {
      report.noteNewPathError(key, e);
      return;
    }

    final int drift =
        compositionDistance(oracleAttSurvivors, newAttSurvivors)
            + compositionDistance(oracleDefSurvivors, newDefSurvivors);
    report.record(
        new Comparison(
            key, oracleAttSurvivors, oracleDefSurvivors, newAttSurvivors, newDefSurvivors, drift));
  }

  /**
   * Combat-eligible unit types for the territory's domain: no infrastructure (factories never
   * fight), and sea zones take only sea or air units while land territories take only non-sea
   * units, matching what the engine will accept in that battle.
   */
  private static List<UnitType> combatPool(final GameData gameData, final boolean water) {
    final List<UnitType> pool = new ArrayList<>();
    for (final UnitType type : gameData.getUnitTypeList()) {
      final UnitAttachment ua = type.getUnitAttachment();
      if (ua.isInfrastructure() || ua.getHitPoints() < 1) {
        continue;
      }
      final boolean domainOk = water ? (ua.isSea() || ua.isAir()) : !ua.isSea();
      if (domainOk) {
        pool.add(type);
      }
    }
    return pool;
  }

  private static Collection<Unit> randomForce(
      final List<UnitType> pool, final GamePlayer owner, final Random rng) {
    final List<UnitType> shuffled = new ArrayList<>(pool);
    java.util.Collections.shuffle(shuffled, rng);
    final int typeCount = 1 + rng.nextInt(Math.min(MAX_TYPES_PER_SIDE, shuffled.size()));
    final List<Unit> force = new ArrayList<>();
    int total = 0;
    for (int i = 0; i < typeCount && total < MAX_FORCE_PER_SIDE; i++) {
      final int room = MAX_FORCE_PER_SIDE - total;
      final int count = 1 + rng.nextInt(Math.min(MAX_COUNT_PER_TYPE, room));
      force.addAll(shuffled.get(i).create(count, owner));
      total += count;
    }
    return force;
  }

  /**
   * Which known-incomplete features the union of both forces exercises, plus which non-default rule
   * flags this scenario sampled; drives the by-tag report. The flag tags are what make drift
   * attributable to a specific flag <em>value</em> — without them a {@code ww2v2=true} run and a
   * {@code ww2v2=false} run of the same forces land in identical buckets.
   */
  private static Set<String> tagsFor(
      final Collection<Unit> attacking,
      final Collection<Unit> defending,
      final RuleFlagSample flags) {
    final Set<String> tags = new java.util.TreeSet<>();
    final List<Unit> all = new ArrayList<>(attacking);
    all.addAll(defending);
    for (final Unit unit : all) {
      final UnitType type = unit.getType();
      final UnitAttachment ua = type.getUnitAttachment();
      if (!UnitSupportAttachment.get(type).isEmpty()) {
        tags.add("support-giver");
      }
      if (ua.getArtillerySupportable() || !ua.getReceivesAbilityWhenWith().isEmpty()) {
        tags.add("support-receiver");
      }
      if (ua.getTransportCapacity() > 0) {
        tags.add("transport");
      }
      if (ua.isAaForCombatOnly()) {
        tags.add("aa");
      }
      if (ua.getIsFirstStrike()) {
        tags.add("first-strike");
      }
      if (ua.getCanEvade()) {
        tags.add("submarine-evade");
      }
      if (!ua.getCanNotBeTargetedBy().isEmpty()) {
        tags.add("cannot-be-targeted");
      }
      if (ua.getHitPoints() > 1) {
        tags.add("multi-hitpoint");
      }
      if (ua.isAir()) {
        tags.add("air");
      }
    }
    tags.addAll(flags.onTags());
    if (STUBBED_FEATURE_TAGS.stream().noneMatch(tags::contains)) {
      tags.add("vanilla");
    }
    return tags;
  }

  /**
   * The tags that touch adapter/model stubs; a scenario clear of all of them is "vanilla". The
   * flag-on tags are included so a non-default flag keeps a fight out of the vanilla bucket —
   * vanilla must stay a clean baseline, so flag-driven drift never hides there.
   */
  private static final List<String> STUBBED_FEATURE_TAGS =
      java.util.stream.Stream.concat(
              java.util.stream.Stream.of(
                  "support-giver",
                  "support-receiver",
                  "transport",
                  "aa",
                  "first-strike",
                  "submarine-evade",
                  "cannot-be-targeted",
                  "multi-hitpoint"),
              RuleFlagSample.ALL_TAGS.stream())
          .collect(Collectors.toList());

  private static <T> T pick(final List<T> items, final Random rng) {
    return items.get(rng.nextInt(items.size()));
  }

  private static GamePlayer pickDistinct(
      final List<GamePlayer> players, final GamePlayer other, final Random rng) {
    GamePlayer candidate;
    do {
      candidate = pick(players, rng);
    } while (candidate.equals(other));
    return candidate;
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

  private static int compositionDistance(
      final Map<String, Integer> a, final Map<String, Integer> b) {
    final Set<String> types = new java.util.TreeSet<>(a.keySet());
    types.addAll(b.keySet());
    int distance = 0;
    for (final String type : types) {
      distance += Math.abs(a.getOrDefault(type, 0) - b.getOrDefault(type, 0));
    }
    return distance;
  }

  private static Map<String, Integer> countByType(final Collection<Unit> units) {
    final Map<String, Integer> counts = new TreeMap<>();
    for (final Unit unit : units) {
      counts.merge(unit.getType().getName(), 1, Integer::sum);
    }
    return counts;
  }

  private static Map<String, Integer> countByType(final Force survivors) {
    final Map<String, Integer> counts = new TreeMap<>();
    survivors
        .counts()
        .forEach((key, count) -> counts.merge(key.profile().type().name(), count, Integer::sum));
    return counts;
  }

  /**
   * A sampled combination of the six {@code RulesProfile} rule flags for one fuzz scenario. Drawn
   * from the run's seeded {@code Random}, so the flag stream is reproducible from {@code SEED}
   * alone — a re-run cannot pin drift on a different flag draw. Exactly the six flags {@code
   * RulesProfile} models are here; {@code twoHitBattleships} and {@code superSubDefenseBonus} are
   * deliberately absent because they are baked into unit hit-points/defense elsewhere and would
   * double-count.
   */
  private record RuleFlagSample(
      boolean ww2v2,
      boolean defendingSubsSneakAttack,
      boolean transportCasualtiesRestricted,
      boolean submersibleSubs,
      boolean submarinesDefendingMaySubmergeOrRetreat,
      boolean lhtrHeavyBombers) {

    /** Every by-tag label the sampler can emit, so "vanilla" can exclude all of them. */
    static final List<String> ALL_TAGS =
        List.of(
            "ww2v2-on",
            "defending-subs-sneak-on",
            "transport-restricted",
            "submersible-subs-on",
            "def-subs-submerge-on",
            "lhtr-heavy-bombers-on");

    /** Draws each of the six flags independently; six {@code rng} draws per scenario. */
    static RuleFlagSample sample(final Random rng) {
      return new RuleFlagSample(
          rng.nextBoolean(),
          rng.nextBoolean(),
          rng.nextBoolean(),
          rng.nextBoolean(),
          rng.nextBoolean(),
          rng.nextBoolean());
    }

    /**
     * Writes all six flags onto {@code gameData} — every flag explicitly, not left at the map
     * default — so the sampled value is what both paths see. Must run before the oracle is
     * constructed (it clones the data) and before the adapter bakes the scenario (it reads live).
     */
    void applyTo(final GameData gameData) {
      setBooleanProperty(gameData, Constants.WW2V2, ww2v2);
      setBooleanProperty(gameData, Constants.DEFENDING_SUBS_SNEAK_ATTACK, defendingSubsSneakAttack);
      setBooleanProperty(
          gameData, Constants.TRANSPORT_CASUALTIES_RESTRICTED, transportCasualtiesRestricted);
      setBooleanProperty(gameData, Constants.SUBMERSIBLE_SUBS, submersibleSubs);
      setBooleanProperty(
          gameData,
          Constants.SUBMARINES_DEFENDING_MAY_SUBMERGE_OR_RETREAT,
          submarinesDefendingMaySubmergeOrRetreat);
      setBooleanProperty(gameData, Constants.LHTR_HEAVY_BOMBERS, lhtrHeavyBombers);
    }

    /** The by-tag report labels for whichever flags this sample turned on. */
    Set<String> onTags() {
      final Set<String> tags = new java.util.TreeSet<>();
      if (ww2v2) {
        tags.add("ww2v2-on");
      }
      if (defendingSubsSneakAttack) {
        tags.add("defending-subs-sneak-on");
      }
      if (transportCasualtiesRestricted) {
        tags.add("transport-restricted");
      }
      if (submersibleSubs) {
        tags.add("submersible-subs-on");
      }
      if (submarinesDefendingMaySubmergeOrRetreat) {
        tags.add("def-subs-submerge-on");
      }
      if (lhtrHeavyBombers) {
        tags.add("lhtr-heavy-bombers-on");
      }
      return tags;
    }
  }

  /** Immutable description of one fuzzed setup — enough to reproduce and to print. */
  private record ScenarioKey(
      String map,
      String territory,
      String attacker,
      String defender,
      Map<String, Integer> attackingForce,
      Map<String, Integer> defendingForce,
      Set<String> tags) {}

  /** One oracle-vs-new comparison; {@code drift} is the summed per-type survivor-count distance. */
  private record Comparison(
      ScenarioKey key,
      Map<String, Integer> oracleAttackerSurvivors,
      Map<String, Integer> oracleDefenderSurvivors,
      Map<String, Integer> newAttackerSurvivors,
      Map<String, Integer> newDefenderSurvivors,
      int drift) {
    boolean matches() {
      return drift == 0;
    }
  }

  /** Accumulates comparisons and errors, then prints the human-facing drift summary. */
  private static final class Report {
    private final long seed;
    private final List<Comparison> comparisons = new ArrayList<>();
    private final Map<String, int[]> byTag = new TreeMap<>(); // tag -> {matched, total}
    private int newPathErrors = 0;
    private int oracleErrors = 0;
    private final Map<String, Integer> newPathErrorKinds = new TreeMap<>();
    private final List<String> skippedMaps = new ArrayList<>();
    private String firstNewPathErrorExample = null;

    Report(final long seed) {
      this.seed = seed;
    }

    int total() {
      return comparisons.size();
    }

    /** {matched, total} for one feature tag, {@code {0, 0}} if no scenario touched it. */
    int[] tagCell(final String tag) {
      return byTag.getOrDefault(tag, new int[2]);
    }

    void record(final Comparison comparison) {
      comparisons.add(comparison);
      for (final String tag : comparison.key().tags()) {
        final int[] cell = byTag.computeIfAbsent(tag, t -> new int[2]);
        if (comparison.matches()) {
          cell[0]++;
        }
        cell[1]++;
      }
    }

    void noteNewPathError(final ScenarioKey key, final RuntimeException e) {
      newPathErrors++;
      newPathErrorKinds.merge(e.getClass().getSimpleName(), 1, Integer::sum);
      if (firstNewPathErrorExample == null) {
        firstNewPathErrorExample =
            describe(key) + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage();
      }
    }

    void noteOracleError(final ScenarioKey key, final RuntimeException e) {
      oracleErrors++;
    }

    void noteSkippedMap(final TestMapGameData map, final String why) {
      skippedMaps.add(map.name() + " (" + why + ")");
    }

    void print() {
      final long matched = comparisons.stream().filter(Comparison::matches).count();
      final StringBuilder out = new StringBuilder();
      out.append(
"""

================ FUZZED BATTLE-CALC DIFFERENTIAL ================
""");
      out.append("seed=").append(seed).append('\n');
      out.append("maps=").append(MAPS).append('\n');
      out.append("scenarios compared=").append(comparisons.size()).append('\n');
      out.append("new-path exceptions=").append(newPathErrors);
      if (!newPathErrorKinds.isEmpty()) {
        out.append(' ').append(newPathErrorKinds);
      }
      out.append('\n');
      out.append("oracle exceptions (invalid fuzz combos, excluded)=")
          .append(oracleErrors)
          .append('\n');
      if (!skippedMaps.isEmpty()) {
        out.append("skipped maps=").append(skippedMaps).append('\n');
      }
      out.append(
          String.format(
              "OVERALL exact-match rate = %d/%d = %.1f%%%n",
              matched, comparisons.size(), percent(matched, comparisons.size())));

      out.append(
"""

-- match rate by feature tag (a scenario counts under every tag it touches) --
""");
      byTag.forEach(
          (tag, cell) ->
              out.append(
                  String.format(
                      "  %-20s %5d/%-5d  %.1f%%%n",
                      tag, cell[0], cell[1], percent(cell[0], cell[1]))));

      out.append(
"""

-- worst-drift examples --
""");
      comparisons.stream()
          .filter(c -> !c.matches())
          .sorted(Comparator.comparingInt(Comparison::drift).reversed())
          .limit(3)
          .forEach(c -> out.append(describeDrift(c)).append('\n'));

      if (firstNewPathErrorExample != null) {
        out.append("\n-- first new-path exception --\n  ")
            .append(firstNewPathErrorExample)
            .append('\n');
      }
      out.append("================================================================\n");
      System.out.println(out);
    }

    private static double percent(final long numerator, final long denominator) {
      return denominator == 0 ? 0.0 : 100.0 * numerator / denominator;
    }

    private static String describe(final ScenarioKey key) {
      return key.map()
          + " @ "
          + key.territory()
          + " : "
          + key.attacker()
          + " "
          + key.attackingForce()
          + " vs "
          + key.defender()
          + " "
          + key.defendingForce()
          + " "
          + key.tags();
    }

    private static String describeDrift(final Comparison c) {
      return "  drift="
          + c.drift()
          + "  "
          + describe(c.key())
          + "\n      oracle survivors: att="
          + c.oracleAttackerSurvivors()
          + " def="
          + c.oracleDefenderSurvivors()
          + "\n      new    survivors: att="
          + c.newAttackerSurvivors()
          + " def="
          + c.newDefenderSurvivors();
    }
  }

  /** One seeded scenario's win% delta, kept for the by-tag aggregation and worst-case listing. */
  private record WinDelta(ScenarioKey key, double oracleWin, double newWin) {
    double delta() {
      return Math.abs(oracleWin - newWin);
    }
  }

  /** Accumulates seeded win% deltas and prints the distributional drift summary. */
  private static final class SeededReport {
    private final long seed;
    private final double tolerance;
    private final List<WinDelta> deltas = new ArrayList<>();
    private final Map<String, double[]> byTag =
        new TreeMap<>(); // tag -> {withinTol, total, sumDelta}
    private int errors = 0;

    SeededReport(final long seed, final double tolerance) {
      this.seed = seed;
      this.tolerance = tolerance;
    }

    int total() {
      return deltas.size();
    }

    void record(final ScenarioKey key, final double oracleWin, final double newWin) {
      final WinDelta delta = new WinDelta(key, oracleWin, newWin);
      deltas.add(delta);
      for (final String tag : key.tags()) {
        final double[] cell = byTag.computeIfAbsent(tag, t -> new double[3]);
        if (delta.delta() <= tolerance) {
          cell[0]++;
        }
        cell[1]++;
        cell[2] += delta.delta();
      }
    }

    void noteError() {
      errors++;
    }

    void print() {
      final long within = deltas.stream().filter(d -> d.delta() <= tolerance).count();
      final StringBuilder out = new StringBuilder();
      out.append(
"""

=========== SEEDED (DISTRIBUTIONAL) BATTLE-CALC DIFFERENTIAL ===========
""");
      out.append("seed=").append(seed).append("  runs/scenario=").append(SEEDED_RUNS).append('\n');
      out.append("attacker-win% agreement tolerance=").append(tolerance).append('\n');
      out.append("scenarios compared=")
          .append(deltas.size())
          .append("  errors=")
          .append(errors)
          .append('\n');
      out.append(
          String.format(
              "OVERALL within-tolerance rate = %d/%d = %.1f%%%n",
              within, deltas.size(), deltas.isEmpty() ? 0.0 : 100.0 * within / deltas.size()));

      out.append(
"""

-- within-tolerance rate and mean |win% delta| by feature tag --
""");
      byTag.forEach(
          (tag, cell) ->
              out.append(
                  String.format(
                      "  %-20s %5.0f/%-5.0f  %5.1f%%   mean|delta|=%.3f%n",
                      tag,
                      cell[0],
                      cell[1],
                      cell[1] == 0 ? 0.0 : 100.0 * cell[0] / cell[1],
                      cell[1] == 0 ? 0.0 : cell[2] / cell[1])));

      out.append(
"""

-- worst win% deltas --
""");
      deltas.stream()
          .sorted(Comparator.comparingDouble(WinDelta::delta).reversed())
          .limit(3)
          .forEach(
              d ->
                  out.append(
                      String.format(
                          "  |delta|=%.3f  oracleWin=%.3f newWin=%.3f  %s @ %s : %s %s vs %s %s %s%n",
                          d.delta(),
                          d.oracleWin(),
                          d.newWin(),
                          d.key().map(),
                          d.key().territory(),
                          d.key().attacker(),
                          d.key().attackingForce(),
                          d.key().defender(),
                          d.key().defendingForce(),
                          d.key().tags())));
      out.append("======================================================================\n");
      System.out.println(out);
    }
  }
}
