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
	
	public static boolean canSerialize(Named obj)
	{
		return 
		   obj instanceof PlayerID ||
		   obj instanceof UnitType ||
		   obj instanceof Territory ||
		   obj instanceof ProductionRule;
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
		else throw new IllegalArgumentException("Wrong type:" + named);
    }
	
	public Named getReference(GameData data)
	{
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
		else throw new IllegalArgumentException("Type not known:" + m_type);
	}
}

