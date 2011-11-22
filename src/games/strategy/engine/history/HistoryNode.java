package games.strategy.engine.history;

import java.io.Serializable;

import javax.swing.tree.DefaultMutableTreeNode;

public abstract class HistoryNode extends DefaultMutableTreeNode implements Serializable
{
	public HistoryNode(final String title, final boolean allowsChildren)
	{
		super(title, allowsChildren);
	}
	
	public String getTitle()
	{
		return (String) super.getUserObject();
	}
	
	public abstract SerializationWriter getWriter();
}
