package games.strategy.engine.data;

import games.strategy.triplea.attachments.UnitTypeComparator;
import java.util.Comparator;

public class RuleComparator<T extends Rule> implements Comparator<T> {
  private final UnitTypeComparator unitTypeComparator = new UnitTypeComparator();

  @Override
  public int compare(T o1, T o2) {
    int o1ResultsSize = o1.getResults().size();
    int o2ResultsSize = o2.getResults().size();

    if (o1ResultsSize == 1 && o2ResultsSize == 1) {
      final NamedAttachable n1 = o1.getAnyResultKey();
      final NamedAttachable n2 = o2.getAnyResultKey();
      if (n1 instanceof UnitType u1) {
        if (n2 instanceof UnitType u2) {
          return unitTypeComparator.compare(u1, u2);
        } else if (n2 instanceof Resource) {
          return -1;
        }

        return n1.getName().compareTo(n2.getName());
      } else if (n1 instanceof Resource r1) {
        if (n2 instanceof UnitType) {
          return 1;
        } else if (n2 instanceof Resource r2) {
          return r1.getName().compareTo(r2.getName());
        } else {
          return n1.getName().compareTo(n2.getName());
        }
      }

      return n1.getName().compareTo(n2.getName());
    }

    if (o1ResultsSize > o2ResultsSize) {
      return -1;
    } else if (o1ResultsSize < o2ResultsSize) {
      return 1;
    } else {
      return o1.getName().compareTo(o2.getName());
    }
  }
}
