/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * ActionPanel.java
 *
 * Created on December 4, 2001, 7:41 PM
 */

package games.strategy.triplea.ui;

import java.awt.*;
import javax.swing.*;

import games.strategy.engine.data.*;
import javax.swing.border.*;

/**
 *
 * Abstract superclass for all action panels. <br>
 *
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 */
public abstract class ActionPanel extends JPanel
{
	private GameData m_data;
	private PlayerID m_currentPlayer;
	private MapPanel m_map;
	private final Object m_lock = new Object();


  /** Creates new ActionPanel */
  public ActionPanel(GameData data, MapPanel map)
  {
    m_data = data;
    m_map = map;
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(new EmptyBorder(5,5,0,0));
  }

	protected GameData getData()
	{
		return m_data;
	}

	public void display(PlayerID player)
	{
		m_currentPlayer = player;
	}

	protected PlayerID getCurrentPlayer()
	{
		return m_currentPlayer;
	}

	protected MapPanel getMap()
	{
		return m_map;
	}

	/**
	 * Returns the object used to synchronize this action panel. <br>
	 * Never changes.
	 */
	protected Object getLock()
	{
		return m_lock;
	}

	/**
	 * Refreshes the action panel.  Should be run within the swing event queue.
	 */
	protected final Runnable REFRESH = new Runnable()
	{
		public void run()
		{
			revalidate();
			repaint();
		}
	};
}
