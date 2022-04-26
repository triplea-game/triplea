package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.triplea.attachments.UnitTypeComparator;
import java.util.Comparator;
import org.triplea.java.collections.IntegerMap;

public abstract class Rule extends DefaultNamed {
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

  public static Comparator<Rule> getComparator() {
    return (o1, o2) -> {
      final UnitTypeComparator utc = new UnitTypeComparator();

      IntegerMap<NamedAttachable> o1Results = o1.getResults();
      IntegerMap<NamedAttachable> o2Results = o2.getResults();

      if (o1Results.size() == 1 && o2Results.size() == 1) {
        final NamedAttachable n1 = o1Results.keySet().iterator().next();
        final NamedAttachable n2 = o2Results.keySet().iterator().next();
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

      if (o1Results.size() > o2Results.size()) {
        return -1;
      } else if (o1Results.size() < o2Results.size()) {
        return 1;
      } else {
        return o1.getName().compareTo(o2.getName());
      }
    };
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

  @Override
  public String toString() {
    return getClass() + ":" + getName();
  }
}
