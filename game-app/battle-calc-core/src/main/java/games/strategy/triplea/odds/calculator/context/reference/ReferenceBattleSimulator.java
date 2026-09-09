package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.BattleResult;
import games.strategy.triplea.odds.calculator.context.model.BattleRound;
import games.strategy.triplea.odds.calculator.context.model.BattleScenario;
import games.strategy.triplea.odds.calculator.context.model.BattleView;
import games.strategy.triplea.odds.calculator.context.model.CombatFlag;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Constraints;
import games.strategy.triplea.odds.calculator.context.model.Dependents;
import games.strategy.triplea.odds.calculator.context.model.Domain;
import games.strategy.triplea.odds.calculator.context.model.FireContext;
import games.strategy.triplea.odds.calculator.context.model.FiringMode;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.Outcome;
import games.strategy.triplea.odds.calculator.context.model.Phase;
import games.strategy.triplea.odds.calculator.context.model.ProfileStats;
import games.strategy.triplea.odds.calculator.context.model.RetreatCheckpoint;
import games.strategy.triplea.odds.calculator.context.model.RollGroup;
import games.strategy.triplea.odds.calculator.context.model.RulesProfile;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.SimulationResults;
import games.strategy.triplea.odds.calculator.context.model.SupportRule;
import games.strategy.triplea.odds.calculator.context.seam.BattleSimulator;
import games.strategy.triplea.odds.calculator.context.seam.CasualtyAllocator;
import games.strategy.triplea.odds.calculator.context.seam.CasualtyOrder;
import games.strategy.triplea.odds.calculator.context.seam.CombatRelations;
import games.strategy.triplea.odds.calculator.context.seam.HitRoller;
import games.strategy.triplea.odds.calculator.context.seam.RandomSource;
import games.strategy.triplea.odds.calculator.context.seam.RetreatPolicy;
import games.strategy.triplea.odds.calculator.context.seam.RollGroupResolver;
import games.strategy.triplea.odds.calculator.context.seam.SupportResolver;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The reference per-round battle pipeline (design §5): composes the reference resolver, roller,
 * allocator, and retreat/relations seams into the Monte-Carlo loop the vectorized simulator will
 * later replace behind the same {@link BattleSimulator} seam. Stays {@code GameData}-free — the
 * adapter bakes every rule into the scenario, so nothing here reaches back into the engine.
 *
 * <p>{@code plan} is per-firing-side: each round plans the attackers and the defenders separately,
 * then interleaves their groups into one true firing sequence (AA, then first strike, then main).
 * Each phase is a simultaneous exchange rolled off a frozen snapshot: first strike off the
 * round-start plan, main off a re-plan taken after first-strike casualties — so both sides fire
 * from a shared exchange-start view while casualties land on the live working map as they are
 * dealt.
 */
public class ReferenceBattleSimulator implements BattleSimulator {

  // A legitimate battle converges in a handful of rounds; the cap only bounds a pathological
  // scenario where neither side can eliminate the other (eg forces that can never target one
  // another) so a run cannot spin forever. Reaching it ends the round loop as a stalemate.
  private static final int MAX_ROUNDS = 100;

  private final RollGroupResolver resolver;
  private final SupportResolver supportResolver;
  private final HitRoller roller;
  private final CasualtyAllocator allocator;
  private final CombatRelations relations;

  public ReferenceBattleSimulator() {
    this(new DiceHitRoller());
  }

  /**
   * Runs the fixed reference pipeline against the given roller — the seam where dice vs low-luck vs
   * a batched-vector impl diverge — so the hit-rolling strategy can be swapped without disturbing
   * resolver, allocator, or retreat wiring.
   */
  public ReferenceBattleSimulator(final HitRoller roller) {
    this.relations = new ReferenceCombatRelations();
    this.supportResolver = new ReferenceSupportResolver();
    this.resolver = new ReferenceRollGroupResolver(supportResolver, relations);
    this.roller = roller;
    this.allocator = new ReferenceCasualtyAllocator();
  }

