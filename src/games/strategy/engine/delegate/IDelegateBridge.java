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

/*
 * DelegateBridge.java
 *
 * Created on October 13, 2001, 4:35 PM
 */

package games.strategy.engine.delegate;

import java.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.message.Message;
import games.strategy.engine.history.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 *
 * A class that communicates with the Delegate.
 * DelegateBridge co-ordinates comunication between the Delegate and both the players
 * and the game data.
 *
 * The reason for communicating through a DelegateBridge is to achieve network
 * transparancy.
 *
 * The delegateBridge allows the Delegate to talk to the player in a safe manner.
 */
public interface IDelegateBridge
{
  /**
   * Messages are sent to the current player
   */
  public Message sendMessage(Message message);

  /**
   * Messages are sent to the current player without waiting for a response.
   */
  public void sendMessageNoResponse(Message message);


  /**
   * Sends a message to the given player.
   */
  public Message sendMessage(Message message, PlayerID player);

  /**
   * Messages are sent to the given player without waiting for a response.
   */
  public void sendMessageNoResponse(Message message, PlayerID player);


  /**
   * Player is initialized to the player specified in the xml data.
   */
  public void setPlayerID(PlayerID aPlayer);
  public PlayerID getPlayerID();

  /**
   * Returns the current step name
   */
  public String getStepName();

  public void addChange(Change aChange);

  /**
   * Delegates should not use random data that comes from any other source.
   */
  public int getRandom(int max, String annotation);
  public int[] getRandom(int max, int count, String annotation);


  /**
   *
   */
    public DelegateHistoryWriter getHistoryWriter();
  }
