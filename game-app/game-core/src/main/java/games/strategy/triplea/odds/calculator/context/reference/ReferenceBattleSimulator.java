package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.BattleResult;
import games.strategy.triplea.odds.calculator.context.model.BattleRound;
import games.strategy.triplea.odds.calculator.context.model.BattleScenario;
import games.strategy.triplea.odds.calculator.context.model.BattleView;
import games.strategy.triplea.odds.calculator.context.model.CombatFlag;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Constraints;
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
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.SimulationResults;
import games.strategy.triplea.odds.calculator.context.seam.BattleSimulator;
import games.strategy.triplea.odds.calculator.context.seam.CasualtyAllocator;
import games.strategy.triplea.odds.calculator.context.seam.CasualtyOrder;
import games.strategy.triplea.odds.calculator.context.seam.CombatRelations;
import games.strategy.triplea.odds.calculator.context.seam.HitRoller;
import games.strategy.triplea.odds.calculator.context.seam.RandomSource;
import games.strategy.triplea.odds.calculator.context.seam.RetreatPolicy;
import games.strategy.triplea.odds.calculator.context.seam.RollGroupResolver;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The reference per-round battle pipeline (design §5): composes the reference resolver, roller,
 * allocator, and retreat/relations seams into the Monte-Carlo loop the vectorized simulator will
 * later replace behind the same {@link BattleSimulator} seam. Stays {@code GameData}-free — the
 * adapter bakes every rule into the scenario, so nothing here reaches back into the engine.
 *
 * <p>{@code plan} is per-firing-side: each round plans the attackers and the defenders separately,
 * then interleaves their groups into one true firing sequence (AA, then first strike, then main).
 * The simulator owns the shared exchange-start snapshot so both sides' DEFERRED main groups roll
 * from pre-exchange counts while casualties land on the live working map as they are dealt.
 */
public class ReferenceBattleSimulator implements BattleSimulator {

  // A legitimate battle converges in a handful of rounds; the cap only bounds a pathological
  // scenario where neither side can eliminate the other (eg forces that can never target one
  // another) so a run cannot spin forever. Reaching it ends the round loop as a stalemate.
  private static final int MAX_ROUNDS = 100;

  // The scenario carries neither dice sides nor a low-luck flag yet, so the reference bakes in
  // standard six-sided normal dice, threaded through by the adapter once low-luck lands. Under
  // alwaysHits the value is inert — every die reads 0 and hits any strength >= 1.
  private static final int DICE_SIDES = 6;
  private static final boolean LOW_LUCK = false;

  private final RollGroupResolver resolver;
  private final HitRoller roller;
  private final CasualtyAllocator allocator;
  private final CombatRelations relations;

