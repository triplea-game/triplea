package games.strategy.engine.framework.startup.mc;

import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.RemoteActionCode;
import games.strategy.net.INode;
import java.util.List;

/**
 * Allows client nodes to access various information from the server node during network game setup.
 */
public interface IServerStartupRemote extends IRemote {
  /** Returns a listing of the players in the game. */
  @RemoteActionCode(9)
  PlayerListing getPlayerListing();

  @RemoteActionCode(13)
  void takePlayer(INode who, String playerName);

  @RemoteActionCode(12)
  void releasePlayer(INode who, String playerName);

  @RemoteActionCode(4)
  void disablePlayer(String playerName);

  @RemoteActionCode(5)
  void enablePlayer(String playerName);

  /**
   * Has the game already started? If true, the server will call our ObserverWaitingToJoin to start
   * the game. Note, the return value may come back after our ObserverWaitingToJoin has been created
   */
  @RemoteActionCode(11)
  boolean isGameStarted(INode newNode);

  @RemoteActionCode(8)
  boolean getIsServerHeadless();

  @RemoteActionCode(6)
  List<String> getAvailableGames();

  @RemoteActionCode(0)
  void changeServerGameTo(String gameName);

  @RemoteActionCode(2)
  void changeToGameSave(byte[] bytes, String fileName);

  @RemoteActionCode(7)
  byte[] getGameOptions();

  @RemoteActionCode(1)
  void changeToGameOptions(byte[] bytes);

  interface ServerModelView {
    PlayerListing getPlayerListing();

    void takePlayer(final INode who, final String playerName);

    void releasePlayer(final INode who, final String playerName);

    void disablePlayer(final String playerName);

    void enablePlayer(final String playerName);

    boolean isGameStarted(final INode newNode);

    byte[] getGameOptions();
  }
}
