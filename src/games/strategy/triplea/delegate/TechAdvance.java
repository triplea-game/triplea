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
import games.strategy.util.Tuple;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	
	@SuppressWarnings("rawtypes")
	private static final Class[] preDefinedTechConstructorParameter = new Class[] { GameData.class };
	
	public static final String TECH_NAME_SUPER_SUBS = "Super subs";
	public static final String TECH_PROPERTY_SUPER_SUBS = "superSub";
	public static final String TECH_NAME_JET_POWER = "Jet Power";
	public static final String TECH_PROPERTY_JET_POWER = "jetPower";
	public static final String TECH_NAME_IMPROVED_SHIPYARDS = "Shipyards";
	public static final String TECH_PROPERTY_IMPROVED_SHIPYARDS = "shipyards";
	public static final String TECH_NAME_AA_RADAR = "AA Radar";
	public static final String TECH_PROPERTY_AA_RADAR = "aARadar";
	public static final String TECH_NAME_LONG_RANGE_AIRCRAFT = "Long Range Aircraft";
	public static final String TECH_PROPERTY_LONG_RANGE_AIRCRAFT = "longRangeAir";
	public static final String TECH_NAME_HEAVY_BOMBER = "Heavy Bomber";
	public static final String TECH_PROPERTY_HEAVY_BOMBER = "heavyBomber";
	public static final String TECH_NAME_IMPROVED_ARTILLERY_SUPPORT = "Improved Artillery Support";
	public static final String TECH_PROPERTY_IMPROVED_ARTILLERY_SUPPORT = "improvedArtillerySupport";
	public static final String TECH_NAME_ROCKETS = "Rockets Advance";
	public static final String TECH_PROPERTY_ROCKETS = "rocket";
	public static final String TECH_NAME_PARATROOPERS = "Paratroopers";
	public static final String TECH_PROPERTY_PARATROOPERS = "paratroopers";
	public static final String TECH_NAME_INCREASED_FACTORY_PRODUCTION = "Increased Factory Production";
	public static final String TECH_PROPERTY_INCREASED_FACTORY_PRODUCTION = "increasedFactoryProduction";
	public static final String TECH_NAME_WAR_BONDS = "War Bonds";
	public static final String TECH_PROPERTY_WAR_BONDS = "warBonds";
	public static final String TECH_NAME_MECHANIZED_INFANTRY = "Mechanized Infantry";
	public static final String TECH_PROPERTY_MECHANIZED_INFANTRY = "mechanizedInfantry";
	public static final String TECH_NAME_INDUSTRIAL_TECHNOLOGY = "Industrial Technology";
	public static final String TECH_PROPERTY_INDUSTRIAL_TECHNOLOGY = "industrialTechnology";
	public static final String TECH_NAME_DESTROYER_BOMBARD = "Destroyer Bombard";
	public static final String TECH_PROPERTY_DESTROYER_BOMBARD = "destroyerBombard";
	
	public static final List<String> s_allPreDefinedTechnologyNames = Collections.unmodifiableList(Arrays.asList(
				TECH_NAME_SUPER_SUBS,
				TECH_NAME_JET_POWER,
				TECH_NAME_IMPROVED_SHIPYARDS,
				TECH_NAME_AA_RADAR,
				TECH_NAME_LONG_RANGE_AIRCRAFT,
				TECH_NAME_HEAVY_BOMBER,
				TECH_NAME_IMPROVED_ARTILLERY_SUPPORT,
				TECH_NAME_ROCKETS,
				TECH_NAME_PARATROOPERS,
				TECH_NAME_INCREASED_FACTORY_PRODUCTION,
				TECH_NAME_WAR_BONDS,
				TECH_NAME_MECHANIZED_INFANTRY,
				TECH_NAME_INDUSTRIAL_TECHNOLOGY,
				TECH_NAME_DESTROYER_BOMBARD));
	
	private static final Map<String, Class<? extends TechAdvance>> s_allPreDefinedTechnologies = createPreDefinedTechnologyMap();
	
	private static final Map<String, Class<? extends TechAdvance>> createPreDefinedTechnologyMap()
	{
		final HashMap<String, Class<? extends TechAdvance>> preDefinedTechMap = new HashMap<String, Class<? extends TechAdvance>>();
		preDefinedTechMap.put(TECH_PROPERTY_SUPER_SUBS, SuperSubsAdvance.class);
		preDefinedTechMap.put(TECH_PROPERTY_JET_POWER, JetPowerAdvance.class);
		preDefinedTechMap.put(TECH_PROPERTY_IMPROVED_SHIPYARDS, ImprovedShipyardsAdvance.class);
		preDefinedTechMap.put(TECH_PROPERTY_AA_RADAR, AARadarAdvance.class);
		preDefinedTechMap.put(TECH_PROPERTY_LONG_RANGE_AIRCRAFT, LongRangeAircraftAdvance.class);
		preDefinedTechMap.put(TECH_PROPERTY_HEAVY_BOMBER, HeavyBomberAdvance.class);
		preDefinedTechMap.put(TECH_PROPERTY_IMPROVED_ARTILLERY_SUPPORT, ImprovedArtillerySupportAdvance.class);
		preDefinedTechMap.put(TECH_PROPERTY_ROCKETS, RocketsAdvance.class);
		preDefinedTechMap.put(TECH_PROPERTY_PARATROOPERS, ParatroopersAdvance.class);
		preDefinedTechMap.put(TECH_PROPERTY_INCREASED_FACTORY_PRODUCTION, IncreasedFactoryProductionAdvance.class);
		preDefinedTechMap.put(TECH_PROPERTY_WAR_BONDS, WarBondsAdvance.class);
		preDefinedTechMap.put(TECH_PROPERTY_MECHANIZED_INFANTRY, MechanizedInfantryAdvance.class);
		preDefinedTechMap.put(TECH_PROPERTY_INDUSTRIAL_TECHNOLOGY, IndustrialTechnologyAdvance.class);
		preDefinedTechMap.put(TECH_PROPERTY_DESTROYER_BOMBARD, DestroyerBombardTechAdvance.class);
		return Collections.unmodifiableMap(preDefinedTechMap);
	}
	
	public TechAdvance(final String name, final GameData data)
	{
		super(name, data);
	}
	
	public abstract String getProperty();
	
	public abstract void perform(PlayerID id, IDelegateBridge bridge);
	
	public abstract boolean hasTech(TechAttachment ta);
	
	private static void createWW2V1Advances(final TechnologyFrontier tf)
	{
		tf.addAdvance(new JetPowerAdvance(tf.getData()));
		tf.addAdvance(new SuperSubsAdvance(tf.getData()));
		tf.addAdvance(new LongRangeAircraftAdvance(tf.getData()));
		tf.addAdvance(new RocketsAdvance(tf.getData()));
		tf.addAdvance(new IndustrialTechnologyAdvance(tf.getData()));
		tf.addAdvance(new HeavyBomberAdvance(tf.getData()));
	}
	
	private static void createWW2V2Advances(final TechnologyFrontier tf)
	{
		tf.addAdvance(new JetPowerAdvance(tf.getData()));
		tf.addAdvance(new SuperSubsAdvance(tf.getData()));
		tf.addAdvance(new LongRangeAircraftAdvance(tf.getData()));
		tf.addAdvance(new RocketsAdvance(tf.getData()));
		tf.addAdvance(new DestroyerBombardTechAdvance(tf.getData()));
		tf.addAdvance(new HeavyBomberAdvance(tf.getData()));
		// tf.addAdvance(new IndustrialTechnologyAdvance(tf.getData()));
	}
	
	private static void createWW2V3Advances(final TechnologyFrontier tf)
	{
		tf.addAdvance(new SuperSubsAdvance(tf.getData()));
		tf.addAdvance(new JetPowerAdvance(tf.getData()));
		tf.addAdvance(new ImprovedShipyardsAdvance(tf.getData()));
		tf.addAdvance(new AARadarAdvance(tf.getData()));
		tf.addAdvance(new LongRangeAircraftAdvance(tf.getData()));
		tf.addAdvance(new HeavyBomberAdvance(tf.getData()));
		tf.addAdvance(new ImprovedArtillerySupportAdvance(tf.getData()));
		tf.addAdvance(new RocketsAdvance(tf.getData()));
		tf.addAdvance(new ParatroopersAdvance(tf.getData()));
		tf.addAdvance(new IncreasedFactoryProductionAdvance(tf.getData()));
		tf.addAdvance(new WarBondsAdvance(tf.getData()));
		tf.addAdvance(new MechanizedInfantryAdvance(tf.getData()));
	}
	
	/**
	 * For the game parser only.
	 * 
	 * @param data
	 */
	public static void createDefaultTechAdvances(final GameData data)
	{
		final TechnologyFrontier tf = data.getTechnologyFrontier();
		final boolean ww2v2 = games.strategy.triplea.Properties.getWW2V2(data);
		final boolean ww2v3 = games.strategy.triplea.Properties.getWW2V3(data);
		if (ww2v2)
			createWW2V2Advances(tf);
		else if (ww2v3)
			createWW2V3Advances(tf);
		else
			createWW2V1Advances(tf);
		
		// now create player tech frontiers
		final List<TechnologyFrontier> frontiers = new ArrayList<TechnologyFrontier>();
		if (ww2v3)
		{
			final TechnologyFrontier an = new TechnologyFrontier("Air and Naval Advances", data);
			final TechnologyFrontier lp = new TechnologyFrontier("Land and Production Advances", data);
			final Tuple<List<TechAdvance>, List<TechAdvance>> ww2v3advances = getWW2v3CategoriesWithTheirAdvances(data);
			an.addAdvance(ww2v3advances.getFirst());
			lp.addAdvance(ww2v3advances.getSecond());
			frontiers.add(an);
			frontiers.add(lp);
		}
		else
		{
			final TechnologyFrontier tas = new TechnologyFrontier("Technology Advances", data);
			tas.addAdvance(new ArrayList<TechAdvance>(tf.getTechs()));
			frontiers.add(tas);
		}
		// add the frontiers
		for (final PlayerID player : data.getPlayerList().getPlayers())
		{
			for (final TechnologyFrontier frontier : frontiers)
			{
				player.getTechnologyFrontierList().addTechnologyFrontier(new TechnologyFrontier(frontier));
			}
		}
	}
	
	public static TechAdvance findDefinedAdvanceAndCreateAdvance(final String s, final GameData data)
	{
		final Class<? extends TechAdvance> clazz = s_allPreDefinedTechnologies.get(s);
		if (clazz == null)
			throw new IllegalArgumentException(s + " is not a valid technology");
		final TechAdvance ta;
		Constructor<? extends TechAdvance> constructor;
		try
		{
			constructor = clazz.getConstructor(preDefinedTechConstructorParameter);
			ta = constructor.newInstance(data);
		} catch (final Exception e)
		{
			e.printStackTrace();
			throw new IllegalStateException(s + " is not a valid technology or could not be instantiated");
		}
		if (ta == null)
			throw new IllegalStateException(s + " is not a valid technology or could not be instantiated");
		return ta;
	}
	
	public static TechAdvance findAdvance(final String propertyString, final GameData data, final PlayerID player)
	{
		for (final TechAdvance t : getTechAdvances(data, player))
		{
			if (t.getProperty().equals(propertyString))
				return t;
		}
		throw new IllegalArgumentException(propertyString + " is not a valid technology");
	}
	
	public static TechAdvance findTechnologyFromAllTechs(final String name, final GameData data, final boolean mustFind)
	{
		TechAdvance type;
		data.acquireReadLock();
		try
		{
			type = data.getTechnologyFrontier().getAdvanceByName(name);
			if (type == null)
				type = data.getTechnologyFrontier().getAdvanceByProperty(name);
			if (type == null && mustFind)
				throw new IllegalStateException("Could not find technology. name:" + name);
		} finally
		{
			data.releaseReadLock();
		}
		return type;
	}
	
	/**
	 * @param data
	 * @return first is air&naval, second is land&production
	 */
	public static Tuple<List<TechAdvance>, List<TechAdvance>> getWW2v3CategoriesWithTheirAdvances(final GameData data)
	{
		List<TechAdvance> allAdvances;
		data.acquireReadLock();
		try
		{
			allAdvances = new ArrayList<TechAdvance>(data.getTechnologyFrontier().getTechs());
		} finally
		{
			data.releaseReadLock();
		}
		final List<TechAdvance> airAndNaval = new ArrayList<TechAdvance>();
		final List<TechAdvance> landAndProduction = new ArrayList<TechAdvance>();
		for (final TechAdvance ta : allAdvances)
		{
			final String propertyString = ta.getProperty();
			if (propertyString.equals(TECH_PROPERTY_SUPER_SUBS) || propertyString.equals(TECH_PROPERTY_JET_POWER) || propertyString.equals(TECH_PROPERTY_IMPROVED_SHIPYARDS)
						|| propertyString.equals(TECH_PROPERTY_AA_RADAR) || propertyString.equals(TECH_PROPERTY_LONG_RANGE_AIRCRAFT) || propertyString.equals(TECH_PROPERTY_HEAVY_BOMBER))
				airAndNaval.add(ta);
			else if (propertyString.equals(TECH_PROPERTY_IMPROVED_ARTILLERY_SUPPORT) || propertyString.equals(TECH_PROPERTY_ROCKETS) || propertyString.equals(TECH_PROPERTY_PARATROOPERS)
						|| propertyString.equals(TECH_PROPERTY_INCREASED_FACTORY_PRODUCTION) || propertyString.equals(TECH_PROPERTY_WAR_BONDS)
						|| propertyString.equals(TECH_PROPERTY_MECHANIZED_INFANTRY))
				landAndProduction.add(ta);
			else
				throw new IllegalStateException("We should not be using ww2v3 categories if we have custom techs: " + propertyString);
		}
		return new Tuple<List<TechAdvance>, List<TechAdvance>>(airAndNaval, landAndProduction);
	}
	
	/**
	 * Returns all tech advances possible in this game.
	 * 
	 * @param data
	 * @return
	 */
	public static List<TechAdvance> getTechAdvances(final GameData data)
	{
		return getTechAdvances(data, null);
	}
	
	/**
	 * Returns all tech advances that this player can possibly research. (Or if Player is null, returns all techs available in the game).
	 * 
	 * @param data
	 * @param player
	 * @return
	 */
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
		// the game has no techs, just return empty list
		// System.out.println("No Techs");
		return new ArrayList<TechAdvance>();
	}
	
	/**
	 * Returns all possible tech categories for this player.
	 * 
	 * @param data
	 * @param player
	 * @return
	 */
	public static List<TechnologyFrontier> getPlayerTechCategories(final GameData data, final PlayerID player)
	{
		/*final TechnologyFrontier technologyFrontier;
		data.acquireReadLock();
		try
		{
			technologyFrontier = data.getTechnologyFrontier();
		} finally
		{
			data.releaseReadLock();
		}*/
		if (player != null) // && technologyFrontier != null && !technologyFrontier.isEmpty())
			return player.getTechnologyFrontierList().getFrontiers();
		throw new IllegalStateException("Player can not be null");
		/*
		System.out.println("Creating default tech categories");
		final List<TechnologyFrontier> tf = new ArrayList<TechnologyFrontier>();
		final TechnologyFrontier an = new TechnologyFrontier("Air and Naval Advances", data);
		final TechnologyFrontier lp = new TechnologyFrontier("Land and Production Advances", data);
		final Tuple<List<TechAdvance>, List<TechAdvance>> ww2v3advances = getWW2v3CategoriesWithTheirAdvances(data);
		an.addAdvance(ww2v3advances.getFirst());
		lp.addAdvance(ww2v3advances.getSecond());
		tf.add(an);
		tf.add(lp);
		return tf;*/
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if (o == null || !(o instanceof TechAdvance))
			return false;
		final TechAdvance other = (TechAdvance) o;
		if (other.getName() == null || getName() == null)
			return false;
		return getName().equals(other.getName());
	}
	
	@Override
	public int hashCode()
	{
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
		super(TECH_NAME_SUPER_SUBS, data);
	}
	
	@Override
	public String getProperty()
	{
		return TECH_PROPERTY_SUPER_SUBS;
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
		super(TECH_NAME_HEAVY_BOMBER, data);
	}
	
	@Override
	public String getProperty()
	{
		return TECH_PROPERTY_HEAVY_BOMBER;
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
		super(TECH_NAME_INDUSTRIAL_TECHNOLOGY, data);
	}
	
	@Override
	public String getProperty()
	{
		return TECH_PROPERTY_INDUSTRIAL_TECHNOLOGY;
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
		super(TECH_NAME_JET_POWER, data);
	}
	
	@Override
	public String getProperty()
	{
		return TECH_PROPERTY_JET_POWER;
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
		super(TECH_NAME_ROCKETS, data);
	}
	
	@Override
	public String getProperty()
	{
		return TECH_PROPERTY_ROCKETS;
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
		super(TECH_NAME_DESTROYER_BOMBARD, data);
	}
	
	@Override
	public String getProperty()
	{
		return TECH_PROPERTY_DESTROYER_BOMBARD;
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
		super(TECH_NAME_LONG_RANGE_AIRCRAFT, data);
	}
	
	@Override
	public String getProperty()
	{
		return TECH_PROPERTY_LONG_RANGE_AIRCRAFT;
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


// Beginning of AA 50 rules

/*
 * Artillery can support multiple infantry
 */
class ImprovedArtillerySupportAdvance extends TechAdvance
{
	private static final long serialVersionUID = 3946378995070209879L;
	
	public ImprovedArtillerySupportAdvance(final GameData data)
	{
		super(TECH_NAME_IMPROVED_ARTILLERY_SUPPORT, data);
	}
	
	@Override
	public String getProperty()
	{
		return TECH_PROPERTY_IMPROVED_ARTILLERY_SUPPORT;
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
		super(TECH_NAME_PARATROOPERS, data);
	}
	
	@Override
	public String getProperty()
	{
		return TECH_PROPERTY_PARATROOPERS;
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
		super(TECH_NAME_INCREASED_FACTORY_PRODUCTION, data);
	}
	
	@Override
	public String getProperty()
	{
		return TECH_PROPERTY_INCREASED_FACTORY_PRODUCTION;
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
		super(TECH_NAME_WAR_BONDS, data);
	}
	
	@Override
	public String getProperty()
	{
		return TECH_PROPERTY_WAR_BONDS;
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
		super(TECH_NAME_MECHANIZED_INFANTRY, data);
	}
	
	@Override
	public String getProperty()
	{
		return TECH_PROPERTY_MECHANIZED_INFANTRY;
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
		super(TECH_NAME_AA_RADAR, data);
	}
	
	@Override
	public String getProperty()
	{
		return TECH_PROPERTY_AA_RADAR;
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
		super(TECH_NAME_IMPROVED_SHIPYARDS, data);
	}
	
	@Override
	public String getProperty()
	{
		return TECH_PROPERTY_IMPROVED_SHIPYARDS;
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
// End of AA 50 rules
