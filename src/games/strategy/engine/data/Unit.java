/*
 * Unit.java
 *
 * Created on October 14, 2001, 12:33 PM
 */

package games.strategy.engine.data;

import java.io.*;
import java.util.*;
import games.strategy.net.GUID;


/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class Unit extends GameDataComponent implements Serializable
{
	//maps GUID -> Unit
	protected static Map s_allUnits = new WeakHashMap();
	
	static final long serialVersionUID = -4776804897761373923L;
	
	//for network identification purposes.
	private static GUID createUID()
	{
		return new GUID();
	}
	
	protected static Unit get(GUID id)
	{
		return (Unit) s_allUnits.get(id);
	}
	
	protected static void put(Unit unit)
	{
		s_allUnits.put(unit.getID(), unit);
	}
	
	private PlayerID m_owner;
	private GUID m_uid;
	
	private UnitType m_type;
	
	/** 
	 * Creates new Unit. Should use a call to UnitType.create() instead.
	 * owner can be null 
	 * id is the id of the unit, should be unique
	 */
	protected Unit(UnitType type, PlayerID owner, GameData data, short id) 
	{
		super(data);
		init(type, owner, data);
		m_uid = new GUID(id);
		s_allUnits.put(m_uid, this);
	}

	/** 
	 * Creates new Unit.  Should use a call to UnitType.create() instead.
	 * owner can be null 
	 */
    protected Unit(UnitType type, PlayerID owner, GameData data) 
	{
		super(data);
		init(type, owner, data);
		m_uid = createUID();
		s_allUnits.put(m_uid, this);
    }
	
	private void init(UnitType type, PlayerID owner, GameData data) 
	{
		m_type = type;
		setOwner(owner);
	}
	

	protected GUID getID()
	{
		return m_uid;
	}
	
	public UnitType getType()
	{
		return m_type;
	}
	
	public UnitType getUnitType()
	{
		return m_type;
	}
	
	public PlayerID getOwner()
	{
		return m_owner;
	}
	
	/**
	 * can be null.
	 */
	public void setOwner(PlayerID player)
	{
		if(player == null)
			player = PlayerID.NULL_PLAYERID;
		m_owner = player;
	}
		
	public boolean equals(Object o)
	{
		if(o == null || ! (o instanceof Unit))
			return false;
		
		Unit other = (Unit) o;
		return this.m_uid.equals(other.m_uid);
	}
	
	public int hashCode()
	{
		return m_uid.hashCode();
	}

	public String toString()
	{
		return m_type.getName() + " owned by " + m_owner.getName();
	}
	
}