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
 * ProductionPanel.java
 *
 * Created on November 7, 2001, 10:19 AM
 */

package games.strategy.triplea.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import games.strategy.triplea.image.*;
import games.strategy.engine.data.*;
import games.strategy.ui.*;
import games.strategy.util.*;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.Constants;


/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 *
 */
public class ProductionPanel extends JPanel
{
  private static JFrame s_owner;
  private static JDialog s_dialog;
  private static ProductionPanel s_panel;

  private Collection m_rules = new ArrayList();
  private JLabel m_left = new JLabel();
  private PlayerID m_id;
  private boolean m_bid;
  private GameData m_data;

  /**
   * Shows the production panel, and returns a map of
   * selected rules.
   */
  public static IntegerMap show(PlayerID id, JFrame parent, GameData data, boolean bid, IntegerMap initialPurchase)
  {

    if(!(parent == s_owner))
      s_dialog = null;

    if(s_dialog == null)
      initDialog(parent);

    s_panel.m_bid = bid;
    s_panel.m_data = data;
    s_panel.initRules(id, data, initialPurchase);
    s_panel.initLayout(id);
    s_panel.calculateLimits();

    s_dialog.pack();
    s_dialog.setLocationRelativeTo(parent);
    s_dialog.show();

    return s_panel.getProduction();

  }



  private static void initDialog(JFrame root)
  {
    s_panel = new ProductionPanel();
    s_dialog = new JDialog(root, "Produce", true);
    s_dialog.getContentPane().add(s_panel);
  }

  /** Creates new ProductionPanel */
    private ProductionPanel()
  {

    }

  private void initRules(PlayerID player, GameData data, IntegerMap initialPurchase)
  {
    m_id= player;
    Iterator iter = player.getProductionFrontier().getRules().iterator();
    while(iter.hasNext())
    {
      ProductionRule productionRule = (ProductionRule) iter.next();
      Rule rule = new Rule(productionRule , data, player);
      int initialQuantity = initialPurchase.getInt(productionRule);
      rule.setQuantity(initialQuantity);
      m_rules.add(rule);
    }

  }

  private void initLayout(PlayerID id)
  {
    this.removeAll();
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    int ipcs = getIPCs();
    add(Box.createVerticalStrut(5));
    add(new JLabel("You have " + ipcs + " " + StringUtil.plural("IPC", ipcs) + "  to spend"));
    add(Box.createVerticalStrut(10));
    Iterator iter = m_rules.iterator();
    while(iter.hasNext())
    {
      this.add( (Rule) iter.next());
    }
    add(Box.createVerticalStrut(5));
    add(m_left);
    setLeft(ipcs);
    add(Box.createVerticalStrut(10));
    add(new JButton(m_done_action));
    add(Box.createVerticalStrut(10));


  }

  private void setLeft(int left)
  {
    m_left.setText("Left to spend:" + left);
  }

  Action m_done_action = new AbstractAction("Done")
  {
    public void actionPerformed(ActionEvent e)
    {
      s_dialog.setVisible(false);
    }
  };

  public IntegerMap getProduction()
  {
    IntegerMap prod = new IntegerMap();
    Iterator iter = m_rules.iterator();
    while(iter.hasNext())
    {
      Rule rule = (Rule) iter.next();
      int quantity = rule.getQuantity();
      if(quantity != 0)
      {
        prod.put(rule.getProductionRule(), quantity);
      }
    }
    return prod;
  }

  private void calculateLimits()
  {
    int ipcs = getIPCs();
    int spent = 0;
    Iterator iter = m_rules.iterator();
    while(iter.hasNext())
    {
      Rule current = (Rule) iter.next();
      spent += current.getQuantity() * current.getCost();
    }
    int leftToSpend = ipcs - spent;
    setLeft(leftToSpend);

    iter = m_rules.iterator();
    while(iter.hasNext())
    {
      Rule current = (Rule) iter.next();
      int max = leftToSpend /  current.getCost();
      max += current.getQuantity();
      current.setMax(max);
    }

  }

  private int getIPCs()
  {
    if(m_bid)
    {
      String propertyName = m_id.getName() + " bid";
      return Integer.parseInt(m_data.getProperties().get(propertyName).toString());
    }
    else
     return m_id.getResources().getQuantity(Constants.IPCS);
  }

  class Rule extends JPanel
  {
    private ScrollableTextField m_text = new ScrollableTextField(0, Integer.MAX_VALUE);
    private int m_cost;
    private GameData m_data;
    private UnitType m_type;
    private ProductionRule m_rule;

    Rule(ProductionRule rule, GameData data, PlayerID id)
    {
      m_data = data;
      m_rule = rule;
      m_cost = rule.getCosts().getInt( m_data.getResourceList().getResource(Constants.IPCS));
      m_type = (UnitType) rule.getResults().keySet().iterator().next();

      this.add( new JLabel(UnitIconImageFactory.instance().getIcon(m_type, id, m_data, false)));
      this.add(new JLabel( " x " + (m_cost < 10 ? " " : "") + m_cost));
      this.add(m_text);
      m_text.addChangeListener(m_listener);
    }

    int getCost()
    {
      return m_cost;
    }

    int getQuantity()
    {
      return m_text.getValue();
    }

    void setQuantity(int quantity)
    {
      m_text.setValue(quantity);
    }

    ProductionRule getProductionRule()
    {
      return m_rule;
    }

    void setMax(int max)
    {
      m_text.setMax(max);
    }
  }

  private ScrollableTextFieldListener m_listener = new ScrollableTextFieldListener()
  {
    public void changedValue(ScrollableTextField stf)
    {
      calculateLimits();
    }
  };

}


