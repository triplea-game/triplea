/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * TechAdvance.java
 * 
 * Created on November 25, 2001, 4:22 PM
 */
package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attatchments.TechAttachment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sean Bridges
 * @version 1.0
 * 
 */
public abstract class TechAdvance implements java.io.Serializable
{
	private static List<TechAdvance> s_WW2V1Advances;
	private static List<TechAdvance> s_WW2V2Advances;
	private static List<TechAdvance> s_WW2V3Advances;
	private static List<TechAdvance> s_AirNavalAdvances;
	private static List<TechAdvance> s_LandProductionAdvances;
	// private static List<TechAdvance> s_WW2V3AdvanceCategories;
	private static List<TechAdvance> s_allDefined;
	private static List<TechnologyFrontier> s_WW2V3Categories;
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
	
	// Technology Categories
	// public static final TechAdvance AIR_NAVAL_ADVANCES = new AirNavalAdvances();
	// public static final TechAdvance LAND_PRODUCTION_ADVANCES = new LandProductionAdvances();
	public static List<TechAdvance> getTechAdvances(final GameData data, final PlayerID player)
	{
		final boolean isWW2V2 = games.strategy.triplea.Properties.getWW2V2(data);
		final boolean isWW2V3 = games.strategy.triplea.Properties.getWW2V3(data);
		if (!data.getTechnologyFrontier().isEmpty())
		{
			if (player != null)
			{
				return player.getTechnologyFrontierList().getAdvances();
			}
			else
			{
				return data.getTechnologyFrontier().getTechs();
			}
		}
		if (isWW2V2)
			return s_WW2V2Advances;
		else if (isWW2V3)
			return s_WW2V3Advances;
		else
			return s_WW2V1Advances;
	}
	
	/*
	public static List<TechAdvance> getAdvancesFromCategory(GameData data, TechAdvance techCategory)
	{        
	    if(techCategory.equals(TechAdvance.AIR_NAVAL_ADVANCES))
	        return s_AirNavalAdvances;  
	    else
	        return s_LandProductionAdvances;
	}
	*/
	public static TechAdvance findDefinedAdvance(final String s)
	{
		for (final TechAdvance t : s_allDefined)
		{
			if (t.getProperty().equals(s))
				return t;
		}
		throw new IllegalArgumentException(s + " is not a valid technology");
	}
	
	public static List<TechnologyFrontier> getTechCategories(final GameData data, final PlayerID player)
	{
		if (player != null && !data.getTechnologyFrontier().isEmpty())
			return player.getTechnologyFrontierList().getFrontiers();
		if (s_WW2V3Categories == null)
		{
			final List<TechnologyFrontier> tf = new ArrayList<TechnologyFrontier>();
			final TechnologyFrontier an = new TechnologyFrontier("Air and Naval Advances", data);
			an.addAdvance(s_AirNavalAdvances);
			final TechnologyFrontier lp = new TechnologyFrontier("Land and Production Advances", data);
			lp.addAdvance(s_LandProductionAdvances);
			tf.add(an);
			tf.add(lp);
			s_WW2V3Categories = tf;
		}
		return s_WW2V3Categories;
	}
	
	public static TechAdvance findAdvance(final String s, final GameData data, final PlayerID player)
	{
		for (final TechAdvance t : getTechAdvances(data, player))
		{
			if (t.getProperty().equals(s))
				return t;
		}
		throw new IllegalArgumentException(s + " is not a valid technology");
	}
	
	public static List<TechAdvance> getDefinedAdvances()
	{
		return s_allDefined;
	}
	
