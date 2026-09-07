package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.CombatProfile;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.RollGroup;
import games.strategy.triplea.odds.calculator.context.model.Side;
import games.strategy.triplea.odds.calculator.context.model.TargetFilter;
import games.strategy.triplea.odds.calculator.context.seam.CombatRelations;
import java.util.Map;

/** Reference relational rules (canNotTarget matrices, destroyer-negates-sub, submerge gate). */
public class ReferenceCombatRelations implements CombatRelations {
  @Override
  public TargetFilter eligibleTargets(
      final RollGroup group, final Force friendly, final Force enemy) {
    throw new UnsupportedOperationException("phase 1");
  }

  @Override
  public boolean firstStrikeNegated(final Side side, final Force friendly, final Force enemy) {
    throw new UnsupportedOperationException("phase 1");
  }

  @Override
  public boolean canSubmerge(final Map<CombatProfile, Integer> cohort, final Force enemy) {
    throw new UnsupportedOperationException("phase 1");
  }
}
