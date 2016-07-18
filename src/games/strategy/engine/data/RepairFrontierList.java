package games.strategy.engine.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RepairFrontierList extends GameDataComponent {
  private static final long serialVersionUID = -5877933681560908405L;
  private final Map<String, RepairFrontier> m_repairFrontiers = new HashMap<>();

  public RepairFrontierList(final GameData data) {
    super(data);
  }

  public void addRepairFrontier(final RepairFrontier pf) {
    m_repairFrontiers.put(pf.getName(), pf);
  }

  public int size() {
    return m_repairFrontiers.size();
  }

  public RepairFrontier getRepairFrontier(final String name) {
    return m_repairFrontiers.get(name);
  }

  public Set<String> getRepairFrontierNames() {
    return m_repairFrontiers.keySet();
  }
}
