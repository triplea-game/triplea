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

import games.strategy.engine.data.Change;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.IBean;

import java.io.File;

/**
 * A dummy forum poster, for when Forum posting is disabled
 */
public class NullForumPoster implements IForumPoster
{
	private static final long serialVersionUID = 6465230505089142268L;

	public NullForumPoster()
	{
	}
	
	public String getDisplayName()
	{
		return "disabled";
	}
	

	public boolean getCanViewPosted()
	{
		return false;
	}
	
	public void setForumId(final String forumId)
	{
	}
	
	public void setUsername(final String s)
	{
	}
	
	public void setPassword(final String s)
	{
	}
	
	public String getForumId()
	{
		return null;
	}
	
	public String getUsername()
	{
		return null;
	}
	
	public String getPassword()
	{
		return null;
	}
	
	public void viewPosted()
	{
	}

	public void clearSensitiveInfo()
	{

	}

	public void gameStepChanged(final String stepName, final String delegateName, final PlayerID player, final int round, final String displayName)
	{
	}
	
	public void gameDataChanged(final Change change)
	{
	}

	public boolean postTurnSummary(String summary)
	{
		return false;
	}

	public String getTurnSummaryRef()
	{
		return null;
	}

	public boolean getIncludeSaveGame()
	{
		return false;
	}

	public void setIncludeSaveGame(boolean include)
	{
	}

	public void addSaveGame(File saveGame, String fileName)
	{
	}

	public IForumPoster doClone()
	{
		return null;
	}

	public boolean supportsSaveGame()
	{
		return false;
	}

	public EditorPanel getEditor()
	{
		return null;
	}

	public boolean sameType(IBean other)
	{
		return other.getClass() == NullForumPoster.class;
	}
}
