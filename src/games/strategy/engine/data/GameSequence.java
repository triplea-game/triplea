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

/*
 * GameSequence.java
 *
 * Created on October 14, 2001, 7:25 AM
 */

package games.strategy.engine.data;


import java.util.*;
/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class GameSequence extends GameDataComponent
{
	private final List m_steps = new ArrayList();
	private int m_currentIndex = -1;
	
	public GameSequence(GameData data) 
	{
		super(data);
    }

	protected void addStep(GameStep step)
	{
		m_steps.add(step);
	}
	
	/**
	 * Removes the first instance of step.
	 */
	protected void remove(GameStep step)
	{
		if(!m_steps.contains(step))
			throw new IllegalArgumentException("Step does not exist");
		
		m_steps.remove(step);
	}
	
	protected void removeStep(int index)
	{
		m_steps.remove(index);
	}
	/*
	public int size()
	{
		return m_steps.size();
	}
	*/
	
	public void next() {
		m_currentIndex++;
		if (m_currentIndex == m_steps.size())
			m_currentIndex = 0;	
	}
	
	public GameStep getStep() 
	{
		return getStep(m_currentIndex);	
	}
	
	public GameStep getStep(int index)
	{
		if ((index < 0) || (index >= m_steps.size()))
			throw new IllegalArgumentException("Attempt to access invalid state: " + index);
			
		return (GameStep) m_steps.get(index);
	}
	
	public Iterator iterator()
	{
		return m_steps.iterator();
	}
	
}
