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

package games.strategy.engine.random;

import games.strategy.engine.message.*;
import games.strategy.engine.framework.*;
import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;

/**
 *  A random source that generates numbers using a secure algorithm shared between two players.
*
*  Code originally contributed by Ben Giddings.
 */
public class CryptoRandomSource implements IRandomSource
{

  //the two players who involved in rolling the dice
  //dice are rolled securly between these two
  final private PlayerID m_diceRollerPlayer1;
  final private PlayerID m_diceRollerPlayer2;
  final private DefaultDelegateBridge m_bridge;


  public CryptoRandomSource(PlayerID dicePlayer1, PlayerID dicePlayer2, DefaultDelegateBridge delegateBridge)
  {
    m_diceRollerPlayer1 = dicePlayer1;
    m_diceRollerPlayer2 = dicePlayer2;
    m_bridge =  delegateBridge;
  }

  /**
    * All delegates should use random data that comes from both players
    * so that neither player cheats.
    */
   public int getRandom(int max, String annotation)
   {
     // Start the seeding operation and get the key
     startRandomGen(max);

     Message msg = new RandomNumberMessage(RandomNumberMessage.SEND_RANDOM, null);

     msg = m_bridge.sendMessage(msg, m_diceRollerPlayer2.getName() + "RandomDest");

     return ((Integer)((RandomNumberMessage)msg).m_obj).intValue();
   }

   /**
    * Delegates should not use random data that comes from any other source.
    */
   public int[] getRandom(int max, int count, String annotation)
   {
     // Start the seeding operation and get the key
     startRandomGen(max);

     Message msg = new RandomNumberMessage(RandomNumberMessage.SEND_RANDOM,
                                           new Integer(count));

     msg = m_bridge.sendMessage(msg, RandomDestination.getRandomDestination(m_diceRollerPlayer2.getName()));

     return (int[])((RandomNumberMessage)msg).m_obj;
   }

   /**
    * Both getRandom and getRandomArray use this method to prepare both
    * players to generate an integer or an array
    */
   private void startRandomGen(int max)
   {
     Message msg = new RandomNumberMessage(RandomNumberMessage.SEND_TRIPLET,
                                           new Integer(max));

     // Send maximum value, request triplet
     msg = m_bridge.sendMessage(msg, (m_diceRollerPlayer1.getName() + "RandomDest"));

     // send triplet, request triplet
     ((RandomNumberMessage)msg).m_request = RandomNumberMessage.SEND_TRIPLET;
     msg = m_bridge.sendMessage(msg, (m_diceRollerPlayer2.getName() + "RandomDest"));

     // send triplet, request key
     ((RandomNumberMessage)msg).m_request = RandomNumberMessage.SEND_KEY;
     msg = m_bridge.sendMessage(msg, (m_diceRollerPlayer1.getName() + "RandomDest"));

     // send key, request key
     ((RandomNumberMessage)msg).m_request = RandomNumberMessage.SEND_KEY;
     msg = m_bridge.sendMessage(msg, (m_diceRollerPlayer2.getName() + "RandomDest"));

     // send key, request nothing
     ((RandomNumberMessage)msg).m_request = RandomNumberMessage.NO_REQUEST;
     m_bridge.sendMessage(msg, (m_diceRollerPlayer1.getName() + "RandomDest"));
   }


}
