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
 * PurchasePanel.java
 *
 * Created on December 4, 2001, 7:00 PM
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
public class PurchasePanel extends ActionPanel
{

	private JLabel actionLabel = new JLabel();
	private IntegerMap m_purchase;
	
	/** Creates new PurchasePanel */
    public PurchasePanel(GameData data,MapPanel map) 
	{
		super(data, map);
    }

	public void display(PlayerID id)
	{
		super.display(id);
		removeAll();
		actionLabel.setText(id.getName() + " production");
		add(actionLabel);
		add(new JButton(PURCHASE_ACTION));
		add(new JButton(DontBother));
		SwingUtilities.invokeLater(REFRESH);
	}
	
	public IntegerMap waitForPurchase()
	{
		synchronized(getLock())
		{
			try
			{
				getLock().wait();
			} catch(InterruptedException ie)
			{
				waitForPurchase();
			}
			return m_purchase;
		}
	}

	
	private final AbstractAction PURCHASE_ACTION = new AbstractAction("Buy")
	{
		public void actionPerformed(ActionEvent e)
		{
			m_purchase = ProductionPanel.show(getCurrentPlayer(), (JFrame) getTopLevelAncestor(), getData());
			synchronized(getLock())
			{
				getLock().notifyAll();
			}
		}
	};

	private Action DontBother = new AbstractAction("Done")
	{
		public void actionPerformed(ActionEvent event)
		{
			synchronized(getLock())
			{
				m_purchase = null;
				getLock().notifyAll();
			}
		}
	};
	
	public String toString()
	{
		return "PurchasePanel";
	}
}
