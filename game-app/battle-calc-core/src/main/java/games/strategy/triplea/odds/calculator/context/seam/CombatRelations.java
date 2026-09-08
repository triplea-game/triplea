package games.strategy.triplea.odds.calculator.context.seam;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.RulesProfile;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.TargetFilter;
import java.util.Map;

/**
 * The combat rules that depend on the OPPOSING force's composition, held behind a seam so a fake or
 * a rules variant swaps in without rippling through the resolver — a destroyer negating a sub, an
 * AA gun that only targets air, a submerge gate all read the enemy, not the profile alone.
 */
public interface CombatRelations {
  /**
   * The enemy profiles a single firing {@code firer} may hit — resolved per firer, not per group,
   * so a surface unit and an air unit firing the same round get their own eligibility (the engine's
   * per-{@code unitType} {@code TargetGroup.findTargets}). The resolver buckets firers by this set.
   */
  TargetFilter eligibleTargets(CombatProfile firer, Force friendly, Force enemy);

  boolean firstStrikeNegated(Side side, Force friendly, Force enemy, RulesProfile rules);

  boolean canSubmerge(
      Side side, Map<CombatProfile, Integer> cohort, Force enemy, RulesProfile rules);
}
