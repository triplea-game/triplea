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
 * These events are written by the delegate, and need to be serialized and sent
*  to all games.
 */
public class RemoteHistoryMessage implements java.io.Serializable
{
    private String m_event;
    private EventChild m_eventChild;
    private Object m_renderingData;

    public RemoteHistoryMessage(String event)
    {
        m_event = event;
    }

    public RemoteHistoryMessage(String text, Object renderingData)
    {
       m_eventChild = new EventChild(text, renderingData);
    }

    public RemoteHistoryMessage(Object renderingData)
    {
      m_renderingData = renderingData;
    }

    public void perform(HistoryWriter writer)
    {
        if(m_event != null)
            writer.startEvent(m_event);
        if(m_eventChild != null)
          writer.addChildToEvent(m_eventChild);
        if (m_renderingData != null)
          writer.setRenderingData(m_renderingData);
    }
}
