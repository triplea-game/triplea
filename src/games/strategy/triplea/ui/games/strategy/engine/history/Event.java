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


    public SerializationWriter getWriter()
    {
        return new EventHistorySerializer(m_description, m_renderingData);
    }
}

class EventHistorySerializer implements SerializationWriter
{
    private String m_eventName;
    private Object m_renderingData;
    
    public EventHistorySerializer(String eventName, Object renderingData)
    {
        m_eventName = eventName;
        m_renderingData = renderingData;
    }
    
    public void write(HistoryWriter writer)
    {
        writer.startEvent(m_eventName);
        if(m_renderingData != null)
            writer.setRenderingData(m_renderingData);   
    }
    
}
