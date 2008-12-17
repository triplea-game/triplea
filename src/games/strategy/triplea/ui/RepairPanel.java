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
 * EditProductionPanel.java
 *
 */

package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.triplea.ui.ProductionPanel.Rule;
import games.strategy.util.IntegerMap;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.KeyStroke;

/**
 * 
 * @author Tony Clayton
 * 
 *  
 */
public class RepairPanel extends ProductionPanel
{
    private GameData m_data;
    private PlayerID m_id;
    private List<Rule> m_rules = new ArrayList<Rule>();
    private UIContext m_uiContext;
    private JFrame m_owner;
    private JDialog m_dialog;
    private boolean m_bid;
    private JButton m_done;
    private JLabel m_left = new JLabel();

    public static IntegerMap<ProductionRule> getProduction(PlayerID id, JFrame parent, GameData data, UIContext context)
    {
        return new RepairPanel(context).show(id, parent, data, false, new IntegerMap<ProductionRule>());
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

    private void initRules(PlayerID player, GameData data, IntegerMap<ProductionRule> initialPurchase)
    {
        m_data.acquireReadLock();
        try
        {
            m_id = player;
            kev
            for(ProductionRule productionRule : player.getRepairFrontier())
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

    /** Creates new RepairPanel */
    private RepairPanel(UIContext uiContext)
    {
        //m_uiContext = uiContext;
        super(uiContext);
    }

    protected void setLeft(int left)
    {
        // no limits, so do nothing here
    }

    protected void calculateLimits()
    {
        Iterator<Rule> iter = getRules().iterator();
        while (iter.hasNext())
        {
            Rule current = iter.next();
            current.setMax(99);
        }
    }

}

