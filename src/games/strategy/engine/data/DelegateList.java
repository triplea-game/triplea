package games.strategy.engine.data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import games.strategy.engine.delegate.IDelegate;

public class DelegateList extends GameDataComponent implements Iterable<IDelegate> {
  private static final long serialVersionUID = 4156921032854553312L;
  private Map<String, IDelegate> m_delegates = new HashMap<String, IDelegate>();

  public DelegateList(final GameData data) {
    super(data);
  }

  public void addDelegate(final IDelegate del) {
    m_delegates.put(del.getName(), del);
  }

  public int size() {
    return m_delegates.size();
  }

  @Override
  public Iterator<IDelegate> iterator() {
    return m_delegates.values().iterator();
  }

  public IDelegate getDelegate(final String name) {
    return m_delegates.get(name);
  }
}
