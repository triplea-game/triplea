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
 * UnitAttatchment.java
 *
 * Created on November 8, 2001, 1:35 PM
 */

package games.strategy.triplea.attatchments;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.DefaultAttatchment;
import games.strategy.engine.data.UnitType;

import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.*;
import games.strategy.engine.data.GameParseException;


/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class UnitAttatchment extends DefaultAttatchment
{
  /**
   * Conveniente method.
   */
  public static UnitAttatchment get(UnitType type)
  {
    return (UnitAttatchment) type.getAttatchment(Constants.UNIT_ATTATCHMENT_NAME);
  }

  private boolean m_isAir = false;
  private boolean m_isSea = false;
  private boolean m_isAA = false;
  private boolean m_isFactory = false;
  private boolean m_canBlitz = false;
  private boolean m_isSub = false;
  private boolean m_canBombard = false;
  private boolean m_isStrategicBomber = false;

  //-1 if cant transport
  private int m_transportCapacity = -1;
  //-1 if cant be transported
  private int m_transportCost = -1;

  //-1 if cant act as a carrier
  private int m_carrierCapacity = -1;
  //-1 if cant land on a carrier
  private int m_carrierCost = -1;


  private int m_movement = 0;
  private int m_attack = 0;
  private int m_defense = 0;

  /** Creates new UnitAttatchment */
    public UnitAttatchment()
  {}

  private TechTracker getTechTracker()
  {
    return DelegateFinder.techDelegate(getData()).getTechTracker();
  }


  public void setCanBlitz(String s)
  {
    m_canBlitz = getBool(s);
  }

  public boolean getCanBlitz()
  {
    return m_canBlitz;
  }

  public void setIsSub(String s)
  {
    m_isSub = getBool(s);
  }

  public boolean isSub()
  {
    return m_isSub;
  }

  public boolean isStrategicBomber()
  {
    return m_isStrategicBomber;
  }

  public void setIsStrategicBomber(String s)
  {
    m_isStrategicBomber = getBool(s);
  }


  public void setCanBombard(String s)
  {
    m_canBombard = getBool(s);
  }

  public boolean getCanBombard()
  {
    return m_canBombard;
  }

  public void setIsAir(String s)
  {
    m_isAir = getBool(s);
  }

  public boolean isAir()
  {
    return m_isAir;
  }

  public void setIsSea(String s)
  {
    m_isSea = getBool(s);
  }

  public boolean isSea()
  {
    return m_isSea;
  }

  public void setIsAA(String s)
  {
    m_isAA = getBool(s);
  }

  public boolean isAA()
  {
    return m_isAA;
  }

  public void setIsFactory(String s)
  {
    m_isFactory = getBool(s);
  }

  public boolean isFactory()
  {
    return m_isFactory;
  }

  public void setTransportCapacity(String s)
  {
    m_transportCapacity = getInt(s);
  }

  public int getTransportCapacity()
  {
    return m_transportCapacity;
  }

  public void setTransportCost(String s)
  {
    m_transportCost = getInt(s);
  }

  public int getTransportCost()
  {
    return m_transportCost;
  }

  public void setCarrierCapacity(String s)
  {
    m_carrierCapacity = getInt(s);
  }

  public int getCarrierCapacity()
  {
    return m_carrierCapacity;
  }

  public void setCarrierCost(String s)
  {
    m_carrierCost = getInt(s);
  }

  public int getCarrierCost()
  {
    return m_carrierCost;
  }

  public void setMovement(String s)
  {
    m_movement = getInt(s);
  }

  public int getMovement(PlayerID player)
  {
    if(m_isAir)
    {
      TechTracker tracker = getTechTracker();
      if(tracker.hasLongRangeAir(player))
        return m_movement + 2;
    }
    return m_movement;
  }

  public void setAttack(String s)
  {
    m_attack = getInt(s);
  }

  public int getAttack(PlayerID player)
  {
    if(m_isSub)
    {
      TechTracker tracker = getTechTracker();
      if(tracker.hasSuperSubs(player))
        return m_attack + 1;
    }

    return m_attack;
  }

  public void setDefense(String s)
  {
    m_defense = getInt(s);
  }

  public int getDefense(PlayerID player)
  {
    if(m_isAir && !m_isStrategicBomber)
    {
      TechTracker tracker = getTechTracker();
      if(tracker.hasJetFighter(player))
        return m_defense + 1;
    }
    return m_defense;
  }


  public int getAttackRolls(PlayerID player)
  {
    if(getAttack(player) == 0)
      return 0;

    if(m_isStrategicBomber)
    {
      TechTracker tracker = getTechTracker();
      if(tracker.hasHeavyBomber(player))
        return 3;
    }
    return 1;
  }

  public void validate() throws GameParseException
  {
    if(m_isAir)
    {
      if(m_isSea ||
        m_isFactory ||
        m_isSub ||
        m_isAA ||
        m_transportCost != -1 ||
        m_transportCapacity != -1 ||
        m_carrierCapacity != -1 ||
        m_canBlitz ||
        m_canBombard
        )
        throw new GameParseException("Invalid Unit attatchment");

    }
    else if(m_isSea)
    {
      if(	m_canBlitz ||
        m_isAA ||
        m_isAir ||
        m_isFactory ||
        m_isStrategicBomber ||
        m_carrierCost != -1 ||
        m_transportCost != -1
        )
        throw new GameParseException("Invalid Unit Attatchemnnt" + this);
    }
    else //if land
    {
      if(m_canBombard ||
        m_isStrategicBomber ||
        m_isSub ||
        m_carrierCapacity != -1 ||
        m_transportCapacity != -1
        )
        throw new GameParseException("Invalid Unit Attatchemnnt" + this);
    }

    if(m_carrierCapacity != -1 && m_carrierCost != -1)
    {
      throw new GameParseException("Invalid Unit Attatchemnnt" + this);
    }

    if(m_transportCost != -1 && m_transportCapacity != -1)
    {
      throw new GameParseException("Invalid Unit Attatchemnnt" + this);
    }


  }

  public String toString()
  {
    return
    " blitz:" + m_canBlitz +
    " bombard:" +m_canBombard +
    " aa:" +m_isAA +
    " air:" +m_isAir +
    " factory:" +m_isFactory +
    " sea:" +m_isSea +
    " strategicBomber:" +m_isStrategicBomber +
    " sub:" +m_isSub +
    " attack:" +m_attack +
    " carrierCapactity:" +m_carrierCapacity +
    " carrierCost:" +m_carrierCost +
    " defense:" +m_defense +
    " movement:" +m_movement +
    " transportCapacity:" +m_transportCapacity +
    " transportCost:" +m_transportCost;
  }
}