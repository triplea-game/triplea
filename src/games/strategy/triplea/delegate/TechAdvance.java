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
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attatchments.TechAttachment;

import java.io.Serializable;
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
public abstract class TechAdvance extends NamedAttachable implements Serializable
{
	private static final long serialVersionUID = -1076712297024403156L;
	private static List<TechAdvance> s_WW2V1Advances = null;
	private static List<TechAdvance> s_WW2V2Advances = null;
	private static List<TechAdvance> s_WW2V3Advances = null;
	private static List<TechAdvance> s_AirNavalAdvances = null;
	private static List<TechAdvance> s_LandProductionAdvances = null;
	private static List<TechAdvance> s_allDefined = null;
	private static List<TechnologyFrontier> s_WW2V3Categories = null;
	public static TechAdvance JET_POWER = null;
	public static TechAdvance SUPER_SUBS = null;
	public static TechAdvance LONG_RANGE_AIRCRAFT = null;
	public static TechAdvance ROCKETS = null;
	public static TechAdvance INDUSTRIAL_TECHNOLOGY = null;
	public static TechAdvance HEAVY_BOMBER = null;
	public static TechAdvance DESTROYER_BOMBARD = null;
	public static TechAdvance IMPROVED_ARTILLERY_SUPPORT = null;
	public static TechAdvance PARATROOPERS = null;
	public static TechAdvance INCREASED_FACTORY_PRODUCTION = null;
	public static TechAdvance WAR_BONDS = null;
	public static TechAdvance MECHANIZED_INFANTRY = null;
	public static TechAdvance AA_RADAR = null;
	public static TechAdvance IMPROVED_SHIPYARDS = null;
	
	public TechAdvance(final String name, final GameData data)
	{
		super(name, data);
	}
	
	public abstract String getProperty();
	
	public abstract void perform(PlayerID id, IDelegateBridge bridge);
	
	public abstract boolean hasTech(TechAttachment ta);
	
