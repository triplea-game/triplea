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
 * TechAdvance.java
 * 
 * Created on November 25, 2001, 4:22 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;

import java.util.*;
import java.util.logging.*;
import java.util.logging.Logger;

/**
 * @author Sean Bridges
 * @version 1.0
 *  
 */
public abstract class TechAdvance implements java.io.Serializable
{
    private static List<TechAdvance> s_3rdEditionAdvances;
    private static List<TechAdvance> s_4thEditionAdvances;
    private static List<TechAdvance> s_AnnivEditionAdvances;
    private static List<TechAdvance> s_AirNavalAdvances;
    private static List<TechAdvance> s_LandProductionAdvances;
    private static List<TechAdvance> s_AnnivAdvanceCategories;
    
    public static final TechAdvance JET_POWER = new JetPowerAdvance();
    public static final TechAdvance SUPER_SUBS = new SuperSubsAdvance();
    public static final TechAdvance LONG_RANGE_AIRCRAFT = new LongRangeAircraftAdvance();
    public static final TechAdvance ROCKETS = new RocketsAdvance();
    public static final TechAdvance INDUSTRIAL_TECHNOLOGY = new IndustrialTechnologyAdvance();
    public static final TechAdvance HEAVY_BOMBER = new HeavyBomberAdvance();
    public static final TechAdvance DESTROYER_BOMBARD = new DestroyerBombardTechAdvance();
    public static final TechAdvance IMPROVED_ARTILLERY_SUPPORT = new ImprovedArtillerySupportAdvance();
    public static final TechAdvance PARATROOPERS = new ParatroopersAdvance();
    public static final TechAdvance INCREASED_FACTORY_PRODUCTION = new IncreasedFactoryProductionAdvance();
    public static final TechAdvance WAR_BONDS = new WarBondsAdvance();
    public static final TechAdvance MECHANIZED_INFANTRY = new MechanizedInfantryAdvance();
    public static final TechAdvance AA_RADAR = new AARadarAdvance();
    public static final TechAdvance IMPROVED_SHIPYARDS = new ImprovedShipyardsAdvance();
    //Technology Categories
    public static final TechAdvance AIR_NAVAL_ADVANCES = new AirNavalAdvances();
    public static final TechAdvance LAND_PRODUCTION_ADVANCES = new LandProductionAdvances();

    public static List<TechAdvance> getTechAdvances(GameData data)
    {
        boolean isFourthEdition = games.strategy.triplea.Properties.getFourthEdition(data);
        //boolean isAnniversaryEditionLandProduction = games.strategy.triplea.Properties.getAnniversaryEditionLandProduction(data);
        //boolean isAnniversaryEditionAirNaval = games.strategy.triplea.Properties.getAnniversaryEditionAirNaval(data);
        boolean isAnniversaryEdition = games.strategy.triplea.Properties.getAnniversaryEdition(data);
        

        
        if(isFourthEdition)
            return s_4thEditionAdvances;
        else if(isAnniversaryEdition)
            return s_AnnivEditionAdvances;
        else
            return s_3rdEditionAdvances;       
    }
    
    public static List<TechAdvance> getTechAdvances(GameData data, TechAdvance techCategory)
    {        
        if(techCategory.equals(TechAdvance.AIR_NAVAL_ADVANCES))
            return s_AirNavalAdvances;  
        else
            return s_LandProductionAdvances;
    }

