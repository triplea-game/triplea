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
 * GUID.java
 *
 * Created on January 4, 2002, 2:33 PM
 */

package games.strategy.net;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

/**
 *
 * A globally unique id.  <br>
 * Backed by a java.rmi.dgc.VMID.
 * 
 * @author  Sean Bridges
 * @see java.rmi.dgc.VMID
 */
public class GUID implements Externalizable
{
    //this prefix is unique across vms
    //intern it to reduce memory consumption
    //by getting a reference to the interned string
    //when we deserialize
    private static final String vm_prefix = new java.rmi.dgc.VMID().toString().intern();
    
    //the local identifier
    private static int s_lastID = 0;
    
	private int m_id;
	private String m_prefix;
	
	/**
	 * Creates an id based on the given int. <br>
	 * GUID's created in this way will be distinct from GUID's 
	 * created with the default constructor. <br>
	 * GUID's created in this way will be equivalent across
	 * vm's.
	 */
	public GUID(int id) 
	{
		m_id = id;
		m_prefix = "autoGen";
	}

	public GUID() 
	{
		synchronized(GUID.class)
		{
		    m_id = s_lastID++;
		}
		m_prefix = vm_prefix;
		
	}	
	
	public boolean equals(Object o)
	{
		if(o == null)
			return false;
		if(! (o instanceof GUID))
			return false;
		
		GUID other = (GUID) o;
		return other.m_prefix.equals(this.m_prefix) && this.m_id == other.m_id;
	}
	
	public int hashCode()
	{
		return ((int) m_id) ^  m_prefix.hashCode();
	}
	
	public String toString()
	{
		return "GUID:" + m_prefix + ":" + m_id;
	}

	
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        m_id = in.readInt();
        m_prefix = ((String) in.readObject() ).intern();
    }

    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(m_id);
        out.writeObject(m_prefix);
        
    }
	
	
}
