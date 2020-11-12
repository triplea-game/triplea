package games.strategy.engine.data;

import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.triplea.settings.ClientSetting;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

/** Please refer to the comments on GameObjectOutputStream. */
public class GameObjectInputStream extends ObjectInputStream {
  private final GameObjectStreamFactory dataSource;

  public GameObjectInputStream(final GameObjectStreamFactory dataSource, final InputStream input)
      throws IOException {
    super(input);
    this.dataSource = dataSource;
    enableResolveObject(true);
  }

  public GameData getData() {
    return dataSource.getData();
  }

  @Override
  protected Object resolveObject(final Object obj) {
    // when loading units, we want to maintain == relationships for many of the game data objects.
    // this is to prevent the situation where we have 2 Territory objects for the
    // the same territory, or two object for the same player id or ...
    // thus, in one vm you can add some units to a territory, and when you serialize the change
    // and look at the Territory object in another vm, the units have not been added
    if (obj instanceof GameData) {
      return dataSource.getData();
    } else if ((obj instanceof GameObjectStreamData)) {
      return ((GameObjectStreamData) obj).getReference(getData());
    } else if (obj instanceof Unit) {
      return resolveUnit((Unit) obj);
    } else {
      return obj;
    }
  }

  private Object resolveUnit(final Unit unit) {
    dataSource.getData().acquireReadLock();
    try {
      final Unit local = dataSource.getData().getUnits().get(unit.getId());
      if (local != null) {
        return local;
      }
      final Unit newLocal;
      if (ClientSetting.showBetaFeatures.getValueOrThrow()) {
        newLocal = new Unit(unit.getId(), unit.getType(), unit.getOwner(), unit.getData());
      } else {
        newLocal = unit;
      }
      dataSource.getData().getUnits().put(newLocal);
      return newLocal;
    } finally {
      dataSource.getData().releaseReadLock();
    }
  }
}
