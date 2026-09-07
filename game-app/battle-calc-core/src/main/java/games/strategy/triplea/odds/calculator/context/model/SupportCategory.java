package games.strategy.triplea.odds.calculator.context.model;

// TODO(phase1): identity may need the bonus-type grouping that AvailableSupports keys on, not just
// a
// name.
/** A support bonus grouping — what a profile gives or receives. */
public record SupportCategory(String name) {

  // {@link CombatProfile}'s gives/receives are never null — a profile with no support uses this.
  public static final SupportCategory NONE = new SupportCategory("none");
}
