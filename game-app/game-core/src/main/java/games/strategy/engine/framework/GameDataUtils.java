package games.strategy.engine.framework;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.injection.Injections;
import org.triplea.io.IoUtils;
import org.triplea.util.Version;

/** A collection of useful methods for working with instances of {@link GameData}. */
@Slf4j
@UtilityClass
public final class GameDataUtils {
  public static Optional<GameData> cloneGameData(GameData data, GameDataManager.Options options) {
    return cloneGameData(data, options, Injections.getInstance().getEngineVersion());
  }

  /**
   * Create a deep copy of GameData. <strong>You should have the game data's read or write lock
   * before calling this method</strong>
   */
  public static Optional<GameData> cloneGameData(
      GameData data, GameDataManager.Options options, Version engineVersion) {
    final byte[] bytes = gameDataToBytes(data, options, engineVersion).orElse(null);
    if (bytes != null) {
      return createGameDataFromBytes(bytes, engineVersion);
    }
    return Optional.empty();
  }

  public static Optional<byte[]> gameDataToBytes(
      GameData data, GameDataManager.Options options, Version engineVersion) {
    try {
      return Optional.of(
          IoUtils.writeToMemory(
              os -> GameDataManager.saveGameUncompressed(os, data, options, engineVersion)));
    } catch (final IOException e) {
      log.error("Failed to clone game data", e);
      return Optional.empty();
    }
  }

  public static Optional<GameData> createGameDataFromBytes(
      final byte[] bytes, final Version engineVersion) {
    try {
      return IoUtils.readFromMemory(
          bytes, inputStream -> GameDataManager.loadGameUncompressed(engineVersion, inputStream));
    } catch (final IOException e) {
      log.error("Failed to clone game data", e);
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
