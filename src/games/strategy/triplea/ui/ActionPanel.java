/*
 * ActionPanel.java
 *
 * Created on December 4, 2001, 7:41 PM
 */

package games.strategy.triplea.ui;

import java.awt.*;
import javax.swing.*;

import games.strategy.engine.data.*;

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
