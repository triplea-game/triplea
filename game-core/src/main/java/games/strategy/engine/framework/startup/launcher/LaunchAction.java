package games.strategy.engine.framework.startup.launcher;

import java.io.File;
import java.util.Set;

import games.strategy.engine.chat.Chat;
import games.strategy.engine.framework.AutoSaveFileUtils;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.player.IGamePlayer;
import games.strategy.sound.ISound;
import games.strategy.triplea.ui.display.ITripleADisplay;

/**
 * Abstraction to allow decoupling the UI framework (namely swing)
 * from the launching code.
 * Ideally the common launching code has no compile-time dependency on swing at all.
 */
public interface LaunchAction {
  void handleGameInterruption(GameSelectorModel gameSelectorModel, ServerModel serverModel);

  void onGameInterrupt();

  void onEnd(String message);

  ITripleADisplay startGame(LocalPlayers localPlayers, IGame game, Set<IGamePlayer> players, Chat chat);

  ISound getSoundChannel(LocalPlayers localPlayers);

  File getAutoSaveFile();

  void onLaunch(ServerGame serverGame);

  AutoSaveFileUtils getAutoSaveFileUtils();
}
