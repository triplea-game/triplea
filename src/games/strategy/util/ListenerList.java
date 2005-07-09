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
 * ListenerList.java
 *
 * Created on November 6, 2001, 8:37 PM
 */

package games.strategy.util;

import java.util.*;

/**
 *
 *
 * Thread safe list of listeners. <p>
 * Calls to iterator return an iterator that will iterate over the list <br>
 * at the time that iterator was called.  Elements can be added or removed <br>
 * to the list while iterating without a concurentModificationException being <br>
 * thrown or the elements being iterator over changing.
 * 
 * @author  Sean Bridges
 * @version 1.0
 */
public class ListenerList <T>
{
	private Collection<T> m_listeners = new LinkedList<T>();
	private Collection<T> m_listenersCached;
	
	public synchronized void add(T o)
	{
		m_listeners.add(o);
		m_listenersCached = null;		
	}
	
	public synchronized void remove(T o)
	{
		m_listeners.remove(o);
		m_listenersCached = null;
	}
	
	public synchronized Iterator<T> iterator()
	{
		if(m_listenersCached == null)
		{
			m_listenersCached = new ArrayList<T>(m_listeners);
		}
		return m_listenersCached.iterator();
	}

	/**
	 * @return a new list with the same elemnts as the current list.
	 */
	public synchronized List<T> toList()
	{
		return new ArrayList<T>(m_listeners);
	}
	
	public synchronized String toString()
	{
		return m_listeners.toString();
	}
	

}	
