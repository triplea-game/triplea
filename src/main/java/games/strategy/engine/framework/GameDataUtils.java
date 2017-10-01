package games.strategy.engine.framework;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameObjectOutputStream;

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
      final ByteArrayOutputStream sink = new ByteArrayOutputStream(10_000);
      GameDataManager.saveGame(sink, data, copyDelegates);
      final ByteArrayInputStream source = new ByteArrayInputStream(sink.toByteArray());
      return GameDataManager.loadGame(source);
    } catch (final IOException e) {
      ClientLogger.logQuietly(e);
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
      final ByteArrayOutputStream sink = new ByteArrayOutputStream(1024);
      try (final GameObjectOutputStream out = new GameObjectOutputStream(sink)) {
        out.writeObject(object);
      }

      final GameObjectStreamFactory factory = new GameObjectStreamFactory(translateInto);
      try (final ByteArrayInputStream source = new ByteArrayInputStream(sink.toByteArray());
          final ObjectInputStream in = factory.create(source)) {
        return (T) in.readObject();
      } catch (final ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }
}