	// initialize the advances, note s_advances is made unmodifiable
	static
	{
		/*
		 * World War 2 Version 1 Tech
		 */
		s_WW2V1Advances = new ArrayList<TechAdvance>();
		s_WW2V1Advances.add(JET_POWER);
		s_WW2V1Advances.add(SUPER_SUBS);
		s_WW2V1Advances.add(LONG_RANGE_AIRCRAFT);
		s_WW2V1Advances.add(ROCKETS);
		s_WW2V1Advances.add(INDUSTRIAL_TECHNOLOGY);
		s_WW2V1Advances.add(HEAVY_BOMBER);
		s_WW2V1Advances = Collections.unmodifiableList(s_WW2V1Advances);
		/*
		 * World War 2 Version 2 Tech
		 */
		s_WW2V2Advances = new ArrayList<TechAdvance>();
		s_WW2V2Advances.add(JET_POWER);
		s_WW2V2Advances.add(SUPER_SUBS);
		s_WW2V2Advances.add(LONG_RANGE_AIRCRAFT);
		s_WW2V2Advances.add(ROCKETS);
		s_WW2V2Advances.add(DESTROYER_BOMBARD);
		s_WW2V2Advances.add(HEAVY_BOMBER);
		s_WW2V2Advances.add(INDUSTRIAL_TECHNOLOGY);
		s_WW2V2Advances = Collections.unmodifiableList(s_WW2V2Advances);
		/*
		 * World War 2 Version 3 Tech
		 */
		s_WW2V3Advances = new ArrayList<TechAdvance>();
		s_WW2V3Advances.add(SUPER_SUBS);
		s_WW2V3Advances.add(JET_POWER);
		s_WW2V3Advances.add(IMPROVED_SHIPYARDS);
		s_WW2V3Advances.add(AA_RADAR);
		s_WW2V3Advances.add(LONG_RANGE_AIRCRAFT);
		s_WW2V3Advances.add(HEAVY_BOMBER);
		s_WW2V3Advances.add(IMPROVED_ARTILLERY_SUPPORT);
		s_WW2V3Advances.add(ROCKETS);
		s_WW2V3Advances.add(PARATROOPERS);
		s_WW2V3Advances.add(INCREASED_FACTORY_PRODUCTION);
		s_WW2V3Advances.add(WAR_BONDS);
		s_WW2V3Advances.add(MECHANIZED_INFANTRY);
		s_WW2V3Advances = Collections.unmodifiableList(s_WW2V3Advances);
		/*
		 * WW2V3 Air/Naval Tech
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
		 * WW2V3 Land/Production Tech
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
		 * WW2V3 Land/Production Tech Categories
		 */
		/*
		s_WW2V3AdvanceCategories = new ArrayList<TechAdvance>();
		s_WW2V3AdvanceCategories.add(AIR_NAVAL_ADVANCES);
		s_WW2V3AdvanceCategories.add(LAND_PRODUCTION_ADVANCES);
		s_WW2V3AdvanceCategories = Collections.unmodifiableList(s_WW2V3AdvanceCategories);
		*/
		/*
		 * List of all hardcoded Techs. 
		 */
		s_allDefined = new ArrayList<TechAdvance>();
		s_allDefined.addAll(s_WW2V3Advances);
		s_allDefined.add(INDUSTRIAL_TECHNOLOGY);
		s_allDefined.add(DESTROYER_BOMBARD);
		s_allDefined = Collections.unmodifiableList(s_allDefined);
	}
	
	public abstract String getName();
	
	public abstract String getProperty();
	
	public abstract void perform(PlayerID id, IDelegateBridge bridge);
	
	public abstract boolean hasTech(TechAttachment ta);
	
	@Override
	public boolean equals(final Object o)
	{
		if (!(o instanceof TechAdvance))
			return false;
		final TechAdvance ta = (TechAdvance) o;
		if (ta.getName() == null || getName() == null)
			return false;
		return getName().equals(ta.getName());
	}
	
	@Override
	public int hashCode()
	{
		if (getName() == null)
			return super.hashCode();
		return getName().hashCode();
	}
	
	@Override
	public String toString()
	{
		return getName();
	}
}


class SuperSubsAdvance extends TechAdvance
{
	@Override
	public String getName()
	{
		return "Super subs";
	}
	
	@Override
	public String getProperty()
	{
		return "superSub";
	}
	
	@Override
	public void perform(final PlayerID id, final IDelegateBridge bridge)
	{
	}
	
