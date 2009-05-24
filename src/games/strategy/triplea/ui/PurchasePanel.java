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


import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.awt.event.ActionEvent;

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
 * @author  Sean Bridges
 * @version 1.0
 */
public class PurchasePanel extends ActionPanel
{

  private JLabel actionLabel = new JLabel();
  private IntegerMap<ProductionRule> m_purchase;
  private boolean m_bid;
  private SimpleUnitPanel m_purchasedPreviousRoundsUnits; 
  private JLabel m_purchasedPreviousRoundsLabel;
  private SimpleUnitPanel m_purhcasedUnits;
  private JLabel m_purchasedLabel = new JLabel();
  private JButton m_buyButton;

  private final String BUY = "Buy...";
  private final String CHANGE = "Change...";

  /** Creates new PurchasePanel */
  public PurchasePanel(GameData data,MapPanel map)
  {
    super(data, map);
    m_purchasedPreviousRoundsUnits = new SimpleUnitPanel(map.getUIContext());
    m_purhcasedUnits = new SimpleUnitPanel(map.getUIContext());
    m_buyButton = new JButton(BUY);
    m_buyButton.addActionListener(PURCHASE_ACTION);
    m_purchasedPreviousRoundsLabel = new JLabel("Unplaced from previous rounds");
  }
  
  public void display(final PlayerID id)
  {
    super.display(id);
    m_purchase = new IntegerMap<ProductionRule>();
    
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
            m_purchasedLabel.setText("");

            add(Box.createVerticalStrut(9));
            add(m_purchasedLabel);
            add(Box.createVerticalStrut(4));

            m_purhcasedUnits.setUnitsFromProductionRuleMap(
                new IntegerMap<ProductionRule>(), id, getData());
            add(m_purhcasedUnits);
            
            
            getData().acquireReadLock();
            try
            {
                m_purchasedPreviousRoundsUnits.setUnitsFromCategories(
                    UnitSeperator.categorize(id.getUnits().getUnits())
                    , getData());
                add(Box.createVerticalStrut(4));
                if(!id.getUnits().isEmpty()) {
                    add(m_purchasedPreviousRoundsLabel);
                    
                }
                add(m_purchasedPreviousRoundsUnits);
            } finally {
                getData().releaseReadLock();
            }
            
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

  public IntegerMap<ProductionRule> waitForPurchase(boolean bid)
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
    return m_purchase;
    
  }

  private final AbstractAction PURCHASE_ACTION = new AbstractAction("Buy")
  {
    public void actionPerformed(ActionEvent e)
    {
        PlayerID player = getCurrentPlayer();
        GameData data = getData();
        
    	m_purchase = ProductionPanel.getProduction(player, (JFrame) getTopLevelAncestor(), data, m_bid, m_purchase,getMap().getUIContext());
    	m_purhcasedUnits.setUnitsFromProductionRuleMap(m_purchase, player, data);
    	if(m_purchase.totalValues() == 0)
    	{
    		m_purchasedLabel.setText("");
    		m_buyButton.setText(BUY);
    	}
    	else
    	{
    		m_buyButton.setText(CHANGE);
    		m_purchasedLabel.setText(m_purchase.totalValues()+MyFormatter.pluralize(" unit", m_purchase.totalValues())+" to be produced:");
    	}
    }
  };

  private Action DoneAction = new AbstractAction("Done")
  {
    
    public void actionPerformed(ActionEvent event)
    {
     
        boolean hasPurchased = m_purchase.totalValues() != 0;
        if(!hasPurchased)
        {
            int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent( PurchasePanel.this), "Are you sure you dont want to buy anything?", "End Purchase", JOptionPane.YES_NO_OPTION);
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
            	    int addedProd = 0;
            	    PlayerID player = getCurrentPlayer();
            	    if(isIncreasedFactoryProduction(player))
            	        addedProd = 2;
            	    
            		for(Territory t : Match.getMatches(getData().getMap().getTerritories(), Matches.territoryHasOwnedFactory(getData(), getCurrentPlayer()))) 
                    {
            		    TerritoryAttachment ta = TerritoryAttachment.get(t);
                        int terrProd = ta.getUnitProduction();
                        if(ta.getProduction() > 2)
                            totalProd += Math.max(0, terrProd + addedProd);
                        else
                            totalProd += Math.max(0, terrProd);
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
            if(!m_bid &&  m_purchase.totalValues() + getCurrentPlayer().getUnits().size() > totalProd)
            {                
                int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent( PurchasePanel.this), "You have purchased more than you can place, continue with purchase?", "End Purchase", JOptionPane.YES_NO_OPTION);
                if(rVal != JOptionPane.YES_OPTION)
                {
                    return;
                }
                
            }
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
    return "PurchasePanel";
  }
}
