package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.BonusTypeId;
import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.Key;
import games.strategy.triplea.odds.calculator.context.model.Lifecycle;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.SupportRule;
import games.strategy.triplea.odds.calculator.context.seam.SupportResolver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reference {@link SupportResolver}: allocates a firing group's support and returns the evaluated
 * profile counts it rolls with. Mirrors the engine's {@code AvailableSupports.giveSupportToUnit}:
 * recipients are served strongest-base-first from a shared supporter pool, and a recipient takes at
 * most {@code maxPerReceiver} supports per {@link BonusTypeId} across every rule sharing that type,
 * each drawn from a <em>distinct</em> supporter. Strength support and roll support are allocated
 * independently (separate pools and caps), exactly as the engine builds separate {@code
 * AvailableSupports} for each. A boosted recipient migrates to a distinct evaluated profile with
 * the bonus baked into its stat or roll count.
 *
 * <p>Enemy (debuff) support is allocated alongside friendly support but from the opposing force's
 * supporters and with its own caps, and within a bonus-type group its rules apply worst (most
 * negative) first — mirroring the engine's separate friendly/enemy {@code AvailableSupports} and
 * its reversed {@code SupportRuleSort}. A recipient's total delta is the sum of both.
 */
public class ReferenceSupportResolver implements SupportResolver {

  // The engine's SupportRuleSort, restricted to the ordering that changes how much support a capped
  // receiver gets: the strongest bonus first, then fewest target unit-types first (so a
  // narrowly-targeted rule acts before a broad one consumes the shared supporters). The giver-power
  // tiebreak only steers casualty selection, so a stable category-name tiebreak stands in for it
  // and
  // keeps the order deterministic. Friendly orders highest-bonus first; enemy orders worst (most
  // negative) first, since the engine reverses the bonus comparison for the debuff pool.
  private static final Comparator<SupportRule> BY_TARGET_THEN_NAME =
      Comparator.comparingInt(SupportRule::targetTypeCount)
          .thenComparing(rule -> rule.from().name())
          .thenComparing(rule -> rule.to().name());
  private static final Comparator<SupportRule> WITHIN_BONUS_TYPE_FRIENDLY =
      Comparator.comparingInt(SupportRule::bonus).reversed().thenComparing(BY_TARGET_THEN_NAME);
  private static final Comparator<SupportRule> WITHIN_BONUS_TYPE_ENEMY =
      Comparator.comparingInt(SupportRule::bonus).thenComparing(BY_TARGET_THEN_NAME);

