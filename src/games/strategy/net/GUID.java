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

import java.io.*;
import java.rmi.dgc.VMID;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * A globally unique id.  <br>
 * Backed by a java.rmi.dgc.VMID.
 * 
 * Written across the network often, so this class is 
 * externalizable to increase effeciency
 * 
 * @author  Sean Bridges
 * @see java.rmi.dgc.VMID
 */
public class GUID implements Externalizable
{
    //this prefix is unique across vms
    private static final VMID vm_prefix = new java.rmi.dgc.VMID();
    
    //the local identifier
    //this coupled with the unique vm prefix comprise
    //our unique id
    private static AtomicInteger s_lastID = new AtomicInteger();
    
	private int m_id;
	private VMID m_prefix;
	
	public GUID() 
	{
		m_id = s_lastID.getAndIncrement();
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
		return  m_id ^  m_prefix.hashCode();
	}
	
	public String toString()
	{
		return "GUID:" + m_prefix + ":" + m_id;
	}
	
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
    {
        m_id = in.readInt();
        m_prefix = (VMID) in.readObject() ;
    }

    public void writeExternal(ObjectOutput out) throws IOException
    {
        out.writeInt(m_id);
        out.writeObject(m_prefix);
    }
    
    public static void main(String[] args)
    {
        System.out.println(new GUID().toString());
    }
}
