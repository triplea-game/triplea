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
package games.strategy.common.ui;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.LocalPlayers;

import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * 
 * @author Lane Schwartz
 * 
 */
public abstract class MainGameFrame extends JFrame
{
	private static final long serialVersionUID = 7433347393639606647L;
	protected LocalPlayers m_localPlayers;
	
	public MainGameFrame(final String name, final LocalPlayers players)
	{
		super(name);
		m_localPlayers = players;
		setIconImage(GameRunner2.getGameIcon(this));
	}
	
	public abstract IGame getGame();
	
	public abstract void leaveGame();
	
	public abstract void stopGame();
	
	public abstract void shutdown();
	
	public abstract void notifyError(String error);
	
	public abstract JComponent getMainPanel();
	
	public abstract void setShowChatTime(final boolean showTime);
	
	public LocalPlayers getLocalPlayers()
	{
		return m_localPlayers;
	}
	/*
	public boolean playing(final PlayerID id)
	{
		return m_localPlayers.playing(id);
	}*/
}
