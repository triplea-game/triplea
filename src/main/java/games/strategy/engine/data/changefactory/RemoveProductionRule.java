package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.ProductionRule;

class RemoveProductionRule extends Change {
  private static final long serialVersionUID = 2312599802275503095L;
  private final ProductionRule m_rule;
  private final ProductionFrontier m_frontier;

  public RemoveProductionRule(final ProductionRule rule, final ProductionFrontier frontier) {
    if (rule == null) {
      throw new IllegalArgumentException("Null rule");
    }
    if (frontier == null) {
      throw new IllegalArgumentException("Null frontier");
    }
    m_rule = rule;
    m_frontier = frontier;
  }

  @Override
  public void perform(final GameData data) {
    m_frontier.removeRule(m_rule);
  }

  @Override
  public Change invert() {
    return new AddProductionRule(m_rule, m_frontier);
  }
}
