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
 * TechTracker.java
 *
 * Created on November 30, 2001, 2:20 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attatchments.TechAttachment;

import java.util.*;

/**
 * Tracks which players have which technology advances.
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class TechTracker implements java.io.Serializable
{

  /** Creates new TechTracker */
  public TechTracker()
  {
  }

  public static Collection<TechAdvance> getTechAdvances(PlayerID id)
  {
    Collection<TechAdvance> rVal = new ArrayList<TechAdvance>();
    TechAttachment attatchment = TechAttachment.get(id);

    if(attatchment.hasHeavyBomber())
    {
      rVal.add(TechAdvance.HEAVY_BOMBER);
    }
    if(attatchment.hasIndustrialTechnology())
    {
      rVal.add(TechAdvance.INDUSTRIAL_TECHNOLOGY);
    }
    if(attatchment.hasJetPower())
    {
      rVal.add(TechAdvance.JET_POWER);
    }
    if(attatchment.hasLongRangeAir())
    {
      rVal.add(TechAdvance.LONG_RANGE_AIRCRAFT);
    }
    if(attatchment.hasRocket())
    {
      rVal.add(TechAdvance.ROCKETS);
    }
    if(attatchment.hasSuperSub())
    {
      rVal.add(TechAdvance.SUPER_SUBS);
    }
    if(attatchment.hasDestroyerBombard())
    {
      rVal.add(TechAdvance.DESTROYER_BOMBARD);
    }
    if(attatchment.hasImprovedArtillerySupport())
    {
      rVal.add(TechAdvance.IMPROVED_ARTILLERY_SUPPORT);
    }
    if(attatchment.hasParatroopers())
    {
      rVal.add(TechAdvance.PARATROOPERS);
    }
    if(attatchment.hasIncreasedFactoryProduction())
    {
      rVal.add(TechAdvance.INCREASED_FACTORY_PRODUCTION);
    }
    if(attatchment.hasWarBonds())
    {
      rVal.add(TechAdvance.WAR_BONDS);
    }
    if(attatchment.hasMechanizedInfantry())
    {
      rVal.add(TechAdvance.MECHANIZED_INFANTRY);
    }
    if(attatchment.hasAARadar())
    {
      rVal.add(TechAdvance.AA_RADAR);
    }
    if(attatchment.hasShipyards())
    {
      rVal.add(TechAdvance.IMPROVED_SHIPYARDS);
    }



    return rVal;
  }
  
  public static Collection<TechAdvance> getTechCategories(PlayerID id)
  {
    Collection<TechAdvance> rVal = new ArrayList<TechAdvance>();
    TechAttachment attatchment = TechAttachment.get(id);

    if(attatchment.hasSuperSub() && attatchment.hasJetPower() && attatchment.hasShipyards() && 
                    attatchment.hasAARadar() && attatchment.hasLongRangeAir() && attatchment.hasHeavyBomber())
    {
      rVal.add(TechAdvance.AIR_NAVAL_ADVANCES);
    }

    if(attatchment.hasImprovedArtillerySupport() && attatchment.hasRocket() && attatchment.hasParatroopers() &&
                    attatchment.hasIncreasedFactoryProduction() && attatchment.hasWarBonds() && attatchment.hasMechanizedInfantry())
    {
      rVal.add(TechAdvance.LAND_PRODUCTION_ADVANCES);
    }
    
    return rVal;
  }
  
  
  public static synchronized void addAdvance(PlayerID player, GameData data, IDelegateBridge bridge, TechAdvance advance)
  {
    Change attatchmentChange = ChangeFactory.attachmentPropertyChange(TechAttachment.get(player), "true", advance.getProperty());
    bridge.addChange(attatchmentChange);
    advance.perform(player, bridge, data);
  }

  public static boolean hasLongRangeAir(PlayerID player)
  {
    return TechAttachment.get(player).hasLongRangeAir();
  }

  public static boolean hasHeavyBomber(PlayerID player)
  {
    return TechAttachment.get(player).hasHeavyBomber();
  }

  public static boolean hasSuperSubs(PlayerID player)
  {
    return TechAttachment.get(player).hasSuperSub();
  }

  public static boolean hasJetFighter(PlayerID player)
  {
    return TechAttachment.get(player).hasJetPower();
  }

  public static boolean hasRocket(PlayerID player)
  {
    return TechAttachment.get(player).hasRocket();
  }

  public static boolean hasIndustrialTechnology(PlayerID player)
  {
    return TechAttachment.get(player).hasIndustrialTechnology();
  }

  public static boolean hasDestroyerBombard(PlayerID player)
  {
    return TechAttachment.get(player).hasDestroyerBombard();
  }

  public static boolean hasImprovedArtillerySupport(PlayerID player)
  {
    return TechAttachment.get(player).hasImprovedArtillerySupport();
  }

  public static boolean hasParatroopers(PlayerID player)
  {
    return TechAttachment.get(player).hasParatroopers();
  }

  public static boolean hasIncreasedFactoryProduction(PlayerID player)
  {
    return TechAttachment.get(player).hasIncreasedFactoryProduction();
  }

  public static boolean hasWarBonds(PlayerID player)
  {
    return TechAttachment.get(player).hasWarBonds();
  }

  public static boolean hasMechanizedInfantry(PlayerID player)
  {
    return TechAttachment.get(player).hasMechanizedInfantry();
  }

  public static boolean hasAARadar(PlayerID player)
  {
    return TechAttachment.get(player).hasAARadar();
  }

  public static boolean hasShipyards(PlayerID player)
  {
    return TechAttachment.get(player).hasShipyards();
  }
}