    public static List<TechAdvance> getTechCategories(GameData data)
    {
        return s_AnnivAdvanceCategories;
    }
    //initialize the advances, note s_advances is made unmodifiable
    static
    {
    	/*
    	 * 3rd Edition Tech
    	 */
        s_3rdEditionAdvances = new ArrayList<TechAdvance>();
        s_3rdEditionAdvances.add(JET_POWER);
        s_3rdEditionAdvances.add(SUPER_SUBS);
        s_3rdEditionAdvances.add(LONG_RANGE_AIRCRAFT);
        s_3rdEditionAdvances.add(ROCKETS);
        s_3rdEditionAdvances.add(INDUSTRIAL_TECHNOLOGY);
        s_3rdEditionAdvances.add(HEAVY_BOMBER);
        s_3rdEditionAdvances = Collections.unmodifiableList(s_3rdEditionAdvances);
        
    	/*
    	 * 4th Edition Tech
    	 */
        s_4thEditionAdvances = new ArrayList<TechAdvance>();
        s_4thEditionAdvances.add(JET_POWER);
        s_4thEditionAdvances.add(SUPER_SUBS);
        s_4thEditionAdvances.add(LONG_RANGE_AIRCRAFT);
        s_4thEditionAdvances.add(ROCKETS);
        s_4thEditionAdvances.add(DESTROYER_BOMBARD);
        s_4thEditionAdvances.add(HEAVY_BOMBER);
		s_4thEditionAdvances.add(INDUSTRIAL_TECHNOLOGY);
        s_4thEditionAdvances = Collections.unmodifiableList(s_4thEditionAdvances);
        
    	/*
    	 * Anniversary Edition Tech
    	 */
        s_AnnivEditionAdvances = new ArrayList<TechAdvance>();
        s_AnnivEditionAdvances.add(IMPROVED_ARTILLERY_SUPPORT);
        s_AnnivEditionAdvances.add(ROCKETS);
        s_AnnivEditionAdvances.add(PARATROOPERS);
        s_AnnivEditionAdvances.add(INCREASED_FACTORY_PRODUCTION);
        s_AnnivEditionAdvances.add(WAR_BONDS);
        s_AnnivEditionAdvances.add(MECHANIZED_INFANTRY);
        s_AnnivEditionAdvances.add(SUPER_SUBS);
        s_AnnivEditionAdvances.add(JET_POWER);
        s_AnnivEditionAdvances.add(IMPROVED_SHIPYARDS);
        s_AnnivEditionAdvances.add(AA_RADAR);
        s_AnnivEditionAdvances.add(LONG_RANGE_AIRCRAFT);
        s_AnnivEditionAdvances.add(HEAVY_BOMBER);
        s_AnnivEditionAdvances = Collections.unmodifiableList(s_AnnivEditionAdvances);
        
        /*
         * Anniversary Edition Air/Naval Tech
         */
        s_AirNavalAdvances = new ArrayList<TechAdvance>();
        s_AirNavalAdvances.add(SUPER_SUBS);
        s_AirNavalAdvances.add(JET_POWER);
        s_AirNavalAdvances.add(IMPROVED_SHIPYARDS);
        s_AirNavalAdvances.add(AA_RADAR);
        s_AirNavalAdvances.add(LONG_RANGE_AIRCRAFT);
        s_AirNavalAdvances.add(HEAVY_BOMBER);
        s_AirNavalAdvances = Collections.unmodifiableList(s_AirNavalAdvances);

        /*
         * Anniversary Edition Land/Production Tech
         */
        s_LandProductionAdvances = new ArrayList<TechAdvance>();
        s_LandProductionAdvances.add(IMPROVED_ARTILLERY_SUPPORT);
        s_LandProductionAdvances.add(ROCKETS);
        s_LandProductionAdvances.add(PARATROOPERS);
        s_LandProductionAdvances.add(INCREASED_FACTORY_PRODUCTION);
        s_LandProductionAdvances.add(WAR_BONDS);
        s_LandProductionAdvances.add(MECHANIZED_INFANTRY);
        s_LandProductionAdvances = Collections.unmodifiableList(s_LandProductionAdvances);
        
        /*
         * Anniversary Edition Land/Production Tech Categories
         */
        s_AnnivAdvanceCategories = new ArrayList<TechAdvance>();
        s_AnnivAdvanceCategories.add(AIR_NAVAL_ADVANCES);
        s_AnnivAdvanceCategories.add(LAND_PRODUCTION_ADVANCES);
        s_AnnivAdvanceCategories = Collections.unmodifiableList(s_AnnivAdvanceCategories);
    }

    public abstract String getName();
    public abstract String getProperty();
    public abstract void perform(PlayerID id, IDelegateBridge bridge, GameData data);

    public boolean equals(Object o)
    {
        if (!(o instanceof TechAdvance))
            return false;

        TechAdvance ta = (TechAdvance) o;

        if (ta.getName() == null || getName() == null)
            return false;

        return getName().equals(ta.getName());
    }

    public int hashCode()
    {
        if (getName() == null)
            return super.hashCode();

        return getName().hashCode();
    }

    public String toString()
    {
        return getName();
    }
}



class SuperSubsAdvance extends TechAdvance
{
    public String getName()
    {
        return "Super subs";
    }

    public String getProperty()
    {
        return "superSub";
    }

    public void perform(PlayerID id, IDelegateBridge bridge, GameData data)
    {
    }
}



class HeavyBomberAdvance extends TechAdvance
{
    public String getName()
    {
        return "Heavy Bomber";
    }

    public String getProperty()
    {
        return "heavyBomber";
    }

    public void perform(PlayerID id, IDelegateBridge bridge, GameData data)
    {
    }

}


//TODO COMCO repeat for Enhanced Shipyards?
class IndustrialTechnologyAdvance extends TechAdvance
{
    public String getName()
    {
        return "Industrial Technology";
    }

    public String getProperty()
    {
        return "industrialTechnology";
    }

    public void perform(PlayerID id, IDelegateBridge bridge, GameData data)
    {
        ProductionFrontier current = id.getProductionFrontier();
        //they already have it
        if(current.getName().endsWith("IndustrialTechnology"))
            return;
        
        String industrialTechName = current.getName() + "IndustrialTechnology";
        
        ProductionFrontier advancedTech = data.getProductionFrontierList().getProductionFrontier(industrialTechName);
        
        //it doesnt exist, dont crash
        if(advancedTech == null)
        {
            Logger.getLogger(TechAdvance.class.getName()).log(Level.WARNING, "No tech named:" + industrialTechName + " not adding tech");
            return;
        }
        
        Change prodChange = ChangeFactory.changeProductionFrontier(id, advancedTech);
        bridge.addChange(prodChange);
    }
}



