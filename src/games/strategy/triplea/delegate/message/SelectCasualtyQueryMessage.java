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
 * SelectCasualtyMessage.java
 *
 * Created on November 19, 2001, 2:57 PM
 */

package games.strategy.triplea.delegate.message;

import java.util.*;

import games.strategy.util.*;
import games.strategy.engine.message.Message;
import games.strategy.engine.data.*;
import games.strategy.triplea.delegate.DiceRoll;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class SelectCasualtyQueryMessage extends BattleMessage
{
  private Collection m_selectFrom;
  private Map m_dependents;
  private int m_count;
  private String m_message;
  private DiceRoll m_dice;
  private PlayerID m_hit;
  private List m_defaultCasualties;

  /** Creates new SelectCasualtyMessage */
  public SelectCasualtyQueryMessage(String step, Collection selectFrom, Map dependents,  int count, String message, DiceRoll dice, PlayerID hit, List defaultCasualties)
  {
    super(step);
    m_selectFrom = new ArrayList(selectFrom);
    m_dependents = new HashMap(dependents);
    m_count = count;
    m_message = message;
    m_dice = dice;
    m_hit = hit;
    m_defaultCasualties = defaultCasualties;
  }

  /**
   * Total number of units that must be killed.
   */
  public int getCount()
  {
    return m_count;
  }

  public Collection getSelectFrom()
  {
    return m_selectFrom;
  }

  public Map getDependent()
  {
    return m_dependents;
  }

  public String getMessage()
  {
    return m_message;
  }

  public String toString()
  {
    return "SelectCasualtyQueryMessage units:" + m_selectFrom + " dependents:" + m_dependents + " count:" + m_count + " message:" + m_message;
  }

  public DiceRoll getDice()
  {
    return m_dice;
  }

  public PlayerID getPlayer()
  {
    return m_hit;
  }

  public List getDefaultCasualties() {
    return m_defaultCasualties;
  }
}
