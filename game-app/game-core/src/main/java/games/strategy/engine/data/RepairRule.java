package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import org.triplea.java.collections.IntegerMap;

/** A repair rule. */
public class RepairRule extends ProductionRule {
  private static final long serialVersionUID = -45646671022993959L;

  public RepairRule(final String name, final GameData data) {
    this(name, data, new IntegerMap<>(), new IntegerMap<>());
  }

  public RepairRule(
      final String name,
      final GameData data,
      final IntegerMap<NamedAttachable> results,
      final IntegerMap<Resource> costs) {
    super(name, data, results, costs);

    checkNotNull(results);
    checkNotNull(costs);
  }

  @Override
  public String toString() {
    return "RepairRule: " + getName();
  }
}
