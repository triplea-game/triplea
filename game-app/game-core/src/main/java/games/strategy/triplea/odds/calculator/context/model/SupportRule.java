package games.strategy.triplea.odds.calculator.context.model;

/**
 * A dynamic modifier some giver profiles grant recipients, allocated once per round force-wide;
 * consume/sort order mirrors the engine's AvailableSupports.
 */
public record SupportRule(
    SupportCategory from,
    SupportCategory to,
    int bonus,
    boolean appliesToStrength,
    int usesPerGiver,
    Side side,
    boolean firstRoundOnly) {}
