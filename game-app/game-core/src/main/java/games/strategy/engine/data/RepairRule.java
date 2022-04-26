package games.strategy.engine.data;

import org.triplea.java.collections.IntegerMap;

/** A repair rule. */
public class RepairRule extends Rule {
  private static final long serialVersionUID = -45646671022993959L;

  public RepairRule(final String name, final GameData data) {
    super(name, data);
  }

  public RepairRule(
      final String name,
      final GameData data,
      final IntegerMap<NamedAttachable> results,
      final IntegerMap<Resource> costs) {
    super(name, data, results, costs);
  }
}
