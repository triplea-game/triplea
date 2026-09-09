package games.strategy.triplea.odds.calculator.context.model;

/**
 * The engine's per-receiver support stack-cap key ({@code UnitSupportAttachment.BonusType} name).
 * Rules sharing a bonus type share one cap: a single receiver takes at most the type's {@code
 * count} supports across all of them. Distinct bonus types stack independently.
 */
public record BonusTypeId(String name) {}
