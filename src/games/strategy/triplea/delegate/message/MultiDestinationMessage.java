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


package games.strategy.triplea.delegate.message;

/**
 * A message that should only be recieved once per physical vm.
 * Some messages are broadcast to all players, but if two players are playing in the same window,
 * then it doesnt make sense to display the message twice.
 *
 * In this case the tripleaPlayer will swallow messages if it sees.
 *
 * Note that responses shouldnt be expected for messages of this type.
 */

public class MultiDestinationMessage
{
  private static int s_lastGeneratedID;
  private static int s_lastSeenID = 0;

  private static synchronized int getNextID()
  {
    s_lastGeneratedID++;
    return s_lastGeneratedID;
  }

  public static synchronized boolean shouldIgnore(MultiDestinationMessage message)
  {
    //not terribly safe, but efficient
    //assumes messages are generated and seen in order
    //from a single source
    //if its not working for you then update the algorithm
    boolean rVal = message.getID() < s_lastSeenID;
    s_lastSeenID = message.getID();
    return rVal;
  }

  private int m_id;

  public MultiDestinationMessage()
  {
    m_id = getNextID();
  }

  private int getID()
  {
    return m_id;
  }
}