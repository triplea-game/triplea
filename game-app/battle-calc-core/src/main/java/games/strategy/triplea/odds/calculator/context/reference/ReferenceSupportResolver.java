package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.SupportRule;
import games.strategy.triplea.odds.calculator.context.seam.SupportResolver;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reference {@link SupportResolver}: allocates each round's support force-wide and returns the
 * evaluated profile counts a firing group rolls with. Scarce support goes to the strongest
 * recipients first, mirroring the engine's {@code PowerStrengthAndRolls} "sort strongest to weakest
 * so the best units get support first" order; a boosted body migrates to a distinct evaluated
 * profile with the bonus baked into its stat.
 */
public class ReferenceSupportResolver implements SupportResolver {

  @Override
  public Map<CombatProfile, Integer> resolve(
      final Force force,
      final Force enemy,
      final Side side,
      final List<SupportRule> rules,
      final int round) {
    final Map<CombatProfile, Integer> evaluated = activeProfileCounts(force);
    for (final SupportRule rule : rules) {
      if (applies(rule, side, round)) {
        applyRule(evaluated, rule, side);
      }
    }
    return evaluated;
  }

  /**
   * A rule fires only for its own {@link Side}, and a {@code firstRoundOnly} rule only at round 1.
   */
  private static boolean applies(final SupportRule rule, final Side side, final int round) {
    return rule.side() == side && (!rule.firstRoundOnly() || round == 1);
  }

  private static void applyRule(
      final Map<CombatProfile, Integer> evaluated, final SupportRule rule, final Side side) {
    final int givers =
        evaluated.entrySet().stream()
            .filter(e -> e.getKey().gives().equals(rule.from()))
            .mapToInt(Map.Entry::getValue)
            .sum();
    int uses = rule.usesPerGiver() * givers;
    if (uses <= 0) {
      return;
    }
    // Snapshot the recipients before mutating so migrated (boosted) profiles are not re-boosted,
    // and consume strongest-base-first so scarce support lands on the best units.
    final List<CombatProfile> recipients =
        evaluated.keySet().stream()
            .filter(profile -> profile.receives().equals(rule.to()))
            .sorted(strongestFirst(side))
            .toList();
    for (final CombatProfile recipient : recipients) {
      if (uses <= 0) {
        break;
      }
      final int available = evaluated.get(recipient);
      final int boosted = Math.min(uses, available);
      final int remaining = available - boosted;
      if (remaining == 0) {
        evaluated.remove(recipient);
      } else {
        evaluated.put(recipient, remaining);
      }
      evaluated.merge(boost(recipient, rule, side), boosted, Integer::sum);
      uses -= boosted;
    }
  }

  /** Aggregates the force's ACTIVE units by profile — withdrawn/dead buckets do not fire. */
  private static Map<CombatProfile, Integer> activeProfileCounts(final Force force) {
    final Map<CombatProfile, Integer> counts = new LinkedHashMap<>();
    for (final Map.Entry<Key, Integer> entry : force.counts().entrySet()) {
      if (entry.getKey().state() == Lifecycle.ACTIVE) {
        counts.merge(entry.getKey().profile(), entry.getValue(), Integer::sum);
      }
    }
    return counts;
  }

  private static Comparator<CombatProfile> strongestFirst(final Side side) {
    return Comparator.comparingInt((final CombatProfile p) -> baseStrength(p, side)).reversed();
  }

  private static int baseStrength(final CombatProfile profile, final Side side) {
    return side == Side.OFFENSE ? profile.attack() : profile.defense();
  }

  /**
   * The evaluated profile a boosted recipient becomes: a strength rule bumps the side-relevant
   * stack (attack on offense, defense on defense), otherwise the bonus adds firing rolls.
   */
  private static CombatProfile boost(
      final CombatProfile base, final SupportRule rule, final Side side) {
    final boolean strength = rule.appliesToStrength();
    final int attack =
        strength && side == Side.OFFENSE ? base.attack() + rule.bonus() : base.attack();
    final int defense =
        strength && side == Side.DEFENSE ? base.defense() + rule.bonus() : base.defense();
    final int rolls = strength ? base.rolls() : base.rolls() + rule.bonus();
    return new CombatProfile(
        base.type(),
        attack,
        defense,
        rolls,
        base.hitPoints(),
        base.domain(),
        base.damage(),
        base.gives(),
        base.receives(),
        base.flags(),
        base.next());
  }
}
