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

import java.util.*;

import games.strategy.engine.data.*;
/**
 * Sent by the engine to the player when a battle is to start.
 *
 */

public class BattleStartMessage extends MultiDestinationMessage implements games.strategy.engine.message.Message
{
  private PlayerID m_defender;
  private PlayerID m_attacker;
  private Territory m_territory;
  private Collection m_attackingUnits;
  private Collection m_defendingUnits;
  //maps Unit-> Collection of units
  private Map m_dependents;

  public BattleStartMessage(PlayerID attacker, PlayerID defender, Territory territory, Collection attackingUnits, Collection defendingUnits, Map dependents)
  {
    m_defender = defender;
    m_attacker = attacker;
    m_territory = territory;
    m_attackingUnits = attackingUnits;
    m_defendingUnits = defendingUnits;
    m_dependents = dependents;
  }

  public PlayerID getDefender()
  {
    return m_defender;
  }

  public PlayerID getAttacker()
  {
    return m_attacker;
  }

  public Territory getTerritory()
  {
    return m_territory;
  }

  public Collection getAttackingUnits()
  {
    return m_attackingUnits;
  }

  public Collection getDefendingUnits()
  {
    return m_defendingUnits;
  }

  public Map getDependents()
  {
    return m_dependents;
  }


}