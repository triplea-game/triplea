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

import games.strategy.engine.data.*;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.TechTracker;


/**
 *  Despite the mis leading name, this attatches not to individual Units but to UnitTypes.
 *  
 * @author  Sean Bridges
 * @version 1.0
 */
public class UnitAttachment extends DefaultAttachment
{
  /**
   * Conveniente method.
   */
  public static UnitAttachment get(UnitType type)
  {
    return (UnitAttachment) type.getAttachment(Constants.UNIT_ATTATCHMENT_NAME);
  }

  private boolean m_isAir = false;
  private boolean m_isSea = false;
  private boolean m_isAA = false;   
  private boolean m_isFactory = false;
  private boolean m_canBlitz = false;
  private boolean m_isAirTransport = false;
  private boolean m_isAirTransportable = false;
  private boolean m_isSub = false;
  private boolean m_canBombard = false;
  private boolean m_isStrategicBomber = false;
  private boolean m_isTwoHit = false;
  private boolean m_isDestroyer = false;
  private boolean m_isArtillery = false;
  private boolean m_isArtillerySupportable = false;
  private boolean m_isMarine = false;
  private boolean m_isInfantry = false;   
  
  //-1 if cant transport
  private int m_transportCapacity = -1;
  //-1 if cant be transported
  private int m_transportCost = -1;

  //-1 if cant act as a carrier
  private int m_carrierCapacity = -1;
  //-1 if cant land on a carrier
  private int m_carrierCost = -1;

  private int m_bombard = -1;
  private int m_unitSupportCount = -1;
  //TODO future use KEV
  // future private int m_artilleryBonus = -1;
  

  private int m_movement = 0;
  private int m_attack = 0;
  private int m_defense = 0;


  /** Creates new UnitAttatchment */
  public UnitAttachment()
  {
  }

  //does nothing, kept to avoid breaking maps
  public void setIsParatroop(String s) {}

  //does nothing, used to keep compatibility with older xml files
  public void setIsMechanized(String s) {}

  public void setIsAirTransport(String s)
  {
	  m_isAirTransport = getBool(s);
  }

  public boolean isAirTransport()
  {
    return m_isAirTransport;
  }

  public void setIsAirTransportable(String s)
  {
	  m_isAirTransportable = getBool(s);
  }

