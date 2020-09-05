package games.strategy.engine.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** A collection of {@link RepairRule}s keyed on the repair rule name. */
public class RepairRules extends GameDataComponent {
  private static final long serialVersionUID = 8153102637443800391L;

  private final Map<String, RepairRule> repairRules = new HashMap<>();

  public RepairRules(final GameData data) {
    super(data);
  }

  public void addRepairRule(final RepairRule pf) {
    repairRules.put(pf.getName(), pf);
  }

  public RepairRule getRepairRule(final String name) {
    return repairRules.get(name);
  }

  public Collection<RepairRule> getRepairRules() {
    return repairRules.values();
  }
}
