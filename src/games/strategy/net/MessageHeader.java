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

package games.strategy.net;

import java.io.Serializable;

//written over the network very often, so make externalizable to 
//increase performance
public class MessageHeader
{
    //if null, then a broadcast
	private final INode m_for;
	private final Serializable m_message;
	private final INode m_from;
    
	/**
	 *  Creates a broadcast message.
	 */
	public MessageHeader(INode from, Serializable message)
	{
        this(null, from, message);
	}
	

	public MessageHeader(INode to, INode from, Serializable message)
	{
        //for can be null if we are a broadcast
		m_for = to;
        //from can be null if the sending node doesnt know its own address
		m_from = from;
		m_message = message;    
	}
	
    
	/**
	 * null if a broadcast
	 */
	public INode getFor()
	{
		return m_for;
	}
	
	public INode getFrom()
	{
		return m_from;
	}

	
	public boolean isBroadcast()
	{
		return m_for == null;
	}
	
	public Serializable getMessage()
	{
		return m_message;
	}
	
	public String toString()
	{
		return "Message header. msg:" + m_message + " to:" + m_for + " from:" + m_from;
	}

  
}

