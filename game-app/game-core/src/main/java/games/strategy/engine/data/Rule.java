package games.strategy.engine.data;

import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

public interface Rule {
  String getName();

  IntegerMap<Resource> getCosts();

  IntegerMap<NamedAttachable> getResults();

  default void addCost(final Resource resource, final int quantity) {
    getCosts().put(resource, quantity);
  }

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
