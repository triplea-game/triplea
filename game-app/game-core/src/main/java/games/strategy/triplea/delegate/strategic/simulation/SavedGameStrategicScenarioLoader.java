package games.strategy.triplea.delegate.strategic.simulation;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.framework.GameDataManager;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/** Loads a saved TripleA game and binds one player turn to the strategic environment. */
public final class SavedGameStrategicScenarioLoader implements StrategicScenarioLoader {
  private final Function<Path, Optional<GameData>> gameDataLoader;

  public SavedGameStrategicScenarioLoader() {
    this(GameDataManager::loadGame);
  }

  SavedGameStrategicScenarioLoader(final Function<Path, Optional<GameData>> gameDataLoader) {
    this.gameDataLoader = Objects.requireNonNull(gameDataLoader);
  }

  @Override
  public StrategicScenario load(final StrategicResetRequest request) {
    Objects.requireNonNull(request);
    final Path path = toExistingFile(request.scenarioPath());
    final GameData data =
        gameDataLoader
            .apply(path)
            .orElseThrow(
                () -> new IllegalArgumentException("could not load TripleA save game: " + path));
    final GamePlayer player = data.getPlayerList().getPlayerId(request.player());
    if (player == null || player.isNull()) {
      throw new IllegalArgumentException(
          "save game has no selectable player named "
              + request.player()
              + "; available: "
              + data.getPlayerList().getPlayers().stream()
                  .filter(candidate -> !candidate.isNull())
                  .map(GamePlayer::getName)
                  .sorted()
                  .toList());
    }
    return new LoadedStrategicScenario(data, player, request.seed(), request.maxActions());
  }

  private static Path toExistingFile(final String scenarioPath) {
    final Path path;
    try {
      path = Path.of(scenarioPath).toAbsolutePath().normalize();
    } catch (final InvalidPathException e) {
      throw new IllegalArgumentException("invalid scenarioPath: " + scenarioPath, e);
    }
    if (!Files.isRegularFile(path)) {
      throw new IllegalArgumentException("scenarioPath is not a regular file: " + path);
    }
    return path;
  }
}
