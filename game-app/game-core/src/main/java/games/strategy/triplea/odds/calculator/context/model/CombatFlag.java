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
  CAN_SUBMERGE,
  // Immunity to being fired on by aircraft absent a friendly destroyer, and the trigger for the
  // submerge-vs-only-air retreat. Baked from a non-empty UnitAttachment#getCanNotBeTargetedBy,
  // which
  // is distinct from canEvade: a Revised submarine evades (CAN_SUBMERGE) yet is still
  // air-targetable,
  // so this rule keys on its own flag rather than reusing CAN_SUBMERGE.
  CANNOT_BE_TARGETED_BY_ALL,
  // Baked from GameData by the adapter and consumed by CombatRelations, eg to deny a facing sub its
  // first-strike/submerge — intrinsic identity, never string-parsed in the core.
  IS_DESTROYER
}