  @Override
  public SimulationResults simulate(
      final BattleScenario scenario, final int runCount, final RandomSource rng) {
    final ProfileStats stats = new ProfileStats(costByProfile(scenario));
    final List<BattleResult> results = new ArrayList<>(runCount);
    for (int run = 0; run < runCount; run++) {
      results.add(runOnce(scenario, stats, rng));
    }
    return new SimulationResults(results);
  }

  /** One Monte-Carlo battle: rounds until a side is gone, a side withdraws, or the cap is hit. */
  private BattleResult runOnce(
      final BattleScenario scenario, final ProfileStats stats, final RandomSource rng) {
    final Map<Key, Integer> attackers = new LinkedHashMap<>(scenario.attackers().counts());
    final Map<Key, Integer> defenders = new LinkedHashMap<>(scenario.defenders().counts());
    // No-battle short-circuit (engine MustFightBattle.fight): a side whose units are all
    // infrastructure cannot be taken as a combat casualty, so the engine skips the battle entirely
    // — no AA fires — and the other side takes the territory untouched. The engine tests the
    // attacker first, so an all-infrastructure attacker resolves as a defender win even against an
    // all-infrastructure defender.
    if (allInfrastructure(attackers)) {
      return noBattle(attackers, defenders, Outcome.DEFENDER_WINS);
    }
    if (allInfrastructure(defenders)) {
      return noBattle(attackers, defenders, Outcome.ATTACKER_WINS);
    }
    int round = 0;
    boolean withdrew = false;
    while (round < MAX_ROUNDS
        && activeCount(attackers) > 0
        && activeCount(defenders) > 0
        && !withdrew) {
      round++;
      withdrew = fightRound(scenario, attackers, defenders, round, stats, rng);
      // A side left with only unescorted transports loses them at round end under the restriction,
      // before the DEAD reconcile so their cargo cascades into the same drop.
      sweepUnescortedTransports(scenario, attackers, defenders);
      // Reconcile: the working map is the next round's force — DEAD buckets drop, ACTIVE and
      // WITHDRAWN survive; support and stats re-evaluate next round off the reduced force.
      dropDead(attackers);
      dropDead(defenders);
    }
    return new BattleResult(
        new Force(new LinkedHashMap<>(attackers)),
        new Force(new LinkedHashMap<>(defenders)),
        round,
        outcome(attackers, defenders));
  }

  /**
   * Runs one round's firing sequence across both sides, returning whether either side withdrew.
   *
   * <p>Each combat phase is a simultaneous exchange read off a snapshot taken at that phase's
   * start: AA and first strike fire off the round-start plan (nothing sea has died yet, so a first
   * striker the opposing first strike kills still gets its shot), then main combat is
   * <em>re-planned</em> from the post-first-strike working map so a unit killed in first strike
   * drops out and a unit damaged in first strike fires at its migrated profile. First-strike
   * negation is stable across the re-plan: a destroyer can die in neither AA (air only) nor first
   * strike (an un-negated sub implies the enemy fields no destroyer), so no group changes phase
   * mid-round.
   */
  private boolean fightRound(
      final BattleScenario scenario,
      final Map<Key, Integer> attackers,
      final Map<Key, Integer> defenders,
      final int round,
      final ProfileStats stats,
      final RandomSource rng) {
    final List<FireStep> aa = new ArrayList<>();
    final List<FireStep> firstStrike = new ArrayList<>();
    collectPhases(
        plan(scenario, Side.OFFENSE, attackers, defenders, round),
        true,
        EnumSet.of(Phase.AA, Phase.FIRST_STRIKE),
        aa,
        firstStrike);
    collectPhases(
        plan(scenario, Side.DEFENSE, defenders, attackers, round),
        false,
        EnumSet.of(Phase.AA, Phase.FIRST_STRIKE),
        aa,
        firstStrike);

    for (final FireStep step : aa) {
      fire(step, scenario, attackers, defenders, round, stats, rng);
    }
    for (final FireStep step : firstStrike) {
      fire(step, scenario, attackers, defenders, round, stats, rng);
    }

    // Submerge is a post-first-strike checkpoint (design §5): subs that took their sneak attack now
    // dive if no enemy destroyer pins them, migrating ACTIVE -> WITHDRAWN so main combat cannot
    // touch them. Gated relationally by CombatRelations, not by the retreat preference alone.
    submerge(
        Side.OFFENSE, scenario.attackerRetreat(), attackers, defenders, scenario.rules(), round);
    submerge(
        Side.DEFENSE, scenario.defenderRetreat(), defenders, attackers, scenario.rules(), round);

    // Re-plan main off the post-first-strike forces: the fresh evaluated vectors are the shared
    // exchange-start snapshot both sides' main groups roll from, now free of first-strike
    // casualties.
    final List<FireStep> main = new ArrayList<>();
    collectPhases(
        plan(scenario, Side.OFFENSE, attackers, defenders, round),
        true,
        EnumSet.of(Phase.GENERAL),
        main);
    collectPhases(
        plan(scenario, Side.DEFENSE, defenders, attackers, round),
        false,
        EnumSet.of(Phase.GENERAL),
        main);
    for (final FireStep step : main) {
      fire(step, scenario, attackers, defenders, round, stats, rng);
    }

    final BattleView view = new BattleView(new Force(attackers), new Force(defenders), round);
    final boolean attackerWithdrew = retreat(scenario.attackerRetreat(), attackers, view);
    final boolean defenderWithdrew = retreat(scenario.defenderRetreat(), defenders, view);
    return attackerWithdrew || defenderWithdrew;
  }

