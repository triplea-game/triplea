package games.strategy.engine.framework;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.io.IoUtils;

/** A collection of useful methods for working with instances of {@link GameData}. */
@Slf4j
@UtilityClass
public final class GameDataUtils {
  /**
   * Create a deep copy of GameData. <strong>You should have the game data's read or write lock
   * before calling this method</strong>
   */
  public static Optional<GameData> cloneGameData(GameData data, GameDataManager.Options options) {
    final byte[] bytes = gameDataToBytes(data, options).orElse(null);
    if (bytes != null) {
      return createGameDataFromBytes(bytes);
    }
    return Optional.empty();
  }

  public static Optional<byte[]> gameDataToBytes(GameData data, GameDataManager.Options options) {
    try {
      return Optional.of(
          IoUtils.writeToMemory(os -> GameDataManager.saveGameUncompressed(os, data, options)));
    } catch (final IOException e) {
      log.error("Failed to clone game data", e);
      return Optional.empty();
    }
  }

  public static Optional<GameData> createGameDataFromBytes(final byte[] bytes) {
    try {
      return IoUtils.readFromMemory(bytes, GameDataManager::loadGameUncompressed);
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

  public static Optional<GameData> cloneGameDataWithHistory(
      GameData gameData, boolean enableSeeking) {
    final var cloneOptions = GameDataManager.Options.builder().withHistory(true).build();
    Optional<GameData> optionalGameDataClone = cloneGameData(gameData, cloneOptions);
    if (enableSeeking) {
      optionalGameDataClone.ifPresent(clone -> clone.getHistory().enableSeeking(null));
    }
    return optionalGameDataClone;
  }
}