class JetPowerAdvance extends TechAdvance
{
    public String getName()
    {
        return "Jet Power";
    }

    public String getProperty()
    {
        return "jetPower";
    }

    public void perform(PlayerID id, IDelegateBridge bridge, GameData data)
    {
    }

}



class RocketsAdvance extends TechAdvance
{
    public String getName()
    {
        return "Rockets Advance";
    }

    public String getProperty()
    {
        return "rocket";
    }

    public void perform(PlayerID id, IDelegateBridge bridge, GameData data)
    {
    }

}

class DestroyerBombardTechAdvance extends TechAdvance
{
    public String getName()
    {
        return "Destroyer Bombard";
    }

    public String getProperty()
    {
        return "destroyerBombard";
    }

    public void perform(PlayerID id, IDelegateBridge bridge, GameData data)
    {
    }
}



class LongRangeAircraftAdvance extends TechAdvance
{
    public String getName()
    {
        return "Long Range Aircraft";
    }

    public String getProperty()
    {
        return "longRangeAir";
    }

    public void perform(PlayerID id, IDelegateBridge bridge, GameData data)
    {
    }
}

/**
 * Beginning of AA 50 rules
 */
/*
 * Artillery can support multiple infantry
 */
class ImprovedArtillerySupportAdvance extends TechAdvance
{
    public String getName()
    {
        return "Improved Artillery Support";
    }

    public String getProperty()
    {
        return "improvedArtillerySupport";
    }

    public void perform(PlayerID id, IDelegateBridge bridge, GameData data)
    {
    }
}

/*
 * Support paratroops
 */
class ParatroopersAdvance extends TechAdvance
{
    public String getName()
    {
        return "Paratroopers";
    }

    public String getProperty()
    {
        return "paratroopers";
    }

    public void perform(PlayerID id, IDelegateBridge bridge, GameData data)
    {
    }
}

/*
 * Increased Factory Production
 */
class IncreasedFactoryProductionAdvance extends TechAdvance
{
    public String getName()
    {
        return "Increased Factory Production";
    }

    public String getProperty()
    {
        return "increasedFactoryProduction";
    }

    public void perform(PlayerID id, IDelegateBridge bridge, GameData data)
    {
    }
}

/*
 * War Bonds
 */
class WarBondsAdvance extends TechAdvance
{
    public String getName()
    {
        return "War Bonds";
    }

    public String getProperty()
    {
        return "warBonds";
    }

    public void perform(PlayerID id, IDelegateBridge bridge, GameData data)
    {
    }
}

/*
 * Mechanized Infantry
 */
class MechanizedInfantryAdvance extends TechAdvance
{
    public String getName()
    {
        return "Mechanized Infantry";
    }

    public String getProperty()
    {
        return "mechanizedInfantry";
    }

    public void perform(PlayerID id, IDelegateBridge bridge, GameData data)
    {
    }
}

/*
 * AA Radar
 */
class AARadarAdvance extends TechAdvance
{
    public String getName()
    {
        return "AA Radar";
    }

    public String getProperty()
    {
        return "aARadar";
    }

    public void perform(PlayerID id, IDelegateBridge bridge, GameData data)
    {
    }
}

class ImprovedShipyardsAdvance extends TechAdvance
{
    public String getName()
    {
        return "Shipyards";
    }

    public String getProperty()
    {
        return "shipyards";
    }

    public void perform(PlayerID id, IDelegateBridge bridge, GameData data)
    {
        ProductionFrontier current = id.getProductionFrontier();
        //they already have it
        if(current.getName().endsWith("Shipyards"))
            return;
        
        String industrialTechName = current.getName() + "Shipyards";
        
        ProductionFrontier advancedTech = data.getProductionFrontierList().getProductionFrontier(industrialTechName);
        
        //it doesnt exist, dont crash
        if(advancedTech == null)
        {
            Logger.getLogger(TechAdvance.class.getName()).log(Level.WARNING, "No tech named:" + industrialTechName + " not adding tech");
            return;
        }
        
        Change prodChange = ChangeFactory.changeProductionFrontier(id, advancedTech);
        bridge.addChange(prodChange);
    }
}


/*
 * Land & Production Tech Category
 */
class LandProductionAdvances extends TechAdvance
{
    public String getName()
    {
        return "Land and Production Advances";
    }

    public String getProperty()
    {
        return "landProduction";
    }

    public void perform(PlayerID id, IDelegateBridge bridge, GameData data)
    {
    }
}


/*
 * Land & Production Tech Category
 */
class AirNavalAdvances extends TechAdvance
{
    public String getName()
    {
        return "Air and Naval Advances";
    }

    public String getProperty()
    {
        return "airNavalAdvances";
    }

    public void perform(PlayerID id, IDelegateBridge bridge, GameData data)
    {
    }
}
/**
 * End of AA 50 rules
 */
