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
public class PlayerID extends NamedAttatchable implements NamedUnitHolder, Serializable
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

	public static final PlayerID NULL_PLAYERID = new PlayerID("no one", true, null)
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

