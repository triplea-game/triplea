package games.strategy.triplea.odds.calculator.context.model;

import java.util.Map;

// TODO(phase1): a cost lookup is enough for OOL/cheapest ordering; richer preferences (weakest,
// finish-damaged-first) may need more per-profile inputs here.
/**
 * Read-only per-profile facts a {@link
 * games.strategy.triplea.odds.calculator.context.seam.CasualtyOrder} ranks on.
 */
public record ProfileStats(Map<CombatProfile, Integer> cost) {}
