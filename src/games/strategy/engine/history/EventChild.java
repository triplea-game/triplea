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
	private static final long serialVersionUID = 2436212909638449323L;
	public final String m_text;
	public final Object m_renderingData;
	
	public EventChild(final String text, final Object renderingData)
	{
		super(text, true);
		m_text = text;
		m_renderingData = renderingData;
	}
	
	public Object getRenderingData()
	{
		return m_renderingData;
	}
	
	@Override
	public String toString()
	{
		return m_text;
	}
	
	@Override
	public SerializationWriter getWriter()
	{
		return new EventChildWriter(m_text, m_renderingData);
	}
}


class EventChildWriter implements SerializationWriter
{
	private static final long serialVersionUID = -7143658060171295697L;
	private final String m_text;
	private final Object m_renderingData;
	
	public EventChildWriter(final String text, final Object renderingData)
	{
		m_text = text;
		m_renderingData = renderingData;
	}
	
	public void write(final HistoryWriter writer)
	{
		writer.addChildToEvent(new EventChild(m_text, m_renderingData));
	}
}
