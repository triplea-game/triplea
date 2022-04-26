package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.triplea.attachments.UnitTypeComparator;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;

public abstract class Rule extends DefaultNamed implements Comparable<Rule> {
  private final IntegerMap<Resource> costs;
  private final IntegerMap<NamedAttachable> results;

  protected Rule(String name, GameData data) {
    this(name, data, new IntegerMap<>(), new IntegerMap<>());
  }

  protected Rule(
      String name, GameData data, IntegerMap<NamedAttachable> results, IntegerMap<Resource> costs) {
    super(name, data);

    checkNotNull(results);
    checkNotNull(costs);

    this.costs = new IntegerMap<>(costs);
    this.results = new IntegerMap<>(results);
  }

  @Override
  public int compareTo(Rule that) {
    final UnitTypeComparator utc = new UnitTypeComparator();

    IntegerMap<NamedAttachable> thisResults = getResults();
    IntegerMap<NamedAttachable> thatResults = that.getResults();

    if (thisResults.size() == 1 && thatResults.size() == 1) {
      final NamedAttachable n1 = thisResults.keySet().iterator().next();
      final NamedAttachable n2 = thatResults.keySet().iterator().next();
      if (n1 instanceof UnitType) {
        final UnitType u1 = (UnitType) n1;
        if (n2 instanceof UnitType) {
          final UnitType u2 = (UnitType) n2;
          return utc.compare(u1, u2);
        } else if (n2 instanceof Resource) {
          // final Resource r2 = (Resource) n2;
          return -1;
        }

        return n1.getName().compareTo(n2.getName());
      } else if (n1 instanceof Resource) {
        final Resource r1 = (Resource) n1;
        if (n2 instanceof UnitType) {
          // final UnitType u2 = (UnitType) n2;
          return 1;
        } else if (n2 instanceof Resource) {
          final Resource r2 = (Resource) n2;
          return r1.getName().compareTo(r2.getName());
        } else {
          return n1.getName().compareTo(n2.getName());
        }
      }

      return n1.getName().compareTo(n2.getName());
    }

    if (thisResults.size() > thatResults.size()) {
      return -1;
    } else if (thisResults.size() < thatResults.size()) {
      return 1;
    } else {
      return getName().compareTo(that.getName());
    }
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
    return costs;
  }

  public IntegerMap<NamedAttachable> getResults() {
    return results;
  }

  public NamedAttachable getAnyResultKey() {
    return CollectionUtils.getAny(results.keySet());
  }

  @Override
  public String toString() {
    return getClass() + ":" + getName();
  }
}
