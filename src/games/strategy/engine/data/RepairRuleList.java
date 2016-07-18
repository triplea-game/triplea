package games.strategy.engine.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RepairRuleList extends GameDataComponent {
  private static final long serialVersionUID = 8153102637443800391L;
  private final Map<String, RepairRule> m_repairRules = new HashMap<>();

  public RepairRuleList(final GameData data) {
    super(data);
  }

  public void addRepairRule(final RepairRule pf) {
    m_repairRules.put(pf.getName(), pf);
  }

  public int size() {
    return m_repairRules.size();
  }

  public RepairRule getRepairRule(final String name) {
    return m_repairRules.get(name);
  }

  public Collection<RepairRule> getRepairRules() {
    return m_repairRules.values();
  }
}
