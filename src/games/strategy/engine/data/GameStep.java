/*
 * GameStep.java
 *
 * Created on October 14, 2001, 7:28 AM
 */

package games.strategy.engine.data;

import games.strategy.engine.delegate.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class GameStep extends GameDataComponent
{
	private final String m_name;
	private final PlayerID m_player;
	private final Delegate m_delegate;
	int m_hashCode = -1;
	
	/** Creates new GameStep */
    public GameStep(String name, PlayerID player, Delegate delegate, GameData data) 
	{
		super(data);
		m_name = name;
		m_player = player;
		m_delegate = delegate;
    }
	
	public String getName()
	{
		return m_name;
	}
	
	public PlayerID getPlayerID()
	{
		return m_player;
	}
	
	public Delegate getDelegate()
	{
		return m_delegate;
	}
	
	public boolean equals(Object o)
	{
		if(o == null || ! (o instanceof GameStep))
			return false;
		
		GameStep other = (GameStep) o;
		
		return other.m_name.equals(this.m_name) &&
		       other.m_delegate.equals(this.m_delegate) &&
			   other.m_player.equals(this.m_player);
	}

	public int hashCode()
	{
		if(m_hashCode == -1)
		{
			String s = m_name + m_delegate + m_player;
			m_hashCode = s.hashCode();
		}
		
		return m_hashCode;
	}
}
