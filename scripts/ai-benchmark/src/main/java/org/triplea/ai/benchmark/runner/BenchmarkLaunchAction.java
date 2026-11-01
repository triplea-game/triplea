package org.triplea.ai.benchmark.runner;

import games.strategy.engine.chat.Chat;
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
import games.strategy.net.websocket.ClientNetworkBridge;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.ui.display.HeadlessDisplay;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.triplea.game.chat.ChatModel;
import org.triplea.sound.HeadlessSoundChannel;

/**
 * Launch hooks for a local, all-AI game with no network, lobby, chat or map resources. The
 * benchmark drives {@link ServerGame} directly, so the server-model hooks are never called.
 */
@Slf4j
final class BenchmarkLaunchAction implements LaunchAction {
  @Override
  public void startGame(
      final LocalPlayers localPlayers,
      final IGame game,
      final Set<Player> players,
      final Chat chat) {
    game.setResourceLoader(new ResourceLoader(List.of()));
    game.setDisplay(new HeadlessDisplay());
    game.setSoundChannel(new HeadlessSoundChannel());
  }

  @Override
  public Collection<PlayerTypes.Type> getPlayerTypes() {
    return PlayerTypes.getBuiltInPlayerTypes();
  }

  @Override
  public void onEnd(final String message) {
    log.info(message);
  }

  @Override
  public void handleError(final String error) {
    log.error(error);
  }

  @Override
  public boolean promptGameStop(
      final String status, final String title, @Nullable final Path mapLocation) {
    return true;
  }

  @Override
  public boolean shouldMinimizeExpensiveAiUse() {
    return false;
  }

  @Override
  public HeadlessAutoSaveFileUtils getAutoSaveFileUtils() {
    return new HeadlessAutoSaveFileUtils();
  }

  @Override
  public Path getAutoSaveFile() {
    return getAutoSaveFileUtils().getHeadlessAutoSaveFile();
  }

  @Override
  public WatcherThreadMessaging createThreadMessaging() {
    return new WatcherThreadMessaging.HeadlessWatcherThreadMessaging();
  }

  @Override
  public PlayerTypes.Type getDefaultLocalPlayerType() {
    return PlayerTypes.PRO_AI;
  }

  @Override
  public void onLaunch(final ServerGame serverGame) {}

  @Override
  public void onGameInterrupt() {}

  @Override
  public void handleGameInterruption(
      final GameSelectorModel gameSelectorModel, final ServerModel serverModel) {}

  @Override
  public Optional<ServerConnectionProps> getFallbackConnection(final Runnable cancelAction) {
    return Optional.empty();
  }

  @Override
  public ChatModel createChatModel(
      final String chatName,
      final Messengers messengers,
      final ClientNetworkBridge clientNetworkBridge) {
    throw new UnsupportedOperationException("benchmark games have no chat");
  }

  @Override
  public IServerStartupRemote getStartupRemote(
      final IServerStartupRemote.ServerModelView serverModelView) {
    throw new UnsupportedOperationException("benchmark games have no server model");
  }
}
