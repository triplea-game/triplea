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


import games.strategy.engine.data.*;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.formatter.MyFormatter;

import java.awt.event.ActionEvent;

import javax.swing.*;

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
  private SimpleUnitPanel m_unitsPanel;
  private JLabel m_purchasedSoFar = new JLabel();
  private JButton m_buyButton;

  private final String BUY = "Buy...";
  private final String CHANGE = "Change...";

  /** Creates new PurchasePanel */
  public PurchasePanel(GameData data,MapPanel map)
  {
    super(data, map);
    m_unitsPanel = new SimpleUnitPanel(map.getUIContext());
    m_buyButton = new JButton(BUY);
    m_buyButton.addActionListener(PURCHASE_ACTION);
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
            m_purchasedSoFar.setText("");

            add(Box.createVerticalStrut(9));
            add(m_purchasedSoFar);
            add(Box.createVerticalStrut(4));

            m_unitsPanel.setUnitsFromProductionRuleMap(new IntegerMap<ProductionRule>(), id, getData());
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
      m_purchase = ProductionPanel.getProduction(getCurrentPlayer(), (JFrame) getTopLevelAncestor(), getData(), m_bid, m_purchase,getMap().getUIContext());
      m_unitsPanel.setUnitsFromProductionRuleMap(m_purchase, getCurrentPlayer(), getData());
      if(m_purchase.totalValues() == 0)
      {
        m_purchasedSoFar.setText("");
        m_buyButton.setText(BUY);
      }
      else
      {
        m_buyButton.setText(CHANGE);
        m_purchasedSoFar.setText(m_purchase.totalValues()+MyFormatter.pluralize(" unit", m_purchase.totalValues())+" to be produced:");
      }
    }
  };

  private Action DoneAction = new AbstractAction("Done")
  {
    @SuppressWarnings("unchecked")
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
        if(isFourthEdition() || isRestrictedPurchase()) 
        {
            int totalProd = 0;
            getData().acquireReadLock();
            try
            {
                for(Territory t : Match.getMatches(getData().getMap().getTerritories(), Matches.territoryHasOwnedFactory(getData(), getCurrentPlayer()))) 
                {
                    totalProd += TerritoryAttachment.get(t).getProduction();
                }
            } finally
            {
                getData().releaseReadLock();
            }
            if(!m_bid &&  m_purchase.totalValues() > totalProd) 
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

  public String toString()
  {
    return "PurchasePanel";
  }
}
