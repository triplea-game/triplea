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
public interface IGameLoader extends java.io.Serializable
{
    
  public static final String CLIENT_PLAYER_TYPE = "Client";  
    
  /**
   * Return an array of player types that can play on the server.
   * This array must not contain any entries that could play on the client.
   *
   * It is assumed that all players can play on either server or client.
   */
  public String[] getServerPlayerTypes();

  /**
   * Create the players.  Given a map of playerName -> type,
   * where type is one of the Strings returned by a getServerPlayerTypes() or IGameLoader.CLIENT_PLAYER_TYPE.
   *
   * @return a Set of GamePlayers
   */
  public Set createPlayers(Map players);

  /**
   * The game is about to start.
   */
  public void startGame(IGame game, Set players);
  
  /**
   * Get the type of the display
   * @return an interface that extends IChannelSubscrobor
   */
  public Class getDisplayType();
  

}
