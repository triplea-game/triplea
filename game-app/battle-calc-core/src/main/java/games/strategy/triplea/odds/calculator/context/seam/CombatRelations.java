package games.strategy.triplea.odds.calculator.context.seam;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.RollGroup;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.TargetFilter;
import java.util.Map;

/**
 * The combat rules that depend on the OPPOSING force's composition, held behind a seam so a fake or
 * a rules variant swaps in without rippling through the resolver — a destroyer negating a sub, an
 * AA gun that only targets air, a submerge gate all read the enemy, not the profile alone.
 */
public interface CombatRelations {
  TargetFilter eligibleTargets(RollGroup group, Force friendly, Force enemy);

  boolean firstStrikeNegated(Side side, Force friendly, Force enemy);

  boolean canSubmerge(Map<CombatProfile, Integer> cohort, Force enemy);
}
