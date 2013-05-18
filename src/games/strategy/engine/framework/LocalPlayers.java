package games.strategy.engine.framework;

import games.strategy.common.player.AbstractHumanPlayer;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.gamePlayer.IGamePlayer;

import java.util.Collections;
import java.util.Set;

public class LocalPlayers
{
	protected final Set<IGamePlayer> m_localPlayers;
	
	public LocalPlayers(final Set<IGamePlayer> localPlayers)
	{
		m_localPlayers = localPlayers;
	}
	
	public Set<IGamePlayer> getLocalPlayers()
	{
		return Collections.unmodifiableSet(m_localPlayers);
	}
	
	public boolean playing(final PlayerID id)
	{
		if (id == null)
			return false;
		for (final IGamePlayer gamePlayer : m_localPlayers)
		{
			if (gamePlayer.getPlayerID().equals(id) && AbstractHumanPlayer.class.isAssignableFrom(gamePlayer.getClass()))
			{
				return true;
			}
		}
		return false;
	}
}