	@Override
	public boolean hasTech(final TechAttachment ta)
	{
		return ta.hasSuperSub();
	}
}


class HeavyBomberAdvance extends TechAdvance
{
	@Override
	public String getName()
	{
		return "Heavy Bomber";
	}
	
	@Override
	public String getProperty()
	{
		return "heavyBomber";
	}
	
	@Override
	public void perform(final PlayerID id, final IDelegateBridge bridge)
	{
	}
	
	@Override
	public boolean hasTech(final TechAttachment ta)
	{
		return ta.hasHeavyBomber();
	}
}


class IndustrialTechnologyAdvance extends TechAdvance
{
	@Override
	public String getName()
	{
		return "Industrial Technology";
	}
	
	@Override
	public String getProperty()
	{
		return "industrialTechnology";
	}
	
	@Override
	public void perform(final PlayerID id, final IDelegateBridge bridge)
	{
		final ProductionFrontier current = id.getProductionFrontier();
		// they already have it
		if (current.getName().endsWith("IndustrialTechnology"))
			return;
		final String industrialTechName = current.getName() + "IndustrialTechnology";
		final ProductionFrontier advancedTech = bridge.getData().getProductionFrontierList().getProductionFrontier(industrialTechName);
		// it doesnt exist, dont crash
		if (advancedTech == null)
		{
			Logger.getLogger(TechAdvance.class.getName()).log(Level.WARNING, "No tech named:" + industrialTechName + " not adding tech");
			return;
		}
		final Change prodChange = ChangeFactory.changeProductionFrontier(id, advancedTech);
		bridge.addChange(prodChange);
	}
	
	@Override
	public boolean hasTech(final TechAttachment ta)
	{
		return ta.hasIndustrialTechnology();
	}
}


class JetPowerAdvance extends TechAdvance
{
	@Override
	public String getName()
	{
		return "Jet Power";
	}
	
	@Override
	public String getProperty()
	{
		return "jetPower";
	}
	
	@Override
	public void perform(final PlayerID id, final IDelegateBridge bridge)
	{
	}
	
	@Override
	public boolean hasTech(final TechAttachment ta)
	{
		return ta.hasJetPower();
	}
}


class RocketsAdvance extends TechAdvance
{
	@Override
	public String getName()
	{
		return "Rockets Advance";
	}
	
	@Override
	public String getProperty()
	{
		return "rocket";
	}
	
	@Override
	public void perform(final PlayerID id, final IDelegateBridge bridge)
	{
	}
	
	@Override
	public boolean hasTech(final TechAttachment ta)
	{
		return ta.hasRocket();
	}
}


class DestroyerBombardTechAdvance extends TechAdvance
{
	@Override
	public String getName()
	{
		return "Destroyer Bombard";
	}
	
	@Override
	public String getProperty()
	{
		return "destroyerBombard";
	}
	
	@Override
	public void perform(final PlayerID id, final IDelegateBridge bridge)
	{
	}
	
	@Override
	public boolean hasTech(final TechAttachment ta)
	{
		return ta.hasDestroyerBombard();
	}
}


class LongRangeAircraftAdvance extends TechAdvance
{
	@Override
	public String getName()
	{
		return "Long Range Aircraft";
	}
	
	@Override
	public String getProperty()
	{
		return "longRangeAir";
	}
	
	@Override
	public void perform(final PlayerID id, final IDelegateBridge bridge)
	{
	}
	
	@Override
	public boolean hasTech(final TechAttachment ta)
	{
		return ta.hasLongRangeAir();
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
	@Override
	public String getName()
	{
		return "Improved Artillery Support";
	}
	
	@Override
	public String getProperty()
	{
		return "improvedArtillerySupport";
	}
	
	@Override
	public void perform(final PlayerID id, final IDelegateBridge bridge)
	{
	}
	
	@Override
	public boolean hasTech(final TechAttachment ta)
	{
		return ta.hasImprovedArtillerySupport();
	}
}


/*
 * Support paratroops
 */
class ParatroopersAdvance extends TechAdvance
{
	@Override
	public String getName()
	{
		return "Paratroopers";
	}
	
