/*
 * IGameLoader.java
 *
 * Created on January 29, 2002, 12:25 PM
 */

package games.strategy.engine.framework;

import java.util.*;

/**
 *
 * A game loader is responsible for telling the framework
 * what types of players are available, for creating players, and
 * starting the game.
 *
 * The name is somewhat misleading since it doesnt actually load the
 * game data, merely performs the game specific steps for starting the game.
 *
 * @author  Sean Bridges
 */
public interface IGameLoader extends  java.io.Serializable
{
	/**
	 * Return an array of player types that can play on the server.
	 * This array must not contain any entries that could play on the client.
     *
     * It is assumed that all players can play on either server or client.
	 */
	String[] getServerPlayerTypes();

	/**
	 * Return an array of player types that can play on the client.
	 * This array must not contain any entries that could play on the server.
     *
     * It is assumed that all players can play on either server or client.
	 */
	String[] getClientPlayerTypes();

	/**
	 * Create the players.  Given a map of playerName -> type,
	 * where type is one of the Strings returned by a get*PlayerType() method.
	 */
	Set createPlayers(Map players);

	/**
	 * The game is about to start.
	 */
	void startGame(IGame game, Set players);
}