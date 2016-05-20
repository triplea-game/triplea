package games.strategy.engine.framework.startup.mc;

import java.util.Set;

import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.message.IRemote;
import games.strategy.net.INode;

public interface IServerStartupRemote extends IRemote {
  /**
   * @return a listing of the players in the game
   */
  PlayerListing getPlayerListing();

  void takePlayer(INode who, String playerName);

  void releasePlayer(INode who, String playerName);

  void disablePlayer(String playerName);

  void enablePlayer(String playerName);

  /**
   * Has the game already started?
   * If true, the server will call our ObserverWaitingToJoin to start the game.
   * Note, the return value may come back after our ObserverWaitingToJoin has been created
   */
  boolean isGameStarted(INode newNode);

  boolean getIsServerHeadless();

  Set<String> getAvailableGames();

  void changeServerGameTo(final String gameName);

  void changeToLatestAutosave(final SaveGameFileChooser.AUTOSAVE_TYPE typeOfAutosave);

  void changeToGameSave(final byte[] bytes, final String fileName);

  byte[] getSaveGame();

  byte[] getGameOptions();

  void changeToGameOptions(final byte[] bytes);
}
