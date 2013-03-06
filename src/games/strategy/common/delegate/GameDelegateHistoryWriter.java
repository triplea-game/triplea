/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy.common.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.history.IDelegateHistoryWriter;

/**
 * Has a subset of the historyWriters functionality.
 * Delegates should only have access to these functions.
 * The rest of the history writers functions should only
 * be used by the GameData
 * 
 * 
 */
public class GameDelegateHistoryWriter implements IDelegateHistoryWriter
{
	IDelegateHistoryWriter m_delegateHistoryWriter;
	GameData m_data;
	
	public GameDelegateHistoryWriter(final IDelegateHistoryWriter delegateHistoryWriter, final GameData data)
	{
		m_delegateHistoryWriter = delegateHistoryWriter;
		m_data = data;
	}
	
	public String getEventPrefix()
	{
		if (BaseEditDelegate.getEditMode(m_data))
			return "EDIT: ";
		return "";
	}
	
	public void startEvent(final String eventName, final Object renderingData)
	{
		if (eventName.startsWith("COMMENT: "))
			m_delegateHistoryWriter.startEvent(eventName, renderingData);
		else
			m_delegateHistoryWriter.startEvent(getEventPrefix() + eventName, renderingData);
	}
	
	public void startEvent(final String eventName)
	{
		if (eventName.startsWith("COMMENT: "))
			m_delegateHistoryWriter.startEvent(eventName);
		else
			m_delegateHistoryWriter.startEvent(getEventPrefix() + eventName);
	}
	
	public void addChildToEvent(final String child)
	{
		if (child.startsWith("COMMENT: "))
			m_delegateHistoryWriter.addChildToEvent(child, null);
		else
			m_delegateHistoryWriter.addChildToEvent(getEventPrefix() + child, null);
	}
	
	public void addChildToEvent(final String child, final Object renderingData)
	{
		if (child.startsWith("COMMENT: "))
			m_delegateHistoryWriter.addChildToEvent(child, renderingData);
		else
			m_delegateHistoryWriter.addChildToEvent(getEventPrefix() + child, renderingData);
	}
	
	/* Set the rendering data for the current event.
	public void setRenderingData(final Object renderingData)
	{
		m_delegateHistoryWriter.setRenderingData(renderingData);
	}*/
}
