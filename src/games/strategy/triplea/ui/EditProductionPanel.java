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
import games.strategy.util.IntegerMap;

import java.util.Iterator;

import javax.swing.JFrame;

/**
 * 
 * @author Tony Clayton
 * 
 *  
 */
public class EditProductionPanel extends ProductionPanel
{

    public static IntegerMap<ProductionRule> getProduction(PlayerID id, JFrame parent, GameData data, UIContext context)
    {
        return new EditProductionPanel(context).show(id, parent, data, false, new IntegerMap<ProductionRule>());
    }
    
    /** Creates new ProductionPanel */
    private EditProductionPanel(UIContext uiContext)
    {
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

