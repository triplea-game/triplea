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
 * BattleInfoMessage.java
 *
 * Created on February 4, 2002, 3:40 PM
 */

package games.strategy.triplea.delegate.message;

import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.delegate.DiceRoll;

/**
 * Sent to inform the player of an event that occured during the battle.
 *
 *
 * @author  Sean Bridges
 */
public class BattleInfoMessage extends BattleMessage
{

  /**
   * Dont send a notification to this player.
   * Allows the ui to ignore notifications if more than
   * one player is sharing the same ui.
   */
  private final Object m_message;
  private final String m_shortMessage;


  
  /**
   * Creates a new instance of BattleInfoMessage
   */
  public BattleInfoMessage(Object message, String shortMessage, String step)
  {
    super(step);
    
    if( ! (message instanceof String || message instanceof DiceRoll))
        throw new IllegalArgumentException("Message of wrong type");
    
    m_message = message;
    m_shortMessage = shortMessage;
    
  }

  public Object getMessage()
  {
    return m_message;
  }

  public String getShortMessage()
  {
    return m_shortMessage;
  }
  
 

}
