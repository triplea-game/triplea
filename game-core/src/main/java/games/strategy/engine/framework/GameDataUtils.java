package games.strategy.engine.framework;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameObjectOutputStream;
import games.strategy.engine.history.History;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Optional;
import java.util.logging.Level;
import lombok.extern.java.Log;
import org.triplea.injection.Injections;
import org.triplea.io.IoUtils;
import org.triplea.util.Version;

/** A collection of useful methods for working with instances of {@link GameData}. */
@Log
public final class GameDataUtils {
  private GameDataUtils() {}

  /**
   * Create a deep copy of GameData without history as it can get large. <strong>You should have the
   * game data's write lock before calling this method</strong>
   */
  public static Optional<GameData> cloneGameDataWithoutHistory(
      final GameData data, final boolean copyDelegates, final Version engineVersion) {
    final History temp = data.getHistory();
    data.resetHistory();
    final Optional<GameData> dataCopy = cloneGameData(data, copyDelegates, engineVersion);
    data.setHistory(temp);
    return dataCopy;
  }

  /**
   * Serializes a game data object but lops off the history data from it. Use this for a faster
   * serialization that does not require the history.
   */
  public static byte[] serializeGameDataWithoutHistory(final GameData data) {
    final History temp = data.getHistory();
    data.resetHistory();
    final byte[] bytes;
    try {
      bytes =
          IoUtils.writeToMemory(
              os ->
                  GameDataManager.saveGame(
                      os, data, false, Injections.getInstance().getEngineVersion()));
    } catch (final IOException e) {
      throw new RuntimeException("Failed to serialize GameData", e);
    } finally {
      data.setHistory(temp);
    }
    return bytes;
  }

  public static Optional<GameData> cloneGameData(final GameData data) {
    return cloneGameData(data, false, Injections.getInstance().getEngineVersion());
  }

  /**
   * Create a deep copy of GameData. <strong>You should have the game data's read or write lock
   * before calling this method</strong>
   */
  public static Optional<GameData> cloneGameData(
      final GameData data, final boolean copyDelegates, final Version engineVersion) {
    try {
      final byte[] bytes =
          IoUtils.writeToMemory(
              os -> GameDataManager.saveGame(os, data, copyDelegates, engineVersion));
      return IoUtils.readFromMemory(
          bytes, inputStream -> GameDataManager.loadGame(engineVersion, inputStream));
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Failed to clone game data", e);
      return Optional.empty();
    }
  }

  /** Translate units, territories and other game data objects from one game data into another. */
  @SuppressWarnings("unchecked")
  public static <T> T translateIntoOtherGameData(final T object, final GameData translateInto) {
    try {
      final byte[] bytes =
          IoUtils.writeToMemory(
              os -> {
                try (ObjectOutputStream out = new GameObjectOutputStream(os)) {
                  out.writeObject(object);
                }
              });
      return IoUtils.readFromMemory(
          bytes,
          is -> {
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
