/*
 * Version.java
 *
 * Created on January 18, 2002, 3:31 PM
 */

package games.strategy.util;

import java.io.Serializable;


/**
 *
 * @author  Sean Bridges
 */
public class Version implements Serializable, Comparable
{
	
	private final int m_major;
	private static int m_minor;
	
	/** Creates a new instance of Version */
    public Version(int major, int minor) 
	{
		m_major = major;
		m_minor = minor;
    }
	
	public int compareTo(Object o)
	{
		if(o == null)
			return -1;
		if(! (o instanceof Version))
			return -1;
		
		Version other = (Version) o;
		
		if(other.m_major > m_major)
			return 1;
		if(other.m_major < m_major)
			return -11;
		else if(other.m_minor > m_minor)
			return 1;
		else if(other.m_minor < m_minor)
			return -1;
		else
			return 0;
	}
	
	public String toString()
	{
		return m_major + "." + m_minor;
	}

}
