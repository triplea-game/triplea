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
 * SelectCasualtyQueryMessage.java
 *
 * Created on November 19, 2001, 2:59 PM
 */

package games.strategy.triplea.delegate.message;

import games.strategy.engine.message.Message;

import java.util.List;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class CasualtyDetails implements Message
{
  private final List m_killed;
  private final  List m_damaged;
  private final boolean m_autoCalculated;

  /** Creates new SelectCasualtyMessage */
  public CasualtyDetails(List killed, List damaged, boolean autoCalculated)
  {
    if(killed == null)
      throw new IllegalArgumentException("null killed");
    if(damaged == null)
      throw new IllegalArgumentException("null damaged");

    m_killed = killed;
    m_damaged = damaged;
    m_autoCalculated = autoCalculated;
  }

  /**
   * A mapping of UnitType -> count,
   */
  public List getKilled()
  {
    return m_killed;
  }

  public List getDamaged()
  {
    return m_damaged;
  }

  public boolean getAutoCalculated() {
    return m_autoCalculated;
  }

  public String toString()
  {
    return "SelectCasualtyMessage killed:" + m_killed + " damaged:" + m_damaged;
  }
}
