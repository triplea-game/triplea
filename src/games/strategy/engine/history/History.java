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
*  -  Round *
*   -   Step *
*       -  Event *
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

import java.util.*;
import javax.swing.tree.*;
import games.strategy.engine.data.*;

public class History  extends DefaultTreeModel implements java.io.Serializable
{
    private final HistoryWriter m_writer = new HistoryWriter(this);
    private final List m_changes = new ArrayList();
    private final GameData m_data;

    private HistoryNode m_currentNode;

    public History(GameData data)
    {
        super(new HistoryNode("Game History", true));
        m_data = data;
    }

    public HistoryWriter getHistoryWriter()
    {
        return m_writer;
    }

    public HistoryNode getLastNode()
    {
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
        int firstChange = getLastChange(start);
        int lastChange = getLastChange(end);

      if(firstChange == lastChange)
          return null;

       List changes = m_changes.subList( Math.min(firstChange, lastChange), Math.max(firstChange, lastChange  ) );

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
      if(m_currentNode == null)
        m_currentNode = getLastNode();

      Change dataChange = getDelta(m_currentNode, node);
      m_currentNode = node;

      if (dataChange != null)
        new ChangePerformer(m_data).perform(dataChange);
    }

    synchronized void changeAdded(Change aChange)
    {
      m_changes.add(aChange);

      if(m_currentNode == null)
        return;
      if(m_currentNode == getLastNode())
        new ChangePerformer(m_data).perform(aChange);
    }

    List  getChanges()
    {
      return m_changes;
    }
}
