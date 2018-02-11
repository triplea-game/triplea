package games.strategy.engine.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RepairFrontierList extends GameDataComponent {
  private static final long serialVersionUID = -5877933681560908405L;
  private final Map<String, RepairFrontier> m_repairFrontiers;

  public RepairFrontierList(final GameData data) {
    this(data, new HashMap<>());
  }

  private RepairFrontierList(final GameData data, final Map<String, RepairFrontier> repairFrontiers) {
    super(data);
    m_repairFrontiers = repairFrontiers;
  }

  protected void addRepairFrontier(final RepairFrontier pf) {
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
  
  RepairFrontierList clone(final GameData newData) {
    return new RepairFrontierList(newData, new HashMap<>(m_repairFrontiers));
  }
}
