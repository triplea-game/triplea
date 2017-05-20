package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class RepairFrontier extends DefaultNamed implements Iterable<RepairRule> {
  private static final long serialVersionUID = -5148536624986056753L;
  private final List<RepairRule> m_rules = new ArrayList<>();
  private List<RepairRule> m_cachedRules;

  /**
   * Creates new RepairFrontier.
   *
   * @param name
   *        name of new repair frontier
   * @param data
   *        game data
   */
  public RepairFrontier(final String name, final GameData data) {
    super(name, data);
  }

  void addRule(final RepairRule rule) {
    if (m_rules.contains(rule)) {
      throw new IllegalStateException("Rule already added:" + rule);
    }
    m_rules.add(rule);
    m_cachedRules = null;
  }

  public List<RepairRule> getRules() {
    if (m_cachedRules == null) {
      m_cachedRules = Collections.unmodifiableList(m_rules);
    }
    return m_cachedRules;
  }

  @Override
  public Iterator<RepairRule> iterator() {
    return getRules().iterator();
  }
}
