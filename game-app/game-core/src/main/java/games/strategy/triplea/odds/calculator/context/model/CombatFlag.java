package games.strategy.triplea.odds.calculator.context.model;

// TODO(phase1): only the design-named flags are here; the adapter port fills in the rest of the
// per-unit combat abilities as it maps UnitAttachment.
/** Boolean combat abilities carried on a {@link CombatProfile}. */
public enum CombatFlag {
  FIRST_STRIKE,
  IS_AA,
  CHOOSE_BEST_ROLL,
  // Intrinsic eligibility to submerge; whether it may actually submerge is relational (no blocking
  // enemy destroyer) and lives in CombatRelations, not here.
  CAN_SUBMERGE
}
