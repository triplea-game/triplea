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
import org.junit.jupiter.api.Test;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.game.server.HeadlessLaunchAction;
import org.triplea.injection.Injections;
import org.triplea.io.ContentDownloader;
import org.triplea.io.FileUtils;

class GameSaveTest {

  private static Injections constructInjections() {
    return Injections.builder()
        .engineVersion(new ProductVersionReader().getVersion())
        .playerTypes(PlayerTypes.getBuiltInPlayerTypes())
        .build();
  }

  @Test
  void testSaveGame() throws Exception {
    if (Injections.getInstance() == null) {
      Injections.init(constructInjections());
      ClientSetting.initialize();
      Path tempFolder = FileUtils.newTempFolder();
      FileUtils.writeToFile(tempFolder.resolve(".triplea-root"), "");
      Files.createDirectory(tempFolder.resolve("assets"));
      ClientFileSystemHelper.setCodeSourceFolder(tempFolder);
    }

    String path = "https://github.com/triplea-maps/pacific_challenge/archive/master.zip";

    final Path mapName = downloadMap(path);
    final GameSelectorModel gameSelector = new GameSelectorModel();
    gameSelector.load(mapName.resolve("map/games/Pacific_Theater_Solo_Challenge.xml"));
    final ServerGame game = startGameWithAis(gameSelector);
    final Path saveFile = Files.createTempFile("save", GameDataFileUtils.getExtension());
    game.saveGame(saveFile);
    assertNotEquals(Files.size(saveFile), 0);
  }

  private static Path downloadMap(final String path) throws IOException, URISyntaxException {
    final Path targetTempFileToDownloadTo = FileUtils.newTempFolder().resolve("map.zip");
    try (ContentDownloader downloader = new ContentDownloader(new URI(path))) {
      Files.copy(downloader.getStream(), targetTempFileToDownloadTo);
    }

    final Path[] mapName = new Path[1];
    ZippedMapsExtractor.unzipMap(targetTempFileToDownloadTo)
        .ifPresent(
            installedMap -> {
              mapName[0] = installedMap;
            });
    return mapName[0];
  }
  
  private static ServerGame startGameWithAis(final GameSelectorModel gameSelector) {
    final GameData gameData = gameSelector.getGameData();
    Map<String, PlayerTypes.Type> playerTypes = new HashMap<>();
    for (var player : gameData.getPlayerList().getPlayers()) {
      playerTypes.put(player.getName(), PlayerTypes.PRO_AI);
    }
    final Set<Player> gamePlayers = gameData.getGameLoader().newPlayers(playerTypes);
    HeadlessLaunchAction launchAction = new HeadlessLaunchAction();
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
