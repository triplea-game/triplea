package games.strategy.engine.data;

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
import games.strategy.triplea.settings.ClientSetting;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.game.server.HeadlessGameServer;
import org.triplea.game.server.HeadlessLaunchAction;
import org.triplea.injection.Injections;
import org.triplea.io.ContentDownloader;
import org.triplea.io.FileUtils;

@UtilityClass
@Slf4j
public class GameTestUtils {
  public static void setUp() throws IOException {
    System.setProperty(GameRunner.TRIPLEA_HEADLESS, "true");
    if (Injections.getInstance() == null) {
      Injections.init(
          Injections.builder()
              .engineVersion(new ProductVersionReader().getVersion())
              .playerTypes(PlayerTypes.getBuiltInPlayerTypes())
              .build());
    }
    ClientSetting.initialize();
    final Path tempFolder = FileUtils.newTempFolder();
    FileUtils.writeToFile(tempFolder.resolve(".triplea-root"), "");
    Files.createDirectory(tempFolder.resolve("assets"));
    ClientFileSystemHelper.setCodeSourceFolder(tempFolder);
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
}
