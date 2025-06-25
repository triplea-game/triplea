package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serial;
import org.triplea.java.collections.IntegerMap;

/** A repair rule. */
public class RepairRule extends DefaultNamed implements Rule {
  @Serial private static final long serialVersionUID = -45646671022993959L;

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

  @Override
  public IntegerMap<Resource> getCosts() {
    return new IntegerMap<>(costs);
  }

  @Override
  public void addCost(final Resource resource, final int quantity) {
    costs.put(resource, quantity);
  }

  @Override
  public IntegerMap<NamedAttachable> getResults() {
    return results;
  }

  @Override
  public String toString() {
    return "RepairRule:" + getName();
  }
}
