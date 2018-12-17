package games.strategy.engine.data;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import games.strategy.engine.delegate.IDelegate;

// TODO: Upon next incompatible release, replace this class with Map<String, IDelegate> and mark the
// corresponding field in GameData as transient.

/**
 * A collection of delegates.
 */
public class DelegateList extends GameDataComponent implements Iterable<IDelegate> {
  private static final long serialVersionUID = 4156921032854553312L;

  private Map<String, IDelegate> delegates = new HashMap<>();

  public DelegateList(final GameData data) {
    super(data);
  }

  public void addDelegate(final IDelegate del) {
    delegates.put(del.getName(), del);
  }

  public int size() {
    return delegates.size();
  }

  @Override
  public Iterator<IDelegate> iterator() {
    return delegates.values().iterator();
  }

  public IDelegate getDelegate(final String name) {
    return delegates.get(name);
  }

  private void writeObject(@SuppressWarnings("unused") final ObjectOutputStream out) {
    // don't write since delegates should be handled separately.
  }

  private void readObject(@SuppressWarnings("unused") final ObjectInputStream in) {
    delegates = new HashMap<>();
  }
}
