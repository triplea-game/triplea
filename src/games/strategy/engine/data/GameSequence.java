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
 * GameSequence.java
 * 
 * Created on October 14, 2001, 7:25 AM
 */
package games.strategy.engine.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author Sean Bridges
 * @version 1.0
 */
public class GameSequence extends GameDataComponent implements Iterable<GameStep>
{
	private final List<GameStep> m_steps = new ArrayList<GameStep>();
	private int m_currentIndex;
	private int m_round = 1;
	private transient Object m_currentStepMutex = new Object();
	// compatible with 0.9.0.2 saved games
	private static final long serialVersionUID = 8205078386807440137L;
	
	public GameSequence(final GameData data)
	{
		super(data);
	}
	
	protected void addStep(final GameStep step)
	{
		m_steps.add(step);
	}
	
	/**
	 * Removes the first instance of step.
	 */
	protected void remove(final GameStep step)
	{
		if (!m_steps.contains(step))
			throw new IllegalArgumentException("Step does not exist");
		m_steps.remove(step);
	}
	
	protected void removeStep(final int index)
	{
		m_steps.remove(index);
	}
	
	protected void removeAllSteps()
	{
		m_steps.clear();
		m_round = 1;
	}
	
	public int getRound()
	{
		return m_round;
	}
	
	public int getStepIndex()
	{
		return m_currentIndex;
	}
	
	/**
	 * 
	 * @return boolean wether the round has changed
	 */
	public boolean next()
	{
		synchronized (m_currentStepMutex)
		{
			m_currentIndex++;
			if (m_currentIndex == m_steps.size())
			{
				m_currentIndex = 0;
				m_round++;
				return true;
			}
			return false;
		}
	}
	
	/**
	 * Only tests to see if we are on the last step.
	 * Used for finding if we need to make a new round or not.
	 * Does not change any data or fields.
	 * 
	 * @return
	 */
	public boolean testWeAreOnLastStep()
	{
		synchronized (m_currentStepMutex)
		{
			if (m_currentIndex + 1 == m_steps.size())
				return true;
			return false;
		}
	}
	
	public GameStep getStep()
	{
		synchronized (m_currentStepMutex)
		{
			return getStep(m_currentIndex);
		}
	}
	
	public GameStep getStep(final int index)
	{
		if ((index < 0) || (index >= m_steps.size()))
			throw new IllegalArgumentException("Attempt to access invalid state: " + index);
		return m_steps.get(index);
	}
	
	public Iterator<GameStep> iterator()
	{
		return m_steps.iterator();
	}
	
	/** make sure transient lock object is initialized on deserialization. */
	private void readObject(final java.io.ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		in.defaultReadObject();
		if (m_currentStepMutex == null)
		{
			m_currentStepMutex = new Object();
		}
	}
}