  private BattleRound plan(
      final BattleScenario scenario,
      final Side side,
      final Map<Key, Integer> firing,
      final Map<Key, Integer> enemy,
      final int round) {
    return resolver.plan(
        side, new Force(firing), new Force(enemy), scenario.rules(), scenario.support(), round);
  }

  /** Bins the planned groups whose phase is in {@code wanted} into the matching output lists. */
  private static void collectPhases(
      final BattleRound plan,
      final boolean offense,
      final EnumSet<Phase> wanted,
      final List<FireStep> aaOrMain,
      final List<FireStep> firstStrike) {
    for (final RollGroup group : plan.firing().sequencedKeySet()) {
      if (group.firing().isEmpty()) {
        continue;
      }
      final Phase phase = phaseOf(group);
      if (!wanted.contains(phase)) {
        continue;
      }
      final FireStep step = new FireStep(group, offense, phase);
      if (phase == Phase.FIRST_STRIKE) {
        firstStrike.add(step);
      } else {
        aaOrMain.add(step);
      }
    }
  }

  /** Single-list overload for the main phase, where no first-strike groups are collected. */
  private static void collectPhases(
      final BattleRound plan,
      final boolean offense,
      final EnumSet<Phase> wanted,
      final List<FireStep> main) {
    collectPhases(plan, offense, wanted, main, new ArrayList<>());
  }

  /**
   * The firing phase a group belongs to: an AA group targets air, an IMMEDIATE non-AA group is a
   * first strike firing off live counts, everything else is main combat firing off the snapshot (a
   * negated first striker lands here as DEFERRED, per the resolver).
   */
  private static Phase phaseOf(final RollGroup group) {
    if (group.firing().keySet().stream().anyMatch(p -> p.flags().contains(CombatFlag.IS_AA))) {
      return Phase.AA;
    }
    return group.firingMode() == FiringMode.IMMEDIATE ? Phase.FIRST_STRIKE : Phase.GENERAL;
  }

