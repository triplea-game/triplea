package games.strategy.engine.data;

import java.io.Serializable;

import games.strategy.util.IntegerMap;

public class RepairRule extends DefaultNamed implements Serializable {
  private static final long serialVersionUID = -45646671022993959L;
  private final IntegerMap<Resource> m_cost = new IntegerMap<>();
  private final IntegerMap<NamedAttachable> m_results = new IntegerMap<>();

  /** Creates new RepairRule */
  public RepairRule(final String name, final GameData data) {
    super(name, data);
  }

  protected void addCost(final Resource resource, final int quantity) {
    m_cost.put(resource, quantity);
  }

  /**
   * Benefits must be a resource or a unit.
   */
  protected void addResult(final NamedAttachable obj, final int quantity) {
    if (!(obj instanceof UnitType) && !(obj instanceof Resource)) {
      throw new IllegalArgumentException("results must be units or resources, not:" + obj.getClass().getName());
    }
    m_results.put(obj, quantity);
  }

  public IntegerMap<Resource> getCosts() {
    return m_cost.copy();
  }

  public IntegerMap<NamedAttachable> getResults() {
    return m_results;
  }

  @Override
  public String toString() {
    return "RepairRule:" + getName();
  }
}
