/*
 * Node.java
 *
 * Created on December 11, 2001, 8:13 PM
 */

package games.strategy.net;

import games.strategy.net.GUID;
import java.net.InetAddress;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class Node implements INode
{	
	private String m_name;
	private int m_port;
	private InetAddress m_address;
	private GUID m_id = new GUID();

	/** Creates new Node */
    public Node(String name, InetAddress address, int port) 
	{
		m_name = name;
		m_address = address;
		m_port = port;
    }

	public String getName() 
	{
		return m_name;
	}	
	
	public boolean equals(Object obj)
	{
		if(obj == null)
			return false;
		if(! (obj instanceof Node))
			return false;
		
		Node other = (Node) obj;
		
		boolean sameID = this.m_id.equals(other.m_id);
		if(sameID && !this.m_name.equals(other.m_name))
			throw new IllegalStateException("Same ids but different names.  This:" + this + " other:" + other);
		
		return sameID;
	}
	
	public int hashCode()
	{
		return m_id.hashCode();
	}
	
	public String toString()
	{
		return m_name + " port:" + m_port + " ip:" + m_address.getHostAddress();
	}
	
	public int getPort()
	{
		return m_port;
	}
	
	public InetAddress getAddress()
	{
		return m_address;
	}
	
}