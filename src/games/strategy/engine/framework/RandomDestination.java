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
 * GamePlayer.java
 *
 * Created on October 27, 2001, 5:15 PM
 */

package games.strategy.engine.framework;

import java.util.*;

import javax.crypto.SecretKey;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.message.*;
import games.strategy.util.RandomGen;
import games.strategy.util.RandomTriplet;

/**
 *
 * A destination for random messages.
 *
 * @author  Ben Giddings
 * @version 1.0
 *
 */
public class RandomDestination implements IDestination
{
  public static String getRandomDestination(String playerName)
  {
    return playerName + "RandomDest";
  }

  private RandomGen m_random_gen;
  private RandomGen m_remote_random_gen;
  private String m_name;

  public RandomDestination(String playerName)
  {
    m_name = getRandomDestination(playerName);
  }

  public String getName()
  {
    return m_name;
  }

  public Message sendMessage(Message message)
  {
    // FIXME
    //System.out.println("TripleAPlayer#handleRandomNumberMessage(" + message + ")");
    RandomNumberMessage rnd_message;

    if (message instanceof RandomNumberMessage)
    {
      rnd_message = (RandomNumberMessage)message;
    }
    else
    {
      throw new RuntimeException("Not a RandomNumberMessage, instead" + message);
    }

    switch (rnd_message.m_request)
    {
    case RandomNumberMessage.SEND_TRIPLET:
      if (rnd_message.m_obj instanceof Integer)
      {
        // We're 'player1' and we're starting things off
        m_random_gen = new RandomGen(((Integer)rnd_message.m_obj).intValue());
      }
      else if (rnd_message.m_obj instanceof RandomTriplet)
      {
        // We're 'player2' and we're receiving the initial data from 'player1'
        m_random_gen = new RandomGen(((RandomTriplet)rnd_message.m_obj).m_max_num);
        m_remote_random_gen = new RandomGen();
        m_remote_random_gen.setTriplet((RandomTriplet)rnd_message.m_obj);
      }
      return new RandomNumberMessage(m_random_gen.getTriplet());

    case RandomNumberMessage.SEND_KEY:
      if (rnd_message.m_obj instanceof RandomTriplet)
      {
        // We're 'player1' and we're receiving the initial data from 'player2'
        m_remote_random_gen = new RandomGen();
        m_remote_random_gen.setTriplet((RandomTriplet)rnd_message.m_obj);
      }
      else if (rnd_message.m_obj instanceof SecretKey)
      {
        // We're 'player2' and we're receiving the key from 'player1'
        m_remote_random_gen.setKey((SecretKey)rnd_message.m_obj);
      }
      return new RandomNumberMessage(m_random_gen.getKey());

    case RandomNumberMessage.SEND_RANDOM:
      if (rnd_message.m_obj instanceof SecretKey)
      {
        // We're 'player1' and we're receiving the secret key from 'player2'
        m_remote_random_gen.setKey((SecretKey)rnd_message.m_obj);

        int the_random = m_random_gen.getSharedRandom(m_remote_random_gen);
        return new RandomNumberMessage(new Integer(the_random));
      }
      else if (rnd_message.m_obj instanceof Integer)
      {
        // We're one of the two players and all of the data has already
        // been exchanged, simply return the random array of the size requested
        int[] random_arr = m_random_gen.getSharedRandomArr(m_remote_random_gen,
                                                           ((Integer)rnd_message.m_obj).intValue());

        return new RandomNumberMessage(random_arr);
      }
      else
      {
        // We're one of the two players and all of the data has already
        // been exchanged, so simply return the random
        int the_random = m_random_gen.getSharedRandom(m_remote_random_gen);
        return new RandomNumberMessage(new Integer(the_random));
      }

      // At this point both sides should have all the data they need to
      // generate random numbers.  This step can happen either before or after
      // the NO_REQUEST step.


    case RandomNumberMessage.NO_REQUEST:
      if (rnd_message.m_obj instanceof SecretKey)
      {
        // We're 'player1' and we're receiving the secret key from 'player2'
        m_remote_random_gen.setKey((SecretKey)rnd_message.m_obj);
      }
      return null;

    default:
      return new ErrorMessage("Bad RandomNumberMessage received");
    }
  }
}
