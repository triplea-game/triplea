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

import javax.swing.*;
import java.awt.event.*;

/**
 * These events are written by the delegate, and need to be serialized and sent
 *  to all games.
 */
public class RemoteHistoryMessage implements java.io.Serializable
{
  //this is a little curious
  //the final variables referenced by the anonymous
  //inner class are serialized when the object is sent over the network
  private Action m_action;

  //this is set before the action is performed
  //its only reason for existing is so I didnt have to create a new interface
  //to pass it to the actions.
  private transient HistoryWriter m_historyWriter;

  public RemoteHistoryMessage(final String event)
  {
    m_action = new AbstractAction()
    {
      public void actionPerformed(ActionEvent e)
      {
        m_historyWriter.startEvent(event);
      }
    };
  }

  public RemoteHistoryMessage(final String text, final Object renderingData)
  {
    m_action = new AbstractAction()
    {
      public void actionPerformed(ActionEvent e)
      {
        m_historyWriter.addChildToEvent(new EventChild(text, renderingData));
      }
    };

  }

  public RemoteHistoryMessage(final Object renderingData)
  {
    m_action = new AbstractAction()
    {
      public void actionPerformed(ActionEvent e)
      {
        m_historyWriter.setRenderingData(renderingData);
      }
    };

  }

  public void perform(HistoryWriter writer)
  {
    m_historyWriter = writer;
    m_action.actionPerformed(null);
    m_historyWriter = null;
  }
}
