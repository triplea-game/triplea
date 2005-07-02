/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */


/*
 * Created on May 20, 2004
 *
 */
package games.strategy.engine.chat;

import java.util.*;

/**
 * @author lnxduk
 *
 */
public class ChatHistory
{
	
	private final List<String> m_history =new ArrayList<String>();
	private int m_HistoryPosition;
	
	ChatHistory()
	{
	}
	
	public void next()
	{
		m_HistoryPosition = Math.min( m_HistoryPosition + 1, m_history.size());	
	}
	
	public void prev()
	{
	  	m_HistoryPosition = Math.max( m_HistoryPosition -1, 0);
	}
	
	public String current()
	{
	    if(m_HistoryPosition == m_history.size())
	        return "";
	    
		return m_history.get(m_HistoryPosition).toString();
	}
	
	public void append(String s)
	{
	    m_history.add(s);
		m_HistoryPosition=m_history.size();
	}
}
