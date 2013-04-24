/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.engine.pbem;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.startup.ui.editors.IBean;

import java.io.File;
import java.util.Vector;

/**
 * An interface for classes that can post a turn summary, the summary may also include a save game if the
 * implementing class supports this
 */
public interface IWebPoster extends IBean
{
	/**
	 * Called when the turn summary should be posted
	 * 
	 * @return true if the post was successful
	 */
	public boolean postTurnSummary(final GameData gameData, final String turnSummary, final String player, int round);
	
	/**
	 */
	public boolean getMailSaveGame();
	
	public void setMailSaveGame(boolean mail);
	
	/**
	 * Called to add the save game to the summary, this should only be called if getIncludeSaveGame returns true
	 * 
	 * @param saveGame
	 *            the save game file
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
	 * 
	 * @return
	 */
	public String getDisplayName();
	
	/**
	 * Get the site id
	 * 
	 * @return the site id
	 */
	public String getSiteId();
	
	/**
	 * Get the host URL.
	 * 
	 * @return
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
