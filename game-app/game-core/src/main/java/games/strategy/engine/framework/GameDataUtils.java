package games.strategy.engine.framework;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameObjectOutputStream;
import games.strategy.engine.history.History;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.io.IoUtils;

/// A collection of useful methods for working with instances of {@link GameData}.
@Slf4j
@UtilityClass
public final class GameDataUtils {
  public static GameData cloneGameDataKeepSameHistory(GameData gameData, boolean enableSeeking) {
    final var cloneOptions =
        GameDataManager.Options.builder()
            .withHistoryCopyMode(GameDataManager.Options.HistoryCopyMode.REFERENCE)
            .build();
    Optional<GameData> optionalGameDataClone = cloneGameData(gameData, cloneOptions);
    if (enableSeeking) {
      optionalGameDataClone.ifPresent(clone -> clone.getHistory().enableSeeking(null));
    }
    return optionalGameDataClone.orElseThrow(
        () -> new IllegalStateException("Game data clone expected."));
  }

  /// Creates a deep copy of the specified game data.
  ///
  /// **The game data's read or write lock must be held before calling this method.**
  ///
  /// @param data the game data to copy
  /// @param options the options controlling how the game data is copied
  /// @return the copied game data, or an empty {@link Optional} if the copy could not be created
  public static Optional<GameData> cloneGameData(GameData data, GameDataManager.Options options) {
    final byte[] bytes = gameDataToBytes(data, options).orElse(null);
    if (bytes != null) {
      Optional<GameData> gameDataCopyFromBytes = createGameDataFromBytes(bytes);
      if (options.withHistoryCopyMode == GameDataManager.Options.HistoryCopyMode.DEEP
          && gameDataCopyFromBytes.isPresent()) {
        History originalHistory = data.getGameHistory();
        gameDataCopyFromBytes.get().getGameHistory().cloneNodesFromHistory(originalHistory);
      }
      return gameDataCopyFromBytes;
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

  /// Translate units, territories and other game data objects from one game data into another.
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
