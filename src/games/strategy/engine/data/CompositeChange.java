/*
 * CompositeChange.java
 *
 * Created on January 3, 2002, 10:32 PM
 */

package games.strategy.engine.data;

import java.util.*;

/**
 *
 * @author  Sean Bridges
 */
public class CompositeChange extends Change
{

	static final long serialVersionUID = 8152962976769419486L;

	private final List m_changes;

	CompositeChange()
	{
		m_changes = new ArrayList();
	}
	
	CompositeChange(List changes)
	{
		m_changes = new ArrayList(changes);
	}
	
	public void add(Change aChange)
	{
		m_changes.add(aChange);
	}

	public Change invert() 
	{
		List newChanges = new ArrayList();
		//to invert a list of changes, process the opposite of 
		//each change in the reverse order of the original list
		for(int i = m_changes.size() - 1; i >= 0; i--)
		{
			Change current = (Change) m_changes.get(i);
			newChanges.add(current.invert());
		}
		return new CompositeChange(newChanges);
	}

	protected void perform(GameData data) 
	{
		for(int i = 0; i < m_changes.size(); i++)
		{
			Change current = (Change) m_changes.get(i);
			current.perform(data);
		}
	}
}
