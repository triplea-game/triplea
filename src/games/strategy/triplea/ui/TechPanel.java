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
 * TechPanel.java
 *
 * Created on December 5, 2001, 7:04 PM
 */

package games.strategy.triplea.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import games.strategy.ui.*;
import games.strategy.util.*;
import games.strategy.util.Util;
import games.strategy.engine.data.*;
import games.strategy.engine.data.events.*;
import games.strategy.engine.message.Message;

import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.attatchments.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class TechPanel extends ActionPanel
{
  private JLabel m_actionLabel = new JLabel();
  private Message m_intMessage;

  /** Creates new BattlePanel */
    public TechPanel(GameData data, MapPanel map)
  {
    super(data, map);
    }

  public void display(PlayerID id)
  {
    super.display(id);
    removeAll();
    m_actionLabel.setText(id.getName() + " Tech Roll");
    add(m_actionLabel);
    add(new JButton(GetTechRollsAction));
    add(new JButton(DontBother));

    getMap().centerOn(TerritoryAttatchment.getCapital(id, getData()));

  }

  public String toString()
  {
    return "TechPanel";
  }

  public Message waitForTech()
  {
    try
    {
      synchronized(getLock())
      {
        getLock().wait();
      }
    } catch(InterruptedException ie)
    {
      waitForTech();
    }

    if(m_intMessage == null)
      return null;

    if( ((IntegerMessage) m_intMessage).getMessage() == 0)
      return null;

    return m_intMessage;
  }

  private List getAvailableTechs()
  {
      Collection currentAdvances  =TechTracker.getTechAdvances(getCurrentPlayer());
      Collection allAdvances =  TechAdvance.getTechAdvances(getData());
      
      return Util.difference(allAdvances, currentAdvances);
  }
  
  private boolean isFourthEdition()
  {
      return getData().getProperties().get(Constants.FOURTH_EDITION, false);
  }
  
  private Action GetTechRollsAction = new AbstractAction("Roll Tech...")
  {
    public void actionPerformed(ActionEvent event)
    {
      TechAdvance advance = null;
      if(isFourthEdition())
      {
          List available = getAvailableTechs();
          if(available.isEmpty())
          {
              JOptionPane.showMessageDialog(TechPanel.this, "No more available tech advances");
              return;
          }
          
          JList list = new JList(new Vector(available));
          JPanel panel = new JPanel();
          panel.setLayout(new BorderLayout());
          panel.add(list, BorderLayout.CENTER);
          panel.add(new JLabel("Select the tech you want to roll for"), BorderLayout.NORTH);
          list.setSelectedIndex(0);
          JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent( TechPanel.this), panel, "Select advance", JOptionPane.PLAIN_MESSAGE);
          advance = (TechAdvance) list.getSelectedValue();
      }
        
      int ipcs = getCurrentPlayer().getResources().getQuantity(Constants.IPCS);
      String message = "Roll Tech";
      TechRollPanel techRollPanel = new TechRollPanel(ipcs);
      int choice = JOptionPane.showConfirmDialog(getTopLevelAncestor(), techRollPanel, message, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
      if(choice != JOptionPane.OK_OPTION)
        return;

      int quantity = techRollPanel.getValue();
      if(advance == null)
          m_intMessage =  new IntegerMessage(quantity);
      else
          m_intMessage = new TechRollMessage(advance, quantity);
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
        m_intMessage = null;
        getLock().notifyAll();
      }
    }
  };


}

class TechRollPanel extends JPanel
{
  int m_ipcs;
  JLabel m_left = new JLabel();
  ScrollableTextField m_textField;

  TechRollPanel(int ipcs)
  {
    setLayout(new GridBagLayout());
    m_ipcs = ipcs;
    JLabel title = new JLabel("Select the number of tech rolls:");
    title.setBorder(new javax.swing.border.EmptyBorder(5,5,5,5));
    m_textField = new ScrollableTextField(0, ipcs / Constants.TECH_ROLL_COST);
    m_textField.addChangeListener(m_listener);
    JLabel costLabel = new JLabel("x5");
    setLabel(ipcs);
    int space = 0;
    add(title, new GridBagConstraints(0,0,3,1,1,1,GridBagConstraints.WEST,GridBagConstraints.NONE, new Insets(5,5,space,space),0,0));
    add(m_textField, new GridBagConstraints(0,1,1,1,0.5,1,GridBagConstraints.EAST,GridBagConstraints.NONE, new Insets(8,10,space,space),0,0));
    add(costLabel, new GridBagConstraints(1,1,1,1,0.5,1,GridBagConstraints.WEST,GridBagConstraints.NONE, new Insets(8,5,space,2),0,0));
    add(m_left, new GridBagConstraints(0,2,3,1,1,1,GridBagConstraints.WEST,GridBagConstraints.NONE, new Insets(10,5,space,space),0,0));
  }

  private void setLabel(int ipcs)
  {
    m_left.setText("Left to spend:" + ipcs);
  }

  private ScrollableTextFieldListener m_listener = new ScrollableTextFieldListener()
  {
    public void changedValue(ScrollableTextField stf)
    {
      setLabel(m_ipcs - (Constants.TECH_ROLL_COST * m_textField.getValue()));
    }
  };

  public int getValue()
  {
    return m_textField.getValue();
  }
}
