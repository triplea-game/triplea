/*
 * TestGameLoader.java
 *
 * Created on January 29, 2002, 12:38 PM
 */

package games.strategy.engine.xml;

import java.util.*;

import games.strategy.engine.framework.IGameLoader;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.message.IChannelSubscribor;

/**
 *
 * @author  Sean Bridges
 */
public class TestGameLoader implements IGameLoader
{
	/**
	 * Return an array of player types that can play on the server.
	 * This array must not contain any entries that could play on the client.
	 */
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
	public void startGame(IGame game, Set players)
	{}
	
	/**
	 * Create the players.  Given a map of playerName -> type,
	 * where type is one of the Strings returned by a get*PlayerType() method.
	 */
	public Set createPlayers(Map players)
	{
		return null;
	}

    /* 
     * @see games.strategy.engine.framework.IGameLoader#getDisplayType()
     */
    public Class getDisplayType()
    {
        return IChannelSubscribor.class;
    }	
	
	
}