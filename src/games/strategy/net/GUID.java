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
	private String m_id;
	
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
