package games.strategy.net;

import java.io.Serializable;

class MessageHeader implements Serializable 
{
	private INode m_for;
	private Serializable m_message;
	private INode m_from;
	
	/**
	 *  Creates a broadcast message.
	 */
	public MessageHeader(INode from, Serializable message)
	{
		m_for = null;
		m_from = from;
		m_message = message;
	}
	
	public MessageHeader(INode to, INode from, Serializable message)
	{
		m_for = to;
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

