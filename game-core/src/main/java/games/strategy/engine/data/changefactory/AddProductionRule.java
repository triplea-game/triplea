package games.strategy.engine.data.changefactory;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;

class AddProductionRule extends Change {
  private static final long serialVersionUID = 2583955907289570063L;

  private final ProductionRule rule;
  private final ProductionFrontier frontier;

  AddProductionRule(final ProductionRule rule, final ProductionFrontier frontier) {
    checkNotNull(rule);
    checkNotNull(frontier);

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
