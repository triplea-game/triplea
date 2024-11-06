package games.strategy.engine.data;

import org.triplea.java.ChangeOnNextMajorRelease;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

/**
 * Superclass for {@link games.strategy.engine.data.RepairRule} and {@link
 * games.strategy.engine.data.ProductionRule}.
 *
 * <p>It could become an {@code abstract class} instead in order to contain more common code chunks
 * from its children. That way, probably {@link games.strategy.engine.data.RuleComparator} can be
 * entirely removed by making the child classes implement {@link java.lang.Comparable}.
 */
@ChangeOnNextMajorRelease
public interface Rule {
  String getName();

  IntegerMap<Resource> getCosts();

  IntegerMap<NamedAttachable> getResults();

  /**
   * Benefits must be a resource or a unit.
   *
   * <p>It's not going to work if any of {@link Rule#getResults()} implementations returns, for
   * example, a copy of {@link org.triplea.java.collections.IntegerMap}.
   */
  default void addResult(final NamedAttachable obj, final int quantity) {
    if (!(obj instanceof UnitType) && !(obj instanceof Resource)) {
      throw new IllegalArgumentException(
          "results must be units or resources, not: " + obj.getClass().getName());
    }

    getResults().put(obj, quantity);
  }

  default NamedAttachable getAnyResultKey() {
    return CollectionUtils.getAny(getResults().keySet());
  }
}
