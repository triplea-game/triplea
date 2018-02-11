package games.strategy.engine.framework;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
   * Clones a single GameData object multiple times.
   * This is faster than calling {@link GameDataUtils#cloneGameData(GameData)} multiple times,
   * because the original GameData object is only getting serialized once.
   * 
   * @param data The {@link GameData} to be cloned.
   * @param times The targeted amount of {@link GameData} objects the returned {@link List} should contain.
   * @return An unmodifiable {@link List} that containing clones of the passed data
   */
  public static List<GameData> cloneGameData(final GameData data, final int times) {
    try {
      final byte[] bytes = IoUtils.writeToMemory(os -> GameDataManager.saveGame(os, data, false));
      return Collections.unmodifiableList(
          IntStream.range(0, times).parallel()
              .mapToObj(it -> {
                try {
                  return IoUtils.readFromMemory(bytes, GameDataManager::loadGame);
                } catch (IOException e) {
                  ClientLogger.logQuietly("Failed to clone game data", e);
                  return null;
                }
              })
              .filter(Objects::nonNull)
              .collect(Collectors.toList()));
    } catch (IOException e) {
      ClientLogger.logQuietly("Failed to clone game data", e);
      return Collections.emptyList();
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