  /** Rolls one group's hits and applies the casualties to the opposing working map. */
  private void fire(
      final FireStep step,
      final BattleScenario scenario,
      final Map<Key, Integer> attackers,
      final Map<Key, Integer> defenders,
      final int round,
      final ProfileStats stats,
      final RandomSource rng) {
    final Map<Key, Integer> targetForce = step.offense() ? defenders : attackers;
    // The group's evaluated vector is the phase-start snapshot: first-strike groups carry the
    // round-start counts (so paired first strikes roll simultaneously), main groups carry the
    // re-planned post-first-strike counts. Both sides thus roll off a frozen exchange snapshot
    // while
    // casualties land per hit on the live target map.
    Map<CombatProfile, Integer> firing = step.group().firing();
    if (firing.isEmpty()) {
      return;
    }
    // AA caps its total dice at the live air-target count each round (engine
    // AaPowerStrengthAndRolls): rebuild the firing vector so each gun carries its capped share as
    // its roll count, letting the ordinary roller roll the capped total. An infinite gun that
    // stably
    // baked one static die otherwise under-fires; a many-guns-vs-few-air stack over-fires.
    if (step.phase() == Phase.AA) {
      final int airTargets =
          liveAirTargetCount(targetForce, step.group().target().eligibleTargets());
      firing =
          AaFireCap.cappedFiring(
              firing, airTargets, p -> step.offense() ? p.attack() : p.defense());
      if (firing.isEmpty()) {
        return;
      }
    }
    final FireContext ctx =
        new FireContext(
            round, step.phase(), step.offense(), scenario.lowLuck(), scenario.diceSides());
    final int hits = roller.roll(firing, ctx, rng);
    if (hits == 0) {
      return;
    }
    final Side targetSide = step.offense() ? Side.DEFENSE : Side.OFFENSE;
    final CasualtyOrder order =
        step.offense() ? scenario.defenderOrder() : scenario.attackerOrder();
    // keep-one-land gating (amphibious/retreat-aware) is a deferred fidelity item; a bare false
    // never withholds the last land unit, which is correct wherever no non-land alternative exists.
    final Constraints constraints =
        new Constraints(false, scenario.rules().transportCasualtiesRestricted());
    // Support-adjusted casualty order (engine CasualtyOrderOfLosses): rank the taking side by the
    // power its force loses when each unit dies, so a supported unit and a supporter both cost more
    // to lose than their bare stat. Computed off this exchange's live target force with the firing
    // side as its enemy (for enemy-debuff support), then handed to the order as effective power.
    final ProfileStats fireStats =
        new ProfileStats(
            stats.cost(),
            marginalPowerLoss(
                activeProfileCounts(targetForce),
                new Force(step.offense() ? attackers : defenders),
                targetSide,
                scenario.support(),
                round,
                scenario.diceSides()));
    final Force after =
        allocator.allocate(
            new Force(targetForce),
            hits,
            step.group().target(),
            scenario.dependents(),
            constraints,
            order,
            step.group().firingMode(),
            targetSide,
            fireStats);
    targetForce.clear();
    targetForce.putAll(after.counts());
  }

  /** Dives the submergeable slice of a side when the rules and enemy composition allow it. */
  private void submerge(
      final Side side,
      final RetreatPolicy policy,
      final Map<Key, Integer> working,
      final Map<Key, Integer> enemy,
      final RulesProfile rules,
      final int round) {
    final Map<CombatProfile, Integer> cohort = activeProfileCounts(working);
    if (!relations.canSubmerge(side, cohort, new Force(enemy), rules)) {
      return;
    }
    final Map<CombatProfile, Integer> diving =
        policy.withdraw(cohort, RetreatCheckpoint.SUBMERGE, viewFor(working, enemy, round));
    migrate(working, diving);
  }

  /** Withdraws a side at round end per its policy; returns whether anything withdrew. */
  private static boolean retreat(
      final RetreatPolicy policy, final Map<Key, Integer> side, final BattleView view) {
    final Map<CombatProfile, Integer> cohort = activeProfileCounts(side);
    if (cohort.isEmpty()) {
      return false;
    }
    final Map<CombatProfile, Integer> withdrawn =
        policy.withdraw(cohort, RetreatCheckpoint.END_OF_ROUND, view);
    if (withdrawn.isEmpty()) {
      return false;
    }
    migrate(side, withdrawn);
    return true;
  }

