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
package games.strategy.engine.vault;

import games.strategy.net.INode;

import java.io.Serializable;

/**
 * @author Sean Bridges
 */
public class VaultID implements Serializable
{
    
    private static long s_currentID;
   
    private synchronized static long getNextID()
    {
        return s_currentID++;
    }
    
    private final INode m_generatedOn;
    //this is a unique and monotone increasing id
    //unique in this vm
    private final long m_uniqueID = getNextID();
    
    VaultID(final INode generatedOn)
    {
        super();
        m_generatedOn = generatedOn;
    }
    
    /**
     * @return Returns the generatedOn.
     */
    INode getGeneratedOn()
    {
        return m_generatedOn;
    }
    /**
     * @return Returns the id.
     */
    long getUniqueID()
    {
        return m_uniqueID;
    }
    
    public boolean equals(Object o)
    {
        if(o == null || !(o instanceof VaultID))
            return false;
        VaultID other = (VaultID) o;
        
        return other.m_generatedOn.equals(this.m_generatedOn) &&
               other.m_uniqueID == this.m_uniqueID;
    }
    
    public int hashCode()
    {
        return ((int) m_uniqueID ) ^ m_generatedOn.getName().hashCode();
    }
    
    public String toString()
    {
        return "VaultID generated on:" + m_generatedOn + " id:" + m_uniqueID;
    }
}
