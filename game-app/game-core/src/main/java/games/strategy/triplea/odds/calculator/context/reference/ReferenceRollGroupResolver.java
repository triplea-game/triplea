package games.strategy.triplea.odds.calculator.context.reference;

import games.strategy.triplea.odds.calculator.context.model.BattleRound;
import games.strategy.triplea.odds.calculator.context.model.Force;
import games.strategy.triplea.odds.calculator.context.model.RulesProfile;
import games.strategy.triplea.odds.calculator.context.seam.CombatRelations;
import games.strategy.triplea.odds.calculator.context.seam.RollGroupResolver;
import games.strategy.triplea.odds.calculator.context.seam.SupportResolver;

/**
 * Reference firing-plan resolver; takes the {@link SupportResolver} and {@link CombatRelations}
 * seams, not impls — relational rules (destroyer negating sub first strike, targeting) go through
 * {@code CombatRelations} rather than matching unit names.
 */
public class ReferenceRollGroupResolver implements RollGroupResolver {
  private final SupportResolver supportResolver;
  private final CombatRelations relations;

  public ReferenceRollGroupResolver(
      final SupportResolver supportResolver, final CombatRelations relations) {
    this.supportResolver = supportResolver;
    this.relations = relations;
  }

  @Override
  public BattleRound plan(
      final Force attackers, final Force defenders, final RulesProfile rules, final int round) {
    throw new UnsupportedOperationException("phase 1");
  }
}
