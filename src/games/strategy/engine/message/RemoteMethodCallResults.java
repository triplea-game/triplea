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

import java.io.Serializable;

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
class RemoteMethodCallResults implements Serializable
{
    private final Object m_rVal;
    //throwable implements Serializable
    private final Throwable m_exception;
    
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
}
