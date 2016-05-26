package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ProductionFrontier extends DefaultNamed implements Iterable<ProductionRule> {
  private static final long serialVersionUID = -5967251608158552892L;
  private final List<ProductionRule> m_rules = new ArrayList<>();
  private List<ProductionRule> m_cachedRules;

  /**
   * Creates new ProductionFrontier
   *
   * @param name
   *        name of production frontier
   * @param data
   *        game data
   */
  public ProductionFrontier(final String name, final GameData data) {
    super(name, data);
  }

  public void addRule(final ProductionRule rule) {
    if (m_rules.contains(rule)) {
      throw new IllegalStateException("Rule already added:" + rule);
    }
    m_rules.add(rule);
    m_cachedRules = null;
  }

  public void removeRule(final ProductionRule rule) {
    if (!m_rules.contains(rule)) {
      throw new IllegalStateException("Rule not present:" + rule);
    }
    m_rules.remove(rule);
    m_cachedRules = null;
  }

  public List<ProductionRule> getRules() {
    if (m_cachedRules == null) {
      m_cachedRules = Collections.unmodifiableList(m_rules);
    }
    return m_cachedRules;
  }

  @Override
  public Iterator<ProductionRule> iterator() {
    return getRules().iterator();
  }
}
