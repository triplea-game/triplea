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
 * RepairRuleList.java
 *
 * Created on October 22, 2001, 10:23 AM
 */

package games.strategy.engine.data;

import java.util.*;

/**
 *
 * @author  Kevin Comcowich
 * @version 1.0
 */
public class RepairRuleList extends GameDataComponent
{
	private final Map<String, RepairRule> m_repairRules = new HashMap<String, RepairRule>();
	
    public RepairRuleList(GameData data) 
	{
		super(data);
    }
	
	protected void addRepairRule(RepairRule pf)
	{
		m_repairRules.put(pf.getName(), pf);
	}
	
	public int size()
	{
		return m_repairRules.size();
	}
	
	public RepairRule getRepairRule(String name)
	{
		return m_repairRules.get(name);
	}
}