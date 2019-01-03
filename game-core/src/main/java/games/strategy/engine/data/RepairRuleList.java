package games.strategy.engine.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** A collection of {@link RepairRule}s keyed on the repair rule name. */
// TODO: rename this class upon next incompatible release to replace "List" suffix since this
// collection is not ordered
public class RepairRuleList extends GameDataComponent {
  private static final long serialVersionUID = 8153102637443800391L;

  private final Map<String, RepairRule> repairRules = new HashMap<>();

  public RepairRuleList(final GameData data) {
    super(data);
  }

  protected void addRepairRule(final RepairRule pf) {
    repairRules.put(pf.getName(), pf);
  }

  public RepairRule getRepairRule(final String name) {
    return repairRules.get(name);
  }

  public Collection<RepairRule> getRepairRules() {
    return repairRules.values();
  }
}
