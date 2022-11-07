package games.strategy.engine.data.changefactory;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;

class RemoveProductionRule extends Change {
  private static final long serialVersionUID = 2312599802275503095L;

  private final ProductionRule rule;
  private final ProductionFrontier frontier;

  RemoveProductionRule(final ProductionRule rule, final ProductionFrontier frontier) {
    checkNotNull(rule);
    checkNotNull(frontier);

    this.rule = rule;
    this.frontier = frontier;
  }

  @Override
  public void perform(final GameState data) {
    frontier.removeRule(rule);
  }

  @Override
  public Change invert() {
    return new AddProductionRule(rule, frontier);
  }
}
