/*
 * PlacePanel.java
 *
 * Created on December 4, 2001, 7:45 PM
 */

package games.strategy.triplea.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.data.events.*;

import games.strategy.triplea.delegate.message.*;


/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class PlacePanel extends ActionPanel 
{

	private JLabel actionLabel = new JLabel();
	private PlaceMessage m_placeMessage;
	
	/** Creates new PlacePanel */
    public PlacePanel(GameData data, MapPanel map) 
	{
		super(data, map);
    }
	
	public void display(PlayerID id)
	{
		super.display(id);
		
		removeAll();
		actionLabel.setText(id.getName() + " place");
		add(actionLabel);
		add(new JButton(DONE_PLACE_ACTION));
		SwingUtilities.invokeLater(REFRESH);
		
	}
	
	public PlaceMessage waitForPlace()
	{
		try
		{
			synchronized(getLock())
			{
				getMap().addMapSelectionListener(PLACE_MAP_SELECTION_LISTENER);
				getLock().wait();
				getMap().removeMapSelectionListener(PLACE_MAP_SELECTION_LISTENER);
			}
		} catch(InterruptedException ie)
		{
			return waitForPlace();
		}
		
		removeAll();
		SwingUtilities.invokeLater(REFRESH);
		return m_placeMessage;
	}

	private final AbstractAction DONE_PLACE_ACTION = new AbstractAction("done")
	{
		public void actionPerformed(ActionEvent e)
		{
			synchronized(getLock())
			{
				m_placeMessage = null;
				getLock().notify();
			}
		}
	};

	private final MapSelectionListener PLACE_MAP_SELECTION_LISTENER = new DefaultMapSelectionListener()
	{
		public void territorySelected(Territory territory) 
		{
			Collection units = getCurrentPlayer().getUnits().getUnits();
			UnitChooser chooser = new UnitChooser(units, Collections.EMPTY_MAP);
			String messageText = "Place units in " + territory.getName();
			int option = JOptionPane.showOptionDialog( (JFrame) getTopLevelAncestor(), chooser, messageText, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
			if(option == JOptionPane.OK_OPTION)
			{
				Collection choosen = chooser.getSelected();
				PlaceMessage message = new PlaceMessage(choosen, territory);
				m_placeMessage = message;
				synchronized(getLock())
				{
					getLock().notify();
				}
			}
		}
	};
	
	public String toString()
	{
		return "PlacePanel";
	}

}
