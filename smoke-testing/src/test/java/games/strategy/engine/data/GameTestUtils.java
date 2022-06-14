package games.strategy.engine.data;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.map.file.system.loader.ZippedMapsExtractor;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.player.Player;
import games.strategy.net.LocalNoOpMessenger;
import games.strategy.net.Messengers;
import games.strategy.net.websocket.ClientNetworkBridge;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.settings.ClientSetting;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.game.server.HeadlessGameServer;
import org.triplea.game.server.HeadlessLaunchAction;
import org.triplea.io.ContentDownloader;
import org.triplea.io.FileUtils;
import org.triplea.java.collections.CollectionUtils;

@UtilityClass
@Slf4j
public class GameTestUtils {
  public static void setUp() throws IOException {
    if (ProductVersionReader.getCurrentVersionOptional().isEmpty()) {
      ProductVersionReader.init();
    }
    // Use a temp dir for downloaded maps to not interfere with the real downloadedMaps folder.
    Path tempHome = FileUtils.newTempFolder();
    System.setProperty("user.home", tempHome.toString());
    ClientSetting.initialize();
    assertTrue(ClientFileSystemHelper.getUserMapsFolder().startsWith(tempHome.toAbsolutePath()));

    Path tempRoot = FileUtils.newTempFolder();
    FileUtils.writeToFile(tempRoot.resolve(".triplea-root"), "");
    Files.createDirectory(tempRoot.resolve("assets"));
    ClientFileSystemHelper.setCodeSourceFolder(tempRoot);
    System.setProperty(GameRunner.TRIPLEA_HEADLESS, "true");
  }

  public static GameSelectorModel loadGameFromURI(String mapName, String mapXmlPath)
      throws Exception {
    GameSelectorModel gameSelector = new GameSelectorModel();
    gameSelector.load(downloadMap(getMapDownloadURI(mapName)).resolve(mapXmlPath));
    return gameSelector;
  }

  private static Path downloadMap(URI uri) throws IOException {
    Path targetTempFileToDownloadTo = FileUtils.newTempFolder().resolve("map.zip");
    log.info("Downloading from: " + uri);
    try (ContentDownloader downloader = new ContentDownloader(uri)) {
      Files.copy(downloader.getStream(), targetTempFileToDownloadTo);
    }
    return ZippedMapsExtractor.unzipMap(targetTempFileToDownloadTo).orElseThrow();
  }

  private static URI getMapDownloadURI(String mapName) throws URISyntaxException {
    return new URI(String.format("https://github.com/triplea-maps/%s/archive/master.zip", mapName));
  }

  public static ServerGame setUpGameWithAis(GameSelectorModel gameSelector) {
    GameData gameData = gameSelector.getGameData();
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
    return Optional.ofNullable(data.getUnitTypeList().getUnitType(name)).orElseThrow();
  }

  public static Territory getTerritory(GameData data, String name) {
    return Optional.ofNullable(data.getMap().getTerritory(name)).orElseThrow();
  }
}
