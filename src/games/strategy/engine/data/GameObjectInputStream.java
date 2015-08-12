package games.strategy.engine.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import games.strategy.engine.framework.GameObjectStreamFactory;

/**
 * Please refer to the comments on GameObjectOutputStream
 */
public class GameObjectInputStream extends ObjectInputStream {
  private final GameObjectStreamFactory m_dataSource;

  /**
   * Creates new GameObjectReader
   *
   * @param dataSource
   *        data source
   * @param input
   *        input stream
   * @throws IOException
   */
  public GameObjectInputStream(final GameObjectStreamFactory dataSource, final InputStream input) throws IOException {
    super(input);
    m_dataSource = dataSource;
    enableResolveObject(true);
  }

  public GameData getData() {
    return m_dataSource.getData();
  }

  @Override
  protected Object resolveObject(final Object obj) throws IOException {
    // when loading units, we want to maintain == relationships for many
    // of the game data objects.
    // this is to prevent the situation where we have 2 Territory objects for the
    // the same territory, or two object for the same player id or ...
    // thus, in one vm you can add some units to a territory, and when you serialize the change
    // and look at the Territory object in another vm, the units have not been added
    if (obj instanceof GameData) {
      return m_dataSource.getData();
    } else if ((obj instanceof GameObjectStreamData)) {
      return ((GameObjectStreamData) obj).getReference(getData());
    } else if (obj instanceof Unit) {
      return resolveUnit((Unit) obj);
    } else {
      return obj;
    }
  }

  private Object resolveUnit(final Unit unit) {
    m_dataSource.getData().acquireReadLock();
    try {
      final Unit local = m_dataSource.getData().getUnits().get(unit.getID());
      if (local != null) {
        return local;
      }
      getData().getUnits().put(unit);
      return unit;
    } finally {
      m_dataSource.getData().releaseReadLock();
    }
  }
}
