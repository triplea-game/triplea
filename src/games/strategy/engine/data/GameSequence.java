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
	
	public int size()
	{
		return m_steps.size();
	}
	
	public GameStep getStep(int index)
	{
		return (GameStep) m_steps.get(index);
	}
	
	public Iterator iterator()
	{
		return m_steps.iterator();
	}
	
}
