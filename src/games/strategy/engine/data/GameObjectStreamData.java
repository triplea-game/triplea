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
 * GameObjectStreamData.java
 *
 * Created on January 3, 2002, 2:48 PM
 */

package games.strategy.engine.data;

import java.io.Serializable;

/**
 *
 * @author  Sean Bridges
 */
public class GameObjectStreamData implements Serializable
{

	private final static int PLAYERID = 1;
	private final static int UNITTYPE = 2;
	private final static int TERRITORY = 3;
	private final static int PRODUCTIONRULE = 4;
	private final static int PRODUCTIONFRONTIER = 5;

	public static boolean canSerialize(Named obj)
	{
		return
		   obj instanceof PlayerID ||
		   obj instanceof UnitType ||
		   obj instanceof Territory ||
		   obj instanceof ProductionRule ||
		   obj instanceof Attatchment ||
		   obj instanceof ProductionFrontier;
	}

	private String m_name;
	private int m_type;

	/** Creates a new instance of GameObjectStreamData */
    public GameObjectStreamData(Named named)
	{
		m_name = named.getName();

		if(named instanceof PlayerID)
		{
			m_type = PLAYERID;
		}
		else if(named instanceof Territory)
		{
			m_type = TERRITORY;
		}
		else if(named instanceof UnitType)
		{
			m_type = UNITTYPE;
		}
		else if(named instanceof ProductionRule)
		{
			m_type = PRODUCTIONRULE;
		}
		else if(named instanceof ProductionFrontier)
		{
		    m_type = PRODUCTIONFRONTIER;
		}
		else throw new IllegalArgumentException("Wrong type:" + named);
    }

	public Named getReference(GameData data)
	{
	    if(data == null)
			throw new IllegalArgumentException("Data cant be null");

		if(m_type == PLAYERID)
		{
			return data.getPlayerList().getPlayerID(m_name);
		}
		else if(m_type == TERRITORY)
		{
			return data.getMap().getTerritory(m_name);
		}
		else if(m_type == UNITTYPE)
		{
			return data.getUnitTypeList().getUnitType(m_name);
		}
		else if(m_type == PRODUCTIONRULE)
		{
			return data.getProductionRuleList().getProductionRule(m_name);
		}
		else if(m_type == PRODUCTIONFRONTIER)
		{
		    return data.getProductionFrontierList().getProductionFrontier(m_name);
		}
		else throw new IllegalArgumentException("Type not known:" + m_type);
	}
}
