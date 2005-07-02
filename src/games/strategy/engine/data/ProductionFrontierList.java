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
 * ProductionFrontierList.java
 *
 * Created on October 22, 2001, 10:20 AM
 */

package games.strategy.engine.data;

import java.util.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class ProductionFrontierList extends GameDataComponent
{
	private final Map<String, ProductionFrontier> m_productionFrontiers = new HashMap<String, ProductionFrontier>();
	
    public ProductionFrontierList(GameData data) 
	{
		super(data);
    }
	
	protected void addProductionFrontier(ProductionFrontier pf)
	{
		m_productionFrontiers.put(pf.getName(), pf);
	}
	
	public int size()
	{
		return m_productionFrontiers.size();
	}
	
	public ProductionFrontier getProductionFrontier(String name)
	{
		return m_productionFrontiers.get(name);
	}
	
	public Set<String> getProductionFrontierNames()
	{
	    return m_productionFrontiers.keySet();
	}
}