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

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;
import games.strategy.triplea.delegate.*;
import games.strategy.engine.data.*;

public class Event extends IndexedHistoryNode implements Renderable
{
    private final String m_description;
    //additional data used for rendering this event
    private Object m_renderingData;

    public String getDescription()
    {
        return m_description;
    }

    Event(String description, int changeStartIndex)
    {
        super(description, changeStartIndex, true);
        m_description = description;

    }

    public Object getRenderingData()
    {
      return m_renderingData;
    }

    public void setRenderingData(Object data)
    {
      m_renderingData = data;
    }
}
