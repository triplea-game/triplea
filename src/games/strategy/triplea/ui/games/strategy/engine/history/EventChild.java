/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package games.strategy.engine.history;

public class EventChild extends HistoryNode implements Renderable
{
    public final String m_text;
    public final Object m_renderingData;

    public EventChild(String text, Object renderingData)
    {
        super(text, true);
        m_text = text;
        m_renderingData = renderingData;
    }

    public Object getRenderingData()
    {
        return m_renderingData;
    }

    public String toString()
    {
        return m_text;
    }

    public SerializationWriter getWriter()
    {
       return new EventChildWriter(m_text, m_renderingData);
    }

}

class EventChildWriter implements SerializationWriter
{
    private final String m_text;
    private final Object m_renderingData;
    
    
    public EventChildWriter(final String text, final Object renderingData)
    {
        m_text = text;
        m_renderingData = renderingData;
    }


    public void write(HistoryWriter writer)
    {
        writer.addChildToEvent(new EventChild(m_text, m_renderingData));
    }
    
}
