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
 * ChangeMessage.java
 *
 * Created on January 1, 2002, 1:29 PM
 */

package games.strategy.engine.framework;

import java.io.Serializable;
import games.strategy.engine.data.Change;
import games.strategy.net.OrderedMessage;

/**
 *
 * @author  Sean Bridges
 */
class ChangeMessage implements Serializable, OrderedMessage
{
	private Change m_change;
	
	/** Creates a new instance of ChangeMessage */
    ChangeMessage(Change aChange) 
	{
		m_change = aChange;
    }
	
	public Change getChange()
	{
		return m_change;
	}
	
	public String toString()
	{
		return "Change message:" + m_change;
	}
}