  /** Migrates the given profile counts from ACTIVE to WITHDRAWN on the working map. */
  private static void migrate(
      final Map<Key, Integer> working, final Map<CombatProfile, Integer> profiles) {
    profiles.forEach(
        (profile, count) -> {
          final Key activeKey = new Key(profile, Lifecycle.ACTIVE);
          final int available = working.getOrDefault(activeKey, 0);
          final int moved = Math.min(available, count);
          if (moved <= 0) {
            return;
          }
          final int remaining = available - moved;
          if (remaining == 0) {
            working.remove(activeKey);
          } else {
            working.put(activeKey, remaining);
          }
          working.merge(new Key(profile, Lifecycle.WITHDRAWN), moved, Integer::sum);
        });
  }

  private static BattleView viewFor(
      final Map<Key, Integer> side, final Map<Key, Integer> enemy, final int round) {
    return new BattleView(new Force(side), new Force(enemy), round);
  }

  /**
   * Round-end removal of a side left with only unescorted transports while the enemy can still
   * fire, cascading each swept transport's cargo. Mirrors {@code RemoveUnprotectedUnits}: gated on
   * the restriction, run DEFENSE then OFFENSE.
   *
   * <p>Three v1 fidelity gaps, all unobserved in the harness: only the transport-only case is
   * modeled, not the broad unprotected-units removal ({@code
   * RemoveUnprotectedUnits#checkUnprotectedUnits}); this runs after the round's retreat/submerge
   * whereas the engine removes transports (Order 72) before retreat (Order 75); and the OFFENSE
   * exemption keys on air alone whereas the engine also exempts an attacker with a retreat
   * territory. The last two are masked because the harness has no movement routes, so no retreat
   * territory exists.
   */
  private void sweepUnescortedTransports(
      final BattleScenario scenario,
      final Map<Key, Integer> attackers,
      final Map<Key, Integer> defenders) {
    if (!scenario.rules().transportCasualtiesRestricted()) {
      return;
    }
    sweepSide(Side.DEFENSE, defenders, attackers, scenario.dependents());
    sweepSide(Side.OFFENSE, attackers, defenders, scenario.dependents());
  }

  private void sweepSide(
      final Side side,
      final Map<Key, Integer> working,
      final Map<Key, Integer> enemy,
      final Dependents deps) {
    // The attacker can retreat rather than die, so an OFFENSE side still holding air is exempt
    // (RemoveUnprotectedUnits#attackerHasRetreat); retreat-territory availability is not modeled.
    if (side == Side.OFFENSE && hasActiveAir(working)) {
      return;
    }
    if (!onlyUnescortedTransportsLeft(working) || !enemyHasActiveFirepower(enemy)) {
      return;
    }
    for (final CombatProfile transport : activeTransports(working)) {
      final int count = working.remove(new Key(transport, Lifecycle.ACTIVE));
      working.merge(new Key(transport, Lifecycle.DEAD), count, Integer::sum);
      for (int shed = 0; shed < count; shed++) {
        ReferenceCasualtyAllocator.cascade(working, transport, deps);
      }
    }
  }

  /**
   * A side is sweepable when it holds at least one ACTIVE transport and no other ACTIVE fighter.
   * Package-private so {@code TransportSweepTest} can pin the trigger in isolation.
   */
  static boolean onlyUnescortedTransportsLeft(final Map<Key, Integer> working) {
    boolean anyTransport = false;
    for (final Map.Entry<Key, Integer> entry : working.entrySet()) {
      if (entry.getKey().state() != Lifecycle.ACTIVE || entry.getValue() <= 0) {
        continue;
      }
      final CombatProfile profile = entry.getKey().profile();
      if (isDependent(profile)) {
        continue;
      }
      if (isTransport(profile)) {
        anyTransport = true;
      } else {
        return false;
      }
    }
    return anyTransport;
  }

  // Approximates the engine's getEnemyUnitsThatCanFire: an active non-transport combatant. A
  // transport's attack of 0 makes it unable to shoot the swept fleet, so it is not firepower here.
  // Over-counts vs the engine, which also requires movement > 0 and a positive attack — a pure
  // attack-0 or immobile sea unit would be excluded there but is counted here; REVISED fields no
  // such unit, so the divergence is unobserved (a v1 gap).
  private static boolean enemyHasActiveFirepower(final Map<Key, Integer> enemy) {
    return enemy.entrySet().stream()
        .anyMatch(
            entry ->
                entry.getKey().state() == Lifecycle.ACTIVE
                    && entry.getValue() > 0
                    && !isTransport(entry.getKey().profile())
                    && !isDependent(entry.getKey().profile()));
  }

