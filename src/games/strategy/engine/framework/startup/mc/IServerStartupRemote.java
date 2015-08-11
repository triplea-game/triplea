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
  public PlayerListing getPlayerListing();

  public void takePlayer(INode who, String playerName);

  public void releasePlayer(INode who, String playerName);

  public void disablePlayer(String playerName);

  public void enablePlayer(String playerName);

  /**
   * Has the game already started?
   * If true, the server will call our ObserverWaitingToJoin to start the game.
   * Note, the return value may come back after our ObserverWaitingToJoin has been created
   */
  public boolean isGameStarted(INode newNode);

  public boolean getIsServerHeadless();

  public Set<String> getAvailableGames();

  public void changeServerGameTo(final String gameName);

  public void changeToLatestAutosave(final SaveGameFileChooser.AUTOSAVE_TYPE typeOfAutosave);

  public void changeToGameSave(final byte[] bytes, final String fileName);

  public byte[] getSaveGame();

  public byte[] getGameOptions();

  public void changeToGameOptions(final byte[] bytes);
}
