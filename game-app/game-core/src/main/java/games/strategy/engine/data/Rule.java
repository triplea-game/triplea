package games.strategy.engine.data;

import org.triplea.java.collections.IntegerMap;

public interface Rule {
  IntegerMap<Resource> getCosts();

  IntegerMap<NamedAttachable> getResults();
}