  private static boolean hasActiveAir(final Map<Key, Integer> working) {
    return working.entrySet().stream()
        .anyMatch(
            entry ->
                entry.getKey().state() == Lifecycle.ACTIVE
                    && entry.getValue() > 0
                    && entry.getKey().profile().domain() == Domain.AIR);
  }

  private static List<CombatProfile> activeTransports(final Map<Key, Integer> working) {
    final List<CombatProfile> transports = new ArrayList<>();
    for (final Map.Entry<Key, Integer> entry : working.entrySet()) {
      if (entry.getKey().state() == Lifecycle.ACTIVE
          && entry.getValue() > 0
          && isTransport(entry.getKey().profile())) {
        transports.add(entry.getKey().profile());
      }
    }
    return transports;
  }

  private static boolean isTransport(final CombatProfile profile) {
    return profile.flags().contains(CombatFlag.IS_TRANSPORT);
  }

  private static boolean isDependent(final CombatProfile profile) {
    return profile.flags().contains(CombatFlag.IS_DEPENDENT);
  }

  /**
   * The battle verdict from what remains: withdrawn units are survivors, so a side is beaten only
   * when nothing of it is left; both sides surviving (cap or a stalemate withdrawal) reads as a
   * draw.
   */
  private static Outcome outcome(
      final Map<Key, Integer> attackers, final Map<Key, Integer> defenders) {
    final int attackerLeft = totalCount(attackers);
    final int defenderLeft = totalCount(defenders);
    if (defenderLeft == 0 && attackerLeft > 0) {
      return Outcome.ATTACKER_WINS;
    }
    if (attackerLeft == 0 && defenderLeft > 0) {
      return Outcome.DEFENDER_WINS;
    }
    return Outcome.DRAW;
  }

  /** Both sides pass through untouched with the given verdict and no rounds fought. */
  private static BattleResult noBattle(
      final Map<Key, Integer> attackers, final Map<Key, Integer> defenders, final Outcome outcome) {
    return new BattleResult(
        new Force(new LinkedHashMap<>(attackers)),
        new Force(new LinkedHashMap<>(defenders)),
        0,
        outcome);
  }

  /**
   * Whether a side fields at least one unit and every one is infrastructure — the engine's
   * no-battle trigger (all defenders match {@code Matches.unitIsInfrastructure}). An empty side
   * falls through to the ordinary outcome instead.
   */
  private static boolean allInfrastructure(final Map<Key, Integer> side) {
    boolean any = false;
    for (final Map.Entry<Key, Integer> entry : side.entrySet()) {
      if (entry.getKey().state() != Lifecycle.ACTIVE || entry.getValue() <= 0) {
        continue;
      }
      any = true;
      if (!entry.getKey().profile().flags().contains(CombatFlag.IS_INFRASTRUCTURE)) {
        return false;
      }
    }
    return any;
  }

  /**
   * Cost keyed by profile for the casualty order, walking each damage chain to its DEAD sentinel.
   */
  private static Map<CombatProfile, Integer> costByProfile(final BattleScenario scenario) {
    final Map<CombatProfile, Integer> byProfile = new LinkedHashMap<>();
    for (final Force force :
        List.of(scenario.attackers(), scenario.defenders(), scenario.bombarding())) {
      for (final Key key : force.counts().keySet()) {
        for (CombatProfile profile = key.profile(); profile != null; profile = profile.next()) {
          byProfile.putIfAbsent(profile, scenario.cost().getOrDefault(profile.type(), 0));
        }
      }
    }
    return byProfile;
  }

