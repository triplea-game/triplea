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

public class TechAttatchment extends DefaultAttatchment
{

  //attatches to a PlayerID

  public static TechAttatchment get(PlayerID id)
  {
    TechAttatchment attatchment = (TechAttatchment) id.getAttatchment(Constants.TECH_ATTATCHMENT_NAME);
    //dont crash
    if(attatchment == null)
        return new TechAttatchment();
    return attatchment;
  }

  private boolean m_heavyBomber;
  private boolean m_longRangeAir;
  private boolean m_jetPower;
  private boolean m_rocket;
  private boolean m_industrialTechnology;
  private boolean m_superSub;
  private boolean m_destroyerBombard;
  
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

  public TechAttatchment()
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
  

}
