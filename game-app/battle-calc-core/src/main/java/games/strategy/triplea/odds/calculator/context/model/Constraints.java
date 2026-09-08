package games.strategy.triplea.odds.calculator.context.model;

/** The hard casualty rules the allocator must honor regardless of the chosen preference. */
public record Constraints(boolean keepOneLand, boolean transportCasualtiesRestricted) {}
