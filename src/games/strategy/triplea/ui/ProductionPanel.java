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
import java.util.List;
import javax.swing.border.*;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 *  
 */
public class ProductionPanel extends JPanel
{
    private static JFrame s_owner;
    private static JDialog s_dialog;
    private static ProductionPanel s_panel;

    private List m_rules = new ArrayList();
    private JLabel m_left = new JLabel();
    private PlayerID m_id;
    private boolean m_bid;
    private GameData m_data;

    /**
     * Shows the production panel, and returns a map of selected rules.
     */
    public static IntegerMap show(PlayerID id, JFrame parent, GameData data, boolean bid, IntegerMap initialPurchase)
    {

        if (!(parent == s_owner))
            s_dialog = null;

        if (s_dialog == null)
            initDialog(parent);

        s_panel.m_bid = bid;
        s_panel.m_data = data;
        s_panel.initRules(id, data, initialPurchase);
        s_panel.initLayout(id);
        s_panel.calculateLimits();

        s_dialog.pack();
        s_dialog.setLocationRelativeTo(parent);
        s_dialog.setVisible(true);

        return s_panel.getProduction();

    }

    private static void initDialog(JFrame root)
    {
        s_panel = new ProductionPanel();
        s_dialog = new JDialog(root, "Produce", true);
        s_dialog.getContentPane().add(s_panel);

        Action closeAction = new AbstractAction("")
        {
            public void actionPerformed(ActionEvent e)
            {
                s_dialog.setVisible(false);
            }
        };
        
        //close the window on escape
        //this is mostly for developers, makes it much easier to quickly cycle through steps
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        s_dialog.getRootPane().registerKeyboardAction(closeAction, stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
    
    }

    /** Creates new ProductionPanel */
    private ProductionPanel()
    {
        
      
    }

    private void initRules(PlayerID player, GameData data, IntegerMap initialPurchase)
    {
        m_id = player;
        Iterator iter = player.getProductionFrontier().getRules().iterator();
        while (iter.hasNext())
        {
            ProductionRule productionRule = (ProductionRule) iter.next();
            Rule rule = new Rule(productionRule, data, player);
            int initialQuantity = initialPurchase.getInt(productionRule);
            rule.setQuantity(initialQuantity);
            m_rules.add(rule);
        }

    }

    private void initLayout(PlayerID id)
    {
        Insets nullInsets = new Insets(0, 0, 0, 0);
        this.removeAll();
        this.setLayout(new GridBagLayout());
        int ipcs = getIPCs();
        JLabel totalIPCs = new JLabel("You have " + ipcs + " " + StringUtil.plural("IPC", ipcs) + "  to spend");
        add(totalIPCs,
                new GridBagConstraints(0, 0, 30, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 0), 0, 0));

        for (int x = 0; x < m_rules.size(); x++)
        {
            boolean even = (x / 2) * 2 == x;
            add((Rule) m_rules.get(x), new GridBagConstraints(x / 2, even ? 1 : 2, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
                    nullInsets, 0, 0));

        }

        add(m_left, new GridBagConstraints(0, 3, 30, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 8, 0, 12), 0, 0));
        setLeft(ipcs);

        add(new JButton(m_done_action), new GridBagConstraints(0, 4, 30, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,
                0, 8, 0), 0, 0));

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
        while (iter.hasNext())
        {
            Rule rule = (Rule) iter.next();
            int quantity = rule.getQuantity();
            if (quantity != 0)
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
        while (iter.hasNext())
        {
            Rule current = (Rule) iter.next();
            spent += current.getQuantity() * current.getCost();
        }
        int leftToSpend = ipcs - spent;
        setLeft(leftToSpend);

        iter = m_rules.iterator();
        while (iter.hasNext())
        {
            Rule current = (Rule) iter.next();
            int max = leftToSpend / current.getCost();
            max += current.getQuantity();
            current.setMax(max);
        }

    }

    private int getIPCs()
    {
        if (m_bid)
        {
            String propertyName = m_id.getName() + " bid";
            return Integer.parseInt(m_data.getProperties().get(propertyName).toString());
        } else
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
            setLayout(new GridBagLayout());
            m_data = data;
            m_rule = rule;
            m_cost = rule.getCosts().getInt(m_data.getResourceList().getResource(Constants.IPCS));
            m_type = (UnitType) rule.getResults().keySet().iterator().next();
            Icon icon = UnitImageFactory.instance().getIcon(m_type, id, m_data, false);
            String text = " x " + (m_cost < 10 ? " " : "") + m_cost;
            JLabel label = new JLabel(text, icon, SwingConstants.LEFT);

            int space = 8;
            this.add(new JLabel(m_type.getName()), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    new Insets(2, 0, 0, 0), 0, 0));
            this.add(label, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, space, space,
                    space), 0, 0));

            this.add(m_text, new GridBagConstraints(0, 2, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, space,
                    space, space), 0, 0));

            m_text.addChangeListener(m_listener);
            setBorder(new EtchedBorder());
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

