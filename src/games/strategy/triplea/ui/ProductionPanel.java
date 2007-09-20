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

import games.strategy.engine.data.*;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.ui.*;
import games.strategy.util.IntegerMap;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EtchedBorder;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 *  
 */
public class ProductionPanel extends JPanel
{

    private JFrame m_owner;
    private JDialog m_dialog;
    private final UIContext m_uiContext;
    
    private List<Rule> m_rules = new ArrayList<Rule>();
    private JLabel m_left = new JLabel();
    private JButton m_done;
    private PlayerID m_id;
    private boolean m_bid;
    private GameData m_data;
    

    public static IntegerMap<ProductionRule> getProduction(PlayerID id, JFrame parent, GameData data, boolean bid, IntegerMap<ProductionRule> initialPurchase, UIContext context)
    {
        return new ProductionPanel(context).show(id, parent, data, bid, initialPurchase);
    }
    
    
    
    /**
     * Shows the production panel, and returns a map of selected rules.
     */
    public IntegerMap<ProductionRule> show(PlayerID id, JFrame parent, GameData data, boolean bid, IntegerMap<ProductionRule> initialPurchase)
    {
        if (!(parent == m_owner))
            m_dialog = null;

        if (m_dialog == null)
            initDialog(parent);

        this.m_bid = bid;
        this.m_data = data;
        this.initRules(id, data, initialPurchase);
        this.initLayout(id);
        this.calculateLimits();

        m_dialog.pack();
        m_dialog.setLocationRelativeTo(parent);
        m_done.requestFocusInWindow();
        m_dialog.setVisible(true);
        
        m_dialog.dispose();

        return getProduction();

    }

    // this method can be accessed by subclasses
    protected List<Rule> getRules()
    {
        return m_rules;
    };

    private void initDialog(JFrame root)
    {
      
        m_dialog = new JDialog(root, "Produce", true);
        m_dialog.getContentPane().add(this);

        Action closeAction = new AbstractAction("")
        {
            public void actionPerformed(ActionEvent e)
            {
                m_dialog.setVisible(false);
            }
        };
        
        //close the window on escape
        //this is mostly for developers, makes it much easier to quickly cycle through steps
        KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        
        String key = "production.panel.close.prod.popup";
        
        m_dialog.getRootPane().getActionMap().put(key, closeAction);
        m_dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(stroke, key);
    }

    /** Creates new ProductionPanel */
    // the constructor can be accessed by subclasses
    protected ProductionPanel(UIContext uiContext)
    {
        m_uiContext = uiContext;
        
      
    }

    private void initRules(PlayerID player, GameData data, IntegerMap<ProductionRule> initialPurchase)
    {
        m_data.acquireReadLock();
        try
        {
            m_id = player;
            
            for(ProductionRule productionRule : player.getProductionFrontier())
            {
                Rule rule = new Rule(productionRule, player, m_uiContext);
                int initialQuantity = initialPurchase.getInt(productionRule);
                rule.setQuantity(initialQuantity);
                m_rules.add(rule);
            }
        }
        finally 
        {
            m_data.releaseReadLock();
        }

    }

    private void initLayout(PlayerID id)
    {
        Insets nullInsets = new Insets(0, 0, 0, 0);
        this.removeAll();
        this.setLayout(new GridBagLayout());
        JLabel legendLabel = new JLabel("Attack/Defense/Movement");
        add(legendLabel,
                new GridBagConstraints(0, 0, 30, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 0), 0, 0));

        for (int x = 0; x < m_rules.size(); x++)
        {
            boolean even = (x / 2) * 2 == x;
            add(m_rules.get(x), new GridBagConstraints(x / 2, even ? 1 : 2, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL,
                    nullInsets, 0, 0));

        }

        add(m_left, new GridBagConstraints(0, 3, 30, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 8, 0, 12), 0, 0));
        m_done = new JButton(m_done_action);
        add(m_done, new GridBagConstraints(0, 4, 30, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,
                0, 8, 0), 0, 0));

    }

    // This method can be overridden by subclasses
    protected void setLeft(int left)
    {
        int total = getIPCs();
        int spent = total - left;
        
        m_left.setText("You have spent " + spent + " out of " + total +  " "  + StringUtil.plural("IPC", total)) ;
    }

    Action m_done_action = new AbstractAction("Done")
    {
        public void actionPerformed(ActionEvent e)
        {
            m_dialog.setVisible(false);
        }
    };

    private IntegerMap<ProductionRule> getProduction()
    {
        IntegerMap<ProductionRule> prod = new IntegerMap<ProductionRule>();
        Iterator<Rule> iter = m_rules.iterator();
        while (iter.hasNext())
        {
            Rule rule = iter.next();
            int quantity = rule.getQuantity();
            if (quantity != 0)
            {
                prod.put(rule.getProductionRule(), quantity);
            }
        }
        return prod;
    }

    // This method can be overridden by subclasses
    protected void calculateLimits()
    {
        int ipcs = getIPCs();
        int spent = 0;
        Iterator<Rule> iter = m_rules.iterator();
        while (iter.hasNext())
        {
            Rule current = iter.next();
            spent += current.getQuantity() * current.getCost();
        }
        int leftToSpend = ipcs - spent;
        setLeft(leftToSpend);

        iter = m_rules.iterator();
        while (iter.hasNext())
        {
            Rule current = iter.next();
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
        private ProductionRule m_rule;
 

        Rule(ProductionRule rule, PlayerID id, UIContext uiContext)
        {
            
            setLayout(new GridBagLayout());
            m_rule = rule;
            m_cost = rule.getCosts().getInt(m_data.getResourceList().getResource(Constants.IPCS));
            UnitType type = (UnitType) rule.getResults().keySet().iterator().next();
            UnitAttachment attach= UnitAttachment.get(type);
            int attack=attach.getAttack(id);
            int movement=attach.getMovement(id);
            int defense=attach.getDefense(id);
            Icon icon = m_uiContext.getUnitImageFactory().getIcon(type, id, m_data, false);
            String text = " x " + (m_cost < 10 ? " " : "") + m_cost;
            JLabel label = new JLabel(text, icon, SwingConstants.LEFT);
            JLabel info=new JLabel(attack+"/"+defense+"/"+movement);

            int space = 8;
            this.add(new JLabel(type.getName()), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE,
                    new Insets(2, 0, 0, 0), 0, 0));
            this.add(label, new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, space, space,
                    space), 0, 0));
            this.add(info, new GridBagConstraints(0, 2, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, space, space,
                    space), 0, 0));

            this.add(m_text, new GridBagConstraints(0, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, space,
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

