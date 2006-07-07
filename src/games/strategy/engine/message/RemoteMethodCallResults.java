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
package games.strategy.engine.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * 
 *
 *
 * @author Sean Bridges
 */
/**
 * The results of a method execution.
 * 
 * Note that either one of m_rVal or m_exception will be null,
 * since the method can either throw or return
 * 
 */
class RemoteMethodCallResults implements Externalizable
{
    private Object m_rVal;
    //throwable implements Serializable
    private Throwable m_exception;
    
    public RemoteMethodCallResults()
    {
    	
    }
    
    public RemoteMethodCallResults(final Object rVal)
    {
        m_rVal = rVal;
        m_exception = null;
    }

    public RemoteMethodCallResults(Throwable exception)
    {
        m_rVal = null;
        m_exception = exception;
    }

    public Throwable getException()
    {
        return m_exception;
    }
    public Object getRVal()
    {
        return m_rVal;
    }

	public void writeExternal(ObjectOutput out) throws IOException 
	{        
        if(m_rVal != null)
        {
            out.writeBoolean(true);
            out.writeObject(m_rVal);
        }
        else
        {
            out.writeBoolean(false);
            out.writeObject(m_exception);    
        }
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException 
	{
        boolean rVal = in.readBoolean();
        if(rVal)
        {
            m_rVal = in.readObject();
        }
        else
        {
            m_exception = (Throwable) in.readObject();
        }
	}
}
