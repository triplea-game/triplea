/*
 * TestGameLoader.java
 * 
 * Created on January 29, 2002, 12:38 PM
 */

package games.strategy.engine.xml;

import games.strategy.engine.data.DefaultUnitFactory;
import games.strategy.engine.data.IUnitFactory;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.engine.message.IChannelSubscribor;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.pbem.IPBEMMessenger;

import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Sean Bridges
 */
public class TestGameLoader implements IGameLoader
{
	/**
	 * Return an array of player types that can play on the server.
	 * This array must not contain any entries that could play on the client.
	 */
	@Override
	public String[] getServerPlayerTypes()
	{
		return null;
	}
	
	/**
	 * Return an array of player types that can play on the client.
	 * This array must not contain any entries that could play on the server.
	 */
	public String[] getClientPlayerTypes()
	{
		return null;
	}
	
	/**
	 * The game is about to start.
	 */
	@Override
	public void startGame(IGame game, Set<IGamePlayer> players)
	{
	}
	
	/**
	 * Get PBEM messengers for Turn Summary notification
	 */
	@Override
	public IPBEMMessenger[] getPBEMMessengers()
	{
		return new IPBEMMessenger[0];
	}
	
	/**
	 * Create the players. Given a map of playerName -> type,
	 * where type is one of the Strings returned by a get*PlayerType() method.
	 */
	@Override
	public Set<IGamePlayer> createPlayers(Map players)
	{
		return null;
	}
	
	/* 
	 * @see games.strategy.engine.framework.IGameLoader#getDisplayType()
	 */
	@Override
	public Class<? extends IChannelSubscribor> getDisplayType()
	{
		return IChannelSubscribor.class;
	}
	
	/* (non-Javadoc)
	 * @see games.strategy.engine.framework.IGameLoader#getRemotePlayerType()
	 */
	@Override
	public Class<? extends IRemote> getRemotePlayerType()
	{
		return IRemote.class;
	}
	
	@Override
	public void shutDown()
	{
	}
	
	@Override
	public IUnitFactory getUnitFactory()
	{
		return new DefaultUnitFactory();
	}
	
}
