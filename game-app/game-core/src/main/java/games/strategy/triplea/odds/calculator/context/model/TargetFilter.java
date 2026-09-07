package games.strategy.triplea.odds.calculator.context.model;

import java.util.Set;

// TODO(phase1): targeting is relational (canNotTarget matrices, AA->air, air-vs-sub+destroyer); a
// flat eligible set is the phase-0 shape and may widen once the resolver computes it.
/** Which raw profiles a firing group's hits may kill. */
public record TargetFilter(Set<CombatProfile> eligibleTargets) {}
