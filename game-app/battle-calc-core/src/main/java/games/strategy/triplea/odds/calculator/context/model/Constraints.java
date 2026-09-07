package games.strategy.triplea.odds.calculator.context.model;

// TODO(phase1): keep-one-land is the first hard rule modelled; targeting eligibility and dependent
// cascade constraints join it as the allocator ports them.
/** The hard casualty rules the allocator must honor regardless of the chosen preference. */
public record Constraints(boolean keepOneLand) {}
