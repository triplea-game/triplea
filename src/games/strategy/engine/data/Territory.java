/*
 * Territory.java
 *
 * Created on October 12, 2001, 1:50 PM
 */

package games.strategy.engine.data;

import java.io.Serializable;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class Territory extends NamedAttatchable implements NamedUnitHolder, Serializable
{
	private static final long serialVersionUID = -6390555051736721082L;
	
	private final boolean m_water;
	private PlayerID m_owner = PlayerID.NULL_PLAYERID;
	private final UnitCollection m_units; 
	
	/** Creates new Territory */
    public Territory(String name, boolean water, GameData data) 
	{
		super(name, data);
		m_water = water;
		m_units = new UnitCollection(this, getData());
    }
	
	public boolean isWater()
	{
		return m_water;
	}	
	
	/**
	 * May be null if not owned.
	 */
	public PlayerID getOwner()
	{
		return m_owner;
	}
	
	public void setOwner(PlayerID newOwner)
	{
		if(newOwner == null)
			newOwner = PlayerID.NULL_PLAYERID;
		m_owner = newOwner;
		getData().notifyTerritoryOwnerChanged(this);
	}

	public UnitCollection getUnits()
	{
		return m_units;
	}
	
	public void notifyChanged() 
	{
		getData().notifyTerritoryUnitsChanged(this);
	}
	
	public String toString()
	{
		return getName();
	}
}
