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
 * ChatMessage.java
 *
 * Created on January 14, 2002, 11:20 AM
 */

package games.strategy.engine.chat;

import java.io.Serializable;

/**
 * A chat message.
 *
 * @author  Sean Bridges
 */
class ChatMessage implements Serializable
{
	private static final long serialVersionUID = 2087299128023065916L;
	
	private String m_message;
	
	ChatMessage(String message)
	{
		m_message = message;
	}
	
	public String getMessage()
	{
		return m_message;
	}

}

