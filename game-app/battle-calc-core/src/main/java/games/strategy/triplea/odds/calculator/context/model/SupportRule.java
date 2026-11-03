package games.strategy.triplea.odds.calculator.context.model;

/**
 * A dynamic modifier some giver profiles grant recipients; consume/sort order mirrors the engine's
 * AvailableSupports. {@code usesPerGiver} is how many distinct recipients one giver can support;
 * {@code bonusType}/{@code maxPerReceiver} carry the stack-cap grouping — a receiver takes at most
 * {@code maxPerReceiver} supports across every rule sharing this rule's {@code bonusType}. Within a
 * shared-bonusType group rules apply highest-{@code bonus} first, then fewest-{@code
 * targetTypeCount} first (the engine's SupportRuleSort, so a narrowly-targeted rule is not wasted
 * before it can act).
 *
 * <p>{@code fromEnemy} picks which force supplies the givers and which way the group sorts: a
 * friendly rule draws givers from the evaluated force and applies strongest-bonus-first, while an
 * enemy (debuff) rule draws givers from the opposing force and applies worst-bonus-first (the
 * engine builds a separate {@code AvailableSupports} for each, with independent caps). {@code side}
 * is always the receiver side the rule fires for.
 */
public record SupportRule(
    SupportCategory from,
    SupportCategory to,
    int bonus,
    boolean appliesToStrength,
    int usesPerGiver,
    Side side,
    boolean firstRoundOnly,
    BonusTypeId bonusType,
    int maxPerReceiver,
    int targetTypeCount,
    boolean fromEnemy) {

  /** A friendly (allied) rule: givers come from the evaluated force, strongest-bonus-first. */
  public SupportRule(
      final SupportCategory from,
      final SupportCategory to,
      final int bonus,
      final boolean appliesToStrength,
      final int usesPerGiver,
      final Side side,
      final boolean firstRoundOnly,
      final BonusTypeId bonusType,
      final int maxPerReceiver,
      final int targetTypeCount) {
    this(
        from,
        to,
        bonus,
        appliesToStrength,
        usesPerGiver,
        side,
        firstRoundOnly,
        bonusType,
        maxPerReceiver,
        targetTypeCount,
        false);
  }
}