  /**
   * The support-adjusted worth of each target profile: the side-relative power its force loses when
   * one such unit dies, re-resolving support with the unit removed. This folds both channels the
   * engine's {@code CasualtyOrderOfLosses} uses — the support a unit receives (its own boosted
   * power) and the support it gives others (their power drops when it is gone) — into one figure
   * the casualty order can rank on. Computed once per firing exchange; a supporter killed
   * mid-exchange is not re-scored until the next round (the engine re-scores per hit — a documented
   * v1 gap).
   */
  private Map<CombatProfile, Integer> marginalPowerLoss(
      final Map<CombatProfile, Integer> target,
      final Force enemy,
      final Side side,
      final List<SupportRule> support,
      final int round,
      final int diceSides) {
    if (target.isEmpty()) {
      return Map.of();
    }
    final int full = resolvedPower(target, enemy, side, support, round, diceSides);
    final Map<CombatProfile, Integer> marginal = new LinkedHashMap<>();
    for (final CombatProfile profile : target.keySet()) {
      final Map<CombatProfile, Integer> without = new LinkedHashMap<>(target);
      final int count = without.get(profile);
      if (count <= 1) {
        without.remove(profile);
      } else {
        without.put(profile, count - 1);
      }
      marginal.put(profile, full - resolvedPower(without, enemy, side, support, round, diceSides));
    }
    return marginal;
  }

  /** Total side-relative firing power of a profile cohort after support is resolved onto it. */
  private int resolvedPower(
      final Map<CombatProfile, Integer> cohort,
      final Force enemy,
      final Side side,
      final List<SupportRule> support,
      final int round,
      final int diceSides) {
    if (cohort.isEmpty()) {
      return 0;
    }
    final Map<CombatProfile, Integer> resolved =
        supportResolver.resolve(activeForceOf(cohort), enemy, side, support, round);
    int power = 0;
    for (final Map.Entry<CombatProfile, Integer> entry : resolved.entrySet()) {
      final CombatProfile profile = entry.getKey();
      final int stat = side == Side.OFFENSE ? profile.attack() : profile.defense();
      power += Math.min(Math.max(stat, 0), diceSides) * profile.rolls() * entry.getValue();
    }
    return power;
  }

  private static Force activeForceOf(final Map<CombatProfile, Integer> profiles) {
    final Map<Key, Integer> counts = new LinkedHashMap<>();
    profiles.forEach(
        (profile, count) -> {
          if (count > 0) {
            counts.put(new Key(profile, Lifecycle.ACTIVE), count);
          }
        });
    return new Force(counts);
  }

  /** Live count of ACTIVE enemy units this AA group may target — the round's air-target cap. */
  private static int liveAirTargetCount(
      final Map<Key, Integer> targetForce, final Set<CombatProfile> eligible) {
    int count = 0;
    for (final Map.Entry<Key, Integer> entry : targetForce.entrySet()) {
      if (entry.getKey().state() == Lifecycle.ACTIVE
          && eligible.contains(entry.getKey().profile())) {
        count += entry.getValue();
      }
    }
    return count;
  }

  private static Map<CombatProfile, Integer> activeProfileCounts(final Map<Key, Integer> working) {
    final Map<CombatProfile, Integer> counts = new LinkedHashMap<>();
    for (final Map.Entry<Key, Integer> entry : working.entrySet()) {
      if (entry.getKey().state() == Lifecycle.ACTIVE && entry.getValue() > 0) {
        counts.merge(entry.getKey().profile(), entry.getValue(), Integer::sum);
      }
    }
    return counts;
  }

  private static int activeCount(final Map<Key, Integer> working) {
    return working.entrySet().stream()
        .filter(entry -> entry.getKey().state() == Lifecycle.ACTIVE)
        .mapToInt(Map.Entry::getValue)
        .sum();
  }

  private static int totalCount(final Map<Key, Integer> working) {
    return working.values().stream().mapToInt(Integer::intValue).sum();
  }

  private static void dropDead(final Map<Key, Integer> working) {
    working.keySet().removeIf(key -> key.state() == Lifecycle.DEAD);
  }

  /** One planned group tagged with the side firing it and the phase it belongs to. */
  private record FireStep(RollGroup group, boolean offense, Phase phase) {}
}
