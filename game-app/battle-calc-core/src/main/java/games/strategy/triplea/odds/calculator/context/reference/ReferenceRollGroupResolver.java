package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.BattleRound;
import games.strategy.triplea.odds.calculator.context.model.CombatFlag;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.DiceMode;
import games.strategy.triplea.odds.calculator.context.model.FiringMode;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.RollGroup;
import games.strategy.triplea.odds.calculator.context.model.RulesProfile;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.SupportRule;
import games.strategy.triplea.odds.calculator.context.model.TargetFilter;
import games.strategy.triplea.odds.calculator.context.seam.CombatRelations;
import games.strategy.triplea.odds.calculator.context.seam.RollGroupResolver;
import games.strategy.triplea.odds.calculator.context.seam.SupportResolver;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;

/**
 * Reference firing-plan resolver; partitions the firing side into the engine's fixed firing
 * sequence (AA, then first strike, then main combat — mirroring {@code MustFightBattle}'s aa ->
 * firststrike -> general step order) and evaluates each partition's support through the injected
 * seams rather than matching unit names.
 */
public class ReferenceRollGroupResolver implements RollGroupResolver {
  private final SupportResolver supportResolver;
  private final CombatRelations relations;

  public ReferenceRollGroupResolver(
      final SupportResolver supportResolver, final CombatRelations relations) {
    this.supportResolver = supportResolver;
    this.relations = relations;
  }

  /**
   * Plans the firing side ({@code friendly}) for one round; first-strike eligibility is recomputed
   * from {@link CombatRelations} every call, so a round whose enemy destroyer has died restores the
   * IMMEDIATE first strike the previous round denied.
   */
  @Override
  public BattleRound plan(
      final Side side,
      final Force friendly,
      final Force enemy,
      final RulesProfile rules,
      final List<SupportRule> support,
      final int round) {
    final Map<CombatProfile, Integer> active = activeProfileCounts(friendly);
    final boolean firstStrikeNegated = relations.firstStrikeNegated(side, friendly, enemy, rules);

    final Map<CombatProfile, Integer> aa = new LinkedHashMap<>();
    final Map<CombatProfile, Integer> firstStrike = new LinkedHashMap<>();
    final Map<CombatProfile, Integer> main = new LinkedHashMap<>();
    for (final Map.Entry<CombatProfile, Integer> entry : active.entrySet()) {
      final CombatProfile profile = entry.getKey();
      if (profile.flags().contains(CombatFlag.IS_AA)) {
        // AA fires only through its 'maxRoundsAa' round (default 1); past it the gun drops out of
        // every partition — it neither re-fires (the engine's per-round gate) nor joins main, since
        // a pure gun has no main-phase attack. -1 means it fires every round.
        if (profile.maxRoundsAa() < 0 || round <= profile.maxRoundsAa()) {
          aa.put(profile, entry.getValue());
        }
      } else if (profile.flags().contains(CombatFlag.FIRST_STRIKE) && !firstStrikeNegated) {
        firstStrike.put(profile, entry.getValue());
      } else {
        // A negated first striker joins main combat, where it fires DEFERRED off the snapshot.
        main.put(profile, entry.getValue());
      }
    }

    final SequencedMap<RollGroup, Set<RollGroup>> firing = new LinkedHashMap<>();
    addGroup(firing, side, friendly, enemy, aa, support, FiringMode.IMMEDIATE, round);
    addGroup(firing, side, friendly, enemy, firstStrike, support, FiringMode.IMMEDIATE, round);
    addGroup(firing, side, friendly, enemy, main, support, FiringMode.DEFERRED, round);
    return new BattleRound(firing);
  }

  private void addGroup(
      final SequencedMap<RollGroup, Set<RollGroup>> firing,
      final Side side,
      final Force friendly,
      final Force enemy,
      final Map<CombatProfile, Integer> partition,
      final List<SupportRule> support,
      final FiringMode mode,
      final int round) {
    if (partition.isEmpty()) {
      return;
    }
    final Force partitionForce = activeForce(partition);
    final Map<CombatProfile, Integer> evaluated =
        supportResolver.resolve(partitionForce, enemy, side, support, round);
    // An empty evaluation means the support resolver applied nothing — eg no rule matched — so the
    // group falls back to its base counts.
    final Map<CombatProfile, Integer> fired = evaluated.isEmpty() ? partition : evaluated;

    // Split the partition per firer by its eligible-target set, mirroring
    // TargetGroup.newTargetGroups: firers that reach the same enemy profiles share one RollGroup.
    // The load-bearing invariant is collapse — a homogeneous partition, every firer eligible for the
    // same targets, yields exactly ONE group identical to the pre-split plan, so dice and casualty
    // flow are unchanged wherever no sub/air asymmetry applies; fragmentation here would perturb
    // every scenario, not just the immunity ones. A firer whose eligible set is empty shoots nothing
    // this round and contributes no group (the engine's 'if (targets.isEmpty()) continue').
    final Map<Set<CombatProfile>, Map<CombatProfile, Integer>> buckets = new LinkedHashMap<>();
    for (final Map.Entry<CombatProfile, Integer> entry : fired.entrySet()) {
      final Set<CombatProfile> eligible =
          relations.eligibleTargets(entry.getKey(), friendly, enemy).eligibleTargets();
      if (eligible.isEmpty()) {
        continue;
      }
      buckets
          .computeIfAbsent(eligible, key -> new LinkedHashMap<>())
          .put(entry.getKey(), entry.getValue());
    }

    // Fewest-targets-first. Sub-group order is observable through target depletion as casualties
    // land on the live map, so it must match the engine — but the engine's key is not size:
    // 'FiringGroupSplitterGeneral' fires the air-vs-sub group ('AIR_FIRE_NON_SUBS') first, then the
    // rest. Fewest-first coincides with that only because the air group's target set is always a
    // subset of every surface firer's (air targets = all enemies minus immune subs), so "air-first"
    // and "fewest-first" pick the same order in any air+surface partition; this relies on that
    // subset coincidence, not on size being the engine's ordering key.
    buckets.entrySet().stream()
        .sorted(Comparator.comparingInt(bucket -> bucket.getKey().size()))
        .forEach(
            bucket ->
                firing.put(
                    new RollGroup(
                        side,
                        bucket.getValue(),
                        new TargetFilter(bucket.getKey()),
                        mode,
                        DiceMode.NORMAL),
                    Set.of()));
  }

  /**
   * The firing side's active profiles. Dependent cargo is a non-combatant and is dropped here, so
   * it never joins an AA, first-strike, or main partition and thus never fires.
   */
  private static Map<CombatProfile, Integer> activeProfileCounts(final Force force) {
    final Map<CombatProfile, Integer> counts = new LinkedHashMap<>();
    for (final Map.Entry<Key, Integer> entry : force.counts().entrySet()) {
      if (entry.getKey().state() == Lifecycle.ACTIVE
          && !entry.getKey().profile().flags().contains(CombatFlag.IS_DEPENDENT)) {
        counts.merge(entry.getKey().profile(), entry.getValue(), Integer::sum);
      }
    }
    return counts;
  }

  private static Force activeForce(final Map<CombatProfile, Integer> partition) {
    final Map<Key, Integer> counts = new LinkedHashMap<>();
    partition.forEach((profile, count) -> counts.put(new Key(profile, Lifecycle.ACTIVE), count));
    return new Force(counts);
  }
}
