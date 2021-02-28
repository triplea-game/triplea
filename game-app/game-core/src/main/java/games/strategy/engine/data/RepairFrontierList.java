package games.strategy.engine.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** A keyed collection of {@link RepairFrontier}s. */
public class RepairFrontierList extends GameDataComponent {
  private static final long serialVersionUID = -5877933681560908405L;

  private final Map<String, RepairFrontier> repairFrontiers = new HashMap<>();

  public RepairFrontierList(final GameData data) {
    super(data);
  }

  public void addRepairFrontier(final RepairFrontier pf) {
    repairFrontiers.put(pf.getName(), pf);
  }

  public RepairFrontier getRepairFrontier(final String name) {
    return repairFrontiers.get(name);
  }

  public Set<String> getRepairFrontierNames() {
    return repairFrontiers.keySet();
  }
}
