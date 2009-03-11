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

package games.strategy.triplea.attatchments;

import games.strategy.engine.data.*;
import games.strategy.triplea.*;

/**
 * @author Sean Bridges
 */

public class TechAttachment extends DefaultAttachment
{

  //attatches to a PlayerID

  public static TechAttachment get(PlayerID id)
  {
    TechAttachment attatchment = (TechAttachment) id.getAttachment(Constants.TECH_ATTATCHMENT_NAME);
    //dont crash
    if(attatchment == null)
        return new TechAttachment();
    return attatchment;
  }

  private boolean m_heavyBomber;
  private boolean m_longRangeAir;
  private boolean m_jetPower;
  private boolean m_rocket;
  private boolean m_industrialTechnology;
  private boolean m_superSub;
  private boolean m_destroyerBombard;
  private boolean m_improvedArtillerySupport;
  private boolean m_paratroopers;
  private boolean m_increasedFactoryProduction;
  private boolean m_warBonds;
  private boolean m_mechanizedInfantry;
  private boolean m_aARadar;
  private boolean m_shipyards;
  
  public void setHeavyBomber(String s)
  {
    m_heavyBomber = getBool(s);
  }

  public void setDestroyerBombard(String s)
  {
      m_destroyerBombard = getBool(s);
  }

  
  public void setLongRangeAir(String s)
  {
    m_longRangeAir = getBool(s);
  }

  public void setJetPower(String s)
  {
    m_jetPower = getBool(s);
  }

  public void setRocket(String s)
  {
    m_rocket = getBool(s);
  }

  public void setIndustrialTechnology(String s)
  {
    m_industrialTechnology = getBool(s);
  }

  public void setSuperSub(String s)
  {
    m_superSub = getBool(s);
  }

  public void setImprovedArtillerySupport(String s)
  {
    m_improvedArtillerySupport = getBool(s);
  }

  public void setParatroopers(String s)
  {
    m_paratroopers = getBool(s);
  }

  public void setIncreasedFactoryProduction(String s)
  {
    m_increasedFactoryProduction = getBool(s);
  }

  public void setWarBonds(String s)
  {
    m_warBonds = getBool(s);
  }

  public void setMechanizedInfantry(String s)
  {
    m_mechanizedInfantry = getBool(s);
  }

  public void setAARadar(String s)
  {
    m_aARadar = getBool(s);
  }

  public void setShipyards(String s)
  {
    m_shipyards = getBool(s);
  }

  public String getHeavyBomber()
  {
    return "" + m_heavyBomber;
  }

  public String getLongRangeAir()
  {
    return "" + m_longRangeAir;
  }

  public String getJetPower()
  {
    return "" + m_jetPower;
  }

  public String getRocket()
  {
    return "" + m_rocket;
  }

  public String getIndustrialTechnology()
  {
    return "" + m_industrialTechnology;
  }

  public String getSuperSub()
  {
    return "" + m_superSub;
  }
  
  public String getDestroyerBombard()
  {
      return "" + m_destroyerBombard;
  }

  public String getImprovedArtillerySupport()
  {
      return "" + m_improvedArtillerySupport;
  }

  public String getParatroopers()
  {
      return "" + m_paratroopers;
  }

  public String getIncreasedFactoryProduction()
  {
      return "" + m_increasedFactoryProduction;
  }

  public String getWarBonds()
  {
      return "" + m_warBonds;
  }

  public String getMechanizedInfantry()
  {
      return "" + m_mechanizedInfantry;
  }

  public String getAARadar()
  {
      return "" + m_aARadar;
  }

  public String getShipyards()
  {
      return "" + m_shipyards;
  }

  public TechAttachment()
  {
  }

  public boolean hasHeavyBomber()
  {
    return m_heavyBomber;
  }

  public boolean hasLongRangeAir()
  {
    return m_longRangeAir;
  }

  public boolean hasJetPower()
  {
    return m_jetPower;
  }

  public boolean hasRocket()
  {
    return m_rocket;
  }

  public boolean hasIndustrialTechnology()
  {
    return m_industrialTechnology;
  }

  public boolean hasSuperSub()
  {
    return m_superSub;
  }
  
  public boolean hasDestroyerBombard()
  {
      return m_destroyerBombard;
  }
  
  public boolean hasImprovedArtillerySupport()
  {
      return m_improvedArtillerySupport;
  }
  
  public boolean hasParatroopers()
  {
      return m_paratroopers;
  }
  
  public boolean hasIncreasedFactoryProduction()
  {
      return m_increasedFactoryProduction;
  }
  
  public boolean hasWarBonds()
  {
      return m_warBonds;
  }
  
  public boolean hasMechanizedInfantry()
  {
      return m_mechanizedInfantry;
  }
  
  public boolean hasAARadar()
  {
      return m_aARadar;
  }
  
  public boolean hasShipyards()
  {
      return m_shipyards;
  }
  
  

}
