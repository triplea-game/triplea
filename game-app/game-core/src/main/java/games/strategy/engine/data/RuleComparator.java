package games.strategy.engine.data;

import games.strategy.triplea.attachments.UnitTypeComparator;
import java.util.Comparator;
import org.triplea.java.collections.IntegerMap;

public class RuleComparator implements Comparator<Rule> {
  @Override
  public int compare(Rule o1, Rule o2) {
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

    return Integer.compare(o2Results.size(), o1Results.size());
  }
}