	@Override
	public String getProperty()
	{
		return "paratroopers";
	}
	
	@Override
	public void perform(final PlayerID id, final IDelegateBridge bridge)
	{
	}
	
	@Override
	public boolean hasTech(final TechAttachment ta)
	{
		return ta.hasParatroopers();
	}
}


/*
 * Increased Factory Production
 */
class IncreasedFactoryProductionAdvance extends TechAdvance
{
	@Override
	public String getName()
	{
		return "Increased Factory Production";
	}
	
	@Override
	public String getProperty()
	{
		return "increasedFactoryProduction";
	}
	
	@Override
	public void perform(final PlayerID id, final IDelegateBridge bridge)
	{
	}
	
	@Override
	public boolean hasTech(final TechAttachment ta)
	{
		return ta.hasIncreasedFactoryProduction();
	}
}


/*
 * War Bonds
 */
class WarBondsAdvance extends TechAdvance
{
	@Override
	public String getName()
	{
		return "War Bonds";
	}
	
	@Override
	public String getProperty()
	{
		return "warBonds";
	}
	
	@Override
	public void perform(final PlayerID id, final IDelegateBridge bridge)
	{
	}
	
	@Override
	public boolean hasTech(final TechAttachment ta)
	{
		return ta.hasWarBonds();
	}
}


/*
 * Mechanized Infantry
 */
class MechanizedInfantryAdvance extends TechAdvance
{
	@Override
	public String getName()
	{
		return "Mechanized Infantry";
	}
	
	@Override
	public String getProperty()
	{
		return "mechanizedInfantry";
	}
	
	@Override
	public void perform(final PlayerID id, final IDelegateBridge bridge)
	{
	}
	
	@Override
	public boolean hasTech(final TechAttachment ta)
	{
		return ta.hasMechanizedInfantry();
	}
}


/*
 * AA Radar
 */
class AARadarAdvance extends TechAdvance
{
	@Override
	public String getName()
	{
		return "AA Radar";
	}
	
	@Override
	public String getProperty()
	{
		return "aARadar";
	}
	
	@Override
	public void perform(final PlayerID id, final IDelegateBridge bridge)
	{
	}
	
	@Override
	public boolean hasTech(final TechAttachment ta)
	{
		return ta.hasAARadar();
	}
}


class ImprovedShipyardsAdvance extends TechAdvance
{
	@Override
	public String getName()
	{
		return "Shipyards";
	}
	
	@Override
	public String getProperty()
	{
		return "shipyards";
	}
	
	@Override
	public void perform(final PlayerID id, final IDelegateBridge bridge)
	{
		final GameData data = bridge.getData();
		if (!games.strategy.triplea.Properties.getUse_Shipyards(data))
			return;
		final ProductionFrontier current = id.getProductionFrontier();
		// they already have it
		if (current.getName().endsWith("Shipyards"))
			return;
		final String industrialTechName = current.getName() + "Shipyards";
		final ProductionFrontier advancedTech = data.getProductionFrontierList().getProductionFrontier(industrialTechName);
		// it doesnt exist, dont crash
		if (advancedTech == null)
		{
			Logger.getLogger(TechAdvance.class.getName()).log(Level.WARNING, "No tech named:" + industrialTechName + " not adding tech");
			return;
		}
		final Change prodChange = ChangeFactory.changeProductionFrontier(id, advancedTech);
		bridge.addChange(prodChange);
	}
	
	@Override
	public boolean hasTech(final TechAttachment ta)
	{
		return ta.hasShipyards();
	}
}
/*
 * Land & Production Tech Category
 */
/*
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

    public void perform(PlayerID id, IDelegateBridge bridge)
    {
    }
}
*/
/*
 * Land & Production Tech Category
 */
/*
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

    public void perform(PlayerID id, IDelegateBridge bridge)
    {
    }
}
*/
/**
 * End of AA 50 rules
 */
