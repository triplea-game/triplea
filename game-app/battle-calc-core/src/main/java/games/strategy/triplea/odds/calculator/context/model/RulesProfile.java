package games.strategy.triplea.odds.calculator.context.model;

import java.util.Map;

// TODO(phase1): the flat flag-bag is a placeholder for the ~60-70 typed combat flags the adapter
// bakes from GameData Properties; replace with named fields as the port enumerates them.
/**
 * The battle-scoped rule flags baked from map Properties; nothing here reaches back into GameData.
 */
public record RulesProfile(Map<String, Boolean> flags) {}
