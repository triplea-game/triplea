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
  final private PlayerID m_remotePlayer;
  final private PlayerID m_localPlayer;
  final private IMessageManager  m_messageManager;


  public CryptoRandomSource(PlayerID dicePlayer1, PlayerID dicePlayer2, IMessageManager MessageManager)
  {
    m_remotePlayer = dicePlayer1;
    m_localPlayer = dicePlayer2;
    m_messageManager = MessageManager;
  }

  /**
    * All delegates should use random data that comes from both players
    * so that neither player cheats.
    */
   public int getRandom(int max, String annotation)
   {
       return getRandom(max, 1, annotation)[0];
   }

   /**
    * Delegates should not use random data that comes from any other source.
    */
   public int[] getRandom(int max, int count, String annotation)
   {
     // Start the seeding operation and get the key
     startRandomGen(max, count, annotation);

     Message msg = new RandomNumberMessage(RandomNumberMessage.SEND_RANDOM,
                                           new Integer(count));
     msg =  m_messageManager.send(msg, RandomDestination.getRandomDestination(m_localPlayer.getName()));

     return (int[])((RandomNumberMessage)msg).m_obj;
   }

   /**
    * Both getRandom and getRandomArray use this method to prepare both
    * players to generate an integer or an array
    */
   private void startRandomGen(int max, int randomCount, String annotation)
   {
     Message msg = new RandomNumberMessage(RandomNumberMessage.SEND_TRIPLET,
                                           new Integer(max));
     

     ((RandomNumberMessage) msg).m_randomCount = randomCount;
     ((RandomNumberMessage) msg).m_annotation = annotation;     
     
     // Send maximum value, request triplet
     msg =  m_messageManager.send(msg, (m_remotePlayer.getName() + "RandomDest"));

     // send triplet, request triplet
     ((RandomNumberMessage)msg).m_request = RandomNumberMessage.SEND_TRIPLET;
     ((RandomNumberMessage) msg).m_randomCount = randomCount;
     ((RandomNumberMessage) msg).m_annotation = annotation;     

     msg =  m_messageManager.send(msg, (m_localPlayer.getName() + "RandomDest"));

     // send triplet, request key
     ((RandomNumberMessage)msg).m_request = RandomNumberMessage.SEND_KEY;
     msg =  m_messageManager.send(msg, (m_remotePlayer.getName() + "RandomDest"));

     // send key, request key
     ((RandomNumberMessage)msg).m_request = RandomNumberMessage.SEND_KEY;
     msg =  m_messageManager.send(msg, (m_localPlayer.getName() + "RandomDest"));

     // send key, request nothing
     ((RandomNumberMessage)msg).m_request = RandomNumberMessage.NO_REQUEST;
      m_messageManager.send(msg, (m_remotePlayer.getName() + "RandomDest"));
   }


}
