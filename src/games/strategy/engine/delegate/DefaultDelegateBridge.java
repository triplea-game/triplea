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
 * DefaultDelegateBridge.java
 *
 * Created on October 27, 2001, 6:58 PM
 */

package games.strategy.engine.delegate;

import java.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.gamePlayer.*;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.message.*;
import games.strategy.engine.transcript.Transcript;

/**
 *
 * Default implementation of DelegateBridge
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class DefaultDelegateBridge implements DelegateBridge 
{
  private static Random s_random;
	
  private final GameData m_data;
  private final GameStep m_step;
  private PlayerID m_player;
  private IGame m_game;
	
  /** Creates new DefaultDelegateBridge */
  public DefaultDelegateBridge(GameData data, GameStep step, IGame game) 
  {
    m_data = data;
    m_step = step;
    m_game = game;
    m_player = m_step.getPlayerID();
  }
	
  public PlayerID getPlayerID() 
  {
    return m_player;
  }
	
  /** 
   * Knowing the seed gives a player an advantage.
   * Do something a little more clever than current time.
   * which could potentially be guessed
   */
  private long getSeed()
  {
    //if the execution path is different before the first random 
    //call is made then the object will have a somewhat random 
    //adress in the virtual machine, especially if 
    //a lot of ui and networking objects are created
    //in response to semi random mouse motion etc.
    //if the excecution is always the same then 
    //this may vary slightly depending on the vm
    Object seedObj = new Object();
    long seed = seedObj.hashCode(); //hash code is an int, 32 bits
    //seed with current time as well
    seed += System.currentTimeMillis();
    return seed;
  }
	
  /**
   * Delegates should not use random data that comes from any other source.
   */
  public synchronized int getRandom(int max) 
  {	
    if(s_random == null)
      s_random = new Random(getSeed());
    return s_random.nextInt(max);
  }

  /**
   * Delegates should not use random data that comes from any other source.
   */	
  public synchronized int[] getRandom(int max, int count) 
  {
    int[] numbers = new int[count];
    for(int i = 0; i < count; i++)
    {
      numbers[i] = getRandom(max);
    }
    return numbers;
  }
	

  /**
   * Both getRandom and getRandomArray use this method to prepare both 
   * players to generate an integer or an array
   */
  private void startRandomGen(int max, PlayerID player1, PlayerID player2)
  {
    Message msg = new RandomNumberMessage(RandomNumberMessage.SEND_TRIPLET, 
                                          new Integer(max));

    // Send maximum value, request triplet
    msg = sendMessage(msg, (player1.getName() + "RandomDest"));

    // send triplet, request triplet
    ((RandomNumberMessage)msg).m_request = RandomNumberMessage.SEND_TRIPLET;
    msg = sendMessage(msg, (player2.getName() + "RandomDest"));

    // send triplet, request key
    ((RandomNumberMessage)msg).m_request = RandomNumberMessage.SEND_KEY;
    msg = sendMessage(msg, (player1.getName() + "RandomDest"));

    // send key, request key
    ((RandomNumberMessage)msg).m_request = RandomNumberMessage.SEND_KEY;
    msg = sendMessage(msg, (player2.getName() + "RandomDest"));

    // send key, request nothing
    ((RandomNumberMessage)msg).m_request = RandomNumberMessage.NO_REQUEST;
    sendMessage(msg, (player1.getName() + "RandomDest"));
  }

  /**
   * All delegates should use random data that comes from both players 
   * so that neither player cheats.
   */
  public int getRandom(int max, PlayerID player1, PlayerID player2)
  {
    // Start the seeding operation and get the key
    startRandomGen(max, player1, player2);

    Message msg = new RandomNumberMessage(RandomNumberMessage.SEND_RANDOM, null);

    msg = sendMessage(msg, player2.getName() + "RandomDest");

    return ((Integer)((RandomNumberMessage)msg).m_obj).intValue();
  }

  /**
   * Delegates should not use random data that comes from any other source.
   */	
  public int[] getRandomArray(int max, int count, PlayerID player1, PlayerID player2)
  {
    // Start the seeding operation and get the key
    startRandomGen(max, player1, player2);

    Message msg = new RandomNumberMessage(RandomNumberMessage.SEND_RANDOM, 
                                          new Integer(count));

    msg = sendMessage(msg, player2.getName() + "RandomDest");

    return (int[])((RandomNumberMessage)msg).m_obj;
  }


  public void addChange(Change aChange) 
  {
    m_game.addChange(aChange);
  }
	
  /**
   * Messages are sent to the current player
   */
  public Message sendMessage(Message message) 
  {
    return m_game.getMessageManager().send(message, m_player.getName());
  }
	
  public void setPlayerID(PlayerID aPlayer) 
  {
    m_player = aPlayer;
  }
	
  /**
   * Sends a message to the given player.  
   */
  public Message sendMessage(Message message, PlayerID player) 
  {
    return m_game.getMessageManager().send(message, player.getName());
  }

  /**
   * Sends a message to the given player.  
   */
  public Message sendMessage(Message message, String dest_name) 
  {
    return m_game.getMessageManager().send(message, dest_name);
  }

  /**
   * Returns the current step name
   */
  public String getStepName() 
  {
    return m_step.getName();
  }	
	
  public Transcript getTranscript()
  {
    return m_game.getTranscript();
  }
}
