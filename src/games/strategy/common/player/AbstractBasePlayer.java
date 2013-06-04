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
package games.strategy.common.player;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.gamePlayer.IPlayerBridge;

/**
 * As a rule, nothing that changes GameData should be in here (it should be in a delegate, and done through an IDelegate using a change).
 * 
 * @author Lane Schwartz
 */
public abstract class AbstractBasePlayer implements IGamePlayer
{
	private final String m_name; // what nation are we playing? ex: "Americans"
	private final String m_type; // what are we? ex: "Human", or "Moore N. Able (AI)"
	private PlayerID m_playerID;
	private IPlayerBridge m_iPlayerBridge;
	
	/**
	 * @param name
	 *            - the name of the player.
	 */
	public AbstractBasePlayer(final String name, final String type)
	{
		m_name = name;
		m_type = type;
	}
	
	/**
	 * Anything that overrides this MUST call super.initialize(iPlayerBridge, playerID);
	 */
	public void initialize(final IPlayerBridge iPlayerBridge, final PlayerID playerID)
	{
		m_iPlayerBridge = iPlayerBridge;
		m_playerID = playerID;
	}
	
	/**
	 * Get the GameData for the game.
	 */
	protected final GameData getGameData()
	{
		return m_iPlayerBridge.getGameData();
	}
	
	/**
	 * Get the IPlayerBridge for this game player.
	 * (This is not a delegate bridge, and we can not send changes on this. Changes should only be done within a delegate, never through a player.)
	 */
	protected final IPlayerBridge getPlayerBridge()
	{
		return m_iPlayerBridge;
	}
	
	public final String getName()
	{
		return m_name;
	}
	
	public final String getType()
	{
		return m_type;
	}
	
	public final PlayerID getPlayerID()
	{
		return m_playerID;
	}
	
	@Override
	public String toString()
	{
		return (m_playerID == null || m_playerID.getName() == null || !m_playerID.getName().equals(m_name)) ?
					(m_type + ":" + m_name + ":" + (m_playerID == null ? "NullID" : m_playerID.getName())) : (m_type + ":" + m_name);
	}
	
	/**
	 * The given phase has started. We parse the phase name and call the appropriate method.
	 */
	public void start(final String stepName)
	{
		if (stepName != null)
		{
			// PlayerBridge is on a different thread than this one, and so it will be updated asynchronously. Need to wait for it.
			String bridgeStep = getPlayerBridge().getStepName();
			int i = 0;
			boolean shownErrorMessage = false;
			while (!stepName.equals(bridgeStep))
			{
				try
				{
					Thread.sleep(100);
					i++;
					if (i > 30 && !shownErrorMessage)
					{
						System.out.println("Start step: " + stepName + " does not match player bridge step: " + bridgeStep + ". Player Bridge GameOver=" + getPlayerBridge().isGameOver()
									+ ", PlayerID: " + getPlayerID().getName() + ", Game: " + getGameData().getGameName() + ". Something wrong or very laggy. Will keep trying for 30 more seconds. ");
						shownErrorMessage = true;
					}
					// TODO: what is the right amount of time to wait before we give up?
					if (i > 310)
					{
						System.err.println("Start step: " + stepName + " still does not match player bridge step: " + bridgeStep
									+ " even after waiting more than 30 seconds. This will probably result in a ClassCastException very soon. Player Bridge GameOver="
									+ getPlayerBridge().isGameOver() + ", PlayerID: " + getPlayerID().getName() + ", Game: " + getGameData().getGameName());
						// getPlayerBridge().printErrorStatus();
						break; // waited more than 30 seconds, so just let stuff run (an error will pop up surely...)
					}
				} catch (final InterruptedException e)
				{
				}
				bridgeStep = getPlayerBridge().getStepName();
			}
		}
	}
	
	// public abstract Class<?> getRemotePlayerType();
}
