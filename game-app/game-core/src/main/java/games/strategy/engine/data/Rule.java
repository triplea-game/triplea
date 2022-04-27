package games.strategy.engine.data;

import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/**
 * Superclass for {@link games.strategy.engine.data.RepairRule} and {@link
 * games.strategy.engine.data.ProductionRule}.
 */
public interface Rule {
  String getName();

  IntegerMap<Resource> getCosts();

  IntegerMap<NamedAttachable> getResults();

  /** Benefits must be a resource or a unit. */
  default void addResult(final NamedAttachable obj, final int quantity) {
    if (!(obj instanceof UnitType) && !(obj instanceof Resource)) {
      throw new IllegalArgumentException(
          "results must be units or resources, not:" + obj.getClass().getName());
    }

    getResults().put(obj, quantity);
  }

  default NamedAttachable getAnyResultKey() {
    return CollectionUtils.getAny(getResults().keySet());
  }
}
