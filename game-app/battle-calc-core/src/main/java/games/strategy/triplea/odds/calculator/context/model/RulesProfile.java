package games.strategy.triplea.odds.calculator.context.model;

/**
 * The battle-scoped rule flags baked from map Properties; nothing here reaches back into GameData.
 * Only the subset battle resolution reads today is modelled — the adapter bakes each field from the
 * matching {@code Properties} getter.
 */
public record RulesProfile(
    boolean ww2v2,
    boolean defendingSubsSneakAttack,
    boolean transportCasualtiesRestricted,
    boolean submersibleSubs,
    boolean submarinesDefendingMaySubmergeOrRetreat,
    boolean lhtrHeavyBombers) {

  /** Standard-rules default: every flag off, matching the engine Properties defaults. */
  public static RulesProfile standard() {
    return new RulesProfile(false, false, false, false, false, false);
  }
}
