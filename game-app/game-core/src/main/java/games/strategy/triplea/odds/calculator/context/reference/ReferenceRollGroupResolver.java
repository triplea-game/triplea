package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.BattleRound;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.RulesProfile;
import games.strategy.triplea.odds.calculator.context.seam.RollGroupResolver;
import games.strategy.triplea.odds.calculator.context.seam.SupportResolver;

/** Reference firing-plan resolver; takes the {@link SupportResolver} seam, not an impl. */
public class ReferenceRollGroupResolver implements RollGroupResolver {
  private final SupportResolver supportResolver;

  public ReferenceRollGroupResolver(final SupportResolver supportResolver) {
    this.supportResolver = supportResolver;
  }

  @Override
  public BattleRound plan(
      final Force attackers, final Force defenders, final RulesProfile rules, final int round) {
    throw new UnsupportedOperationException("phase 1");
  }
}
