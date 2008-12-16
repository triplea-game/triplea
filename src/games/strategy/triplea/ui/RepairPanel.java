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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;

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

    public static IntegerMap<ProductionRule> getProduction(PlayerID id, JFrame parent, GameData data, UIContext context)
    {
        return new RepairPanel(context).show(id, parent, data, false, new IntegerMap<ProductionRule>());
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
                if(rule.toString().startsWith("repair"))
                {                	
                	int initialQuantity = initialPurchase.getInt(productionRule);
                	rule.setQuantity(initialQuantity);
                	m_rules.add(rule);
                }
            }
        }
        finally 
        {
            m_data.releaseReadLock();
        }
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

