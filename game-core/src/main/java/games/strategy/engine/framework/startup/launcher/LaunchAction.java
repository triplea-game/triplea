package games.strategy.engine.framework.startup.launcher;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.framework.AutoSaveFileUtils;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.player.Player;
import java.io.File;
import java.util.Set;
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

  File getAutoSaveFile();

  void onLaunch(ServerGame serverGame);

  AutoSaveFileUtils getAutoSaveFileUtils();
}
