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
	private ArrayList m_History;
	private ArrayList m_CurrentHistory;
	private int m_HistoryPosition;
	ChatHistory()
	{
		m_History=new ArrayList();
		m_CurrentHistory=new ArrayList();
		m_CurrentHistory.add("");
		m_HistoryPosition=0;
	}
	boolean hasNextHistory()
	{
		if(m_HistoryPosition<m_CurrentHistory.size()-1)
			return true;
		return false;
	}
	boolean hasPrevHistory()
	{
		if(m_HistoryPosition>0)
			return true;
		return false;
	}
	void setHistory(String s)
	{
		m_CurrentHistory.set(m_HistoryPosition,s);
	}
	void nextHistory()
	{
		m_HistoryPosition++;
	}
	void prevHistory()
	{
		m_HistoryPosition--;
	}
	String getHistory()
	{
		return m_CurrentHistory.get(m_HistoryPosition).toString();
	}
	void insertHistory(String s){
		m_History.add(s);
		m_HistoryPosition=m_History.size();
		m_CurrentHistory=new ArrayList(m_History);
		m_CurrentHistory.add("");
	}
}
