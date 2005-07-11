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

import java.io.*;

/**
 *
 * @author  Sean Bridges
 */
public class GameObjectStreamData implements Externalizable
{

    enum GameType
    {
      PLAYERID, UNITTYPE, TERRITORY, PRODUCTIONRULE, PRODUCTIONFRONTIER 
    }
    

	public static boolean canSerialize(Named obj)
	{
		return
		   obj instanceof PlayerID ||
		   obj instanceof UnitType ||
		   obj instanceof Territory ||
		   obj instanceof ProductionRule ||
		   obj instanceof IAttatchment ||
		   obj instanceof ProductionFrontier;
	}

	private String m_name;
	private GameType m_type;

	public GameObjectStreamData()
	{
	    
	}
	
	/** Creates a new instance of GameObjectStreamData */
    public GameObjectStreamData(Named named)
	{
		m_name = named.getName();

		if(named instanceof PlayerID)
		{
			m_type = GameType.PLAYERID;
		}
		else if(named instanceof Territory)
		{
			m_type = GameType.TERRITORY;
		}
		else if(named instanceof UnitType)
		{
			m_type = GameType.UNITTYPE;
		}
		else if(named instanceof ProductionRule)
		{
			m_type = GameType.PRODUCTIONRULE;
		}
		else if(named instanceof ProductionFrontier)
		{
		    m_type = GameType.PRODUCTIONFRONTIER;
		}
		else throw new IllegalArgumentException("Wrong type:" + named);
    }

	public Named getReference(GameData data)
	{
	    if(data == null)
			throw new IllegalArgumentException("Data cant be null");

        switch(m_type)
        {
            case PLAYERID : 
                return data.getPlayerList().getPlayerID(m_name);
            case TERRITORY :
                return data.getMap().getTerritory(m_name);
            case UNITTYPE :
                return data.getUnitTypeList().getUnitType(m_name);
            case PRODUCTIONRULE :
                return data.getProductionRuleList().getProductionRule(m_name);
            case PRODUCTIONFRONTIER:
                return data.getProductionFrontierList().getProductionFrontier(m_name);
        }
        throw new IllegalStateException("Unknown type" + this);
	}


    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        m_name = (String) in.readObject();
        m_type = GameType.values()[in.readByte()];
        
    }

    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeObject(m_name);
        out.writeByte((byte) m_type.ordinal());
    }
}
