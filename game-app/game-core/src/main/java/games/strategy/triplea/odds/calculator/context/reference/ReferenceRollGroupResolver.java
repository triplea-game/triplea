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
   * Plans the firing side ({@code attackers}) for one round; first-strike eligibility is recomputed
   * from {@link CombatRelations} every call, so a round whose enemy destroyer has died restores the
   * IMMEDIATE first strike the previous round denied.
   */
  @Override
  public BattleRound plan(
      final Force attackers,
      final Force defenders,
      final RulesProfile rules,
      final List<SupportRule> support,
      final int round) {
    final Side side = Side.OFFENSE;
    final Map<CombatProfile, Integer> active = activeProfileCounts(attackers);
    final boolean firstStrikeNegated = relations.firstStrikeNegated(side, attackers, defenders);

    final Map<CombatProfile, Integer> aa = new LinkedHashMap<>();
    final Map<CombatProfile, Integer> firstStrike = new LinkedHashMap<>();
    final Map<CombatProfile, Integer> main = new LinkedHashMap<>();
    for (final Map.Entry<CombatProfile, Integer> entry : active.entrySet()) {
      final CombatProfile profile = entry.getKey();
      if (profile.flags().contains(CombatFlag.IS_AA)) {
        aa.put(profile, entry.getValue());
      } else if (profile.flags().contains(CombatFlag.FIRST_STRIKE) && !firstStrikeNegated) {
        firstStrike.put(profile, entry.getValue());
      } else {
        // A negated first striker joins main combat, where it fires DEFERRED off the snapshot.
        main.put(profile, entry.getValue());
      }
    }

    final SequencedMap<RollGroup, Set<RollGroup>> firing = new LinkedHashMap<>();
    addGroup(firing, side, attackers, defenders, aa, support, FiringMode.IMMEDIATE, round);
    addGroup(firing, side, attackers, defenders, firstStrike, support, FiringMode.IMMEDIATE, round);
    addGroup(firing, side, attackers, defenders, main, support, FiringMode.DEFERRED, round);
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
    // Targeting reads the enemy composition, so the filter is derived from CombatRelations off a
    // preliminary group rather than left empty; the fired group carries the resulting TargetFilter.
    final RollGroup unfiltered =
        new RollGroup(side, fired, new TargetFilter(Set.of()), mode, DiceMode.NORMAL);
    final TargetFilter target = relations.eligibleTargets(unfiltered, friendly, enemy);
    firing.put(new RollGroup(side, fired, target, mode, DiceMode.NORMAL), Set.of());
  }

  private static Map<CombatProfile, Integer> activeProfileCounts(final Force force) {
    final Map<CombatProfile, Integer> counts = new LinkedHashMap<>();
    for (final Map.Entry<Key, Integer> entry : force.counts().entrySet()) {
      if (entry.getKey().state() == Lifecycle.ACTIVE) {
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