  @Override
  public Map<CombatProfile, Integer> resolve(
      final Force force,
      final Force enemy,
      final Side side,
      final List<SupportRule> rules,
      final int round) {
    final Map<CombatProfile, Integer> base = activeProfileCounts(force);
    final List<SupportRule> applicable =
        rules.stream().filter(rule -> applies(rule, side, round)).toList();
    if (applicable.isEmpty()) {
      return base;
    }
    final List<SupportRule> friendlyRules =
        applicable.stream().filter(r -> !r.fromEnemy()).toList();
    final List<SupportRule> enemyRules =
        applicable.stream().filter(SupportRule::fromEnemy).toList();
    // Enemy (debuff) supporters come from the opposing force; the engine builds a separate
    // AvailableSupports for each relationship, so friendly and enemy caps never share counters.
    final Map<CombatProfile, Integer> enemyBase = activeProfileCounts(enemy);
    final Allocation friendlyStrength =
        allocationFor(base, friendlyRules, true, WITHIN_BONUS_TYPE_FRIENDLY);
    final Allocation friendlyRolls =
        allocationFor(base, friendlyRules, false, WITHIN_BONUS_TYPE_FRIENDLY);
    final Allocation enemyStrength =
        allocationFor(enemyBase, enemyRules, true, WITHIN_BONUS_TYPE_ENEMY);
    final Allocation enemyRolls =
        allocationFor(enemyBase, enemyRules, false, WITHIN_BONUS_TYPE_ENEMY);

    final Map<CombatProfile, Integer> evaluated = new LinkedHashMap<>();
    final List<CombatProfile> receivers = new ArrayList<>();
    for (final CombatProfile profile : base.keySet()) {
      if (isReceiver(profile, applicable)) {
        receivers.add(profile);
      } else {
        evaluated.merge(profile, base.get(profile), Integer::sum);
      }
    }
    receivers.sort(strongestFirst(side));
    for (final CombatProfile receiver : receivers) {
      final int count = base.get(receiver);
      for (int i = 0; i < count; i++) {
        final int strengthBonus =
            friendlyStrength.giveTo(receiver) + enemyStrength.giveTo(receiver);
        final int rollBonus = friendlyRolls.giveTo(receiver) + enemyRolls.giveTo(receiver);
        evaluated.merge(boost(receiver, strengthBonus, rollBonus, side), 1, Integer::sum);
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

  private static boolean isReceiver(final CombatProfile profile, final List<SupportRule> rules) {
    return rules.stream().anyMatch(rule -> profile.receives().contains(rule.to()));
  }

  /**
   * One partition's (strength or roll) allocatable support: the rules that apply, grouped by bonus
   * type, plus a mutable per-rule supporter pool that depletes as recipients are served.
   */
  private static final class Allocation {
    private final Map<BonusTypeId, List<SupportRule>> byBonusType;
    // Per rule, one entry per distinct supporter unit, holding its remaining support points; a
    // supporter contributes at most one support per recipient, so serving a recipient decrements
    // distinct entries.
    private final Map<SupportRule, int[]> pool;

    Allocation(
        final Map<BonusTypeId, List<SupportRule>> byBonusType, final Map<SupportRule, int[]> pool) {
      this.byBonusType = byBonusType;
      this.pool = pool;
    }

    /** The total bonus this partition grants one recipient, consuming supporters as it goes. */
    int giveTo(final CombatProfile receiver) {
      int total = 0;
      for (final List<SupportRule> group : byBonusType.values()) {
        int maxPerBonusType = group.get(0).maxPerReceiver();
        for (final SupportRule rule : group) {
          if (!receiver.receives().contains(rule.to())) {
            continue;
          }
          final int available = Math.min(rule.maxPerReceiver(), distinctAvailable(pool.get(rule)));
          if (available > 0) {
            draw(pool.get(rule), available);
            total += available * rule.bonus();
          }
          maxPerBonusType -= available;
          if (maxPerBonusType <= 0) {
            break;
          }
        }
      }
      return total;
    }
  }

  private static Allocation allocationFor(
      final Map<CombatProfile, Integer> giverBase,
      final List<SupportRule> applicable,
      final boolean strength,
      final Comparator<SupportRule> order) {
    final Map<BonusTypeId, List<SupportRule>> byBonusType = new LinkedHashMap<>();
    final Map<SupportRule, int[]> pool = new LinkedHashMap<>();
    for (final SupportRule rule : applicable) {
      if (rule.appliesToStrength() != strength) {
        continue;
      }
      final int givers = giverCount(giverBase, rule);
      if (givers == 0) {
        continue;
      }
      final int[] points = new int[givers];
      Arrays.fill(points, rule.usesPerGiver());
      pool.put(rule, points);
      byBonusType.computeIfAbsent(rule.bonusType(), key -> new ArrayList<>()).add(rule);
    }
    byBonusType.values().forEach(group -> group.sort(order));
    return new Allocation(byBonusType, pool);
  }

  private static int giverCount(final Map<CombatProfile, Integer> base, final SupportRule rule) {
    int givers = 0;
    for (final Map.Entry<CombatProfile, Integer> entry : base.entrySet()) {
      if (entry.getKey().gives().contains(rule.from())) {
        givers += entry.getValue();
      }
    }
    return givers;
  }

  /** The number of distinct supporters that still hold a support point. */
  private static int distinctAvailable(final int[] points) {
    int available = 0;
    for (final int remaining : points) {
      if (remaining > 0) {
        available++;
      }
    }
    return available;
  }

  /** Consumes one support point from each of the first {@code count} still-available supporters. */
  private static void draw(final int[] points, final int count) {
    int drawn = 0;
    for (int i = 0; i < points.length && drawn < count; i++) {
      if (points[i] > 0) {
        points[i]--;
        drawn++;
      }
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
   * The evaluated profile a supported recipient becomes: strength support bumps the side-relevant
   * stat (attack on offense, defense on defense) and roll support adds firing rolls. A zero/zero
   * bonus yields a profile equal to the base, so unsupported recipients stay in their bucket.
   * Strength may go negative from enemy debuffs — the hit roller floors it at fire time (engine
   * {@code StrengthValue}) — but roll count is clamped at 0 here, mirroring {@code RollValue},
   * since a negative roll count is meaningless to every downstream consumer.
   */
  private static CombatProfile boost(
      final CombatProfile base, final int strengthBonus, final int rollBonus, final Side side) {
    final int attack = side == Side.OFFENSE ? base.attack() + strengthBonus : base.attack();
    final int defense = side == Side.DEFENSE ? base.defense() + strengthBonus : base.defense();
    return new CombatProfile(
        base.type(),
        attack,
        defense,
        Math.max(0, base.rolls() + rollBonus),
        base.maxRoundsAa(),
        base.maxAaAttacks(),
        base.hitPoints(),
        base.domain(),
        base.damage(),
        base.gives(),
        base.receives(),
        base.flags(),
        base.next());
  }
}
