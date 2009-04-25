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

package games.strategy.engine.history;

/**
* A history of the game.
*
* Stored as a tree, the data is organized as
* Root
*  -  Round 
*   -   Step 
*       -  Event
*         - Child 
*           
*
*
*
* Round - the current round in the game, eg 1, 2, 3
* Step - the current step, eg Britian Combat Move
* Event - an event that happened in the game, eg Russia buys 8 inf.
*
*
 */

import games.strategy.engine.data.*;

import java.io.*;
import java.util.*;

import javax.swing.SwingUtilities;
import javax.swing.tree.*;

public class History extends DefaultTreeModel implements java.io.Serializable
{
    private final HistoryWriter m_writer = new HistoryWriter(this);
    private final List<Change> m_changes = new ArrayList<Change>();
    private final GameData m_data;

    private HistoryNode m_currentNode;

    private void assertCorrectThread()
    {
        if(m_data.areChangesOnlyInSwingEventThread() && !SwingUtilities.isEventDispatchThread())
            throw new IllegalStateException("Wrong thread");
    }
    
    public History(GameData data)
    {
        super(new RootHistoryNode("Game History", true));
        m_data = data;
    }

    public HistoryWriter getHistoryWriter()
    {
        return m_writer;
    }

    public HistoryNode getLastNode()
    {
        assertCorrectThread();
        return getLastChildInternal( (HistoryNode) getRoot());
    }

    private HistoryNode getLastChildInternal(HistoryNode node)
    {
        if(node.getChildCount() == 0)
            return node;
        return getLastChildInternal((HistoryNode) node.getLastChild());
    }

    private int getLastChange(HistoryNode node)
    {
      int rVal;
      if (node == getRoot())
        rVal = 0;
      else if (node instanceof Event)
        rVal = ( (Event) node).getChangeEndIndex();
      else if (node instanceof EventChild)

        rVal = ( (Event) node.getParent()).getChangeEndIndex();
      else
        rVal = ( (IndexedHistoryNode) node).getChangeStartIndex();

      if (rVal == -1)
        return m_changes.size();
      return rVal;
    }

    public Change getDelta(HistoryNode start, HistoryNode end)
    {
        assertCorrectThread();
        int firstChange = getLastChange(start);
        int lastChange = getLastChange(end);

        if(firstChange == lastChange)
          return null;

       List<Change> changes = m_changes.subList( Math.min(firstChange, lastChange), Math.max(firstChange, lastChange  ) );

       Change compositeChange = new CompositeChange(changes);
       if(lastChange >= firstChange )
       {
           return compositeChange;
       }
       else
       {
           return compositeChange.invert();
       }
    }

    public synchronized void gotoNode(HistoryNode node)
    {
        assertCorrectThread();
        getGameData().acquireWriteLock();
        try
        {
        
          if(m_currentNode == null)
            m_currentNode = getLastNode();
    
          Change dataChange = getDelta(m_currentNode, node);
          m_currentNode = node;
    
          if (dataChange != null)
            new ChangePerformer(m_data).perform(dataChange);
        }
        finally
        {
            getGameData().releaseWriteLock();
        }          
    }

    synchronized void changeAdded(Change aChange)
    {
        getGameData().acquireWriteLock();
        try
        {
        
              m_changes.add(aChange);
        
              if(m_currentNode == null)
                return;
              if(m_currentNode == getLastNode())
                new ChangePerformer(m_data).perform(aChange);
              
        }
        finally
        {
            getGameData().releaseWriteLock();
        }
      
    }

    private Object writeReplace() throws ObjectStreamException
    {
        return new SerializedHistory(this, m_data, m_changes);
    }
    
    List<Change> getChanges()
    {
        return m_changes;
    }
    
    GameData getGameData()
    {
        return m_data;
    }
}

/**
 * DefaultTreeModel is not serializable across jdk versions
 * Instead we use an instance of this class to store our data
 * 
 */
class SerializedHistory implements Serializable
{
    private final List<SerializationWriter> m_Writers = new ArrayList<SerializationWriter>();
    private GameData m_data;
    
    
    
    public SerializedHistory(History history, GameData data, List<Change> changes)
    {
        m_data = data;
        int changeIndex = 0;
        Enumeration enumeration = ((DefaultMutableTreeNode) history.getRoot()).preorderEnumeration();
        enumeration.nextElement();
        while(enumeration.hasMoreElements())
        {
            HistoryNode node = (HistoryNode) enumeration.nextElement();
            
            //write the changes to the start of the node
            if(node instanceof IndexedHistoryNode)
            {
                while(changeIndex < ((IndexedHistoryNode) node).getChangeStartIndex())
                {
                 m_Writers.add(new ChangeSerializationWriter(changes.get(changeIndex)));
                 changeIndex++;
                }
            }

            //write the node itself
            m_Writers.add(node.getWriter()); 
        }

        //write out remaining changes
        while(changeIndex < changes.size())
        {
         m_Writers.add(new ChangeSerializationWriter(changes.get(changeIndex)));
         changeIndex++;
        }
        
    }
    
    public Object readResolve() throws ObjectStreamException
    {
        History rVal = new History(m_data);
        HistoryWriter historyWriter = rVal.getHistoryWriter();
        Iterator<SerializationWriter> iter = m_Writers.iterator();
        
        while (iter.hasNext())
        {
            SerializationWriter element = iter.next();
            element.write(historyWriter);
        }
        
        return rVal;
    }

}

class RootHistoryNode extends HistoryNode
{
    public RootHistoryNode(String title, boolean allowsChildren)
    {
        super(title, allowsChildren);
    }

    public SerializationWriter getWriter()
    {
        throw new IllegalStateException("Not implemented");
    }
    
    
}

interface SerializationWriter extends Serializable
{
    public void write(HistoryWriter writer);
}

class ChangeSerializationWriter implements SerializationWriter
{
    private Change aChange;
    
    public ChangeSerializationWriter(Change change)
    {
        aChange = change;
    }
    
    public void write(HistoryWriter writer)
    {
        writer.addChange(aChange);
    }
}

