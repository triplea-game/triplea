package games.strategy.triplea.odds.calculator.context.model;

/**
 * A dynamic modifier some giver profiles grant recipients; consume/sort order mirrors the engine's
 * AvailableSupports. {@code usesPerGiver} is how many distinct recipients one giver can support;
 * {@code bonusType}/{@code maxPerReceiver} carry the stack-cap grouping — a receiver takes at most
 * {@code maxPerReceiver} supports across every rule sharing this rule's {@code bonusType}. Within a
 * shared-bonusType group rules apply highest-{@code bonus} first, then fewest-{@code
 * targetTypeCount} first (the engine's SupportRuleSort, so a narrowly-targeted rule is not wasted
 * before it can act).
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
    int targetTypeCount) {}