	/**
	 * This should be called before parsing Techs, when we are parsing the xml.
	 */
	public static void setStaticTechs(final GameData data)
	{
		// we first create all the hard-coded techs
		JET_POWER = new JetPowerAdvance(data);
		SUPER_SUBS = new SuperSubsAdvance(data);
		LONG_RANGE_AIRCRAFT = new LongRangeAircraftAdvance(data);
		ROCKETS = new RocketsAdvance(data);
		INDUSTRIAL_TECHNOLOGY = new IndustrialTechnologyAdvance(data);
		HEAVY_BOMBER = new HeavyBomberAdvance(data);
		DESTROYER_BOMBARD = new DestroyerBombardTechAdvance(data);
		IMPROVED_ARTILLERY_SUPPORT = new ImprovedArtillerySupportAdvance(data);
		PARATROOPERS = new ParatroopersAdvance(data);
		INCREASED_FACTORY_PRODUCTION = new IncreasedFactoryProductionAdvance(data);
		WAR_BONDS = new WarBondsAdvance(data);
		MECHANIZED_INFANTRY = new MechanizedInfantryAdvance(data);
		AA_RADAR = new AARadarAdvance(data);
		IMPROVED_SHIPYARDS = new ImprovedShipyardsAdvance(data);
		
		// then initialize the advances, note s_advances is made unmodifiable
		
		// World War 2 Version 1 Tech
		s_WW2V1Advances = new ArrayList<TechAdvance>();
		s_WW2V1Advances.add(JET_POWER);
		s_WW2V1Advances.add(SUPER_SUBS);
		s_WW2V1Advances.add(LONG_RANGE_AIRCRAFT);
		s_WW2V1Advances.add(ROCKETS);
		s_WW2V1Advances.add(INDUSTRIAL_TECHNOLOGY);
		s_WW2V1Advances.add(HEAVY_BOMBER);
		s_WW2V1Advances = Collections.unmodifiableList(s_WW2V1Advances);
		
		// World War 2 Version 2 Tech
		s_WW2V2Advances = new ArrayList<TechAdvance>();
		s_WW2V2Advances.add(JET_POWER);
		s_WW2V2Advances.add(SUPER_SUBS);
		s_WW2V2Advances.add(LONG_RANGE_AIRCRAFT);
		s_WW2V2Advances.add(ROCKETS);
		s_WW2V2Advances.add(DESTROYER_BOMBARD);
		s_WW2V2Advances.add(HEAVY_BOMBER);
		s_WW2V2Advances.add(INDUSTRIAL_TECHNOLOGY);
		s_WW2V2Advances = Collections.unmodifiableList(s_WW2V2Advances);
		
		// World War 2 Version 3 Tech
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
		
		// WW2V3 Air/Naval Tech
		s_AirNavalAdvances = new ArrayList<TechAdvance>();
		s_AirNavalAdvances.add(SUPER_SUBS);
		s_AirNavalAdvances.add(JET_POWER);
		s_AirNavalAdvances.add(IMPROVED_SHIPYARDS);
		s_AirNavalAdvances.add(AA_RADAR);
		s_AirNavalAdvances.add(LONG_RANGE_AIRCRAFT);
		s_AirNavalAdvances.add(HEAVY_BOMBER);
		s_AirNavalAdvances = Collections.unmodifiableList(s_AirNavalAdvances);
		
		// WW2V3 Land/Production Tech
		s_LandProductionAdvances = new ArrayList<TechAdvance>();
		s_LandProductionAdvances.add(IMPROVED_ARTILLERY_SUPPORT);
		s_LandProductionAdvances.add(ROCKETS);
		s_LandProductionAdvances.add(PARATROOPERS);
		s_LandProductionAdvances.add(INCREASED_FACTORY_PRODUCTION);
		s_LandProductionAdvances.add(WAR_BONDS);
		s_LandProductionAdvances.add(MECHANIZED_INFANTRY);
		s_LandProductionAdvances = Collections.unmodifiableList(s_LandProductionAdvances);
		
		// List of all hardcoded Techs.
		s_allDefined = new ArrayList<TechAdvance>();
		s_allDefined.addAll(s_WW2V3Advances);
		s_allDefined.add(INDUSTRIAL_TECHNOLOGY);
		s_allDefined.add(DESTROYER_BOMBARD);
		s_allDefined = Collections.unmodifiableList(s_allDefined);
	}
	
	public static List<TechAdvance> getTechAdvances(final GameData data)
	{
		return getTechAdvances(data, null);
	}
	
	public static List<TechAdvance> getTechAdvances(final GameData data, final PlayerID player)
	{
		final TechnologyFrontier technologyFrontier;
		data.acquireReadLock();
		try
		{
			technologyFrontier = data.getTechnologyFrontier();
		} finally
		{
			data.releaseReadLock();
		}
		if (technologyFrontier != null && !technologyFrontier.isEmpty())
		{
			if (player != null)
			{
				return player.getTechnologyFrontierList().getAdvances();
			}
			else
			{
				return technologyFrontier.getTechs();
			}
		}
		final boolean isWW2V2 = games.strategy.triplea.Properties.getWW2V2(data);
		final boolean isWW2V3 = games.strategy.triplea.Properties.getWW2V3(data);
		if (isWW2V2)
			return s_WW2V2Advances;
		else if (isWW2V3)
			return s_WW2V3Advances;
		else
			return s_WW2V1Advances;
	}
	
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
		final TechnologyFrontier technologyFrontier;
		data.acquireReadLock();
		try
		{
			technologyFrontier = data.getTechnologyFrontier();
		} finally
		{
			data.releaseReadLock();
		}
		if (player != null && technologyFrontier != null && !technologyFrontier.isEmpty())
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
	private static final long serialVersionUID = -5469354766630425933L;
	
