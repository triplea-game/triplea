package games.strategy.engine.history;

import java.io.Serializable;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Known subclasses of HistoryNode:<br>
 * -> RootHistoryNode<br>
 * -> EventChild<br>
 * -> IndexedHistoryNode<br>
 * -> -> Step<br>
 * -> -> Event<br>
 * -> -> Round
 * 
 */
public abstract class HistoryNode extends DefaultMutableTreeNode implements Serializable
{
	private static final long serialVersionUID = 628623470654123887L;
	
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
