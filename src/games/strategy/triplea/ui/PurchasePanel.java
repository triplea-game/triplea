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

import games.strategy.triplea.Constants;
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
  private boolean m_bid;
  private SimpleUnitPanel m_unitsPanel = new SimpleUnitPanel();
  private JLabel m_purchasedSoFar = new JLabel();
  private JButton m_buyButton;

  private final String BUY = "Buy...";
  private final String CHANGE = "Change...";

  /** Creates new PurchasePanel */
  public PurchasePanel(GameData data,MapPanel map)
  {
    super(data, map);
    m_buyButton = new JButton(BUY);
    m_buyButton.addActionListener(PURCHASE_ACTION);
  }

  public void display(PlayerID id)
  {
    super.display(id);
    m_purchase = new IntegerMap();
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

    m_unitsPanel.setUnitsFromProductionRuleMap(new IntegerMap(), id, getData());
    add(m_unitsPanel);
    add(Box.createVerticalGlue());
    SwingUtilities.invokeLater(REFRESH);
  }

  private void refreshActionLabelText()
  {
    actionLabel.setText(getCurrentPlayer().getName() + " production " + (m_bid ? " for bid" : ""));
  }

  public IntegerMap waitForPurchase(boolean bid)
  {
    m_bid = bid;
    refreshActionLabelText();

    synchronized(getLock())
    {
      try
      {
        getLock().wait();
      } catch(InterruptedException ie)
      {
        waitForPurchase(bid);
      }
      return m_purchase;
    }
  }


  private final AbstractAction PURCHASE_ACTION = new AbstractAction("Buy")
  {
    public void actionPerformed(ActionEvent e)
    {
      m_purchase = ProductionPanel.show(getCurrentPlayer(), (JFrame) getTopLevelAncestor(), getData(), m_bid, m_purchase);
      m_unitsPanel.setUnitsFromProductionRuleMap(m_purchase, getCurrentPlayer(), getData());
      if(m_purchase.totalValues() == 0)
      {
        m_purchasedSoFar.setText("");
        m_buyButton.setText(BUY);
      }
      else
      {
        m_buyButton.setText(CHANGE);
        if(m_purchase.totalValues() == 1)
          m_purchasedSoFar.setText("1 unit to be prooduced:");
        else
          m_purchasedSoFar.setText(m_purchase.totalValues() + " units to be produced:");
      }
    }
  };

  private Action DoneAction = new AbstractAction("Done")
  {
    public void actionPerformed(ActionEvent event)
    {
      synchronized(getLock())
      {
        boolean hasPurchased = m_purchase.totalValues() != 0;
        if(!hasPurchased)
        {
            int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent( PurchasePanel.this), "Are you sure you dont want to buy anything?", "End Pruchase", JOptionPane.YES_NO_OPTION);
            if(rVal != JOptionPane.YES_OPTION)
            {
                return;
            }
        }
        
          
        getLock().notifyAll();
      }
    }
  };

  public String toString()
  {
    return "PurchasePanel";
  }
}
