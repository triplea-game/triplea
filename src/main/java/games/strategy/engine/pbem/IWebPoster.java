package games.strategy.engine.pbem;

import java.io.File;
import java.util.Vector;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.startup.ui.editors.IBean;

/**
 * An interface for classes that can post a turn summary, the summary may also include a save game if the
 * implementing class supports this.
 */
public interface IWebPoster extends IBean {
  /**
   * Called when the turn summary should be posted.
   *
   * @return true if the post was successful
   */
  boolean postTurnSummary(final GameData gameData, final String turnSummary, final String player, int round);

  boolean getMailSaveGame();

  void setMailSaveGame(boolean mail);

  /**
   * Called to add the save game to the summary, this should only be called if getIncludeSaveGame returns true.
   *
   * @param saveGame
   *        the save game file
   */
  void addSaveGame(File saveGame, String fileName);

  /**
   * Create a clone of this object.
   *
   * @return the clone
   */
  IWebPoster doClone();

  void clearSensitiveInfo();

  /**
   * Get the display name.
   */
  @Override
  String getDisplayName();

  /**
   * Get the site id.
   *
   * @return the site id
   */
  String getSiteId();

  /**
   * Get the host URL.
   */
  String getHost();

  Vector<String> getAllHosts();

  String getGameName();

  void setSiteId(String siteId);

  void setGameName(String gameName);

  /**
   * Set the host name.
   */
  void setHost(String host);

  void setAllHosts(Vector<String> hosts);

  void addToAllHosts(String host);

  /**
   * Opens a browser and go to the web site, identified by the site id.
   */
  void viewSite();

  /**
   * Each poster provides a message that is displayed on the progress bar when testing the poster.
   *
   * @return the progress bar message
   */
  String getTestMessage();

  String getServerMessage();
}
