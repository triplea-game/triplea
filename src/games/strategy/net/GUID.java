/*
 * GUID.java
 *
 * Created on January 4, 2002, 2:33 PM
 */

package games.strategy.net;

import java.io.Serializable;

/**
 *
 * A globally unique id.  <br>
 * Backed by a java.rmi.dgc.VMID.
 * 
 * @author  Sean Bridges
 * @see java.rmi.dgc.VMID
 */
public class GUID implements Serializable
{
	String m_id;
	
	/**
	 * Creates an id based on the given int. <br>
	 * GUID's created in this way will be distinct from GUID's 
	 * created with the default constructor. <br>
	 * GUID's created in this way will be equivalent across
	 * vm's.
	 */
    public GUID(int id) 
	{
		m_id = String.valueOf(id);
    }

	public GUID() 
	{
		m_id = new java.rmi.dgc.VMID().toString();
	}	
	
	public boolean equals(Object o)
	{
		if(o == null)
			return false;
		if(! (o instanceof GUID))
			return false;
		
		GUID other = (GUID) o;
		return other.m_id.equals(this.m_id);
	}
	
	public int hashCode()
	{
		return m_id.hashCode();
	}
	
	public String toString()
	{
		return "GUID:" + m_id;
	}
}
