package games.strategy.engine.pbem;

import java.io.File;
import java.util.Vector;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.startup.ui.editors.IBean;

/**
 * An interface for classes that can post a turn summary, the summary may also include a save game if the
 * implementing class supports this
 */
public interface IWebPoster extends IBean {
  /**
   * Called when the turn summary should be posted
   *
   * @return true if the post was successful
   */
  public boolean postTurnSummary(final GameData gameData, final String turnSummary, final String player, int round);

  public boolean getMailSaveGame();

  public void setMailSaveGame(boolean mail);

  /**
   * Called to add the save game to the summary, this should only be called if getIncludeSaveGame returns true
   *
   * @param saveGame
   *        the save game file
   * @param fileName
   */
  void addSaveGame(File saveGame, String fileName);

  /**
   * Create a clone of this object
   *
   * @return the clone
   */
  public IWebPoster doClone();

  void clearSensitiveInfo();

  /**
   * Get the display name
   */
  @Override
  public String getDisplayName();

  /**
   * Get the site id
   *
   * @return the site id
   */
  public String getSiteId();

  /**
   * Get the host URL.
   */
  public String getHost();

  public Vector<String> getAllHosts();

  public String getGameName();

  public void setSiteId(String siteId);

  public void setGameName(String gameName);

  /**
   * Set the host name
   */
  public void setHost(String host);

  public void setAllHosts(Vector<String> hosts);

  public void addToAllHosts(String host);

  /**
   * Opens a browser and go to the web site, identified by the site id
   */
  public void viewSite();

  /**
   * Each poster provides a message that is displayed on the progress bar when testing the poster
   *
   * @return the progress bar message
   */
  String getTestMessage();

  String getServerMessage();
}
