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
 *
 * Created on November 14, 2008, 9:34 AM
 */

package games.strategy.engine.data;

import java.io.*;
import java.util.Vector;

/**
 *
 * @author  Kevin Comcowich
 * @version 1.0
 */
public class NationalObjective extends NamedAttachable implements Attachable, Serializable 
{
	private static final long serialVersionUID = -3188649933172531176L;
	//private final Integer m_objectiveValue;
	private PlayerID m_objectivePlayer = PlayerID.NULL_PLAYERID;
	private final Boolean m_objectiveMet;
	

	/** Creates new National Objective **/
    public NationalObjective(String name, PlayerID player, Vector objectives, GameData data)
	{
		super(name, data);
		m_objectivePlayer=player;
		//m_objectiveValue=value;
		m_objectiveMet = false;
    }

/*	public Integer getObjectiveValue()
	{
		return m_objectiveValue;
	}*/
	
	public PlayerID getObjectivePlayer()
	{
		return m_objectivePlayer;
	}
	
	
	
	
	/*
	public void setProductionFrontier(ProductionFrontier frontier)
	{
		m_productionFrontier = frontier;
	}

	public ProductionFrontier getProductionFrontier()
	{
		return m_productionFrontier;
	}

	public void notifyChanged()
	{
	}

	public boolean isNull()
	{
		return false;
	}

	public static final PlayerID NULL_PLAYERID = new PlayerID("Neutral", true, null)
	{
        // compatible with 0.9.0.2 saved games
        private static final long serialVersionUID = -6596127754502509049L;
		public boolean isNull()
		{
			return true;
		}
	};

	public String toString()
	{
		return "PlayerID named:" + getName();
	}

    public String getType()
    {
        return UnitHolder.PLAYER;
    }
    */

}

