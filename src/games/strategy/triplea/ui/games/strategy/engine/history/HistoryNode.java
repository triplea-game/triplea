package games.strategy.engine.history;

import javax.swing.tree.*;
import java.io.Serializable;


public abstract class HistoryNode extends DefaultMutableTreeNode implements Serializable
{
    public HistoryNode(String title, boolean allowsChildren)
    {
        super(title, allowsChildren);
    }

    public String getTitle()
    {
      return (String) super.getUserObject();
    }
    
    public abstract SerializationWriter getWriter();
}