  public boolean isAirTransportable()
  {
    return m_isAirTransportable;
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

  
  public void setIsDestroyer(String s)
  {
    m_isDestroyer = getBool(s);
  }

  public boolean getIsDestroyer()
  {
    return m_isDestroyer;
  }


  public void setCanBombard(String s)
  {
    m_canBombard = getBool(s);
  }

  public boolean getCanBombard(PlayerID player)
  {
    if(m_canBombard)
        return true;
    if(m_isDestroyer && TechAttachment.get(player).hasDestroyerBombard())
      return true;
    
    return false;
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

  public boolean isInfantry()
  {
    return m_isInfantry;
  }

  public boolean isMarine()
  {
    return m_isMarine;
  }

  public void setIsFactory(String s)
  {
    m_isFactory = getBool(s);
  }

  public boolean isFactory()
  {
    return m_isFactory;
  }

  public void setIsMarine(String s)
  {
    m_isMarine = getBool(s);
  }

  public boolean getIsMarine()
  {
    return m_isMarine;
  }

  public void setIsInfantry(String s)
  {
    m_isInfantry = getBool(s);
  }
  
  public boolean getIsInfantry()
  {
    return m_isInfantry;
  }
  
  public void setTransportCapacity(String s)
  {
    m_transportCapacity = getInt(s);
  }

  public int getTransportCapacity()
  {
    return m_transportCapacity;
  }

  public void setIsTwoHit(String s)
  {
      m_isTwoHit = getBool(s);
  }

  public String getIsTwoHit()
  {
    return "" + m_isTwoHit;
  }

  public boolean isTwoHit()
  {
      return m_isTwoHit;
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

  public boolean isArtillery()
  {
    return m_isArtillery;
  }

  public void setArtillery(String s)
  {
    m_isArtillery = getBool(s);
  }
  
  public boolean isArtillerySupportable()
  {
    return m_isArtillerySupportable;
  }

  public void setArtillerySupportable(String s)
  {
    m_isArtillerySupportable = getBool(s);
  }
  
  //TODO future use KEV
  /*public int getArtilleryBonus()
  {
    return m_artilleryBonus >0 ? m_artilleryBonus : 1;
  }
  
  public void setArtilleryBonus(String s)
  {
	  m_artilleryBonus = getInt(s);
  }*/

  public void setunitSupportCount(String s)
  {
	  m_unitSupportCount = getInt(s);
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
      
      if(TechTracker.hasLongRangeAir(player))
        return m_movement + 2;
    }
    return m_movement;
  }

  public void setAttack(String s)
  {
    m_attack = getInt(s);
  }

  public void setBombard(String s)
  {
    m_bombard = getInt(s);
  }
  
  public int getAttack(PlayerID player)
  {
    if(m_isSub)
    {      
      if(TechTracker.hasSuperSubs(player))
        return m_attack + 1;
    }
    
    if(m_isAir && !m_isStrategicBomber)
    {      
      if(TechTracker.hasJetFighter(player) && isWW2V3TechModel(player.getData()))
          return m_attack + 1;
    }

    return m_attack;
  }


  public int getBombard(PlayerID player)
  {
	  return m_bombard > 0 ? m_bombard : m_attack;
  }

  public int getUnitSupportCount(PlayerID player)
  {
	  return m_unitSupportCount >0 ? m_unitSupportCount : 1;
  }
  
  int getRawAttack()
  {
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
        if(TechTracker.hasJetFighter(player) && !isWW2V3TechModel(player.getData()))
            return m_defense + 1;
    }
    if(m_isSub && TechTracker.hasSuperSubs(player))
    {

    	int bonus = games.strategy.triplea.Properties.getSuper_Sub_Defense_Bonus(player.getData());

    	if(bonus > 0)
            return m_defense + bonus;
    }
    
    return m_defense;
  }


  public int getAttackRolls(PlayerID player)
  {
    if(getAttack(player) == 0)
      return 0;

    if(m_isStrategicBomber && TechTracker.hasHeavyBomber(player))
    {        	
        return new Integer(games.strategy.triplea.Properties.getHeavy_Bomber_Dice_Rolls(getData()));
    }
    
    return 1;
  }

  public void validate(GameData data) throws GameParseException
  {
    if(m_isAir)
    {
      if(m_isSea ||
        m_isFactory ||
        m_isSub ||
        m_isAA ||
        m_transportCost != -1 ||
        //m_transportCapacity != -1 ||
        m_carrierCapacity != -1 ||
        m_canBlitz ||
        m_canBombard || 
        m_isMarine
        )
        throw new GameParseException("Invalid Unit attatchment" + this);

    }
    else if(m_isSea)
    {
      if(	m_canBlitz ||
        m_isAA ||
        m_isAir ||
        m_isFactory ||
        m_isStrategicBomber ||
        m_carrierCost != -1 ||
        m_transportCost != -1 ||
        m_isMarine
        )
        throw new GameParseException("Invalid Unit Attatchment" + this);
    }
    else //if land
    {
      if(m_canBombard ||
        m_isStrategicBomber ||
        m_isSub ||
        m_carrierCapacity != -1 ||
        m_bombard != -1 ||
        m_transportCapacity != -1
        )
        throw new GameParseException("Invalid Unit Attatchment" + this);
    }

    if(m_carrierCapacity != -1 && m_carrierCost != -1)
    {
      throw new GameParseException("Invalid Unit Attatchment" + this);
    }

    if(m_transportCost != -1 && m_transportCapacity != -1)
    {
      throw new GameParseException("Invalid Unit Attatchment" + this);
    }

    if(m_isSea && m_transportCapacity != -1 && Properties.getTransportCasualtiesRestricted(data) && (m_attack > 0 || m_defense > 0)) 
    {
    	throw new GameParseException("Restricted transports cannot have attack or defense");
    }

  }

  private boolean isWW2V3TechModel(GameData data)
  {
      return games.strategy.triplea.Properties.getWW2V3TechModel(data);
  }

  public String toString()
  {
    return
    " blitz:" + m_canBlitz +
    " canBombard:" +m_canBombard +
    " Bombard:" +m_bombard +
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
    " transportCost:" +m_transportCost+
    " destroyer:" + m_isDestroyer;
  }

}
