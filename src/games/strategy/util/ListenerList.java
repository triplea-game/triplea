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
public class ListenerList
{
	private Collection m_listeners = new LinkedList();
	private Collection m_listenersCached;
	
	public synchronized void add(Object o)
	{
		m_listeners.add(o);
		m_listenersCached = null;		
	}
	
	public synchronized void remove(Object o)
	{
		m_listeners.remove(o);
		m_listenersCached = null;
	}
	
	public synchronized Iterator iterator()
	{
		if(m_listenersCached == null)
		{
			m_listenersCached = new ArrayList(m_listeners);
		}
		return m_listenersCached.iterator();
	}

	/**
	 * @return a new list with the same elemnts as the current list.
	 */
	public synchronized List toList()
	{
		return new ArrayList(m_listeners);
	}
	
	public synchronized String toString()
	{
		return m_listeners.toString();
	}
	

}	