	public SuperSubsAdvance(final GameData data)
	{
		super("Super subs", data);
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
		return ta.getSuperSub();
	}
}


class HeavyBomberAdvance extends TechAdvance
{
	private static final long serialVersionUID = -1743063539572684675L;
	
	public HeavyBomberAdvance(final GameData data)
	{
		super("Heavy Bomber", data);
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
		return ta.getHeavyBomber();
	}
}


class IndustrialTechnologyAdvance extends TechAdvance
{
	private static final long serialVersionUID = -21252592806022090L;
	
	public IndustrialTechnologyAdvance(final GameData data)
	{
		super("Industrial Technology", data);
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
		return ta.getIndustrialTechnology();
	}
}


class JetPowerAdvance extends TechAdvance
{
	private static final long serialVersionUID = -9124162661008361132L;
	
	public JetPowerAdvance(final GameData data)
	{
		super("Jet Power", data);
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
		return ta.getJetPower();
	}
}


class RocketsAdvance extends TechAdvance
{
	private static final long serialVersionUID = 1526117896586201770L;
	
	public RocketsAdvance(final GameData data)
	{
		super("Rockets Advance", data);
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
		return ta.getRocket();
	}
}


class DestroyerBombardTechAdvance extends TechAdvance
{
	private static final long serialVersionUID = -4977423636387126617L;
	
	public DestroyerBombardTechAdvance(final GameData data)
	{
		super("Destroyer Bombard", data);
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
		return ta.getDestroyerBombard();
	}
}


class LongRangeAircraftAdvance extends TechAdvance
{
	private static final long serialVersionUID = 1986380888336238652L;
	
	public LongRangeAircraftAdvance(final GameData data)
	{
		super("Long Range Aircraft", data);
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
		return ta.getLongRangeAir();
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
	private static final long serialVersionUID = 3946378995070209879L;
	
	public ImprovedArtillerySupportAdvance(final GameData data)
	{
		super("Improved Artillery Support", data);
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
		return ta.getImprovedArtillerySupport();
	}
}


/*
 * Support paratroops
 */
class ParatroopersAdvance extends TechAdvance
{
	private static final long serialVersionUID = 1457384348499672184L;
	
	public ParatroopersAdvance(final GameData data)
	{
		super("Paratroopers", data);
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
		return ta.getParatroopers();
	}
}


/*
 * Increased Factory Production
 */
class IncreasedFactoryProductionAdvance extends TechAdvance
{
	private static final long serialVersionUID = 987606878563485763L;
	
	public IncreasedFactoryProductionAdvance(final GameData data)
	{
		super("Increased Factory Production", data);
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
		return ta.getIncreasedFactoryProduction();
	}
}


/*
 * War Bonds
 */
class WarBondsAdvance extends TechAdvance
{
	private static final long serialVersionUID = -9048146216351059811L;
	
	public WarBondsAdvance(final GameData data)
	{
		super("War Bonds", data);
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
		return ta.getWarBonds();
	}
}


/*
 * Mechanized Infantry
 */
class MechanizedInfantryAdvance extends TechAdvance
{
	private static final long serialVersionUID = 3040670614877450791L;
	
	public MechanizedInfantryAdvance(final GameData data)
	{
		super("Mechanized Infantry", data);
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
		return ta.getMechanizedInfantry();
	}
}


/*
 * AA Radar
 */
class AARadarAdvance extends TechAdvance
{
	private static final long serialVersionUID = 6464021231625252901L;
	
	public AARadarAdvance(final GameData data)
	{
		super("AA Radar", data);
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
		return ta.getAARadar();
	}
}


class ImprovedShipyardsAdvance extends TechAdvance
{
	private static final long serialVersionUID = 7613381831727736711L;
	
	public ImprovedShipyardsAdvance(final GameData data)
	{
		super("Shipyards", data);
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
		return ta.getShipyards();
	}
}
/**
 * End of AA 50 rules
 */
