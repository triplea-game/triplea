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
    return (TechAttatchment) id.getAttatchment(Constants.TECH_ATTATCHMENT_NAME);
  }

  private boolean mheavyBomber;
  private boolean mlongRangeAir;
  private boolean mjetPower;
  private boolean mrocket;
  private boolean mindustrialTechnology;
  private boolean msuperSub;

  public void setHeavyBomber(String s)
  {
    mheavyBomber = getBool(s);
  }

  public void setLongRangeAir(String s)
  {
    mlongRangeAir = getBool(s);
  }

  public void setJetPower(String s)
  {
    mjetPower = getBool(s);
  }

  public void setRocket(String s)
  {
    mrocket = getBool(s);
  }

  public void setIndustrialTechnology(String s)
  {
    mindustrialTechnology = getBool(s);
  }

  public void setSuperSub(String s)
  {
    msuperSub = getBool(s);
  }

  public String getHeavyBomber()
  {
    return "" + mheavyBomber;
  }

  public String getLongRangeAir()
  {
    return "" + mlongRangeAir;
  }

  public String getJetPower()
  {
    return "" + mjetPower;
  }

  public String getRocket()
  {
    return "" + mrocket;
  }

  public String getIndustrialTechnology()
  {
    return "" + mindustrialTechnology;
  }

  public String getsuperSub()
  {
    return "" + msuperSub;
  }

  public TechAttatchment()
  {
  }

  public boolean hasHeavyBomber()
  {
    return mheavyBomber;
  }

  public boolean hasLongRangeAir()
  {
    return mlongRangeAir;
  }

  public boolean hasJetPower()
  {
    return mjetPower;
  }

  public boolean hasRocket()
  {
    return mrocket;
  }

  public boolean hasIndustrialTechnology()
  {
    return mindustrialTechnology;
  }

  public boolean hasSuperSub()
  {
    return msuperSub;
  }

}
