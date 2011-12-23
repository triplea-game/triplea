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

import games.strategy.engine.framework.startup.ui.editors.IBean;

import java.io.File;

/**
 * An interface for classes that can post a turn summary, the summary may also include a save game if the
 * implementing class supports this
 */
public interface IForumPoster extends IBean
{
	/**
	 * Called when the turn summary should be posted
	 *
	 *
	 * @param summary the summary
	 * @return true if the post was successful
	 */
	public boolean postTurnSummary(String summary);

	/**
	 * Get the reference to the posted turn summary
	 * @return the reference string, often a URL
	 */
	public String getTurnSummaryRef();

	/**
	 * Should the summary include the save game. When this is true the client must call addSaveGame prior to calling
	 * postSummary
	 * @return true if the save game should be included in the summary
	 */
	public boolean getIncludeSaveGame();

	/**
	 * Configure if the save game should be included in the summary post
	 * @param include true if the save game should be included
	 */
	public void setIncludeSaveGame(boolean include);

	/**
	 * Called to add the save game to the summary, this should only be called if getIncludeSaveGame returns true
	 *
	 * @param saveGame the save game file
	 * @param fileName
	 */
	void addSaveGame(File saveGame, String fileName);

	/**
	 * Create a clone of this object
	 * @return the clone
	 */
	IForumPoster doClone();


	/**
	 * Returns true if this forum poster supports attaching save games
	 * @return true if save games are supported
	 */
	public boolean supportsSaveGame();

	/**
	 * Can you view the forum post with this poster
	 * @return
	 */
	public boolean getCanViewPosted();

	/**
	 * Get the user name to login with
	 * @return
	 */
	public String getDisplayName();


	/**
	 * Get the forum id
	 * @return the forum id
	 */
	public String getForumId();

	/**
	 * get the user name to login
	 * @return the user name
	 */
	public String getUsername();

	/**
	 * Get the password to login
 	 * @return the password
	 */
	public String getPassword();

	/**
	 * Set the forum id
	 * @param forumId the new forum id
	 */
	public void setForumId(String forumId);

	/**
	 * Set the user name
	 * @param username the new user name
	 */
	public void setUsername(String username);

	/**
	 * Set the password
	 * @param password the new password
	 */
	public void setPassword(String password);

	/**
	 * Opens a browser and go to the forum post, identified by the forumId
	 */
	public void viewPosted();

	/**
	 * Remove any sensitive information, like passwords before this object is saved in as a game properties
	 */
	void clearSensitiveInfo();


}
