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

/*
 * RepairPanel.java
 * 
 * Created on December 4, 2001, 7:00 PM
 */

package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.IntegerMap;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * 
 * @author Kevin Comcowich
 * @version 1.0
 */
public class RepairPanel extends ActionPanel
{
	
	private JLabel actionLabel = new JLabel();
	private HashMap<Unit, IntegerMap<RepairRule>> m_repair;
	
	private boolean m_bid;
	private SimpleUnitPanel m_unitsPanel;
	private JLabel m_repairdSoFar = new JLabel();
	private JButton m_buyButton;
	
	private final String BUY = "Repair...";
	private final String CHANGE = "Change...";
	
	/** Creates new RepairPanel */
	public RepairPanel(GameData data, MapPanel map)
	{
		super(data, map);
		m_unitsPanel = new SimpleUnitPanel(map.getUIContext());
		m_buyButton = new JButton(BUY);
		m_buyButton.addActionListener(PURCHASE_ACTION);
	}
	
	@Override
	public void display(final PlayerID id)
	{
		super.display(id);
		
		m_repair = new HashMap<Unit, IntegerMap<RepairRule>>();
		
		SwingUtilities.invokeLater(new Runnable()
		{
			
			@Override
			public void run()
		{
			removeAll();
			actionLabel.setText(id.getName() + " repair");
			m_buyButton.setText(BUY);
			add(actionLabel);
			add(m_buyButton);
			add(new JButton(DoneAction));
			m_repairdSoFar.setText("");
			
			add(Box.createVerticalStrut(9));
			add(m_repairdSoFar);
			add(Box.createVerticalStrut(4));
			
			m_unitsPanel.setUnitsFromRepairRuleMap(new HashMap<Unit, IntegerMap<RepairRule>>(), id, getData());
			add(m_unitsPanel);
			add(Box.createVerticalGlue());
			SwingUtilities.invokeLater(REFRESH);
		}
			
		});
		
	}
	
	private void refreshActionLabelText()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			
			@Override
			public void run()
		{
			actionLabel.setText(getCurrentPlayer().getName() + " production " + (m_bid ? " for bid" : ""));
		}
			
		});
		
	}
	
	public HashMap<Unit, IntegerMap<RepairRule>> waitForRepair(boolean bid)
	{
		m_bid = bid;
		refreshActionLabelText();
		
		// automatically "click" the buy button for us!
		SwingUtilities.invokeLater(
					new Runnable()
				{
					@Override
					public void run()
				{
					PURCHASE_ACTION.actionPerformed(null);
				}
				});
		
		waitForRelease();
		return m_repair;
		
	}
	
	private final AbstractAction PURCHASE_ACTION = new AbstractAction("Buy")
	{
		@Override
		public void actionPerformed(ActionEvent e)
	{
		PlayerID player = getCurrentPlayer();
		GameData data = getData();
		
		m_repair = ProductionRepairPanel.getProduction(player, (JFrame) getTopLevelAncestor(), data, m_bid, m_repair, getMap().getUIContext());
		
		m_unitsPanel.setUnitsFromRepairRuleMap(m_repair, player, data);
		int totalValues = getTotalValues(m_repair);
		if (totalValues == 0)
		{
			m_repairdSoFar.setText("");
			m_buyButton.setText(BUY);
		}
		else
		{
			m_buyButton.setText(CHANGE);
			m_repairdSoFar.setText(totalValues + MyFormatter.pluralize(" unit", totalValues) + " to be repaired:");
		}
	}
	};
	
	// Spin through the territories to get this.
	private int getTotalValues(HashMap<Unit, IntegerMap<RepairRule>> m_repair)
	{
		Collection<Unit> units = m_repair.keySet();
		Iterator<Unit> iter = units.iterator();
		int totalValues = 0;
		
		while (iter.hasNext())
		{
			Unit unit = iter.next();
			totalValues += m_repair.get(unit).totalValues();
		}
		return totalValues;
	}
	
	private Action DoneAction = new AbstractAction("Done")
	{
		
		@Override
		public void actionPerformed(ActionEvent event)
	{
		boolean hasPurchased = getTotalValues(m_repair) != 0;
		if (!hasPurchased)
		{
			int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(RepairPanel.this), "Are you sure you dont want to repair anything?", "End Purchase", JOptionPane.YES_NO_OPTION);
			if (rVal != JOptionPane.YES_OPTION)
			{
				return;
			}
		}
		
		// give a warning if the player tries to produce too much
		// does this piece of code even do anything? why is it here?
		/*if(isWW2V2() || isRestrictedPurchase() || isSBRAffectsUnitProduction() || isDamageFromBombingDoneToUnitsInsteadOfTerritories()) 
		{
		    int totalProd = 0;
		    getData().acquireReadLock();
		    try
		    {
		    	if(isSBRAffectsUnitProduction())
		    	{
		    	    int addedProd = 0;
		    	    PlayerID player = getCurrentPlayer();
		    	    if(isIncreasedFactoryProduction(player))
		    	        addedProd = 2;
		    	    
		    		for(Territory t : Match.getMatches(getData().getMap().getTerritories(), Matches.territoryHasOwnedIsFactoryOrCanProduceUnits(getData(), getCurrentPlayer()))) 
		            {
		    		    TerritoryAttachment ta = TerritoryAttachment.get(t);
		    		    int terrProd = ta.getUnitProduction();
		    		    if(ta.getProduction() > 2)
		    		        totalProd += Math.max(0, terrProd + addedProd);
		    		    else
		    		        totalProd += Math.max(0, terrProd);
		            }
		    	}
		    	else if (isDamageFromBombingDoneToUnitsInsteadOfTerritories())
		    	{
		    		// check to make sure we did not over repair the individual unit?
		    	}
		    	else
		    	{
		            for(Territory t : Match.getMatches(getData().getMap().getTerritories(), Matches.territoryHasOwnedIsFactoryOrCanProduceUnits(getData(), getCurrentPlayer()))) 
		            {
		                totalProd += TerritoryAttachment.get(t).getProduction();
		            }
		    	}
		    } finally
		    {
		        getData().releaseReadLock();
		    }
		}*/

		release();
		
	}
	};
	
	private boolean isIncreasedFactoryProduction(PlayerID player)
	{
		TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
		if (ta == null)
			return false;
		return ta.hasIncreasedFactoryProduction();
	}
	
	@Override
	public String toString()
	{
		return "RepairPanel";
	}
}
