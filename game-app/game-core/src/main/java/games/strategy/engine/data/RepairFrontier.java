package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** A collection of {@link RepairRule}s. */
public class RepairFrontier extends DefaultNamed implements Iterable<RepairRule> {
  private static final long serialVersionUID = -5148536624986056753L;

  private final List<RepairRule> rules;
  private List<RepairRule> cachedRules;

  public RepairFrontier(final String name, final GameData data) {
    this(name, data, List.of());
  }

  public RepairFrontier(final String name, final GameData data, final List<RepairRule> rules) {
    super(name, data);

    checkNotNull(rules);

    this.rules = new ArrayList<>(rules);
  }

  public void addRule(final RepairRule rule) {
    if (rules.contains(rule)) {
      throw new IllegalStateException("Rule already added:" + rule);
    }
    rules.add(rule);
    cachedRules = null;
  }

  public List<RepairRule> getRules() {
    if (cachedRules == null) {
      cachedRules = Collections.unmodifiableList(rules);
    }
    return cachedRules;
  }

  @Override
  public Iterator<RepairRule> iterator() {
    return getRules().iterator();
  }
}
