package games.strategy.engine.framework.startup.launcher;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.framework.AutoSaveFileUtils;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.WatcherThreadMessaging;
import games.strategy.engine.framework.startup.mc.IServerStartupRemote;
import games.strategy.engine.framework.startup.mc.ServerConnectionProps;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.player.Player;
import games.strategy.net.Messengers;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import org.triplea.game.chat.ChatModel;
import org.triplea.sound.ISound;

/**
 * Abstraction to allow decoupling the UI framework (namely swing) from the launching code. Ideally
 * the common launching code has no compile-time dependency on swing at all.
 */
public interface LaunchAction {
  void handleGameInterruption(GameSelectorModel gameSelectorModel, ServerModel serverModel);

  void onGameInterrupt();

  void onEnd(String message);

  IDisplay startGame(LocalPlayers localPlayers, IGame game, Set<Player> players, Chat chat);

  ISound getSoundChannel(LocalPlayers localPlayers);

  Path getAutoSaveFile();

  void onLaunch(ServerGame serverGame);

  AutoSaveFileUtils getAutoSaveFileUtils();

  ChatModel createChatModel(String chatName, Messengers messengers);

  PlayerTypes.Type getDefaultPlayerType();

  WatcherThreadMessaging createThreadMessaging();

  Optional<ServerConnectionProps> getFallbackConnection(Runnable cancelAction);

  void handleError(String error);

  IServerStartupRemote getStartupRemote(IServerStartupRemote.ServerModelView serverModelView);

  /**
   * Method to call if the game should try to stop the game. Implementations may choose to prompt
   * the user with a dialog to prevent stopping the game.
   *
   * @return true if the game should stop execution, false otherwise.
   */
  boolean promptGameStop(String status, String title);
}
