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
 * TabbedProductionPanel.java
 *
 * Created on June 11, 2011
 */

package games.strategy.triplea.ui;

import games.strategy.engine.data.*;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.util.IntegerMap;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

/**
 * 
 * @author Edwin van der Wal
 * @version 1.0
 * 
 *  
 */
public class TabbedProductionPanel extends ProductionPanel
{
    

    protected TabbedProductionPanel(UIContext uiContext) {
		super(uiContext);
	}

	public static IntegerMap<ProductionRule> getProduction(PlayerID id, JFrame parent, GameData data, boolean bid, IntegerMap<ProductionRule> initialPurchase, UIContext context)
    {
        return new TabbedProductionPanel(context).show(id, parent, data, bid, initialPurchase);
    }
        

    class UnitRulePanel extends JPanel {
    	protected UnitRulePanel() {
    		this.removeAll();
    		this.setLayout(new GridBagLayout());
    	}
    	
    	protected void addRules(List<Rule> mRules) {
    	       int rows = Math.max(1, mRules.size()/8);
    	         
    	         for (int x = 0; x < mRules.size(); x++)
    	         {
    	        	 add(mRules.get(x), new GridBagConstraints(x / rows, (x % rows), 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.BOTH,
    	        			 new Insets(0, 0, 0, 0), 0, 0));
    	        }
    	}
    }
    
    @Override
    protected void initLayout(PlayerID id)
    {
        Insets nullInsets = new Insets(0, 0, 0, 0);
        this.removeAll();
        this.setLayout(new GridBagLayout());
        JLabel legendLabel = new JLabel("Attack/Defense/Movement");
        add(legendLabel, new GridBagConstraints(0, 0, 30, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 0), 0, 0));

        JTabbedPane tabs = new JTabbedPane();
        UnitRulePanel allPanel = new UnitRulePanel();
        UnitRulePanel landPanel = new UnitRulePanel();
        UnitRulePanel airPanel = new UnitRulePanel();
        UnitRulePanel seaPanel = new UnitRulePanel();
        UnitRulePanel constructPanel = new UnitRulePanel();
        
        add(tabs,new GridBagConstraints(0,1,400,1,1,1,GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 0), 0, 0));

        ArrayList<Rule> allRules = new ArrayList<Rule>();
        ArrayList<Rule> landRules = new ArrayList<Rule>();
        ArrayList<Rule> airRules = new ArrayList<Rule>();
        ArrayList<Rule> seaRules = new ArrayList<Rule>();
        ArrayList<Rule> constructRules = new ArrayList<Rule>();


        for(Rule rule : m_rules) {
            UnitType type = (UnitType) rule.getProductionRule().getResults().keySet().iterator().next();
            UnitAttachment attach= UnitAttachment.get(type);
            allRules.add(rule);
            if(attach.isConstruction() || attach.isFactory()) {
            	constructRules.add(rule);
            } else if(attach.isSea()) {
            	seaRules.add(rule);
            } else if(attach.isAir()) {
				airRules.add(rule);
            } else {
            	landRules.add(rule);
            }
        }
        
        addRulesToPanelWithRows(allPanel, allRules);
        addRulesToPanelWithRows(landPanel, landRules);
        addRulesToPanelWithRows(airPanel, airRules);
        addRulesToPanelWithRows(seaPanel, seaRules);
        addRulesToPanelWithRows(constructPanel, constructRules);

        if(allRules.size()>0)
        	tabs.addTab("All", allPanel);
        if(landRules.size()>0)
        	tabs.addTab("Land", landPanel);
        if(airRules.size()>0)
        	tabs.addTab("Air", airPanel);
        if(seaRules.size()>0)
        	tabs.addTab("Sea", seaPanel);
        if(constructRules.size()>0)
        	tabs.addTab("Construction",constructPanel);
        
        add(m_left, new GridBagConstraints(0, 2, 30, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 8, 0, 12), 0, 0));
        m_done = new JButton(m_done_action);
        add(m_done, new GridBagConstraints(0, 3, 30, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,  0, 8, 0), 0, 0));
    }
    
    private void addRulesToPanelWithRows(UnitRulePanel panel, ArrayList<Rule> rules)
    {
        Insets nullInsets = new Insets(0, 0, 0, 0);

        int rows = rules.size() / 7;
        rows = Math.max(2, rows);
        
        for (int x = 0; x < rules.size(); x++)
        {
        	panel.add(rules.get(x), new GridBagConstraints(x / rows, (x % rows) + 1, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.BOTH, nullInsets, 0, 0));
        }
    }
}

