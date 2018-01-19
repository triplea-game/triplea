package games.strategy.engine.framework;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameObjectOutputStream;
import games.strategy.io.IoUtils;

/**
 * A collection of useful methods for working with instances of {@link GameData}.
 */
public final class GameDataUtils {
  private GameDataUtils() {}

  public static GameData cloneGameData(final GameData data) {
    return cloneGameData(data, false);
  }

  /**
   * Create a deep copy of GameData.
   * <strong>You should have the game data's read or write lock before calling this method</strong>
   */
  public static GameData cloneGameData(final GameData data, final boolean copyDelegates) {
    try {
      final byte[] bytes = IoUtils.writeToMemory(os -> GameDataManager.saveGame(os, data, copyDelegates));
      return IoUtils.readFromMemory(bytes, GameDataManager::loadGame);
    } catch (final IOException e) {
      ClientLogger.logQuietly("Failed to clone game data", e);
      return null;
    }
  }

  /**
   * Translate units,territories and other game data objects from one
   * game data into another.
   */
  @SuppressWarnings("unchecked")
  public static <T> T translateIntoOtherGameData(final T object, final GameData translateInto) {
    try {
      final byte[] bytes = IoUtils.writeToMemory(os -> {
        try (ObjectOutputStream out = new GameObjectOutputStream(os)) {
          out.writeObject(object);
        }
      });
      return IoUtils.readFromMemory(bytes, is -> {
        final GameObjectStreamFactory factory = new GameObjectStreamFactory(translateInto);
        try (ObjectInputStream in = factory.create(is)) {
          return (T) in.readObject();
        } catch (final ClassNotFoundException e) {
          throw new IOException(e);
        }
      });
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
