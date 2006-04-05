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
 * PlayerID.java
 *
 * Created on October 13, 2001, 9:34 AM
 */

package games.strategy.engine.data;

import java.io.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class PlayerID extends NamedAttachable implements NamedUnitHolder, Serializable
{
	private final boolean m_optional;
	private final UnitCollection m_unitsHeld;
	private final ResourceCollection m_resources;
	private ProductionFrontier m_productionFrontier;

	/** Creates new Player */
    public PlayerID(String name, boolean optional, GameData data)
	{
		super(name, data);
		m_optional = optional;
		m_unitsHeld = new UnitCollection(this, getData());
		m_resources = new ResourceCollection(getData());
    }

	public boolean getOptional()
	{
		return m_optional;
	}

	public UnitCollection getUnits()
	{
		return m_unitsHeld;
	}

	public ResourceCollection getResources()
	{
		return m_resources;
	}

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
		public boolean isNull()
		{
			return true;
		}
	};

	public String toString()
	{
		return "PlayerID named:" + getName();
	}


}

