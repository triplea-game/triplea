package games.strategy.net;

/**
 * Sent when a new node is added or removed.
 */

class NodeChangeServerMessage extends ServerMessage 
{

	private INode m_node;
	private boolean m_add;

	NodeChangeServerMessage(boolean add, INode node)
	{
		m_add = add;
		m_node = node;		
	}
	
	public boolean getAdd()
	{
		return m_add;
	}
	
	public INode getNode()
	{
		return m_node;
	}
}