  public ReferenceBattleSimulator() {
    this.relations = new ReferenceCombatRelations();
    this.resolver = new ReferenceRollGroupResolver(new ReferenceSupportResolver(), relations);
    this.roller = new DiceHitRoller();
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
    int round = 0;
    boolean withdrew = false;
    while (round < MAX_ROUNDS
        && activeCount(attackers) > 0
        && activeCount(defenders) > 0
        && !withdrew) {
      round++;
      withdrew = fightRound(scenario, attackers, defenders, round, stats, rng);
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
   * Both plans are derived from the round-start forces, so a DEFERRED group's evaluated vector is
   * already the exchange-start snapshot; an IMMEDIATE group re-reads the live working map instead.
   */
  private boolean fightRound(
      final BattleScenario scenario,
      final Map<Key, Integer> attackers,
      final Map<Key, Integer> defenders,
      final int round,
      final ProfileStats stats,
      final RandomSource rng) {
    final BattleRound attackPlan =
        resolver.plan(
            new Force(attackers),
            new Force(defenders),
            scenario.rules(),
            scenario.support(),
            round);
    final BattleRound defendPlan =
        resolver.plan(
            new Force(defenders),
            new Force(attackers),
            scenario.rules(),
            scenario.support(),
            round);

    final List<FireStep> aa = new ArrayList<>();
    final List<FireStep> firstStrike = new ArrayList<>();
    final List<FireStep> main = new ArrayList<>();
    collect(attackPlan, true, aa, firstStrike, main);
    collect(defendPlan, false, aa, firstStrike, main);

    for (final FireStep step : aa) {
      fire(step, scenario, attackers, defenders, round, stats, rng);
    }
    for (final FireStep step : firstStrike) {
      fire(step, scenario, attackers, defenders, round, stats, rng);
    }

    // Submerge is a post-first-strike checkpoint (design §5): subs that took their sneak attack now
    // dive if no enemy destroyer pins them, migrating ACTIVE -> WITHDRAWN so main combat cannot
    // touch them. Gated relationally by CombatRelations, not by the retreat preference alone.
    submerge(scenario.attackerRetreat(), attackers, defenders, round);
    submerge(scenario.defenderRetreat(), defenders, attackers, round);

    for (final FireStep step : main) {
      fire(step, scenario, attackers, defenders, round, stats, rng);
    }

    final BattleView view = new BattleView(new Force(attackers), new Force(defenders), round);
    final boolean attackerWithdrew = retreat(scenario.attackerRetreat(), attackers, view);
    final boolean defenderWithdrew = retreat(scenario.defenderRetreat(), defenders, view);
    return attackerWithdrew || defenderWithdrew;
  }

  /** Bins one side's planned groups into the shared AA / first-strike / main firing phases. */
  private static void collect(
      final BattleRound plan,
      final boolean offense,
      final List<FireStep> aa,
      final List<FireStep> firstStrike,
      final List<FireStep> main) {
    for (final RollGroup group : plan.firing().sequencedKeySet()) {
      if (group.firing().isEmpty()) {
        continue;
      }
      final Phase phase = phaseOf(group);
      final FireStep step = new FireStep(group, offense, phase);
      switch (phase) {
        case AA -> aa.add(step);
        case FIRST_STRIKE -> firstStrike.add(step);
        default -> main.add(step);
      }
    }
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
    final Map<Key, Integer> firingForce = step.offense() ? attackers : defenders;
    final Map<Key, Integer> targetForce = step.offense() ? defenders : attackers;
    final Map<CombatProfile, Integer> firing =
        step.group().firingMode() == FiringMode.IMMEDIATE
            ? liveFiring(step.group(), firingForce)
            : step.group().firing();
    if (firing.isEmpty()) {
      return;
    }
    final FireContext ctx =
        new FireContext(round, step.phase(), step.offense(), LOW_LUCK, DICE_SIDES);
    final int hits = roller.roll(firing, ctx, rng);
    if (hits == 0) {
      return;
    }
    final Side targetSide = step.offense() ? Side.DEFENSE : Side.OFFENSE;
    final CasualtyOrder order =
        step.offense() ? scenario.defenderOrder() : scenario.attackerOrder();
    // keep-one-land gating (amphibious/retreat-aware) is a deferred fidelity item; a bare false
    // never withholds the last land unit, which is correct wherever no non-land alternative exists.
    final Constraints constraints = new Constraints(false);
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
            stats);
    targetForce.clear();
    targetForce.putAll(after.counts());
  }

  /**
   * An IMMEDIATE group's firing vector against the current working map: a first striker killed
   * earlier this round fires fewer dice. Support-boosted evaluated profiles never appear raw in the
   * working map, so they fall back to their planned count rather than reading as zero.
   */
  private static Map<CombatProfile, Integer> liveFiring(
      final RollGroup group, final Map<Key, Integer> working) {
    final Map<CombatProfile, Integer> active = activeProfileCounts(working);
    final Map<CombatProfile, Integer> live = new LinkedHashMap<>();
    group
        .firing()
        .forEach(
            (profile, planned) -> {
              final Integer liveCount = active.get(profile);
              final int count = liveCount != null ? liveCount : planned;
              if (count > 0) {
                live.put(profile, count);
              }
            });
    return live;
  }

  /** Dives the submergeable slice of a side when no enemy destroyer pins it. */
  private void submerge(
      final RetreatPolicy policy,
      final Map<Key, Integer> side,
      final Map<Key, Integer> enemy,
      final int round) {
    final Map<CombatProfile, Integer> cohort = activeProfileCounts(side);
    if (!relations.canSubmerge(cohort, new Force(enemy))) {
      return;
    }
    final Map<CombatProfile, Integer> diving =
        policy.withdraw(cohort, RetreatCheckpoint.SUBMERGE, viewFor(side, enemy, round));
    migrate(side, diving);
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
