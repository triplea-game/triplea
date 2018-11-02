package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;

class AddProductionRule extends Change {
  private static final long serialVersionUID = 2583955907289570063L;

  private final ProductionRule rule;
  private final ProductionFrontier frontier;

  public AddProductionRule(final ProductionRule rule, final ProductionFrontier frontier) {
    if (rule == null) {
      throw new IllegalArgumentException("Null rule");
    }
    if (frontier == null) {
      throw new IllegalArgumentException("Null frontier");
    }
    this.rule = rule;
    this.frontier = frontier;
  }

  @Override
  public void perform(final GameData data) {
    frontier.addRule(rule);
  }

  @Override
  public Change invert() {
    return new RemoveProductionRule(rule, frontier);
  }
}
