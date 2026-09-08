package games.strategy.triplea.odds.calculator.context.model;

/**
 * Boolean combat abilities carried on a {@link CombatProfile}, baked by the adapter from a unit's
 * {@code UnitAttachment} plus the map rule flags, and consumed by the resolver, relations, and
 * roller. Battle-resolution abilities are added here as the differential harness lights up rules
 * that read them.
 */
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
  IS_DESTROYER,
  // Marks a non-combatant cargo profile, eg a land unit carried by a transport in a sea battle.
  // CombatRelations excludes it from targeting and RollGroupResolver from firing, so it leaves the
  // battle only through the dependent cascade when its carrier is killed.
  IS_DEPENDENT,
  // Marks a non-combat sea transport: protected as a casualty class while a combatant can still soak
  // a hit under 'transportCasualtiesRestricted', and swept off the board once left unescorted.
  IS_TRANSPORT
}
