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
/*
 * CompositeChange.java
 * 
 * Created on January 3, 2002, 10:32 PM
 */
package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.List;

/**
 * A Change made of several changes.
 * 
 * @author Sean Bridges
 */
public class CompositeChange extends Change
{
	static final long serialVersionUID = 8152962976769419486L;
	private final List<Change> m_changes;
	
	public CompositeChange(final Change... changes)
	{
		this();
		add(changes);
	}
	
	public CompositeChange()
	{
		m_changes = new ArrayList<Change>();
	}
	
	public CompositeChange(final List<Change> changes)
	{
		m_changes = new ArrayList<Change>(changes);
	}
	
	public void add(final Change... changes)
	{
		for (final Change aChange : changes)
		{
			if (!aChange.isEmpty())
				m_changes.add(aChange);
		}
	}
	
	@Override
	public Change invert()
	{
		final List<Change> newChanges = new ArrayList<Change>();
		// to invert a list of changes, process the opposite of
		// each change in the reverse order of the original list
		for (int i = m_changes.size() - 1; i >= 0; i--)
		{
			final Change current = m_changes.get(i);
			newChanges.add(current.invert());
		}
		return new CompositeChange(newChanges);
	}
	
	@Override
	protected void perform(final GameData data)
	{
		for (int i = 0; i < m_changes.size(); i++)
		{
			final Change current = m_changes.get(i);
			current.perform(data);
		}
	}
	
	/**
	 * 
	 * @return true if this change is empty, or composed of empty changes
	 */
	@Override
	public boolean isEmpty()
	{
		for (final Change c : m_changes)
		{
			if (!c.isEmpty())
				return false;
		}
		return true;
	}
	
	public List<Change> getChanges()
	{
		return new ArrayList<Change>(m_changes);
	}
	
	@Override
	public String toString()
	{
		return "CompositeChange <" + m_changes.toString() + ">";
	}
}
