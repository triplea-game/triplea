package org.triplea.ai.benchmark.runner;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.gameparser.GameParser;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.framework.GameDataManager;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.net.LocalNoOpMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.websocket.ClientNetworkBridge;
import games.strategy.triplea.settings.ClientSetting;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonatype.goodies.prefs.memory.MemoryPreferences;

/** Loads kata start positions and builds all-AI games that run in this JVM without a UI. */
final class HeadlessEngine {
  private HeadlessEngine() {}

  /** Once per JVM, before any game: in-memory settings, no AI pauses, an empty install root. */
  static void bootstrap() throws IOException {
    ClientSetting.setPreferences(new MemoryPreferences());
    ClientSetting.aiMovePauseDuration.setValue(0);
    ClientSetting.aiCombatStepPauseDuration.setValue(0);
    final Path root = Files.createTempDirectory("ai-benchmark");
    Files.writeString(root.resolve(".triplea-root"), "");
    Files.createDirectory(root.resolve("assets"));
    ClientFileSystemHelper.setCodeSourceFolder(root);
    System.setProperty(GameRunner.TRIPLEA_HEADLESS, "true");
  }

  /** A fresh copy of the kata's start position with its game option overrides applied. */
  static GameData load(final Kata kata) {
    final Path file = kata.start().file();
    final GameData data =
        switch (kata.start().kind()) {
          case MAP -> GameParser.parse(file, false).orElse(null);
          case SAVE -> GameDataManager.loadGame(file).orElse(null);
        };
    if (data == null) {
      throw new IllegalArgumentException("Cannot load " + file);
    }
    kata.props().forEach((name, value) -> setProperty(data, name, value));
    return data;
  }

  /** Builds a game where {@code seatAi} plays the seat and {@code otherAi} plays everyone else. */
  static ServerGame newGame(
      final GameData data,
      final List<GamePlayer> seatPlayers,
      final PlayerTypes.Type seatAi,
      final PlayerTypes.Type otherAi) {
    final Map<String, PlayerTypes.Type> types = new HashMap<>();
    for (final GamePlayer player : data.getPlayerList().getPlayers()) {
      types.put(player.getName(), seatPlayers.contains(player) ? seatAi : otherAi);
    }
    final var players = data.getGameLoader().newPlayers(types);
    final var launchAction = new BenchmarkLaunchAction();
    final ServerGame game =
        new ServerGame(
            data,
            players,
            new HashMap<>(),
            new Messengers(new LocalNoOpMessenger()),
            ClientNetworkBridge.NO_OP_SENDER,
            launchAction);
    game.setDelegateAutosavesEnabled(false);
    game.setStopGameOnDelegateExecutionStop(true);
    data.getGameLoader().startGame(game, players, launchAction, null);
    game.setUpGameForRunningSteps();
    return game;
  }

  /**
   * Sets a game option the way the game-options UI does. {@code GameProperties.set} only writes a
   * constant, which an editable property of the same name shadows on read.
   */
  @SuppressWarnings("unchecked")
  private static void setProperty(final GameData data, final String name, final String value) {
    final Object current = data.getProperties().get(name);
    if (current == null) {
      throw new IllegalArgumentException("props: no game option named '" + name + "'");
    }
    final Object parsed =
        switch (current) {
          case Boolean _ -> parseBoolean(name, value);
          case Integer _ -> Integer.valueOf(value);
          default -> value;
        };
    final var editable =
        (IEditableProperty<Object>) data.getProperties().getEditablePropertiesByName().get(name);
    if (editable == null) {
      data.getProperties().set(name, parsed);
    } else {
      editable.validateAndSet(parsed);
    }
    if (!data.getProperties().get(name).equals(parsed)) {
      throw new IllegalArgumentException("props: game option '" + name + "' rejected " + value);
    }
  }

  private static Boolean parseBoolean(final String name, final String value) {
    if (!List.of("true", "false").contains(value)) {
      throw new IllegalArgumentException("props: " + name + " needs true or false, not " + value);
    }
    return Boolean.valueOf(value);
  }
}
