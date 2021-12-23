package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import org.triplea.java.collections.IntegerMap;

/** A repair rule. */
public class RepairRule extends DefaultNamed {
  private static final long serialVersionUID = -45646671022993959L;

  private final IntegerMap<Resource> costs;
  private final IntegerMap<NamedAttachable> results;

  public RepairRule(final String name, final GameData data) {
    this(name, data, new IntegerMap<>(), new IntegerMap<>());
  }

  public RepairRule(
      final String name,
      final GameData data,
      final IntegerMap<NamedAttachable> results,
      final IntegerMap<Resource> costs) {
    super(name, data);

    checkNotNull(results);
    checkNotNull(costs);

    this.costs = new IntegerMap<>(costs);
    this.results = new IntegerMap<>(results);
  }

  public void addCost(final Resource resource, final int quantity) {
    costs.put(resource, quantity);
  }

  /** Benefits must be a resource or a unit. */
  public void addResult(final NamedAttachable obj, final int quantity) {
    if (!(obj instanceof UnitType) && !(obj instanceof Resource)) {
      throw new IllegalArgumentException(
          "results must be units or resources, not:" + obj.getClass().getName());
    }
    results.put(obj, quantity);
  }

  public IntegerMap<Resource> getCosts() {
    return new IntegerMap<>(costs);
  }

  public IntegerMap<NamedAttachable> getResults() {
    return results;
  }

  @Override
  public String toString() {
    return "RepairRule:" + getName();
  }
}
