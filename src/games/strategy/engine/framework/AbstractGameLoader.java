package games.strategy.engine.framework;

import games.strategy.sound.ISound;

/**
 * 
 * @author veqryn
 * 
 */
abstract public class AbstractGameLoader implements IGameLoader
{
	private static final long serialVersionUID = 5698425046660960332L;
	protected transient ISound m_soundChannel;
	protected transient IGame m_game;
	
	/**
	 * Return an array of player types that can play on the server.
	 */
	abstract public String[] getServerPlayerTypes();
	
	public void shutDown()
	{
		if (m_soundChannel != null)
		{
			m_game.removeSoundChannel(m_soundChannel);
			m_soundChannel.shutDown();
			m_soundChannel = null;
		}
	}
}
