package games.strategy.engine.data;

import static org.mockito.Mockito.mock;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.gameparser.GameParser;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.player.Player;
import games.strategy.net.LocalNoOpMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.websocket.ClientNetworkBridge;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.settings.ClientSetting;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.sonatype.goodies.prefs.memory.MemoryPreferences;
import org.triplea.game.server.HeadlessGameServer;
import org.triplea.game.server.HeadlessLaunchAction;
import org.triplea.io.FileUtils;
import org.triplea.java.collections.CollectionUtils;

@UtilityClass
@Slf4j
public class GameTestUtils {
  public static void setUp() throws IOException {

    HeadlessLaunchAction.setSkipMapResourceLoading(true);

    ClientSetting.setPreferences(new MemoryPreferences());
    ClientSetting.aiMovePauseDuration.setValue(0);
    ClientSetting.aiCombatStepPauseDuration.setValue(0);

    Path tempRoot = FileUtils.newTempFolder();
    FileUtils.writeToFile(tempRoot.resolve(".triplea-root"), "");
    Files.createDirectory(tempRoot.resolve("assets"));
    ClientFileSystemHelper.setCodeSourceFolder(tempRoot);
    System.setProperty(GameRunner.TRIPLEA_HEADLESS, "true");
  }

  public static ServerGame setUpGameWithAis(String xmlName) {
    Path xmlFilePath = Path.of("src", "test", "resources", "map-xmls", xmlName);
    if (!Files.exists(xmlFilePath)) {
      throw new IllegalStateException(
          "Error, expected test file to exist: " + xmlFilePath.toAbsolutePath());
    }

    GameData gameData =
        GameParser.parse(xmlFilePath, false)
            .orElseThrow(() -> new RuntimeException("Error parsing file: " + xmlFilePath));
    Map<String, PlayerTypes.Type> playerTypes = new HashMap<>();
    for (var player : gameData.getPlayerList().getPlayers()) {
      playerTypes.put(player.getName(), PlayerTypes.PRO_AI);
    }
    Set<Player> gamePlayers = gameData.getGameLoader().newPlayers(playerTypes);
    HeadlessLaunchAction launchAction = new HeadlessLaunchAction(mock(HeadlessGameServer.class));
    Messengers messengers = new Messengers(new LocalNoOpMessenger());
    ServerGame game =
        new ServerGame(
            gameData,
            gamePlayers,
            new HashMap<>(),
            messengers,
            ClientNetworkBridge.NO_OP_SENDER,
            launchAction);
    game.setDelegateAutosavesEnabled(false);
    // Note: This doesn't actually start the AI players' turns. For that, call game.startGame().
    gameData.getGameLoader().startGame(game, gamePlayers, launchAction, null);
    return game;
  }

  public static void runStepsUntil(ServerGame game, String stopAfterStepName) {
    boolean done = false;
    while (!done) {
      done = game.getData().getSequence().getStep().getName().equals(stopAfterStepName);
      game.runNextStep();
    }
  }

  public static void addUnits(Territory t, GamePlayer player, String... unitTypes) {
    for (String unitType : unitTypes) {
      t.getUnitCollection().add(getUnitType(player.getData(), unitType).create(player));
    }
  }

  public static int countUnitsOfType(GamePlayer player, String unitType) {
    // Note: We don't use game.getUnits() because units are never removed from there.
    // We also don't use player.getUnits() because those are just the units-to-place.
    int count = 0;
    for (Territory t : player.getData().getMap().getTerritories()) {
      count += countUnitsOfType(t, player, unitType);
    }
    return count;
  }

  public static int countUnitsOfType(Territory t, GamePlayer p, String unitType) {
    Predicate<Unit> matcher =
        Matches.unitIsOwnedBy(p).and(Matches.unitIsOfType(getUnitType(p.getData(), unitType)));
    return CollectionUtils.countMatches(t.getUnits(), matcher);
  }

  public static UnitType getUnitType(GameData data, String name) {
    return data.getUnitTypeList().getUnitTypeOrThrow(name);
  }

  public static Territory getTerritoryOrThrow(GameData data, String name) {
    return data.getMap().getTerritoryOrThrow(name);
  }
}
