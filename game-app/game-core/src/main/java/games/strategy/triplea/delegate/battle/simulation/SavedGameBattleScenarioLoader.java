package games.strategy.triplea.delegate.battle.simulation;

import games.strategy.engine.data.GameData;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.triplea.delegate.battle.BattleDelegate;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.BattleTracker;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

/** Loads a saved TripleA game and exposes one pending observable battle as a scenario. */
public final class SavedGameBattleScenarioLoader implements BattleScenarioLoader {
  private static final Comparator<IBattle> BATTLE_ORDER =
      Comparator.comparing((IBattle battle) -> battle.getTerritory().getName())
          .thenComparing(battle -> battle.getBattleType().name())
          .thenComparing(battle -> battle.getBattleId().toString());

  private final Function<Path, Optional<GameData>> gameDataLoader;

  public SavedGameBattleScenarioLoader() {
    this(GameDataManager::loadGame);
  }

  SavedGameBattleScenarioLoader(final Function<Path, Optional<GameData>> gameDataLoader) {
    this.gameDataLoader = Objects.requireNonNull(gameDataLoader);
  }

  @Override
  public BattleScenario load(final BattleResetRequest request) {
    Objects.requireNonNull(request);
    final Path path = toExistingFile(request.scenarioPath());
    final GameData gameData =
        gameDataLoader
            .apply(path)
            .orElseThrow(
                () -> new IllegalArgumentException("could not load TripleA save game: " + path));
    final BattleTracker battleTracker = findBattleTracker(gameData);
    final List<IBattle> battles = observablePendingBattles(battleTracker);
    final IBattle selected = selectBattle(battles, request);
    return new LoadedBattleScenario(gameData, selected, request.seed());
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

  private static BattleTracker findBattleTracker(final GameData gameData) {
    final IDelegate delegate =
        gameData
            .getDelegateOptional("battle")
            .orElseThrow(() -> new IllegalArgumentException("save game has no battle delegate"));
    if (!(delegate instanceof BattleDelegate battleDelegate)) {
      throw new IllegalArgumentException(
          "save game's battle delegate has unsupported type: " + delegate.getClass().getName());
    }
    return battleDelegate.getBattleTracker();
  }

  private static List<IBattle> observablePendingBattles(final BattleTracker battleTracker) {
    return Arrays.stream(BattleType.values())
        .flatMap(type -> battleTracker.getPendingBattles(type).stream())
        .filter(BattleState.class::isInstance)
        .distinct()
        .sorted(BATTLE_ORDER)
        .toList();
  }

  private static IBattle selectBattle(
      final List<IBattle> battles, final BattleResetRequest request) {
    if (battles.isEmpty()) {
      throw new IllegalArgumentException(
          "save game has no pending battle that implements BattleState");
    }

    final UUID battleId = parseBattleId(request.battleId());
    Stream<IBattle> matches = battles.stream();
    if (battleId != null) {
      matches = matches.filter(battle -> battle.getBattleId().equals(battleId));
    }
    if (request.territory() != null) {
      matches =
          matches.filter(battle -> battle.getTerritory().getName().equals(request.territory()));
    }

    return matches
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "no pending observable battle matched the reset request; available: "
                        + describe(battles)));
  }

  private static UUID parseBattleId(final String battleId) {
    if (battleId == null) {
      return null;
    }
    try {
      return UUID.fromString(battleId);
    } catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException("battleId is not a valid UUID: " + battleId, e);
    }
  }

  private static String describe(final List<IBattle> battles) {
    return battles.stream()
        .map(
            battle ->
                battle.getBattleId()
                    + "@"
                    + battle.getTerritory().getName()
                    + "["
                    + battle.getBattleType().name()
                    + "]")
        .toList()
        .toString();
  }
}
