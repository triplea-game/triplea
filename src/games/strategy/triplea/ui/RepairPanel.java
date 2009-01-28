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
 * RepairPanel.java
 *
 * Created on December 4, 2001, 7:00 PM
 */

package games.strategy.triplea.ui;


import games.strategy.engine.data.*;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.ui.ProductionRepairPanel.Rule;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;

/**
 *
 * @author  Kevin Comcowich
 * @version 1.0
 */
public class RepairPanel extends ActionPanel
{

  private JLabel actionLabel = new JLabel();
  private IntegerMap<RepairRule> m_repair;
  private boolean m_bid;
  private SimpleUnitPanel m_unitsPanel;
  private JLabel m_repairdSoFar = new JLabel();
  private JButton m_buyButton;

  private final String BUY = "Buy...";
  private final String CHANGE = "Change...";
  
  //private UIContext m_map;

  /** Creates new RepairPanel */
  public RepairPanel(GameData data,MapPanel map)
  {
    super(data, map);
    m_unitsPanel = new SimpleUnitPanel(map.getUIContext());
    m_buyButton = new JButton(BUY);
    m_buyButton.addActionListener(PURCHASE_ACTION);
  }

  public void display(final PlayerID id)
  {
    super.display(id);
    m_repair = new IntegerMap<RepairRule>();
    
    SwingUtilities.invokeLater(new Runnable()
    {
    
        public void run()
        {
            removeAll();
            actionLabel.setText(id.getName() + " production");
            m_buyButton.setText(BUY);
            add(actionLabel);
            add(m_buyButton);
            add(new JButton(DoneAction));
            m_repairdSoFar.setText("");

            add(Box.createVerticalStrut(9));
            add(m_repairdSoFar);
            add(Box.createVerticalStrut(4));

            m_unitsPanel.setUnitsFromRepairRuleMap(new IntegerMap<RepairRule>(), id, getData());
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
    
        public void run()
        {
            actionLabel.setText(getCurrentPlayer().getName() + " production " + (m_bid ? " for bid" : ""));
        }
    
    });
    
  }

  public IntegerMap<RepairRule> waitForRepair(boolean bid)
  {
    m_bid = bid;
    refreshActionLabelText();
    
    //automatically "click" the buy button for us!
    SwingUtilities.invokeLater(
    		  new Runnable()
    		 {
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
    public void actionPerformed(ActionEvent e)
    {
        PlayerID player = getCurrentPlayer();
        GameData data = getData();
 /*
        	            m_repairPanel.setUnitsFromRepairRuleMap(new IntegerMap<RepairRule>(), player, getData());
        	            //TODO COMCO debit their IPCs for the repair
        	            int total = 2 * quantity;
        	            Resource ipcs = data.getResourceList().getResource(Constants.IPCS);
        	            String transcriptText = player.getName() + " spends " + quantity + MyFormatter.pluralize(" ipc", quantity)+"; to repair " + total+ " production";
        	            data.getHistory().
        	            aBridge.getHistoryWriter().startEvent(transcriptText);

        	            Change change = ChangeFactory.changeResourcesChange(player, ipcs, -quantity);
        	            aBridge.addChange(change);       	            
*/
        //m_map = getMap().getUIContext();
    	
    	m_repair = ProductionRepairPanel.getProduction(player, (JFrame) getTopLevelAncestor(), data, m_bid, m_repair, getMap().getUIContext());
    	m_unitsPanel.setUnitsFromRepairRuleMap(m_repair, player, data);
    	if(m_repair.totalValues() == 0)
    	{
    		m_repairdSoFar.setText("");
    		m_buyButton.setText(BUY);
    	}
    	else
    	{
    		m_buyButton.setText(CHANGE);
    		m_repairdSoFar.setText(m_repair.totalValues()+MyFormatter.pluralize(" unit", m_repair.totalValues())+" to be repaired:");
    	}
    }
  };

  private Action DoneAction = new AbstractAction("Done")
  {
    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent event)
    {
     
        boolean hasPurchased = m_repair.totalValues() != 0;
        if(!hasPurchased)
        {
            int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent( RepairPanel.this), "Are you sure you dont want to buy anything?", "End Purchase", JOptionPane.YES_NO_OPTION);
            if(rVal != JOptionPane.YES_OPTION)
            {
                return;
            }
        }
        
        //give a warning if the 
        //player tries to produce too much
      //Kev check here for factory max bug/feature request
        if(isFourthEdition() || isRestrictedPurchase() || isSBRAffectsUnitProduction()) 
        {
            int totalProd = 0;
            getData().acquireReadLock();
            try
            {
            	if(isSBRAffectsUnitProduction())
            	{
            	    /*ProductionRepairPanel prp = new ProductionRepairPanel(m_map);
                    List<Rule> rules = prp.getRules();*/
            	    
            	    int addedProd = 0;
            	    PlayerID player = getCurrentPlayer();
            	    if(isIncreasedFactoryProduction(player))
            	        addedProd = 2;
            	    
            		for(Territory t : Match.getMatches(getData().getMap().getTerritories(), Matches.territoryHasOwnedFactory(getData(), getCurrentPlayer()))) 
                    {
            		    int terrProd = TerritoryAttachment.get(t).getUnitProduction();
            		    totalProd += Math.max(0, terrProd + addedProd);
                    }
            	}
            	else
            	{            		
	                for(Territory t : Match.getMatches(getData().getMap().getTerritories(), Matches.territoryHasOwnedFactory(getData(), getCurrentPlayer()))) 
	                {
	                    totalProd += TerritoryAttachment.get(t).getProduction();
	                }
            	}
            } finally
            {
                getData().releaseReadLock();
            }
            /*if(!m_bid &&  m_repair.totalValues() > totalProd) 
            {                
                int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent( RepairPanel.this), "You have purchased more than you can place, continue with purchase?", "End Purchase", JOptionPane.YES_NO_OPTION);
                if(rVal != JOptionPane.YES_OPTION)
                {
                    return;
                }
                
            }*/
        }
        
          
        release();
     
    }
  };

  private boolean isIncreasedFactoryProduction(PlayerID player)    
  {
      TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTATCHMENT_NAME);
      if(ta == null)
      	return false;
      return ta.hasIncreasedFactoryProduction();
  }
  
  public String toString()
  {
    return "RepairPanel";
  }
}
