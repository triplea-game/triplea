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
import games.strategy.util.Tuple;

import java.awt.*;
import java.math.BigDecimal;
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
    private int m_rows;
	private int m_columns;

	protected TabbedProductionPanel(UIContext uiContext) {
		super(uiContext);
	}

	public static IntegerMap<ProductionRule> getProduction(PlayerID id, JFrame parent, GameData data, boolean bid, IntegerMap<ProductionRule> initialPurchase, UIContext context)
    {
        return new TabbedProductionPanel(context).show(id, parent, data, bid, initialPurchase);
    }
       
    
    @Override
    protected void initLayout(PlayerID id)
    {
        this.removeAll();
        this.setLayout(new GridBagLayout());
        add(new JLabel("Attack/Defense/Movement"), new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 0), 0, 0));

        JTabbedPane tabs = new JTabbedPane();
        
        add(tabs,new GridBagConstraints(0,1,1,1,100,100,GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(8, 8, 8, 0), 0, 0));
        
        ProductionTabsProperties properties = ProductionTabsProperties.getInstance(m_id, m_rules, UIContext.getMapDir());
        List<Tuple<String,List<Rule>>> ruleLists = getRuleLists(properties);
        calculateXY(properties, largestList(ruleLists));
        
        for(Tuple<String,List<Rule>> ruleList:ruleLists) {
        	if(ruleList.getSecond().size()>0) {
        		tabs.addTab(ruleList.getFirst(), new JScrollPane(getRulesPanel(ruleList.getSecond())));
        	}
        }
               
        add(m_left, new GridBagConstraints(0, 2, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(8, 8, 0, 12), 0, 0));
        m_done = new JButton(m_done_action);
        add(m_done, new GridBagConstraints(0, 3, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0,  0, 8, 0), 0, 0));
        
        Dimension dtab = tabs.getPreferredSize();
        Dimension dthis = this.getPreferredSize();
        if (dtab != null && dthis != null)
        {
        	tabs.setPreferredSize(new Dimension(dtab.width + 4, dtab.height + 4)); // for whatever dumb reason, the tabs need a couple extra height and width or else scroll bars will appear
            this.setPreferredSize(new Dimension(dthis.width + 8, dthis.height + 24)); // for whatever dumb reason, the window needs to be at least 16 pixels greater in height than normal, to accommodate the tabs
        }
        tabs.validate();
        this.validate();
    }
    
    private void calculateXY(ProductionTabsProperties properties, int largestList) {
      	if(properties == null || properties.getRows()==0 || properties.getColumns()==0 || properties.getRows() * properties.getColumns() < largestList) {
    		int m_maxColumns;
        	if (largestList <= 36) // 8 columns 2 rows is perfect for small screens, 12 columns 3 rows is perfect for mid-sized screens, while 16 columns and 5-8 rows is perfect for really big screens.
            	m_maxColumns = Math.max(8, Math.min(12, new BigDecimal(largestList).divide(new BigDecimal(3),BigDecimal.ROUND_UP).intValue()));
            else if (largestList <= 64)
            	m_maxColumns = Math.max(8, Math.min(16, new BigDecimal(largestList).divide(new BigDecimal(4),BigDecimal.ROUND_UP).intValue()));
            else
            	m_maxColumns = Math.max(8, Math.min(16, new BigDecimal(largestList).divide(new BigDecimal(5),BigDecimal.ROUND_UP).intValue()));
    		m_rows = Math.max(2, new BigDecimal(largestList).divide(new BigDecimal(m_maxColumns),BigDecimal.ROUND_UP).intValue());
			m_columns = Math.max(3, new BigDecimal(largestList).divide(new BigDecimal(m_rows), BigDecimal.ROUND_UP).intValue());	
    	} else {
    		m_rows = Math.max(2, properties.getRows());
    	    m_columns = Math.max(3, properties.getColumns()); // There are small display problems if the size is less than 2x3 cells.
    	}
	}
    
    private int largestList(final List<Tuple<String,List<Rule>>> ruleLists) {
    	int largestList = 0;
    	for (Tuple<String,List<Rule>> tuple : ruleLists)
    	{
    		if (largestList < tuple.getSecond().size())
    			largestList = tuple.getSecond().size();
    	}
    	return largestList;
    }
    
    private void checkLists(final List<Tuple<String, List<Rule>>> ruleLists) {
    	List<Rule> rulesCopy = new ArrayList<Rule>(m_rules);
    	for (Tuple<String,List<Rule>> tuple : ruleLists)
    	{
    		for (Rule rule : tuple.getSecond())
    		{
    			rulesCopy.remove(rule);
    		}
    	}
    	if (rulesCopy.size() > 0)
    		throw new IllegalStateException("production_tabs: must include all player production rules/units");
    }

	private List<Tuple<String, List<Rule>>> getRuleLists(ProductionTabsProperties properties) {
    	
    	if(properties != null && !properties.useDefaultTabs()) {
    		List<Tuple<String, List<Rule>>> ruleLists = properties.getRuleLists();
    		checkLists(ruleLists);
    		return ruleLists;
    	} else {
    		return getDefaultRuleLists();
    	}
 
	}

	private List<Tuple<String, List<Rule>>> getDefaultRuleLists() {
    	
    	List<Tuple<String, List<Rule>>> ruleLists = new ArrayList<Tuple<String,List<Rule>>>();
        ArrayList<Rule> allRules = new ArrayList<Rule>();
        ArrayList<Rule> landRules = new ArrayList<Rule>();
        ArrayList<Rule> airRules = new ArrayList<Rule>();
        ArrayList<Rule> seaRules = new ArrayList<Rule>();
        ArrayList<Rule> constructRules = new ArrayList<Rule>();
        ArrayList<Rule> upgradeConsumesRules = new ArrayList<Rule>();

        for(Rule rule : m_rules) {
            UnitType type = (UnitType) rule.getProductionRule().getResults().keySet().iterator().next();
            UnitAttachment attach= UnitAttachment.get(type);
            
            allRules.add(rule);
            
            if (attach.getConsumesUnits() != null && attach.getConsumesUnits().totalValues() >= 1)
            	upgradeConsumesRules.add(rule);
            
            if(attach.isConstruction() || attach.isFactory()) { // canproduceUnits isn't checked on purpose, since this category is for units that can be placed anywhere (placed without needing a factory).
            	constructRules.add(rule);
            } else if(attach.isSea()) {
            	seaRules.add(rule);
            } else if(attach.isAir()) {
				airRules.add(rule);
            } else {
            	landRules.add(rule);
            }
        }
        
        ruleLists.add(new Tuple<String,List<Rule>>("All",allRules));
        ruleLists.add(new Tuple<String,List<Rule>>("Land",landRules));
        ruleLists.add(new Tuple<String,List<Rule>>("Air",airRules));
        ruleLists.add(new Tuple<String,List<Rule>>("Sea",seaRules));
        ruleLists.add(new Tuple<String,List<Rule>>("Construction",constructRules));
        ruleLists.add(new Tuple<String,List<Rule>>("Upgrades/Consumes",upgradeConsumesRules));
        
	    return ruleLists;
	}

	private JPanel getRulesPanel(List<Rule> rules)
    {
    	JPanel panel = new JPanel();
    	panel.setLayout(new GridLayout(m_rows,m_columns));
    	JPanel[][] panelHolder = new JPanel[m_rows][m_columns];
    	
    	for (int m = 0; m < m_rows; m++) {
    		for (int n = 0; n < m_columns; n++) {
    			panelHolder[m][n] = new JPanel(new BorderLayout());
    			panel.add(panelHolder[m][n]);
    		}
    	}
        
        for (int x = 0; x < m_columns * m_rows; x++)
        {
        	if (x < rules.size())
        		panelHolder[(x % m_rows)][(x / m_rows)].add(rules.get(x).getPanelComponent());
        	//else
        		//panelHolder[(x % m_rows)][(x / m_rows)].add(new JPanel());
        }
        return panel;
    }
}

