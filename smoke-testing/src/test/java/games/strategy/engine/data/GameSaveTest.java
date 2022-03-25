package games.strategy.engine.data;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.GameDataFileUtils;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.game.server.HeadlessLaunchAction;
import org.triplea.injection.Injections;
import org.triplea.io.ContentDownloader;
import org.triplea.io.FileUtils;

/**
 * Checks that no error is encountered when saving a game on several different maps. This test
 * downloads the maps in question and starts an all-AI game on each of them, before saving.
 *
 * <p>Note: A variety of maps are used to ensure different engine features are exercised since
 * object serialization may run into errors that depend on the state of the object graph.
 */
class GameSaveTest {

  @BeforeAll
  public static void setUp() throws IOException {
    Injections.init(
        Injections.builder()
            .engineVersion(new ProductVersionReader().getVersion())
            .playerTypes(PlayerTypes.getBuiltInPlayerTypes())
            .build());
    ClientSetting.initialize();
    final Path tempFolder = FileUtils.newTempFolder();
    FileUtils.writeToFile(tempFolder.resolve(".triplea-root"), "");
    Files.createDirectory(tempFolder.resolve("assets"));
    ClientFileSystemHelper.setCodeSourceFolder(tempFolder);
  }

  @ParameterizedTest
  @CsvSource({
    "world_war_ii_revised,map/games/ww2v2.xml",
    "pacific_challenge,map/games/Pacific_Theater_Solo_Challenge.xml"
  })
  void testSaveGame(final String mapName, final String mapXmlPath) throws Exception {
    final Path mapFolderPath = downloadMap(getMapDownloadURI(mapName));
    final GameSelectorModel gameSelector = new GameSelectorModel();
    gameSelector.load(mapFolderPath.resolve(mapXmlPath));
    final ServerGame game = startGameWithAis(gameSelector);
    final Path saveFile = Files.createTempFile("save", GameDataFileUtils.getExtension());
    game.saveGame(saveFile);
    assertNotEquals(Files.size(saveFile), 0);
  }

  private static Path downloadMap(final URI uri) throws IOException {
    final Path targetTempFileToDownloadTo = FileUtils.newTempFolder().resolve("map.zip");
    try (ContentDownloader downloader = new ContentDownloader(uri)) {
      Files.copy(downloader.getStream(), targetTempFileToDownloadTo);
    }

    final Path[] mapFolderPath = new Path[1];
    ZippedMapsExtractor.unzipMap(targetTempFileToDownloadTo)
        .ifPresent(
            installedMap -> {
              mapFolderPath[0] = installedMap;
            });
    return mapFolderPath[0];
  }

  private static URI getMapDownloadURI(final String mapName) throws URISyntaxException {
    return new URI(String.format("https://github.com/triplea-maps/%s/archive/master.zip", mapName));
  }

  private static ServerGame startGameWithAis(final GameSelectorModel gameSelector) {
    final GameData gameData = gameSelector.getGameData();
    final Map<String, PlayerTypes.Type> playerTypes = new HashMap<>();
    for (var player : gameData.getPlayerList().getPlayers()) {
      playerTypes.put(player.getName(), PlayerTypes.PRO_AI);
    }
    final Set<Player> gamePlayers = gameData.getGameLoader().newPlayers(playerTypes);
    final HeadlessLaunchAction launchAction = new HeadlessLaunchAction();
    final Messengers messengers = new Messengers(new LocalNoOpMessenger());
    final ServerGame game =
        new ServerGame(
            gameData,
            gamePlayers,
            new HashMap<>(),
            messengers,
            ClientNetworkBridge.NO_OP_SENDER,
            launchAction);
    gameData.getGameLoader().startGame(game, gamePlayers, launchAction, null);
    return game;
  }
}
