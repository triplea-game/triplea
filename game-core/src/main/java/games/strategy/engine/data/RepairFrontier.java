package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class RepairFrontier extends DefaultNamed implements Iterable<RepairRule> {
  private static final long serialVersionUID = -5148536624986056753L;
  private final List<RepairRule> m_rules;
  private List<RepairRule> m_cachedRules;

  public RepairFrontier(final String name, final GameData data) {
    this(name, data, Collections.emptyList());
  }

  public RepairFrontier(final String name, final GameData data, final List<RepairRule> rules) {
    super(name, data);

    checkNotNull(rules);

    m_rules = new ArrayList<>(rules);
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
