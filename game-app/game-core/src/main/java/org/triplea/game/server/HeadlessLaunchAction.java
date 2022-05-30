package org.triplea.game.server;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.chat.HeadlessChat;
import games.strategy.engine.chat.MessengersChatTransmitter;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.framework.HeadlessAutoSaveFileUtils;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.WatcherThreadMessaging;
import games.strategy.engine.framework.startup.launcher.LaunchAction;
import games.strategy.engine.framework.startup.mc.IServerStartupRemote;
import games.strategy.engine.framework.startup.mc.ServerConnectionProps;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.player.Player;
import games.strategy.net.Messengers;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.display.HeadlessDisplay;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.triplea.game.chat.ChatModel;
import org.triplea.sound.HeadlessSoundChannel;
import org.triplea.sound.ISound;

@Slf4j
public class HeadlessLaunchAction implements LaunchAction {

  private final HeadlessGameServer headlessGameServer;

  public HeadlessLaunchAction(final HeadlessGameServer headlessGameServer) {
    this.headlessGameServer = headlessGameServer;
  }

  @Override
  public void handleGameInterruption(
      final GameSelectorModel gameSelectorModel, final ServerModel serverModel) {
    log.info("Game ended, going back to waiting.");
    // if we do not do this, we can get into an infinite loop of launching a game,
    // then crashing out, then launching, etc.
    serverModel.setAllPlayersToNullNodes();
    final Path autoSaveFile = getAutoSaveFileUtils().getHeadlessAutoSaveFile();
    if (Files.exists(autoSaveFile)) {
      gameSelectorModel.load(autoSaveFile);
    }
  }

  @Override
  public void onGameInterrupt() {
    // tell headless server to wait for new connections:
    HeadlessGameServer.waitForUsersHeadlessInstance();
  }

  @Override
  public void onEnd(final String message) {
    log.info(message);
  }

  @Override
  public IDisplay startGame(
      final LocalPlayers localPlayers,
      final IGame game,
      final Set<Player> players,
      final Chat chat) {

    UiContext.setResourceLoader(game.getData());
    return new HeadlessDisplay();
  }

  @Override
  public ISound getSoundChannel(final LocalPlayers localPlayers) {
    return new HeadlessSoundChannel();
  }

  @Override
  public Path getAutoSaveFile() {
    return getAutoSaveFileUtils().getHeadlessAutoSaveFile();
  }

  @Override
  public void onLaunch(final ServerGame serverGame) {
    HeadlessGameServer.setServerGame(serverGame);
  }

  @Override
  public HeadlessAutoSaveFileUtils getAutoSaveFileUtils() {
    return new HeadlessAutoSaveFileUtils();
  }

  @Override
  public ChatModel createChatModel(String chatName, Messengers messengers) {
    return new HeadlessChat(new Chat(new MessengersChatTransmitter(chatName, messengers)));
  }

  @Override
  public PlayerTypes.Type getDefaultPlayerType() {
    return PlayerTypes.WEAK_AI;
  }

  @Override
  public WatcherThreadMessaging createThreadMessaging() {
    return new WatcherThreadMessaging.HeadlessWatcherThreadMessaging();
  }

  @Override
  public Optional<ServerConnectionProps> getFallbackConnection(Runnable cancelAction) {
    return Optional.empty();
  }

  @Override
  public void handleError(String error) {
    log.error(error);
  }

  @Override
  public IServerStartupRemote getStartupRemote(
      IServerStartupRemote.ServerModelView serverModelView) {
    return new HeadlessServerStartupRemote(serverModelView, headlessGameServer);
  }
}
