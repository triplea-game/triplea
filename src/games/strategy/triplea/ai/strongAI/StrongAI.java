package games.strategy.triplea.ai.strongAI;

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
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.ai.AbstractAI;
import games.strategy.triplea.ai.Dynamix_AI.DUtils;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.AirMovementValidator;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.BattleDelegate;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.dataObjects.BattleListing;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IBattleDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;
import games.strategy.util.Tuple;
import games.strategy.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

/*
 * 
 * A stronger AI, based on some additional complexity, used the weak AI as a blueprint.<p>
 * This still needs work. Known Issues:
 * 1) *fixed* if a group of fighters on water needs 1 to land on island, it doesn't...works correctly if there is only 1 fighter
 * 2) *fixed* if Germany is winning, it doesn't switch to buying up transports
 * 3) *fixed* if a transport is at an owned territory with a factory, it won't leave unless it has units
 * 4) *fixed* Germany doesn't guard the shoreline well
 * 5) Ships are moving 1 territory too close to a large pack of ships (better analysis)
 * 6) *fixed* Ships make themselves vulnerable to plane attack
 * 7) *partial* No submerging or retreating has been implemented
 * 8) *fixed* Planes still occasionally stop in an unoccupied territory which is open to invasion with cheap units
 * 9) Need to analyze 1 territory further and delay attack if it brings a set of units under overwhelming odds
 * 10) *fixed* Units are still loaded onto transports from Southern Europe/Western US even when enemy forces are in neighboring terr
 * 11) *fixed* Still putting transport on a factory that is closest to enemy cap
 * 12) *fixed* Transports should scope out available units and go to them
 * 13) *fixed* When allied forces are nearby, occasionally attacks are with deficient forces
 * 14) *fixed* Occasionally air is attacking large units without accompanying land forces
 * 15) *fixed* AI needs to protect countries nearest its capital
 * 16) *fixed* AI allows England to be invaded because purchases that focus on navy are not buying any infantry
 * 17) *fixed* AI needs to keep fighters home when attackers are near the capital
 * 
 * 
 * @author Kevin Moore
 * 2008-2009
 */
@SuppressWarnings("deprecation")
public class StrongAI extends AbstractAI implements IGamePlayer, ITripleaPlayer
{
	// Amphib Route will hold the water Terr, Land Terr combination for unloading units
	private final static Logger s_logger = Logger.getLogger(StrongAI.class.getName());
	private Territory m_factTerr = null, m_seaTerr = null; // determine the target Territory during Purchase and Save it
	// private final Territory m_battleTerr = null;
	private Territory m_myCapital = null;
	// private List<Territory> m_alliedTerrs = new ArrayList<Territory>(), m_transTerrs = new ArrayList<Territory>();
	private List<Territory> m_transportDropOffLocales = new ArrayList<Territory>();
	private final boolean m_AE = false;
	private boolean m_transports_may_die = true, m_cap_danger = false, m_natObjective = false;
	private boolean m_bought_Attack_Ships = false, m_keep_Ships_At_Base = false, m_bought_Transports = false;
	private boolean m_onOffense = false;
	private HashMap<Territory, Territory> amphibMap = new HashMap<Territory, Territory>();
	private HashMap<Territory, Collection<Unit>> shipsMovedMap = new HashMap<Territory, Collection<Unit>>();
	private final Collection<Territory> m_seaTerrAttacked = new ArrayList<Territory>();
	private final Collection<Territory> m_landTerrAttacked = new ArrayList<Territory>();
	private final Collection<Territory> m_impassableTerrs = new ArrayList<Territory>();
	
	/** Creates new TripleAPlayer */
	public StrongAI(final String name, final String type)
	{
		super(name, type);
	}
	
	private void set_onOffense(final boolean value)
	{
		m_onOffense = value;
	}
	
	private boolean get_onOffense()
	{
		return m_onOffense;
	}
	
	private void getEdition()
	{
		final GameData data = getPlayerBridge().getGameData();
		// m_AE = games.strategy.triplea.Properties.getWW2V3(data);
		m_transports_may_die = !games.strategy.triplea.Properties.getTransportCasualtiesRestricted(data);
		m_natObjective = games.strategy.triplea.Properties.getNationalObjectives(data);
	}
	
	private void setImpassableTerrs(final PlayerID player)
	{
		final GameData data = getPlayerBridge().getGameData();
		m_impassableTerrs.clear();
		for (final Territory t : data.getMap().getTerritories())
		{
			if (Matches.TerritoryIsPassableAndNotRestricted(player, data).invert().match(t) && Matches.TerritoryIsLand.match(t))
				m_impassableTerrs.add(t);
		}
	}
	
	private Collection<Territory> getImpassableTerrs()
	{
		return m_impassableTerrs;
	}
	
	private boolean transportsMayDieFirst()
	{
		return m_transports_may_die;
	}
	
	private void setAttackShipPurchase(final boolean doBuyAttackShip)
	{
		m_bought_Attack_Ships = doBuyAttackShip;
	}
	
	private boolean getAttackShipPurchase()
	{
		return m_bought_Attack_Ships;
	}
	
	private void setDidPurchaseTransports(final boolean didBuyTransports)
	{
		m_bought_Transports = didBuyTransports;
	}
	
	@SuppressWarnings("unused")
	private boolean getDidPurchaseTransports()
	{
		return m_bought_Transports;
	}
	
	private void setTransportDropOffLocales(final List<Territory> transportDropOffLocales)
	{
		m_transportDropOffLocales = transportDropOffLocales;
	}
	
	private List<Territory> getTransportDropOffLocales()
	{
		return m_transportDropOffLocales;
	}
	
	private void setSeaTerrAttacked(final Collection<Territory> seaTerr)
	{
		m_seaTerrAttacked.addAll(seaTerr);
	}
	
	private void clearSeaTerrAttacked()
	{
		m_seaTerrAttacked.clear();
	}
	
	private List<Territory> getSeaTerrAttacked()
	{
		final List<Territory> seaTerr = new ArrayList<Territory>(m_seaTerrAttacked);
		return seaTerr;
	}
	
	private void setLandTerrAttacked(final Collection<Territory> landTerr)
	{
		m_landTerrAttacked.addAll(landTerr);
	}
	
	private void clearLandTerrAttacked()
	{
		m_landTerrAttacked.clear();
	}
	
	private List<Territory> getLandTerrAttacked()
	{
		final List<Territory> landTerr = new ArrayList<Territory>(m_landTerrAttacked);
		return landTerr;
	}
	
	private void setSeaTerr(final Territory seaTerr)
	{
		m_seaTerr = seaTerr;
	}
	
	private Territory getSeaTerr()
	{
		return m_seaTerr;
	}
	
	@SuppressWarnings("unused")
	private void setKeepShipsAtBase(final boolean keep)
	{
		m_keep_Ships_At_Base = keep;
	}
	
	private boolean getKeepShipsAtBase()
	{
		return m_keep_Ships_At_Base;
	}
	
	private boolean useProductionData()
	{
		return m_AE;
	}
	
	private void setFactory(final Territory t)
	{
		m_factTerr = t;
	}
	
	private Territory getFactory()
	{
		return m_factTerr;
	}
	
	private HashMap<Territory, Territory> getAmphibMap()
	{
		return amphibMap;
	}
	
	private void setAmphibMap(final HashMap<Territory, Territory> xAmphibMap)
	{
		amphibMap = xAmphibMap;
	}
	
	private HashMap<Territory, Collection<Unit>> getShipsMovedMap()
	{
		return shipsMovedMap;
	}
	
	private void setShipsMovedMap(final HashMap<Territory, Collection<Unit>> xMovedMap)
	{
		shipsMovedMap = xMovedMap;
	}
	
	private void setCapDanger(final Boolean danger)
	{
		m_cap_danger = danger; // use this to track when determined the capital is in danger
	}
	
	private boolean getCapDanger()
	{
		return m_cap_danger;
	}
	
	@SuppressWarnings("unused")
	private boolean getNationalObjectives()
	{
		return m_natObjective;
	}
	
	@Override
	protected void tech(final ITechDelegate techDelegate, final GameData data, final PlayerID player)
	{
		if (!games.strategy.triplea.Properties.getWW2V3TechModel(data))
			return;
		long last, now;
		last = System.currentTimeMillis();
		s_logger.fine("Doing Tech ");
		final Territory myCapitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		final float eStrength = SUtils.getStrengthOfPotentialAttackers(myCapitol, data, player, false, true, null);
		float myStrength = SUtils.strength(myCapitol.getUnits().getUnits(), false, false, false);
		final List<Territory> areaStrength = SUtils.getNeighboringLandTerritories(data, player, myCapitol);
		for (final Territory areaTerr : areaStrength)
			myStrength += SUtils.strength(areaTerr.getUnits().getUnits(), false, false, false) * 0.75F;
		final boolean capDanger = myStrength < (eStrength * 1.25F + 3.0F);
		
		final Resource pus = data.getResourceList().getResource(Constants.PUS);
		final int PUs = player.getResources().getQuantity(pus);
		final Resource techtokens = data.getResourceList().getResource(Constants.TECH_TOKENS);
		final int TechTokens = player.getResources().getQuantity(techtokens);
		int TokensToBuy = 0;
		if (!capDanger && TechTokens < 3 && PUs > Math.random() * 160)
			TokensToBuy = 1;
		if (TechTokens > 0 || TokensToBuy > 0)
		{
			final List<TechnologyFrontier> cats = TechAdvance.getPlayerTechCategories(data, player);
			// retaining 65% chance of choosing land advances using basic ww2v3 model.
			if (data.getTechnologyFrontier().isEmpty())
			{
				if (Math.random() > 0.35)
					techDelegate.rollTech(TechTokens + TokensToBuy, cats.get(1), TokensToBuy, null);
				else
					techDelegate.rollTech(TechTokens + TokensToBuy, cats.get(0), TokensToBuy, null);
			}
			else
			{
				final int rand = (int) (Math.random() * cats.size());
				techDelegate.rollTech(TechTokens + TokensToBuy, cats.get(rand), TokensToBuy, null);
			}
		}
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
	}
	
	private Route getAmphibRoute(final PlayerID player, final boolean nonCombat)
	{
		if (!isAmphibAttack(player, false))
			return null;
		final GameData data = getPlayerBridge().getGameData();
		final Territory ourCapitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		final Match<Territory> endMatch = new Match<Territory>()
		{
			@Override
			public boolean match(final Territory o)
			{
				final boolean impassable = TerritoryAttachment.get(o) != null && TerritoryAttachment.get(o).getIsImpassible();
				boolean isLandableOn = false;
				if (nonCombat)
					isLandableOn = Matches.isTerritoryAllied(player, data).match(o); // We want to land for amphibious, so we want only friendly ters
				else
					isLandableOn = !Matches.isTerritoryAllied(player, data).match(o); // We want to attack, so we want only enemy ters
				return !impassable && !o.isWater() && SUtils.hasLandRouteToEnemyOwnedCapitol(o, player, data) && isLandableOn;
			}
		};
		final Match<Territory> routeCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data));
		final Route withNoEnemy = SUtils.findNearest(ourCapitol, endMatch, routeCond, data);
		if (withNoEnemy != null && withNoEnemy.getLength() > 0)
			return withNoEnemy;
		// this will fail if our capitol is not next to water, c'est la vie.
		final Route route = SUtils.findNearest(ourCapitol, endMatch, Matches.TerritoryIsWater, data); // If we couldn't find an enemy free ncm sea route, just check for any water route
		if (route == null || route.getLength() == 0)
			return null;
		return route;
	}
	
	private boolean isAmphibAttack(final PlayerID player, final boolean requireWaterFactory)
	{
		final GameData data = getPlayerBridge().getGameData();
		final Territory capitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, getPlayerBridge().getGameData());
		if (capitol == null || !capitol.getOwner().equals(player))
			return false;
		if (requireWaterFactory)
		{
			final List<Territory> factories = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitCanProduceUnits);
			final List<Territory> waterFactories = SUtils.stripLandLockedTerr(data, factories);
			if (waterFactories.isEmpty())
				return false;
		}
		// find a land route to an enemy territory from our capitol
		boolean amphibPlayer = !SUtils.hasLandRouteToEnemyOwnedCapitol(capitol, player, data);
		int totProduction = 0, allProduction = 0;
		if (amphibPlayer)
		{
			final List<Territory> allFactories = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitCanProduceUnits);
			// allFactories.remove(capitol);
			for (final Territory checkFactory : allFactories)
			{
				final boolean isLandRoute = SUtils.hasLandRouteToEnemyOwnedCapitol(checkFactory, player, data);
				final int factProduction = TerritoryAttachment.get(checkFactory).getProduction();
				allProduction += factProduction;
				if (isLandRoute)
					totProduction += factProduction;
			}
		}
		// if the land based production is greater than 2/5 (used to be 1/3) of all factory production, turn off amphib
		// works better on NWO where Brits start with factories in North Africa
		amphibPlayer = amphibPlayer ? (totProduction * 5 < allProduction * 2) : false;
		return amphibPlayer;
	}
	
	// determine danger to the capital and set the capdanger variable...other routines can just get the CapDanger var
	// should be called prior to combat, non-combat and purchasing units to assess the current situation
	private HashMap<Territory, Float> determineCapDanger(final PlayerID player, final GameData data)
	{
		final float threatFactor = 1.05F;
		float enemyStrength = 0.0F, ourStrength = 0.0F;
		boolean capDanger = false;
		final HashMap<Territory, Float> factMap = new HashMap<Territory, Float>();
		final Territory myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		if (myCapital == null)
			return factMap;
		final List<Territory> factories = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitCanProduceUnits);
		if (!factories.contains(myCapital))
			factories.add(myCapital);
		final boolean tFirst = transportsMayDieFirst();
		for (final Territory factory : factories)
		{
			enemyStrength = SUtils.getStrengthOfPotentialAttackers(myCapital, data, player, tFirst, false, null);
			factMap.put(factory, enemyStrength);
		}
		final float capPotential = factMap.get(myCapital);
		ourStrength = SUtils.strength(myCapital.getUnits().getUnits(), false, false, tFirst);
		ourStrength += 2.0F; // assume new units on the way...3 infantry ~= 8.1F
		if (capPotential > ourStrength * threatFactor) // we are in trouble
			capDanger = true;
		setCapDanger(capDanger); // save this for unit placement
		return factMap;
	}
	
	@Override
	protected void move(final boolean nonCombat, final IMoveDelegate moveDel, final GameData data, final PlayerID player)
	{
		if (nonCombat)
			doNonCombatMove(moveDel, player);
		else
			doCombatMove(moveDel, player);
		pause();
	}
	
	private void doNonCombatMove(final IMoveDelegate moveDel, final PlayerID player)
	{
		final GameData data = getPlayerBridge().getGameData();
		Long last, now;
		boolean foundOwnedUnits = false;
		for (final Territory ter : data.getMap().getTerritories())
		{
			if (ter.getUnits().someMatch(Matches.unitIsOwnedBy(player)))
			{
				foundOwnedUnits = true;
				break;
			}
		}
		if (!foundOwnedUnits)
			return; // If we don't own any units, just end right now
		/* Squid
		 * initialisting common data
		 */
		// m_alliedTerrs = SUtils.allAlliedTerritories(data, player);
		// m_transTerrs = SUtils.findCertainShips(data, player, Matches.UnitIsTransport);
		last = System.currentTimeMillis();
		final List<Collection<Unit>> moveUnits = new ArrayList<Collection<Unit>>();
		final List<Route> moveRoutes = new ArrayList<Route>();
		final List<Collection<Unit>> transportsToLoad = new ArrayList<Collection<Unit>>();
		s_logger.fine("Start NonCombat for: " + player.getName());
		s_logger.fine("populateTransportLoad");
		populateTransportLoad(true, data, moveUnits, moveRoutes, transportsToLoad, player);
		doMove(moveUnits, moveRoutes, transportsToLoad, moveDel);
		moveRoutes.clear();
		moveUnits.clear();
		transportsToLoad.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("protectOurAllies");
		protectOurAllies(true, data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveRoutes.clear();
		moveUnits.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("planesToCarriers");
		planesToCarriers(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveRoutes.clear();
		moveUnits.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("bomberNonComMove");
		bomberNonComMove(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveRoutes.clear();
		moveUnits.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		determineCapDanger(player, data);
		s_logger.fine("populateNonComTransportMove");
		populateNonComTransportMove(data, moveUnits, moveRoutes, player);
		// simulatedNonCombatTransportMove(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveRoutes.clear();
		moveUnits.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		// unload the transports that can be unloaded
		s_logger.fine("populateTransportUnLoadNonCom");
		populateTransportUnloadNonCom(true, data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveUnits.clear();
		moveRoutes.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		// check to see if we have missed any transports
		s_logger.fine("checkUnMovedTransports");
		checkUnmovedTransports(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveUnits.clear();
		moveRoutes.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("bringShipsToTransports");
		bringShipsToTransports(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveUnits.clear();
		moveRoutes.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("populateNonCombatSea");
		populateNonCombatSea(true, data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveUnits.clear();
		moveRoutes.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("stopBlitzAttack");
		stopBlitzAttack(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveUnits.clear();
		moveRoutes.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("populateNonCombat");
		populateNonCombat(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveUnits.clear();
		moveRoutes.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("movePlanesHomeNonCom");
		movePlanesHomeNonCom(moveUnits, moveRoutes, player, data); // combine this with checkPlanes at some point
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveUnits.clear();
		moveRoutes.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		// check to see if we have vulnerable planes
		s_logger.fine("CheckPlanes");
		CheckPlanes(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveUnits.clear();
		moveRoutes.clear();
		transportsToLoad.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("secondLookSea");
		secondLookSea(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveUnits.clear();
		moveRoutes.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("populateTransportLoad");
		populateTransportLoad(true, data, moveUnits, moveRoutes, transportsToLoad, player); // another pass on loading
		doMove(moveUnits, moveRoutes, transportsToLoad, moveDel);
		moveRoutes.clear();
		moveUnits.clear();
		transportsToLoad.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		// unload the transports that can be unloaded
		s_logger.fine("populateTransportUnloadNonCom");
		populateTransportUnloadNonCom(false, data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveUnits.clear();
		moveRoutes.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("nonCombatPlanes");
		nonCombatPlanes(data, player, moveUnits, moveRoutes);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveUnits.clear();
		moveRoutes.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("secondNonCombat");
		secondNonCombat(moveUnits, moveRoutes, player, data);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveUnits.clear();
		moveRoutes.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("populateFinalTransportUnload");
		populateFinalTransportUnload(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("Finished NonCombat for: " + player.getName());
		// In maps such as NWO, America as Moore never moves unit from center to the coast
		s_logger.fine("populateFinalMoveToCoast");
		populateFinalMoveToCoast(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveUnits.clear();
		moveRoutes.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		// In maps such as NWO, America as Moore leaves dozens of transports at the amphib destination, never coming back for units
		s_logger.fine("populateMoveUnusedTransportsToFillLocation");
		populateMoveUnusedTransportsToFillLocation(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveUnits.clear();
		moveRoutes.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
	}
	
	private void doCombatMove(final IMoveDelegate moveDel, final PlayerID player)
	{
		Long last, now;
		final GameData data = getPlayerBridge().getGameData();
		boolean foundOwnedUnits = false;
		for (final Territory ter : data.getMap().getTerritories())
		{
			if (ter.getUnits().getMatches(Matches.unitIsOwnedBy(player)).size() > 0)
			{
				foundOwnedUnits = true;
				break;
			}
		}
		if (!foundOwnedUnits)
			return; // If we don't own any units, just end right now
		getEdition();
		setImpassableTerrs(player);
		/* Squid
		 * initialising shared data
		 */
		// m_alliedTerrs = SUtils.allAlliedTerritories(data, player);
		// m_transTerrs = SUtils.findCertainShips(data, player, Matches.UnitIsTransport);
		m_myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		clearSeaTerrAttacked(); // clear out global vars
		clearLandTerrAttacked();
		final List<Collection<Unit>> moveUnits = new ArrayList<Collection<Unit>>();
		final List<Route> moveRoutes = new ArrayList<Route>();
		final List<Collection<Unit>> transportsToLoad = new ArrayList<Collection<Unit>>();
		determineCapDanger(player, data);
		s_logger.fine("Start Combat for: " + player.getName());
		last = System.currentTimeMillis();
		s_logger.fine("Defend Start and End of Transport Chain");
		defendTransportingLocations(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		if (!moveUnits.isEmpty() || !moveRoutes.isEmpty())
		{
			s_logger.finer("moving " + moveUnits);
			s_logger.finer("Route " + moveRoutes);
		}
		moveUnits.clear();
		moveRoutes.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("Sea Combat Move");
		// let sea battles occur before we load transports
		populateCombatMoveSea(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		if (!moveUnits.isEmpty() || !moveRoutes.isEmpty())
		{
			s_logger.finer("moving " + moveUnits);
			s_logger.finer("Route " + moveRoutes);
		}
		moveUnits.clear();
		moveRoutes.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("populateTransportLoad");
		populateTransportLoad(false, data, moveUnits, moveRoutes, transportsToLoad, player);
		doMove(moveUnits, moveRoutes, transportsToLoad, moveDel);
		if (!moveUnits.isEmpty() || !moveRoutes.isEmpty())
		{
			s_logger.finer("moving " + moveUnits);
			s_logger.finer("Route " + moveRoutes);
		}
		moveUnits.clear();
		moveRoutes.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("protectOurAllies");
		protectOurAllies(true, data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		if (!moveUnits.isEmpty() || !moveRoutes.isEmpty())
		{
			s_logger.finer("moving " + moveUnits);
			s_logger.finer("Route " + moveRoutes);
		}
		moveRoutes.clear();
		moveUnits.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("Special Transport Move");
		specialTransportMove(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		if (!moveUnits.isEmpty() || !moveRoutes.isEmpty())
		{
			s_logger.finer("moving " + moveUnits);
			s_logger.finer("Route " + moveRoutes);
		}
		moveRoutes.clear();
		moveUnits.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("Quick transport Unload");
		quickTransportUnload(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		if (!moveUnits.isEmpty() || !moveRoutes.isEmpty())
		{
			s_logger.finer("moving " + moveUnits);
			s_logger.finer("Route " + moveRoutes);
		}
		moveRoutes.clear();
		moveUnits.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("Amphib Map Unload");
		amphibMapUnload(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		if (!moveUnits.isEmpty() || !moveRoutes.isEmpty())
		{
			s_logger.finer("moving " + moveUnits);
			s_logger.finer("Route " + moveRoutes);
		}
		moveRoutes.clear();
		moveUnits.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("firstTransportMove"); // probably don't need this anymore
		firstTransportMove(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		if (!moveUnits.isEmpty() || !moveRoutes.isEmpty())
		{
			s_logger.finer("moving " + moveUnits);
			s_logger.finer("Route " + moveRoutes);
		}
		moveRoutes.clear();
		moveUnits.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("Populate Transport Move");
		populateTransportMove(data, moveUnits, moveRoutes, player);
		// simulatedTransportMove(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		if (!moveUnits.isEmpty() || !moveRoutes.isEmpty())
		{
			s_logger.finer("moving " + moveUnits);
			s_logger.finer("Route " + moveRoutes);
		}
		moveUnits.clear();
		moveRoutes.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("Amphib Map Unload");
		amphibMapUnload(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		if (!moveUnits.isEmpty() || !moveRoutes.isEmpty())
		{
			s_logger.finer("moving " + moveUnits);
			s_logger.finer("Route " + moveRoutes);
		}
		moveRoutes.clear();
		moveUnits.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		s_logger.fine("Populate Transport Unload");
		populateTransportUnload(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		if (!moveUnits.isEmpty() || !moveRoutes.isEmpty())
		{
			s_logger.finer("moving " + moveUnits);
			s_logger.finer("Route " + moveRoutes);
		}
		moveRoutes.clear();
		moveUnits.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		// find second amphib target
		/*        Route altRoute = getAlternativeAmphibRoute( player);
		        if(altRoute != null)
		           	moveCombatSea( data, moveUnits, moveRoutes, player, altRoute, 1);

		        doMove(moveUnits, moveRoutes, null, moveDel);
		        moveUnits.clear();
		        moveRoutes.clear();
		        transportsToLoad.clear();

		        //we want to move loaded transports before we try to fight our battles
		        populateNonCombatSea(false, data, moveUnits, moveRoutes, player);
				doMove(moveUnits, moveRoutes, null, moveDel);
		        moveRoutes.clear();
		        moveUnits.clear();
		*/
		s_logger.fine("Regular Combat Move");
		populateCombatMove(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		if (!moveUnits.isEmpty() || !moveRoutes.isEmpty())
		{
			s_logger.finer("moving " + moveUnits);
			s_logger.finer("Route " + moveRoutes);
		}
		moveUnits.clear();
		moveRoutes.clear();
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
		// any planes left for an overwhelming attack?
		s_logger.fine("Special Plane Attack");
		specialPlaneAttack(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		if (!moveUnits.isEmpty() || !moveRoutes.isEmpty())
		{
			s_logger.finer("moving " + moveUnits);
			s_logger.finer("Route " + moveRoutes);
		}
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		last = now;
	}
	
	@Override
	protected void battle(final IBattleDelegate battleDelegate, final GameData data, final PlayerID player)
	{
		// generally all AI's will follow the same logic.
		// loop until all battles are fought.
		// rather than try to analyze battles to figure out which must be fought before others
		// as in the case of a naval battle preceding an amphibious attack,
		// keep trying to fight every battle
		long now, start;
		start = System.currentTimeMillis();
		s_logger.fine("Doing Battles");
		while (true)
		{
			final BattleListing listing = battleDelegate.getBattles();
			// all fought
			if (listing.getBattles().isEmpty())
			{
				now = System.currentTimeMillis();
				s_logger.finest("Time Taken " + (now - start));
				return;
			}
			set_onOffense(true);
			for (final Entry<BattleType, Collection<Territory>> entry : listing.getBattles().entrySet())
			{
				for (final Territory current : entry.getValue())
				{
					final String error = battleDelegate.fightBattle(current, entry.getKey().isBombingRun(), entry.getKey());
					if (error != null)
						s_logger.fine(error);
				}
			}
			set_onOffense(false);
		}
	}
	
	private void planesToCarriers(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		final CompositeMatch<Unit> ownedAC = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsCarrier);
		// CompositeMatch<Unit> nonTransportSeaUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSea, Matches.UnitIsNotTransport);
		final CompositeMatch<Unit> fighterUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanLandOnCarrier);
		// CompositeMatch<Unit> seaAttackUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.UnitIsNotTransport, HasntMoved);
		// CompositeMatch<Unit> seaAirAttackUnitNotMoved = new CompositeMatchOr<Unit>(seaAttackUnit, fighterUnit);
		final CompositeMatch<Territory> noNeutralOrAA = new CompositeMatchAnd<Territory>(SUtils.TerritoryIsNotImpassableToAirUnits(data), Matches.territoryHasEnemyAAforCombatOnly(player, data)
					.invert());
		final List<Territory> ACTerrs = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitIsCarrier);
		final List<Territory> myFighterTerr = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitCanLandOnCarrier);
		final List<Unit> alreadyMoved = new ArrayList<Unit>();
		final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
		for (final Territory ACTerr : ACTerrs)
		{
			// are there fighters over water that we can help land?
			final List<Unit> ACUnits = ACTerr.getUnits().getMatches(ownedAC);
			int carrierSpace = 0;
			for (final Unit carrier1 : ACUnits)
			{
				carrierSpace += UnitAttachment.get(carrier1.getType()).getCarrierCapacity();
			}
			final List<Unit> ACFighters = ACTerr.getUnits().getMatches(fighterUnit);
			int fighterSpaceUsed = 0;
			for (final Unit fighter1 : ACFighters)
				fighterSpaceUsed += UnitAttachment.get(fighter1.getType()).getCarrierCost();
			final int availSpace = carrierSpace - fighterSpaceUsed;
			ACUnits.removeAll(alreadyMoved);
			if (ACUnits.size() == 0 || availSpace <= 0)
				continue;
			for (final Territory f : myFighterTerr)
			{
				if (f == ACTerr)
					continue;
				if (Matches.TerritoryIsLand.match(f) && SUtils.doesLandExistAt(f, data, false))
				{
					final Set<Territory> nextNeighbors = data.getMap().getNeighbors(f, Matches.territoryHasNoAlliedUnits(player, data).invert());
					for (final Territory nTerr : nextNeighbors)
					{
						final float strength = SUtils.strength(nTerr.getUnits().getUnits(), false, false, true);
						final float eStrength = SUtils.getStrengthOfPotentialAttackers(nTerr, data, player, true, true, null);
						if (strength > eStrength)
							continue;
					}
				}
				Territory fTerr = f;
				if (Matches.TerritoryIsLand.match(f) && delegate.getBattleTracker().wasBattleFought(f)) // might be an island
					fTerr = SUtils.getClosestWaterTerr(f, ACTerr, data, player, false);
				// Route fACRoute = SUtils.getMaxSeaRoute(data, ACTerr, fTerr, player, false);
				final Route fACRoute = data.getMap().getWaterRoute(ACTerr, fTerr);
				if (fACRoute == null)
					continue;
				final List<Unit> myFighters = f.getUnits().getMatches(fighterUnit);
				myFighters.removeAll(alreadyMoved);
				if (myFighters.isEmpty())
					continue;
				final IntegerMap<Unit> fighterMoveMap = new IntegerMap<Unit>();
				for (final Unit f1 : myFighters)
				{
					fighterMoveMap.put(f1, TripleAUnit.get(f1).getMovementLeft());
				}
				SUtils.reorder(myFighters, fighterMoveMap, false);
				final int fACDist = fACRoute.getLength();
				final int fightMove = MoveValidator.getMaxMovement(myFighters);
				if (AirMovementValidator.canLand(myFighters, ACTerr, player, data))
				{
					final Route fACRoute2 = data.getMap().getRoute(f, ACTerr, noNeutralOrAA);
					if (fACRoute2 != null && (fACRoute2.getLength() <= fightMove))
					{
						moveUnits.add(myFighters);
						moveRoutes.add(fACRoute2);
						alreadyMoved.addAll(myFighters);
						alreadyMoved.addAll(ACUnits);
					}
					continue;
				}
				final int ACMove = MoveValidator.getLeastMovement(ACUnits);
				final Route fACRoute2 = SUtils.getMaxSeaRoute(data, ACTerr, fTerr, player, false, ACMove);
				if (fACRoute2 == null || fACRoute2.getEnd() == null)
					continue;
				final Territory targetTerr = fACRoute2.getEnd();
				final Route fighterRoute = data.getMap().getRoute(f, targetTerr, noNeutralOrAA);
				if (fighterRoute == null)
					continue;
				if (fACDist <= (fightMove + ACMove)) // move carriers and fighters
				{
					final Iterator<Unit> fighterIter = myFighters.iterator();
					final List<Unit> fighters = new ArrayList<Unit>();
					while (fighterIter.hasNext())
					{
						final Unit fighter = fighterIter.next();
						if (MoveValidator.hasEnoughMovement(fighter, fighterRoute))
							fighters.add(fighter);
					}
					if (fighters.size() > 0)
					{
						moveUnits.add(myFighters);
						moveRoutes.add(fighterRoute);
						alreadyMoved.addAll(myFighters);
						moveUnits.add(ACUnits);
						moveRoutes.add(fACRoute2);
						alreadyMoved.addAll(ACUnits);
					}
				}
			}
		}
	}
	
	private void checkUnmovedTransports(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		// setImpassableTerrs(player);
		// Collection<Territory> impassableTerrs = getImpassableTerrs();
		// TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		// Simply: Move Transports Back Toward a Factory
		final CompositeMatch<Unit> transUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport);
		final CompositeMatch<Unit> ourTransUnit = new CompositeMatchAnd<Unit>(transUnit, Matches.unitIsOwnedBy(player), Matches.transportIsNotTransporting(), Matches.unitHasNotMoved);
		final List<Territory> transTerr = SUtils.findTersWithUnitsMatching(data, player, ourTransUnit);
		// CompositeMatch<Unit> ourTransUnit2 = new CompositeMatchAnd<Unit>(transUnit, Matches.unitIsOwnedBy(player), Matches.transportIsTransporting());
		final List<Territory> ourFactories = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitCanProduceUnits);
		final List<Territory> ourSeaSpots = new ArrayList<Territory>();
		final CompositeMatch<Unit> ourLandUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand, Matches.UnitIsNotInfrastructure);
		// CompositeMatch<Unit> escortUnit = new CompositeMatchOr<Unit>(Matches.UnitIsSea, Matches.alliedUnit(player, data), Matches.UnitIsNotTransport);
		// CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsAir);
		// CompositeMatch<Unit> escortAirUnit = new CompositeMatchOr<Unit>(escortUnit, airUnit);
		// CompositeMatch<Territory> enemyLand = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotNeutral, Matches.territoryHasEnemyUnits(player, data), Matches.TerritoryHasProductionValueAtLeast(1));
		// CompositeMatch<Territory> noenemyWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data));
		// List<Territory> unmovedTransportTerrs = SUtils.findCertainShips(data, player, ourTransUnit2);
		if (transTerr.isEmpty())
			return;
		final List<Unit> alreadyMoved = new ArrayList<Unit>();
		final List<Territory> ourEnemyTerr = new ArrayList<Territory>();
		final List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
		final List<Territory> moveToTerr = new ArrayList<Territory>();
		moveToTerr.addAll(ourEnemyTerr);
		moveToTerr.addAll(ourFriendlyTerr);
		// final IntegerMap<Territory> ourFacts = new IntegerMap<Territory>();
		final HashMap<Territory, Territory> connectTerr = new HashMap<Territory, Territory>();
		for (final Territory xT : ourFactories)
		{
			// final int numUnits = xT.getUnits().getMatches(ourLandUnit).size() + TerritoryAttachment.get(xT).getProduction();
			final Set<Territory> factNeighbors = data.getMap().getNeighbors(xT, Matches.TerritoryIsWater);
			// ourFacts.put(xT, numUnits);
			for (final Territory factTest : factNeighbors)
				connectTerr.put(factTest, xT);
			ourSeaSpots.addAll(factNeighbors);
		}
		// check for locations with units and no threat or real way to use them
		final List<Territory> allTerr = SUtils.allAlliedTerritories(data, player);
		allTerr.removeAll(ourFactories);
		final List<Territory> otherNonSeaSpots = new ArrayList<Territory>();
		for (final Territory xT2 : allTerr)
		{
			final boolean hasWater = SUtils.isWaterAt(xT2, data);
			final Route nearestRoute = SUtils.findNearest(xT2, Matches.territoryHasEnemyUnits(player, data), Matches.isTerritoryAllied(player, data), data);
			if (hasWater && (nearestRoute == null || nearestRoute.getLength() > 4 || nearestRoute.crossesWater())) // bad guys are far away...
				otherNonSeaSpots.add(xT2);
		}
		// int minDist = 100;
		Territory closestT = null;
		for (final Territory t : transTerr)
		{
			final List<Unit> ourTransports = t.getUnits().getMatches(ourTransUnit);
			ourTransports.removeAll(alreadyMoved);
			if (ourTransports.isEmpty())
				continue;
			final int maxTransDistance = MoveValidator.getLeastMovement(ourTransports);
			final IntegerMap<Territory> distMap = new IntegerMap<Territory>();
			final IntegerMap<Territory> unitsMap = new IntegerMap<Territory>();
			if (!ourSeaSpots.contains(t))
			{
				for (final Territory t2 : ourSeaSpots)
				{
					final Route thisRoute = SUtils.getMaxSeaRoute(data, t, t2, player, false, maxTransDistance);
					int thisDist = 0;
					if (thisRoute == null)
						thisDist = 100;
					else
						thisDist = thisRoute.getLength();
					final int numUnits = t2.getUnits().getMatches(ourLandUnit).size() + 6; // assume new units on the factory
					distMap.put(t2, thisDist);
					unitsMap.put(t2, numUnits);
				}
			}
			for (final Territory checkAnother : otherNonSeaSpots)
			{
				int thisDist = 0;
				final int numUnits = checkAnother.getUnits().getMatches(ourLandUnit).size();
				final Territory qTerr = SUtils.getClosestWaterTerr(checkAnother, t, data, player, false);
				thisDist = data.getMap().getWaterDistance(t, qTerr);
				if (thisDist == -1)
					thisDist = 100;
				connectTerr.put(qTerr, checkAnother);
				distMap.put(qTerr, thisDist);
				unitsMap.put(qTerr, numUnits);
			}
			final Set<Territory> allWaterTerr = distMap.keySet();
			int score = 0, bestScore = 0;
			for (final Territory waterTerr : allWaterTerr)
			{
				// figure out the best Territory to send it to
				score = unitsMap.getInt(waterTerr) - distMap.getInt(waterTerr);
				final int numTrans = waterTerr.getUnits().getMatches(ourTransUnit).size();
				if (waterTerr == t)
				{
					final Territory landTerr = connectTerr.get(t);
					final int moveNum = (numTrans * 2 - unitsMap.getInt(waterTerr)) / 2;
					int score2 = 0, bestScore2 = 0;
					Territory newTerr = null;
					for (final Territory waterTerr2 : allWaterTerr)
					{
						if (waterTerr2 == waterTerr || landTerr == connectTerr.get(waterTerr2))
							continue;
						score2 = unitsMap.getInt(waterTerr2) - distMap.getInt(waterTerr2);
						if (score2 > bestScore2)
						{
							bestScore2 = score2;
							newTerr = waterTerr2;
						}
					}
					final Route goRoute = SUtils.getMaxSeaRoute(data, t, newTerr, player, false, maxTransDistance);
					if (goRoute == null)
						continue;
					final List<Unit> tmpTrans = new ArrayList<Unit>();
					for (int i = 0; i < Math.min(moveNum, numTrans); i++)
					{
						final Unit trans2 = ourTransports.get(i);
						tmpTrans.add(trans2);
					}
					if (moveNum > 0 && !tmpTrans.isEmpty())
					{
						ourTransports.removeAll(tmpTrans);
						moveUnits.add(tmpTrans);
						moveRoutes.add(goRoute);
						alreadyMoved.addAll(tmpTrans);
					}
					continue;
				}
				if (score > bestScore)
				{
					bestScore = score;
					closestT = waterTerr;
				}
			}
			if (closestT != null && t != closestT && !ourTransports.isEmpty())
			{
				final int maxTDist = MoveValidator.getLeastMovement(ourTransports);
				final Route ourRoute = SUtils.getMaxSeaRoute(data, t, closestT, player, false, maxTDist);
				/*				List<Unit> escortUnits = t.getUnits().getMatches(escortUnit);
								if (escortUnits.isEmpty())
									continue;
								int maxEscortDistance = MoveValidator.getLeastMovement(escortUnits);
								escortUnits.removeAll(alreadyMoved);
								if (escortUnits.size() > 0)
								{
									moveUnits.add(escortUnits);
									moveRoutes.add(ourRoute);
									alreadyMoved.addAll(escortUnits);
								}
				*/
				moveUnits.add(ourTransports);
				moveRoutes.add(ourRoute);
			}
			// minDist = 100;
			closestT = null;
		}
	}
	
	private void populateTransportLoad(final boolean nonCombat, final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes,
				final List<Collection<Unit>> transportsToLoad, final PlayerID player)
	{
		final TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		final CompositeMatch<Unit> owned = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
		final CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(owned, Matches.UnitCanBeTransported, Matches.UnitCanMove, Matches.UnitIsNotInfrastructure);
		final CompositeMatch<Unit> transUnit = new CompositeMatchAnd<Unit>(owned, Matches.UnitIsTransport);
		// CompositeMatch<Unit> factoryUnit = new CompositeMatchAnd<Unit>(owned, Matches.UnitIsFactory);
		// CompositeMatch<Unit> enemyFactoryUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsFactory);
		// List<Territory> myTerritories = SUtils.allAlliedTerritories(data, player);
		// CompositeMatch<Territory> enemyAndNoWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player), Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data));
		// CompositeMatch<Territory> noEnemyOrWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player), Matches.isTerritoryAllied(player, data));
		final List<Territory> transTerr = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitIsTransport);
		if (transTerr.isEmpty())
			return;
		final List<Territory> myTerritories = new ArrayList<Territory>();
		// only looking at T's next to transports.
		for (final Territory t : transTerr)
			for (final Territory t2 : data.getMap().getNeighbors(t))
				if (Matches.isTerritoryAllied(player, data).match(t2))
					myTerritories.add(t2);
		// Territory capitol = TerritoryAttachment.getCapital(player, data);
		// boolean ownMyCapitol = capitol.getOwner().equals(player);
		Unit transport = null;
		int badGuyDist = 0, badGuyFactDist = 0;
		// start at our factories
		// List<Territory> factTerr = SUtils.findUnitTerr(data, player, factoryUnit);
		// if (!factTerr.contains(capitol) && ownMyCapitol)
		// factTerr.add(capitol);
		final List<Unit> transportsFilled = new ArrayList<Unit>();
		/*
		        for (Territory factory : factTerr)
		        {
		        	Route fRoute = SUtils.findNearest(factory, enemyAndNoWater, noEnemyOrWater, data);
		        	boolean closeEnemy = (fRoute != null ? fRoute.getLength() <= 3 : false);
					if (SUtils.doesLandExistAt(factory, data, false) && Matches.territoryHasEnemyLandNeighbor(data, player).match(factory) || closeEnemy)
						continue;
					Set<Territory> myNeighbors = data.getMap().getNeighbors(factory, Matches.TerritoryIsWater);
					List<Unit> unitsTmp = factory.getUnits().getMatches(landUnit);
					List<Unit> unitsToLoad = SUtils.sortTransportUnits(unitsTmp);
					
					for (Territory seaFactTerr : myNeighbors)
					{
						List<Unit> transportUnits = seaFactTerr.getUnits().getMatches(transUnit);
						if (seaFactTerr.isWater() && transportUnits.size()>0)
						{
							List<Unit> units = new ArrayList<Unit>();
		 		            List<Unit> finalTransUnits = new ArrayList<Unit>();
							int transCount = transportUnits.size();
							List<Unit> transCopy = new ArrayList<Unit>(transportUnits);
			            	for (int j=transCount-1; j >= 0; j--)
							{
								transport = transCopy.get(j);
								int free = tracker.getAvailableCapacity(transport);
								if (free <=0)
								{
									transportUnits.remove(j);
									continue;
								}
								Iterator<Unit> iter = unitsToLoad.iterator();
								boolean addOne = false;
								while(iter.hasNext() && free > 0)
								{
									Unit current = iter.next();
									UnitAttachment ua = UnitAttachment.get(current.getType());
									if (ua.isAir())
										continue;
									if (ua.getTransportCost() <= free)
									{
										iter.remove();
										free -= ua.getTransportCost();
										units.add(current);
										addOne = true;
									}
								}
		    					if (addOne)
		    					    finalTransUnits.add(transport);
							}
							if (units.size() > 0)
							{
								Route route = data.getMap().getRoute(factory, seaFactTerr);
								moveUnits.add(units);
								moveRoutes.add(route);
								transportsToLoad.add( finalTransUnits);
								transportsFilled.addAll( finalTransUnits);
								unitsToLoad.removeAll(units);
							}
						}
					}
				} //done with factories
				myTerritories.removeAll(factTerr);
		*/
		for (final Territory checkThis : myTerritories)
		{
			final Route xRoute = null;
			final boolean isLand = SUtils.doesLandExistAt(checkThis, data, true);
			boolean landRoute = false;
			if (isLand)
				landRoute = SUtils.landRouteToEnemyCapital(checkThis, xRoute, data, player);
			// don't consider moved units
			final List<Unit> unitsTmp = checkThis.getUnits().getMatches(new CompositeMatchAnd<Unit>(landUnit, Matches.unitHasNotMoved));
			final List<Unit> unitsToLoad = SUtils.sortTransportUnits(unitsTmp);
			if (unitsToLoad.size() == 0)
				continue;
			final int maxMovement = MoveValidator.getMaxMovement(unitsToLoad);
			// List<Territory> blockThese = new ArrayList<Territory>();
			final Set<Territory> xNeighbors = data.getMap().getNeighbors(checkThis);
			if (isLand)
			{
				final CompositeMatch<Territory> noWaterEnemy = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player, data),
							Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data));
				final CompositeMatch<Territory> noWaterAllied = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player, data), Matches.isTerritoryAllied(player, data));
				final Route badGuyDR = SUtils.findNearest(checkThis, noWaterEnemy, noWaterAllied, data);
				final Route badGuyFactory = SUtils.findNearest(checkThis, Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits),
							Matches.TerritoryIsNotImpassableToLandUnits(player, data), data);
				// boolean noWater = true;
				if (badGuyFactory == null)
					badGuyFactDist = 100;
				else
					badGuyFactDist = badGuyFactory.getLength();
				if (badGuyDR == null)
					badGuyDist = 100;
				else
				{
					// noWater = SUtils.RouteHasNoWater(badGuyDR);
					badGuyDist = badGuyDR.getLength();
				}
				if (landRoute)
					badGuyDist--; // less likely if we have a land Route to Capital from here
				if (badGuyFactDist <= 4 * maxMovement && badGuyDist <= 3 * maxMovement)
					continue;
			}
			// TODO: Track transports that only received 1 unit
			for (final Territory neighbor : xNeighbors)
			{
				if (!neighbor.isWater())
					continue;
				final List<Unit> units = new ArrayList<Unit>();
				final List<Unit> transportUnits = neighbor.getUnits().getMatches(transUnit);
				transportUnits.removeAll(transportsFilled);
				final List<Unit> finalTransUnits = new ArrayList<Unit>();
				if (transportUnits.size() == 0)
					continue;
				final int transCount = transportUnits.size();
				for (int j = transCount - 1; j >= 0; j--)
				{
					transport = transportUnits.get(j);
					int free = tracker.getAvailableCapacity(transport);
					if (free <= 0)
					{
						transportUnits.remove(j);
						continue;
					}
					final Iterator<Unit> iter = unitsToLoad.iterator();
					boolean addOne = false;
					while (iter.hasNext() && free > 0)
					{
						final Unit current = iter.next();
						final UnitAttachment ua = UnitAttachment.get(current.getType());
						if (ua.getIsAir())
							continue;
						if (ua.getTransportCost() <= free)
						{
							iter.remove();
							free -= ua.getTransportCost();
							units.add(current);
							addOne = true;
						}
					}
					if (addOne)
						finalTransUnits.add(transport);
				}
				if (units.size() > 0)
				{
					final Route route = data.getMap().getRoute(checkThis, neighbor);
					moveUnits.add(units);
					moveRoutes.add(route);
					transportsToLoad.add(finalTransUnits);
					transportsFilled.addAll(finalTransUnits);
					unitsToLoad.removeAll(units);
				}
			}
		}
	}
	
	private void populateNonComTransportMove(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		final boolean tFirst = transportsMayDieFirst();
		final TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		// CompositeMatch<Unit> enemyUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data));
		// CompositeMatch<Unit> landAndEnemy = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, enemyUnit);
		// CompositeMatch<Unit> airEnemyUnit = new CompositeMatchAnd<Unit>(enemyUnit, Matches.UnitIsAir);
		final CompositeMatch<Unit> transUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport);
		final CompositeMatch<Unit> ourTransUnit = new CompositeMatchAnd<Unit>(transUnit, Matches.unitIsOwnedBy(player), Matches.unitHasNotMoved);
		// CompositeMatch<Unit> escortUnits = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSea, Matches.UnitIsNotTransport);
		final List<Territory> transTerr2 = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitIsTransport);
		if (transTerr2.isEmpty())
			return;
		final HashMap<Territory, Float> rankMap = SUtils.rankAmphibReinforcementTerritories(data, null, player, tFirst);
		// s_logger.fine("Amphib Terr Rank: "+rankMap);
		final List<Territory> targetTerrs = new ArrayList<Territory>(rankMap.keySet());
		final List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
		// List<PlayerID> ePlayers = SUtils.getEnemyPlayers(data, player);
		final Route amphibRoute = getAmphibRoute(player, true);
		final boolean isAmphib = isAmphibAttack(player, false);
		if (isAmphib && amphibRoute != null && amphibRoute.getEnd() != null)
		{
			final Territory quickDumpTerr = amphibRoute.getEnd();
			final float remainingStrengthNeeded = 1000.0F;
			SUtils.inviteTransports(true, quickDumpTerr, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, tFirst, false, null);
		}
		// PlayerID ePlayer = ePlayers.get(0);
		final float distanceFactor = 0.85F;
		// Target allied territories next to a bad guy
		final List<Territory> allAlliedWithEnemyNeighbor = SUtils.getTerritoriesWithEnemyNeighbor(data, player, true, false);
		allAlliedWithEnemyNeighbor.retainAll(targetTerrs);
		SUtils.reorder(allAlliedWithEnemyNeighbor, rankMap, true);
		for (final Territory aT : allAlliedWithEnemyNeighbor)
		{
			SUtils.inviteTransports(true, aT, 1000.0F, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, tFirst, false, null);
		}
		if (amphibRoute != null)
		{
			for (final Territory tT : transTerr2)
			{
				final Territory amphibDockTerr = amphibRoute.getTerritoryAtStep(amphibRoute.getLength() - 2);
				if (amphibDockTerr != null)
				{
					final List<Unit> transUnits = tT.getUnits().getMatches(Matches.transportIsTransporting());
					transUnits.removeAll(unitsAlreadyMoved);
					if (transUnits.isEmpty())
						continue;
					final int tDist = MoveValidator.getMaxMovement(transUnits);
					final Route dockSeaRoute = SUtils.getMaxSeaRoute(data, tT, amphibDockTerr, player, false, tDist);
					if (dockSeaRoute == null)
						continue;
					final Iterator<Unit> tIter = transUnits.iterator();
					final List<Unit> addUnits = new ArrayList<Unit>();
					while (tIter.hasNext())
					{
						final Unit transport = tIter.next();
						if (MoveValidator.hasEnoughMovement(transport, dockSeaRoute))
						{
							addUnits.add(transport);
							addUnits.addAll(tracker.transporting(transport));
						}
					}
					s_logger.fine("pnct " + addUnits);
					s_logger.fine("pnct " + dockSeaRoute);
					moveUnits.add(addUnits);
					moveRoutes.add(dockSeaRoute);
					unitsAlreadyMoved.addAll(addUnits);
				}
			}
		}
		for (final Territory t : transTerr2)
		{
			/*
			 * 1) Determine our available loaded units
			 */
			// int distanceToEnemy = 100;
			// Route enemyDistanceRoute = SUtils.findNearestNotEmpty(t, Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data), Matches.TerritoryIsNotImpassable, data);
			// if (enemyDistanceRoute != null)
			// distanceToEnemy = enemyDistanceRoute.getLength() + 2; // give it some room
			final List<Unit> ourLandingUnits = new ArrayList<Unit>();
			final List<Unit> mytrans = t.getUnits().getMatches(ourTransUnit);
			mytrans.removeAll(unitsAlreadyMoved);
			final Iterator<Unit> mytransIter = mytrans.iterator();
			while (mytransIter.hasNext())
			{
				final Unit thisTrans = mytransIter.next();
				if (tracker.isTransporting(thisTrans))
					ourLandingUnits.addAll(tracker.transporting(thisTrans));
				else
				{
					mytransIter.remove();
				}
			}
			if (mytrans.isEmpty())
				continue;
			final HashMap<Territory, Float> rankMap2 = new HashMap<Territory, Float>(rankMap);
			for (final Territory target : targetTerrs)
			{
				Float rankValue = rankMap2.get(target);
				final Territory newGoTerr = SUtils.getSafestWaterTerr(target, t, null, data, player, false, tFirst);
				if (newGoTerr == null)
					continue;
				final int thisDist = data.getMap().getWaterDistance(t, newGoTerr);
				final float multiplier = (float) Math.exp(distanceFactor * (thisDist - 2));
				if (thisDist > 2)
				{
					rankValue -= rankValue * multiplier;
					rankMap2.put(target, rankValue);
				}
			}
			SUtils.reorder(targetTerrs, rankMap2, true);
			// Territory targetCap = SUtils.closestEnemyCapital(t, data, player);
			final int tDistance = MoveValidator.getMaxMovement(mytrans);
			final Iterator<Territory> tTIter = targetTerrs.iterator();
			while (tTIter.hasNext() && !mytrans.isEmpty())
			{
				final Territory target = tTIter.next();
				final Set<Territory> targetNeighbors = data.getMap().getNeighbors(target, Matches.TerritoryIsWater);
				if (targetNeighbors.contains(t))
				{
					unitsAlreadyMoved.addAll(mytrans);
					mytrans.clear();
					continue;
				}
				final Territory seaTarget = SUtils.getSafestWaterTerr(target, t, null, data, player, false, tFirst);
				// TODO: getSafestWaterTerr is returning absolutely idiotic results. For example, letting Italy to move its med transports all the way from sz51 to sz27 next to russia's baltic fleet. Or UK to move from sz1 to sz61. WTF
				if (seaTarget == null)
					continue;
				Route seaRoute = SUtils.getMaxSeaRoute(data, t, seaTarget, player, false, tDistance);
				if (seaRoute == null) // This is half of the fix for the transport issue where AI doesn't move amphibiously in some situations, such as America on Great War
				{
					final HashMap<Match<Territory>, Integer> matches = new HashMap<Match<Territory>, Integer>();
					matches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasAlliedUnits(player, data)), 2);
					matches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data)), 3);
					matches.put(Matches.TerritoryIsWater, 6);
					seaRoute = data.getMap().getCompositeRoute(t, seaTarget, matches);
					if (seaRoute == null)
						continue;
					seaRoute = SUtils.TrimRoute_BeforeFirstTerWithEnemyUnits(seaRoute, tDistance, player, data);
					if (seaRoute == null)
						continue;
					final float transportsStrength = SUtils.strength(mytrans, false, true, tFirst);
					float existingTerStrength = 0;
					float newStrength = 0;
					if (seaRoute.getLength() > 1)
						existingTerStrength = SUtils.strength(seaRoute.getEnd().getUnits().getMatches(Matches.UnitIsSea), false, true, tFirst);
					newStrength = transportsStrength + existingTerStrength;
					if (SUtils.getStrengthOfPotentialAttackers(seaRoute.getEnd(), data, player, tFirst, false, new ArrayList<Territory>()) > newStrength)
						continue; // If the strength of the potential attackers is more than the strenth of our transports plus the sea units in the destination, cancel this move. You can take this out if you want, but it may cause suicide ncm moves
				}
				final Iterator<Unit> transIter = mytrans.iterator();
				final List<Unit> moveTransports = new ArrayList<Unit>();
				final List<Unit> moveLoad = new ArrayList<Unit>();
				while (transIter.hasNext())
				{
					final Unit transport = transIter.next();
					if (MoveValidator.hasEnoughMovement(transport, seaRoute))
					{
						moveTransports.add(transport);
						moveLoad.addAll(TripleAUnit.get(transport).getTransporting());
					}
				}
				if (!moveTransports.isEmpty())
				{
					moveTransports.addAll(moveLoad);
					moveUnits.add(moveTransports);
					moveRoutes.add(seaRoute);
					mytrans.clear();
				}
			}
		}
	}
	
	/**
	 * Unloads a transport into an unoccupied enemy Territory
	 * Useful to update AI on true enemy territories for other transport moves
	 * 
	 * @param data
	 * @param moveUnits
	 * @param moveRoutes
	 * @param player
	 */
	private void quickTransportUnload(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		// TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		final CompositeMatch<Unit> loadedTransport = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsTransport, Matches.transportIsTransporting());
		final CompositeMatch<Territory> friendlyWaterTerr = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasUnitsOwnedBy(player));
		final CompositeMatch<Territory> landPassable = new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsPassableAndNotRestricted(player, data));
		final CompositeMatch<Territory> enemyLandWithWater = new CompositeMatchAnd<Territory>(landPassable, Matches.isTerritoryEnemy(player, data), Matches.territoryHasWaterNeighbor(data));
		final CompositeMatch<Unit> myBlitzUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBlitz);
		final CompositeMatch<Unit> myLandUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand);
		final List<Territory> emptyEnemyTerrs = new ArrayList<Territory>();
		final List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
		for (final Territory emptyEnemyTerr : data.getMap().getTerritories())
		{
			if (enemyLandWithWater.match(emptyEnemyTerr) && Matches.territoryIsEmptyOfCombatUnits(data, player).match(emptyEnemyTerr) && Matches.TerritoryIsNotImpassable.match(emptyEnemyTerr))
				emptyEnemyTerrs.add(emptyEnemyTerr);
		}
		for (final Territory enemyTerr : emptyEnemyTerrs)
		{
			final Set<Territory> transTerrs = data.getMap().getNeighbors(enemyTerr, friendlyWaterTerr);
			final List<Territory> landNeighborTerrs = SUtils.getNeighboringLandTerritories(data, player, enemyTerr);
			final Iterator<Territory> lNIter = landNeighborTerrs.iterator();
			while (lNIter.hasNext())
			{
				final Territory thisTerr = lNIter.next();
				final float eAttackStrength = SUtils.getStrengthOfPotentialAttackers(enemyTerr, data, player, false, true, null);
				final float defenseStrength = SUtils.strength(thisTerr.getUnits().getUnits(), false, false, false);
				if (eAttackStrength > (defenseStrength * 1.10F - 2.0F)
							|| (Matches.territoryIsAlliedAndHasAlliedUnitMatching(data, player, Matches.UnitCanProduceUnits).match(thisTerr) && eAttackStrength > defenseStrength))
				{
					lNIter.remove();
					continue;
				}
				else if (Matches.territoryHasAlliedNeighborWithAlliedUnitMatching(data, player, Matches.UnitCanProduceUnits).match(thisTerr))
				{
					final Set<Territory> myFactNeighbors = data.getMap().getNeighbors(thisTerr, Matches.territoryIsAlliedAndHasAlliedUnitMatching(data, player, Matches.UnitCanProduceUnits));
					for (final Territory newTerr : myFactNeighbors)
					{
						final float eAttackStrength2 = SUtils.getStrengthOfPotentialAttackers(newTerr, data, player, false, true, null);
						final float defenseStrength2 = SUtils.strength(newTerr.getUnits().getUnits(), false, false, false);
						if (eAttackStrength2 > defenseStrength2)
						{
							lNIter.remove();
							break;
						}
					}
				}
			}
			final Iterator<Territory> lNIter2 = landNeighborTerrs.iterator();
			boolean attacked = false;
			// Land Blitz Unit preferred
			while (lNIter2.hasNext() && !attacked)
			{
				final Territory thisTerr = lNIter2.next();
				final List<Unit> myBlitzers = thisTerr.getUnits().getMatches(myBlitzUnit);
				myBlitzers.removeAll(unitsAlreadyMoved);
				if (!myBlitzers.isEmpty())
				{
					final Unit blitzUnit = myBlitzers.get(0);
					final Route goRoute = data.getMap().getRoute(thisTerr, enemyTerr);
					if (goRoute != null)
					{
						moveUnits.add(Collections.singletonList(blitzUnit));
						moveRoutes.add(goRoute);
						unitsAlreadyMoved.add(blitzUnit);
						attacked = true;
					}
				}
			}
			// one more pass for non-blitz Unit
			final Iterator<Territory> lNIter3 = landNeighborTerrs.iterator();
			while (lNIter3.hasNext() && !attacked)
			{
				final Territory thisTerr = lNIter3.next();
				final List<Unit> myAttackers = thisTerr.getUnits().getMatches(myLandUnit);
				myAttackers.removeAll(unitsAlreadyMoved);
				if (!myAttackers.isEmpty())
				{
					final Unit attackUnit = myAttackers.get(0);
					final Route goRoute = data.getMap().getRoute(thisTerr, enemyTerr);
					if (goRoute != null)
					{
						moveUnits.add(Collections.singletonList(attackUnit));
						moveRoutes.add(goRoute);
						unitsAlreadyMoved.add(attackUnit);
						attacked = true;
					}
				}
			}
			final Iterator<Territory> transIter = transTerrs.iterator();
			while (transIter.hasNext() && !attacked)
			{
				final Territory transTerr = transIter.next();
				final List<Unit> loadedTransports = transTerr.getUnits().getMatches(loadedTransport);
				loadedTransports.removeAll(unitsAlreadyMoved);
				if (!loadedTransports.isEmpty())
				{
					final Unit transport = loadedTransports.get(0);
					final Collection<Unit> loadedUnits = TripleAUnit.get(transport).getTransporting();
					final Route dumpRoute = data.getMap().getRoute(transTerr, enemyTerr);
					if (dumpRoute != null)
					{
						moveUnits.add(loadedUnits);
						moveRoutes.add(dumpRoute);
						unitsAlreadyMoved.add(transport);
						attacked = true;
					}
				}
			}
		}
	}
	
	/**
	 * Take territories that are empty
	 * 
	 * @param data
	 * @param moveUnits
	 * @param moveRoutes
	 * @param player
	 */
	private void specialTransportMove(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		// setImpassableTerrs(player);
		// Collection<Territory> impassableTerrs = getImpassableTerrs();
		final TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		final HashMap<Territory, Territory> amphibMap = new HashMap<Territory, Territory>();
		final CompositeMatch<Unit> transUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.transportIsTransporting());
		final CompositeMatch<Territory> landPassable = new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsPassableAndNotRestricted(player, data));
		final CompositeMatch<Territory> enemyEmpty = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemy(player, data), Matches.territoryHasEnemyLandUnits(player, data).invert(), landPassable,
					Matches.territoryHasWaterNeighbor(data));
		final CompositeMatch<Territory> enemyTarget = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemy(player, data), Matches.territoryHasWaterNeighbor(data),
					Matches.TerritoryIsNotImpassable);
		final List<Territory> transTerr = SUtils.findTersWithUnitsMatching(data, player, transUnit);
		if (transTerr.isEmpty())
		{
			s_logger.fine("Transports not found on entire map for player: " + player.getName());
			return;
		}
		final List<Territory> seaTerrAttacked = getSeaTerrAttacked();
		final boolean tFirst = transportsMayDieFirst();
		final List<Territory> inRangeTerr = new ArrayList<Territory>();
		final List<Territory> oneUnitTerr = new ArrayList<Territory>();
		for (final Territory startTerr : transTerr)
		{
			final Set<Territory> enemyTerr = data.getMap().getNeighbors(startTerr, 3);
			for (final Territory eCheck : enemyTerr)
			{
				if (!inRangeTerr.contains(eCheck) && enemyEmpty.match(eCheck))
					inRangeTerr.add(eCheck);
				else if (!oneUnitTerr.contains(eCheck) && enemyTarget.match(eCheck) && eCheck.getUnits().countMatches(Matches.UnitIsLand) < 3 && !SUtils.doesLandExistAt(eCheck, data, false))
					oneUnitTerr.add(eCheck); // islands are easy for the ranking system to miss
			}
		}
		if (inRangeTerr.isEmpty() && oneUnitTerr.isEmpty())
			return;
		final List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
		final List<Territory> ourEnemyTerr = new ArrayList<Territory>();
		final HashMap<Territory, Float> rankMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, seaTerrAttacked, player, tFirst, false, false);
		final Set<Territory> rankTerr = rankMap.keySet();
		inRangeTerr.retainAll(rankTerr); // should clear out any neutrals that are not common between the two
		oneUnitTerr.retainAll(rankTerr);
		// s_logger.fine("Attackable Territories: "+inRangeTerr);
		// s_logger.fine("Invadable Territories: "+oneUnitTerr);
		SUtils.reorder(inRangeTerr, rankMap, true);
		/*
		 * RankTerritories heavily emphasizes the land based attacks. One Unit Terr will get the amphib attacker
		 * going after easy to take islands...using RankTerr, but not comparing to territories which have a direct
		 * line to an enemy cap. Adjust the number of units allowed in the count for landUnits.
		 */
		SUtils.reorder(oneUnitTerr, rankMap, true);
		final List<Territory> landTerrConquered = new ArrayList<Territory>();
		final List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
		Route goRoute = new Route();
		for (final Territory landTerr : inRangeTerr)
		{
			// float eAttackPotential = SUtils.getStrengthOfPotentialAttackers(landTerr, data, player, tFirst, true, landTerrConquered);
			final PlayerID ePlayer = landTerr.getOwner();
			float myAttackPotential = SUtils.getStrengthOfPotentialAttackers(landTerr, data, ePlayer, tFirst, true, null);
			myAttackPotential += TerritoryAttachment.get(landTerr).getProduction() * 2;
			// if (myAttackPotential < (0.75F*eAttackPotential - 3.0F))
			// continue;
			for (final Territory sourceTerr : transTerr)
			{
				Territory goTerr = SUtils.getSafestWaterTerr(landTerr, sourceTerr, seaTerrAttacked, data, player, false, tFirst);
				if (goTerr == null)
				{
					goTerr = SUtils.getClosestWaterTerr(landTerr, sourceTerr, data, player, false);
					if (goTerr == null)
						continue;
				}
				final List<Unit> transports = sourceTerr.getUnits().getMatches(transUnit);
				transports.removeAll(unitsAlreadyMoved);
				if (transports.isEmpty())
					continue;
				final int tDist = MoveValidator.getMaxMovement(transports);
				goRoute = SUtils.getMaxSeaRoute(data, sourceTerr, goTerr, player, false, tDist);
				if (goRoute == null || goRoute.getEnd() != goTerr)
					continue;
				final Collection<Unit> unitsToMove = new ArrayList<Unit>();
				if (Matches.territoryHasEnemyNonNeutralNeighborWithEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits).match(goTerr)
							|| Matches.territoryHasEnemyNonNeutralNeighborWithEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits).match(landTerr))
				{
					unitsToMove.addAll(transports);
					for (final Unit transport : transports)
						unitsToMove.addAll(tracker.transporting(transport));
				}
				else
				{
					final Unit oneTransport = transports.get(0);
					unitsToMove.addAll(tracker.transporting(oneTransport));
					unitsToMove.add(oneTransport);
				}
				landTerrConquered.add(landTerr);
				moveUnits.add(unitsToMove);
				moveRoutes.add(goRoute);
				unitsAlreadyMoved.addAll(unitsToMove);
			}
		}
		for (final Territory easyTerr : oneUnitTerr)
		{
			final float eStrength = SUtils.strength(easyTerr.getUnits().getMatches(Matches.enemyUnit(player, data)), false, false, tFirst);
			float strengthNeeded = eStrength * 1.35F + 3.0F;
			final List<Collection<Unit>> xMoveUnits = new ArrayList<Collection<Unit>>();
			final List<Route> xMoveRoutes = new ArrayList<Route>();
			final List<Unit> xAlreadyMoved = new ArrayList<Unit>(unitsAlreadyMoved);
			final float transStrength = SUtils.inviteTransports(false, easyTerr, strengthNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, tFirst, true, seaTerrAttacked);
			int transCount = 0;
			for (final Collection<Unit> xCollection : xMoveUnits)
			{
				transCount += xCollection.size();
			}
			final int routeNumbers = xMoveRoutes.size();
			Territory landingTerr = null;
			// for now, just target the last route's endpoint
			if (routeNumbers > 0)
				landingTerr = xMoveRoutes.get(routeNumbers - 1).getEnd();
			if (transStrength < 1.0F)
				continue;
			strengthNeeded -= transStrength;
			strengthNeeded = Math.max(strengthNeeded, 3.0F);
			float BBStrength = 0.0F;
			if (landingTerr != null)
				BBStrength = SUtils.inviteBBEscort(landingTerr, strengthNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player);
			float planeStrength = 0.0F;
			if (strengthNeeded > 0.0F)
			{
				planeStrength = SUtils.invitePlaneAttack(false, false, easyTerr, strengthNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player);
			}
			final float myAttackStrength = BBStrength + transStrength + planeStrength;
			if (myAttackStrength > (eStrength + 1.0F))
			{
				moveUnits.addAll(xMoveUnits);
				moveRoutes.addAll(xMoveRoutes);
				final Iterator<Route> routeIter = moveRoutes.iterator();
				while (routeIter.hasNext())
				{
					final Route checkRoute = routeIter.next();
					final Territory tTerr = checkRoute.getEnd();
					amphibMap.put(tTerr, easyTerr);
				}
				unitsAlreadyMoved.addAll(xAlreadyMoved);
				landTerrConquered.add(easyTerr);
				if (landingTerr != null)
				{
					final float seaPotential = SUtils.getStrengthOfPotentialAttackers(landingTerr, data, player, tFirst, false, seaTerrAttacked);
					float mySeaStrength = tFirst ? transCount : 0;
					mySeaStrength += BBStrength;
					if (mySeaStrength < seaPotential * 0.80F - 2.0F)
					{ // don't subtract 2 if we have no strength yet...it might leave transports with no protection
						final float remainingStrengthNeeded = mySeaStrength > 0.50F ? seaPotential * 0.80F - 2.0F - mySeaStrength : seaPotential * 0.80F;
						SUtils.inviteShipAttack(landingTerr, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, false, tFirst, false);
					}
				}
			}
		}
		setAmphibMap(amphibMap);
		setLandTerrAttacked(landTerrConquered);
	}
	
	private void firstTransportMove(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		final TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		final CompositeMatch<Unit> transUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport);
		final CompositeMatch<Unit> transportingUnit = new CompositeMatchAnd<Unit>(transUnit, Matches.unitIsOwnedBy(player), Matches.transportIsTransporting());
		// CompositeMatch<Unit> escortUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsNotTransport, Matches.UnitIsCarrier.invert());
		final CompositeMatch<Territory> landPassable = new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsPassableAndNotRestricted(player, data));
		// CompositeMatch<Territory> endOfRoute = new CompositeMatchAnd<Territory>(landPassable, Matches.territoryHasRouteToEnemyCapital(data, player));
		final CompositeMatch<Territory> routeCondition = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater);
		final List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
		final Territory capitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		if (!isAmphibAttack(player, false) || !Matches.territoryHasWaterNeighbor(data).match(capitol))
			return;
		final Set<Territory> waterCapNeighbors = data.getMap().getNeighbors(capitol, Matches.TerritoryIsWater);
		if (waterCapNeighbors.isEmpty()) // should not happen, but just in case on some wierd map
			return;
		final Territory waterCap = waterCapNeighbors.iterator().next();
		final Route goRoute = SUtils.findNearest(waterCap, Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data), routeCondition, data);
		// s_logger.fine("First Transport move Route: "+ goRoute);
		if (goRoute == null || goRoute.getEnd() == null)
			return;
		final Territory endTerr = goRoute.getEnd();
		final List<Territory> waterTerrs = new ArrayList<Territory>();
		waterTerrs.addAll(data.getMap().getNeighbors(endTerr, 20)); // 14 (or 6) appears to be arbitrary number of territories away the enemy is, that is the maximum we can move towards with transports (20or14 are for GreatWar, 12or9 for ww2v3, 6 was original)
		// List<Territory> waterTerrs = SUtils.getExactNeighbors(endTerr, 6, player, false); //TODO: why is kevin checking distance = 6 here?
		final Set<Territory> xWaterTerrs = data.getMap().getNeighbors(endTerr, 3); // And why remove distance 3 from it?
		waterTerrs.removeAll(xWaterTerrs);
		final Iterator<Territory> wIter = waterTerrs.iterator();
		while (wIter.hasNext()) // clean out land and empty territories
		{
			final Territory waterTerr = wIter.next();
			if (Matches.TerritoryIsLand.match(waterTerr) || Matches.territoryHasNoAlliedUnits(player, data).match(waterTerr))
				wIter.remove();
		}
		for (final Territory myTerr : waterTerrs)
		{ // don't move transports which are next to a good spot
			// Set<Territory> neighborList = data.getMap().getNeighbors(myTerr, Matches.territoryHasRouteToEnemyCapital(data, player));
			final Set<Territory> neighborList = data.getMap().getNeighbors(myTerr, landPassable);
			final Iterator<Territory> nIter = neighborList.iterator();
			while (nIter.hasNext())
			{
				final Territory nTerr = nIter.next();
				if (!SUtils.hasLandRouteToEnemyOwnedCapitol(nTerr, player, data))
					nIter.remove();
			}
			if (!neighborList.isEmpty())
				continue;
			final List<Territory> neighborList2 = SUtils.getExactNeighbors(myTerr, 2, player, data, true);
			boolean skipTerr = false;
			for (final Territory nTerr : neighborList2)
			{
				// if (Matches.territoryHasRouteToEnemyCapital(data, player).match(nTerr))
				if (SUtils.hasLandRouteToEnemyOwnedCapitol(nTerr, player, data))
					skipTerr = true;
			}
			if (skipTerr)
				continue;
			final Territory closeTerr = SUtils.getClosestWaterTerr(endTerr, myTerr, data, player, true);
			final List<Unit> transUnits = myTerr.getUnits().getMatches(transportingUnit);
			transUnits.removeAll(unitsAlreadyMoved);
			if (transUnits.isEmpty())
				continue;
			final int maxDistance = MoveValidator.getMaxMovement(transUnits);
			final Route seaRoute = SUtils.getMaxSeaRoute(data, myTerr, closeTerr, player, true, maxDistance);
			if (seaRoute == null)
				continue;
			final List<Unit> transportedUnits = new ArrayList<Unit>();
			for (final Unit transport : transUnits)
				transportedUnits.addAll(tracker.transporting(transport));
			transUnits.addAll(transportedUnits);
			moveUnits.add(transUnits);
			moveRoutes.add(seaRoute);
		}
	}
	
	private void defendTransportingLocations(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		// (VEQRYN) the purpose of this method is to put 1 decent ship at the beginning and the end of a amphibious route, if there is an amphibious route and we own transports
		// because the ships may already be there, they will show up as still having movement after this method unless we do something,
		// so since we don't want the next method to move them, we must manually set their movement to zero that way they will not be included in future methods
		final CompositeMatch<Territory> routeCondition = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater);
		final CompositeMatch<Territory> endCondition = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data), Matches.TerritoryIsLand);
		final CompositeMatch<Territory> endCondition2 = new CompositeMatchAnd<Territory>(Matches.territoryHasLandRouteToEnemyCapital(data, player),
					Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data), Matches.TerritoryIsLand);
		// CompositeMatch<Unit> ourTransports = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsTransport);
		final CompositeMatch<Unit> ourWarships = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSea, Matches.unitHasDefenseThatIsMoreThanOrEqualTo(1), Matches.UnitIsNotSub,
					Matches.UnitIsNotTransport);
		final CompositeMatch<Unit> enemyWarships = new CompositeMatchAnd<Unit>(Matches.unitIsEnemyOf(data, player), Matches.UnitIsSea, Matches.unitCanAttack(player));
		final CompositeMatch<Unit> enemyAir = new CompositeMatchAnd<Unit>(Matches.unitIsEnemyOf(data, player), Matches.UnitIsAir, Matches.unitCanAttack(player));
		final CompositeMatch<Unit> enemyAttackStuff = new CompositeMatchAnd<Unit>(Matches.unitIsEnemyOf(data, player), Matches.unitCanAttack(player));
		final Territory capitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		if (!isAmphibAttack(player, false) || !Matches.territoryHasWaterNeighbor(data).match(capitol)) // will return if capitol is not next to water
			return;
		final Set<Territory> waterCapNeighbors = data.getMap().getNeighbors(capitol, Matches.TerritoryIsWater);
		if (waterCapNeighbors.isEmpty()) // should not happen, but just in case on some weird map
			return;
		// List<Territory> terrsWithOurTransports = SUtils.findUnitTerr(data, player, ourTransports);
		// Set<Territory> waterNearCap = data.getMap().getNeighbors(capitol, 4);
		// terrsWithOurTransports.retainAll(waterNearCap);
		// if (terrsWithOurTransports.isEmpty() && !getDidPurchaseTransports())
		// return;
		// populate alreadyMoved before we begin
		final List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
		for (final Unit u : data.getUnits().getUnits())
		{
			if (u.getOwner().equals(player) && TripleAUnit.get(u).getMovementLeft() < 1)
				unitsAlreadyMoved.add(u);
		}
		final Territory waterCap = waterCapNeighbors.iterator().next();
		final Route goRoute = SUtils.findNearest(waterCap, endCondition, routeCondition, data);
		final Route goRoute2 = SUtils.findNearest(waterCap, endCondition2, routeCondition, data);
		if (goRoute == null)
			return;
		final Territory startTerr = goRoute.getStart();
		Territory endTerr = goRoute.getStart();
		Territory firstEndTerr = goRoute.getStart();
		if (goRoute.getLength() > 1)
			endTerr = goRoute.getTerritoryAtStep(goRoute.getLength() - 2);
		if (goRoute2 != null && goRoute2.getLength() > 1)
			firstEndTerr = goRoute2.getTerritoryAtStep(goRoute2.getLength() - 2);
		else if (goRoute.getLength() > 1)
			firstEndTerr = goRoute.getTerritoryAtStep(goRoute.getLength() - 2);
		final List<Territory> TransportDropOffLocales = new ArrayList<Territory>(getTransportDropOffLocales());
		if (TransportDropOffLocales.isEmpty())
		{
			final List<Territory> firstEndTerrs = new ArrayList<Territory>();
			firstEndTerrs.add(firstEndTerr);
			setTransportDropOffLocales(firstEndTerrs);
		}
		else
			firstEndTerr = TransportDropOffLocales.get(0);
		// TODO: this could be greatly improved by using the route finding info from each way moore finds transporting routes AND by making some of the routine stuff here into a separate method
		final List<Collection<Unit>> xUnits = new ArrayList<Collection<Unit>>();
		final List<Route> xRoutes = new ArrayList<Route>();
		final List<Territory> terrsWithEnemyWarships = SUtils.findUnitTerr(data, player, enemyWarships);
		final List<Territory> terrsWithEnemyAir = SUtils.findUnitTerr(data, player, enemyAir);
		final List<Territory> terrsWithEnemyAttackStuff = SUtils.findUnitTerr(data, player, enemyAttackStuff);
		final Set<Territory> startTerrNeighborsW = data.getMap().getNeighbors(startTerr, 2);
		final Set<Territory> endTerrNeighborsW = data.getMap().getNeighbors(endTerr, 2);
		final Set<Territory> firstEndTerrNeighborsW = data.getMap().getNeighbors(firstEndTerr, 2);
		final Set<Territory> startTerrNeighborsA = data.getMap().getNeighbors(startTerr, 3); // could be anywhere from 3 to 5, if you imagine the range of bombers
		final Set<Territory> endTerrNeighborsA = data.getMap().getNeighbors(endTerr, 3);
		final Set<Territory> firstEndTerrNeighborsA = data.getMap().getNeighbors(firstEndTerr, 3);
		final Set<Territory> startFar = data.getMap().getNeighbors(startTerr, 4); // these could be 4 or 5
		final Set<Territory> firstEndFar = data.getMap().getNeighbors(firstEndTerr, 4);
		startTerrNeighborsW.retainAll(terrsWithEnemyWarships);
		startTerrNeighborsA.retainAll(terrsWithEnemyAir);
		endTerrNeighborsW.retainAll(terrsWithEnemyWarships);
		endTerrNeighborsA.retainAll(terrsWithEnemyAir);
		firstEndTerrNeighborsW.retainAll(terrsWithEnemyWarships);
		firstEndTerrNeighborsA.retainAll(terrsWithEnemyAir);
		startFar.retainAll(terrsWithEnemyAttackStuff);
		firstEndFar.retainAll(terrsWithEnemyAttackStuff);
		// List<PlayerID> enemyplayers = SUtils.getEnemyPlayers(data, player);
		// PlayerID ePlayer = enemyplayers.get(0);
		float startNeededStrength = 1 + SUtils.getStrengthOfPotentialAttackers(startTerr, data, player, false, false, null);
		float endNeededStrength = 1 + SUtils.getStrengthOfPotentialAttackers(endTerr, data, player, false, false, null);
		float firstEndNeededStrength = 1 + SUtils.getStrengthOfPotentialAttackers(firstEndTerr, data, player, false, false, null);
		startNeededStrength = Math.max(startNeededStrength, 5 * startTerrNeighborsW.size());
		startNeededStrength = Math.max(startNeededStrength, 3 * startTerrNeighborsA.size());
		endNeededStrength = Math.max(endNeededStrength, 5 * endTerrNeighborsW.size());
		endNeededStrength = Math.max(endNeededStrength, 3 * endTerrNeighborsA.size());
		firstEndNeededStrength = Math.max(firstEndNeededStrength, 5 * firstEndTerrNeighborsW.size());
		firstEndNeededStrength = Math.max(firstEndNeededStrength, 3 * firstEndTerrNeighborsA.size());
		// put a ship at the beginning of the route
		if (Matches.TerritoryIsWater.match(startTerr) && Matches.territoryHasNoEnemyUnits(player, data).match(startTerr) && startNeededStrength > 0)
		{
			// check to see if there is no way we can win if we move there
			final CompositeMatch<Unit> ourWarshipsZ = new CompositeMatchAnd<Unit>(ourWarships, Matches.unitIsInTerritory(startTerr).invert(), Matches.unitIsInTerritory(firstEndTerr).invert(), Matches
						.unitIsInTerritory(endTerr).invert());
			float ourNeighborsStrength = SUtils.inviteShipAttack(startTerr, startNeededStrength, new ArrayList<Unit>(unitsAlreadyMoved), new ArrayList<Collection<Unit>>(), new ArrayList<Route>(),
						data, player, false, false, false, ourWarshipsZ);
			ourNeighborsStrength += SUtils.strengthOfTerritory(data, startTerr, player, false, true, false, false);
			if ((ourNeighborsStrength < startNeededStrength * 0.8F && startNeededStrength > 3) || ourNeighborsStrength > startNeededStrength * 1.4F + 1)
			{
				if (data.getSequence().getRound() <= 2 || startFar.isEmpty()) // it takes a few turns for stuff to settle, and we may want to attack things or move elsewhere
					startNeededStrength = 0;
				else if (firstEndTerr.equals(startTerr))
					startNeededStrength = 8;
				else
					startNeededStrength = 1; // we want to gradually wear down the enemy's air force even if it costs us, to relieve pressure on russia
			}
			// check if we have anything there already, and if so, set it to zero movement and reduce needed strength
			if (Matches.territoryHasUnitsThatMatch(ourWarships).match(startTerr))
			{
				final List<Unit> myWarships = startTerr.getUnits().getMatches(ourWarships);
				Collections.shuffle(myWarships); // we don't always want to have the weakest units stay first, in case we may need those destroyers for anti sub warfare
				// float myWarshipsStrength = SUtils.strength(myWarships, false, true, false);
				final Iterator<Unit> shipIter = myWarships.iterator();
				while (shipIter.hasNext() && startNeededStrength > 0)
				{
					final Unit ship = shipIter.next();
					TripleAUnit.get(ship).setAlreadyMoved(TripleAUnit.get(ship).getMaxMovementAllowed());
					final List<Unit> shipTemp = new ArrayList<Unit>();
					shipTemp.add(ship);
					startNeededStrength = Math.max(0, startNeededStrength - SUtils.strength(shipTemp, false, true, false));
				}
			}
			if (startNeededStrength > 0)
			{
				final CompositeMatch<Unit> ourWarshipsX = new CompositeMatchAnd<Unit>(ourWarships);
				if (endNeededStrength > 1 && !startTerr.equals(endTerr))
					ourWarshipsX.addInverse(Matches.unitIsInTerritory(endTerr));
				SUtils.inviteShipAttack(startTerr, startNeededStrength, unitsAlreadyMoved, xUnits, xRoutes, data, player, false, false, false, ourWarshipsX);
				moveUnits.addAll(xUnits);
				moveRoutes.addAll(xRoutes);
			}
			xUnits.clear();
			xRoutes.clear();
		}
		// put a ship at the end of the first route we ever made, unless it is a 1 territory route
		// if (terrsWithOurTransports.isEmpty())
		// return;
		if (!firstEndTerr.equals(startTerr) && Matches.TerritoryIsWater.match(firstEndTerr) && Matches.territoryHasNoEnemyUnits(player, data).match(firstEndTerr) && firstEndNeededStrength > 0)
		{
			// check to see if there is no way we can win if we move there
			final CompositeMatch<Unit> ourWarshipsZ = new CompositeMatchAnd<Unit>(ourWarships, Matches.unitIsInTerritory(firstEndTerr).invert(), Matches.unitIsInTerritory(endTerr).invert());
			float ourNeighborsStrength = SUtils.inviteShipAttack(firstEndTerr, firstEndNeededStrength, new ArrayList<Unit>(unitsAlreadyMoved), new ArrayList<Collection<Unit>>(),
						new ArrayList<Route>(), data, player, false, false, false, ourWarshipsZ);
			ourNeighborsStrength += SUtils.strengthOfTerritory(data, firstEndTerr, player, false, true, false, false);
			if ((ourNeighborsStrength < firstEndNeededStrength * 0.9F && firstEndNeededStrength > 3) || ourNeighborsStrength > firstEndNeededStrength * 1.4F + 1 || data.getSequence().getRound() <= 2) // first 2 rounds, don't try to defend route end, instead attack stuff
			{
				if (data.getSequence().getRound() <= 2 || firstEndFar.isEmpty()) // it takes a few turns for stuff to settle, and we may want to attack things or move elsewhere
					firstEndNeededStrength = 0;
				else
					firstEndNeededStrength = 8; // we want to gradually wear down the enemy's air force even if it costs us, to relieve pressure on russia
			}
			// check if we have anything there already, and if so, set it to zero movement and reduce needed strength
			if (Matches.territoryHasUnitsThatMatch(ourWarships).match(firstEndTerr))
			{
				final List<Unit> myWarships = firstEndTerr.getUnits().getMatches(ourWarships);
				Collections.shuffle(myWarships); // we don't always want to have the weakest units stay first, in case we may need those destroyers for anti sub warfare
				// float myWarshipsStrength = SUtils.strength(myWarships, false, true, false);
				final Iterator<Unit> shipIter = myWarships.iterator();
				while (shipIter.hasNext() && firstEndNeededStrength > 0)
				{
					final Unit ship = shipIter.next();
					final int x = UnitAttachment.get(TripleAUnit.get(ship).getType()).getMovement(player);
					TripleAUnit.get(ship).setAlreadyMoved(x);
					final List<Unit> shipTemp = new ArrayList<Unit>();
					shipTemp.add(ship);
					firstEndNeededStrength = Math.max(0, firstEndNeededStrength - SUtils.strength(shipTemp, false, true, false));
				}
			}
			if (firstEndNeededStrength > 0)
			{
				SUtils.inviteShipAttack(firstEndTerr, firstEndNeededStrength, unitsAlreadyMoved, xUnits, xRoutes, data, player, false, false, false, ourWarships);
				moveUnits.addAll(xUnits);
				moveRoutes.addAll(xRoutes);
			}
			xUnits.clear();
			xRoutes.clear();
		}
		// put a ship at the end of the newest route, unless it is a 1 territory route
		if (!endTerr.equals(startTerr) && !endTerr.equals(firstEndTerr) && Matches.TerritoryIsWater.match(endTerr) && Matches.territoryHasNoEnemyUnits(player, data).match(endTerr)
					&& endNeededStrength > 0)
		{
			// check to see if there is no way we can win if we move there
			final CompositeMatch<Unit> ourWarshipsZ = new CompositeMatchAnd<Unit>(ourWarships, Matches.unitIsInTerritory(endTerr).invert());
			float ourNeighborsStrength = SUtils.inviteShipAttack(endTerr, endNeededStrength, new ArrayList<Unit>(unitsAlreadyMoved), new ArrayList<Collection<Unit>>(), new ArrayList<Route>(), data,
						player, false, false, false, ourWarshipsZ);
			ourNeighborsStrength += SUtils.strengthOfTerritory(data, endTerr, player, false, true, false, false);
			if ((ourNeighborsStrength < endNeededStrength * 0.9F && endNeededStrength > 3) || ourNeighborsStrength > endNeededStrength * 1.4F + 1 || data.getSequence().getRound() <= 2) // first 2 rounds, don't try to defend route end, instead attack stuff
			{
				if (data.getSequence().getRound() <= 3) // it takes a few turns for stuff to settle, and we may want to attack things or move elsewhere
					endNeededStrength = 0;
				else
					endNeededStrength = 1; // we want to gradually wear down the enemy's air force even if it costs us, to relieve pressure on russia
			}
			// check if we have anything there already, and if so, set it to zero movement and reduce needed strength
			if (Matches.territoryHasUnitsThatMatch(ourWarships).match(endTerr))
			{
				final List<Unit> myWarships = endTerr.getUnits().getMatches(ourWarships);
				Collections.shuffle(myWarships); // we don't always want to have the weakest units stay first, in case we may need those destroyers for anti sub warfare
				// float myWarshipsStrength = SUtils.strength(myWarships, false, true, false);
				final Iterator<Unit> shipIter = myWarships.iterator();
				while (shipIter.hasNext() && endNeededStrength > 0)
				{
					final Unit ship = shipIter.next();
					final int x = UnitAttachment.get(TripleAUnit.get(ship).getType()).getMovement(player);
					TripleAUnit.get(ship).setAlreadyMoved(x);
					final List<Unit> shipTemp = new ArrayList<Unit>();
					shipTemp.add(ship);
					endNeededStrength = Math.max(0, endNeededStrength - SUtils.strength(shipTemp, false, true, false));
				}
			}
			if (endNeededStrength > 0)
			{
				SUtils.inviteShipAttack(endTerr, endNeededStrength, unitsAlreadyMoved, xUnits, xRoutes, data, player, false, false, false, ourWarships);
				moveUnits.addAll(xUnits);
				moveRoutes.addAll(xRoutes);
			}
			xUnits.clear();
			xRoutes.clear();
		}
	}
	
	private void populateTransportMove(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		/*
		 * Want to initiate attack on any territory within range of transports by:
		 * 1) Determining a general ranking of territories
		 * 2) Finding a safe location for invading
		 * 3) Leaving transports at the base factory if not
		 * 		a) Allow the non-combat transport move to determine a target farther away
		 */
		setImpassableTerrs(player);
		final Collection<Territory> impassableTerrs = getImpassableTerrs();
		final boolean tFirst = transportsMayDieFirst();
		// boolean isAmphib = isAmphibAttack(player, false);
		final Route amphibRoute = getAmphibRoute(player, false);
		// boolean aggressive = SUtils.determineAggressiveAttack(data, player, 1.4F);
		final TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		final CompositeMatch<Unit> transUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport);
		final CompositeMatch<Unit> transportingUnit = new CompositeMatchAnd<Unit>(transUnit, Matches.unitIsOwnedBy(player), Matches.transportIsTransporting());
		// CompositeMatch<Unit> escortUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsNotTransport, Matches.UnitIsCarrier.invert(), HasntMoved);
		// List<Territory> dontMoveFrom = new ArrayList<Territory>();
		final List<Territory> alreadyAttacked = getLandTerrAttacked();
		final List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
		final HashMap<Territory, Territory> amphibMap = new HashMap<Territory, Territory>();
		for (final Unit u : data.getUnits().getUnits())
		{
			if (u.getOwner().equals(player) && TripleAUnit.get(u).getMovementLeft() < 1)
				unitsAlreadyMoved.add(u);
		}
		/**
		 * First determine if attack ships have been purchased and limit moves at that factory
		 */
		// boolean attackShipsBought = getKeepShipsAtBase();
		// Territory baseFactory = getSeaTerr();
		/*		if (attackShipsBought && baseFactory != null)
				{
					Set<Territory> baseTerrs = data.getMap().getNeighbors(baseFactory, Matches.TerritoryIsWater);
					dontMoveFrom.addAll(baseTerrs);
					for (Territory baseTerr : baseTerrs)
					{
						unitsAlreadyMoved.addAll(baseTerr.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
					}
				}
		*/
		final List<Territory> seaTerrAttacked = getSeaTerrAttacked();
		alreadyAttacked.addAll(seaTerrAttacked);
		Territory amphibAttackTerr = null;
		// go back to the amphib route
		if (amphibRoute != null)
		{
			amphibAttackTerr = amphibRoute.getEnd();
		}
		final List<Territory> occTransTerr = SUtils.findTersWithUnitsMatching(data, player, transportingUnit);
		// not used
		// occTransTerr.removeAll(dontMoveFrom);
		if (occTransTerr == null || occTransTerr.isEmpty())
			return;
		final IntegerMap<Territory> transCountMap = new IntegerMap<Territory>();
		// territories with loaded transports in order of most transports.
		for (final Territory transTerr : occTransTerr)
			transCountMap.put(transTerr, transTerr.getUnits().countMatches(transportingUnit));
		SUtils.reorder(occTransTerr, transCountMap, true);
		final List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
		final List<Territory> ourEnemyTerr = new ArrayList<Territory>();
		final HashMap<Territory, Float> landTerrMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, seaTerrAttacked, player, tFirst, true, false);
		// not used
		// IntegerMap<Territory> targetMap = SUtils.targetTerritories(data, player, 4);
		// Collection<Territory> targetTerritories = targetMap.keySet();
		/*		for (Territory tT : targetTerritories)
				{
					if (landTerrMap.containsKey(tT))
					{
						Float lTValue = landTerrMap.get(tT);
						lTValue += targetMap.getInt(tT)*2;
						landTerrMap.put(tT, lTValue);
					}
				}
				
		*/
		// not used
		/*
		List<PlayerID> enemyplayers = SUtils.getEnemyPlayers(data, player);
		PlayerID ePlayer = enemyplayers.get(0);
		
		List<Territory> checkForInvasion = new ArrayList<Territory>();
		checkForInvasion.addAll(ourEnemyTerr);
		checkForInvasion.addAll(ourFriendlyTerr);
		checkForInvasion.removeAll(impassableTerrs);
		*/
		for (final Territory transTerr : occTransTerr)
		{
			final List<Unit> ourTransports = transTerr.getUnits().getMatches(transportingUnit);
			// valid here?
			ourTransports.removeAll(unitsAlreadyMoved);
			if (ourTransports.isEmpty())
				continue;
			final int tmpDistance = MoveValidator.getMaxMovement(ourTransports);
			// territories next to our maximum move. variable not used, can consolidate
			final Set<Territory> transTerrNeighbors = data.getMap().getNeighbors(transTerr, tmpDistance + 1);
			final List<Territory> tmpTerrList = new ArrayList<Territory>(transTerrNeighbors);
			final Iterator<Territory> tTIter = tmpTerrList.iterator();
			// remove invalid landing zones
			while (tTIter.hasNext())
			{
				final Territory tmpTerr = tTIter.next();
				if (Matches.TerritoryIsWater.match(tmpTerr) || Matches.TerritoryIsPassableAndNotRestricted(player, data).invert().match(tmpTerr)
							|| Matches.territoryHasWaterNeighbor(data).invert().match(tmpTerr))
					tTIter.remove();
			}
			// all territories on the map, ranked
			final HashMap<Territory, Float> landTerrMap2 = new HashMap<Territory, Float>(landTerrMap);
			int minDist = 0;
			final Iterator<Territory> cFIter = tmpTerrList.iterator();
			// loop landing zones
			while (cFIter.hasNext())
			{
				final Territory lT = cFIter.next();
				// remove friendly landing zones, with no enemy neighbor. that are not under threat.
				if (Matches.isTerritoryAllied(player, data).match(lT) && Matches.territoryHasEnemyLandNeighbor(data, player).invert().match(lT))
				{
					final float testPotential = SUtils.getStrengthOfPotentialAttackers(lT, data, player, tFirst, true, null);
					if (testPotential <= 1.0F)
					{
						cFIter.remove();
						continue;
					}
				}
				// remove friendly landing zones, invalidates above check.
				else if (Matches.isTerritoryAllied(player, data).match(lT))
				{
					cFIter.remove(); // List contains enemy Territories & Territories with EnemyLand Neighbor
					continue;
				}
				// remove if landing zone isn't in our ranked map, or closest unload point doesn't exist
				// this shouldn't be possible
				final Territory safeTerr = SUtils.getClosestWaterTerr(lT, transTerr, data, player, tFirst);
				if (safeTerr == null || !landTerrMap2.containsKey(lT))
				{
					cFIter.remove();
					continue;
				}
				// remove if we can't reach, this is not consistent
				minDist = data.getMap().getWaterDistance(transTerr, safeTerr);
				if (minDist > tmpDistance || minDist == -1)
				{
					cFIter.remove();
					continue;
				}
				// add 20 priority to main amphib route. this should be done at different scope perhaps?
				// check to make sure ranking hasn't already done it.
				if (lT.equals(amphibAttackTerr))
				{
					final Float amphibValue = landTerrMap2.get(lT) + 20.00F;
					landTerrMap2.put(lT, amphibValue);
				}
				// Float newVal = landTerrMap2.get(lT) - (minDist-1)*(targetTerritories.contains(lT) ? 1 : 2);
				// landTerrMap2.put(lT, newVal);
			} // end looping landing zones
			if (tmpTerrList.isEmpty())
				continue;
			SUtils.reorder(tmpTerrList, landTerrMap2, true);
			// if (ourTransports.isEmpty())
			// continue;
			// first pass, in theory tmpTerrList only contains valid landing zones, and is ordererd
			// neighbours of the current transport location
			final List<Territory> tTerrNeighbors = new ArrayList<Territory>(data.getMap().getNeighbors(transTerr));
			// potentially terribly slow
			tTerrNeighbors.removeAll(impassableTerrs);
			// duplicate tmpdistance earlier
			final int tDist = MoveValidator.getMaxMovement(ourTransports);
			final Iterator<Territory> targetIter = tmpTerrList.iterator();
			// boolean movedTransports = false;
			// loop landing zones again
			while (targetIter.hasNext())
			{
				final Territory targetTerr = targetIter.next();
				final List<Collection<Unit>> xUnits = new ArrayList<Collection<Unit>>();
				final List<Route> xRoutes = new ArrayList<Route>();
				final List<Unit> xMoved = new ArrayList<Unit>(unitsAlreadyMoved);
				float defendingStrength = 0.0F, planeStrength = 0.0F, blitzStrength = 0.0F, landStrength = 0.0F, BBStrength = 0.0F;
				float ourShipStrength = 0.0F;
				Territory targetTerr2 = null;
				// verify this is required, may invalidate tDist
				ourTransports.removeAll(unitsAlreadyMoved);
				if (ourTransports.isEmpty())
					break;
				final Iterator<Unit> tIter = ourTransports.iterator();
				// if we are next to the landing zone already (and it is enemy, which is invalid, we removed allieds earlier)
				if (tTerrNeighbors.contains(targetTerr) && Matches.isTerritoryEnemy(player, data).match(targetTerr))
				{
					// the unload route
					final Route shortRoute = data.getMap().getRoute(transTerr, targetTerr);
					// shouldn't be possible but ok.
					if (shortRoute != null)
					{ // discourage invasions that don't have staying potential...add 1/5 of return potential attack
						// assume this works correctly for enemy territories. it better.
						final float eShortPotential = SUtils.getStrengthOfPotentialAttackers(targetTerr, data, player, tFirst, true, alreadyAttacked);
						// units to unload
						final List<Unit> tLoadUnits = new ArrayList<Unit>();
						// enemy strength at landing zone
						final float eShortStrength = SUtils.strength(targetTerr.getUnits().getMatches(Matches.enemyUnit(player, data)), false, false, tFirst);
						// threshold, str at landing zone + 1/5 of potential attack
						final float goStrength = (eShortStrength + 0.2F * eShortPotential) * 1.45F + 2.0F;
						float ourQuickStrength = 0.0F;
						// transports to move
						final List<Unit> qtransMoved = new ArrayList<Unit>();
						// loop our transports
						while (tIter.hasNext() && ourQuickStrength < goStrength)
						{
							final Unit transport = tIter.next();
							final List<Unit> qUnits = TripleAUnit.get(transport).getTransporting();
							tLoadUnits.addAll(qUnits);
							ourQuickStrength += SUtils.strength(qUnits, true, false, tFirst);
							qtransMoved.add(transport);
						}
						if (ourQuickStrength == 0.0F)
							continue;
						final List<Unit> qAMoved = new ArrayList<Unit>(unitsAlreadyMoved);
						final List<Collection<Unit>> qunitMoved = new ArrayList<Collection<Unit>>();
						final List<Route> qRoutes = new ArrayList<Route>();
						float qPlaneStrength = 0.0F;
						float qBBStrength = 0.0F;
						float qBlitzStrength = 0.0F;
						float qLandStrength = 0.0F;
						// why different threshold? either way, invite other units to attack landing zone
						if (ourQuickStrength < (eShortStrength + 0.25F * eShortPotential))
						{
							qBBStrength = SUtils.inviteBBEscort(transTerr, goStrength - ourQuickStrength, qAMoved, qunitMoved, qRoutes, data, player);
							ourQuickStrength += qBBStrength;
							qPlaneStrength = SUtils.invitePlaneAttack(false, false, targetTerr, goStrength - ourQuickStrength, qAMoved, qunitMoved, qRoutes, data, player);
							ourQuickStrength += qPlaneStrength;
							qBlitzStrength = SUtils.inviteBlitzAttack(false, targetTerr, goStrength - ourQuickStrength, qAMoved, qunitMoved, qRoutes, data, player, true, true);
							ourQuickStrength += qBlitzStrength;
							qLandStrength = SUtils.inviteLandAttack(false, targetTerr, goStrength - ourQuickStrength, qAMoved, qunitMoved, qRoutes, data, player, false,
										Matches.territoryHasEnemyNonNeutralNeighborWithEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits).match(targetTerr), alreadyAttacked);
							ourQuickStrength += qLandStrength;
						}
						// yet another threshold. wtf, this is much higher
						if (ourQuickStrength >= (eShortStrength * 1.05F + 2.0F + eShortPotential))
						{
							// if we got some friends to attack add them
							if (qBBStrength + qPlaneStrength + qBlitzStrength + qLandStrength > 0.0F)
							{
								moveUnits.addAll(qunitMoved);
								moveRoutes.addAll(qRoutes);
								s_logger.finer("PTMa moving " + qunitMoved);
								s_logger.finer("PTMa route " + qRoutes);
								// for (Collection<Unit> goUnit : qunitMoved)
								unitsAlreadyMoved.addAll(qAMoved);
							}
							// Route qRoute = data.getMap().getRoute(transTerr, targetTerr);
							moveUnits.add(tLoadUnits);
							moveRoutes.add(shortRoute);
							s_logger.finer("PTMb moving " + tLoadUnits);
							s_logger.finer("PTMb route " + shortRoute);
							unitsAlreadyMoved.addAll(tLoadUnits);
							unitsAlreadyMoved.addAll(qtransMoved);
							// send all attackships that are with me, invalid. we are unloading
							// in theory bombarders have already been handled. ships to protect us should be handled elsewhere
							/*
							List<Unit> escorts = transTerr.getUnits().getMatches(escortUnit);
							escorts.removeAll(unitsAlreadyMoved);
							if (!escorts.isEmpty())
							{
								moveUnits.add(escorts);
								moveRoutes.add(qRoute);
								s_logger.finer("PTMc moving " +escorts);
								s_logger.finer("PTMc route " +qRoute);
								unitsAlreadyMoved.addAll(escorts);
							}
							*/
						}
					} // end shortroute null check
						// invalid, we already know we can't unload. is this intended
						// to unload to friendlies ? if so the code blocks are messed up, target removal above is bad.
					/*
					else if (tTerrNeighbors.contains(targetTerr))
					{
						float ePotential = SUtils.getStrengthOfPotentialAttackers(targetTerr, data, player, tFirst, true, null);
						float qStrength = SUtils.strength(targetTerr.getUnits().getUnits(), false, false, tFirst);
						float qStrengthNeeded = ePotential*1.45F + 3.0F;
						List<Unit> qLoadUnits = new ArrayList<Unit>();
						List<Unit> qTransUnits = new ArrayList<Unit>();
						while (tIter.hasNext() && qStrengthNeeded > qStrength)
						{
							Unit transport = tIter.next();
							List<Unit> qUnits = TripleAUnit.get(transport).getTransporting();
							qStrength += SUtils.strength(qUnits, false, false, tFirst);
							qLoadUnits.addAll(qUnits);
							qTransUnits.add(transport);
						}
						if (qTransUnits.isEmpty())
							continue;
						List<Unit> qAMoved = new ArrayList<Unit>();
						List<Collection<Unit>> qunitMoved = new ArrayList<Collection<Unit>>();
						List<Route> qRoutes = new ArrayList<Route>();
						float qPlaneStrength = 0.0F;
						float qBBStrength = 0.0F;
						float qBlitzStrength = 0.0F;
						float qLandStrength = 0.0F;
						if (qStrength < ePotential*0.65F)
						{
							qBBStrength = SUtils.inviteBBEscort(targetTerr, qStrengthNeeded - qStrength, qAMoved, qunitMoved, qRoutes, data, player);
							qStrength += qBBStrength;
							qPlaneStrength = SUtils.invitePlaneAttack(false, false, targetTerr, qStrengthNeeded - qStrength, qAMoved, qunitMoved, qRoutes, data, player);
							qStrength += qPlaneStrength;
							qBlitzStrength = SUtils.inviteBlitzAttack(false, targetTerr, qStrengthNeeded - qStrength, qAMoved, qunitMoved, qRoutes, data, player, true, true);
							qStrength += qBlitzStrength;
							qLandStrength = SUtils.inviteLandAttack(false, targetTerr, qStrengthNeeded - qStrength, qAMoved, qunitMoved, qRoutes, data, player, false, Matches.territoryHasEnemyFactoryNeighbor(data, player).match(targetTerr), alreadyAttacked);
							qStrength += qLandStrength;
						}
						if (qStrength > ePotential*0.65F)
						{
							Route qRoute = data.getMap().getRoute(transTerr, targetTerr);
							moveUnits.add(qLoadUnits);
							moveRoutes.add(qRoute);
							s_logger.finer("PTMd moving " +qLoadUnits);
							s_logger.finer("PTMd route " +qRoute);
							unitsAlreadyMoved.addAll(qLoadUnits);
							unitsAlreadyMoved.addAll(qTransUnits);
							if (qBBStrength + qPlaneStrength + qBlitzStrength + qLandStrength > 0.0F)
							{
								moveUnits.addAll(qunitMoved);
								moveRoutes.addAll(qRoutes);
								s_logger.finer("PTMe moving " +qunitMoved);
								s_logger.finer("PTMe route " +qRoutes);
								for (Collection<Unit> goUnit : qunitMoved)
									unitsAlreadyMoved.addAll(goUnit);
								List<Territory>escortTerrs = new ArrayList<Territory>();
								for (Route xRoute : qRoutes)
								{
									if (!escortTerrs.contains(xRoute.getEnd()))
										escortTerrs.add(xRoute.getEnd());
								}
								float xNeeded = 3.0F; //go simple for now
								for (Territory escortTerr : escortTerrs)
								{
									SUtils.inviteShipAttack(escortTerr, xNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, false, tFirst, false);
									s_logger.fine("PTMf inviting escorts?!" + escortTerr);
								}
							}
						}
					} */
				} // end we are next to the landing zone
				else
					targetTerr2 = SUtils.getSafestWaterTerr(targetTerr, transTerr, seaTerrAttacked, data, player, false, tFirst);
				// arbitrary distance check, read. move on to next landing zone if we can't move next to it.
				if (targetTerr2 == null || data.getMap().getWaterDistance(transTerr, targetTerr2) > 2)
					continue;
				final Route targetRoute = SUtils.getMaxSeaRoute(data, transTerr, targetTerr2, player, false, tDist);
				if (targetRoute == null || targetRoute.getEnd() == null)
				{
					continue;
				}
				final List<Unit> defendingUnits = new ArrayList<Unit>();
				float enemyStrengthAtTarget = 0.0F;
				if (tFirst)
					ourShipStrength = ourTransports.size() * 1.0F;
				final List<Unit> landUnits = new ArrayList<Unit>();
				final Iterator<Unit> transIter = ourTransports.iterator();
				while (transIter.hasNext())
				{
					// adds _all_ loaded units, check this is valid
					final Unit transport = transIter.next();
					// if (tracker.isTransporting(transport))
					landUnits.addAll(tracker.transporting(transport));
					// else
					// transIter.remove();
				}
				float ourInvasionStrength = SUtils.strength(landUnits, true, false, tFirst);
				float xDefendingPotential = 0.0F;
				// again we already removed allies, so this is invalid
				if (Matches.isTerritoryAllied(player, data).match(targetTerr))
				{
					defendingStrength = SUtils.getStrengthOfPotentialAttackers(targetTerr, data, player, tFirst, true, alreadyAttacked);
					final float alliedStrength = SUtils.strength(targetTerr.getUnits().getUnits(), false, false, tFirst);
					ourInvasionStrength += alliedStrength * 1.35F + 6.0F; // want to move in even when they have advantage
				}
				else
				{
					defendingUnits.addAll(targetTerr.getUnits().getMatches(Matches.enemyUnit(player, data)));
					defendingStrength = SUtils.strength(defendingUnits, false, false, tFirst);
					xDefendingPotential = SUtils.getStrengthOfPotentialAttackers(targetTerr, data, player, tFirst, true, alreadyAttacked);
					// threshold. different from nextto check. typical
					float minStrengthNeeded = (defendingStrength * 1.65F + 3.0F) - ourInvasionStrength + xDefendingPotential * 0.25F;
					// invite firends
					BBStrength = SUtils.inviteBBEscort(targetTerr2, minStrengthNeeded, xMoved, xUnits, xRoutes, data, player);
					minStrengthNeeded -= BBStrength;
					blitzStrength = SUtils.inviteBlitzAttack(false, targetTerr, minStrengthNeeded, xMoved, xUnits, xRoutes, data, player, true, false);
					minStrengthNeeded -= blitzStrength;
					landStrength = SUtils.inviteLandAttack(false, targetTerr, minStrengthNeeded, xMoved, xUnits, xRoutes, data, player, true, false, alreadyAttacked);
					minStrengthNeeded -= landStrength;
					planeStrength = SUtils.invitePlaneAttack(false, false, targetTerr, minStrengthNeeded, xMoved, xUnits, xRoutes, data, player);
					minStrengthNeeded -= planeStrength;
					ourInvasionStrength += BBStrength + blitzStrength + landStrength + planeStrength;
				}
				// boolean weAttacked = false;
				// total including all friends and our loaded units, again messed up threshold
				final boolean weCanWin = ourInvasionStrength > (defendingStrength * 1.10F + Math.max(0.25F * xDefendingPotential, 2.0F));
				/*				if (!weCanWin)
								{
									List<Unit> calcUnits = new ArrayList<Unit>(landUnits);
									for (Collection<Unit> xU : xUnits)
										calcUnits.addAll(xU);
									HashMap<PlayerID, IntegerMap<UnitType>> costMap = SUtils.getPlayerCostMap(data);
									weCanWin = SUtils.calculateTUVDifference(targetTerr, calcUnits, defendingUnits, costMap, player, data, aggressive, Properties.getAirAttackSubRestricted(data));
								}
				*/
				if (weCanWin)
				{
					float compareStrength = 0.0F;
					// calculate if we can win the sea battle,
					ourShipStrength += BBStrength * 2.0F;
					final List<Unit> enemyShipsAtTarget = targetTerr2.getUnits().getMatches(Matches.enemyUnit(player, data));
					enemyStrengthAtTarget = SUtils.strength(enemyShipsAtTarget, false, true, tFirst);
					;
					final List<Unit> shipsAtTarget = targetTerr2.getUnits().getMatches(Matches.alliedUnit(player, data));
					ourShipStrength += SUtils.strength(shipsAtTarget, false, true, tFirst);
					final float strengthDiff = enemyStrengthAtTarget - ourShipStrength;
					ourShipStrength += SUtils.inviteShipAttack(targetTerr2, strengthDiff * 2.5F, xMoved, xUnits, xRoutes, data, player, false, tFirst, false);
					// again with the inconsistent thresholds.
					compareStrength = ourShipStrength * 1.25F + (ourShipStrength > 2.0F ? 3.0F : 0.0F);
					// if we win the sea battle
					// this currently moves the things but doesn't unload them.
					// not consistent with first first part.
					// if we unload later, no doubt all this shit will be checked again
					if (enemyStrengthAtTarget <= compareStrength || enemyStrengthAtTarget < 2.0F)
					{// TODO: Limit transports to what is needed for amphibious attack
						alreadyAttacked.add(targetTerr); // consider as if we finished off targetTerr
						unitsAlreadyMoved.addAll(ourTransports);
						alreadyAttacked.add(targetTerr2);
						// weAttacked = true;
						amphibMap.put(targetTerr2, targetTerr);
						// bogus check, this shouldn't happen
						if (transTerr != targetTerr2)
						{
							ourTransports.addAll(landUnits);
							moveUnits.add(new LinkedList<Unit>(ourTransports));
							moveRoutes.add(targetRoute);
							s_logger.finer("PTM moving " + ourTransports + " To land at " + targetTerr);
							s_logger.finer("PTM route " + targetRoute);
						}
						if (xUnits.size() > 0)
						{
							// for (Collection<Unit> x1 : xUnits)
							moveUnits.addAll(xUnits);
							moveRoutes.addAll(xRoutes);
							s_logger.finer("PTM moving " + xUnits + "to assist in invasion of " + targetTerr);
							s_logger.finer("PTM route " + xRoutes);
							unitsAlreadyMoved.addAll(xMoved);
						}
						// don't move ships already at the battle that we need.
						// this shouldn't be done here. pretty odd
						// previous attackers will not be moved anyway in theory
						// will prevent submerged subs moving, but only during this method
						// and submerged subs should already have done something during combatsea
						if (enemyStrengthAtTarget > 2.0F)
						{
							float checkStrength = ourShipStrength - SUtils.strength(shipsAtTarget, false, true, tFirst);
							final List<Unit> markUnits = new ArrayList<Unit>();
							final Iterator<Unit> shipIter = shipsAtTarget.iterator();
							while (shipIter.hasNext() && (checkStrength * 1.20F + 2.0F) < enemyStrengthAtTarget)
							{
								final Unit unitX = shipIter.next();
								checkStrength += SUtils.uStrength(unitX, false, true, tFirst);
								markUnits.add(unitX);
							}
							if (markUnits.size() > 0)
								unitsAlreadyMoved.addAll(markUnits);
						}
						// movedTransports = true;
					}
				}
				// confirm correct usage of alreadyattacked. at the very least it
				// isn't updated in first case.
				// if (!weAttacked)
				// alreadyAttacked.remove(targetTerr);
			} // end looping landing zones again
		}
		setAmphibMap(amphibMap);
		/*		if (isAmphib)
				{
					s_logger.fine("Player: "+player.getName());
					s_logger.fine("Units: "+moveUnits);
					s_logger.fine("Routes: "+moveRoutes);
				}
		*/
	}
	
	private void amphibMapUnload(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		final TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		final CompositeMatch<Unit> transportingUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsTransport, Matches.transportIsTransporting());
		final HashMap<Territory, Territory> amphibMap = getAmphibMap();
		final Set<Territory> invadeFrom = amphibMap.keySet();
		for (final Territory transTerr : invadeFrom)
		{
			final Territory targetTerr = amphibMap.get(transTerr);
			final List<Unit> transports = transTerr.getUnits().getMatches(transportingUnit);
			final List<Unit> tUnits = new ArrayList<Unit>();
			for (final Unit transport : transports)
				tUnits.addAll(tracker.transporting(transport));
			final Route tRoute = data.getMap().getRoute(transTerr, targetTerr);
			if (tRoute != null && tRoute.getLength() == 1)
			{
				moveUnits.add(tUnits);
				moveRoutes.add(tRoute);
			}
		}
	}
	
	/**
	 * Unload Transports in Non-combat phase
	 * Setup for first pass to unload transports which have moved
	 * 
	 * @param onlyMoved
	 *            - only transports which moved previously and are loaded
	 * @param nonCombat
	 * @param data
	 * @param moveUnits
	 * @param moveRoutes
	 * @param player
	 */
	private void populateTransportUnloadNonCom(final boolean onlyMoved, final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		final TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		CompositeMatch<Unit> transUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsTransport);
		if (onlyMoved)
			transUnit = new CompositeMatchAnd<Unit>(transUnit, Matches.unitHasMoved);
		// CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand);
		final List<Territory> transTerr = SUtils.findTersWithUnitsMatching(data, player, transUnit);
		if (transTerr.isEmpty())
			return;
		final List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
		final Territory capitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		final boolean capDanger = getCapDanger();
		final boolean tFirst = transportsMayDieFirst();
		// List<Territory> threats = new ArrayList<Territory>();
		// boolean alliedCapDanger = SUtils.threatToAlliedCapitals(data, player, threats, tFirst);
		final List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
		// List<Territory> ourEnemyTerr = new ArrayList<Territory>();
		// HashMap<Territory, Float> rankMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, tFirst, true, true);
		final HashMap<Territory, Float> rankMap = SUtils.rankAmphibReinforcementTerritories(data, null, player, tFirst);
		if (!capDanger)
		{
			rankMap.remove(capitol);
			ourFriendlyTerr.remove(capitol);
		}
		final List<Territory> ourTerrNextToEnemyTerr = SUtils.getTerritoriesWithEnemyNeighbor(data, player, true, false);
		SUtils.removeNonAmphibTerritories(ourTerrNextToEnemyTerr, data);
		if (ourTerrNextToEnemyTerr.size() > 1)
			SUtils.reorder(ourTerrNextToEnemyTerr, rankMap, true);
		for (final Territory xT : transTerr)
		{
			final List<Territory> xTNeighbors = new ArrayList<Territory>(data.getMap().getNeighbors(xT, Matches.isTerritoryAllied(player, data)));
			xTNeighbors.retainAll(ourTerrNextToEnemyTerr);
			if (xTNeighbors.isEmpty())
				continue;
			SUtils.reorder(xTNeighbors, rankMap, true);
			final Territory landingTerr = xTNeighbors.get(0); // put them all here... TODO: check for need
			final Route landingRoute = data.getMap().getRoute(xT, landingTerr);
			final List<Unit> transUnits = xT.getUnits().getMatches(transUnit);
			final Iterator<Unit> tIter = transUnits.iterator();
			final List<Unit> landingUnits = new ArrayList<Unit>();
			while (tIter.hasNext())
			{
				final Unit transport = tIter.next();
				if (tracker.transporting(transport) != null)
					landingUnits.addAll(tracker.transporting(transport));
			}
			if (landingUnits.isEmpty())
				continue;
			moveUnits.add(landingUnits);
			moveRoutes.add(landingRoute);
			unitsAlreadyMoved.addAll(landingUnits);
		}
		/*        SUtils.reorderTerrByFloat(ourFriendlyTerr, rankMap, true);
				for (Territory t: transTerr)
				{
					List<Unit> transUnits = t.getUnits().getMatches(transUnit);
					Iterator<Unit> transIter = transUnits.iterator();
					List<Unit> ourUnits = new ArrayList<Unit>();
					while (transIter.hasNext())
					{
						Unit transport = transIter.next();
						ourUnits.addAll(tracker.transporting(transport));
					}
					if (ourUnits.size() == 0)
						continue;
					float addStrength = SUtils.strength(ourUnits, false, false, tFirst);
					Territory landOn = null;
					List<Territory> tNeighbors = SUtils.getNeighboringLandTerritories(data, player, t);
					if (tNeighbors.isEmpty())
						continue;
					if (!capDanger)
						tNeighbors.remove(capitol);
					SUtils.reorderTerrByFloat(tNeighbors, rankMap, true);
					Iterator<Territory> nIter = tNeighbors.iterator();
					while (nIter.hasNext() && landOn == null)
					{
						Territory thisTerr = nIter.next();
						float enemyStrength = SUtils.getStrengthOfPotentialAttackers(thisTerr, data, player, tFirst, true, null);
						float ourStrength = SUtils.strength(thisTerr.getUnits().getMatches(Matches.alliedUnit(player, data)), false, false, tFirst);
						if ((ourStrength+addStrength) > enemyStrength*0.45F || (SUtils.territoryHasThreatenedAlliedFactoryNeighbor(data, thisTerr, player)))
							landOn = thisTerr;
					}
					if (landOn == null && !onlyMoved && !tNeighbors.isEmpty())
						landOn = tNeighbors.get(0);
					if (landOn != null)
					{
		            	Route route = new Route();
		            	route.setStart(t);
		            	route.add(landOn);
		            	moveUnits.add(ourUnits);
		            	moveRoutes.add(route);
					}
				}
		*/
	}
	
	private void populateTransportUnload(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		// setImpassableTerrs(player);
		// Collection<Territory> impassableTerrs = getImpassableTerrs();
		final boolean tFirst = transportsMayDieFirst();
		final TransportTracker tTracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		final int size = data.getMap().getTerritories().size();
		final Territory eTerr[] = new Territory[size]; // revised game has 79 territories and 64 sea zones
		final float eStrength[] = new float[size];
		float eS = 0.00F;
		final CompositeMatch<Unit> enemyUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data));
		final CompositeMatch<Unit> landAndOwned = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.unitIsOwnedBy(player));
		final CompositeMatch<Unit> landAndEnemy = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, enemyUnit);
		final CompositeMatch<Unit> airEnemyUnit = new CompositeMatchAnd<Unit>(enemyUnit, Matches.UnitIsAir);
		final CompositeMatch<Unit> landOrAirEnemy = new CompositeMatchOr<Unit>(landAndEnemy, airEnemyUnit);
		final CompositeMatch<Unit> transportingUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsTransport, Transporting);
		final CompositeMatch<Unit> enemyfactories = new CompositeMatchAnd<Unit>(Matches.UnitCanProduceUnits, enemyUnit);
		final CompositeMatch<Unit> transporting = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.transportIsTransporting());
		float remainingStrength = 100.0F;
		final List<Territory> transTerr = SUtils.findTersWithUnitsMatching(data, player, transporting);
		if (transTerr.isEmpty())
			return;
		final List<Territory> enemyCaps = SUtils.findUnitTerr(data, player, enemyfactories);
		final List<Territory> tempECaps = new ArrayList<Territory>(enemyCaps);
		final List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
		final List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
		final List<Territory> ourEnemyTerr = new ArrayList<Territory>();
		final List<Territory> alreadyAttacked = new ArrayList<Territory>();
		final HashMap<Territory, Float> rankMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, tFirst, true, false);
		// List<Territory> goTerr = new ArrayList<Territory>(rankMap.keySet());
		// SUtils.reorder(goTerr, rankMap, true);
		final CompositeMatch<Territory> enemyLand = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemy(player, data), Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable);
		for (final Territory qT : tempECaps) // add all neighbors
		{
			final Set<Territory> nTerr = data.getMap().getNeighbors(qT, enemyLand);
			if (nTerr.size() > 0)
				enemyCaps.addAll(nTerr);
		}
		// int maxCap = enemyCaps.size() - 2;
		// if (maxPasses < maxCap)
		// maxPasses=1;
		// Territory tempTerr = null, tempTerr2 = null;
		enemyCaps.retainAll(rankMap.keySet());
		SUtils.reorder(enemyCaps, rankMap, true);
		/*
		 * Search list: Production Value...capitals all have high values
		 * if friendly, dump there first
		 * if enemy, but we are stronger...dump next
		 * enemy capital will always get the first look
		 */
		final List<Unit> xAlreadyMoved = new ArrayList<Unit>();
		final List<Collection<Unit>> xMoveUnits = new ArrayList<Collection<Unit>>();
		final List<Route> xMoveRoutes = new ArrayList<Route>();
		for (final Territory eC : enemyCaps) // priority...send units into capitals & capneighbors when possible
		// for (Territory eC : goTerr)
		{
			final Set<Territory> neighborTerr = data.getMap().getNeighbors(eC, Matches.TerritoryIsWater);
			final List<Unit> capUnits = eC.getUnits().getMatches(landOrAirEnemy);
			final float capStrength = SUtils.strength(capUnits, false, false, tFirst);
			float invadeStrength = SUtils.strength(eC.getUnits().getMatches(Matches.unitIsOwnedBy(player)), true, false, tFirst);
			if (Matches.isTerritoryFriendly(player, data).match(eC))
			{
				for (final Territory nF : neighborTerr)
				{
					final List<Unit> quickLandingUnits = new ArrayList<Unit>();
					final List<Unit> nFTrans = nF.getUnits().getMatches(transportingUnit);
					final Iterator<Unit> nFIter = nFTrans.iterator();
					while (nFIter.hasNext())
					{
						final Unit transport = nFIter.next();
						quickLandingUnits.addAll(tTracker.transporting(transport));
					}
					final Route quickLandRoute = new Route();
					quickLandRoute.setStart(nF);
					quickLandRoute.add(eC);
					// if (quickLandRoute != null)
					// {
					moveUnits.add(quickLandingUnits);
					moveRoutes.add(quickLandRoute);
					unitsAlreadyMoved.addAll(quickLandingUnits);
					if (transTerr.contains(nF))
						transTerr.remove(nF);
					// }
				}
			}
			for (final Territory nT : neighborTerr)
			{
				if (nT.getUnits().someMatch(transportingUnit))
				{
					final List<Unit> specialLandUnits = nT.getUnits().getMatches(landAndOwned);
					specialLandUnits.removeAll(unitsAlreadyMoved);
					if (specialLandUnits.isEmpty())
						continue;
					invadeStrength = SUtils.strength(specialLandUnits, true, false, tFirst);
					final Set<Territory> attackNeighbors = data.getMap().getNeighbors(eC, Matches.isTerritoryFriendly(player, data));
					float localStrength = 0.0F;
					for (final Territory aN : attackNeighbors)
					{
						if (aN.isWater()) // don't count anything from water
							continue;
						final List<Unit> localUnits = aN.getUnits().getMatches(landAndOwned);
						localUnits.removeAll(unitsAlreadyMoved);
						if (localUnits.isEmpty())
							continue;
						localStrength += SUtils.strength(localUnits, true, false, tFirst);
						xMoveUnits.add(localUnits);
						final Route localRoute = data.getMap().getLandRoute(aN, eC);
						xMoveRoutes.add(localRoute);
						xAlreadyMoved.addAll(localUnits);
					}
					final float ourStrength = invadeStrength + localStrength;
					remainingStrength = (capStrength * 2.20F + 5.00F) - ourStrength;
					xAlreadyMoved.addAll(unitsAlreadyMoved);
					final float blitzStrength = SUtils.inviteBlitzAttack(false, eC, remainingStrength, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, true, true);
					remainingStrength -= blitzStrength;
					final float planeStrength = SUtils.invitePlaneAttack(false, false, eC, remainingStrength, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player);
					remainingStrength -= planeStrength;
					final List<Territory> alliedTerr = SUtils.getNeighboringLandTerritories(data, player, eC);
					float alliedStrength = 0.0F;
					final CompositeMatch<Unit> alliedButNotMyUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player).invert(), Matches.alliedUnit(player, data));
					for (final Territory aCheck : alliedTerr)
						alliedStrength += SUtils.strength(aCheck.getUnits().getMatches(alliedButNotMyUnit), true, false, tFirst);
					final float attackFactor = (alliedStrength > 0.75F * capStrength) ? 0.92F : 1.04F; // let retreat handle this
					if ((invadeStrength + localStrength + blitzStrength + planeStrength) >= attackFactor * capStrength)
					{
						final Route specialRoute = data.getMap().getRoute(nT, eC);
						moveUnits.add(specialLandUnits);
						moveRoutes.add(specialRoute);
						unitsAlreadyMoved.addAll(specialLandUnits);
						moveUnits.addAll(xMoveUnits);
						moveRoutes.addAll(xMoveRoutes);
						for (final Collection<Unit> xCollect : xMoveUnits)
							unitsAlreadyMoved.addAll(xCollect);
						if (transTerr.contains(nT))
							transTerr.remove(nT);
						alreadyAttacked.add(eC);
					}
				}
				xMoveUnits.clear();
				xMoveRoutes.clear();
				xAlreadyMoved.clear();
			}
		}
		for (final Territory t : transTerr) // complete check
		{
			final List<Unit> transUnits = t.getUnits().getMatches(transportingUnit);
			transUnits.removeAll(unitsAlreadyMoved);
			final List<Unit> units = t.getUnits().getMatches(landAndOwned);
			units.removeAll(unitsAlreadyMoved);
			float ourStrength = SUtils.strength(units, true, false, tFirst);
			if (units.size() == 0)
				continue;
			final List<Territory> enemy = SUtils.getNeighboringEnemyLandTerritories(data, player, t);
			final List<Territory> enemyCopy = new ArrayList<Territory>(enemy);
			// List<Unit> alreadyOut = new ArrayList<Unit>();
			// quick check for empty territories
			final Map<Unit, Collection<Unit>> transMap = tTracker.transporting(transUnits);
			int i = 0;
			for (final Territory t2 : enemy) // find strength of all enemy terr (defensive)
			{
				eTerr[i] = t2;
				eStrength[i] = SUtils.strength(t2.getUnits().getMatches(landOrAirEnemy), false, false, tFirst);
				eStrength[i] -= SUtils.strength(t2.getUnits().getMatches(Matches.unitIsOwnedBy(player)), true, false, tFirst);
				i++;
			}
			float tmpStrength = 0.0F;
			Territory tmpTerr = null;
			for (int j2 = 0; j2 < i - 1; j2++) // sort the territories by strength
			{
				tmpTerr = eTerr[j2];
				tmpStrength = eStrength[j2];
				final Set<Territory> badFactTerr = data.getMap().getNeighbors(tmpTerr, Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits));
				if ((badFactTerr.size() > 0) && (tmpStrength * 1.10F + 5.00F) <= eStrength[j2 + 1])
					continue; // if it is next to a factory, don't move it down
				if (tmpStrength < eStrength[j2 + 1])
				{
					eTerr[j2] = eTerr[j2 + 1];
					eStrength[j2] = eStrength[j2 + 1];
					eTerr[j2 + 1] = tmpTerr;
					eStrength[j2 + 1] = tmpStrength;
				}
			}
			// Consideration: There might be a land based invasion of an empty terr available
			for (final Territory x : enemyCopy)
			{
				if (Matches.isTerritoryEnemy(player, data).match(x) && Matches.territoryIsEmptyOfCombatUnits(data, player).match(x))
				{
					float topStrength = eStrength[0];
					float winStrength = 0.0F;
					final float newStrength = ourStrength;
					for (int jC = 0; jC < enemy.size() - 1; jC++)
					{
						if (!enemy.contains(eTerr[jC]))
							continue;
						topStrength = eStrength[jC];
						if (newStrength > topStrength && winStrength == 0.0F) // what we can currently win
							winStrength = topStrength;
					}
					final Iterator<Unit> transIter = transUnits.iterator();
					boolean gotOne = false;
					while (transIter.hasNext() && !gotOne)
					{
						final Unit transport = transIter.next();
						if (!tTracker.isTransporting(transport) || transport == null)
							continue;
						final Collection<Unit> transportUnits = transMap.get(transport);
						if (transportUnits == null)
							continue;
						final float minusStrength = SUtils.strength(transportUnits, true, false, tFirst);
						if ((newStrength - minusStrength) > winStrength)
						{
							final Route xRoute = data.getMap().getRoute(t, x);
							moveUnits.add(transportUnits);
							moveRoutes.add(xRoute);
							unitsAlreadyMoved.addAll(transportUnits);
							enemy.remove(x);
							ourStrength -= minusStrength;
							gotOne = true;
							alreadyAttacked.add(x);
						}
					}
				}
			}
			for (int j = 0; j < i; j++) // just find the first terr we can invade
			{
				units.removeAll(unitsAlreadyMoved);
				xAlreadyMoved.addAll(unitsAlreadyMoved);
				float ourStrength2 = ourStrength;
				eS = eStrength[j];
				final Territory invadeTerr = eTerr[j];
				if (!enemy.contains(invadeTerr))
					continue;
				final float strengthNeeded = 2.15F * eS + 3.00F;
				final float airStrength = 0.0F;
				ourStrength2 += airStrength;
				float rStrength = strengthNeeded - ourStrength2;
				final float planeStrength = SUtils.invitePlaneAttack(true, false, invadeTerr, rStrength, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player);
				rStrength -= planeStrength;
				final float blitzStrength = SUtils.inviteBlitzAttack(false, invadeTerr, rStrength, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, true, false);
				rStrength -= blitzStrength;
				final float landStrength = SUtils.inviteLandAttack(false, invadeTerr, rStrength, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, true, false, alreadyAttacked);
				/*				List<Unit> aBattleUnit = new ArrayList<Unit>(units);
								for (Collection<Unit> qUnits : xMoveUnits)
									aBattleUnit.addAll(qUnits);
								aBattleUnit.addAll(invadeTerr.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
								List<Unit> dBattleUnit = invadeTerr.getUnits().getMatches(landOrAirEnemy);
								IntegerMap<UnitType> aMap = SUtils.convertListToMap(aBattleUnit);
								IntegerMap<UnitType> dMap = SUtils.convertListToMap(dBattleUnit);
								boolean weWin = SUtils.quickBattleEstimator(aMap, dMap, player, invadeTerr.getOwner(), false, Properties.getAirAttackSubRestricted(data));
				*/
				final boolean weWin = (planeStrength + blitzStrength + landStrength + ourStrength) > (eS * 1.15F + 2.0F);
				/**
				 * Invade if we should win...or we should barely lose (enemy projected to only have 1 remaining defender)
				 */
				// if (weWin || (dMap.totalValues()==1 && dBattleUnit.size() > 3))
				if (weWin)
				{
					final Route route = new Route();
					route.setStart(t);
					route.add(invadeTerr);
					moveUnits.add(units);
					moveRoutes.add(route);
					unitsAlreadyMoved.addAll(units);
					moveUnits.addAll(xMoveUnits);
					moveRoutes.addAll(xMoveRoutes);
					for (final Collection<Unit> xCollect : xMoveUnits)
						unitsAlreadyMoved.addAll(xCollect);
					alreadyAttacked.add(invadeTerr);
				}
				xAlreadyMoved.clear();
				xMoveUnits.clear();
				xMoveRoutes.clear();
			}
		}
	}
	
	/*
	private List<Unit> load2Transports(boolean reload, GameData data, List<Unit> transportsToLoad, Territory loadFrom, PlayerID player)
	{
		TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		List<Unit> units = new ArrayList<Unit>();
		for (Unit transport : transportsToLoad)
		{
			Collection<Unit> landunits = tracker.transporting(transport);
			for (Unit u : landunits)
			{
				units.add(u);
			}
		}
		return units;
	}
	*/
	private void doMove(final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final List<Collection<Unit>> transportsToLoad, final IMoveDelegate moveDel)
	{
		for (int i = 0; i < moveRoutes.size(); i++)
		{
			pause();
			if (moveRoutes.get(i) == null || moveRoutes.get(i).getEnd() == null || moveRoutes.get(i).getStart() == null)
			{
				s_logger.fine("Route not valid" + moveRoutes.get(i) + " units:" + moveUnits.get(i));
				continue;
			}
			String result;
			if (transportsToLoad == null)
			{
				result = moveDel.move(moveUnits.get(i), moveRoutes.get(i));
			}
			else
				result = moveDel.move(moveUnits.get(i), moveRoutes.get(i), transportsToLoad.get(i));
			if (result != null)
			{
				s_logger.fine("could not move " + moveUnits.get(i) + " over " + moveRoutes.get(i) + " because : " + result + "\n");
			}
		}
	}
	
	private boolean markFactoryUnits(final GameData data, final PlayerID player, final Collection<Unit> unitsAlreadyMoved)
	{
		final HashMap<Territory, Float> capMap = determineCapDanger(player, data);
		final Territory myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		final List<Unit> myCapUnits = myCapital.getUnits().getMatches(Matches.unitIsOwnedBy(player));
		final List<Unit> alliedCapUnits = myCapital.getUnits().getMatches(Matches.alliedUnit(player, data));
		final float alliedCapStrength = SUtils.strength(alliedCapUnits, false, false, true);
		final Iterator<Unit> capUnitIter = myCapUnits.iterator();
		final float actualAlliedStrength = alliedCapStrength - SUtils.strength(myCapUnits, false, false, true);
		float capStrengthNeeded = capMap.get(myCapital) - actualAlliedStrength;
		while (capUnitIter.hasNext() && capStrengthNeeded > 0.0F)
		{
			final Unit capUnit = capUnitIter.next();
			capStrengthNeeded -= SUtils.uStrength(capUnit, false, false, true);
			unitsAlreadyMoved.add(capUnit);
		}
		final boolean capDanger = capStrengthNeeded > 0.0F;
		return capDanger;
	}
	
	/**
	 * Add all ships around a factory into a group which is not be moved
	 * 
	 * @param data
	 * @param player
	 * @param alreadyMoved
	 *            - List of units to be modified
	 */
	private void markBaseShips(final GameData data, final PlayerID player, final List<Unit> alreadyMoved)
	{
		if (getKeepShipsAtBase() && getSeaTerr() != null)
		{
			final Set<Territory> baseTerrs = data.getMap().getNeighbors(getSeaTerr(), Matches.TerritoryIsWater);
			for (final Territory bT : baseTerrs)
			{
				alreadyMoved.addAll(bT.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
			}
		}
	}
	
	private void specialPlaneAttack(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		// setImpassableTerrs(player);
		// Collection<Territory> impassableTerrs = getImpassableTerrs();
		final Collection<Unit> alreadyMoved = new HashSet<Unit>();
		// Territory myCapital = TerritoryAttachment.getCapital(player, data);
		final boolean tFirst = transportsMayDieFirst();
		final Match<Unit> notAlreadyMoved = new CompositeMatchAnd<Unit>(new Match<Unit>()
		{
			@Override
			public boolean match(final Unit o)
			{
				return !alreadyMoved.contains(o);
			}
		});
		final Match<Unit> ownedUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
		final Match<Unit> HasntMoved2 = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.unitHasNotMoved, notAlreadyMoved);
		final Match<Unit> ownedAndNotMoved = new CompositeMatchAnd<Unit>(ownedUnit, Matches.unitHasNotMoved);
		final Match<Unit> airAttackUnit = new CompositeMatchAnd<Unit>(ownedAndNotMoved, Matches.UnitIsAir);
		final Match<Unit> enemySubUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsSub);
		final Match<Unit> fighterUnit = new CompositeMatchAnd<Unit>(Matches.UnitCanLandOnCarrier, HasntMoved2);
		final Match<Unit> bomberUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsStrategicBomber, HasntMoved2);
		final Match<Unit> destroyerUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsDestroyer);
		final CompositeMatch<Territory> noEnemyAA = new CompositeMatchAnd<Territory>(Matches.territoryHasEnemyAAforCombatOnly(player, data).invert(), Matches.TerritoryIsNotImpassable);
		// Check to see if we have total air superiority...4:1 or greater...if so, let her rip
		final List<Territory> myAirTerr = SUtils.findUnitTerr(data, player, airAttackUnit);
		final List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
		float planeStrength = 0.0F, shipStrength = 0.0F;
		for (final Territory AttackFrom : myAirTerr)
		{
			final List<Unit> myFighters = AttackFrom.getUnits().getMatches(fighterUnit);
			// int fighterCount = myFighters.size();
			float myFighterStrength = SUtils.strength(myFighters, true, false, false);
			final List<Unit> myBombers = AttackFrom.getUnits().getMatches(bomberUnit);
			float myBomberStrength = SUtils.strength(myBombers, true, false, false);
			// int bomberCount = myBombers.size();
			float myTotalStrength = myFighterStrength + myBomberStrength;
			final Set<Territory> myNeighbors = data.getMap().getNeighbors(AttackFrom);
			final Set<Territory> enemyNeighbors = data.getMap().getNeighbors(AttackFrom, Matches.territoryHasEnemyUnits(player, data));
			for (final Territory check2 : myNeighbors)
			{
				final Set<Territory> check2Terr = data.getMap().getNeighbors(check2, Matches.territoryHasEnemyUnits(player, data));
				if (check2Terr != null && check2Terr.size() > 0)
				{
					for (final Territory enemyOnly : check2Terr)
					{
						if (!enemyNeighbors.contains(enemyOnly))
							enemyNeighbors.add(enemyOnly);
					}
				}
			}
			final List<Territory> waterEnemies = new ArrayList<Territory>();
			for (final Territory w : enemyNeighbors)
			{
				if (w.isWater())
				{
					waterEnemies.add(w);
					final List<Unit> eUnits = w.getUnits().getMatches(Matches.enemyUnit(player, data));
					final float waterStrength = SUtils.strength(eUnits, false, true, tFirst);
					float ourWaterStrength = 0.0F;
					if (w.getUnits().allMatch(enemySubUnit) && Properties.getAirAttackSubRestricted(data))
					{ // need a destroyer
						final List<Territory> destroyerTerr = SUtils.findOurShips(w, data, player, destroyerUnit);
						boolean dAttacked = false;
						// float dStrength = 0.0F;
						if (destroyerTerr.size() > 0)
						{
							for (final Territory dT : destroyerTerr)
							{
								final List<Unit> destroyers = dT.getUnits().getMatches(destroyerUnit);
								final int dDist = MoveValidator.getLeastMovement(destroyers);
								final Route dRoute = SUtils.getMaxSeaRoute(data, dT, w, player, true, dDist);
								if (dRoute == null || dRoute.getLength() > 2)
									continue;
								final List<Unit> dUnits = new ArrayList<Unit>();
								for (final Unit d : destroyers)
								{
									if (MoveValidator.hasEnoughMovement(d, dRoute))
									{
										dUnits.add(d);
									}
								}
								if (dUnits.size() > 0)
								{
									moveUnits.add(dUnits);
									moveRoutes.add(dRoute);
									unitsAlreadyMoved.addAll(dUnits);
									dAttacked = true;
									ourWaterStrength += SUtils.strength(dUnits, true, false, tFirst);
								}
							}
						}
						if (dAttacked)
						{
							float stillNeeded = waterStrength * 2.25F + 4.00F - ourWaterStrength;
							planeStrength = SUtils.invitePlaneAttack(false, false, w, stillNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player);
							stillNeeded -= planeStrength;
						}
					}
					else
					{
						float stillNeeded = waterStrength * 2.25F + 4.00F;
						final List<Collection<Unit>> xMoveUnits = new ArrayList<Collection<Unit>>();
						final List<Route> xMoveRoutes = new ArrayList<Route>();
						final List<Unit> xMoved = new ArrayList<Unit>(unitsAlreadyMoved);
						planeStrength = SUtils.invitePlaneAttack(false, false, w, stillNeeded, xMoved, xMoveUnits, xMoveRoutes, data, player);
						stillNeeded -= planeStrength;
						shipStrength = SUtils.inviteShipAttack(w, stillNeeded, xMoved, xMoveUnits, xMoveRoutes, data, player, true, tFirst, false);
						stillNeeded -= shipStrength;
						if (stillNeeded <= 1.0F)
						{
							moveUnits.addAll(xMoveUnits);
							moveRoutes.addAll(xMoveRoutes);
							for (final Collection<Unit> qUnits : xMoveUnits)
								unitsAlreadyMoved.addAll(qUnits);
						}
					}
				}
			}
			enemyNeighbors.removeAll(waterEnemies);
			myFighters.removeAll(unitsAlreadyMoved);
			myBombers.removeAll(unitsAlreadyMoved);
			myFighterStrength = SUtils.strength(myFighters, true, false, tFirst);
			myBomberStrength = SUtils.strength(myBombers, true, false, tFirst);
			myTotalStrength = myFighterStrength + myBomberStrength;
			// fighterCount = myFighters.size();
			// bomberCount = myBombers.size();
			// if (enemyNeighbors != null)
			// {
			for (final Territory badGuys : enemyNeighbors)
			{
				final List<Unit> enemyUnits = badGuys.getUnits().getMatches(Matches.enemyUnit(player, data));
				float badGuyStrength = 0.0F;
				if (badGuys.isWater())
					badGuyStrength = SUtils.strength(enemyUnits, false, true, tFirst);
				else
					badGuyStrength = SUtils.strength(enemyUnits, false, false, tFirst);
				final int badGuyCount = enemyUnits.size();
				final float needStrength = 2.4F * badGuyStrength + 3.00F;
				float actualStrength = 0.0F;
				final List<Unit> myAttackers = new ArrayList<Unit>();
				final List<Unit> myAttackers2 = new ArrayList<Unit>();
				final List<Unit> allUnits = new ArrayList<Unit>();
				allUnits.addAll(myFighters);
				allUnits.addAll(myBombers);
				// IntegerMap<UnitType> attackTypes = SUtils.convertListToMap(allUnits);
				// IntegerMap<UnitType> badTypes = SUtils.convertListToMap(enemyUnits);
				final HashMap<PlayerID, IntegerMap<UnitType>> costMap = SUtils.getPlayerCostMap(data);
				final boolean weWinTUV = SUtils.calculateTUVDifference(badGuys, allUnits, enemyUnits, costMap, player, data, false, Properties.getAirAttackSubRestricted(data), tFirst);
				if (myTotalStrength > needStrength && weWinTUV)
				{
					int actualAttackers = 0;
					final Route myRoute = data.getMap().getRoute(AttackFrom, badGuys, noEnemyAA);
					if (myRoute == null || myRoute.getEnd() == null)
						continue;
					if (!myFighters.isEmpty() && AirMovementValidator.canLand(myFighters, myRoute.getEnd(), player, data))
					{
						for (final Unit f : myFighters)
						{
							if (actualStrength < needStrength)
							{
								myAttackers.add(f);
								actualStrength += SUtils.airstrength(f, true);
								actualAttackers++;
							}
						}
						if (actualAttackers > 0 && actualStrength > needStrength) // && myRoute != null
						{
							moveUnits.add(myAttackers);
							moveRoutes.add(myRoute);
							alreadyMoved.addAll(myAttackers);
							myFighters.removeAll(myAttackers);
						}
					}
					if ((actualStrength > needStrength && (actualAttackers > badGuyCount + 1)) || myBombers.size() == 0 || myRoute.getEnd() == null) // || myRoute == null
						continue;
					if (!myBombers.isEmpty() && AirMovementValidator.canLand(myBombers, myRoute.getEnd(), player, data))
					{
						for (final Unit b : myBombers)
						{
							if (actualStrength < needStrength || (actualAttackers <= badGuyCount + 1))
							{
								myAttackers2.add(b);
								actualStrength += SUtils.airstrength(b, true);
							}
						}
						if (myAttackers.size() > 0) // && myRoute != null
						{
							moveUnits.add(myAttackers2);
							moveRoutes.add(myRoute);
							alreadyMoved.addAll(myAttackers2);
							myBombers.removeAll(myAttackers2);
						}
					}
				}
			}
			// }
		}
	}
	
	private void protectOurAllies(final boolean nonCombat, final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		final CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand, Matches.UnitIsNotInfrastructure);
		final CompositeMatch<Unit> carrierUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsCarrier);
		final CompositeMatch<Unit> fighterUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanLandOnCarrier);
		if (!nonCombat)
		{
			landUnit.add(Matches.UnitCanNotMoveDuringCombatMove.invert());
			carrierUnit.add(Matches.UnitCanNotMoveDuringCombatMove.invert());
			fighterUnit.add(Matches.UnitCanNotMoveDuringCombatMove.invert());
		}
		final List<Territory> threats = new ArrayList<Territory>();
		final boolean tFirst = transportsMayDieFirst(), noncombat = true;
		final List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
		// boolean capDanger = markFactoryUnits(data, player, unitsAlreadyMoved);
		final Territory myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		final boolean alliedCapDanger = SUtils.threatToAlliedCapitals(data, player, threats, tFirst);
		final List<Territory> seaTerrAttacked = getSeaTerrAttacked();
		final List<Territory> alreadyAttacked = Collections.emptyList();
		if (alliedCapDanger)
		{
			final List<Territory> threatRemoved = new ArrayList<Territory>();
			// first, can we take out any of the threats?
			float planeStrength = 0.0F;
			for (final Territory threatTerr : threats)
			{
				final Set<Territory> allThreats = data.getMap().getNeighbors(threatTerr, Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data));
				final HashMap<Territory, Float> threatMap = new HashMap<Territory, Float>();
				for (final Territory checkThreat : allThreats)
				{
					final float eStrength = SUtils.strength(checkThreat.getUnits().getMatches(Matches.enemyUnit(player, data)), false, false, tFirst);
					threatMap.put(checkThreat, eStrength);
				}
				final List<Territory> allThreatTerr = new ArrayList<Territory>(allThreats);
				SUtils.reorder(allThreatTerr, threatMap, true);
				final List<Collection<Unit>> xMovesKeep = new ArrayList<Collection<Unit>>();
				final List<Route> xRoutesKeep = new ArrayList<Route>();
				final List<Unit> xMovedKeep = new ArrayList<Unit>();
				for (final Territory checkThreat : allThreatTerr)
				{
					final float eStrength = threatMap.get(checkThreat);
					final List<Collection<Unit>> xMoves = new ArrayList<Collection<Unit>>();
					final List<Route> xRoutes = new ArrayList<Route>();
					final List<Unit> xMovedUnits = new ArrayList<Unit>();
					xMovedUnits.addAll(xMovedKeep);
					float needStrength = eStrength * 1.25F + 3.0F;
					// float totStrength = 0.0F;
					needStrength = SUtils.inviteLandAttack(false, checkThreat, needStrength, xMovedUnits, xMoves, xRoutes, data, player, true, true, alreadyAttacked);
					needStrength = SUtils.inviteTransports(false, checkThreat, needStrength, xMovedUnits, xMoves, xRoutes, data, player, tFirst, false, seaTerrAttacked);
					needStrength = SUtils.inviteBlitzAttack(false, checkThreat, needStrength, xMovedUnits, xMoves, xRoutes, data, player, true, true);
					final float thisPlaneStrength = SUtils.invitePlaneAttack(false, false, checkThreat, needStrength, xMovedUnits, xMoves, xRoutes, data, player);
					planeStrength += thisPlaneStrength;
					needStrength -= thisPlaneStrength;
					if (needStrength < 0.0F)
					{
						threatRemoved.add(checkThreat);
						xMovedKeep.addAll(xMovedUnits);
						xMovesKeep.addAll(xMoves);
						xRoutesKeep.addAll(xRoutes);
					}
				}
				final float newThreat = SUtils.getStrengthOfPotentialAttackers(threatTerr, data, player, tFirst, true, threatRemoved);
				final float alliedStrength = SUtils.strength(threatTerr.getUnits().getUnits(), false, false, tFirst) + planeStrength;
				if (alliedStrength < newThreat) // commit to the attacks
				{
					for (final Collection<Unit> x1 : xMovesKeep)
						moveUnits.add(x1);
					moveRoutes.addAll(xRoutesKeep);
					unitsAlreadyMoved.addAll(xMovedKeep);
				}
			}
			if (SUtils.shipThreatToTerr(myCapital, data, player, tFirst) > 2)
			{
				// don't use fighters on AC near capital if there is a strong threat to ships
				final List<Territory> fighterTerr = SUtils.findOnlyMyShips(myCapital, data, player, carrierUnit);
				for (final Territory fT : fighterTerr)
				{
					final List<Unit> fighterUnits = fT.getUnits().getMatches(fighterUnit);
					fighterUnits.removeAll(unitsAlreadyMoved);
					unitsAlreadyMoved.addAll(fighterUnits);
				}
				final List<Territory> transportTerr = SUtils.findOnlyMyShips(myCapital, data, player, Matches.UnitIsTransport);
				if (tFirst) // if transports have no value in ship fight...let them go...we can catch up to them in nonCombat
				{
					for (final Territory tranTerr : transportTerr)
					{
						unitsAlreadyMoved.addAll(tranTerr.getUnits().getMatches(Matches.UnitIsTransport));
					}
				}
			}
			for (final Territory testCap : threats)
			{
				float remainingStrengthNeeded = SUtils.getStrengthOfPotentialAttackers(testCap, data, player, tFirst, true, null);
				remainingStrengthNeeded -= SUtils.strength(testCap.getUnits().getUnits(), false, false, tFirst);
				final float blitzStrength = SUtils.inviteBlitzAttack(true, testCap, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, false, true);
				remainingStrengthNeeded -= blitzStrength;
				planeStrength = SUtils.invitePlaneAttack(true, false, testCap, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player);
				remainingStrengthNeeded -= planeStrength;
				final Set<Territory> copyOne = data.getMap().getNeighbors(testCap, 1);
				for (final Territory moveFrom : copyOne)
				{
					if (!moveFrom.isWater() && moveFrom.getUnits().someMatch(landUnit))
					{
						final List<Unit> helpUnits = moveFrom.getUnits().getMatches(landUnit);
						final Route aRoute = data.getMap().getRoute(moveFrom, testCap, Matches.territoryHasEnemyAAforCombatOnly(player, data).invert());
						if (aRoute != null)
						{
							if (MoveValidator.hasEnoughMovement(helpUnits, aRoute))
							{
								moveUnits.add(helpUnits);
								moveRoutes.add(aRoute);
								unitsAlreadyMoved.addAll(helpUnits);
							}
							else
							{
								final List<Unit> workList = new ArrayList<Unit>();
								for (final Unit goUnit : helpUnits)
								{
									if (MoveValidator.hasEnoughMovement(goUnit, aRoute))
										workList.add(goUnit);
								}
								if (workList.size() > 0)
								{
									moveUnits.add(workList);
									moveRoutes.add(aRoute);
									unitsAlreadyMoved.addAll(workList);
								}
							}
						}
					}
				}
				// only use seaTerrAttacked if this is in the combat loop...noncombat will know the results of combat moves
				if (SUtils.isWaterAt(testCap, data))
					SUtils.inviteTransports(noncombat, testCap, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, tFirst, false, noncombat ? null : seaTerrAttacked);
				else
				{
					final Set<Territory> testCapNeighbors = data.getMap().getNeighbors(testCap, Matches.isTerritoryAllied(player, data));
					for (final Territory tCN : testCapNeighbors)
					{
						SUtils.inviteTransports(noncombat, tCN, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, tFirst, false, noncombat ? null : seaTerrAttacked);
					}
				}
			}
		}
	}
	
	private void bringShipsToTransports(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		final boolean tFirst = transportsMayDieFirst();
		final Collection<Unit> alreadyMoved = new HashSet<Unit>();
		// Territory myCapital = TerritoryAttachment.getCapital(player, data);
		/*
		Match<Unit> notAlreadyMoved = new CompositeMatchAnd<Unit>(new Match<Unit>()
		{
			
			public boolean match(Unit o)
			{
				return !alreadyMoved.contains(o);
			}
		});
		*/
		final Match<Unit> ownedUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
		final Match<Unit> mySeaAirUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsNotTransport);
		final Match<Unit> myCarrierUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsCarrier);
		final Match<Unit> myAirUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitCanLandOnCarrier);
		final Match<Unit> myCarrierGroup = new CompositeMatchOr<Unit>(myCarrierUnit, myAirUnit);
		final Match<Unit> alliedTransport = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsTransport);
		final Match<Unit> alliedSeaAttackUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsSea, Matches.UnitIsNotTransport);
		final Match<Unit> alliedAirAttackUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsAir);
		final Match<Unit> alliedSeaAirAttackUnit = new CompositeMatchOr<Unit>(alliedSeaAttackUnit, alliedAirAttackUnit);
		final HashMap<Territory, Collection<Unit>> shipsMap = new HashMap<Territory, Collection<Unit>>();
		int allShips = 0;
		int enemyShips = 0;
		final List<PlayerID> ePlayers = SUtils.getEnemyPlayers(data, player);
		final PlayerID ePTemp = ePlayers.get(0);
		final List<PlayerID> alliedPlayers = SUtils.getEnemyPlayers(data, ePTemp);
		for (final PlayerID ePlayer : ePlayers)
			enemyShips += countSeaUnits(data, ePlayer);
		for (final PlayerID aPlayer : alliedPlayers)
			allShips += countSeaUnits(data, aPlayer);
		// float targetFactor = 0.55F;
		// if (allShips > enemyShips*2)
		// targetFactor = 0.45F;
		final List<Territory> alliedTransTerr = SUtils.findUnitTerr(data, player, alliedTransport);
		final HashMap<Territory, Float> attackAtTrans = new HashMap<Territory, Float>();
		final Iterator<Territory> aTIter = alliedTransTerr.iterator();
		while (aTIter.hasNext())
		{
			final Territory aT = aTIter.next();
			final float aTEStrength = SUtils.getStrengthOfPotentialAttackers(aT, data, player, tFirst, false, null);
			if (aTEStrength < 2.0F)
				aTIter.remove();
			else
				attackAtTrans.put(aT, aTEStrength);
		}
		SUtils.reorder(alliedTransTerr, attackAtTrans, true);
		for (final Territory sendToTrans : alliedTransTerr)
		{
			final float enemyStrength = attackAtTrans.get(sendToTrans);
			float targetStrength = enemyStrength * 1.25F + (enemyStrength > 2.0F ? 3.00F : 0.0F);
			float strengthAdded = 0.0F;
			if (tFirst)
				strengthAdded += SUtils.strength(sendToTrans.getUnits().getMatches(Matches.UnitIsTransport), false, true, tFirst);
			final List<Unit> mySeaUnits = sendToTrans.getUnits().getMatches(mySeaAirUnit);
			mySeaUnits.removeAll(alreadyMoved);
			final List<Unit> alliedSeaUnits = sendToTrans.getUnits().getMatches(alliedSeaAirAttackUnit);
			alliedSeaUnits.removeAll(mySeaUnits);
			final float alliedStrength = SUtils.strength(alliedSeaUnits, false, true, tFirst);
			targetStrength -= alliedStrength;
			strengthAdded += alliedStrength;
			if (targetStrength <= 0.0F)
				continue;
			final List<Collection<Unit>> xUnits = new ArrayList<Collection<Unit>>();
			final List<Unit> xMoved = new ArrayList<Unit>(alreadyMoved);
			final List<Route> xRoutes = new ArrayList<Route>();
			final Iterator<Unit> mySeaIter = mySeaUnits.iterator();
			while (mySeaIter.hasNext() && targetStrength <= 0.0F)
			{
				final Unit myUnit = mySeaIter.next();
				if (myAirUnit.match(myUnit))
					continue;
				float uStrength = 0.0F;
				if (Matches.UnitIsCarrier.match(myUnit))
				{
					final List<Unit> carrierGroup = new ArrayList<Unit>(sendToTrans.getUnits().getMatches(myCarrierGroup));
					uStrength = SUtils.strength(carrierGroup, false, true, tFirst);
					xMoved.addAll(carrierGroup);
				}
				else
				{
					uStrength = SUtils.uStrength(myUnit, false, true, tFirst);
					xMoved.add(myUnit);
				}
				targetStrength -= uStrength;
				strengthAdded += uStrength;
			}
			final float shipStrength = SUtils.inviteShipAttack(sendToTrans, targetStrength, xMoved, xUnits, xRoutes, data, player, false, tFirst, false);
			strengthAdded += shipStrength;
			moveUnits.addAll(xUnits);
			moveRoutes.addAll(xRoutes);
			alreadyMoved.addAll(xMoved);
		}
		final int totShipMoves = moveUnits.size();
		for (int i = 0; i < totShipMoves; i++)
		{
			final Collection<Unit> newUnits = moveUnits.get(i);
			final Route thisRoute = moveRoutes.get(i);
			final Territory endTerr = thisRoute.getEnd();
			if (shipsMap.containsKey(endTerr))
				newUnits.addAll(shipsMap.get(endTerr));
			shipsMap.put(endTerr, newUnits);
		}
		setShipsMovedMap(shipsMap);
	}
	
	private void secondLookSea(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		final Match<Unit> ownedUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
		final Match<Unit> enemySeaUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.enemyUnit(player, data));
		final Match<Unit> seaAttackUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsSea, Matches.UnitIsNotTransport);
		final Match<Unit> transportUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsTransport);
		final Match<Unit> airAttackUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsAir);
		final Match<Unit> seaAirAttackUnit = new CompositeMatchOr<Unit>(seaAttackUnit, airAttackUnit);
		final Match<Unit> alliedSeaAttackUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsSea, Matches.unitIsOwnedBy(player).invert());
		final Match<Unit> alliedAirAttackUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsAir, Matches.unitIsOwnedBy(player).invert());
		final Match<Unit> alliedTransport = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsTransport, Matches.unitIsOwnedBy(player).invert());
		final Match<Unit> alliedSeaAirAttackUnit = new CompositeMatchOr<Unit>(alliedSeaAttackUnit, alliedAirAttackUnit);
		final Match<Territory> routeCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data));
		final Match<Territory> endCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data));
		final List<Territory> seaAttackTerr = SUtils.findTersWithUnitsMatching(data, player, seaAttackUnit);
		final boolean tFirst = transportsMayDieFirst();
		final HashMap<Territory, Collection<Unit>> shipsMap = getShipsMovedMap();
		final List<Unit> alreadyMoved = new ArrayList<Unit>();
		for (final Territory moveTerr : seaAttackTerr)
		{
			if (shipsMap.containsKey(moveTerr))
				alreadyMoved.addAll(shipsMap.get(moveTerr));
			final List<Unit> attackUnits = moveTerr.getUnits().getMatches(seaAirAttackUnit);
			attackUnits.removeAll(alreadyMoved);
			if (attackUnits.isEmpty())
				continue;
			final int moveDist = MoveValidator.getLeastMovement(attackUnits);
			if (moveDist == 0)
				continue;
			final List<Unit> transportUnits = moveTerr.getUnits().getMatches(transportUnit);
			final boolean transportUnitsPresent = transportUnits.size() > 0;
			final List<Unit> alliedTransports = moveTerr.getUnits().getMatches(alliedTransport);
			final float thisThreat = SUtils.getStrengthOfPotentialAttackers(moveTerr, data, player, tFirst, false, null);
			final float myStrength = SUtils.strength(attackUnits, false, true, tFirst);
			final float alliedStrength = SUtils.strength(moveTerr.getUnits().getMatches(alliedSeaAirAttackUnit), false, true, tFirst);
			final boolean alliedUnitsPresent = alliedStrength > 0.0F || alliedTransports.size() > 0;
			if ((alliedUnitsPresent && alliedStrength > thisThreat * 0.75F) || thisThreat == 0.0F || (!alliedUnitsPresent && !transportUnitsPresent)) // don't need us here
			{
				final int maxUnits = 100;
				// Route eRoute = SUtils.findNearest(moveTerr, endCond, routeCond, data);
				Route eRoute = SUtils.findNearestMaxContaining(moveTerr, endCond, routeCond, enemySeaUnit, maxUnits, data);
				if (eRoute == null)
					continue;
				if (MoveValidator.validateCanal(eRoute, null, player, data) == null)
				{
					if (eRoute.getLength() > moveDist)
					{
						final Route changeRoute = new Route();
						changeRoute.setStart(moveTerr);
						for (int i = 1; i <= moveDist; i++)
							changeRoute.add(eRoute.getTerritories().get(i));
						eRoute = changeRoute;
					}
				}
				if (MoveValidator.validateCanal(eRoute, null, player, data) == null) // check again
					continue;
				final Route eRoute2 = SUtils.getMaxSeaRoute(data, moveTerr, eRoute.getEnd(), player, false, moveDist);
				if (eRoute2 == null || eRoute2.getEnd() == null)
					continue;
				final float endStrength = SUtils.getStrengthOfPotentialAttackers(eRoute2.getEnd(), data, player, tFirst, false, null);
				Route xRoute = new Route();
				if (myStrength > endStrength)
					xRoute = eRoute2;
				else
				{
					eRoute2.getTerritories().remove(eRoute2.getEnd());
					final float endStrength2 = SUtils.getStrengthOfPotentialAttackers(eRoute2.getEnd(), data, player, tFirst, false, null);
					float myStrength2 = SUtils.strength(eRoute2.getEnd().getUnits().getMatches(Matches.alliedUnit(player, data)), false, true, tFirst);
					myStrength2 += myStrength;
					if (myStrength2 > endStrength2 * 0.65F)
						xRoute = eRoute2;
					else
						xRoute = null;
				}
				if (xRoute != null)
				{
					final List<Unit> tUnits = new ArrayList<Unit>();
					if (MoveValidator.hasEnoughMovement(attackUnits, xRoute))
						tUnits.addAll(attackUnits);
					else
					{
						for (final Unit moveUnit : attackUnits)
						{
							if (MoveValidator.hasEnoughMovement(moveUnit, xRoute))
								tUnits.add(moveUnit);
						}
					}
					if (tUnits.size() > 0)
					{
						moveUnits.add(tUnits);
						moveRoutes.add(xRoute);
					}
				}
			}
		}
	}
	
	/**
	 * prepares nonCombat Moves for Sea
	 * 
	 * @param nonCombat
	 * @param data
	 * @param moveUnits
	 * @param moveRoutes
	 * @param player
	 * @param amphibRoute
	 * @param maxTrans
	 *            -
	 *            if -1 unlimited
	 */
	private void populateNonCombatSea(final boolean nonCombat, final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		final boolean tFirst = transportsMayDieFirst();
		setImpassableTerrs(player);
		final Collection<Territory> impassableTerrs = getImpassableTerrs();
		final Collection<Unit> alreadyMoved = new HashSet<Unit>();
		final Territory myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		final HashMap<Territory, Collection<Unit>> shipsMovedMap = getShipsMovedMap();
		final Match<Unit> notAlreadyMoved = new CompositeMatchAnd<Unit>(new Match<Unit>()
		{
			@Override
			public boolean match(final Unit o)
			{
				return !alreadyMoved.contains(o);
			}
		});
		final Match<Unit> ownedUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
		final Match<Unit> ownedAC = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsCarrier);
		final Match<Unit> HasntMoved2 = new CompositeMatchAnd<Unit>(Matches.unitHasNotMoved, notAlreadyMoved);
		final Match<Unit> enemySeaUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.enemyUnit(player, data));
		// Match<Unit> enemyAirUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.enemyUnit(player, data));
		// Match<Unit> enemyLandUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.enemyUnit(player, data));
		// Match<Unit> landOrAirEnemy = new CompositeMatchOr<Unit>(enemyAirUnit, enemyLandUnit);
		final Match<Unit> seaAttackUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsSea, Matches.UnitIsNotTransport);
		final Match<Unit> airAttackUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsAir);
		// final Match<Unit> subUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsSub);
		final Match<Unit> seaAirAttackUnit = new CompositeMatchOr<Unit>(seaAttackUnit, airAttackUnit);
		final Match<Unit> seaAirAttackUnitNotMoved = new CompositeMatchAnd<Unit>(seaAirAttackUnit, HasntMoved2);
		final Match<Unit> fighterUnit = new CompositeMatchAnd<Unit>(Matches.UnitCanLandOnCarrier, ownedUnit, HasntMoved2);
		// Match<Unit> fighterUnit2 = new CompositeMatchAnd<Unit>(Matches.UnitCanLandOnCarrier, ownedUnit);
		final Match<Unit> bomberUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsStrategicBomber, ownedUnit, HasntMoved2);
		final Match<Unit> carrierCanMove = new CompositeMatchAnd<Unit>(Matches.unitHasNotMoved, ownedAC);
		final Match<Unit> alliedSeaAttackUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsSea);
		final Match<Unit> alliedAirAttackUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsAir);
		final Match<Unit> alliedSeaAirAttackUnit = new CompositeMatchOr<Unit>(alliedSeaAttackUnit, alliedAirAttackUnit);
		final Match<Territory> noNeutralOrAA = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassable, Matches.territoryHasEnemyAAforCombatOnly(player, data).invert());
		final Match<Territory> noEnemyWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data));
		final Match<Territory> enemyWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data));
		final List<Territory> seaAttackTerr = SUtils.findTersWithUnitsMatching(data, player, seaAttackUnit);
		final List<Territory> enemySeaTerr = SUtils.findUnitTerr(data, player, enemySeaUnit);
		// final List<Territory> mySubTerr = SUtils.findUnitTerr(data, player, subUnit);
		// List<Territory> myFighterTerr = SUtils.findUnitTerr(data, player, fighterUnit2);
		final List<Territory> skippedTerr = new ArrayList<Territory>();
		/**
		 * First determine if attack ships have been purchased and limit moves at that factory
		 */
		final List<Unit> xMoved = new ArrayList<Unit>();
		markBaseShips(data, player, xMoved);
		Territory seaFactTerr = getSeaTerr();
		/*
		 * If we are locking down ships around capital, find the strongest point to combine ships
		 * Make it the favorite for placing ships
		 */
		if (xMoved.size() > 0)
		{
			final Set<Territory> neighborList = data.getMap().getNeighbors(myCapital, Matches.TerritoryIsWater);
			final List<Route> xR = new ArrayList<Route>();
			final List<Collection<Unit>> xM = new ArrayList<Collection<Unit>>();
			final List<Unit> xAM = new ArrayList<Unit>();
			int maxShips = 0;
			Territory maxShipTerr = null, maxStrengthTerr = null;
			float maxStrength = 0.0F;
			final float goStrength = 1000.0F;
			for (final Territory nT : neighborList)
			{
				final float thisStrength = SUtils.inviteShipAttack(nT, goStrength, xAM, xM, xR, data, player, false, tFirst, true);
				final int unitCount = xAM.size();
				if (unitCount > maxShips)
				{
					maxShipTerr = nT;
					maxShips = unitCount;
				}
				if (thisStrength > maxStrength)
				{
					maxStrengthTerr = nT;
					maxStrength = thisStrength;
				}
				xAM.clear();
				xM.clear();
				xR.clear();
			}
			// TODO: incorporate intelligence between maxStrength & maxShip
			if (maxStrengthTerr != null)
			{
				SUtils.inviteShipAttack(maxStrengthTerr, goStrength, alreadyMoved, moveUnits, moveRoutes, data, player, false, tFirst, true);
				s_logger.finer("PNCS consolidate with purchase: units not specified ");
				seaFactTerr = maxStrengthTerr;
				setSeaTerr(seaFactTerr);
			}
			else if (maxShipTerr != null)
			{
				SUtils.inviteShipAttack(maxShipTerr, goStrength, alreadyMoved, moveUnits, moveRoutes, data, player, false, tFirst, true);
				s_logger.finer("PNCS consolidate with purchase2: units not specified ");
				seaFactTerr = maxShipTerr;
				setSeaTerr(seaFactTerr);
			}
		}
		else if (seaFactTerr != null)
		{
			float seaFactStrength = SUtils.getStrengthOfPotentialAttackers(seaFactTerr, data, player, tFirst, false, null);
			final List<Unit> seaUnitsPurchased = player.getUnits().getMatches(Matches.UnitIsSea);
			seaFactStrength -= SUtils.strength(seaUnitsPurchased, false, true, tFirst);
			if (seaFactStrength > 0.0F)
			{
				SUtils.inviteShipAttack(seaFactTerr, seaFactStrength, alreadyMoved, moveUnits, moveRoutes, data, player, false, tFirst, true);
				s_logger.finer("PNCS defend purchase: units not specified ");
			}
		}
		alreadyMoved.addAll(xMoved);
		final List<Territory> transTerr = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitIsTransport);
		final IntegerMap<Territory> transMap = new IntegerMap<Territory>();
		final HashMap<Territory, Float> transStrengthMap = new HashMap<Territory, Float>();
		for (final Territory tT : transTerr)
		{
			final float tStrength = SUtils.getStrengthOfPotentialAttackers(tT, data, player, tFirst, false, null);
			final int tUnits = tT.getUnits().countMatches(Matches.UnitIsTransport);
			transMap.put(tT, tUnits);
			transStrengthMap.put(tT, tStrength);
		}
		SUtils.reorder(transTerr, transMap, true);
		final List<Territory> transTerr2 = new ArrayList<Territory>(transTerr);
		for (final Territory trans : transTerr2)
		{
			final Collection<Unit> ourAttackUnits = trans.getUnits().getUnits();
			// float ourTStrength = SUtils.strength(ourAttackUnits, false, true, tFirst);
			final float eStrength = transStrengthMap.get(trans).floatValue();
			if (eStrength < 0.50F) // enemy has nothing here
			{
				transTerr.remove(trans);
			}
			// lock down enough units to protect
			float strengthNeeded = eStrength;
			final List<Unit> alreadyCounted = new ArrayList<Unit>();
			for (final Unit aUnit : ourAttackUnits)
			{ // only allow fighters to be counted with carriers...otherwise they have to land somewhere else
				final UnitType uT = aUnit.getType();
				if (strengthNeeded <= 0.0F || alreadyCounted.contains(aUnit) || Matches.UnitTypeCanLandOnCarrier.match(uT))
					continue;
				if (Matches.UnitTypeIsCarrier.match(uT))
				{
					strengthNeeded -= SUtils.uStrength(aUnit, false, true, tFirst);
					int numFighters = UnitAttachment.get(uT).getCarrierCapacity();
					for (final Unit aUnit2 : ourAttackUnits)
					{
						if (!alreadyCounted.contains(aUnit2) && Matches.UnitTypeCanLandOnCarrier.match(uT) && numFighters > 0)
						{
							strengthNeeded -= SUtils.uStrength(aUnit2, false, true, tFirst);
							alreadyCounted.add(aUnit2);
							numFighters--;
						}
					}
					alreadyCounted.add(aUnit);
				}
				else
				{
					strengthNeeded -= SUtils.uStrength(aUnit, false, true, tFirst);
					alreadyCounted.add(aUnit);
				}
			}
			s_logger.finer("PNCS stationary to defend transports " + alreadyCounted + " at " + trans);
			alreadyMoved.addAll(alreadyCounted);
		}
		// int maxUnits = 0;
		final Route eShipRoute = SUtils.findNearest(myCapital, enemyWater, noEnemyWater, data);
		Territory goHere = null;
		// final Territory seaTarget = null;
		if (eShipRoute != null && eShipRoute.getLength() <= 5)
			goHere = eShipRoute.getEnd();
		float alliedStrength = 0.0F, badGuyStrength = 0.0F, ownedStrength = 0.0F;
		// first check our attack ship territories
		for (final Territory myTerr : seaAttackTerr)
		{
			final List<Unit> myAttackUnits = myTerr.getUnits().getMatches(seaAirAttackUnit);
			final List<Unit> alliedAttackUnits = myTerr.getUnits().getMatches(alliedSeaAirAttackUnit);
			if (shipsMovedMap.containsKey(myTerr))
				alreadyMoved.addAll(shipsMovedMap.get(myTerr));
			boolean keepGoing = true;
			badGuyStrength = SUtils.getStrengthOfPotentialAttackers(myTerr, data, player, tFirst, false, null);
			ownedStrength = SUtils.strength(myAttackUnits, false, true, tFirst);
			alliedStrength = SUtils.strength(alliedAttackUnits, false, true, tFirst);
			if ((alliedStrength > 1.00F && alliedStrength + 6.00F > badGuyStrength * 0.65F) && (badGuyStrength > 2.00F))
			{ // where is the source of the attack?
				final Set<Territory> bgSourceTerr = data.getMap().getNeighbors(myTerr, 2);
				bgSourceTerr.removeAll(impassableTerrs);
				Territory mainSourceTerr = null;
				for (final Territory bgSource : bgSourceTerr)
				{
					if (Matches.TerritoryIsWater.match(bgSource) && Matches.territoryHasEnemyUnits(player, data).match(bgSource))
					{
						final List<Unit> bgUnits = bgSource.getUnits().getMatches(Matches.enemyUnit(player, data));
						final float bgTerrStrength = SUtils.strength(bgUnits, true, true, tFirst);
						if (bgTerrStrength > 0.5F * badGuyStrength)
							mainSourceTerr = bgSource;
					}
				}
				if (mainSourceTerr != null)
				{
					final Set<Territory> sourceNeighbors = data.getMap().getNeighbors(mainSourceTerr, 2);
					sourceNeighbors.removeAll(impassableTerrs);
					float maxStrength = 0.0F;
					Territory maxStrengthTerr = null;
					for (final Territory sN : sourceNeighbors)
					{
						if (Matches.TerritoryIsWater.match(sN) && Matches.territoryHasNoAlliedUnits(player, data).invert().match(sN) && !skippedTerr.contains(sN))
						{
							final List<Unit> sNUnits = sN.getUnits().getMatches(Matches.alliedUnit(player, data));
							sNUnits.removeAll(alreadyMoved);
							if (sNUnits.size() == 0)
								continue;
							final float quickStrength = SUtils.strength(sNUnits, false, true, tFirst);
							if (quickStrength > maxStrength)
							{
								maxStrength = quickStrength;
								maxStrengthTerr = sN;
							}
						}
					}
					if (maxStrengthTerr != null)
					{
						final float newBadGuyStrength = (badGuyStrength * 0.75F - maxStrength);
						SUtils.inviteShipAttack(maxStrengthTerr, newBadGuyStrength, alreadyMoved, moveUnits, moveRoutes, data, player, false, tFirst, false);
						s_logger.finer("PNCS consolidate threatened units?: units not specified. at" + maxStrengthTerr);
						alreadyMoved.addAll(maxStrengthTerr.getUnits().getMatches(Matches.alliedUnit(player, data)));
					}
				}
				keepGoing = false;
			}
			if (!keepGoing)
			{
				skippedTerr.add(myTerr);
				continue;
			}
			// This overrides everything below, but it gets the ships moving...obviously we may be sacrificing them...
			Route quickRoute = null;
			int minSeaDist = 100;
			int moveDist = MoveValidator.getLeastMovement(myAttackUnits);
			if (badGuyStrength > alliedStrength * 1.65F + 3.0F)
			{
				final Set<Territory> myMoveNeighbors = data.getMap().getNeighbors(myTerr, 2);
				myMoveNeighbors.removeAll(impassableTerrs);
				final HashMap<Territory, Float> MNmap = new HashMap<Territory, Float>();
				for (final Territory MNterr : myMoveNeighbors)
				{
					if (!MNterr.isWater() || Matches.territoryHasEnemyUnits(player, data).match(MNterr))
						continue;
					final float enemyStrength = SUtils.getStrengthOfPotentialAttackers(MNterr, data, player, tFirst, true, null);
					float MNStrength = SUtils.strength(MNterr.getUnits().getMatches(Matches.alliedUnit(player, data)), false, true, tFirst);
					MNStrength += ownedStrength;
					MNmap.put(MNterr, enemyStrength - MNStrength);
				}
				final Set<Territory> MNterrs = MNmap.keySet();
				final List<Territory> MNterrs2 = new ArrayList<Territory>(MNterrs);
				SUtils.reorder(MNterrs2, MNmap, true);
				final Iterator<Territory> MNIter = MNterrs2.iterator();
				boolean MNdone = false;
				goHere = null;
				while (MNIter.hasNext() && !MNdone)
				{
					final Territory MNterr = MNIter.next();
					if ((ownedStrength + MNmap.get(MNterr)) < 0.0F)
					{
						quickRoute = SUtils.getMaxSeaRoute(data, myTerr, MNterr, player, false, moveDist);
						if (quickRoute != null && quickRoute.getEnd() == MNterr)
						{
							goHere = MNterr;
							MNdone = true;
						}
					}
				}
				if (goHere != null)
				{
					s_logger.finer("PNCS consolidate threatend units2?: " + myAttackUnits + " route " + quickRoute);
					moveUnits.add(myAttackUnits);
					moveRoutes.add(quickRoute);
					alreadyMoved.addAll(myAttackUnits);
					continue;
				}
			}
			for (final Territory badSeaTerr : enemySeaTerr)
			{
				final Route seaCheckRoute = SUtils.getMaxSeaRoute(data, myTerr, badSeaTerr, player, false, moveDist);
				if (seaCheckRoute == null)
					continue;
				final int newDist = seaCheckRoute.getLength();
				if (newDist < minSeaDist)
				{
					goHere = badSeaTerr;
					minSeaDist = newDist;
					quickRoute = seaCheckRoute;
				}
			}
			myAttackUnits.removeAll(alreadyMoved);
			final Iterator<Unit> checkIter = myAttackUnits.iterator();
			while (checkIter.hasNext())
			{
				final Unit checkOne = checkIter.next();
				if (!Matches.unitHasNotMoved.match(checkOne))
					checkIter.remove();
			}
			if (myAttackUnits.size() > 0 && goHere != null && quickRoute != null)
			{
				final float goHereStrength = SUtils.getStrengthOfPotentialAttackers(goHere, data, player, tFirst, false, null);
				final float ourStrength = SUtils.strength(myAttackUnits, false, true, tFirst) + SUtils.strength(goHere.getUnits().getMatches(alliedSeaAirAttackUnit), false, true, tFirst);
				if (ourStrength >= goHereStrength * 0.75F)
				{
					s_logger.finer("PNCS move towards enemy?  " + myAttackUnits + " in " + goHere + " route " + quickRoute);
					moveUnits.add(myAttackUnits);
					moveRoutes.add(quickRoute);
					alreadyMoved.addAll(myAttackUnits);
				}
				else
					skippedTerr.add(myTerr);
			}
			else
				skippedTerr.add(myTerr);
			goHere = null;
			if (badGuyStrength == 0.0F)
			{
				final Route eRoute = SUtils.findNearest(myTerr, enemyWater, noEnemyWater, data);
				if (eRoute != null)
				{
					final int eLength = eRoute.getLength();
					if (eRoute.getEnd() != null)
					{
						boolean moveForward = false;
						final List<Unit> canGoUnits = new ArrayList<Unit>(myAttackUnits);
						canGoUnits.removeAll(alreadyMoved);
						ownedStrength = SUtils.strength(canGoUnits, false, true, tFirst);
						Territory theTarget = null;
						if (eLength <= 4)
						{
							final Territory endTerr = eRoute.getEnd();
							final float eStrength = SUtils.strength(endTerr.getUnits().getUnits(), false, true, tFirst);
							final float xtraEStrength = SUtils.getStrengthOfPotentialAttackers(endTerr, data, player, tFirst, false, null);
							final float potentialStrength = eStrength * 0.75F + 0.25F * xtraEStrength;
							if (ownedStrength > potentialStrength)
							{
								theTarget = eRoute.getTerritories().get(eRoute.getLength() - 1);
								moveForward = true;
							}
						}
						else
						{
							theTarget = eRoute.getTerritories().get(2);
							final float eStrength = SUtils.getStrengthOfPotentialAttackers(theTarget, data, player, tFirst, false, null);
							if (ownedStrength > eStrength * 0.65F)
								moveForward = true;
							else
							{
								theTarget = eRoute.getTerritories().get(1);
								final float xEStrength = SUtils.getStrengthOfPotentialAttackers(theTarget, data, player, tFirst, false, null);
								if (ownedStrength > xEStrength * 0.45F)
									moveForward = true;
							}
						}
						if (moveForward)
						{
							moveDist = MoveValidator.getLeastMovement(canGoUnits);
							final Route canGoRoute = SUtils.getMaxSeaRoute(data, myTerr, theTarget, player, false, moveDist);
							s_logger.finer("PNCS move towards enemy2?: " + canGoUnits + " at " + eRoute.getEnd() + " route " + canGoRoute);
							moveUnits.add(canGoUnits);
							moveRoutes.add(canGoRoute);
							alreadyMoved.addAll(canGoUnits);
						}
					}
				}
			}
		}
		final HashMap<Territory, Float> enemyMap = new HashMap<Territory, Float>();
		final List<Territory> enemyTerr = SUtils.findUnitTerr(data, player, enemySeaUnit);
		// int numTerr = enemyTerr.size();
		for (final Territory t2 : enemyTerr) // find strength of all enemy terr (defensive)
		{
			enemyMap.put(t2, SUtils.strength(t2.getUnits().getMatches(enemySeaUnit), false, true, tFirst));
		}
		SUtils.reorder(enemyTerr, enemyMap, true);
		for (final Territory enemy : enemyTerr)
		{
			final List<Territory> ourShipTerrs = SUtils.findOurShips(enemy, data, player);
			for (final Territory shipTerr : ourShipTerrs)
			{
				if (!shipTerr.isWater())
					continue;
				if (data.getMap().getNeighbors(shipTerr, enemyWater).size() > 0)
				{
					skippedTerr.add(shipTerr);
					continue;
				}
				final List<Territory> Neighbors2 = SUtils.getExactNeighbors(shipTerr, 2, player, data, false);
				boolean continueOn = true;
				for (final Territory N2 : Neighbors2)
				{
					if (enemyWater.match(N2))
						continueOn = false;
				}
				if (!continueOn)
				{
					skippedTerr.add(shipTerr);
					continue;
				}
				final float eS1 = SUtils.getStrengthOfPotentialAttackers(shipTerr, data, player, tFirst, true, null);
				final Set<Territory> lookAroundTerr = data.getMap().getNeighbors(shipTerr, 5);
				lookAroundTerr.removeAll(impassableTerrs);
				final List<Territory> hasEnemyShips = new ArrayList<Territory>();
				for (final Territory eShipTerr : lookAroundTerr)
				{
					if (enemyWater.match(eShipTerr))
						hasEnemyShips.add(eShipTerr);
				}
				final List<Unit> moveableUnits = shipTerr.getUnits().getMatches(seaAirAttackUnitNotMoved);
				moveableUnits.removeAll(alreadyMoved);
				final Iterator<Unit> mUIter = moveableUnits.iterator();
				while (mUIter.hasNext())
				{
					final Unit mU = mUIter.next();
					if (!MoveValidator.hasEnoughMovement(mU, 1))
						mUIter.remove();
				}
				final List<Unit> unMoveableUnits = shipTerr.getUnits().getMatches(Matches.unitHasMoved);
				final float unmoveableStrength = SUtils.strength(unMoveableUnits, false, true, tFirst);
				if (unmoveableStrength < eS1 * .65F) // can we leave a ship behind and protect it?
				{
					float testStrength = unmoveableStrength;
					final List<Unit> leaveUnits = new ArrayList<Unit>();
					for (final Unit leaveUnit : moveableUnits)
					{
						if (testStrength < eS1 * 0.65F)
						{
							final float addOn = SUtils.uStrength(leaveUnit, false, true, tFirst);
							leaveUnits.add(leaveUnit);
							testStrength += addOn;
						}
					}
					moveableUnits.removeAll(leaveUnits);
				}
				if (moveableUnits.size() > 0 && hasEnemyShips.size() == 1)
				{
					final float moveableStrength = SUtils.strength(moveableUnits, false, true, tFirst);
					final Territory enemyShipTerr = hasEnemyShips.get(0);
					Route nRoute = data.getMap().getWaterRoute(shipTerr, enemyShipTerr);
					if (nRoute == null)
						continue;
					final int moveDist = MoveValidator.getLeastMovement(moveableUnits);
					if (MoveValidator.validateCanal(nRoute, null, player, data) != null)
					{
						nRoute = SUtils.getMaxSeaRoute(data, shipTerr, enemyShipTerr, player, false, moveDist);
						if (nRoute == null)
							continue;
					}
					else
					{
						final Route nRoute2 = new Route();
						final int goLength = nRoute.getLength();
						final Territory goPoint = (moveDist >= goLength) ? nRoute.getEnd() : nRoute.getTerritories().get(moveDist);
						final float goPointStrength = SUtils.getStrengthOfPotentialAttackers(goPoint, data, player, tFirst, false, null);
						if (goPoint != nRoute.getEnd())
						{
							nRoute2.setStart(shipTerr);
							for (int i = 1; i <= moveDist; i++)
								nRoute2.add(nRoute.getTerritories().get(i));
							nRoute = nRoute2;
						}
						if (goPointStrength * 0.55F < moveableStrength)
						{
							s_logger.finer("PNCS moving units unknown reason?: " + moveableUnits + " route " + nRoute2);
							moveUnits.add(moveableUnits);
							moveRoutes.add(nRoute2);
							alreadyMoved.addAll(moveableUnits);
						}
					}
				}
			}
		}
		// check the skipped Territories...see if there are ships we can combine
		final List<Territory> dontMoveFrom = new ArrayList<Territory>();
		for (final Territory check1 : skippedTerr)
		{
			for (final Territory check2 : skippedTerr)
			{
				if (check1 == check2 || dontMoveFrom.contains(check2))
					continue;
				final int check1Dist = SUtils.distanceToEnemy(check1, data, player, true);
				final int check2Dist = SUtils.distanceToEnemy(check2, data, player, true);
				Territory start = null;
				Territory stop = null;
				if (check1Dist > check2Dist)
				{
					start = check2;
					stop = check1;
				}
				else
				{
					start = check1;
					stop = check2;
				}
				final List<Unit> swapUnits = start.getUnits().getMatches(seaAirAttackUnitNotMoved);
				swapUnits.removeAll(alreadyMoved);
				if (swapUnits.isEmpty())
					continue;
				final int swapDist = MoveValidator.getLeastMovement(swapUnits);
				final Route swapRoute = SUtils.getMaxSeaRoute(data, start, stop, player, false, swapDist);
				if (swapRoute != null)
				{
					// planes only move if carrier in fleet or at target
					// no consideration for landing spaces.
					if (!((swapRoute.getEnd() != null && swapRoute.getEnd().getUnits().someMatch(ownedAC)) || start.getUnits().someMatch(carrierCanMove)))
						swapUnits.removeAll(start.getUnits().getMatches(airAttackUnit));
					if (swapUnits.isEmpty())
						continue;
					s_logger.finer("PNCS consolidate fleet " + swapUnits + " to " + stop + " route " + swapRoute);
					moveUnits.add(swapUnits);
					moveRoutes.add(swapRoute);
					alreadyMoved.addAll(swapUnits);
					dontMoveFrom.add(stop); // make sure check1 is blocked on the 2nd pass...ships are moving to it
				}
			}
		}
		final List<Territory> fTerr = SUtils.findUnitTerr(data, player, fighterUnit);
		final List<Territory> bTerr = SUtils.findUnitTerr(data, player, bomberUnit);
		final List<Territory> allTerr = new ArrayList<Territory>();
		if (fTerr != null)
			allTerr.addAll(fTerr);
		if (bTerr != null)
			allTerr.addAll(bTerr);
		if (nonCombat)
		{
			for (final Territory newTerr : allTerr)
			{
				boolean enemyFound = false;
				final Set<Territory> sNewTerr = data.getMap().getNeighbors(newTerr, 2);
				sNewTerr.removeAll(impassableTerrs);
				for (final Territory cEnemyTerr : sNewTerr)
				{
					if (Matches.territoryHasEnemyUnits(player, data).match(cEnemyTerr))
						enemyFound = true;
				}
				if (enemyFound)
					continue;
				final Territory capTerr = null;
				final int minDist = 0;
				Territory goPoint = SUtils.getAlliedLandTerrNextToEnemyCapital(minDist, capTerr, newTerr, data, player);
				final Route capRoute = data.getMap().getRoute(newTerr, goPoint, noNeutralOrAA);
				if (capRoute == null)
					continue;
				final int cRLen = capRoute.getLength();
				boolean foundit = false;
				Territory BtargetTerr = null;
				Territory FtargetTerr = null;
				final List<Territory> cRTerrs = capRoute.getTerritories();
				// Iterator<Territory> cRIter = cRTerrs.iterator();
				for (int i = cRLen - 1; i >= 0; i--)
				{
					goPoint = cRTerrs.get(i);
					final float testStrength = SUtils.getStrengthOfPotentialAttackers(goPoint, data, player, tFirst, true, null);
					final float ourStrength = SUtils.strength(goPoint.getUnits().getMatches(Matches.alliedUnit(player, data)), false, false, tFirst);
					if (ourStrength > 0.65F * testStrength && i <= 4 && Matches.isTerritoryAllied(player, data).match(goPoint))
					{
						FtargetTerr = goPoint;
						foundit = true;
					}
					if (ourStrength > 0.65F * testStrength && i <= 6 && Matches.isTerritoryAllied(player, data).match(goPoint))
					{
						BtargetTerr = goPoint;
						foundit = true;
					}
				}
				if (foundit)
				{
					final List<Unit> fAirUnits = newTerr.getUnits().getMatches(fighterUnit);
					fAirUnits.removeAll(alreadyMoved);
					final List<Unit> bombUnits = newTerr.getUnits().getMatches(bomberUnit);
					bombUnits.removeAll(alreadyMoved);
					final Route BcapRoute = data.getMap().getRoute(newTerr, BtargetTerr, noNeutralOrAA);
					final Route FcapRoute = data.getMap().getRoute(newTerr, FtargetTerr, noNeutralOrAA);
					if (BcapRoute != null && bombUnits.size() > 0 && AirMovementValidator.canLand(bombUnits, BtargetTerr, player, data))
					{
						boolean canLand = true;
						for (final Unit b1 : bombUnits)
						{
							if (canLand)
								canLand = SUtils.airUnitIsLandable(b1, newTerr, BtargetTerr, player, data);
						}
						if (canLand)
						{
							s_logger.finer("PNCS send bomber to ECap: " + bombUnits + " route " + BcapRoute);
							moveRoutes.add(BcapRoute);
							moveUnits.add(bombUnits);
							alreadyMoved.addAll(bombUnits);
						}
					}
					if (FcapRoute != null && fAirUnits.size() > 0 && !newTerr.getUnits().someMatch(ownedAC) && AirMovementValidator.canLand(fAirUnits, FtargetTerr, player, data))
					{
						boolean canLand = true;
						for (final Unit f1 : fAirUnits)
						{
							if (canLand)
								canLand = SUtils.airUnitIsLandable(f1, newTerr, FtargetTerr, player, data);
						}
						if (canLand)
						{
							s_logger.finer("PNCS send fighter to ECap: " + fAirUnits + " route " + FcapRoute);
							moveRoutes.add(FcapRoute);
							moveUnits.add(fAirUnits);
							alreadyMoved.addAll(fAirUnits);
						}
					}
				}
			}
		}
		// other planes...move toward the largest enemy mass of units
		// currently not executed
		// TODO: implement
		/*
		for (final Territory subTerr : mySubTerr)
		{
			if (!subTerr.isWater() || seaTarget == null || subTerr == seaTarget)
				continue;
			final List<Unit> allMyUnits = subTerr.getUnits().getMatches(ownedUnit);
			allMyUnits.removeAll(alreadyMoved);
			if (allMyUnits.isEmpty())
				continue;
			final int unitDist = MoveValidator.getMaxMovement(allMyUnits);
			final Route myRoute = SUtils.getMaxSeaRoute(data, subTerr, seaTarget, player, false, unitDist);
			if (myRoute == null)
				continue;
			final List<Unit> moveThese = new ArrayList<Unit>();
			for (final Unit sendUnit : allMyUnits)
			{
				if (MoveValidator.hasEnoughMovement(sendUnit, myRoute))
					moveThese.add(sendUnit);
			}
			s_logger.finer("PNCS unknown?: " + moveThese + " route " + myRoute);
			moveUnits.add(moveThese);
			moveRoutes.add(myRoute);
		}
		*/
		// SUtils.verifyMoves(moveUnits, moveRoutes, data, player);
	}
	
	private void nonCombatPlanes(final GameData data, final PlayerID player, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes)
	{
		// specifically checks for available Carriers and finds a place for plane
		final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
		final Match<Unit> ownedUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
		final Match<Unit> ACOwned = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsCarrier);
		final Match<Unit> ACAllied = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsCarrier);
		final Match<Unit> fighterAndAllied = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitCanLandOnCarrier);
		final Match<Unit> fighterAndOwned = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitCanLandOnCarrier);
		// Match<Unit> alliedUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data));
		final List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
		final CompositeMatch<Territory> notNeutralOrAA = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassable, Matches.territoryHasEnemyAAforCombatOnly(player, data).invert());
		// Territory myCapital = TerritoryAttachment.getCapital(player, data);
		// boolean capDanger = markFactoryUnits(data, player, unitsAlreadyMoved);
		final boolean tFirst = transportsMayDieFirst();
		// List<Territory> fighterTerr = SUtils.findTersWithUnitsMatching(data, player, fighterAndOwned);
		final List<Territory> alliedThreats = new ArrayList<Territory>();
		final boolean alliedDanger = SUtils.threatToAlliedCapitals(data, player, alliedThreats, tFirst);
		if (alliedDanger)
		{
			for (final Territory aThreat : alliedThreats)
			{
				if (aThreat.getUnits().someMatch(fighterAndOwned))
					unitsAlreadyMoved.addAll(aThreat.getUnits().getMatches(fighterAndOwned));
			}
		}
		final List<Territory> acTerr1 = SUtils.ACTerritory(player, data);
		if (acTerr1.size() == 0)
		{
			return;
		}
		final IntegerMap<Territory> acSpaceMap = new IntegerMap<Territory>();
		final HashMap<Territory, Float> acAttackMap = new HashMap<Territory, Float>();
		for (final Territory ACMap : acTerr1)
		{
			final float ACMapStrength = SUtils.getStrengthOfPotentialAttackers(ACMap, data, player, tFirst, false, null);
			acAttackMap.put(ACMap, ACMapStrength);
		}
		SUtils.reorder(acTerr1, acAttackMap, true);
		for (final Territory ACMap : acTerr1)
		{
			final List<Unit> ACMapUnits = ACMap.getUnits().getMatches(ACOwned);
			int ownedCarrierSpace = 0;
			for (final Unit carrier1 : ACMapUnits)
				ownedCarrierSpace += UnitAttachment.get(carrier1.getType()).getCarrierCapacity();
			final List<Unit> ACAlliedMapUnits = ACMap.getUnits().getMatches(ACAllied);
			int alliedCarrierSpace = 0;
			for (final Unit carrier1 : ACAlliedMapUnits)
				alliedCarrierSpace += UnitAttachment.get(carrier1.getType()).getCarrierCapacity();
			final List<Unit> ACfighterUnits = ACMap.getUnits().getMatches(fighterAndOwned);
			final List<Unit> ACAlliedfighterUnits = ACMap.getUnits().getMatches(fighterAndAllied);
			final int xAlliedSpace = Math.max(ACAlliedfighterUnits.size() - alliedCarrierSpace, 0);
			final int aSpace = ownedCarrierSpace - ACfighterUnits.size() - xAlliedSpace;
			acSpaceMap.put(ACMap, aSpace);
		}
		final List<Territory> myFighterTerr = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitCanLandOnCarrier);
		myFighterTerr.removeAll(acTerr1);
		for (final Territory t : myFighterTerr)
		{
			final List<Unit> tPlanes = t.getUnits().getMatches(fighterAndOwned);
			if (tPlanes.size() <= 0)
				continue;
			for (final Territory acT : acTerr1)
			{
				final Route acRoute = data.getMap().getRoute(t, acT, notNeutralOrAA);
				if (acRoute == null)
					continue;
				final List<Unit> fMoveUnits = new ArrayList<Unit>();
				for (final Unit fUnit : tPlanes)
				{
					if (MoveValidator.hasEnoughMovement(fUnit, acRoute))
						fMoveUnits.add(fUnit);
				}
				if (fMoveUnits.size() == 0)
					continue;
				int availSpace = acSpaceMap.getInt(acT);
				final List<Unit> tempUnits = new ArrayList<Unit>();
				if (availSpace > 0)
				{
					final Iterator<Unit> fIter = fMoveUnits.iterator();
					while (availSpace > 0 && fIter.hasNext())
					{
						final Unit fMoveUnit = fIter.next();
						tempUnits.add(fMoveUnit);
						availSpace--;
					}
					if (tempUnits.size() > 0)
					{
						moveUnits.add(tempUnits);
						moveRoutes.add(acRoute);
						unitsAlreadyMoved.addAll(tempUnits);
						acSpaceMap.put(acT, availSpace);
					}
				}
				else if (availSpace < 0 && t.isWater() || delegate.getBattleTracker().wasBattleFought(t)) // need to move something off
				{
					final List<Unit> alreadyMoved = new ArrayList<Unit>();
					final List<Unit> myFighters = acT.getUnits().getMatches(fighterAndOwned);
					int maxPass = 0;
					final int fightersNum = myFighters.size();
					while (availSpace < 0 && maxPass <= fightersNum)
					{
						int max = 0;
						maxPass++;
						final Iterator<Unit> iter = myFighters.iterator();
						Unit moveIt = null;
						while (iter.hasNext())
						{
							final Unit unit = iter.next();
							if (alreadyMoved.contains(unit))
								continue;
							final int left = TripleAUnit.get(unit).getMovementLeft();
							if (left >= max)
							{
								max = left;
								moveIt = unit;
							}
						}
						if (moveIt == null) // no planes can move!!!
							continue;
						final Route nearRoute = SUtils.findNearest(acT, Matches.isTerritoryAllied(player, data), notNeutralOrAA, data);
						if (nearRoute == null)
							continue;
						if (MoveValidator.hasEnoughMovement(moveIt, nearRoute))
						{
							moveUnits.add(Collections.singleton(moveIt));
							moveRoutes.add(nearRoute);
							alreadyMoved.contains(moveIt);
							availSpace++;
							acSpaceMap.put(acT, availSpace);
							myFighters.remove(moveIt);
						}
					}
				}
			}
		}
	}
	
	private void populateCombatMoveSea(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		final boolean isAmphib = isAmphibAttack(player, false);
		final boolean attackShipsPurchased = getAttackShipPurchase();
		// TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		final boolean tFirst = transportsMayDieFirst();
		final Collection<Unit> unitsAlreadyMoved = new HashSet<Unit>();
		final List<Collection<Unit>> attackUnits = new ArrayList<Collection<Unit>>();
		// Collection<Territory> allBomberTerr = new ArrayList<Territory>();
		final Match<Unit> notAlreadyMoved = new CompositeMatchAnd<Unit>(new Match<Unit>()
		{
			@Override
			public boolean match(final Unit o)
			{
				return !unitsAlreadyMoved.contains(o);
			}
		});
		for (final Unit u : data.getUnits().getUnits())
		{
			if (u.getOwner().equals(player) && TripleAUnit.get(u).getMovementLeft() < 1)
				unitsAlreadyMoved.add(u);
		}
		final Match<Unit> ownedUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
		final CompositeMatch<Unit> seaUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsSea, notAlreadyMoved);
		final CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsAir, notAlreadyMoved);
		final CompositeMatch<Unit> seaAirUnit = new CompositeMatchOr<Unit>(seaUnit, airUnit);
		final CompositeMatch<Unit> alliedSeaUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsSea);
		final CompositeMatch<Unit> alliedAirUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsAir);
		final CompositeMatch<Unit> alliedSeaAirUnit = new CompositeMatchOr<Unit>(alliedAirUnit, alliedSeaUnit);
		final CompositeMatch<Unit> alliedSeaAirUnitNotOwned = new CompositeMatchOr<Unit>(alliedSeaAirUnit, ownedUnit);
		// CompositeMatch<Unit> attackable = new CompositeMatchAnd<Unit>(ownedUnit, notAlreadyMoved);
		final CompositeMatch<Unit> enemySeaUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsSea);
		// CompositeMatch<Unit> enemyAirUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsAir);
		// CompositeMatch<Unit> enemyAirSeaUnit = new CompositeMatchOr<Unit>(enemySeaUnit, enemyAirUnit);
		final CompositeMatch<Unit> enemyNonTransport = new CompositeMatchAnd<Unit>(enemySeaUnit, Matches.UnitIsNotTransport);
		final CompositeMatch<Unit> enemySub = new CompositeMatchAnd<Unit>(enemySeaUnit, Matches.UnitIsSub);
		// CompositeMatch<Unit> carrierUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsCarrier);
		// CompositeMatch<Unit> myDestroyer = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsDestroyer);
		final List<Territory> seaTerrAttacked = new ArrayList<Territory>();
		final List<Route> attackRoute = new ArrayList<Route>();
		float attackFactor = 1.68F; // adjust to make attacks more or less likely (1.05 is too low)
		if (isAmphib)
			attackFactor = 1.68F; // if amphibious assume tendency to buy more ships
		final HashMap<Territory, Float> sortTerritories = new HashMap<Territory, Float>();
		final List<Territory> enemyTerr = new ArrayList<Territory>();
		for (final Territory t : data.getMap().getTerritories())
		{
			if (t.isWater() && t.getUnits().someMatch(enemySeaUnit))
			{
				sortTerritories.put(t, SUtils.strength(t.getUnits().getMatches(Matches.enemyUnit(player, data)), false, true, tFirst));
				enemyTerr.add(t);
			}
		}
		SUtils.reorder(enemyTerr, sortTerritories, true);
		int maxShipCount = 0;
		Territory maxShipsTerr = null, seaPlaceFact = null;
		// boolean seaTerrSet = false;
		final Territory myCapital = m_myCapital;
		// HashMap<Territory, Float> checkForMorePlanes = new HashMap<Territory, Float>();
		/**
		 * If ships were purchased because of large ship disadvantage, bring close ships to the spot
		 * unless we can take out the ships at a point next to our capital
		 * Can we take out the largest group and reduce enemy to a manageable size?
		 * Steps: 1) Find largest group
		 * 2) Figure out the remaining Strength
		 * 3) See if units purchased is > remaining Strength {attack!}
		 * 4) if NOT, see how many units are remaining if we win battle
		 * 5) Add those to purchased set and see if > remaining Enemy Strength {attack!}
		 */
		// why was this conditional removed? extremely invalid
		// this is _not_ what this method does.
		if (attackShipsPurchased)
		{
			final List<Collection<Unit>> xMoves2 = new ArrayList<Collection<Unit>>();
			final List<Route> xRoutes2 = new ArrayList<Route>();
			final List<Unit> xAlreadyMoved2 = new ArrayList<Unit>(unitsAlreadyMoved);
			seaPlaceFact = getSeaTerr();
			// set factory to one under most threat from ships.
			if (seaPlaceFact == null)
			{ // if purchasing didn't specify the factory, figure it out
				for (final Territory bestTarget : SUtils.findTersWithUnitsMatching(data, player, Matches.UnitCanProduceUnits))
				{
					final int thisShipCount = SUtils.shipThreatToTerr(bestTarget, data, player, tFirst);
					if (thisShipCount > maxShipCount)
					{
						seaPlaceFact = bestTarget;
						maxShipCount = thisShipCount;
					}
				}
			}
			else
			{
				maxShipCount = SUtils.shipThreatToTerr(seaPlaceFact, data, player, tFirst);
				// seaTerrSet = true;
			}
			boolean attackGroup = false;
			// only doing anything at all if we have a capital and we are building ships there
			// checking seaPlaceFact makes it irrelevant
			// if (myCapital != null && seaPlaceFact == myCapital)
			if (myCapital != null && seaPlaceFact != null)
			{
				// List<Territory> eSTerr = SUtils.findUnits(myCapital, data, enemySeaUnit, 3);
				final List<Territory> eSTerr = SUtils.findUnits(seaPlaceFact, data, enemySeaUnit, 3);
				float totStrengthEnemyShips = 0.0F;
				int maxUnitCount = 0, totUnitCount = 0;
				float maxStrength = 0.0F;
				Territory largestGroupTerr = null;
				List<Unit> largestGroup = new ArrayList<Unit>();
				// nearby enemy ships
				for (final Territory eST : eSTerr)
				{
					final List<Unit> enemyGroup = eST.getUnits().getMatches(enemySeaUnit);
					totUnitCount += enemyGroup.size();
					final float thisGroupStrength = SUtils.strength(enemyGroup, false, true, tFirst);
					totStrengthEnemyShips += thisGroupStrength;
					if (thisGroupStrength > maxStrength)
					{
						maxStrength = thisGroupStrength;
						largestGroupTerr = eST;
						maxUnitCount = enemyGroup.size();
						largestGroup = enemyGroup;
					}
				}
				// territory with most strength
				if (largestGroupTerr != null)
				{
					float remainingStrength = totStrengthEnemyShips * 1.25F + 2.0F;
					final float shipStrength = SUtils.inviteShipAttack(largestGroupTerr, remainingStrength, xAlreadyMoved2, xMoves2, xRoutes2, data, player, true, tFirst, tFirst);
					// s_logger.fine("Attacking: "+largestGroupTerr.getName()+"; Ship Strength: "+shipStrength);
					remainingStrength -= shipStrength;
					final float planeStrength = SUtils.invitePlaneAttack(false, false, largestGroupTerr, remainingStrength, xAlreadyMoved2, xMoves2, xRoutes2, data, player);
					remainingStrength -= planeStrength;
					final float thisAttackStrength = shipStrength + planeStrength;
					if (thisAttackStrength > maxStrength) // what happens if we knock out the biggest group?
					{
						final float newStrength = totStrengthEnemyShips - thisAttackStrength;
						// int remainingUnitCount = totUnitCount - maxUnitCount;
						final List<Unit> ourShips = new ArrayList<Unit>();
						for (final Collection<Unit> shipGroup : xMoves2)
						{
							ourShips.addAll(shipGroup);
						}
						final IntegerMap<UnitType> ourUnits = SUtils.convertListToMap(ourShips);
						// int ourOriginalCount = ourUnits.totalValues();
						final IntegerMap<UnitType> enemyUnits = SUtils.convertListToMap(largestGroup);
						final List<PlayerID> ePlayers = SUtils.getEnemyPlayers(data, player);
						final PlayerID ePlayer = ePlayers.get(0);
						final boolean weWin = SUtils.quickBattleEstimator(ourUnits, enemyUnits, player, ePlayer, true, Properties.getAirAttackSubRestricted(data));
						final float adjustedStrength = SUtils.strength(player.getUnits().getMatches(seaAirUnit), true, true, tFirst);
						if (newStrength < adjustedStrength) // ATTACK!
							attackGroup = true;
						else if (weWin)
						{
							// number of enemy ships remaining? appears to be confusion here. battle estimator modifies the maps.
							final int remainingShips = totUnitCount - (maxUnitCount - enemyUnits.totalValues());
							// -2 in line with original above. though not sure the threshold is required
							if (remainingShips <= player.getUnits().getMatches(seaAirUnit).size() - 2) // ATTACK!
								attackGroup = true;
						}
						if (attackGroup)
						{
							s_logger.finer("SCM moving " + xMoves2 + " to attack big threat on sea factory: " + xRoutes2);
							moveUnits.addAll(xMoves2);
							moveRoutes.addAll(xRoutes2);
							unitsAlreadyMoved.addAll(xAlreadyMoved2);
							seaTerrAttacked.add(largestGroupTerr);
						}
					}
				} // end largest group is not null
					// TODO: do not perform non combat movements here
				int localMax = 0, localShipCount = 0;
				for (final Territory x : data.getMap().getNeighbors(seaPlaceFact, Matches.TerritoryIsWater))
				{
					// check this, allied units includes own.
					localShipCount = x.getUnits().countMatches(alliedSeaUnit);
					if (localShipCount > localMax)
					{
						maxShipsTerr = x;
						localMax = localShipCount;
					}
				}
				if (maxShipsTerr != null) // This whole code block was only supposed to do stuff if maxShipsTerr was not-null, so just skip it if the var is null(even though it doesn't cause errors)
				{
					final List<Territory> shipTerrAtCapitol = SUtils.findOnlyMyShips(seaPlaceFact, data, player, Matches.UnitIsSea);
					for (final Territory xT : shipTerrAtCapitol)
					{
						final float shipBuildupTerStrength = SUtils.strength(maxShipsTerr.getUnits().getMatches(Matches.UnitIsSea), false, true, tFirst);
						final float shipBuildupTerEnemyStrength = SUtils.getStrengthOfPotentialAttackers(maxShipsTerr, data, player, tFirst, false, new ArrayList<Territory>());
						final Route amphiRoute = getAmphibRoute(player, false);
						// This is the second half of the transport bug fix for when AI doesn't move amphibiously in some situations, such as America on Great War
						if (shipBuildupTerStrength >= shipBuildupTerEnemyStrength) // This buildup ter is already safe
						{
							// and we either aren't amphibious, or the buildup ter is not neighboring our amphibious target(like it could be for Japan), so break because we don't need to consolidate everything else to this ter
							if (amphiRoute == null || !data.getMap().getNeighbors(maxShipsTerr).contains(amphiRoute.getEnd()))
							{
								break;
							}
						}
						final List<Unit> myShips = xT.getUnits().getMatches(Matches.unitIsOwnedBy(player));
						myShips.removeAll(unitsAlreadyMoved);
						if (myShips.isEmpty())
						{
							continue;
						}
						final int shipDist = MoveValidator.getLeastMovement(myShips);
						final Route localShipRoute = SUtils.getMaxSeaRoute(data, xT, maxShipsTerr, player, true, shipDist);
						if (localShipRoute != null)
						{
							if (myShips.size() > 0)
							{
								s_logger.finer("SCM moving " + myShips + " to consolidate at factory Route: " + localShipRoute);
								moveUnits.add(myShips);
								moveRoutes.add(localShipRoute);
								unitsAlreadyMoved.addAll(myShips);
								seaTerrAttacked.add(maxShipsTerr);
								enemyTerr.remove(maxShipsTerr);
							}
						}
					}
				}
			}
			/*			if (!attackGroup && seaPlaceFact != null)
						{
							setKeepShipsAtBase(true);
							if (!seaTerrSet)
								setSeaTerr(seaPlaceFact);
							List<Unit> xMoved = new ArrayList<Unit>();
							markBaseShips(data, player, xMoved);
							unitsAlreadyMoved.addAll(xMoved);
						}
						*/
		}
		for (final Territory t2 : enemyTerr)
		{
			final List<Collection<Unit>> xMoves = new ArrayList<Collection<Unit>>();
			final List<Route> xRoutes = new ArrayList<Route>();
			final List<Unit> xAlreadyMoved = new ArrayList<Unit>(unitsAlreadyMoved);
			// List<Collection<Unit>> xPMoves = new ArrayList<Collection<Unit>>();
			// List<Route> xPRoutes = new ArrayList<Route>();
			// List<Unit> xPAlreadyMoved = new ArrayList<Unit>(unitsAlreadyMoved);
			final Territory enemy = t2;
			final float enemyStrength = sortTerritories.get(enemy).floatValue();
			final List<Unit> enemySubs = t2.getUnits().getMatches(enemySub);
			final float subStrength = SUtils.strength(enemySubs, false, true, tFirst);
			float strengthNeeded = attackFactor * enemyStrength + 3.0F;
			float ourStrength = 0.0F, alliedStrength = 0.0F;
			float maxStrengthNeeded = 2.4F * enemyStrength + 3.0F;
			// float minStrengthNeeded = Math.min(strengthNeeded + 5.0F, maxStrengthNeeded);
			float minStrengthNeeded = strengthNeeded;
			// float starterStrength = minStrengthNeeded;
			// pointless? if transports can be casualties and str == 0 is impossible
			// if (tFirst && enemyStrength == 0.0F)
			// continue;
			attackUnits.clear();
			attackRoute.clear();
			float planeStrength = 0.0F;
			final boolean AttackShipsPresent = enemy.getUnits().someMatch(enemyNonTransport);
			/**
			 * If only transports:
			 * 1) What is the potential Attack @ t
			 * 2) Do we have a ship unit block large enough to take it out?
			 * 3) If not, can we send planes without moving there and stay away from danger where we are?
			 * Remember that this will be low on the strength list, so already looked at major attacks
			 */
			boolean shipsAttacked = false;
			// Added by Wisconsin to detect if there are any units with a defense value greater than zero
			boolean foundEnemyUnitWithDefense = false;
			for (final Unit enemyUnit : enemy.getUnits().getUnits())
			{
				if (UnitAttachment.get(enemyUnit.getUnitType()).getDefense(enemyUnit.getOwner()) > 0)
				{
					foundEnemyUnitWithDefense = true;
				}
			}
			if (!AttackShipsPresent || !foundEnemyUnitWithDefense) // All transports (or ships with no defense)
			{
				if (!tFirst || !foundEnemyUnitWithDefense)
				{
					minStrengthNeeded = 1.0F;
					maxStrengthNeeded = 1.0F;
				}
				planeStrength = SUtils.invitePlaneAttack(false, false, enemy, minStrengthNeeded, xAlreadyMoved, xMoves, xRoutes, data, player);
				maxStrengthNeeded -= planeStrength;
				minStrengthNeeded -= planeStrength;
				boolean nonTransport = planeStrength > 0.0F;
				if (maxStrengthNeeded > 0.0F)
				{
					final float shipStrength = SUtils.inviteShipAttack(enemy, maxStrengthNeeded, xAlreadyMoved, xMoves, xRoutes, data, player, true, tFirst, tFirst);
					if (planeStrength == 0.0F)
					{
						for (final Collection<Unit> xUnits : xMoves)
						{
							for (final Unit thisUnit : xUnits)
							{
								if (Matches.UnitIsNotTransport.match(thisUnit))
									nonTransport = true;
							}
						}
					}
					minStrengthNeeded -= shipStrength;
					maxStrengthNeeded -= shipStrength;
				}
				if (nonTransport && minStrengthNeeded <= 0.0F)
				{
					seaTerrAttacked.add(enemy);
					moveRoutes.addAll(xRoutes);
					for (final Collection<Unit> xUnits : xMoves)
						moveUnits.add(xUnits);
					s_logger.finer("SCM moving " + xMoves + " to attack undefendables: " + xRoutes);
					unitsAlreadyMoved.addAll(xAlreadyMoved);
					// not used
					// if (maxStrengthNeeded > 0.0F)
					// checkForMorePlanes.put(enemy, maxStrengthNeeded);
				}
				continue;
			}
			// starterStrength = minStrengthNeeded;
			final boolean subNeedsDestroyer = Properties.getAirAttackSubRestricted(data);
			final boolean enemySubsOnly = enemy.getUnits().allMatch(Matches.UnitIsSub);
			float shipStrength = 0.0F, destroyerStrength = 0.0F;
			// only subs and subs are free targets for planes, only send ships if we can't send planes
			if (enemySubsOnly && !subNeedsDestroyer)
			{
				planeStrength = SUtils.invitePlaneAttack(false, false, enemy, minStrengthNeeded, xAlreadyMoved, xMoves, xRoutes, data, player);
				minStrengthNeeded -= planeStrength;
				if (planeStrength <= 0.0F) // attack with ships???
				{
					shipStrength = SUtils.inviteShipAttack(enemy, minStrengthNeeded, xAlreadyMoved, xMoves, xRoutes, data, player, true, tFirst, tFirst);
					minStrengthNeeded -= shipStrength;
				}
			}
			else
			// all other cases
			{
				if (subNeedsDestroyer && subStrength > 0.0F) // send a destroyer if we need em
				{
					destroyerStrength = SUtils.inviteShipAttack(enemy, 1, xAlreadyMoved, xMoves, xRoutes, data, player, true, tFirst, tFirst, Matches.UnitIsDestroyer);
					minStrengthNeeded -= destroyerStrength;
					/*
					Route destroyerRoute = SUtils.findNearest(enemy, Matches.TerritoryHasOwnedDestroyer(player), Matches.TerritoryIsWater, data);
					if (destroyerRoute != null && destroyerRoute.getLength() <= 2)
					{
						Territory destroyerTerr = destroyerRoute.getEnd();
						if (destroyerTerr != null)
						{
							List<Unit> destroyers = destroyerTerr.getUnits().getMatches(myDestroyer);
							destroyers.removeAll(unitsAlreadyMoved);
							destroyerStrength = SUtils.strength(destroyers, true, true, tFirst);
							minStrengthNeeded -= destroyerStrength;
						}
					}
					*/
				}
				// invite attack
				shipStrength = SUtils.inviteShipAttack(enemy, minStrengthNeeded, xAlreadyMoved, xMoves, xRoutes, data, player, true, tFirst, tFirst);
				minStrengthNeeded -= shipStrength;
				shipStrength += destroyerStrength;
				planeStrength = SUtils.invitePlaneAttack(false, false, enemy, minStrengthNeeded, xAlreadyMoved, xMoves, xRoutes, data, player);
				minStrengthNeeded -= planeStrength;
			}
			if (shipStrength > 0.0F)
				shipsAttacked = true; // we have sent some ships
			ourStrength += shipStrength;
			// alliedStrength += shipStrength;
			ourStrength += planeStrength;
			alliedStrength = ourStrength;
			// implies attacking with only planes increase threshold
			// i.e. only go through with attack if we can add something else
			// dubious this has value, invalid way to handle in any case
			// if (planeStrength > strengthNeeded && !shipsAttacked && !enemySubsOnly) //good chance of losing a plane
			// {
			// starterStrength += 3.0F;
			// minStrengthNeeded = Math.max(minStrengthNeeded, 3.0F);
			// }
			// if we can't win yet, calculate local allied ship str. our own ships are allied!
			if (minStrengthNeeded > 0.0F)
			{
				// needs rewrite, arbitrary distance
				final Set<Territory> alliedCheck = data.getMap().getNeighbors(enemy, 2); // TODO: assumption of distance = 2
				for (final Territory qAlliedCheck : alliedCheck)
				{
					final List<Unit> qAlliedUnits = qAlliedCheck.getUnits().getMatches(alliedSeaAirUnitNotOwned);
					// qAlliedUnits.removeAll(unitsAlreadyMoved);
					alliedStrength += SUtils.strength(qAlliedUnits, true, true, tFirst);
				}
				// if local str is sufficient, make attack 15% more likely, not required
				// if (alliedStrength > strengthNeeded)
				// minStrengthNeeded -= (strengthNeeded*0.15F);
			}
			// boolean considerSubStrength = true;
			final boolean destroyerAttacked = destroyerStrength > 0.0F;
			if (!subNeedsDestroyer && !shipsAttacked) // only planes...enemy sub strength doesn't matter
			{
				strengthNeeded -= subStrength; // ignore subs
				// minStrengthNeeded -= subStrength*attackFactor;
				maxStrengthNeeded -= subStrength * 2.4F + 3.0F; // don't invite overkill to a won fight
				// considerSubStrength = false;
			}
			// add overkill? has this been changed from maxStr at some point???
			// lets not suck planes up here, let them decide later
			maxStrengthNeeded -= ourStrength;
			if (maxStrengthNeeded > 0.0F)
			{
				// starterStrength = maxStrengthNeeded;
				// maxStrengthNeeded -= ourStrength;
				// float newPlaneStrength = SUtils.invitePlaneAttack(false, false, enemy, maxStrengthNeeded, xAlreadyMoved, xMoves, xRoutes, data, player);
				// ourStrength += newPlaneStrength;
				// planeStrength += newPlaneStrength;
				// maxStrengthNeeded -= newPlaneStrength;
				shipStrength = SUtils.inviteShipAttack(enemy, maxStrengthNeeded, xAlreadyMoved, xMoves, xRoutes, data, player, true, tFirst, tFirst);
				// if (shipStrength > 0.0F)
				// shipsAttacked = true;
				ourStrength += shipStrength;
				// minStrengthNeeded -= shipStrength;
			}
			// not possible.
			// if (!considerSubStrength && shipsAttacked)
			// strengthNeeded += subStrength;
			final boolean weCanAttack = (subNeedsDestroyer && enemySubsOnly) ? (destroyerAttacked) : true;
			final boolean alliedSuperiority = alliedStrength > strengthNeeded;
			// weCanAttack and we have enough strength or we have local supperiority and we have 86% of str (i.e. we are sacrificing)
			if (weCanAttack && ((ourStrength > strengthNeeded) || (alliedSuperiority && ourStrength > 0.86F * strengthNeeded)))
			{
				s_logger.finer("SCM moving " + xMoves + " to attack " + enemy + " route: " + xRoutes);
				seaTerrAttacked.add(enemy);
				moveRoutes.addAll(xRoutes);
				// for (Collection<Unit> xUnits : xMoves)
				// moveUnits.add(xUnits);
				moveUnits.addAll(xMoves);
				unitsAlreadyMoved.addAll(xAlreadyMoved);
				// maxStrengthNeeded -= ourStrength;
				// not used currently
				// if (maxStrengthNeeded > 0.0F)
				// checkForMorePlanes.put(enemy, maxStrengthNeeded);
			}
			// pointless
			// shipsAttacked = false;
		}
		setSeaTerrAttacked(seaTerrAttacked);
		// SUtils.verifyMoves(moveUnits, moveRoutes, data, player);
	}
	
	/*
	private Route getAlternativeAmphibRoute(final PlayerID player)
	{
		if (!isAmphibAttack(player, false))
			return null;
		
		final GameData data = getPlayerBridge().getGameData();
		Match<Territory> routeCondition = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data));
		
		// should select all territories with loaded transports
		Match<Territory> transportOnSea = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasLandUnitsOwnedBy(player));
		Route altRoute = null;
		int length = Integer.MAX_VALUE;
		for (Territory t : data.getMap())
		{
			if (!transportOnSea.match(t))
				continue;
			CompositeMatchAnd<Unit> ownedTransports = new CompositeMatchAnd<Unit>(Matches.UnitCanTransport, Matches.unitIsOwnedBy(player), HasntMoved);
			CompositeMatchAnd<Territory> enemyTerritory = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemy(player, data), Matches.TerritoryIsLand, new InverseMatch<Territory>(
						Matches.TerritoryIsNeutral), Matches.TerritoryIsEmpty);
			int trans = t.getUnits().countMatches(ownedTransports);
			if (trans > 0)
			{
				Route newRoute = SUtils.findNearest(t, enemyTerritory, routeCondition, data);
				if (newRoute != null && length > newRoute.getLength())
				{
					altRoute = newRoute;
				}
			}
		}
		return altRoute;
	}
	*/
	/**
	 * check for planes that need to land
	 */
	private void CheckPlanes(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	// check for planes that need to move
	// don't let planes stay in territory alone if it can be attacked
	// we've already check Carriers in moveNonComPlanes
	{
		final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
		final Match<Territory> canLand = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), new Match<Territory>()
		{
			@Override
			public boolean match(final Territory o)
			{
				return !delegate.getBattleTracker().wasConquered(o);
			}
		});
		final Match<Unit> bomberUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsStrategicBomber);
		final Match<Territory> routeCondition = new CompositeMatchAnd<Territory>(Matches.territoryHasEnemyAAforCombatOnly(player, data).invert(), Matches.TerritoryIsPassableAndNotRestricted(player,
					data));
		final Match<Unit> fighterUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanLandOnCarrier);
		final Territory myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		List<Territory> planeTerr = new ArrayList<Territory>();
		planeTerr = SUtils.TerritoryOnlyPlanes(data, player);
		planeTerr.remove(myCapital);
		if (planeTerr.size() == 0) // skip...no loner planes
			return;
		for (final Territory t : planeTerr)
		{
			final List<Unit> airUnits = t.getUnits().getMatches(fighterUnit);
			final List<Unit> bombUnits = t.getUnits().getMatches(bomberUnit);
			final List<Unit> sendFighters = new ArrayList<Unit>();
			final List<Unit> sendBombers = new ArrayList<Unit>();
			final Route route2 = SUtils.findNearestNotEmpty(t, canLand, routeCondition, data);
			if (route2 == null)
				continue;
			// Territory endTerr = route2.getTerritories().get(route2.getLength());
			int sendNum = 0;
			for (final Unit f : airUnits)
			{
				if (MoveValidator.hasEnoughMovement(f, route2))
				{
					sendFighters.add(f);
					sendNum++;
				}
			}
			if (sendNum > 0)
			{
				moveUnits.add(sendFighters);
				moveRoutes.add(route2);
			}
			sendNum = 0;
			for (final Unit b : bombUnits)
			{
				if (MoveValidator.hasEnoughMovement(b, route2))
				{
					sendBombers.add(b);
					sendNum++;
				}
			}
			if (sendNum > 0)
			{
				moveUnits.add(sendBombers);
				moveRoutes.add(route2);
			}
		}
	}
	
	private void stopBlitzAttack(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		setImpassableTerrs(player);
		final Collection<Territory> impassableTerrs = getImpassableTerrs();
		// CompositeMatch<Unit> myUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsInfantry, HasntMoved);
		final CompositeMatch<Unit> alliedLandUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsLand);
		final CompositeMatch<Unit> blitzBlocker = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitCanNotProduceUnits, Matches.UnitIsNotInfrastructure, Matches.UnitIsNotAA);
		final CompositeMatch<Unit> anyUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanNotProduceUnits, Matches.UnitIsNotInfrastructure, Matches.UnitIsNotAA);
		boolean capDanger = getCapDanger(); // do not mark units at capital for non-movement
		final boolean tFirst = transportsMayDieFirst();
		final Territory myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		final List<Route> blitzTerrRoutes = new ArrayList<Route>();
		final float enemyStrength = SUtils.getStrengthOfPotentialAttackers(myCapital, data, player, tFirst, false, null);
		final float ourStrength = SUtils.strength(myCapital.getUnits().getUnits(), false, false, tFirst);
		final List<Territory> TerrToBlock = new ArrayList<Territory>();
		final List<Territory> possBlitzTerr = SUtils.possibleBlitzTerritories(myCapital, data, player);
		final List<Territory> cantBlockList = new ArrayList<Territory>();
		for (final Territory pB : possBlitzTerr)
		{
			if (Matches.isTerritoryEnemy(player, data).match(pB))
			{
				cantBlockList.addAll(data.getMap().getNeighbors(pB, Matches.territoryHasEnemyBlitzUnits(player, data)));
			}
		}
		cantBlockList.removeAll(possBlitzTerr); // could be overlaps here
		final float blitzStrength = SUtils.determineEnemyBlitzStrength(myCapital, blitzTerrRoutes, null, data, player);
		boolean noChangeOnPass = blitzStrength > 0.0F;
		while (noChangeOnPass)
		{
			boolean listChanged = false;
			for (final Route bRoute : blitzTerrRoutes)
			{
				if (bRoute != null && !cantBlockList.contains(bRoute.getStart()))
				{
					final Territory midTerr = bRoute.getTerritories().get(1);
					if (!TerrToBlock.contains(midTerr) && Matches.isTerritoryFriendly(player, data).match(midTerr))
					{
						listChanged = true;
						TerrToBlock.add(midTerr);
					}
				}
			}
			if (!listChanged)
				noChangeOnPass = false;
			blitzTerrRoutes.clear();
			SUtils.determineEnemyBlitzStrength(myCapital, blitzTerrRoutes, TerrToBlock, data, player);
		}
		if (blitzStrength == 0.0F)
			return;
		if (enemyStrength - blitzStrength < ourStrength) // removing blitzers eliminates the threat to cap
			capDanger = false; // do everything to clear them out
		final List<Territory> capNeighbors = SUtils.getNeighboringLandTerritories(data, player, myCapital);
		final List<Territory> capDoNotUse = new ArrayList<Territory>();
		final Iterator<Territory> capIter = capNeighbors.iterator();
		while (capIter.hasNext())
		{
			final Territory thisCapTerr = capIter.next();
			if (thisCapTerr.getUnits().countMatches(blitzBlocker) <= 1)
			{
				capIter.remove();
				capDoNotUse.add(thisCapTerr);
			}
		}
		final List<Unit> alreadyMoved = new ArrayList<Unit>();
		final List<Territory> goBlockTerr = new ArrayList<Territory>();
		final HashMap<Territory, Float> blockTerrMap = new HashMap<Territory, Float>();
		for (final Territory blockTerr : TerrToBlock)
		{
			final List<Territory> myNeighbors = SUtils.getNeighboringLandTerritories(data, player, blockTerr);
			myNeighbors.removeAll(capDoNotUse);
			myNeighbors.removeAll(goBlockTerr);
			for (final Territory myTerr : myNeighbors)
			{
				final float attackStrength = SUtils.getStrengthOfPotentialAttackers(myTerr, data, player, tFirst, true, null);
				blockTerrMap.put(myTerr, attackStrength);
			}
			goBlockTerr.addAll(myNeighbors);
		}
		SUtils.reorder(goBlockTerr, blockTerrMap, false);
		if (capDanger)
			goBlockTerr.remove(myCapital);
		for (final Territory moveFrom : goBlockTerr)
		{
			final List<Unit> ourUnits = moveFrom.getUnits().getMatches(anyUnit);
			final List<Unit> alliedUnits = moveFrom.getUnits().getMatches(Matches.alliedUnit(player, data));
			final float eStrength = blockTerrMap.get(moveFrom);
			float aStrength = SUtils.strength(alliedUnits, false, false, true);
			final Set<Territory> moveFromNeighbors = data.getMap().getNeighbors(moveFrom, Matches.territoryHasNoEnemyUnits(player, data));
			moveFromNeighbors.removeAll(impassableTerrs);
			moveFromNeighbors.retainAll(TerrToBlock);
			if (moveFromNeighbors.isEmpty())
				continue;
			final Iterator<Territory> neighborIter = moveFromNeighbors.iterator();
			if (aStrength > eStrength && ourUnits.size() > 1)
			{
				final Iterator<Unit> ourIter = ourUnits.iterator();
				while (ourIter.hasNext() && ourUnits.size() > 1 && aStrength > eStrength && neighborIter.hasNext())
				{
					final Unit unit = ourIter.next();
					final Territory moveHere = neighborIter.next();
					final Route moveRoute = data.getMap().getLandRoute(moveFrom, moveHere);
					if (moveRoute != null && MoveValidator.hasEnoughMovement(unit, moveRoute))
					{
						moveUnits.add(Collections.singleton(unit));
						moveRoutes.add(moveRoute);
						alreadyMoved.add(unit);
						neighborIter.remove();
						TerrToBlock.remove(moveHere);
						aStrength -= SUtils.uStrength(unit, false, false, tFirst);
					}
				}
			}
		}
		if (TerrToBlock.size() > 0) // still more to move
		{
			final Iterator<Territory> bIter = TerrToBlock.iterator();
			while (bIter.hasNext())
			{
				final float strengthNeeded = 1.0F;
				final Territory moveHere = bIter.next();
				final float bStrength = SUtils.inviteBlitzAttack(true, moveHere, strengthNeeded, alreadyMoved, moveUnits, moveRoutes, data, player, false, capDanger);
				if (bStrength > 0.0F)
					bIter.remove();
			}
		}
		if (TerrToBlock.size() > 0) // still more
		{
			for (final Territory xTerr : goBlockTerr)
			{
				final Set<Territory> goBTerrs = data.getMap().getNeighbors(xTerr, Matches.territoryHasNoEnemyUnits(player, data));
				goBTerrs.removeAll(impassableTerrs);
				goBTerrs.retainAll(TerrToBlock);
				if (goBTerrs.isEmpty())
					continue;
				final List<Unit> ourUnits = xTerr.getUnits().getMatches(anyUnit);
				final List<Unit> alliedUnits = xTerr.getUnits().getMatches(alliedLandUnit);
				final boolean canGo = (alliedUnits.size() > ourUnits.size()) || (alliedUnits.size() > 1);
				ourUnits.removeAll(alreadyMoved);
				if (!canGo || ourUnits.size() == 0)
					continue;
				if (canGo)
				{
					final Iterator<Territory> goBIter = goBTerrs.iterator();
					final Iterator<Unit> unitIter = ourUnits.iterator();
					final boolean movedIn = false;
					while (unitIter.hasNext() && !movedIn && goBIter.hasNext() && alliedUnits.size() > 1)
					{
						final Territory goTerr = goBIter.next();
						final Route unitRoute = data.getMap().getLandRoute(xTerr, goTerr);
						if (unitRoute == null)
							continue;
						final Unit nextUnit = unitIter.next();
						if (MoveValidator.hasEnoughMovement(nextUnit, unitRoute))
						{
							moveUnits.add(Collections.singleton(nextUnit));
							moveRoutes.add(unitRoute);
							alreadyMoved.add(nextUnit);
							TerrToBlock.remove(goTerr);
							unitIter.remove();
							alliedUnits.remove(nextUnit);
						}
					}
				}
			}
		}
		if (TerrToBlock.size() > 0)
		{
			for (final Territory checkAgain : TerrToBlock)
			{
				final float strengthNeeded = 1.0F;
				SUtils.invitePlaneAttack(true, false, checkAgain, strengthNeeded, alreadyMoved, moveUnits, moveRoutes, data, player);
			}
		}
	}
	
	private void SetCapGarrison(final Territory myCapital, final PlayerID player, final float totalInvasion, final Collection<Unit> alreadyMoved)
	{
		// Make sure we keep enough units in the capital for defense.
		final CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsNotSea, Matches.UnitIsNotAA, Matches.UnitIsNotInfrastructure,
					Matches.unitHasNotMoved.invert());
		final List<Unit> myCapUnits = myCapital.getUnits().getMatches(landUnit);
		float capGarrisonStrength = 0.0F;
		for (final Unit x : myCapUnits)
		{
			if ((capGarrisonStrength * 0.9F - 3F) <= totalInvasion)
			{
				capGarrisonStrength += SUtils.uStrength(x, false, false, false);
				alreadyMoved.add(x);
			}
		}
		if (capGarrisonStrength < totalInvasion)
		{
			final CompositeMatch<Unit> landUnit2 = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsNotSea, Matches.UnitIsNotAA, Matches.UnitIsNotInfrastructure,
						Matches.unitHasNotMoved);
			final List<Unit> myCapUnits2 = myCapital.getUnits().getMatches(landUnit2);
			for (final Unit x : myCapUnits2)
			{
				if ((capGarrisonStrength * 0.9F - 3F) <= totalInvasion)
				{
					capGarrisonStrength += SUtils.uStrength(x, false, false, false);
					alreadyMoved.add(x);
				}
			}
		}
	}
	
	private void populateNonCombat(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		float ourStrength = 0.0F, attackerStrength = 0.0F;
		float totalInvasion = 0.0F, ourCapStrength = 0.0F;
		boolean capDanger = false;
		final boolean tFirst = transportsMayDieFirst();
		// Collection<Territory> territories = data.getMap().getTerritories();
		final List<Territory> emptiedTerr = new ArrayList<Territory>();
		final List<Territory> fortifiedTerr = new ArrayList<Territory>();
		final List<Territory> alliedTerr = SUtils.allAlliedTerritories(data, player);
		final Territory myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		final List<Territory> movedInto = new ArrayList<Territory>();
		final List<Unit> alreadyMoved = new ArrayList<Unit>();
		final CompositeMatchAnd<Territory> moveThrough = new CompositeMatchAnd<Territory>(Matches.TerritoryIsPassableAndNotRestricted(player, data), Matches.TerritoryIsNotNeutralButCouldBeWater,
					Matches.TerritoryIsLand);
		final CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsNotSea, Matches.UnitIsNotAA, Matches.UnitIsNotInfrastructure,
					Matches.UnitCanNotProduceUnits, Matches.unitHasNotMoved);
		final CompositeMatch<Unit> infantryUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsInfantry);
		final CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir);
		final CompositeMatch<Unit> alliedUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsLand);
		final CompositeMatch<Unit> myTransportUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsTransport);
		// populate alreadyMoved before we begin
		for (final Unit u : data.getUnits().getUnits())
		{
			if (u.getOwner().equals(player) && TripleAUnit.get(u).getMovementLeft() < 1)
				alreadyMoved.add(u);
		}
		final List<Territory> alreadyAttacked = Collections.emptyList();
		// Look at Capital before anything else
		final float dangerFactor = 1.05F;
		ourCapStrength = SUtils.strength(myCapital.getUnits().getUnits(), false, false, tFirst);
		totalInvasion = SUtils.getStrengthOfPotentialAttackers(myCapital, data, player, tFirst, true, null);
		final StrengthEvaluator capStrEval = StrengthEvaluator.evalStrengthAt(data, player, myCapital, false, true, tFirst, true);
		final boolean directCapDanger = totalInvasion > (ourCapStrength * 0.95F - 3.00F);
		capDanger = capStrEval.inDanger(dangerFactor);
		final List<Territory> badNeighbors = SUtils.getNeighboringEnemyLandTerritories(data, player, myCapital);
		final List<Territory> myNeighbors = SUtils.getNeighboringLandTerritories(data, player, myCapital);
		for (final Territory neighborTerr : myNeighbors)
		{
			final List<Territory> nextNeighbors = SUtils.getNeighboringEnemyLandTerritories(data, player, neighborTerr);
			nextNeighbors.removeAll(badNeighbors);
			if (nextNeighbors.size() > 0)
			{
				final List<Unit> myUnits = neighborTerr.getUnits().getMatches(landUnit);
				if (myUnits.size() > 0) // make sure we have units
				{
					final int leastMove = MoveValidator.getLeastMovement(myUnits); // Are there units that cannot be moved
					if (leastMove > 0)
					{
						final List<Unit> infUnits = neighborTerr.getUnits().getMatches(infantryUnit);
						if (infUnits.size() > 0)
						{
							final Unit oneUnit = infUnits.get(0);
							s_logger.finer("PNC stationary " + oneUnit + "in " + neighborTerr + " defend around capital");
							alreadyMoved.add(oneUnit);
						}
						else
						{
							final Unit oneUnit = myUnits.get(0);
							s_logger.finer("PNC stationary " + oneUnit + "in " + neighborTerr + " defend around capital");
							alreadyMoved.add(oneUnit);
						}
					}
				}
			}
		}
		final HashMap<Territory, Float> SNeighbor = new HashMap<Territory, Float>();
		for (final Territory xNeighbor : myNeighbors)
		{
			SNeighbor.put(xNeighbor, SUtils.getStrengthOfPotentialAttackers(xNeighbor, data, player, tFirst, true, null));
		}
		SUtils.reorder(myNeighbors, SNeighbor, false);
		final HashMap<Territory, Float> addStrength = new HashMap<Territory, Float>();
		for (final Territory qT : alliedTerr)
		{
			addStrength.put(qT, 0.0F);
		}
		if (directCapDanger) // borrow some units
		{
			/*
			 * First pass, only remove units necessary, but maintain a strong defense
			 * Second pass, ignore defense of neighbors and protect capitol
			 */
			for (final Territory tx3 : myNeighbors)
			{
				if ((ourCapStrength * dangerFactor - 3.00F) > totalInvasion)
					continue;
				final float stayAboveStrength = SNeighbor.get(tx3).floatValue() * 0.75F;
				final List<Unit> allUnits = tx3.getUnits().getMatches(landUnit);
				float currStrength = SUtils.strength(allUnits, false, false, tFirst);
				if (currStrength < stayAboveStrength)
					continue;
				final List<Unit> sendIn = new ArrayList<Unit>();
				final Iterator<Unit> uIter = allUnits.iterator();
				while (uIter.hasNext() && (((ourCapStrength * dangerFactor - 3.00F) <= totalInvasion) && currStrength > stayAboveStrength))
				{
					final Unit x = uIter.next();
					final float uStrength = SUtils.uStrength(x, false, false, tFirst);
					ourCapStrength += uStrength;
					currStrength -= uStrength;
					sendIn.add(x);
				}
				final Route quickRoute = data.getMap().getLandRoute(tx3, myCapital);
				moveUnits.add(sendIn);
				moveRoutes.add(quickRoute);
				s_logger.finer("PNC defend capital " + sendIn + " route " + quickRoute);
				alreadyMoved.addAll(sendIn);
			}
			final List<Collection<Unit>> xMoveUnits = new ArrayList<Collection<Unit>>();
			final List<Route> xMoveRoutes = new ArrayList<Route>();
			float remainingStrengthNeeded = (totalInvasion - (ourCapStrength * dangerFactor - 3.00F)) * 1.05F;
			float blitzStrength = 0.0F, planeStrength = 0.0F;
			final float transStrength = 0.0F;
			float landStrength = 0.0F;
			if (remainingStrengthNeeded > 0.0F)
				blitzStrength = SUtils.inviteBlitzAttack(true, myCapital, remainingStrengthNeeded, alreadyMoved, xMoveUnits, xMoveRoutes, data, player, false, true);
			remainingStrengthNeeded -= blitzStrength;
			if (remainingStrengthNeeded > 0.0F)
				planeStrength = SUtils.invitePlaneAttack(true, false, myCapital, remainingStrengthNeeded, alreadyMoved, xMoveUnits, xMoveRoutes, data, player);
			remainingStrengthNeeded -= planeStrength;
			// Go Back to the neighbors and pull in what is needed
			if (remainingStrengthNeeded > 0.0F)
				landStrength -= SUtils.inviteLandAttack(true, myCapital, remainingStrengthNeeded, alreadyMoved, xMoveUnits, xMoveRoutes, data, player, false, true, alreadyAttacked);
			remainingStrengthNeeded -= landStrength;
			// if (remainingStrengthNeeded > 0.0F)
			// transStrength = SUtils.inviteTransports(true, myCapital, remainingStrengthNeeded, alreadyMoved, moveUnits, moveRoutes, data, player, tFirst, false, null);
			final List<Unit> myCapUnits = myCapital.getUnits().getMatches(landUnit);
			s_logger.finer("PNC defend capital-abandon surrounding " + xMoveUnits + " route " + xMoveRoutes);
			moveUnits.addAll(xMoveUnits);
			moveRoutes.addAll(xMoveRoutes);
			alreadyMoved.addAll(myCapUnits);
			ourCapStrength += blitzStrength + planeStrength + landStrength + transStrength;
			capDanger = totalInvasion > ourCapStrength;
		}
		if (capDanger) // see if we have units 3/2 away from capitol that we can bring back
		{
			final List<Territory> outerTerrs = SUtils.getExactNeighbors(myCapital, 3, player, data, false);
			final Iterator<Territory> outerIter = outerTerrs.iterator();
			final HashMap<Territory, Float> outerMap = new HashMap<Territory, Float>();
			final HashMap<Territory, Float> outerEMap = new HashMap<Territory, Float>();
			while (outerIter.hasNext())
			{
				final Territory outerTerr = outerIter.next();
				if (outerTerr.isWater() || Matches.isTerritoryAllied(player, data).match(outerTerr) || data.getMap().getLandRoute(outerTerr, myCapital) == null)
					outerIter.remove();
				final float myStrength = SUtils.strength(outerTerr.getUnits().getMatches(landUnit), false, false, tFirst);
				final float outerEStrength = SUtils.getStrengthOfPotentialAttackers(myCapital, data, player, tFirst, true, null);
				outerMap.put(outerTerr, myStrength);
				outerEMap.put(outerTerr, outerEStrength);
			}
			SUtils.reorder(outerTerrs, outerEMap, false); // try based on enemy strength...lowest first
			float strengthNeeded = capStrEval.strengthMissing(dangerFactor);
			for (final Territory outerTerr : outerTerrs) // need combination of closest to capital and least likely to get mauled
			{
				final List<Territory> oTNeighbors = SUtils.getNeighboringLandTerritories(data, player, outerTerr);
				// IntegerMap<Territory> distMap = new IntegerMap<Territory>();
				final HashMap<Territory, Float> oTNMap = new HashMap<Territory, Float>();
				final int checkDist = data.getMap().getLandDistance(outerTerr, myCapital);
				final Iterator<Territory> oTNIter = oTNeighbors.iterator();
				while (oTNIter.hasNext())
				{
					final Territory oTN = oTNIter.next();
					final int oTNDist = data.getMap().getLandDistance(oTN, myCapital);
					final float oTNEStrength = SUtils.getStrengthOfPotentialAttackers(oTN, data, player, tFirst, true, null);
					if (checkDist > oTNDist)
						oTNMap.put(oTN, oTNEStrength);
					else
						oTNIter.remove();
				}
				SUtils.reorder(oTNeighbors, oTNMap, false);
				final float outerEStrength = SUtils.getStrengthOfPotentialAttackers(outerTerr, data, player, tFirst, true, null);
				final List<Unit> ourOuterUnits = outerTerr.getUnits().getMatches(landUnit);
				ourOuterUnits.removeAll(alreadyMoved);
				final List<Unit> ourPlanes = outerTerr.getUnits().getMatches(airUnit);
				ourPlanes.removeAll(alreadyMoved);
				final float thisTerrStrength = SUtils.strength(outerTerr.getUnits().getUnits(), false, false, tFirst);
				float diffStrength = outerEStrength - thisTerrStrength;
				final boolean EAdvantage = diffStrength > 1.5F * thisTerrStrength;
				for (final Territory oTN : oTNeighbors)
				{
					// check and make sure we are not killing this territory
					final Route oTNRoute = data.getMap().getLandRoute(outerTerr, oTN);
					if (oTNRoute == null)
						continue;
					if (EAdvantage && ourOuterUnits.size() > 1 && strengthNeeded > 0.0F) // move all but 1
					{
						moveUnits.add(ourOuterUnits);
						moveRoutes.add(oTNRoute);
						s_logger.finer("PNC move towards capital " + ourOuterUnits + " route " + oTNRoute);
						alreadyMoved.addAll(ourOuterUnits);
						final float strengthAdded = SUtils.strength(ourOuterUnits, false, false, tFirst);
						strengthNeeded -= strengthAdded;
						diffStrength += strengthAdded;
					}
					else if (ourOuterUnits.size() > 1)
					{
						// move some units in to reduce strengthNeeded
						final Iterator<Unit> oIter = ourOuterUnits.iterator();
						final List<Unit> addUnits = new ArrayList<Unit>();
						while (diffStrength < 0.0F && strengthNeeded > 0.0F && oIter.hasNext())
						{
							final Unit oUnit = oIter.next();
							final float oStrength = SUtils.uStrength(oUnit, false, false, tFirst);
							addUnits.add(oUnit);
							strengthNeeded -= oStrength;
							diffStrength += oStrength;
						}
						if (addUnits.size() > 1)
						{
							moveUnits.add(addUnits);
							moveRoutes.add(oTNRoute);
							s_logger.finer("PNC move towards capital2? " + addUnits + " route " + oTNRoute);
							alreadyMoved.addAll(addUnits);
						}
					}
				}
			}
		}
		else
			// Make sure we keep enough units in the capital for defense.
			SetCapGarrison(myCapital, player, totalInvasion, alreadyMoved);
		// eliminate blitz territories first
		final List<Territory> possBlitzTerr = SUtils.possibleBlitzTerritories(myCapital, data, player);
		for (final Territory pB : possBlitzTerr)
		{
			if (Matches.isTerritoryEnemy(player, data).match(pB))
				continue;
			final List<Unit> ourUnits = pB.getUnits().getMatches(landUnit);
			if (ourUnits.isEmpty())
			{
				final float strengthNeeded = 1.0F;
				SUtils.inviteLandAttack(true, pB, strengthNeeded, alreadyMoved, moveUnits, moveRoutes, data, player, false, false, alreadyAttacked);
				continue;
			}
			if (MoveValidator.getLeastMovement(ourUnits) > 0)
			{
				final Unit dontMoveUnit = ourUnits.get(0);
				s_logger.finer("PNC stationary  " + dontMoveUnit + " to prevent blitz though " + pB);
				alreadyMoved.add(dontMoveUnit);
			}
		}
		// List<Territory> beenThere = new ArrayList<Territory>();
		for (final Territory t : alliedTerr)
		{
			if (Matches.TerritoryIsWater.match(t) || !Matches.territoryHasLandUnitsOwnedBy(player).match(t))
				continue;
			// check for blitzable units
			final CompositeMatch<Unit> blitzUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBlitz);
			final CompositeMatch<Territory> enemyPassableNotWater = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemy(player, data), Matches.TerritoryIsPassableAndNotRestricted(player, data),
						Matches.TerritoryIsLand);
			final CompositeMatch<Territory> enemyPassableNotWaterNotNeutral = new CompositeMatchAnd<Territory>(enemyPassableNotWater, Matches.TerritoryIsNotNeutralButCouldBeWater);
			final CompositeMatch<Territory> routeCondition = new CompositeMatchAnd<Territory>(Matches.TerritoryIsPassableAndNotRestricted(player, data), Matches.isTerritoryAllied(player, data));
			final List<Unit> blitzUnits = t.getUnits().getMatches(blitzUnit);
			blitzUnits.removeAll(alreadyMoved);
			Route goRoute = SUtils.findNearest(t, enemyPassableNotWater, routeCondition, data);
			if (goRoute != null)
			{
				final Territory endTerr = goRoute.getEnd();
				if (Matches.TerritoryIsNeutralButNotWater.match(endTerr))
				{
					float pValue = TerritoryAttachment.get(endTerr).getProduction();
					final float enemyStrength = SUtils.strength(endTerr.getUnits().getUnits(), false, false, tFirst);
					final Route xRoute = SUtils.findNearest(t, enemyPassableNotWaterNotNeutral, routeCondition, data);
					if (enemyStrength > pValue * 9) // why bother...focus on enemies
					{
						goRoute = xRoute;
					}
					else
					{ // make sure going in this direction is preferred
						// int neutralDist = goRoute.getLength() + 1;
						if (xRoute != null && xRoute.getEnd() != null)
						{
							final Territory realEnemy = xRoute.getEnd();
							float eValue = TerritoryAttachment.get(realEnemy).getProduction();
							// int enemyDist = xRoute.getLength();
							final Set<Territory> neutralNeighbors = data.getMap().getNeighbors(endTerr, enemyPassableNotWater);
							for (final Territory nTerr : neutralNeighbors)
							{
								int xValue = TerritoryAttachment.get(nTerr).getProduction();
								if (Matches.TerritoryIsNeutralButNotWater.match(nTerr))
								{
									final float testStrength = SUtils.strength(endTerr.getUnits().getUnits(), false, false, tFirst);
									if (testStrength > xValue)
										xValue = 0; // not a neutral we will invade
								}
								pValue += xValue;
							}
							final Set<Territory> enemyNeighbors = data.getMap().getNeighbors(realEnemy, enemyPassableNotWater);
							for (final Territory nTerr : enemyNeighbors)
							{
								final TerritoryAttachment ta = TerritoryAttachment.get(nTerr);
								if (ta != null)
									eValue += ta.getProduction();
							}
							if (pValue < eValue)
								goRoute = xRoute;
						}
						Territory lastTerr = goRoute.getEnd();
						if (Matches.isTerritoryEnemy(player, data).match(lastTerr))
						{
							lastTerr = goRoute.getTerritories().get(goRoute.getLength() - 1);
							goRoute = data.getMap().getRoute(t, lastTerr, Matches.isTerritoryAllied(player, data));
						}
					}
				}
			}
			/*            if (goRoute != null && goRoute.getLength() > 2 && !blitzUnits.isEmpty())
			            {
			            	Route newRoute = new Route();
			            	newRoute.setStart(goRoute.getStart());
			            	newRoute.add(goRoute.getTerritories().get(1));
			            	newRoute.add(goRoute.getTerritories().get(2));
			            	moveUnits.add(blitzUnits);
			            	moveRoutes.add(newRoute);
			            	alreadyMoved.addAll(blitzUnits);
			            }
			*/
			// these are the units we can move
			final CompositeMatch<Unit> moveOfType = new CompositeMatchAnd<Unit>();
			moveOfType.add(Matches.unitIsOwnedBy(player));
			moveOfType.add(Matches.UnitIsNotAA);
			moveOfType.add(Matches.UnitCanNotProduceUnits);
			moveOfType.add(Matches.UnitIsNotInfrastructure);
			moveOfType.add(Matches.UnitIsLand);
			final List<Unit> units = t.getUnits().getMatches(moveOfType);
			units.removeAll(alreadyMoved);
			if (units.size() == 0)
				continue;
			final int minDistance = Integer.MAX_VALUE;
			Territory to = null;
			// Collection<Unit> unitsHere = t.getUnits().getMatches(moveOfType);
			// realStrength = SUtils.strength(unitsHere, false, false, tFirst) - SUtils.allairstrength(unitsHere, false);
			ourStrength = SUtils.strength(t.getUnits().getUnits(), false, false, tFirst) + addStrength.get(t);
			attackerStrength = SUtils.getStrengthOfPotentialAttackers(t, data, player, tFirst, false, null);
			if ((t.getUnits().someMatch(Matches.UnitCanProduceUnits) || t.getUnits().someMatch(Matches.UnitIsInfrastructure)) && t != myCapital)
			{ // protect factories...rather than just not moving, plan to move some in if necessary
				// Don't worry about units that have been moved toward capital
				if (attackerStrength > (ourStrength + 5.0F))
				{
					final List<Territory> myNeighbors2 = SUtils.getNeighboringLandTerritories(data, player, t);
					if (capDanger)
						myNeighbors2.remove(myCapital);
					for (final Territory t3 : myNeighbors2)
					{ // get everything
						final List<Unit> allUnits = t3.getUnits().getMatches(moveOfType);
						final List<Unit> sendIn2 = new ArrayList<Unit>();
						for (final Unit x2 : allUnits)
						{
							if ((ourStrength - 5.0F) < attackerStrength && !alreadyMoved.contains(x2))
							{
								ourStrength += SUtils.uStrength(x2, false, false, tFirst);
								sendIn2.add(x2);
							}
						}
						if (sendIn2.isEmpty())
							continue;
						final Route quickRoute = data.getMap().getLandRoute(t3, t);
						addStrength.put(t, addStrength.get(t) + SUtils.strength(sendIn2, false, false, tFirst));
						moveUnits.add(sendIn2);
						moveRoutes.add(quickRoute);
						s_logger.finer("PNC defend factory/AA " + sendIn2 + " route " + quickRoute);
						alreadyMoved.addAll(sendIn2);
						movedInto.add(t);
					}
				}
				float tmpStrength = 0.0F;
				final List<Unit> tUnits = t.getUnits().getMatches(Matches.unitIsOwnedBy(player));
				final List<Collection<Unit>> tGoUnits = new ArrayList<Collection<Unit>>();
				SUtils.breakUnitsBySpeed(tGoUnits, data, player, tUnits);
				for (final Collection<Unit> tUnits2 : tGoUnits)
				{
					final Iterator<Unit> tUnitIter = tUnits2.iterator();
					while (tmpStrength < attackerStrength && tUnitIter.hasNext())
					{
						final Unit xUnit = tUnitIter.next();
						s_logger.finer("PNC stationary defend factory/AA " + xUnit + " in " + t);
						alreadyMoved.add(xUnit); // lock them down
						tmpStrength += SUtils.uStrength(xUnit, false, false, tFirst);
					}
				}
			}
			// if an emminent attack on capital, pull back toward capital
			final List<Territory> myENeighbors = SUtils.getNeighboringEnemyLandTerritories(data, player, t);
			Route retreatRoute = null;
			if (myENeighbors.size() == 0 && ((ourCapStrength * 1.08F + 5.0F) < totalInvasion))
			{ // this territory has no enemy neighbors...pull back toward capital
				if (t != myCapital && data.getMap().getLandRoute(t, myCapital) != null)
				{
					final List<Territory> myNeighbors3 = SUtils.getNeighboringLandTerritories(data, player, t);
					myNeighbors3.remove(myCapital);
					int minCapDist = 100;
					Territory targetTerr = null;
					for (final Territory myTerr : myNeighbors3)
					{
						final int thisCapDist = data.getMap().getLandDistance(myTerr, myCapital);
						if (thisCapDist < minCapDist)
						{
							minCapDist = thisCapDist;
							targetTerr = myTerr;
							retreatRoute = data.getMap().getLandRoute(t, myTerr);
						}
					}
					if (retreatRoute != null)
					{
						final List<Unit> myMoveUnits = t.getUnits().getMatches(landUnit);
						myMoveUnits.removeAll(alreadyMoved);
						final int totUnits2 = myMoveUnits.size();
						final List<Unit> myRealMoveUnits = new ArrayList<Unit>();
						for (int i3 = 0; i3 < totUnits2; i3++)
						{
							if ((ourCapStrength * 1.08F + 5.0F) < totalInvasion)
							{
								final Unit moveThisUnit = myMoveUnits.get(i3);
								ourCapStrength += 0.5F * SUtils.uStrength(moveThisUnit, true, false, tFirst);
								myRealMoveUnits.add(moveThisUnit);
							}
						}
						if (myRealMoveUnits.size() > 0)
						{
							addStrength.put(targetTerr, addStrength.get(targetTerr) + SUtils.strength(myRealMoveUnits, false, false, tFirst));
							moveRoutes.add(retreatRoute);
							moveUnits.add(myRealMoveUnits);
							s_logger.finer("PNC retreat to capital " + myRealMoveUnits + " route " + retreatRoute);
							alreadyMoved.addAll(myRealMoveUnits);
							movedInto.add(targetTerr);
						}
					}
				}
			}
			if (fortifiedTerr.contains(t)) // don't move...we've joined units
				continue;
			// float newInvasion = SUtils.getStrengthOfPotentialAttackers(t, data, player, tFirst, true, null);
			if (attackerStrength > (ourStrength * 1.85F + 6.0F) && !movedInto.contains(t)) // overwhelming attack...look to retreat
			{
				final List<Territory> myFriendTerr = SUtils.getNeighboringLandTerritories(data, player, t);
				int maxUnits = 0, thisUnits = 0;
				Territory mergeTerr = null;
				for (final Territory fTerr : myFriendTerr)
				{
					if (emptiedTerr.contains(fTerr))
						continue; // don't move somewhere we have already retreated from
					final List<Territory> badGuysTerr = SUtils.getNeighboringEnemyLandTerritories(data, player, fTerr);
					if (badGuysTerr.size() > 0) // give preference to the front
						thisUnits += 4;
					final float fTerrAttackers = SUtils.getStrengthOfPotentialAttackers(fTerr, data, player, tFirst, true, null);
					final StrengthEvaluator strEval = StrengthEvaluator.evalStrengthAt(data, player, fTerr, false, true, tFirst, true);
					if (fTerrAttackers > 8.0F && fTerrAttackers < (strEval.getAlliedStrengthInRange() + ourStrength) * 1.05F)
						thisUnits += 4;
					else if (fTerrAttackers > 0.0F && fTerrAttackers < (strEval.getAlliedStrengthInRange() + ourStrength) * 1.05F)
						thisUnits += 1;
					thisUnits += fTerr.getUnits().getMatches(alliedUnit).size();
					if (thisUnits > maxUnits)
					{
						maxUnits = thisUnits;
						mergeTerr = fTerr;
					}
				}
				if (mergeTerr != null)
				{
					final Route myRetreatRoute = data.getMap().getLandRoute(t, mergeTerr);
					final List<Unit> myRetreatUnits = t.getUnits().getMatches(landUnit);
					final int totUnits3 = myRetreatUnits.size();
					for (int i4 = totUnits3 - 1; i4 >= 0; i4--)
					{
						final Unit u4 = myRetreatUnits.get(i4);
						if (alreadyMoved.contains(u4))
							myRetreatUnits.remove(u4);
					}
					addStrength.put(mergeTerr, addStrength.get(mergeTerr) + SUtils.strength(myRetreatUnits, false, false, tFirst));
					moveUnits.add(myRetreatUnits);
					moveRoutes.add(myRetreatRoute);
					s_logger.finer("PNC consolidating units? " + myRetreatUnits + " route " + myRetreatRoute);
					alreadyMoved.addAll(myRetreatUnits);
					fortifiedTerr.add(mergeTerr);
					movedInto.add(mergeTerr);
					emptiedTerr.add(t);
				}
			}
			/*
			            //find the nearest enemy owned capital or factory
			            List<Territory> allBadTerr = SUtils.allEnemyTerritories(data, player);
			            List<Territory> enemyCapTerr = SUtils.getEnemyCapitals(data, player);
			            // Territory bestCapitol = null;
			            // Territory bestFactory = null;
			            for (Territory capitol : enemyCapTerr)
			            {
			                Route route = data.getMap().getRoute(t, capitol, moveThrough);
			                if(route != null)
			                {
			                    int distance = route.getLength();
			                    if(distance != 0 && distance < minDistance)
			                    {
			                        minDistance = distance;
			                        to = capitol;
			                    }
			                }
			            }
			            // bestCapitol = to;
			            for (Territory badFactTerr : allBadTerr)
			            {
							if (!Matches.territoryHasEnemyFactory(data, player).match(badFactTerr) || enemyCapTerr.contains(badFactTerr))
							    continue;
			           		if (Matches.TerritoryIsNeutral.match(badFactTerr))
			        		{
			                	float pValue = TerritoryAttachment.get(badFactTerr).getProduction();
			                	float enemyStrength = SUtils.strength(badFactTerr.getUnits().getUnits(), false, false, tFirst);
			                    if (enemyStrength > pValue*9) //why bother...focus on enemies
			                    	continue;
			        		}
							Route badRoute = data.getMap().getRoute(t, badFactTerr, moveThrough);
							if (badRoute != null)
							{
								int badDist = badRoute.getLength();
								if (badDist > 0 && badDist < minDistance)
								{
									minDistance = badDist;
									to = badFactTerr;
								}
							}
						}
			//            CompositeMatchAnd<Territory> routeCondition = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), Matches.TerritoryIsLand);
			            Route newRoute = SUtils.findNearest(t, Matches.territoryHasEnemyUnits(player, data ), moveThrough, data);
			            // move to any enemy territory
			            if(newRoute == null)
			            {
			              	newRoute = SUtils.findNearest(t, Matches.isTerritoryEnemy(player, data), moveThrough, data);
			            	if (newRoute != null)
			            	{
			            		Territory endTerr = newRoute.getEnd();
			            		if (Matches.TerritoryIsNeutral.match(endTerr))
			            		{
			                    	float pValue = TerritoryAttachment.get(endTerr).getProduction();
			                    	float enemyStrength = SUtils.strength(endTerr.getUnits().getUnits(), false, false, tFirst);
			                        if (enemyStrength > pValue*9) //why bother...focus on enemies
			                        	newRoute = SUtils.findNearest(t, endCondition2, routeCondition, data);
			            		}
			            	}
			            }
			*/
			if (goRoute == null) // just advance to nearest enemy owned factory
			{
				goRoute = SUtils.findNearest(t, Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits), routeCondition, data);
				if (goRoute != null)
				{
					final Territory endGoTerr = goRoute.getTerritories().get(goRoute.getLength() - 1);
					goRoute = data.getMap().getRoute(t, endGoTerr, routeCondition);
				}
			}
			final boolean isAmphib = isAmphibAttack(player, false);
			if (goRoute == null && isAmphib) // move toward the largest contingent of transports
			{
				if (Matches.territoryIsAlliedAndHasAlliedUnitMatching(data, player, Matches.UnitCanProduceUnits).match(t))
					continue;
				else
				{
					final List<Territory> transportTerrs = SUtils.findOnlyMyShips(t, data, player, Matches.UnitIsTransport);
					if (transportTerrs.size() > 0)
					{
						final IntegerMap<Territory> transMap = new IntegerMap<Territory>();
						for (final Territory xTransTerr : transportTerrs)
							transMap.put(xTransTerr, xTransTerr.getUnits().countMatches(myTransportUnit));
						SUtils.reorder(transportTerrs, transMap, true);
					}
					for (final Territory tTerr : transportTerrs)
					{
						final List<Territory> myLandTerrs = SUtils.getNeighboringLandTerritories(data, player, tTerr);
						boolean goodRoute = false;
						final Iterator<Territory> mLIter = myLandTerrs.iterator();
						while (mLIter.hasNext() && !goodRoute)
						{
							final Territory mLT = mLIter.next();
							goRoute = data.getMap().getRoute(t, mLT, Matches.TerritoryIsNotImpassableToLandUnits(player, data));
							if (goRoute != null)
							{
								goodRoute = true;
								to = mLT;
							}
						}
					}
					if (goRoute == null)
						continue;
				}
			}
			if (goRoute == null)
				continue;
			final int newDistance = goRoute.getLength();
			final List<Collection<Unit>> unitsBySpeed = new ArrayList<Collection<Unit>>();
			final List<Unit> moveableUnits = new ArrayList<Unit>(units);
			moveableUnits.removeAll(alreadyMoved);
			SUtils.breakUnitsBySpeed(unitsBySpeed, data, player, moveableUnits);
			if (to != null && minDistance <= (newDistance + 1))
			{
				if (units.size() > 0)
				{
					final Route rC = data.getMap().getRoute(t, to, moveThrough);
					if (rC != null)
						goRoute = rC;
				}
			}
			for (final Collection<Unit> goUnits : unitsBySpeed)
			{
				final int maxDist = MoveValidator.getMaxMovement(goUnits);
				if (maxDist == 0)
				{
					alreadyMoved.addAll(goUnits);
					continue;
				}
				Route newRoute2 = new Route(); // don't modify newRoute
				if (goRoute.getLength() < maxDist)
					newRoute2 = goRoute;
				else
				{
					final Iterator<Territory> newIter = goRoute.iterator();
					newRoute2.setStart(t);
					newIter.next();
					while (newIter.hasNext() && newRoute2.getLength() < maxDist)
					{
						final Territory oneTerr = newIter.next();
						if (Matches.isTerritoryAllied(player, data).match(oneTerr))
							newRoute2.add(oneTerr);
					}
				}
				// checking 0 length routes and routes through enemies
				if (newRoute2.getLength() < 1)
					continue;
				moveUnits.add(goUnits);
				moveRoutes.add(newRoute2);
				s_logger.finer("PNC move towards:default " + goUnits + " route " + newRoute2);
				alreadyMoved.addAll(goUnits);
				final Territory endPoint = newRoute2.getEnd();
				if (!movedInto.contains(endPoint))
					movedInto.add(endPoint);
			}
			/*            if (firstStep != null && Matches.isTerritoryAllied(player, data).match(firstStep) && route != null)
			            {
			                moveUnits.add(units);
			            	moveRoutes.add(route);
			            	movedInto.add(firstStep);
			            	addStrength.put(firstStep, addStrength.get(firstStep) + SUtils.strength(units, false, false, tFirst));
			            }
			*/
		}
	}
	
	/*
	 * A second pass at the set of nonCombat Land Units
	 * Ignores all neutrals (for now)
	 * 
	 */
	private void secondNonCombat(final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player, final GameData data)
	{
		final List<Unit> alreadyMoved = new ArrayList<Unit>();
		final boolean tFirst = transportsMayDieFirst();
		final CompositeMatch<Unit> unMovedLand = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.UnitIsNotAA, Matches.UnitCanNotProduceUnits, Matches.UnitIsNotInfrastructure);
		final CompositeMatch<Unit> ourUnMovedLand = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), unMovedLand, Matches.unitHasNotMoved);
		final List<Territory> unMovedLandTerr = SUtils.findTersWithUnitsMatching(data, player, ourUnMovedLand);
		final HashMap<Territory, Float> landMap = new HashMap<Territory, Float>();
		final List<Territory> ourOwnedTerr = SUtils.allAlliedTerritories(data, player);
		for (final Territory ourTerr : ourOwnedTerr)
		{
			final float eStrength = SUtils.getStrengthOfPotentialAttackers(ourTerr, data, player, tFirst, true, null);
			final float myStrength = SUtils.strength(ourTerr.getUnits().getUnits(), false, false, tFirst);
			final float diffStrength = eStrength - myStrength;
			landMap.put(ourTerr, diffStrength);
		}
		final List<Territory> myFactories = Match.getMatches(SUtils.findTersWithUnitsMatching(data, player, Matches.UnitCanProduceUnits), Matches.isTerritoryAllied(player, data));
		for (final Territory factTerr : myFactories)
		{
			float diffStrength = landMap.get(factTerr).floatValue();
			if (diffStrength > 0.0F) // we need other units
			{
				final List<Territory> landNeighbors = SUtils.getNeighboringLandTerritories(data, player, factTerr);
				final List<Territory> grabFromTerr = new ArrayList<Territory>();
				for (final Territory lN : landNeighbors)
				{
					if (unMovedLandTerr.contains(lN))
						grabFromTerr.add(lN);
				}
				SUtils.reorder(grabFromTerr, landMap, false);
				final Iterator<Territory> grabIter = grabFromTerr.iterator();
				while (grabIter.hasNext() && diffStrength > 0.0F)
				{
					final Territory availTerr = grabIter.next();
					float availStrength = -landMap.get(availTerr).floatValue();
					final List<Unit> availUnits = SUtils.sortTransportUnits(availTerr.getUnits().getMatches(ourUnMovedLand));
					availUnits.removeAll(alreadyMoved);
					final Iterator<Unit> availIter = availUnits.iterator();
					final List<Unit> moveThese = new ArrayList<Unit>();
					while (availIter.hasNext() && diffStrength > 0.0F && availStrength > 0.0F)
					{
						final Unit moveOne = availIter.next();
						final float thisUnitStrength = SUtils.uStrength(moveOne, false, false, tFirst);
						diffStrength -= thisUnitStrength;
						availStrength -= thisUnitStrength;
						moveThese.add(moveOne);
					}
					landMap.put(availTerr, -availStrength);
					final Route aRoute = data.getMap().getLandRoute(availTerr, factTerr);
					moveUnits.add(moveThese);
					moveRoutes.add(aRoute);
					alreadyMoved.addAll(moveThese);
				}
				landMap.put(factTerr, diffStrength);
			}
		}
		final CompositeMatch<Territory> endCondition = new CompositeMatchAnd<Territory>(Matches.territoryHasEnemyUnits(player, data), Matches.TerritoryIsNotNeutralButCouldBeWater,
					Matches.TerritoryIsLand);
		for (final Territory ownedTerr : unMovedLandTerr)
		{// TODO: find another territory to join if possible
			// TODO: for some reason, unMovedLandTerr is containing conflicted territories where combat didn't
			// complete- causing the need for the ownedTerr check below
			if (ownedTerr.isWater() || !ownedTerr.getOwner().equals(player))
				continue;
			float diffStrength = -landMap.get(ownedTerr).floatValue();
			if (diffStrength > 0.0F && ownedTerr.getUnits().getMatches(unMovedLand).size() > 1 && data.getMap().getNeighbors(ownedTerr, endCondition).isEmpty())
			{
				final Route closestERoute = SUtils.findNearest(ownedTerr, endCondition, Matches.TerritoryIsNotImpassableToLandUnits(player, data), data);
				if (closestERoute == null || closestERoute.getEnd() == null)
					continue;
				final List<Unit> ourOwnedUnits = ownedTerr.getUnits().getMatches(ourUnMovedLand);
				ourOwnedUnits.removeAll(alreadyMoved);
				if (ourOwnedUnits.isEmpty())
					continue;
				final List<Collection<Unit>> ourOwnedUnits2 = new ArrayList<Collection<Unit>>();
				SUtils.breakUnitsBySpeed(ourOwnedUnits2, data, player, ourOwnedUnits);
				final Territory targetTerr = closestERoute.getEnd();
				// List<Unit> moveTheseUnits = SUtils.sortTransportUnits(ourOwnedUnits);
				// Territory goTerr = closestERoute.getTerritories().get(1);
				final float goTerrStrength = SUtils.strength(targetTerr.getUnits().getMatches(Matches.alliedUnit(player, data)), false, false, tFirst);
				final float goTerrEStrength = SUtils.getStrengthOfPotentialAttackers(targetTerr, data, player, tFirst, true, null);
				final float goDiffStrength = goTerrEStrength - goTerrStrength;
				if (goDiffStrength - diffStrength > 8.0F)
					continue;
				for (final Collection<Unit> goUnits : ourOwnedUnits2)
				{
					final Iterator<Unit> unitIter = goUnits.iterator();
					final int moveDist = MoveValidator.getLeastMovement(goUnits);
					final List<Unit> moveThese = new ArrayList<Unit>();
					Territory realTargetTerr = closestERoute.getTerritories().get(closestERoute.getLength() - 1);
					Route targetRoute = new Route();
					if (moveDist < closestERoute.getLength())
						realTargetTerr = closestERoute.getTerritories().get(moveDist);
					targetRoute = data.getMap().getRoute(ownedTerr, realTargetTerr, Matches.isTerritoryAllied(player, data));
					while (unitIter.hasNext() && diffStrength > 0.0F)
					{
						final Unit oneUnit = unitIter.next();
						diffStrength -= SUtils.uStrength(oneUnit, false, false, tFirst);
						moveThese.add(oneUnit);
					}
					moveUnits.add(moveThese);
					moveRoutes.add(targetRoute);
					alreadyMoved.addAll(moveThese);
					landMap.put(ownedTerr, -diffStrength);
				}
			}
		}
		
		// find factories with movement, and move them to places we own if they are sitting on top of each other
		final CompositeMatch<Unit> ownedUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
		final CompositeMatch<Unit> ourFactory = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitCanProduceUnits);
		final CompositeMatch<Unit> moveableFactory = new CompositeMatchAnd<Unit>(ourFactory, Matches.UnitCanMove, Matches.unitHasMovementLeft);
		final List<Territory> moveableFactoryTerritories = SUtils.findUnitTerr(data, player, moveableFactory);
		if (!moveableFactoryTerritories.isEmpty())
		{
			final CompositeMatch<Territory> endConditionEnemyLand = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemy(player, data), Matches.TerritoryIsNotImpassable, Matches.TerritoryIsLand);
			final CompositeMatch<Territory> routeConditionLand = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), Matches.TerritoryIsNotImpassable, Matches.TerritoryIsLand);
			final List<Territory> owned = SUtils.allOurTerritories(data, player);
			final List<Territory> existingFactories = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitCanProduceUnits);
			owned.removeAll(existingFactories);
			final List<Territory> isWaterConvoy = SUtils.onlyWaterTerr(data, owned);
			owned.removeAll(isWaterConvoy);
			final CompositeMatch<Territory> goodFactTerr = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player, data), Matches.isTerritoryOwnedBy(player));
			for (final Territory moveableFactoryTerr : moveableFactoryTerritories)
			{
				final List<Unit> moveableFactories = moveableFactoryTerr.getUnits().getMatches(moveableFactory);
				if (moveableFactories.size() > 0 && moveableFactoryTerr.getUnits().getMatches(ourFactory).size() > 1)
				{
					final List<Territory> goodNeighbors = new ArrayList<Territory>(data.getMap().getNeighbors(moveableFactoryTerr, goodFactTerr));
					goodNeighbors.retainAll(owned);
					Collections.shuffle(goodNeighbors);
					final IntegerMap<Territory> terrValue = new IntegerMap<Territory>();
					for (final Territory moveFactToTerr : goodNeighbors)
					{
						// sorting territories to have ones with greatest production and closeness to enemy first (by land, then by sea) (veqryn)
						int territoryValue = 0;
						territoryValue += Math.random() < .4 ? 1 : 0;
						territoryValue += Math.random() < .4 ? 1 : 0;
						if (SUtils.hasLandRouteToEnemyOwnedCapitol(moveFactToTerr, player, data))
							territoryValue += 3;
						if (SUtils.findNearest(moveFactToTerr, Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits),
									Matches.TerritoryIsNotImpassableToLandUnits(player, data), data) != null)
							territoryValue += 1;
						if (Matches.territoryHasWaterNeighbor(data).match(moveFactToTerr))
							territoryValue += 3;
						Route r = SUtils.findNearest(moveFactToTerr, endConditionEnemyLand, routeConditionLand, data);
						if (r != null)
						{
							territoryValue += 10 - r.getLength();
						}
						else
						{
							r = SUtils.findNearest(moveFactToTerr, endConditionEnemyLand, Matches.TerritoryIsWater, data);
							if (r != null)
								territoryValue += 8 - r.getLength();
							else
								territoryValue -= 115;
						}
						territoryValue += 4 * TerritoryAttachment.get(moveFactToTerr).getProduction();
						final List<Territory> weOwnAll = SUtils.getNeighboringEnemyLandTerritories(data, player, moveFactToTerr);
						final List<Territory> isWater = SUtils.onlyWaterTerr(data, weOwnAll);
						weOwnAll.removeAll(isWater);
						final Iterator<Territory> weOwnAllIter = weOwnAll.iterator();
						while (weOwnAllIter.hasNext())
						{
							final Territory tempFact = weOwnAllIter.next();
							if (Matches.TerritoryIsNeutralButNotWater.match(tempFact) || Matches.TerritoryIsImpassable.match(tempFact))
								weOwnAllIter.remove();
						}
						territoryValue -= 15 * weOwnAll.size();
						if (TerritoryAttachment.get(moveFactToTerr).getProduction() < 2)
							territoryValue -= 100;
						if (TerritoryAttachment.get(moveFactToTerr).getProduction() < 1)
							territoryValue -= 100;
						terrValue.put(moveFactToTerr, territoryValue);
					}
					SUtils.reorder(goodNeighbors, terrValue, true);
					if (goodNeighbors.size() == 0)// goodNeighbors == null ||
						continue;
					int i = 0;
					int j = 0;
					final int diff = moveableFactoryTerr.getUnits().getMatches(ourFactory).size() - moveableFactories.size();
					for (final Unit factoryUnit : moveableFactories)
					{
						if (diff < 1 && j >= moveableFactories.size() - 1)
							continue;
						if (i >= goodNeighbors.size())
							i = 0;
						moveRoutes.add(data.getMap().getRoute(moveableFactoryTerr, goodNeighbors.get(i)));
						moveUnits.add(Collections.singleton(factoryUnit));
						i++;
						j++;
					}
				}
			}
		}
	}
	
	/**
	 * Routine for moving planes that are in locations in which they cannot land
	 * Sends bombers to capitals...fighters to remote locations
	 * 2/1/10 Correctly handles carrier size and fighter cost
	 * 
	 * @param moveUnits
	 * @param moveRoutes
	 * @param player
	 * @param data
	 */
	private void movePlanesHomeNonCom(final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player, final GameData data)
	{
		// planes are doing silly things like landing in territories that can be invaded with cheap units
		// we want planes to find an aircraft carrier
		final IMoveDelegate delegateRemote = (IMoveDelegate) getPlayerBridge().getRemoteDelegate();
		final CompositeMatch<Unit> alliedFactory = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitCanProduceUnits);
		final CompositeMatch<Unit> fighterUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanLandOnCarrier);
		final CompositeMatch<Unit> bomberUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanLandOnCarrier.invert(), Matches.UnitIsAir);
		final CompositeMatch<Unit> alliedFighterUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitCanLandOnCarrier);
		final CompositeMatch<Unit> carrierUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsCarrier);
		final CompositeMatch<Unit> alliedACUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsCarrier);
		// CompositeMatch<Unit> alliedBomberUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitCanLandOnCarrier.invert(), Matches.UnitIsAir);
		final CompositeMatch<Territory> noEnemyNeighbor = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), Matches.territoryHasEnemyLandNeighbor(data, player).invert());
		final CompositeMatch<Territory> noNeutralOrAA = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotNeutralButCouldBeWater, Matches.territoryHasEnemyAAforCombatOnly(player, data).invert(),
					Matches.TerritoryIsNotImpassable);
		final List<Unit> alreadyMoved = new ArrayList<Unit>();
		final List<Territory> alreadyCheck = new ArrayList<Territory>();
		final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
		final Match<Territory> canLand = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), new Match<Territory>()
		{
			@Override
			public boolean match(final Territory o)
			{
				return !delegate.getBattleTracker().wasConquered(o);
			}
		});
		final Match<Territory> routeCondition = new CompositeMatchAnd<Territory>(Matches.territoryHasEnemyAAforCombatOnly(player, data).invert(), Matches.TerritoryIsPassableAndNotRestricted(player,
					data));
		final Territory myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		final List<Territory> alliedFactories = SUtils.findUnitTerr(data, player, alliedFactory);
		for (final Territory tBomb : delegateRemote.getTerritoriesWhereAirCantLand()) // move bombers to capital first
		{
			final List<Unit> bomberUnits = tBomb.getUnits().getMatches(bomberUnit);
			bomberUnits.removeAll(alreadyMoved);
			final List<Unit> sendBombers = new ArrayList<Unit>();
			alreadyCheck.add(tBomb);
			for (final Unit bU : bomberUnits)
			{
				if (bU == null)
					continue;
				final boolean landable = SUtils.airUnitIsLandable(bU, tBomb, myCapital, player, data);
				if (landable)
					sendBombers.add(bU);
			}
			if (sendBombers.size() > 0 && tBomb != myCapital)
			{
				final Route bomberRoute = data.getMap().getRoute(tBomb, myCapital, noNeutralOrAA);
				if (bomberRoute != null && bomberRoute.getEnd() != null && AirMovementValidator.canLand(sendBombers, bomberRoute.getEnd(), player, data))
				{
					moveRoutes.add(bomberRoute);
					moveUnits.add(sendBombers);
					alreadyMoved.addAll(sendBombers);
				}
			}
			bomberUnits.removeAll(sendBombers); // see if there are any left
			final Iterator<Unit> bUIter = bomberUnits.iterator();
			while (bUIter.hasNext())
			{
				boolean landedOne = false;
				final Unit bU = bUIter.next();
				if (bU == null)
					continue;
				for (final Territory aFactory : alliedFactories)
				{
					final Route bomberFactoryRoute = data.getMap().getRoute(tBomb, aFactory, noNeutralOrAA);
					if (bomberFactoryRoute != null && MoveValidator.hasEnoughMovement(bU, bomberFactoryRoute) && !landedOne)
					{
						moveUnits.add(Collections.singleton(bU));
						moveRoutes.add(bomberFactoryRoute);
						alreadyMoved.add(bU);
						bUIter.remove();
						landedOne = true;
					}
				}
				if (landedOne)
					continue;
				final Route goodBomberRoute = SUtils.findNearest(tBomb, noEnemyNeighbor, noNeutralOrAA, data);
				if (goodBomberRoute != null && MoveValidator.hasEnoughMovement(bU, goodBomberRoute))
				{
					moveUnits.add(Collections.singleton(bU));
					moveRoutes.add(goodBomberRoute);
					alreadyMoved.add(bU);
					bUIter.remove();
					landedOne = true;
				}
				if (landedOne)
					continue;
				final Route bomberRoute2 = SUtils.findNearestNotEmpty(tBomb, canLand, routeCondition, data);
				if (bomberRoute2 != null && MoveValidator.hasEnoughMovement(bU, bomberRoute2))
				{
					moveRoutes.add(bomberRoute2);
					moveUnits.add(Collections.singleton(bU));
					alreadyMoved.add(bU);
					bUIter.remove();
					landedOne = true;
				}
				if (landedOne)
					continue;
				final Route bomberRoute3 = SUtils.findNearest(tBomb, canLand, noNeutralOrAA, data);
				if (bomberRoute3 != null && MoveValidator.hasEnoughMovement(bU, bomberRoute3))
				{
					final List<Unit> qAdd = new ArrayList<Unit>();
					qAdd.add(bU);
					moveRoutes.add(bomberRoute3);
					moveUnits.add(qAdd);
					alreadyMoved.add(bU);
					bUIter.remove();
				}
			}
		}
		// CompositeMatch<Territory> avoidTerr = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotNeutral, Matches.TerritoryIsNotImpassable, Matches.territoryHasEnemyAA(player, data).invert());
		final List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
		final List<Territory> ourEnemyTerr = new ArrayList<Territory>();
		HashMap<Territory, Float> rankMap = new HashMap<Territory, Float>();
		rankMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, false, false, true);
		final Collection<Territory> badPlaneTerrs = delegateRemote.getTerritoriesWhereAirCantLand();
		// s_logger.fine("Player: "+player.getName()+"Planes Need to Move From: "+badPlaneTerrs);
		for (final Territory tFight : badPlaneTerrs)
		{
			final List<Unit> fighterUnits = new ArrayList<Unit>(tFight.getUnits().getMatches(fighterUnit));
			fighterUnits.removeAll(alreadyMoved);
			// s_logger.fine("Territory: "+tFight+"; Planes: "+fighterUnits);
			if (fighterUnits.isEmpty())
				continue;
			final List<Unit> ACUnits = new ArrayList<Unit>(tFight.getUnits().getMatches(carrierUnit));
			int carrierSpace = 0;
			for (final Unit carrier1 : ACUnits)
				carrierSpace += UnitAttachment.get(carrier1.getType()).getCarrierCapacity();
			final List<Unit> alliedACUnits = new ArrayList<Unit>(tFight.getUnits().getMatches(alliedACUnit));
			int alliedCarrierSpace = 0;
			for (final Unit carrier1 : alliedACUnits)
				alliedCarrierSpace += UnitAttachment.get(carrier1.getType()).getCarrierCapacity();
			final List<Unit> alliedFighters = new ArrayList<Unit>(tFight.getUnits().getMatches(alliedFighterUnit));
			alliedFighters.removeAll(fighterUnits);
			// int maxUnits = carrierSpace;
			// int alliedACMax = alliedCarrierSpace;
			int totFighters = fighterUnits.size();
			// int alliedTotFighters = alliedFighters.size();
			// int fighterSpace = 0;
			int alliedFighterSpace = 0;
			for (final Unit fighter1 : fighterUnits)
				totFighters += UnitAttachment.get(fighter1.getType()).getCarrierCost();
			for (final Unit fighter1 : alliedFighters)
				alliedFighterSpace += UnitAttachment.get(fighter1.getType()).getCarrierCost();
			final List<Collection<Unit>> fighterList = new ArrayList<Collection<Unit>>();
			SUtils.breakUnitsBySpeed(fighterList, data, player, fighterUnits);
			int needToLand = fighterUnits.size();
			if (carrierSpace > 0)
			{
				final Iterator<Collection<Unit>> fIter = fighterList.iterator();
				while (fIter.hasNext() && carrierSpace > 0)
				{
					final Collection<Unit> newFighters = fIter.next();
					final Iterator<Unit> nFIter = newFighters.iterator();
					while (nFIter.hasNext() && carrierSpace > 0)
					{
						final Unit markFighter = nFIter.next();
						carrierSpace -= UnitAttachment.get(markFighter.getType()).getCarrierCost();
						nFIter.remove(); // plane is marked to stay on the carrier
						needToLand--;
					}
				}
			}
			final Iterator<Collection<Unit>> fIter = fighterList.iterator();
			while (needToLand > 0 && fIter.hasNext())
			{// each group will have an identical movement
				final Collection<Unit> fighterGroup = fIter.next();
				if (fighterGroup.isEmpty())
					continue;
				final int flightDistance = MoveValidator.getMaxMovement(fighterGroup);
				final Set<Territory> allTerr = data.getMap().getNeighbors(tFight, flightDistance);
				final List<Territory> landingZones = Match.getMatches(allTerr, canLand);
				SUtils.reorder(landingZones, rankMap, false);
				final Iterator<Territory> lzIter = landingZones.iterator();
				for (final Territory t : landingZones)
				{
					s_logger.fine("possible" + t + " " + rankMap.get(t));
				}
				while (needToLand > 0 && lzIter.hasNext() && !fighterGroup.isEmpty())
				{
					final Territory landingZone = lzIter.next();
					s_logger.fine("trying" + landingZone + " " + rankMap.get(landingZone));
					if (Matches.TerritoryIsPassableAndNotRestricted(player, data).match(landingZone) && AirMovementValidator.canLand(fighterGroup, landingZone, player, data))
					{
						final Route landingRoute = data.getMap().getRoute(tFight, landingZone, noNeutralOrAA);
						if (landingRoute != null && MoveValidator.hasEnoughMovement(fighterGroup, landingRoute))
						{
							final Iterator<Unit> fIter2 = fighterGroup.iterator();
							final List<Unit> landThese = new ArrayList<Unit>();
							boolean landSome = false;
							while (needToLand > 0 && fIter2.hasNext())
							{
								final Unit fighter = fIter2.next();
								landThese.add(fighter);
								fIter2.remove();
								needToLand--;
								landSome = true;
								s_logger.fine("Added: " + fighter + "; Left To Land: " + needToLand);
							}
							if (landSome)
							{
								moveUnits.add(landThese);
								moveRoutes.add(landingRoute);
								alreadyMoved.addAll(landThese);
							}
						}
					}
				}
			}
		}
	}
	
	private void bomberNonComMove(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
		final CompositeMatch<Unit> myBomberUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsStrategicBomber);
		final CompositeMatch<Unit> alliedFactory = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitCanProduceUnits);
		// CompositeMatch<Unit> enemyFactory = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsFactory);
		final CompositeMatch<Territory> waterOrLand = new CompositeMatchOr<Territory>(Matches.TerritoryIsWater, Matches.TerritoryIsLand);
		final List<Territory> alliedFactories = SUtils.findUnitTerr(data, player, alliedFactory);
		// List<Territory> enemyFactories = SUtils.findUnitTerr(data, player, enemyFactory);
		final List<Territory> bomberTerrs = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitIsStrategicBomber);
		if (bomberTerrs.isEmpty())
			return;
		final Iterator<Territory> bTerrIter = bomberTerrs.iterator();
		while (bTerrIter.hasNext())
		{
			final Territory bTerr = bTerrIter.next();
			final Route bRoute = SUtils.findNearest(bTerr, Matches.territoryHasEnemyUnits(player, data), waterOrLand, data);
			if (bRoute == null || bRoute.getLength() < 4 && Matches.TerritoryIsLand.match(bTerr) && !delegate.getBattleTracker().wasBattleFought(bTerr))
			{
				bTerrIter.remove();
			}
		}
		if (bomberTerrs.isEmpty())
			return;
		final List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
		final IntegerMap<Territory> shipMap = new IntegerMap<Territory>();
		final HashMap<Territory, Float> strengthMap = new HashMap<Territory, Float>();
		for (final Territory aF : alliedFactories)
		{
			if (delegate.getBattleTracker().wasConquered(aF))
				continue; // don't allow planes to move toward a just conquered factory
			final Route distToEnemy = SUtils.findNearest(aF, Matches.territoryHasEnemyUnits(player, data), waterOrLand, data);
			if (distToEnemy == null || distToEnemy.getEnd() == null)
				continue;
			final int eDist = distToEnemy.getLength();
			// int shipThreat = SUtils.shipThreatToTerr(aF, data, player, true);
			shipMap.put(aF, eDist);
			final float eStrength = SUtils.getStrengthOfPotentialAttackers(aF, data, player, true, false, null);
			strengthMap.put(aF, eStrength);
		}
		final List<Territory> checkTerrs = new ArrayList<Territory>(shipMap.keySet());
		if (!checkTerrs.isEmpty())
		{
			SUtils.reorder(checkTerrs, shipMap, false);
			for (final Territory checkTerr : checkTerrs)
			{
				for (final Territory bTerr : bomberTerrs)
				{
					final Route bRoute = data.getMap().getRoute(bTerr, checkTerr, waterOrLand);
					if (bRoute == null || bRoute.getEnd() == null)
						continue;
					final List<Unit> bUnits = bTerr.getUnits().getMatches(myBomberUnit);
					bUnits.removeAll(unitsAlreadyMoved);
					if (bUnits.isEmpty())
						continue;
					if (MoveValidator.hasEnoughMovement(bUnits, bRoute))
					{
						moveUnits.add(bUnits);
						moveRoutes.add(bRoute);
						unitsAlreadyMoved.addAll(bUnits);
					}
					else
					{
						final Iterator<Unit> bIter = bUnits.iterator();
						while (bIter.hasNext())
						{
							final Unit bomber = bIter.next();
							if (MoveValidator.hasEnoughMovement(bomber, bRoute))
							{
								moveUnits.add(Collections.singleton(bomber));
								moveRoutes.add(bRoute);
								unitsAlreadyMoved.add(bomber);
							}
						}
					}
				}
			}
			checkTerrs.clear();
			checkTerrs.addAll(strengthMap.keySet());
			SUtils.reorder(checkTerrs, strengthMap, true);
			for (final Territory checkTerr : checkTerrs)
			{
				for (final Territory bTerr : bomberTerrs)
				{
					final Route bRoute = data.getMap().getRoute(bTerr, checkTerr, SUtils.TerritoryIsNotImpassableToAirUnits(data));
					if (bRoute == null || bRoute.getEnd() == null)
						continue;
					final List<Unit> bUnits = bTerr.getUnits().getMatches(myBomberUnit);
					bUnits.removeAll(unitsAlreadyMoved);
					if (bUnits.isEmpty())
						continue;
					if (MoveValidator.hasEnoughMovement(bUnits, bRoute))
					{
						moveUnits.add(bUnits);
						moveRoutes.add(bRoute);
						unitsAlreadyMoved.addAll(bUnits);
					}
					else
					{
						final Iterator<Unit> bIter = bUnits.iterator();
						while (bIter.hasNext())
						{
							final Unit bomber = bIter.next();
							if (MoveValidator.hasEnoughMovement(bomber, bRoute))
							{
								moveUnits.add(Collections.singleton(bomber));
								moveRoutes.add(bRoute);
								unitsAlreadyMoved.add(bomber);
							}
						}
					}
				}
			}
		}
	}
	
	// TODO: Rework combat move into 3 separate phases. This will give us a better look at the real potential attackers after a set of moves.
	private void populateCombatMove(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		/* Priorities:
		** 1) We lost our capital
		** 2) Units placed next to our capital
		** 3) Enemy players capital
		** 4) Enemy players factories
		*/
		setImpassableTerrs(player);
		final Collection<Territory> impassableTerrs = getImpassableTerrs();
		final HashMap<PlayerID, IntegerMap<UnitType>> costMap = SUtils.getPlayerCostMap(data);
		final boolean aggressive = SUtils.determineAggressiveAttack(data, player, 1.4F);
		float maxAttackFactor = 2.00F;
		float attackFactor = 1.73F;
		final float attackFactor2 = 1.11F; // emergency attack...weaken enemy
		final Collection<Unit> unitsAlreadyMoved = new HashSet<Unit>();
		final List<Territory> enemyOwned = SUtils.getNeighboringEnemyLandTerritories(data, player, true);
		enemyOwned.removeAll(impassableTerrs);
		// Include neutral territories that are worth attacking.
		// enemyOwned.addAll(SUtils.getNeighboringNeutralLandTerritories(data, player, true));
		final boolean tFirst = transportsMayDieFirst();
		final List<Territory> alreadyAttacked = getLandTerrAttacked();
		final Territory myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		final float eCapStrength = SUtils.getStrengthOfPotentialAttackers(myCapital, data, player, tFirst, false, null);
		float ourStrength = SUtils.strength(myCapital.getUnits().getUnits(), false, false, tFirst);
		boolean capDanger = eCapStrength > ourStrength;
		// List<Territory> capitalNeighbors = SUtils.getNeighboringLandTerritories(data, player, myCapital);
		final boolean ownMyCapital = myCapital.getOwner() == player;
		final List<Territory> emptyBadTerr = new ArrayList<Territory>();
		float remainingStrengthNeeded = 0.0F;
		final List<Territory> liveEnemyCaps = SUtils.getLiveEnemyCapitals(data, player);
		final CompositeMatch<Unit> attackable = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand, Matches.UnitIsNotAA, Matches.UnitCanMove,
					Matches.UnitCanNotProduceUnits, Matches.UnitIsNotInfrastructure, Matches.UnitCanNotMoveDuringCombatMove.invert(), new Match<Unit>()
					{
						@Override
						public boolean match(final Unit o)
						{
							return !unitsAlreadyMoved.contains(o);
						}
					});
		// CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir);
		final CompositeMatch<Unit> alliedNotOwned = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), new InverseMatch<Unit>(Matches.unitIsOwnedBy(player)));
		final CompositeMatch<Unit> alliedAirUnit = new CompositeMatchAnd<Unit>(alliedNotOwned, Matches.UnitIsAir);
		final CompositeMatch<Unit> alliedLandUnit = new CompositeMatchAnd<Unit>(alliedNotOwned, Matches.UnitIsLand, Matches.UnitIsNotAA, Matches.UnitCanNotProduceUnits,
					Matches.UnitIsNotInfrastructure);
		final CompositeMatch<Unit> alliedAirLandUnitNotOwned = new CompositeMatchOr<Unit>(alliedAirUnit, alliedLandUnit);
		final CompositeMatch<Unit> blitzUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBlitz);
		final CompositeMatch<Unit> enemyLandAirUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsNotSea, Matches.UnitIsNotAA, Matches.UnitCanNotProduceUnits, Matches.UnitIsNotInfrastructure);
		final CompositeMatch<Territory> enemyTerr = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data),
					Matches.TerritoryIsNotImpassableToLandUnits(player, data));
		if (!ownMyCapital) // We lost our capital
		{
			attackFactor = 0.78F; // attack like a maniac, maybe we will win
			enemyOwned.remove(myCapital); // handle the capital directly
		}
		final List<Territory> bigProblem2 = SUtils.getNeighboringEnemyLandTerritories(data, player, myCapital); // attack these guys first
		// HashMap<Territory, Float> sortTerritories = new HashMap<Territory, Float>();
		final HashMap<Territory, Float> sortProblems = new HashMap<Territory, Float>();
		// int numTerr = 0;
		int numTerrProblem = 0, realProblems = 0;
		float xStrength = 0.0F;
		Territory xTerr = null;
		final HashMap<Territory, Float> enemyMap = new HashMap<Territory, Float>();
		final Territory maxAttackTerr = SUtils.landAttackMap(data, player, enemyMap);
		if (maxAttackTerr == null)
		{
			return;
		}
		SUtils.reorder(enemyOwned, enemyMap, true);
		// numTerr = enemyMap.size();
		float aggregateStrength = 0.0F;
		final Iterator<Territory> bPIter = bigProblem2.iterator();
		while (bPIter.hasNext())
		{
			final Territory bPTerr = bPIter.next();
			if (Matches.TerritoryIsNeutralButNotWater.match(bPTerr)) // don't worry about neutrals in the Big Problems
				bPIter.remove();
		}
		for (final Territory tProb : bigProblem2) // rank the problems
		{
			if (!tProb.getUnits().someMatch(Matches.enemyUnit(player, data)))
				xStrength = 0.0F;
			else
				xStrength = SUtils.strength(tProb.getUnits().getUnits(), false, false, tFirst);
			aggregateStrength += xStrength;
			if (xStrength > 10.0F)
				realProblems++;
			sortProblems.put(tProb, xStrength);
			numTerrProblem++;
		}
		SUtils.reorder(bigProblem2, sortProblems, true);
		final List<Territory> seaTerrAttacked = getSeaTerrAttacked();
		final List<Collection<Unit>> xMoveUnits = new ArrayList<Collection<Unit>>();
		final List<Route> xMoveRoutes = new ArrayList<Route>();
		final List<Unit> xAlreadyMoved = new ArrayList<Unit>();
		if (!ownMyCapital) // attack the capital with everything we have
		{
			final Collection<Territory> attackFrom = SUtils.getNeighboringLandTerritories(data, player, myCapital);
			final float badCapStrength = SUtils.strength(myCapital.getUnits().getUnits(), false, false, tFirst);
			float capStrength = 0.0F;
			for (final Territory checkCap : attackFrom)
			{
				final List<Unit> units = checkCap.getUnits().getMatches(attackable);
				capStrength += SUtils.strength(units, true, false, tFirst);
			}
			float xRS = 1000.0F;
			xRS -= SUtils.inviteBlitzAttack(false, myCapital, xRS, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, true, true);
			boolean groundUnits = ((1000.0F - xRS) > 1.0F);
			xRS -= SUtils.invitePlaneAttack(false, false, myCapital, xRS, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player);
			xRS -= SUtils.inviteTransports(false, myCapital, xRS, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, tFirst, false, seaTerrAttacked);
			capStrength += 1000.0F - xRS;
			if (capStrength > badCapStrength * 0.78F) // give us a chance...
			{
				for (final Territory owned : attackFrom)
				{
					final List<Unit> units = owned.getUnits().getMatches(attackable);
					if (units.size() > 0)
					{
						unitsAlreadyMoved.addAll(units);
						moveUnits.add(units);
						moveRoutes.add(data.getMap().getRoute(owned, myCapital));
						alreadyAttacked.add(myCapital);
						groundUnits = true;
					}
				}
				if (groundUnits)
				{
					moveUnits.addAll(xMoveUnits);
					moveRoutes.addAll(xMoveRoutes);
					unitsAlreadyMoved.addAll(xAlreadyMoved);
				}
			}
		}
		xMoveUnits.clear();
		xMoveRoutes.clear();
		xAlreadyMoved.clear();
		if (capDanger)
		{
			// List<Unit> myCapUnits = myCapital.getUnits().getMatches(Matches.unitIsOwnedBy(player));
			final List<Territory> eCapTerrs = SUtils.getNeighboringEnemyLandTerritories(data, player, myCapital);
			final Iterator<Territory> eCIter = eCapTerrs.iterator();
			while (eCIter.hasNext())
			{
				final Territory noNeutralTerr = eCIter.next();
				if (Matches.TerritoryIsNeutralButNotWater.match(noNeutralTerr))
					eCIter.remove();
			}
			// float totECapStrength = SUtils.getStrengthOfPotentialAttackers(myCapital, data, player, tFirst, true, alreadyAttacked);
			final HashMap<Territory, Float> eCapMap = new HashMap<Territory, Float>();
			float maxStrength = 0.0F;
			// Territory maxSTerr = null;
			for (final Territory eCapTerr : eCapTerrs)
			{
				final List<Unit> eCapUnits = eCapTerr.getUnits().getMatches(Matches.enemyUnit(player, data));
				final float eStrength = SUtils.strength(eCapUnits, false, false, tFirst);
				eCapMap.put(eCapTerr, eStrength);
				if (eStrength > maxStrength)
				{
					maxStrength = eStrength;
					// maxSTerr = eCapTerr;
				}
			}
			SUtils.reorder(eCapTerrs, eCapMap, true);
			final List<Collection<Unit>> tempMoves = new ArrayList<Collection<Unit>>();
			final List<Route> tempRoutes = new ArrayList<Route>();
			final List<Unit> tempAMoved = new ArrayList<Unit>();
			// float totStrengthEliminated = 0.0F;
			final List<Territory> capThreatElim = new ArrayList<Territory>(alreadyAttacked);
			xAlreadyMoved.addAll(unitsAlreadyMoved);
			float strengthForAttack = 0.0F;
			float capAttackFactor = 1.45F;
			if (eCapTerrs.size() > 1)
				capAttackFactor = 1.25F;
			for (final Territory killTerr : eCapTerrs)
			{
				final float realEStrength = eCapMap.get(killTerr);
				float sNeeded = realEStrength * capAttackFactor + 5.0F;
				final float blitzS = SUtils.inviteBlitzAttack(false, killTerr, sNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, true, true);
				sNeeded -= blitzS;
				final float planeS = SUtils.invitePlaneAttack(false, false, killTerr, sNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player);
				sNeeded -= planeS;
				final float landS = SUtils.inviteLandAttack(false, killTerr, sNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, true, true, alreadyAttacked);
				sNeeded -= landS;
				final float transS = SUtils.inviteTransports(false, killTerr, sNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, tFirst, false, seaTerrAttacked);
				sNeeded -= transS;
				strengthForAttack = blitzS + planeS + landS + transS;
				if (strengthForAttack > (realEStrength * 0.92F + 2.0F)) // we can retreat into the capital
				{
					tempMoves.addAll(xMoveUnits);
					tempRoutes.addAll(xMoveRoutes);
					tempAMoved.addAll(xAlreadyMoved);
					capThreatElim.add(killTerr);
				}
				xMoveUnits.clear();
				xMoveRoutes.clear();
				xAlreadyMoved.clear();
				xAlreadyMoved.addAll(unitsAlreadyMoved);
				xAlreadyMoved.addAll(tempAMoved);
			}
			final float xCapThreat = SUtils.getStrengthOfPotentialAttackers(myCapital, data, player, tFirst, true, capThreatElim);
			final Collection<Unit> alliedCapUnits = myCapital.getUnits().getUnits();
			final Collection<Unit> purchaseUnits = player.getUnits().getUnits();
			float newStrength = SUtils.strength(purchaseUnits, false, false, tFirst);
			for (final Collection<Unit> xUnits : tempMoves)
			{
				alliedCapUnits.removeAll(xUnits);
			}
			final float strengthLeft = SUtils.strength(alliedCapUnits, false, false, tFirst);
			newStrength += strengthLeft;
			// boolean hasBombers = false;
			if (newStrength > xCapThreat * 0.92F)
			{
				alreadyAttacked.addAll(capThreatElim);
				for (final Collection<Unit> tM : tempMoves)
					moveUnits.add(tM);
				moveRoutes.addAll(tempRoutes);
				unitsAlreadyMoved.addAll(tempAMoved);
				capDanger = false;
			}
			else
			{
				capDanger = markFactoryUnits(data, player, unitsAlreadyMoved);
				if (capDanger)
				{
					final Collection<Unit> allFactoryUnits = myCapital.getUnits().getUnits();
					float myTotalStrength = SUtils.strength(allFactoryUnits, false, false, tFirst);
					final Collection<Unit> newUnits = player.getUnits().getUnits();
					myTotalStrength += SUtils.strength(newUnits, false, false, tFirst) * 0.75F; // play it safe
					/*if (myTotalStrength < eCapStrength * 1.25F)
					{
						float addStrength = eCapStrength * 1.25F - myTotalStrength;
						float landStrength = SUtils.inviteLandAttack(false, myCapital, addStrength + 2.0F, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, false, true, alreadyAttacked);
					}*/
				}
			}
		}
		else
			// Make sure we keep enough units in the capital for defense.
			// TODO: Consider what is being taken out by attack and reduce strength
			SetCapGarrison(myCapital, player, eCapStrength, unitsAlreadyMoved);
		xMoveUnits.clear();
		xMoveRoutes.clear();
		xAlreadyMoved.clear();
		// find factories with movement, and move them to places we own if they are sitting on top of each other
		final CompositeMatch<Unit> ownedUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
		final CompositeMatch<Unit> ourFactory = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitCanProduceUnits);
		final CompositeMatch<Unit> moveableFactory = new CompositeMatchAnd<Unit>(ourFactory, Matches.UnitCanMove, Matches.UnitCanNotMoveDuringCombatMove.invert());
		final List<Territory> moveableFactoryTerritories = SUtils.findUnitTerr(data, player, moveableFactory);
		if (!moveableFactoryTerritories.isEmpty())
		{
			final CompositeMatch<Territory> endConditionEnemyLand = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemy(player, data), Matches.TerritoryIsNotImpassable, Matches.TerritoryIsLand);
			final CompositeMatch<Territory> routeConditionLand = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), Matches.TerritoryIsNotImpassable, Matches.TerritoryIsLand);
			final List<Territory> owned = SUtils.allOurTerritories(data, player);
			final List<Territory> existingFactories = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitCanProduceUnits);
			owned.removeAll(existingFactories);
			final List<Territory> isWaterConvoy = SUtils.onlyWaterTerr(data, owned);
			owned.removeAll(isWaterConvoy);
			final CompositeMatch<Territory> goodFactTerr = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player, data), Matches.isTerritoryOwnedBy(player));
			for (final Territory moveableFactoryTerr : moveableFactoryTerritories)
			{
				final List<Unit> moveableFactories = moveableFactoryTerr.getUnits().getMatches(moveableFactory);
				if (moveableFactories.size() > 0 && moveableFactoryTerr.getUnits().getMatches(ourFactory).size() > 1)
				{
					final List<Territory> goodNeighbors = new ArrayList<Territory>(data.getMap().getNeighbors(moveableFactoryTerr, goodFactTerr));
					goodNeighbors.retainAll(owned);
					Collections.shuffle(goodNeighbors);
					final IntegerMap<Territory> terrValue = new IntegerMap<Territory>();
					for (final Territory moveFactToTerr : goodNeighbors)
					{
						// sorting territories to have ones with greatest production and closeness to enemy first (by land, then by sea) (veqryn)
						int territoryValue = 0;
						if (SUtils.hasLandRouteToEnemyOwnedCapitol(moveFactToTerr, player, data))
							territoryValue += 3;
						if (SUtils.findNearest(moveFactToTerr, Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits),
									Matches.TerritoryIsNotImpassableToLandUnits(player, data), data) != null)
							territoryValue += 1;
						if (Matches.territoryHasWaterNeighbor(data).match(moveFactToTerr))
							territoryValue += 3;
						Route r = SUtils.findNearest(moveFactToTerr, endConditionEnemyLand, routeConditionLand, data);
						if (r != null)
						{
							territoryValue += 10 - r.getLength();
						}
						else
						{
							r = SUtils.findNearest(moveFactToTerr, endConditionEnemyLand, Matches.TerritoryIsWater, data);
							if (r != null)
								territoryValue += 8 - r.getLength();
							else
								territoryValue -= 115;
						}
						territoryValue += 4 * TerritoryAttachment.get(moveFactToTerr).getProduction();
						final List<Territory> weOwnAll = SUtils.getNeighboringEnemyLandTerritories(data, player, moveFactToTerr);
						final List<Territory> isWater = SUtils.onlyWaterTerr(data, weOwnAll);
						weOwnAll.removeAll(isWater);
						final Iterator<Territory> weOwnAllIter = weOwnAll.iterator();
						while (weOwnAllIter.hasNext())
						{
							final Territory tempFact = weOwnAllIter.next();
							if (Matches.TerritoryIsNeutralButNotWater.match(tempFact) || Matches.TerritoryIsImpassable.match(tempFact))
								weOwnAllIter.remove();
						}
						territoryValue -= 15 * weOwnAll.size();
						if (TerritoryAttachment.get(moveFactToTerr).getProduction() < 2)
							territoryValue -= 100;
						if (TerritoryAttachment.get(moveFactToTerr).getProduction() < 1)
							territoryValue -= 100;
						terrValue.put(moveFactToTerr, territoryValue);
					}
					SUtils.reorder(goodNeighbors, terrValue, true);
					if (goodNeighbors.size() == 0)// goodNeighbors == null ||
						continue;
					int i = 0;
					int j = 0;
					final int diff = moveableFactoryTerr.getUnits().getMatches(ourFactory).size() - moveableFactories.size();
					for (final Unit factoryUnit : moveableFactories)
					{
						if (diff < 1 && j >= moveableFactories.size() - 1)
							continue;
						if (i >= goodNeighbors.size())
							i = 0;
						moveRoutes.add(data.getMap().getRoute(moveableFactoryTerr, goodNeighbors.get(i)));
						moveUnits.add(Collections.singleton(factoryUnit));
						i++;
						j++;
					}
				}
			}
		}
		final List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
		final List<Territory> ourEnemyTerr = new ArrayList<Territory>();
		final HashMap<Territory, Float> rankMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, tFirst, false, false);
		SUtils.reorder(liveEnemyCaps, rankMap, true);
		for (final Territory badCapitol : liveEnemyCaps)
		{
			xMoveUnits.clear();
			xMoveRoutes.clear();
			xAlreadyMoved.clear();
			xAlreadyMoved.addAll(unitsAlreadyMoved);
			final Collection<Unit> badCapUnits = badCapitol.getUnits().getUnits();
			final float badCapStrength = SUtils.strength(badCapUnits, false, false, tFirst);
			float alliedCapStrength = 0.0F;
			float ourXStrength = 0.0F;
			final List<Territory> alliedCapTerr = SUtils.getNeighboringLandTerritories(data, player, badCapitol);
			// if (alliedCapTerr == null || alliedCapTerr.isEmpty())
			// continue;
			final List<Unit> alliedCUnits = new ArrayList<Unit>();
			final List<Unit> ourCUnits = new ArrayList<Unit>();
			if (!alliedCapTerr.isEmpty())
			{
				for (final Territory aT : alliedCapTerr)
				{ // alliedCUnits contains ourCUnits
					alliedCUnits.addAll(aT.getUnits().getMatches(alliedAirLandUnitNotOwned));
					ourCUnits.addAll(aT.getUnits().getMatches(attackable));
				}
				ourCUnits.removeAll(unitsAlreadyMoved);
				alliedCapStrength += SUtils.strength(alliedCUnits, true, false, tFirst);
				ourXStrength += SUtils.strength(ourCUnits, true, false, tFirst);
			}
			remainingStrengthNeeded = badCapStrength * 2.5F + 8.0F; // bring everything to get the capital
			// float origSNeeded = remainingStrengthNeeded;
			final float blitzStrength = SUtils.inviteBlitzAttack(false, badCapitol, remainingStrengthNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, true, true);
			remainingStrengthNeeded -= blitzStrength;
			final float transStrength = SUtils.inviteTransports(false, badCapitol, remainingStrengthNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, tFirst, false, seaTerrAttacked);
			remainingStrengthNeeded -= transStrength;
			final float airStrength = SUtils.invitePlaneAttack(false, false, badCapitol, remainingStrengthNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player);
			remainingStrengthNeeded -= airStrength;
			final float addLandStrength = blitzStrength + transStrength;
			if (ourXStrength < 1.0F && addLandStrength < 1.0F)
				continue;
			final float additionalStrength = addLandStrength + airStrength;
			// alliedCapStrength += additionalStrength;
			ourXStrength += additionalStrength;
			final Collection<Unit> invasionUnits = ourCUnits;
			for (final Collection<Unit> invaders : xMoveUnits)
				invasionUnits.addAll(invaders);
			boolean weWin = SUtils.calculateTUVDifference(badCapitol, invasionUnits, badCapUnits, costMap, player, data, aggressive, Properties.getAirAttackSubRestricted(data), tFirst);
			if (weWin || (alliedCapStrength > (badCapStrength * 1.10F + 3.0F) && (ourXStrength > (0.82F * badCapStrength + 3.0F))))
			{
				// s_logger.fine("Player: "+player.getName() + "; Bad Cap: "+badCapitol.getName() + "; Our Attack Units: "+ ourCUnits);
				// s_logger.fine("Allied Cap Strength: "+alliedCapStrength+"; Bad Cap Strength: "+badCapStrength+"; Our Strength: "+ourXStrength);
				enemyOwned.remove(badCapitol); // no need to attack later
				for (final Territory aT2 : alliedCapTerr)
				{
					final List<Unit> ourCUnits2 = aT2.getUnits().getMatches(attackable);
					ourCUnits2.removeAll(unitsAlreadyMoved);
					moveUnits.add(ourCUnits2);
					final Route aR = data.getMap().getLandRoute(aT2, badCapitol);
					moveRoutes.add(aR);
					unitsAlreadyMoved.addAll(ourCUnits2);
				}
				moveUnits.addAll(xMoveUnits);
				moveRoutes.addAll(xMoveRoutes);
				unitsAlreadyMoved.addAll(xAlreadyMoved);
				alreadyAttacked.add(badCapitol);
			}
			weWin = false;
			xMoveUnits.clear();
			xMoveRoutes.clear();
			xAlreadyMoved.clear();
		}
		// find the territories we can just walk into
		enemyOwned.removeAll(alreadyAttacked);
		enemyOwned.retainAll(rankMap.keySet());
		SUtils.reorder(enemyOwned, rankMap, true);
		for (final Territory enemy : enemyOwned)
		{
			xMoveUnits.clear();
			xMoveRoutes.clear();
			xAlreadyMoved.clear();
			xAlreadyMoved.addAll(unitsAlreadyMoved);
			final float eStrength = SUtils.strength(enemy.getUnits().getUnits(), false, false, tFirst);
			if (eStrength < 0.50F)
			{
				// only take it with 1 unit
				boolean taken = false;
				final Set<Territory> nextTerrs = data.getMap().getNeighbors(enemy, enemyTerr);
				final Iterator<Territory> nTIter = nextTerrs.iterator();
				while (nTIter.hasNext())
				{
					final Territory nTcheck = nTIter.next();
					if (Matches.TerritoryIsImpassableToLandUnits(player, data).match(nTcheck))
						nTIter.remove();
				}
				final HashMap<Territory, Float> canBeTaken = new HashMap<Territory, Float>();
				for (final Territory nextOne : nextTerrs)
				{
					final List<Territory> myGoodNeighbors = SUtils.getNeighboringLandTerritories(data, player, nextOne);
					if (myGoodNeighbors.size() > 0) // we own the neighbors...let them handle bringing blitz units in
						continue;
					final List<Unit> totUnits = nextOne.getUnits().getMatches(enemyLandAirUnit);
					final float thisStrength = SUtils.strength(totUnits, false, false, tFirst);
					canBeTaken.put(nextOne, thisStrength);
				}
				final Set<Territory> blitzTerrs = canBeTaken.keySet();
				for (final Territory attackFrom : data.getMap().getNeighbors(enemy, Matches.territoryHasLandUnitsOwnedBy(player)))
				{
					if (taken)
						break;
					// just get an infantry at the top of the queue
					final List<Unit> aBlitzUnits = attackFrom.getUnits().getMatches(blitzUnit);
					aBlitzUnits.removeAll(unitsAlreadyMoved);
					Territory findOne = null;
					if (canBeTaken.size() > 0) // we have another terr we can take
					{
						for (final Territory attackTo : blitzTerrs)
						{
							if (canBeTaken.get(attackTo) < 1.0F)
								findOne = attackTo;
						}
					}
					if (findOne != null && !aBlitzUnits.isEmpty()) // use a tank
					{
						for (final Territory bT : blitzTerrs)
						{
							if (canBeTaken.get(bT) < 4.0F)
							{
								final Route newRoute = new Route();
								newRoute.setStart(attackFrom);
								newRoute.add(enemy);
								newRoute.add(bT);
								Unit deleteThisOne = null;
								for (final Unit tank : aBlitzUnits)
								{
									if (deleteThisOne == null)
									{
										moveUnits.add(Collections.singleton(tank));
										moveRoutes.add(newRoute);
										unitsAlreadyMoved.add(tank);
										deleteThisOne = tank;
										alreadyAttacked.add(enemy);
										emptyBadTerr.remove(enemy);
									}
								}
								aBlitzUnits.remove(deleteThisOne);
							}
						}
					}
					else
					// use an infantry
					{
						final List<Unit> unitsSorted = SUtils.sortTransportUnits(attackFrom.getUnits().getMatches(attackable));
						unitsSorted.removeAll(unitsAlreadyMoved);
						for (final Unit unit : unitsSorted)
						{
							moveRoutes.add(data.getMap().getRoute(attackFrom, enemy));
							if (attackFrom.isWater())
							{
								final List<Unit> units2 = attackFrom.getUnits().getMatches(Matches.unitIsLandAndOwnedBy(player));
								moveUnits.add(Util.difference(units2, unitsAlreadyMoved));
								unitsAlreadyMoved.addAll(units2);
							}
							else
								moveUnits.add(Collections.singleton(unit));
							unitsAlreadyMoved.add(unit);
							alreadyAttacked.add(enemy);
							emptyBadTerr.add(enemy);
							taken = true;
							break;
						}
					}
				}
			}
		}
		ourStrength = 0.0F;
		float badStrength = 0.0F;
		// boolean weAttacked = false, weAttacked2 = false;
		// int EinfArtCount = 0, OinfArtCount = 0;
		bigProblem2.removeAll(alreadyAttacked);
		SUtils.reorder(bigProblem2, rankMap, true);
		// TODO: Rewrite this section. It could be much cleaner.
		xAlreadyMoved.clear();
		xMoveUnits.clear();
		xMoveRoutes.clear();
		for (final Territory badTerr : bigProblem2)
		{
			xMoveUnits.clear();
			xMoveRoutes.clear();
			xAlreadyMoved.clear();
			// weAttacked = false;
			final Collection<Unit> enemyUnits = badTerr.getUnits().getUnits();
			badStrength = SUtils.strength(enemyUnits, false, false, tFirst);
			if (badStrength > 0.0F)
			{
				ourStrength = 0.0F;
				final List<Territory> capitalAttackTerr = new ArrayList<Territory>(data.getMap().getNeighbors(badTerr, Matches.territoryHasLandUnitsOwnedBy(player)));
				final List<Unit> capSaverUnits = new ArrayList<Unit>();
				for (final Territory capSavers : capitalAttackTerr)
				{
					capSaverUnits.addAll(capSavers.getUnits().getMatches(attackable));
				}
				capSaverUnits.removeAll(unitsAlreadyMoved);
				ourStrength += SUtils.strength(capSaverUnits, true, false, tFirst);
				remainingStrengthNeeded = badStrength * attackFactor + 4.0F - ourStrength;
				xAlreadyMoved.addAll(capSaverUnits);
				xAlreadyMoved.addAll(unitsAlreadyMoved);
				float blitzStrength = SUtils.inviteBlitzAttack(false, badTerr, remainingStrengthNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, true, false);
				remainingStrengthNeeded -= blitzStrength;
				float seaStrength = SUtils.inviteTransports(false, badTerr, remainingStrengthNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, tFirst, false, seaTerrAttacked);
				remainingStrengthNeeded -= seaStrength;
				// weAttacked = (ourStrength + blitzStrength) > 0.0F; // land Units confirmed
				float planeStrength = SUtils.invitePlaneAttack(false, false, badTerr, remainingStrengthNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player);
				ourStrength += blitzStrength + seaStrength;
				if (ourStrength < 1.0F)
					continue;
				else
					ourStrength += planeStrength;
				final List<Unit> allMyUnits = new ArrayList<Unit>(capSaverUnits);
				for (final Collection<Unit> xUnits : xMoveUnits)
					allMyUnits.addAll(xUnits);
				boolean weWin = SUtils.calculateTUVDifference(badTerr, allMyUnits, enemyUnits, costMap, player, data, aggressive, Properties.getAirAttackSubRestricted(data), tFirst);
				if (weWin)
				{
					if (bigProblem2.size() > 1)
						maxAttackFactor = 1.54F;// concerned about overextending if more than 1 territory
					remainingStrengthNeeded = (maxAttackFactor * badStrength) + 3.0F;
					final float landStrength = SUtils.inviteLandAttack(false, badTerr, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, true, false, alreadyAttacked);
					remainingStrengthNeeded -= landStrength;
					blitzStrength = SUtils.inviteBlitzAttack(false, badTerr, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, true, false);
					remainingStrengthNeeded -= blitzStrength;
					planeStrength = SUtils.invitePlaneAttack(false, false, badTerr, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player);
					remainingStrengthNeeded -= planeStrength;
					seaStrength = SUtils.inviteTransports(false, badTerr, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, tFirst, false, seaTerrAttacked);
					remainingStrengthNeeded -= seaStrength;
					// weAttacked = true;
				}
				weWin = false;
				/* This is causing bad results
					            remainingStrengthNeeded += 2.0F;
					            if (weAttacked && remainingStrengthNeeded > 0.0F)
					            {
					            	remainingStrengthNeeded -= SUtils.inviteBlitzAttack(false, badTerr, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, true, false);
					            	remainingStrengthNeeded -= SUtils.invitePlaneAttack(false, false, badTerr, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player);
					            	remainingStrengthNeeded -= SUtils.inviteTransports(false, badTerr, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, tFirst, false, seaTerrAttacked);
					            }
				*/
			}
			xMoveUnits.clear();
			xMoveRoutes.clear();
			xAlreadyMoved.clear();
		}
		/**
		 * Thought here is to organize units that are aggressive in a large bunch
		 */
		final float maxEStrength = enemyMap.get(maxAttackTerr);
		final PlayerID maxAPlayer = maxAttackTerr.getOwner();
		final float myAttackStrength = SUtils.getStrengthOfPotentialAttackers(maxAttackTerr, data, maxAPlayer, tFirst, false, null);
		// estimate the target route (most likely the closest allied factory)
		// List<Territory> ourTerr = SUtils.getNeighboringLandTerritories(data, player, maxAttackTerr);
		final Route enemyRoute = SUtils.findNearest(maxAttackTerr, Matches.territoryIsAlliedAndHasAlliedUnitMatching(data, player, Matches.UnitCanProduceUnits),
					Matches.TerritoryIsNotImpassableToLandUnits(player, data), data);
		if (enemyRoute != null)
		{
			if (myAttackStrength > maxEStrength)
			{
				// do we need to consolidate units?
			}
			else
			{
				// figure out how to pull units toward the attacking group
			}
		}
		// find the territories we can reasonably expect to take
		float alliedStrength = 0.0F;
		// StrengthEvaluator capStrEval = StrengthEvaluator.evalStrengthAt(data, player, myCapital, false, true, tFirst, true);
		// boolean useCapNeighbors = true;
		// if (capStrEval.inDanger(0.90F)) // TODO: really evaluate the territories around the capitol
		// useCapNeighbors = false; // don't use the capital neighbors to attack terr which are not adjacent to cap
		SUtils.reorder(enemyOwned, rankMap, true);
		enemyOwned.removeAll(alreadyAttacked);
		for (final Territory enemy : enemyOwned)
		{
			xMoveUnits.clear();
			xMoveRoutes.clear();
			xAlreadyMoved.clear();
			final Collection<Unit> eUnits = enemy.getUnits().getUnits();
			final float enemyStrength = SUtils.strength(eUnits, false, false, tFirst);
			final TerritoryAttachment ta = TerritoryAttachment.get(enemy);
			final float pValue = ta.getProduction();
			if (Matches.TerritoryIsNeutralButNotWater.match(enemy) && enemyStrength > pValue * 9 && Math.random() < 0.9) // why bother...focus on enemies
				continue; // TODO: Strengthen this determination
			if (enemyStrength > 0.0F)
			{
				ourStrength = 0.0F;
				alliedStrength = 0.0F;
				final Set<Territory> attackFrom = data.getMap().getNeighbors(enemy, Matches.territoryHasNoEnemyUnits(player, data));
				attackFrom.removeAll(impassableTerrs);
				final HashMap<Territory, Float> strengthMap = new HashMap<Territory, Float>();
				alreadyAttacked.add(enemy);
				for (final Territory aCheck : attackFrom)
					strengthMap.put(aCheck, SUtils.getStrengthOfPotentialAttackers(aCheck, data, player, tFirst, true, alreadyAttacked));
				final List<Unit> dontMoveWithUnits = new ArrayList<Unit>();
				final List<Territory> attackList = new ArrayList<Territory>(attackFrom);
				SUtils.reorder(attackList, strengthMap, false); // order our available terr by weakest enemy potential
				final List<Unit> myAUnits = new ArrayList<Unit>();
				for (final Territory checkTerr2 : attackList)
				{
					float strengthLimit = 0.0F;
					if (Matches.territoryIsAlliedAndHasAlliedUnitMatching(data, player, Matches.UnitCanProduceUnits).match(checkTerr2))
					{
						strengthLimit = strengthMap.get(checkTerr2);
						final List<Unit> ourFactUnits = checkTerr2.getUnits().getMatches(Matches.UnitIsNotSea);
						ourFactUnits.removeAll(unitsAlreadyMoved);
						final float factStrength = SUtils.strength(ourFactUnits, false, false, tFirst);
						if (strengthLimit * 0.5F > (factStrength + TerritoryAttachment.get(checkTerr2).getProduction() * 3.0F))
							strengthLimit = 0.0F; // won't matter if we stay here
					}
					final List<Unit> goodUnits = checkTerr2.getUnits().getMatches(attackable);
					goodUnits.removeAll(unitsAlreadyMoved);
					final Iterator<Unit> goodUIter = goodUnits.iterator();
					final Route gRoute = data.getMap().getLandRoute(checkTerr2, enemy);
					if (gRoute == null || goodUnits.isEmpty())
						continue;
					while (goodUIter.hasNext())
					{
						final Unit goodUnit = goodUIter.next();
						if (!MoveValidator.hasEnoughMovement(goodUnit, gRoute) || strengthLimit > 0.0F)
						{
							goodUIter.remove();
							dontMoveWithUnits.add(goodUnit); // block these off later on
							strengthLimit -= SUtils.uStrength(goodUnit, false, false, tFirst);
						}
					}
					ourStrength += SUtils.strength(goodUnits, true, false, tFirst);
					final List<Unit> aUnits = checkTerr2.getUnits().getMatches(alliedAirLandUnitNotOwned);
					aUnits.removeAll(goodUnits);
					aUnits.removeAll(unitsAlreadyMoved);
					alliedStrength += SUtils.strength(aUnits, true, false, tFirst);
					myAUnits.addAll(goodUnits);
				}
				float xRS = 1000.0F;
				xAlreadyMoved.addAll(unitsAlreadyMoved);
				xAlreadyMoved.addAll(myAUnits);
				final float blitzStrength = SUtils.inviteBlitzAttack(false, enemy, xRS, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, true, false);
				xRS -= blitzStrength;
				final float planeStrength = SUtils.invitePlaneAttack(false, false, enemy, xRS, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player);
				xRS -= planeStrength;
				ourStrength += blitzStrength;
				if (ourStrength < 1.0F)
					continue;
				else
					ourStrength += planeStrength;
				final List<Unit> allMyUnits = new ArrayList<Unit>(myAUnits);
				for (final Collection<Unit> xUnits : xMoveUnits)
					allMyUnits.addAll(xUnits);
				boolean weWin = false;
				if (Matches.TerritoryIsNeutralButNotWater.match(enemy))
				{
					if (ourStrength > (attackFactor2 * enemyStrength + 3.0F))
						weWin = true;
				}
				else
					weWin = SUtils.calculateTUVDifference(enemy, allMyUnits, eUnits, costMap, player, data, aggressive, Properties.getAirAttackSubRestricted(data), tFirst);
				if (!weWin)
				{
					alreadyAttacked.remove(enemy);
					continue;
				}
				weWin = false;
				remainingStrengthNeeded = (attackFactor * enemyStrength) + 4.0F; // limit the attackers
				if (attackFrom.size() == 1) // if we have 1 big attacker
				{
					xTerr = attackList.get(0);
					final List<Territory> enemyLandTerr = SUtils.getNeighboringEnemyLandTerritories(data, player, xTerr);
					if (enemyLandTerr.size() == 1) // the only enemy territory is the one we are attacking
						remainingStrengthNeeded = (maxAttackFactor * enemyStrength) + 4.0F; // blow it away
				}
				final float landStrength2 = SUtils.inviteLandAttack(false, enemy, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, true, false, alreadyAttacked);
				remainingStrengthNeeded -= landStrength2;
				final float blitzStrength2 = SUtils.inviteBlitzAttack(false, enemy, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, true, false);
				remainingStrengthNeeded -= blitzStrength2;
				final float planeStrength2 = SUtils.invitePlaneAttack(false, false, enemy, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player);
				// deleteStrength = SUtils.verifyPlaneAttack(data, xMoveUnits, xMoveRoutes, player, alreadyAttacked);
				// planeStrength -= deleteStrength;
				remainingStrengthNeeded -= planeStrength2;
				final float seaStrength2 = SUtils.inviteTransports(false, enemy, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, tFirst, false, seaTerrAttacked);
				remainingStrengthNeeded -= seaStrength2;
			}
			// weAttacked2 = false;
			xMoveUnits.clear();
			xMoveRoutes.clear();
			xAlreadyMoved.clear();
		}
		populateBomberCombat(data, unitsAlreadyMoved, moveUnits, moveRoutes, player);
	}
	
	/**
	 * Push all remaining loaded units onto the best possible land location
	 * 
	 * @param data
	 * @param unitsAlreadyMoved
	 * @param moveUnits
	 * @param moveRoutes
	 * @param player
	 */
	private void populateFinalTransportUnload(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		setImpassableTerrs(player);
		// Collection<Territory> impassableTerrs = getImpassableTerrs();
		final TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		final boolean tFirst = transportsMayDieFirst();
		final Match<Unit> myTransport = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsTransport);
		final Match<Unit> transports = new CompositeMatchAnd<Unit>(Matches.transportIsTransporting(), myTransport);
		final List<Territory> transTerr = SUtils.findTersWithUnitsMatching(data, player, transports);
		if (transTerr.isEmpty())
			return;
		final List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
		final List<Territory> ourEnemyTerr = new ArrayList<Territory>();
		final HashMap<Territory, Float> rankMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, tFirst, true, true);
		for (final Territory t : transTerr)
		{
			final List<Unit> myTransports = t.getUnits().getMatches(myTransport);
			final List<Unit> loadedUnits = new ArrayList<Unit>();
			final Iterator<Unit> tIter = myTransports.iterator();
			while (tIter.hasNext())
			{
				final Unit transport = tIter.next();
				if (!tracker.isTransporting(transport))
					tIter.remove();
				else
					loadedUnits.addAll(tracker.transporting(transport));
			}
			if (myTransports.isEmpty() || loadedUnits.isEmpty())
				continue;
			final List<Territory> unloadTerr = SUtils.getNeighboringLandTerritories(data, player, t);
			if (unloadTerr.isEmpty())
				continue;
			SUtils.reorder(unloadTerr, rankMap, true);
			final Territory landOn = unloadTerr.get(0);
			final Route landRoute = data.getMap().getRoute(t, landOn);
			if (landRoute != null)
			{
				moveUnits.add(loadedUnits);
				moveRoutes.add(landRoute);
			}
		}
	}
	
	/**
	 * Populate final move to coast moves. (Any units that are in a non-coastal ter that have absolutely no land threats, and haven't moved, move them to the coast)
	 * 
	 * @param data
	 * @param unitsAlreadyMoved
	 * @param moveUnits
	 * @param moveRoutes
	 * @param player
	 */
	private void populateFinalMoveToCoast(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		final Match<Territory> territoryIsOwnedByXOrAllyAndIsPort = new Match<Territory>()
		{
			@Override
			public boolean match(final Territory ter)
			{
				if (!data.getRelationshipTracker().isAllied(player, ter.getOwner()))
					return false;
				if (data.getMap().getNeighbors(ter, Matches.TerritoryIsWater).isEmpty())
					return false;
				return true;
			}
		};
		for (final Territory ter : data.getMap().getTerritoriesOwnedBy(player))
		{
			if (ter.isWater())
				continue;
			if (data.getMap().getNeighbors(ter, Matches.TerritoryIsWater).size() > 0)
				continue; // Skip, cause we're already next to water
			if (SUtils.findNearest(ter, new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data), Matches.TerritoryIsLand),
						Matches.TerritoryIsNotImpassableToLandUnits(player, data), data) != null)
				continue;
			final List<Unit> unmovedUnits = ter.getUnits().getMatches(
						new CompositeMatchAnd<Unit>(Matches.unitHasMovementLeft, Matches.UnitIsNotAA, Matches.UnitCanBeTransported, Matches.unitIsOwnedBy(player)));
			if (unmovedUnits.isEmpty())
				continue;
			@SuppressWarnings("unchecked")
			final List<Unit> landThreats = SUtils.findAttackers(ter, 25, new HashSet<Integer>(), player, data, new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.unitIsEnemyOf(data, player)),
						Match.ALWAYS_MATCH, new ArrayList<Territory>(), new ArrayList<Route>(), false);
			if (landThreats.size() > 0)
				continue; // We're only moving to the coast if the ter is landlocked and has no land threats
			Territory closestPortTer = null;
			int closestDistance = Integer.MAX_VALUE;
			for (final Territory portTer : data.getMap().getTerritories())
			{
				if (portTer.isWater())
					continue;
				if (!territoryIsOwnedByXOrAllyAndIsPort.match(portTer))
					continue;
				final Route passableLandRoute = data.getMap().getRoute(ter, portTer, new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable));
				if (passableLandRoute == null)
					continue;
				final int distance = passableLandRoute.getLength();
				if (distance < closestDistance)
				{
					closestPortTer = portTer;
					closestDistance = distance;
				}
			}
			if (closestPortTer == null || closestPortTer.equals(ter))
				continue; // No coastal ter found to go to
			final int slowestUnitMovement = DUtils.GetSlowestMovementUnitInList(unmovedUnits);
			Route passableLandRoute = data.getMap().getRoute(ter, closestPortTer, new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable));
			if (passableLandRoute != null)
				passableLandRoute = DUtils.TrimRoute_BeforeFirstTerWithEnemyUnits(passableLandRoute, slowestUnitMovement, player, data);
			if (passableLandRoute != null)
			{
				moveUnits.add(unmovedUnits);
				moveRoutes.add(passableLandRoute);
			}
		}
	}
	
	/**
	 * Populate move unused transports to fill location. (Any transports that have not been used AT ALL this round, and are not by any immediate land units, move them to fill location)
	 * (Really, what this method does is it finds all factories with land units waiting that do not have sufficient transports. Then it has each of those ters call for unused transports to come.
	 * After all those calls, the cap calls all other unused transports that don't have units on them or nearby.)
	 * 
	 * @param data
	 * @param moveUnits
	 * @param moveRoutes
	 * @param player
	 */
	private void populateMoveUnusedTransportsToFillLocation(final GameData data, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		final Territory ourCap = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		if (!isAmphibAttack(player, false) || !Matches.territoryHasWaterNeighbor(data).match(ourCap)) // Will return if capitol is not next to water
			return;
		final Match<Unit> unusedTransportMatch = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsTransport, Matches.transportIsNotTransporting(), Matches.unitHasMovementLeft,
					Matches.unitHasNotMoved);
		for (final Territory ter : data.getMap().getTerritoriesOwnedBy(player))
		{
			if (ter.isWater())
				continue;
			if (data.getMap().getNeighbors(ter, Matches.TerritoryIsWater).isEmpty())
				continue; // Skip, cause we're not next to water
			if (SUtils.findNearest(ter, new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data), Matches.TerritoryIsLand),
						Matches.TerritoryIsNotImpassableToLandUnits(player, data), data) != null)
				continue; // If this ter has a land route to an enemy don't try to call transports to pick them up
			int transportSpaceNeededForTerUnits = 0;
			for (final Unit unit : ter.getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand, Matches.UnitCanBeTransported, Matches.UnitIsNotAA)))
			{
				final int transportCost = UnitAttachment.get(unit.getUnitType()).getTransportCost();
				if (transportCost <= 0)
					continue;
				transportSpaceNeededForTerUnits += transportCost;
			}
			int transportSpaceLeftNearby = 0;
			for (final Territory seaTer : DUtils.GetTerritoriesWithinXDistanceOfYMatchingZ(data, ter, 3, Matches.TerritoryIsWater))
			{
				for (final Unit transport : seaTer.getUnits().getMatches(unusedTransportMatch))
					transportSpaceLeftNearby += UnitAttachment.get(transport.getUnitType()).getTransportCapacity();
			}
			if (transportSpaceLeftNearby >= transportSpaceNeededForTerUnits)
				continue; // We already have enough transports here
			boolean foundAnyUnusedTransports = false;
			// Now loop through unused transports and move them to ter
			// We keep calling unused transports till we meet the space need of ter units
			for (final Territory seaTer : data.getMap().getTerritories())
			{
				if (!seaTer.isWater())
					continue;
				final List<Unit> unitsToMoveToTer = new ArrayList<Unit>();
				for (final Unit unusedTransport : seaTer.getUnits().getMatches(unusedTransportMatch))
				{
					if (transportSpaceNeededForTerUnits > transportSpaceLeftNearby)
					{
						unitsToMoveToTer.add(unusedTransport);
						transportSpaceLeftNearby += UnitAttachment.get(unusedTransport.getUnitType()).getTransportCapacity();
					}
					else
						break;
				}
				if (unitsToMoveToTer.isEmpty())
					continue;
				final int slowestUnitMovement = DUtils.GetSlowestMovementUnitInList(unitsToMoveToTer);
				final HashMap<Match<Territory>, Integer> matches = new HashMap<Match<Territory>, Integer>();
				matches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasAlliedUnits(player, data)), 2);
				matches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data)), 3);
				matches.put(Matches.TerritoryIsWater, 6);
				Route seaRoute = data.getMap().getCompositeRoute(seaTer, data.getMap().getNeighbors(ter, Matches.TerritoryIsWater).iterator().next(), matches);
				if (seaRoute != null)
					seaRoute = DUtils.TrimRoute_BeforeFirstTerWithEnemyUnits(seaRoute, slowestUnitMovement, player, data);
				if (seaRoute != null)
				{
					foundAnyUnusedTransports = true;
					moveUnits.add(unitsToMoveToTer);
					moveRoutes.add(seaRoute);
				}
			}
			if (!foundAnyUnusedTransports)
				break;
		}
		// Now loop through unused transports and move them to cap
		// We call all unused transports
		for (final Territory seaTer : data.getMap().getTerritories())
		{
			if (!seaTer.isWater())
				continue;
			final List<Unit> unitsToMoveToTer = new ArrayList<Unit>();
			for (final Unit unusedTransport : seaTer.getUnits().getMatches(unusedTransportMatch))
				unitsToMoveToTer.add(unusedTransport);
			if (unitsToMoveToTer.isEmpty())
				continue;
			final int slowestUnitMovement = DUtils.GetSlowestMovementUnitInList(unitsToMoveToTer);
			final HashMap<Match<Territory>, Integer> matches = new HashMap<Match<Territory>, Integer>();
			matches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasAlliedUnits(player, data)), 2);
			matches.put(new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data)), 3);
			matches.put(Matches.TerritoryIsWater, 6);
			Route seaRoute = data.getMap().getCompositeRoute(seaTer, data.getMap().getNeighbors(ourCap, Matches.TerritoryIsWater).iterator().next(), matches);
			if (seaRoute != null)
				seaRoute = DUtils.TrimRoute_BeforeFirstTerWithEnemyUnits(seaRoute, slowestUnitMovement, player, data);
			if (seaRoute != null)
			{
				moveUnits.add(unitsToMoveToTer);
				moveRoutes.add(seaRoute);
			}
		}
	}
	
	private void populateBomberCombat(final GameData data, final Collection<Unit> unitsAlreadyMoved, final List<Collection<Unit>> moveUnits, final List<Route> moveRoutes, final PlayerID player)
	{
		// bombers will be more involved in attacks...if they are still available, then bomb
		final Match<Unit> ownBomber = new CompositeMatchAnd<Unit>(Matches.UnitIsStrategicBomber, Matches.unitIsOwnedBy(player), Matches.unitHasNotMoved);
		final Match<Territory> routeCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassable, Matches.territoryHasEnemyAAforCombatOnly(player, data).invert());
		final List<Unit> alreadyMoved = new ArrayList<Unit>();
		final IntegerMap<Territory> bomberImpactMap = new IntegerMap<Territory>();
		final List<Territory> enemyFactories = new ArrayList<Territory>();
		final boolean unitProduction = Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data);
		for (final Territory xT : data.getMap().getTerritories())
		{
			if (Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, Matches.UnitCanProduceUnitsAndCanBeDamaged).match(xT))
				enemyFactories.add(xT);
		}
		int factProduction = 0;
		for (final Territory eFact : enemyFactories)
		{
			factProduction = unitProduction ? TripleAUnit.getProductionPotentialOfTerritory(eFact.getUnits().getUnits(), eFact, eFact.getOwner(), data, true, true) : TerritoryAttachment.get(eFact)
						.getProduction();
			bomberImpactMap.put(eFact, factProduction);
		}
		SUtils.reorder(enemyFactories, bomberImpactMap, true);
		final List<Territory> bomberTerrs = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitIsStrategicBomber);
		for (final Territory t : enemyFactories)
		{
			int bombable = bomberImpactMap.getInt(t); // WW2V3 model TODO: build all current game models into method
			int bombersDeployed = 0;
			for (final Territory bombTerr : bomberTerrs)
			{
				final Collection<Unit> bombers = t.getUnits().getMatches(ownBomber);
				bombers.removeAll(alreadyMoved);
				if (bombers.isEmpty())
					continue;
				final Match<Territory> routeCondOrEnd = new CompositeMatchOr<Territory>(routeCond, Matches.territoryIs(t));
				final Route bombRoute = data.getMap().getRoute(t, bombTerr, routeCondOrEnd);
				if (bombRoute == null || bombRoute.getEnd() == null || bombable <= 0)
					continue;
				final Iterator<Unit> bIter = bombers.iterator();
				while (bIter.hasNext() && bombable > 0)
				{
					final Unit bomber = bIter.next();
					if (bomber == null)
						continue;
					if (AirMovementValidator.canLand(Collections.singleton(bomber), bombTerr, player, data))
					{
						moveUnits.add(Collections.singleton(bomber));
						moveRoutes.add(bombRoute);
						alreadyMoved.add(bomber);
						final UnitAttachment bA = UnitAttachment.get(bomber.getUnitType());
						bombersDeployed++;
						if (bombersDeployed % 6 != 0)
							bombable -= bA.getAttackRolls(player) * 3; // assume every 6th bomber is shot down
					}
				}
			}
		}
	}
	
	private int countTransports(final GameData data, final PlayerID player)
	{
		final CompositeMatchAnd<Unit> ownedTransport = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.unitIsOwnedBy(player));
		int sum = 0;
		for (final Territory t : data.getMap())
		{
			sum += t.getUnits().countMatches(ownedTransport);
		}
		return sum;
	}
	
	private int countLandUnits(final GameData data, final PlayerID player)
	{
		final CompositeMatchAnd<Unit> ownedLandUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.unitIsOwnedBy(player));
		int sum = 0;
		for (final Territory t : data.getMap())
		{
			sum += t.getUnits().countMatches(ownedLandUnit);
		}
		return sum;
	}
	
	/**
	 * Count everything except transports
	 * 
	 * @param data
	 * @param player
	 * @return
	 */
	private int countSeaUnits(final GameData data, final PlayerID player)
	{
		final CompositeMatchAnd<Unit> ownedSeaUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.unitIsOwnedBy(player), Matches.UnitIsNotTransport);
		int sum = 0;
		for (final Territory t : data.getMap())
		{
			sum += t.getUnits().countMatches(ownedSeaUnit);
		}
		return sum;
	}
	
	@Override
	protected void purchase(final boolean purchaseForBid, int PUsToSpend, final IPurchaseDelegate purchaseDelegate, final GameData data, final PlayerID player)
	{
		long last, now;
		last = System.currentTimeMillis();
		s_logger.fine("Doing Purchase ");
		if (PUsToSpend == 0 && player.getResources().getQuantity(data.getResourceList().getResource(Constants.PUS)) == 0) // Check whether the player has ANY PU's to spend...
			return;
		// TODO: lot of tweaks have gone into this routine without good organization...need to cleanup
		// breakdown Rules by type and cost
		final int currentRound = data.getSequence().getRound();
		int highPrice = 0;
		final List<ProductionRule> rules = player.getProductionFrontier().getRules();
		final IntegerMap<ProductionRule> purchase = new IntegerMap<ProductionRule>();
		final List<ProductionRule> landProductionRules = new ArrayList<ProductionRule>();
		final List<ProductionRule> airProductionRules = new ArrayList<ProductionRule>();
		final List<ProductionRule> seaProductionRules = new ArrayList<ProductionRule>();
		final List<ProductionRule> transportProductionRules = new ArrayList<ProductionRule>();
		final List<ProductionRule> subProductionRules = new ArrayList<ProductionRule>();
		final IntegerMap<ProductionRule> bestAttack = new IntegerMap<ProductionRule>();
		final IntegerMap<ProductionRule> bestDefense = new IntegerMap<ProductionRule>();
		final IntegerMap<ProductionRule> bestTransport = new IntegerMap<ProductionRule>();
		final IntegerMap<ProductionRule> bestMaxUnits = new IntegerMap<ProductionRule>();
		final IntegerMap<ProductionRule> bestMobileAttack = new IntegerMap<ProductionRule>();
		// ProductionRule highRule = null;
		ProductionRule carrierRule = null, fighterRule = null;
		int carrierFighterLimit = 0, maxFighterAttack = 0;
		float averageSeaMove = 0;
		final Resource pus = data.getResourceList().getResource(Constants.PUS);
		boolean isAmphib = isAmphibAttack(player, true);
		setDidPurchaseTransports(false);
		for (final ProductionRule ruleCheck : rules)
		{
			final int costCheck = ruleCheck.getCosts().getInt(pus);
			final UnitType x = (UnitType) ruleCheck.getResults().keySet().iterator().next();
			// Remove from consideration any unit with Zero Movement
			if (UnitAttachment.get(x).getMovement(player) < 1 && !(UnitAttachment.get(x).getCanProduceUnits()))
				continue;
			// Remove from consideration any unit with Zero defense, or 3 or more attack/defense than defense/attack, that is not a transport/factory/aa unit
			if (((UnitAttachment.get(x).getAttack(player) - UnitAttachment.get(x).getDefense(player) >= 3 || UnitAttachment.get(x).getDefense(player) - UnitAttachment.get(x).getAttack(player) >= 3) || UnitAttachment
						.get(x).getDefense(player) < 1)
						&& !(UnitAttachment.get(x).getCanProduceUnits() || (UnitAttachment.get(x).getTransportCapacity() > 0 && Matches.UnitTypeIsSea.match(x))))
			{
				// maybe the map only has weird units. make sure there is at least one of each type before we decide not to use it (we are relying on the fact that map makers generally put specialty units AFTER useful units in their production lists [ie: bombers listed after fighters, mortars after artillery, etc.])
				if (Matches.UnitTypeIsAir.match(x) && !airProductionRules.isEmpty())
					continue;
				if (Matches.UnitTypeIsSea.match(x) && !seaProductionRules.isEmpty())
					continue;
				if (!Matches.UnitTypeCanProduceUnits.match(x) && !landProductionRules.isEmpty() && !Matches.UnitTypeIsAir.match(x) && !Matches.UnitTypeIsSea.match(x))
					continue;
			}
			// Remove from consideration any unit which has maxBuiltPerPlayer
			if (Matches.UnitTypeHasMaxBuildRestrictions.match(x))
				continue;
			// Remove from consideration any unit which has consumesUnits
			if (Matches.UnitTypeConsumesUnitsOnCreation.match(x))
				continue;
			if (Matches.UnitTypeIsAir.match(x))
			{
				airProductionRules.add(ruleCheck);
			}
			else if (Matches.UnitTypeIsSea.match(x))
			{
				seaProductionRules.add(ruleCheck);
				averageSeaMove += UnitAttachment.get(x).getMovement(player);
			}
			else if (!Matches.UnitTypeCanProduceUnits.match(x))
			{
				if (costCheck > highPrice)
				{
					highPrice = costCheck;
				}
				landProductionRules.add(ruleCheck);
			}
			if (Matches.UnitTypeCanTransport.match(x) && Matches.UnitTypeIsSea.match(x))
			{
				// might be more than 1 transport rule... use ones that can hold at least "2" capacity (we should instead check for median transport cost, and then add all those at or above that capacity)
				if (UnitAttachment.get(x).getTransportCapacity() > 1)
					transportProductionRules.add(ruleCheck);
			}
			if (Matches.UnitTypeIsSub.match(x))
				subProductionRules.add(ruleCheck);
			if (Matches.UnitTypeIsCarrier.match(x)) // might be more than 1 carrier rule...use the one which will hold the most fighters
			{
				final int thisFighterLimit = UnitAttachment.get(x).getCarrierCapacity();
				if (thisFighterLimit >= carrierFighterLimit)
				{
					carrierRule = ruleCheck;
					carrierFighterLimit = thisFighterLimit;
				}
			}
			if (Matches.UnitTypeCanLandOnCarrier.match(x)) // might be more than 1 fighter...use the one with the best attack
			{
				final int thisFighterAttack = UnitAttachment.get(x).getAttack(player);
				if (thisFighterAttack > maxFighterAttack)
				{
					fighterRule = ruleCheck;
					maxFighterAttack = thisFighterAttack;
				}
			}
		}
		if (averageSeaMove / seaProductionRules.size() >= 1.8) // most sea units move at least 2 movement, so remove any sea units with 1 movement (dumb t-boats) (some maps like 270BC have mostly 1 movement sea units, so we must be sure not to remove those)
		{
			final List<ProductionRule> seaProductionRulesCopy = new ArrayList<ProductionRule>(seaProductionRules);
			for (final ProductionRule seaRule : seaProductionRulesCopy)
			{
				final UnitType x = (UnitType) seaRule.getResults().keySet().iterator().next();
				if (UnitAttachment.get(x).getMovement(player) < 2)
					seaProductionRules.remove(seaRule);
			}
		}
		if (subProductionRules.size() > 0 && seaProductionRules.size() > 0)
		{
			if (subProductionRules.size() / seaProductionRules.size() < 0.3) // remove submarines from consideration, unless we are mostly subs
			{
				seaProductionRules.removeAll(subProductionRules);
			}
		}
		if (purchaseForBid)
		{
			int buyLimit = PUsToSpend / 3;
			if (buyLimit == 0)
				buyLimit = 1;
			boolean landPurchase = true, goTransports = false;
			// boolean alreadyBought = false;
			final List<Territory> enemyTerritoryBorderingOurTerrs = SUtils.getNeighboringEnemyLandTerritories(data, player);
			if (enemyTerritoryBorderingOurTerrs.isEmpty())
				landPurchase = false;
			if (Math.random() > 0.25)
				seaProductionRules.removeAll(subProductionRules);
			if (PUsToSpend < 25)
			{
				if ((!isAmphib || Math.random() < 0.15) && landPurchase)
				{
					SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, landProductionRules, PUsToSpend, buyLimit, data, player, 2);
				}
				else
				{
					landPurchase = false;
					buyLimit = PUsToSpend / 5; // assume a larger threshhold
					if (Math.random() > 0.40)
					{
						SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, seaProductionRules, PUsToSpend, buyLimit, data, player, 2);
					}
					else
					{
						goTransports = true;
					}
				}
			}
			else if ((!isAmphib || Math.random() < 0.15) && landPurchase)
			{
				if (Math.random() > 0.80)
				{
					SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, landProductionRules, PUsToSpend, buyLimit, data, player, 2);
				}
			}
			else if (Math.random() < 0.35)
			{
				if (Math.random() > 0.55 && carrierRule != null && fighterRule != null)
				{// force a carrier purchase if enough available $$ for it and at least 1 fighter
					final int cost = carrierRule.getCosts().getInt(pus);
					final int fighterCost = fighterRule.getCosts().getInt(pus);
					if ((cost + fighterCost) <= PUsToSpend)
					{
						purchase.add(carrierRule, 1);
						purchase.add(fighterRule, 1);
						carrierFighterLimit--;
						PUsToSpend -= (cost + fighterCost);
						while ((PUsToSpend >= fighterCost) && carrierFighterLimit > 0)
						{ // max out the carrier
							purchase.add(fighterRule, 1);
							carrierFighterLimit--;
							PUsToSpend -= fighterCost;
						}
					}
				}
				final int airPUs = PUsToSpend / 6;
				SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, airProductionRules, airPUs, buyLimit, data, player, 2);
				final boolean buyAttack = Math.random() > 0.50;
				for (final ProductionRule rule1 : airProductionRules)
				{
					int buyThese = bestAttack.getInt(rule1);
					final int cost = rule1.getCosts().getInt(pus);
					if (!buyAttack)
						buyThese = bestDefense.getInt(rule1);
					PUsToSpend -= cost * buyThese;
					while (PUsToSpend < 0 && buyThese > 0)
					{
						buyThese--;
						PUsToSpend += cost;
					}
					if (buyThese > 0)
						purchase.add(rule1, buyThese);
				}
				final int landPUs = PUsToSpend;
				buyLimit = landPUs / 3;
				bestAttack.clear();
				bestDefense.clear();
				bestMaxUnits.clear();
				bestMobileAttack.clear();
				SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, landProductionRules, landPUs, buyLimit, data, player, 2);
			}
			else
			{
				landPurchase = false;
				buyLimit = PUsToSpend / 8; // assume higher end purchase
				seaProductionRules.addAll(airProductionRules);
				if (Math.random() > 0.45)
					SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, seaProductionRules, PUsToSpend, buyLimit, data, player, 2);
				else
				{
					goTransports = true;
				}
			}
			final List<ProductionRule> processRules = new ArrayList<ProductionRule>();
			if (landPurchase)
				processRules.addAll(landProductionRules);
			else
			{
				if (goTransports)
					processRules.addAll(transportProductionRules);
				else
					processRules.addAll(seaProductionRules);
			}
			final boolean buyAttack = Math.random() > 0.25;
			int buyThese = 0, numBought = 0;
			for (final ProductionRule rule1 : processRules)
			{
				final int cost = rule1.getCosts().getInt(pus);
				if (goTransports)
					buyThese = PUsToSpend / cost;
				else if (buyAttack)
					buyThese = bestAttack.getInt(rule1);
				else if (Math.random() <= 0.25)
					buyThese = bestDefense.getInt(rule1);
				else
					buyThese = bestMaxUnits.getInt(rule1);
				PUsToSpend -= cost * buyThese;
				while (buyThese > 0 && PUsToSpend < 0)
				{
					buyThese--;
					PUsToSpend += cost;
				}
				if (buyThese > 0)
				{
					numBought += buyThese;
					purchase.add(rule1, buyThese);
				}
			}
			bestAttack.clear();
			bestDefense.clear();
			bestTransport.clear();
			bestMaxUnits.clear();
			bestMobileAttack.clear();
			if (PUsToSpend > 0) // verify a run through the land units
			{
				buyLimit = PUsToSpend / 2;
				SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, landProductionRules, PUsToSpend, buyLimit, data, player, 2);
				for (final ProductionRule rule2 : landProductionRules)
				{
					final int cost = rule2.getCosts().getInt(pus);
					buyThese = bestDefense.getInt(rule2);
					PUsToSpend -= cost * buyThese;
					while (buyThese > 0 && PUsToSpend < 0)
					{
						buyThese--;
						PUsToSpend += cost;
					}
					if (buyThese > 0)
						purchase.add(rule2, buyThese);
				}
			}
			purchaseDelegate.purchase(purchase);
			return;
		}
		pause();
		// s_logger.fine("Player: "+ player.getName()+"; PUs: "+PUsToSpend);
		final boolean tFirst = transportsMayDieFirst();
		boolean shipCapitalThreat = false;
		isAmphib = isAmphibAttack(player, false);
		final CompositeMatch<Unit> enemyUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data));
		final CompositeMatch<Unit> attackShip = new CompositeMatchAnd<Unit>(Matches.UnitIsNotTransport, Matches.UnitIsSea);
		final CompositeMatch<Unit> enemyAttackShip = new CompositeMatchAnd<Unit>(enemyUnit, attackShip);
		final CompositeMatch<Unit> enemyFighter = new CompositeMatchAnd<Unit>(enemyUnit, Matches.UnitCanLandOnCarrier);
		final CompositeMatch<Unit> ourAttackShip = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), attackShip);
		final CompositeMatch<Unit> alliedAttackShip = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), attackShip);
		final CompositeMatch<Unit> enemyTransport = new CompositeMatchAnd<Unit>(enemyUnit, Matches.UnitIsTransport);
		final CompositeMatch<Unit> ourFactories = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanProduceUnits);
		final CompositeMatch<Unit> transUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.unitIsOwnedBy(player));
		final CompositeMatch<Unit> fighter = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanLandOnCarrier);
		final CompositeMatch<Unit> alliedFighter = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitCanLandOnCarrier);
		final CompositeMatch<Unit> transportableUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBeTransported, Matches.UnitCanNotProduceUnits,
					Matches.UnitIsNotInfrastructure);
		final CompositeMatch<Unit> ACUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsCarrier);
		final CompositeMatch<Territory> enemyAndNoWater = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data),
					Matches.TerritoryIsNotImpassableToLandUnits(player, data));
		final CompositeMatch<Territory> noEnemyOrWater = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), Matches.TerritoryIsNotImpassableToLandUnits(player, data));
		final CompositeMatch<Territory> enemyOnWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data));
		final Territory myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		boolean factPurchased = false;
		final boolean isLand = SUtils.doesLandExistAt(myCapital, data, false); // gives different info than isamphib
		@SuppressWarnings("unused")
		boolean skipShips = false;
		boolean buyTransports = true;
		boolean buyPlanesOnly = false, buyOnePlane = false, buyBattleShip = false, buyOneShip = false, buyCarrier = false;
		// boolean buySub = false;
		final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
		List<Territory> factories = new ArrayList<Territory>();
		if (ra != null && ra.getPlacementAnyTerritory()) // make them all available for placing
		{
			factories = SUtils.allOurTerritories(data, player);
		}
		else
		{
			factories = SUtils.findUnitTerr(data, player, ourFactories);
		}
		final List<Territory> waterFactories = SUtils.stripLandLockedTerr(data, SUtils.findUnitTerr(data, player, ourFactories));
		final List<Territory> enemyAttackShipTerr = SUtils.findUnitTerr(data, player, enemyAttackShip);
		final List<Territory> ourAttackShipTerr = SUtils.findUnitTerr(data, player, alliedAttackShip);
		final List<Territory> enemyTransportTerr = SUtils.findUnitTerr(data, player, enemyTransport);
		int capUnitCount = myCapital.getUnits().countMatches(transportableUnit);
		final Set<Territory> capNeighbors = data.getMap().getNeighbors(myCapital, Matches.TerritoryIsWater);
		for (final Territory capN : capNeighbors)
			capUnitCount -= capN.getUnits().countMatches(transUnit) * 2;
		int EASCount = 0, OASCount = 0, ETTCount = 0;
		final int factoryCount = factories.size();
		final int totTransports = countTransports(data, player);
		final int totAttackSeaUnits = countSeaUnits(data, player);
		final int totLandUnits = countLandUnits(data, player);
		int totEAttackSeaUnits = 0;
		final List<PlayerID> enemyPlayers = SUtils.getEnemyPlayers(data, player);
		for (final PlayerID ePlayer : enemyPlayers)
			totEAttackSeaUnits += countSeaUnits(data, ePlayer);
		// boolean seaPlaneThreat = false;
		// float avgSeaThreat = 0.0F;
		// float ourLocalSeaProtection = 0.0F;
		int waterProduction = 0;
		final Iterator<Territory> wIter = waterFactories.iterator();
		while (wIter.hasNext())
		{
			final Territory wFact = wIter.next();
			waterProduction += TerritoryAttachment.get(wFact).getProduction();
		}
		// we don't have enough factories through which to launch attack
		if (isAmphib
					&& ((waterProduction < 6 && PUsToSpend > 26) || (waterProduction < 4 && PUsToSpend > 15) || (waterProduction < 10 && PUsToSpend > 70) || (waterProduction < 2) || (Math.random() < 0.33 && PUsToSpend > 250)))
		{
			// List<Territory> allMyTerrs = SUtils.allOurTerritories(data, player);
			final float risk = 0.0F;
			final Territory waterFact = SUtils.findFactoryTerritory(data, player, risk, true, true);
			if (waterFact != null)
			{
				waterProduction += TerritoryAttachment.get(waterFact).getProduction();// might want to buy 2
				for (final ProductionRule factoryRule : rules)
				{
					final int cost = factoryRule.getCosts().getInt(pus);
					final UnitType factoryType = (UnitType) factoryRule.getResults().keySet().iterator().next();
					if (Matches.UnitTypeCanProduceUnitsAndIsConstruction.match(factoryType))
					{
						if (PUsToSpend >= cost && !factPurchased)
						{
							setFactory(waterFact);
							purchase.add(factoryRule, 1);
							PUsToSpend -= cost;
							factPurchased = true;
						}
					}
				}
				if (factPurchased && (PUsToSpend < 16 || waterProduction <= 0))
					purchaseDelegate.purchase(purchase); // This is all we will purchase
				else if (factPurchased)
				{
					final double random = Math.random();
					SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, seaProductionRules, PUsToSpend, waterProduction, data, player, 0);
					for (final ProductionRule rule1 : seaProductionRules)
					{
						int buyThese = 0;
						if (random <= 0.40)
							buyThese = bestDefense.getInt(rule1);
						else
							buyThese = bestTransport.getInt(rule1);
						final int cost = rule1.getCosts().getInt(pus);
						PUsToSpend -= cost * buyThese;
						while (PUsToSpend < 0 && buyThese > 0)
						{
							buyThese--;
							PUsToSpend += cost;
						}
						if (buyThese <= 0)
							continue;
						purchase.add(rule1, buyThese);
						final UnitType rule1UT = (UnitType) rule1.getResults().keySet().iterator().next();
						if (Matches.UnitTypeCanTransport.match(rule1UT) && buyThese > 0)
							setDidPurchaseTransports(true);
					}
					purchaseDelegate.purchase(purchase);
				}
				now = System.currentTimeMillis();
				s_logger.finest("Time Taken " + (now - last));
				return;
			}
		}
		if (isAmphib && !waterFactories.isEmpty())
		{ // figure out how much protection we need
			// Territory safeTerr = null;
			final Territory closestEnemyCapitol = SUtils.closestEnemyCapital(myCapital, data, player); // find the closest factory to our cap
			if (closestEnemyCapitol != null)
			{
				final int capEDist = data.getMap().getDistance(myCapital, closestEnemyCapitol);
				Territory myClosestFactory = SUtils.closestToEnemyCapital(waterFactories, data, player, false); // this is probably our attack base
				if (myClosestFactory != null)
				{
					final int cFactEDist = data.getMap().getDistance(myClosestFactory, closestEnemyCapitol);
					if (cFactEDist >= capEDist) // make sure that we use the capitol if it is equidistance
						myClosestFactory = myCapital;
					s_logger.fine("Capital: " + myCapital + "; Closest Enemy Capitol: " + closestEnemyCapitol + "; Closest Factory: " + myClosestFactory);
					int distFromFactoryToECap = data.getMap().getDistance(closestEnemyCapitol, myClosestFactory);
					distFromFactoryToECap = Math.max(distFromFactoryToECap, 3);
					final List<Territory> cap3Neighbors = new ArrayList<Territory>(data.getMap().getNeighbors(myClosestFactory, distFromFactoryToECap));
					final Iterator<Territory> nIter = cap3Neighbors.iterator();
					while (nIter.hasNext())
					{
						final Territory thisTerr = nIter.next();
						if (Matches.TerritoryIsLand.match(thisTerr))
						{
							nIter.remove();
							continue;
						}
						final int distToFactory = data.getMap().getDistance(myClosestFactory, thisTerr);
						final int distToECap = data.getMap().getDistance(closestEnemyCapitol, thisTerr);
						if ((distToECap + distToFactory) > (distFromFactoryToECap + 2) && distToFactory > 1) // always include all factory neighbors
						{
							nIter.remove();
						}
					}
					final List<Unit> ourUnits = new ArrayList<Unit>();
					// int seaCapCount = cap3Neighbors.size();
					float totSeaThreat = 0.0F;
					for (final Territory seaCapTerr : cap3Neighbors)
					{
						ourUnits.addAll(seaCapTerr.getUnits().getMatches(alliedAttackShip));
						totSeaThreat += SUtils.getStrengthOfPotentialAttackers(seaCapTerr, data, player, tFirst, false, null);
					}
				}
			}
			// avgSeaThreat = totSeaThreat / seaCapCount;
			// ourLocalSeaProtection = SUtils.strength(ourUnits, false, true, tFirst);
		}
		// negative of this is that it assumes all ships in same general area
		// Brits and USA start with ships in two theaters
		for (final Territory EAST : enemyAttackShipTerr)
		{
			EASCount += EAST.getUnits().countMatches(enemyAttackShip);
			EASCount += EAST.getUnits().countMatches(enemyFighter);
		}
		for (final Territory OAST : ourAttackShipTerr)
		{
			OASCount += OAST.getUnits().countMatches(alliedAttackShip);
			OASCount += OAST.getUnits().countMatches(alliedFighter);
		}
		for (final Territory ETT : enemyTransportTerr)
			ETTCount += ETT.getUnits().countMatches(enemyTransport); // # of enemy transports
		boolean doBuyAttackShips = false;
		Territory factCheckTerr = myCapital;
		if (Matches.territoryHasWaterNeighbor(data).invert().match(myCapital))
		{// TODO: This is a weak way of looking at it...need to localize...the problem is a player in two theaters (USA, UK)
			if (EASCount > (OASCount + 2))
				doBuyAttackShips = true;
			if (EASCount > (OASCount * 3))
				buyPlanesOnly = true;
			final Iterator<Territory> wFIter = waterFactories.iterator();
			Territory factTerr = null;
			Territory lastGoodFactTerr = null;
			Territory newFactTerr = null;
			while (wFIter.hasNext() && factTerr == null)
			{
				newFactTerr = wFIter.next();
				if (TerritoryAttachment.get(newFactTerr).getProduction() > 2)
				{
					lastGoodFactTerr = newFactTerr;
					if ((wFIter.hasNext() && Math.random() >= 0.50) || !wFIter.hasNext())
						factTerr = newFactTerr;
				}
				if (wFIter.hasNext() && factTerr == null && lastGoodFactTerr != null)
					factTerr = lastGoodFactTerr;
			}
			factCheckTerr = (factTerr != null) ? factTerr : (!waterFactories.isEmpty()) ? waterFactories.iterator().next() : null;
		}
		float strength1 = 0.0F, strength2 = 0.0F, airPotential = 0.0F;
		if (factCheckTerr != null)
		{
			final Territory myCapWaterTerr = SUtils.findASeaTerritoryToPlaceOn(factCheckTerr, data, player, tFirst);
			if (myCapWaterTerr != null)
			{
				strength1 = SUtils.getStrengthOfPotentialAttackers(myCapWaterTerr, data, player, tFirst, false, null);
				strength2 = SUtils.getStrengthOfPotentialAttackers(myCapWaterTerr, data, player, tFirst, true, null);
				airPotential = strength1 - strength2;
			}
		}
		final List<Territory> myShipTerrs = SUtils.findOnlyMyShips(myCapital, data, player, alliedAttackShip);
		int shipCount = 0;
		for (final Territory shipT : myShipTerrs)
			shipCount += shipT.getUnits().countMatches(alliedAttackShip);
		int totPU = 0, totProd = 0, PUSea = 0, PULand = 0;
		float purchaseT;
		// String error = null;
		// boolean localShipThreat = false;
		int maxShipThreat = 0, currShipThreat = 0, minDistanceToEnemy = 1000;
		// Territory localShipThreatTerr = null;
		boolean nonCapitolFactoryThreat = false;
		final boolean seaAdvantageEnemy = ((tFirst ? totTransports : 0) * 10 + totAttackSeaUnits * 10) < (totEAttackSeaUnits * 9 + (tFirst ? ETTCount * 5 : 0));
		for (final Territory fT : factories)
		{
			final int thisFactProduction = TerritoryAttachment.get(fT).getProduction();
			totPU += thisFactProduction;
			totProd += TerritoryAttachment.get(fT).getUnitProduction();
			if (!useProductionData())
				totProd = totPU;
			if (isAmphib)
			{
				currShipThreat = SUtils.shipThreatToTerr(fT, data, player, tFirst);
				if ((currShipThreat > 3 && !seaAdvantageEnemy) || (currShipThreat > 2 && seaAdvantageEnemy)) // TODO: Emphasis is exclusively on capital: needs to be expanded to handle pacific Jap fleet
				{
					// localShipThreat = true;
					if (fT == myCapital)
					{
						setSeaTerr(myCapital);
						shipCapitalThreat = true;
					}
					if (currShipThreat > maxShipThreat)
					{
						maxShipThreat = currShipThreat;
						// localShipThreatTerr = fT;
					}
				}
			}
			else
			{
				// determine minimum ground distance to enemy
				final Route minDistRoute = SUtils.findNearest(fT, Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data),
							Matches.TerritoryIsNotImpassableToLandUnits(player, data), data);
				int thisMinDist = 1000;
				if (minDistRoute != null)
					thisMinDist = minDistRoute.getLength();
				minDistanceToEnemy = Math.min(thisMinDist, minDistanceToEnemy);
			}
			currShipThreat = 0;
			final float factThreat = SUtils.getStrengthOfPotentialAttackers(fT, data, player, tFirst, true, null);
			final float factStrength = SUtils.strength(fT.getUnits().getUnits(), false, false, tFirst);
			if (factThreat > factStrength)
				nonCapitolFactoryThreat = true;
		}
		// maximum # of units
		int unitCount = 0;
		int leftToSpend = PUsToSpend;
		totPU = leftToSpend;
		purchaseT = 1.00F;
		if (isAmphib)
			purchaseT = 0.50F;
		final List<Territory> ACTerrs = SUtils.ACTerritory(player, data);
		int ACCount = 0, fighterCount = 0;
		for (final Territory ACTerr : ACTerrs)
			ACCount += ACTerr.getUnits().countMatches(ACUnit);
		final List<Territory> fighterTerrs = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitCanLandOnCarrier);
		for (final Territory fighterTerr : fighterTerrs)
			fighterCount += fighterTerr.getUnits().countMatches(fighter);
		// If other factors allow, buy one plane
		if (ACCount > fighterCount)
			buyOnePlane = true;
		final Territory capitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		final Match<Unit> ourFactoriesThatCanBeDamaged = new CompositeMatchAnd<Unit>(ourFactories, Matches.UnitCanBeDamaged);
		final List<Territory> rfactories = Match.getMatches(SUtils.findUnitTerr(data, player, ourFactoriesThatCanBeDamaged), Matches.isTerritoryOwnedBy(player));
		List<RepairRule> rrules = Collections.emptyList();
		if (player.getRepairFrontier() != null && games.strategy.triplea.Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(data)) // figure out if anything needs to be repaired
		{
			rrules = player.getRepairFrontier().getRules();
			final IntegerMap<RepairRule> repairMap = new IntegerMap<RepairRule>();
			final HashMap<Unit, IntegerMap<RepairRule>> repair = new HashMap<Unit, IntegerMap<RepairRule>>();
			final Collection<Unit> unitsThatCanProduceNeedingRepair = new ArrayList<Unit>();
			final Collection<Unit> unitsThatAreDisabledNeedingRepair = new ArrayList<Unit>();
			final CompositeMatchAnd<Unit> ourDisabled = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsDisabled);
			final int minimumUnitPrice = 3;
			int diff = 0;
			int totalDamage = 0;
			int capDamage = 0;
			int capProduction = 0;
			Unit capUnit = null;
			int maxUnits = (totPU - 1) / minimumUnitPrice;
			int currentProduction = 0;
			int maxProduction = 0;
			Collections.shuffle(rfactories); // we should sort this
			for (final Territory fixTerr : rfactories)
			{
				if (!Matches.territoryIsOwnedAndHasOwnedUnitMatching(data, player, Matches.UnitCanProduceUnitsAndCanBeDamaged).match(fixTerr))
					continue;
				final Unit possibleFactoryNeedingRepair = TripleAUnit.getBiggestProducer(Match.getMatches(fixTerr.getUnits().getUnits(), ourFactoriesThatCanBeDamaged), fixTerr, player, data, false);
				if (Matches.UnitHasTakenSomeBombingUnitDamage.match(possibleFactoryNeedingRepair))
					unitsThatCanProduceNeedingRepair.add(possibleFactoryNeedingRepair);
				unitsThatAreDisabledNeedingRepair.addAll(Match.getMatches(fixTerr.getUnits().getUnits(), ourDisabled));
				final TripleAUnit taUnit = (TripleAUnit) possibleFactoryNeedingRepair;
				maxProduction += TripleAUnit.getHowMuchCanUnitProduce(possibleFactoryNeedingRepair, fixTerr, player, data, false, true);
				diff = taUnit.getUnitDamage();
				totalDamage += diff;
				if (fixTerr == capitol)
				{
					capDamage += diff;
					capProduction = TripleAUnit.getHowMuchCanUnitProduce(possibleFactoryNeedingRepair, fixTerr, player, data, true, true);
					capUnit = possibleFactoryNeedingRepair;
				}
				currentProduction += TripleAUnit.getHowMuchCanUnitProduce(possibleFactoryNeedingRepair, fixTerr, player, data, true, true);
			}
			rfactories.remove(capitol);
			unitsThatCanProduceNeedingRepair.remove(capUnit);
			// assume minimum unit price is 3, and that we are buying only that... if we over repair, oh well, that is better than under-repairing
			// goal is to be able to produce all our units, and at least half of that production in the capitol
			if ((capProduction <= maxUnits / 2 || rfactories.isEmpty()) && capUnit != null) // if capitol is super safe, we don't have to do this. and if capitol is under siege, we should repair enough to place all our units here
			{
				for (final RepairRule rrule : rrules)
				{
					/*if (capUnit == null)
						continue;*/
					if (!capUnit.getUnitType().equals(rrule.getResults().keySet().iterator().next()))
						continue;
					if (!Matches.territoryIsOwnedAndHasOwnedUnitMatching(data, player, Matches.UnitCanProduceUnitsAndCanBeDamaged).match(capitol))
						continue;
					final TripleAUnit taUnit = (TripleAUnit) capUnit;
					diff = taUnit.getUnitDamage();
					final int unitProductionAllowNegative = TripleAUnit.getHowMuchCanUnitProduce(capUnit, capUnit.getTerritoryUnitIsIn(), player, data, false, true) - diff;
					if (!rfactories.isEmpty())
						diff = Math.min(diff, (maxUnits / 2 - unitProductionAllowNegative) + 1);
					else
						diff = Math.min(diff, (maxUnits - unitProductionAllowNegative));
					diff = Math.min(diff, leftToSpend - minimumUnitPrice);
					if (diff > 0)
					{
						if (unitProductionAllowNegative >= 0)
							currentProduction += diff;
						else
							currentProduction += diff + unitProductionAllowNegative;
						repairMap.add(rrule, diff);
						repair.put(capUnit, repairMap);
						leftToSpend -= diff;
						purchaseDelegate.purchaseRepair(repair);
						repair.clear();
						repairMap.clear();
						maxUnits = (leftToSpend - 1) / minimumUnitPrice; // ideally we would adjust this after each single PU spent, then re-evaluate everything.
					}
				}
			}
			int i = 0;
			while (currentProduction < maxUnits && i < 2)
			{
				for (final RepairRule rrule : rrules)
				{
					for (final Unit fixUnit : unitsThatCanProduceNeedingRepair)
					{
						if (fixUnit == null || !fixUnit.getType().equals(rrule.getResults().keySet().iterator().next()))
							continue;
						if (!Matches.territoryIsOwnedAndHasOwnedUnitMatching(data, player, Matches.UnitCanProduceUnitsAndCanBeDamaged).match(fixUnit.getTerritoryUnitIsIn()))
							continue;
						// we will repair the first territories in the list as much as we can, until we fulfill the condition, then skip all other territories
						if (currentProduction >= maxUnits)
							continue;
						final TripleAUnit taUnit = (TripleAUnit) fixUnit;
						diff = taUnit.getUnitDamage();
						final int unitProductionAllowNegative = TripleAUnit.getHowMuchCanUnitProduce(fixUnit, fixUnit.getTerritoryUnitIsIn(), player, data, false, true) - diff;
						if (i == 0)
						{
							if (unitProductionAllowNegative < 0)
								diff = Math.min(diff, (maxUnits - currentProduction) - unitProductionAllowNegative);
							else
								diff = Math.min(diff, (maxUnits - currentProduction));
						}
						diff = Math.min(diff, leftToSpend - minimumUnitPrice);
						if (diff > 0)
						{
							if (unitProductionAllowNegative >= 0)
								currentProduction += diff;
							else
								currentProduction += diff + unitProductionAllowNegative;
							repairMap.add(rrule, diff);
							repair.put(fixUnit, repairMap);
							leftToSpend -= diff;
							purchaseDelegate.purchaseRepair(repair);
							repair.clear();
							repairMap.clear();
							maxUnits = (leftToSpend - 1) / minimumUnitPrice; // ideally we would adjust this after each single PU spent, then re-evaluate everything.
						}
					}
				}
				rfactories.add(capitol);
				if (capUnit != null)
					unitsThatCanProduceNeedingRepair.add(capUnit);
				i++;
			}
		}
		// determine current land risk to the capitol
		// float realSeaThreat = 0.0F;
		float realLandThreat = 0.0F;
		determineCapDanger(player, data);
		final StrengthEvaluator capStrEvalLand = StrengthEvaluator.evalStrengthAt(data, player, myCapital, true, true, tFirst, true);
		// boolean capDanger = capStrEvalLand.inDanger(0.85F);
		final boolean capDanger = getCapDanger();
		// s_logger.fine("Player: "+player.getName()+"; Capital Danger: "+capDanger);
		int fighterPresent = 0;
		if (capDanger && totProd > 0) // focus on Land Units and buy before any other decisions are made
		{
			landProductionRules.addAll(airProductionRules); // just in case we have a lot of PU
			SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, landProductionRules, leftToSpend, totProd, data, player, 0);
			for (final ProductionRule rule1 : landProductionRules)
			{
				int buyThese = bestDefense.getInt(rule1);
				final int cost = rule1.getCosts().getInt(pus);
				leftToSpend -= cost * buyThese;
				// s_logger.fine("Cap Danger"+"; Player: "+player.getName()+"Left To Spend: "+leftToSpend);
				while (leftToSpend < 0 && buyThese > 0)
				{
					buyThese--;
					leftToSpend += cost;
				}
				if (buyThese <= 0)
					continue;
				purchase.add(rule1, buyThese);
				unitCount += buyThese;
			}
			purchaseDelegate.purchase(purchase);
			now = System.currentTimeMillis();
			s_logger.finest("Time Taken " + (now - last));
			return;
		}
		float PUNeeded = 0.0F;
		// s_logger.fine("Ship Capital Threat: "+shipCapitalThreat+"; Max Ship Threat: "+maxShipThreat);
		if (shipCapitalThreat) // don't panic on a small advantage
		{
			if (games.strategy.triplea.Properties.getWW2V3(data)) // cheaper naval units
				PUNeeded = maxShipThreat * 5.5F;
			else
				PUNeeded = maxShipThreat * 6.5F;
		} // Every 10.0F advantage needs about 7 PU to stop (TODO: Build function for PU needed for ships)
		/*        else
		        { //force a transport purchase early in the game
		        }
		*/
		realLandThreat = capStrEvalLand.strengthMissing(0.85F);
		final boolean noCapitalThreat = capStrEvalLand.getEnemyStrengthInRange() < 0.50F;
		if ((totEAttackSeaUnits + 2) < totAttackSeaUnits) // override above if we have more on the map
			doBuyAttackShips = false;
		if (isAmphib && shipCapitalThreat && (noCapitalThreat || realLandThreat < -4.0F))
		{ // want to buy ships when we are overwhelmed
			if (Math.random() > 0.80)
				buyOnePlane = true;
			if (nonCapitolFactoryThreat && Math.random() <= 0.70)
			{
				buyPlanesOnly = true;
				doBuyAttackShips = false;
			}
			else
			{
				buyBattleShip = true;
				doBuyAttackShips = true;
			}
			if (!tFirst)
				buyTransports = false;
		}
		else if (!isAmphib && (noCapitalThreat))
		{
			if (Math.random() > 0.50)
			{
				buyOnePlane = true;
			}
			final Route dRoute = SUtils.findNearest(myCapital, enemyAndNoWater, noEnemyOrWater, data);
			if (shipCapitalThreat && dRoute.getLength() > 3)
				buyBattleShip = false;
		}
		else if (!isAmphib && !isLand && (realLandThreat < -8.0F || noCapitalThreat)) // Britain or Japan with mainland factories...don't let units pile up on capitol
		{
			if ((tFirst && maxShipThreat > 3) || (!tFirst && maxShipThreat > 2))
			{
				doBuyAttackShips = true;
				purchaseT = 0.15F;
				buyTransports = false;
			}
			else if (capUnitCount > 14) // units piling up on capital
			{
				buyTransports = true;
				skipShips = false;
				purchaseT = 0.25F;
			}
		}
		if (isAmphib && isLand)
		{
			purchaseT = 0.60F;
			if (capUnitCount > 14)
			{
				buyTransports = true;
				purchaseT = 0.25F; // By the way, Veqryn, the lower this purchaseT var is, the more sea units we will buy
			}
			if (maxShipThreat > 3 || (doBuyAttackShips && !tFirst))
			{
				buyTransports = false;
				purchaseT = 0.14F;
			}
			if (totTransports * 6 > totLandUnits) // we have plenty of transports
			{
				buyTransports = false;
				if (!doBuyAttackShips)
					skipShips = true;
				purchaseT = 1.00F;
			}
		}
		if (isAmphib && doBuyAttackShips && realLandThreat < 15.0F) // we are overwhelmed on sea
			purchaseT = 0.00F;
		else if (isAmphib && !isLand)
		{
			if (currentRound < 6 && Math.random() < 0.55F)
				buyOneShip = true; // will be overridden by doBuyAttackShips
			if (realLandThreat < 2.0F)
			{
				buyTransports = true;
				skipShips = false;
			}
			if (capUnitCount > 15 && realLandThreat < 0.0F) // units piling up on capital
			{
				purchaseT = 0.20F;
			}
			else if (capUnitCount > 12)
			{
				purchaseT = 0.49F;
			}
			else if (capUnitCount > 6)
			{
				purchaseT = 0.62F;
			}
			else if (totTransports * 3 > totLandUnits) // we have plenty of transports
			{
				buyTransports = false;
				if (!doBuyAttackShips)
					skipShips = true;
				purchaseT = 1.00F;
			}
			else
				purchaseT = 0.64F;
		}
		if (isAmphib && (PUNeeded > leftToSpend + 8) && Math.random() < 0.80) // they have major advantage, let's wait another turn
		{
			// Territory safeTerr = SUtils.getSafestWaterTerr(myCapital, null, null, data, player, false, tFirst);
			leftToSpend = Math.min(leftToSpend, (int) realLandThreat);
			purchaseT = 1.00F;
			buyTransports = false;
		}
		if (PUNeeded > 0.60F * leftToSpend)
		{
			buyTransports = false;
			if (isAmphib && (purchaseT != 0.00F && purchaseT != 1.00F))
			{
				if (realLandThreat <= 0.0F)
					purchaseT = 0.18F;
				else
					purchaseT = 0.54F;
				doBuyAttackShips = true;
			}
		}
		if (!isAmphib)
		{
			final boolean noWater = !SUtils.isWaterAt(myCapital, data);
			purchaseT = 0.82F;
			if (Math.random() < 0.88 || realLandThreat > 4.0F)
				purchaseT = 1.00F;
			if (noWater)
			{
				purchaseT = 1.00F;
				skipShips = true;
			}
		}
		final float fSpend = leftToSpend;
		PUSea = (int) (fSpend * (1.00F - purchaseT));
		PULand = leftToSpend - PUSea;
		// int minCost = Integer.MAX_VALUE;
		// Test for how badly we want transports
		// If we have a land route to enemy capital...forget about it (ie: we are amphib)
		// If we have land units close to us...forget about it
		// If we have a ton of units in our capital or territories connected to it, then let's buy transports
		if (isAmphib & !doBuyAttackShips)
		{
			int transportableUnitsUsage = 0;
			final List<Territory> myTerritories = SUtils.allAlliedTerritories(data, player);
			myTerritories.retainAll(Match.getMatches(myTerritories, Matches.territoryHasValidLandRouteTo(data, capitol)));
			for (final Territory xTerr : myTerritories)
			{
				if (!Matches.territoryHasEnemyLandNeighbor(data, player).match(xTerr))
				{
					final List<Unit> unitsOnTerr = new ArrayList<Unit>(xTerr.getUnits().getUnits());
					for (final Unit unit : unitsOnTerr)
					{
						if (!transportableUnit.match(unit))
							continue;
						final UnitAttachment ua = UnitAttachment.get(unit.getUnitType());
						if (ua == null || ua.getTransportCost() < 0)
							continue;
						transportableUnitsUsage += ua.getTransportCost();
					}
				}
			}
			int transportCapacityLeftForAllTransportsTogether = 0;
			final CompositeMatch<Unit> myAvailableTransports = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.transportIsNotTransporting(), Matches.unitIsOwnedBy(player));
			final List<Territory> myTransTerrs = SUtils.findTersWithUnitsMatching(data, player, myAvailableTransports);
			final List<Territory> waterFactoriesByCap = new ArrayList<Territory>(waterFactories);
			waterFactoriesByCap.retainAll(myTerritories);
			for (final Territory ter : myTransTerrs)
			{
				int closestWFact = 99999;
				// remove territories & transports that are more than 3 territories away from any owned factory that touches water
				for (final Territory wfact : waterFactoriesByCap)
				{
					final int dist = data.getMap().getDistance(wfact, ter, Matches.TerritoryIsLandOrWater);
					if (dist >= 0)
					{
						if (closestWFact > dist)
							closestWFact = dist;
					}
				}
				if (closestWFact > 3)
					continue;
				for (final Unit transport : ter.getUnits())
				{
					if (!myAvailableTransports.match(transport))
						continue;
					final UnitAttachment ua = UnitAttachment.get(transport.getUnitType());
					if (ua == null)
						continue;
					if (ua.getTransportCapacity() < 2) // we don't want to include frigates from Napoleonic in this list
						continue;
					final int transportCapacity = ua.getTransportCapacity();
					/*for (Unit unitOnTransport : TripleAUnit.get(transport).getTransporting())
					  {
					      UnitAttachment ua2 = UnitAttachment.get(unitOnTransport.getUnitType());
					      if (ua2 == null)
					          continue;
					      transportCapacity -= ua2.getTransportCost();
					  }*/
					transportCapacityLeftForAllTransportsTogether += transportCapacity;
				}
			}
			if (transportCapacityLeftForAllTransportsTogether > transportableUnitsUsage + 15) // roughly equal to > 2-3 full transports
			{
				skipShips = true;
				PULand = leftToSpend;
				PUSea = 0;
				buyTransports = false;
			}
			else if (transportCapacityLeftForAllTransportsTogether < transportableUnitsUsage)
			{
				buyTransports = true;
				skipShips = false;
				PULand = leftToSpend - 12; // enough for a single transport in great war (or a cruiser)
				PULand = Math.max(PULand, 0);
				PUSea = leftToSpend - PULand;
				// If we're at the point where all our transports don't even have space for the cap units (and we're amphi), time to start buying tons of transports
				if (transportCapacityLeftForAllTransportsTogether + 6 < transportableUnitsUsage)
				{
					PUSea = leftToSpend - 12; // We want to buy almost all transports
					PUSea = Math.max(PUSea, 12); // At least 12 for a great war transport
					PUSea = Math.max(PUSea, leftToSpend / 2); // At least half of PUs for transports
					PUSea = Math.max(PUSea, 0); // Never less than zero, this would cause errors
					PULand = leftToSpend - PUSea; // Set the land purchase amount to whatever is left
				}
				// If we're at the point where all our transports don't even have space for 1 half of the cap units(and we're amphi), time to start buying all transports
				if (transportCapacityLeftForAllTransportsTogether + 6 < transportableUnitsUsage / 2)
				{
					PUSea = leftToSpend; // We want to buy all transports
					PUSea = Math.max(PUSea, 0); // Never less than zero, this would cause errors
					PULand = leftToSpend - PUSea; // Set the land purchase amount to whatever is left(nothing)
				}
			}
		}
		// Purchase land units first
		/**
		 * Determine ships/planes within 6 territories/sea zones of capital and around the amphib route endpoint
		 */
		boolean removeSubs = false;
		if (isAmphib)
		{
			final CompositeMatch<Unit> ourAirUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir);
			final CompositeMatch<Unit> enemyAirUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsAir);
			final CompositeMatch<Unit> alliedAirUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsAir);
			final Set<Territory> myTerr = data.getMap().getNeighbors(myCapital, 6);
			final Route amphibRoute = getAmphibRoute(player, false);
			if (amphibRoute != null && amphibRoute.getEnd() != null)
			{
				final Territory amphibTerr = amphibRoute.getEnd();
				final Set<Territory> amphibTerrs = data.getMap().getNeighbors(amphibTerr, 3);
				myTerr.addAll(amphibTerrs);
			}
			final List<Unit> myShipsPlanes = new ArrayList<Unit>();
			final List<Unit> enemyShipsPlanes = new ArrayList<Unit>();
			final List<Unit> alliedShipsPlanes = new ArrayList<Unit>();
			int enemyShipCount = 0, enemyPlaneCount = 0;
			for (final Territory checkTerr : myTerr)
			{
				if (Matches.TerritoryIsWater.match(checkTerr))
				{// only count planes which are actually defending sea units
					myShipsPlanes.addAll(checkTerr.getUnits().getMatches(ourAttackShip));
					myShipsPlanes.addAll(checkTerr.getUnits().getMatches(ourAirUnit));
					alliedShipsPlanes.addAll(checkTerr.getUnits().getMatches(alliedAirUnit));
					alliedShipsPlanes.addAll(checkTerr.getUnits().getMatches(alliedAttackShip));
					final List<Unit> enemyShips = checkTerr.getUnits().getMatches(enemyAttackShip);
					enemyShipsPlanes.addAll(enemyShips);
					enemyShipCount += enemyShips.size();
					alliedShipsPlanes.removeAll(myShipsPlanes);
				}
				final List<Unit> enemyPlanes = checkTerr.getUnits().getMatches(enemyAirUnit);
				enemyShipsPlanes.addAll(enemyPlanes);
				enemyPlaneCount += enemyPlanes.size();
			}
			final int myTotUnits = myShipsPlanes.size() + alliedShipsPlanes.size() / 2;
			final int enemyTotUnits = enemyShipsPlanes.size();
			if (enemyTotUnits > 0 && myTotUnits < enemyTotUnits + 2)
			{
				doBuyAttackShips = true;
				PUSea = leftToSpend;
				PULand = 0;
				if (enemyPlaneCount > enemyShipCount)
					removeSubs = true;
			}
		}
		int landConstant = 2; // we want to loop twice to spread out our purchase
		boolean highPriceLandUnits = false;
		if (leftToSpend > 10 * (totProd) && !doBuyAttackShips) // if not buying ships, buy planes
		{
			if (Math.random() <= 0.85)
				buyPlanesOnly = true;
			buyCarrier = true;
			buyBattleShip = false;
		}
		else if (leftToSpend > 5 * (totProd - 1))
			buyOnePlane = true;
		boolean extraPUonPlanes = false;
		if (capDanger) // capital in trouble...purchase units accordingly...most expensive available
		{
			if (!isLand && !doBuyAttackShips) // try to balance the need for Naval units here
			{
				PULand = leftToSpend;
				PUSea = 0;
			}
			extraPUonPlanes = true;
			buyTransports = false;
		}
		highPriceLandUnits = (highPrice * totProd + 3) < PULand;
		boolean buyfactory = false;
		// boolean buyExtraLandUnits = true; // fix this later...we might want to save PUs
		final int maxPurch = leftToSpend / 3;
		if ((maxPurch > (totProd + 4) && !isAmphib) || maxPurch > (totProd + 12)) // more money than places to put units...buy more expensive units & a Factory
		{
			buyfactory = true;
			landConstant = 2;
			buyOnePlane = true;
			highPriceLandUnits = true;
		}
		if (realLandThreat <= 0.0F && !doBuyAttackShips && !buyTransports && !isAmphib && maxPurch > totProd + 2)
			highPriceLandUnits = true;
		if (landConstant != 2 || (highPriceLandUnits && PULand >= 35) || totProd == 0)
		{
			buyfactory = true;
			// int numFactory = 0;
		}
		if (isAmphib && !doBuyAttackShips && totTransports <= 15) // TODO: look at deleting this...12 is arbitrary
			buyTransports = true;
		if (highPriceLandUnits && (!isAmphib || (isAmphib && !buyTransports && !doBuyAttackShips)) && !buyPlanesOnly)
		{
			/*			int hpBuy = buyOnePlane ? (totProd - unitCount) - 1 : totProd;
						int buyThese = leftToSpend/highPrice;
						buyThese = Math.min(buyThese, hpBuy - unitCount);
						leftToSpend -= highPrice*buyThese;
						s_logger.fine("High Price Units"+"; Player: "+player.getName()+"Left To Spend: "+leftToSpend);
						while (leftToSpend < 0 && buyThese > 0)
						{
							buyThese--;
							leftToSpend+= highPrice;
						}
						if (buyThese > 0)
						{
							PULand -= highPrice*buyThese;
							unitCount += buyThese;
							purchase.add(highRule, buyThese);
						}
			*/
			landProductionRules.addAll(airProductionRules); // Try this...add planes into the mix and let the purchase routine handle
		}
		int maxBuy = (totProd - unitCount);
		maxBuy = (purchaseT < 0.70F) ? (maxBuy * 3) / 4 : (doBuyAttackShips ? 2 : 0);
		if (buyOnePlane)
			maxBuy--;
		if (isAmphib && !doBuyAttackShips)
		{
			final List<Territory> myFTerrs = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitCanLandOnCarrier);
			if (myFTerrs.isEmpty() && !buyOnePlane)
			{
				buyOnePlane = true;
				maxBuy--;
			}
			if (currentRound <= 4 && !buyOnePlane && Math.random() < 0.65)
			{
				buyOneShip = true;
				maxBuy++;
			}
		}
		final List<ProductionRule> newSeaProductionRules = new ArrayList<ProductionRule>(seaProductionRules);
		if (buyOneShip)
		{
			if (Math.random() > 0.25) // no subs 75% of the time if buying only 1 ship
			{
				final Iterator<ProductionRule> subRule = newSeaProductionRules.iterator();
				while (subRule.hasNext())
				{
					final ProductionRule checkRule = subRule.next();
					final UnitType x = (UnitType) checkRule.getResults().keySet().iterator().next();
					if (Matches.UnitTypeIsSub.match(x))
						subRule.remove();
					else if (Matches.UnitTypeIsSea.match(x) && Matches.unitTypeCanAttack(player).invert().match(x))
						subRule.remove(); // want to purchase an attacking unit
				}
			}
			ProductionRule maxRule = null;
			int maxCost = 0;
			if (Math.random() <= 0.5) // take out battleships 50% of the time
			{
				final Iterator<ProductionRule> BBRule = newSeaProductionRules.iterator();
				while (BBRule.hasNext())
				{
					final ProductionRule checkRule = BBRule.next();
					final UnitType x = (UnitType) checkRule.getResults().keySet().iterator().next();
					if (Matches.UnitTypeHasMoreThanOneHitPointTotal.match(x))
						BBRule.remove();
				}
			}
			for (final ProductionRule shipRule : newSeaProductionRules)
			{ // random purchase
				// UnitType x = (UnitType) shipRule.getResults().keySet().iterator().next();
				final int shipcost = shipRule.getCosts().getInt(pus);
				if (maxRule == null && shipcost < PUSea && (Math.random() < 0.20 || shipRule.equals(newSeaProductionRules.get(newSeaProductionRules.size() - 1))))
				{
					maxCost = shipcost;
					maxRule = shipRule;
				}
			}
			if (maxRule != null && maxCost <= leftToSpend && unitCount < totProd)
			{// buy as many as possible
				int buyThese = PUSea / maxCost;
				buyThese = (unitCount + buyThese) <= totProd ? buyThese : totProd - unitCount;
				leftToSpend -= maxCost * buyThese;
				if (leftToSpend < 0)
				{
					buyThese--;
					leftToSpend += maxCost;
				}
				if (!doBuyAttackShips)
				{
					PUSea = 0;
					PULand = leftToSpend;
				}
				if (buyThese > 0)
				{
					purchase.add(maxRule, buyThese);
					unitCount += buyThese;
					final UnitType maxRuleUT = (UnitType) maxRule.getResults().keySet().iterator().next();
					if (Matches.UnitTypeCanTransport.match(maxRuleUT) && buyThese > 0)
						setDidPurchaseTransports(true);
				}
			}
		}
		// s_logger.fine("Player: "+player.getName()+"; Is Amphib: "+isAmphib+"; IsLand: "+isLand+"; DoBuyAttackShips: "+doBuyAttackShips+"; Buy Transports: "+buyTransports);
		// s_logger.fine("PUs: "+leftToSpend+"; PU Land: "+PULand+"; PU Sea: "+PUSea+"; TotProduction: "+totProd+"; Current Unit Count: "+unitCount);
		if (PUSea > 0 && (doBuyAttackShips || buyBattleShip) && maxBuy > 0 && unitCount < totProd) // attack oriented sea units
		{
			if (isAmphib && !capDanger && maxShipThreat > 2)
				PUSea = leftToSpend;
			if (unitCount < 2)
				setAttackShipPurchase(true);
			fighterPresent = myCapital.getUnits().countMatches(Matches.UnitCanLandOnCarrier);
			if (PUSea > 0)
			{
				int buyThese = 0;
				int AttackType = 1; // bestAttack
				if (Math.random() <= 0.45) // for ships, focus on defense set most of the time
					AttackType = 2;
				if (Math.random() >= 0.65 && factoryCount == 1 && PUNeeded > 0.75 * leftToSpend) // 50% maxUnits when need a lot of ships
					AttackType = 3;
				final Route eShipRoute = SUtils.findNearest(myCapital, enemyOnWater, Matches.TerritoryIsWater, data);
				int enemyShipDistance = 0;
				if (eShipRoute != null)
					enemyShipDistance = eShipRoute.getLength();
				if (enemyShipDistance > 3)
					AttackType = 5;
				if (buyBattleShip)
				{
					for (final ProductionRule BBRule : seaProductionRules)
					{
						final UnitType results = (UnitType) BBRule.getResults().keySet().iterator().next();
						if (Matches.UnitTypeHasMoreThanOneHitPointTotal.match(results))
						{
							final int BBcost = BBRule.getCosts().getInt(pus);
							if (leftToSpend >= BBcost)
							{
								unitCount++;
								PUSea -= BBcost;
								leftToSpend -= BBcost;
								purchase.add(BBRule, 1);
								maxBuy--;
							}
						}
					}
				}
				if (buyCarrier)
				{
					boolean carrierBought = false;
					for (final ProductionRule CarrierRule : seaProductionRules)
					{
						final UnitType results = (UnitType) CarrierRule.getResults().keySet().iterator().next();
						if (Matches.UnitTypeIsCarrier.match(results))
						{
							final int Carriercost = CarrierRule.getCosts().getInt(pus);
							if (leftToSpend >= Carriercost)
							{
								unitCount++;
								PUSea -= Carriercost;
								leftToSpend -= Carriercost;
								purchase.add(CarrierRule, 1);
								maxBuy--;
								carrierBought = true;
							}
						}
					}
					if (carrierBought && leftToSpend > 0 && unitCount < totProd && fighterRule != null)
					{
						boolean fighterBought = false;
						// UnitType results = (UnitType) fighterRule.getResults().keySet().iterator().next();
						if (!fighterBought)
						{
							final int fighterCost = fighterRule.getCosts().getInt(pus);
							if (leftToSpend >= fighterCost)
							{
								unitCount++;
								PUSea -= fighterCost;
								leftToSpend -= fighterCost;
								purchase.add(fighterRule, 1);
								maxBuy--;
								fighterBought = true;
							}
						}
					}
				}
				if (PUSea > 0)
				{
					if (removeSubs)
					{
						final Iterator<ProductionRule> sPIter = seaProductionRules.iterator();
						while (sPIter.hasNext())
						{
							final ProductionRule shipRule = sPIter.next();
							final UnitType subUnit = (UnitType) shipRule.getResults().keySet().iterator().next();
							if (Matches.UnitTypeIsSub.match(subUnit))
								sPIter.remove();
						}
					}
					SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, seaProductionRules, PUSea, maxBuy, data, player, fighterPresent);
					for (final ProductionRule rule1 : seaProductionRules)
					{
						switch (AttackType)
						{
							case 1:
								buyThese = bestAttack.getInt(rule1);
								break;
							case 2:
								buyThese = bestDefense.getInt(rule1);
								break;
							case 3:
								buyThese = bestMaxUnits.getInt(rule1);
								break;
							case 5:
								buyThese = bestMobileAttack.getInt(rule1);
								break;
						}
						final int cost = rule1.getCosts().getInt(pus);
						int numToBuy = 0;
						final UnitType results = (UnitType) rule1.getResults().keySet().iterator().next();
						while (unitCount < totProd && leftToSpend >= cost && PUSea >= cost && numToBuy < buyThese)
						{
							unitCount++;
							leftToSpend -= cost;
							PUSea -= cost;
							numToBuy++;
							if (Matches.UnitTypeIsCarrier.match(results) && fighterRule != null) // attempt to add a fighter to every carrier purchased
							{
								boolean fighterBought = false;
								if (!fighterBought)
								{
									final int fighterCost = fighterRule.getCosts().getInt(pus);
									if (leftToSpend >= fighterCost && unitCount < totProd)
									{
										unitCount++;
										PUSea -= fighterCost;
										leftToSpend -= fighterCost;
										purchase.add(fighterRule, 1);
										fighterBought = true;
									}
								}
							}
						}
						purchase.add(rule1, numToBuy);
						if (Matches.UnitTypeCanTransport.match(results) && numToBuy > 0)
							setDidPurchaseTransports(true);
					}
				}
			}
			bestAttack.clear();
			bestDefense.clear();
			bestTransport.clear();
			bestMaxUnits.clear();
		}
		if (leftToSpend >= 15) // determine factory first to make sure enough PU...doesn't count toward units
		{
			int numFactory = 0;
			for (final Territory fT2 : factories)
			{
				if (SUtils.hasLandRouteToEnemyOwnedCapitol(fT2, player, data))
					numFactory++;
				if (!SUtils.doesLandExistAt(fT2, data, false))
					continue;
				final List<Territory> enemyFactoriesInRange = new ArrayList<Territory>(data.getMap().getNeighbors(fT2, 3));
				final Iterator<Territory> eFIter = enemyFactoriesInRange.iterator();
				while (eFIter.hasNext())
				{// count enemy factory which is close enough to take
					final Territory factTerr = eFIter.next();
					if (Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits).invert().match(factTerr)
								|| data.getMap().getLandRoute(fT2, factTerr) == null)
						eFIter.remove();
				}
				numFactory += enemyFactoriesInRange.size();
			}
			if ((numFactory >= 2) && (leftToSpend < 60)) // some maps have a lot of cheap territories, may need more factories (veqryn)
				buyfactory = false; // allow 2 factories on the same continent
			if ((numFactory >= 4) && (leftToSpend < 120)) // some maps have a lot of money, may need more factories (veqryn)
				buyfactory = false; // allow 4 factories on the same continent
			if (!buyfactory)
			{
				int minDistToEnemy = 100;
				for (final Territory fT3 : factories) // what is the minimum distance to the enemy?
				{
					final Route landDistRoute = SUtils.findNearest(fT3, Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(player, data), Matches.TerritoryIsNotImpassable, data);
					if (landDistRoute != null && landDistRoute.getLength() < minDistToEnemy)
						minDistToEnemy = landDistRoute.getLength();
				}
				if (minDistToEnemy > 5) // even if a lot of factories...build a factory closer to enemy
					buyfactory = true;
			}
			if (((maxPurch > (totProd + 12)) && highPriceLandUnits && PULand > 70 && PULand > 7 * totProd) || totProd == 0) // stinking rich, who cares if many factories are close by
				buyfactory = true;
			/*
			 * Watch out for having a good distance to enemy, but laying factories back at your base
			 * Goal is to get Germany building factories as the invasion of Russia takes place in NWO
			 */
			if (buyfactory)
			{
				for (final ProductionRule factoryRule : rules)
				{
					final int cost = factoryRule.getCosts().getInt(pus);
					final UnitType factoryType = (UnitType) factoryRule.getResults().keySet().iterator().next();
					if (Matches.UnitTypeCanProduceUnitsAndIsConstruction.match(factoryType))
					{
						if (leftToSpend >= cost && !factPurchased)
						{
							final float riskFactor = 1.0F;
							Territory factTerr = SUtils.findFactoryTerritory(data, player, riskFactor, buyfactory, false);
							// AI will attempt to buy a factory if it has no production. Will attempt both ways of purchasing. Will fail if it has no territories of value >=2 and touching water OR no territories that are completely surrounded by friendly territories
							if (factTerr == null && totProd <= 0)
								factTerr = SUtils.findFactoryTerritory(data, player, riskFactor, buyfactory, true);
							if (factTerr != null)
							{
								setFactory(factTerr);
								purchase.add(factoryRule, 1);
								leftToSpend -= cost;
								PULand -= cost;
								factPurchased = true;
								if (PULand < 0)
									PUSea = leftToSpend;
							}
						}
					}
				}
			}
		} // done buying factories...only buy 1
		maxBuy = (totProd - unitCount);
		maxBuy = (purchaseT > 0.25) ? maxBuy / 2 : maxBuy;
		PUSea = Math.min(PUSea, leftToSpend - PULand);
		if (buyTransports && maxBuy > 0 && !transportProductionRules.isEmpty())
		{ // assume a single transport rule
			final ProductionRule tRule = transportProductionRules.get(0);
			int cost = tRule.getCosts().getInt(pus);
			int numTrans = leftToSpend / cost;
			numTrans = Math.min(numTrans, maxBuy);
			int numToBuy = 0;
			while (unitCount < totProd && leftToSpend >= cost && PUSea >= cost && numToBuy < numTrans)
			{
				unitCount++;
				leftToSpend -= cost;
				PUSea -= cost;
				numToBuy++;
			}
			if (airPotential > 1.0F && shipCount <= 2)
			{ // exchange a transport for a destroyer
				numToBuy--;
				leftToSpend += cost;
				PUSea += cost;
				unitCount--;
				for (final ProductionRule destroyerRule : seaProductionRules)
				{
					final UnitType d = (UnitType) destroyerRule.getResults().keySet().iterator().next();
					if (Matches.UnitTypeIsDestroyer.match(d))
					{
						cost = destroyerRule.getCosts().getInt(pus);
						while (cost >= leftToSpend && unitCount < totProd)
						{
							purchase.add(destroyerRule, 1);
							leftToSpend -= cost;
							PUSea -= cost;
							unitCount++;
						}
					}
				}
			}
			purchase.add(tRule, numToBuy);
			final UnitType tRuleUT = (UnitType) tRule.getResults().keySet().iterator().next();
			if (Matches.UnitTypeCanTransport.match(tRuleUT) && numToBuy > 0)
				setDidPurchaseTransports(true);
		}
		maxBuy = totProd - unitCount;
		maxBuy = buyOnePlane ? (maxBuy - 1) : maxBuy;
		bestAttack.clear();
		bestDefense.clear();
		bestTransport.clear();
		bestMaxUnits.clear();
		bestMobileAttack.clear();
		if (!buyPlanesOnly && maxBuy > 0) // attack oriented land units
		{
			SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, landProductionRules, PULand, maxBuy, data, player, fighterPresent);
			int buyThese = 0;
			int AttackType = 1; // bestAttack
			if (Math.random() <= 0.65 || (isLand && Math.random() > 0.80)) // just to switch it up...every once in a while, buy the defensive set
				AttackType = 2;
			if ((Math.random() >= 0.25 && factoryCount >= 2) || (nonCapitolFactoryThreat && Math.random() < 0.75)) // if we have a lot of factories, use the max Unit set most of the time
				AttackType = 3;
			if ((isAmphib && Math.random() < 0.90) || Math.random() < 0.25)
			{
				if (bestTransport.totalValues() + 3 >= bestMaxUnits.totalValues() && bestTransport.totalValues() > 0) // Attack type 4 returns stupid results, like buy a single tank when you have 60 PUs to spend, so for now we are going to limit it with this
					AttackType = 4;
				else
					AttackType = 3;
			}
			if ((!isAmphib && minDistanceToEnemy >= 4 && Math.random() >= 0.10) || (minDistanceToEnemy >= 2 && Math.random() >= 0.85))
				AttackType = 5;
			// String attackString = AttackType == 1 ? "Best Attack" : AttackType == 2 ? "Best Defense" : AttackType == 3 ? "Best Max Units" : AttackType == 4 ? "Best Transport" : "Best Mobile";
			for (final ProductionRule rule1 : landProductionRules)
			{
				switch (AttackType)
				{
					case 1:
						buyThese = bestAttack.getInt(rule1);
						break;
					case 2:
						buyThese = bestDefense.getInt(rule1);
						break;
					case 3:
						buyThese = bestMaxUnits.getInt(rule1);
						break;
					case 4:
						buyThese = bestTransport.getInt(rule1);
						break;
					case 5:
						buyThese = bestMobileAttack.getInt(rule1);
				}
				final int cost = rule1.getCosts().getInt(pus);
				int numToBuy = 0;
				while (unitCount < totProd && leftToSpend >= 0 && leftToSpend >= cost && PULand >= cost && numToBuy < buyThese)
				{
					unitCount++;
					leftToSpend -= cost;
					PULand -= cost;
					numToBuy++;
				}
				purchase.add(rule1, numToBuy);
			}
			bestAttack.clear();
			bestDefense.clear();
			bestTransport.clear();
			bestMaxUnits.clear();
			bestMobileAttack.clear();
		}
		maxBuy = totProd - unitCount;
		if (((buyPlanesOnly || buyOnePlane) && maxBuy > 0) && Math.random() < 0.60)
		{
			maxBuy = (buyOnePlane && !buyPlanesOnly) ? 1 : maxBuy;
			SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, airProductionRules, leftToSpend, maxBuy, data, player, fighterPresent);
			int buyThese = 0;
			int AttackType = 1; // bestAttack
			if (Math.random() <= 0.50)
				AttackType = 2;
			if (Math.random() >= 0.50 && factoryCount > 2)
				AttackType = 3;
			else if (Math.random() > 0.25)
				AttackType = 5;
			for (final ProductionRule rule1 : airProductionRules)
			{
				switch (AttackType)
				{
					case 1:
						buyThese = bestAttack.getInt(rule1);
						break;
					case 2:
						buyThese = bestDefense.getInt(rule1);
						break;
					case 3:
						buyThese = bestMaxUnits.getInt(rule1);
						break;
					case 5:
						buyThese = bestMobileAttack.getInt(rule1);
						break;
				}
				final int cost = rule1.getCosts().getInt(pus);
				int numToBuy = 0;
				while (unitCount < totProd && leftToSpend >= 0 && leftToSpend >= cost && numToBuy < buyThese)
				{
					unitCount++;
					leftToSpend -= cost;
					PULand -= cost;
					numToBuy++;
				}
				purchase.add(rule1, numToBuy);
			}
			bestAttack.clear();
			bestDefense.clear();
			bestTransport.clear();
			bestMaxUnits.clear();
			bestMobileAttack.clear();
		}
		if (isAmphib)
		{
			PUSea = leftToSpend; // go ahead and make them available TODO: make sure that it is worth buying a transport
			if (!transportProductionRules.isEmpty())
			{
				final ProductionRule transRule = transportProductionRules.get(0);
				final int cost = transRule.getCosts().getInt(pus);
				maxBuy = leftToSpend / cost;
				maxBuy = Math.max(1, maxBuy - 1);
				if (cost * maxBuy <= leftToSpend)
				{
					purchase.add(transRule, maxBuy);
					leftToSpend -= cost * maxBuy;
					final UnitType transRuleUT = (UnitType) transRule.getResults().keySet().iterator().next();
					if (Matches.UnitTypeCanTransport.match(transRuleUT) && maxBuy > 0)
						setDidPurchaseTransports(true);
				}
			}
		}
		bestAttack.clear();
		bestDefense.clear();
		bestTransport.clear();
		bestMaxUnits.clear();
		bestMobileAttack.clear();
		if (leftToSpend > 0 && (unitCount < totProd) && extraPUonPlanes)
		{
			for (final ProductionRule planeProd : rules)
			{
				final int planeCost = planeProd.getCosts().getInt(pus);
				if (leftToSpend < planeCost || unitCount >= totProd)
					continue;
				final UnitType plane = (UnitType) planeProd.getResults().keySet().iterator().next();
				if (Matches.UnitTypeIsAir.match(plane))
				{
					if (capDanger && !Matches.unitTypeCanBombard(player).match(plane)) // buy best defensive plane
					{
						final int maxPlanes = totProd - unitCount;
						final int costPlanes = leftToSpend / planeCost;
						int buyThese = Math.min(maxPlanes, costPlanes);
						leftToSpend -= maxPlanes * planeCost;
						// s_logger.fine("Extra Air"+"; Player: "+player.getName()+"Left To Spend: "+leftToSpend);
						while (leftToSpend < 0 && buyThese > 0)
						{
							buyThese--;
							leftToSpend += planeCost;
						}
						if (buyThese > 0)
							purchase.add(planeProd, buyThese);
					}
					else
					{
						leftToSpend -= planeCost;
						if (leftToSpend > 0)
						{
							purchase.add(planeProd, 1);
							unitCount++;
						}
					}
				}
			}
		}
		bestAttack.clear();
		bestDefense.clear();
		bestTransport.clear();
		bestMaxUnits.clear();
		bestMobileAttack.clear();
		if (unitCount < totProd && leftToSpend > 2) // attack oriented land units
		{
			SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, landProductionRules, PULand, maxBuy, data, player, fighterPresent);
			int buyThese = 0;
			int AttackType = 1; // bestAttack
			if (Math.random() <= 0.65 || (isLand && Math.random() > 0.80)) // just to switch it up...every once in a while, buy the defensive set
				AttackType = 2;
			if ((Math.random() >= 0.25 && factoryCount >= 2) || (nonCapitolFactoryThreat && Math.random() < 0.75)) // if we have a lot of factories, use the max Unit set most of the time
				AttackType = 3;
			if ((isAmphib && Math.random() < 0.90) || Math.random() < 0.25)
			{
				if (bestTransport.totalValues() + 3 >= bestMaxUnits.totalValues() && bestTransport.totalValues() > 0) // Attack type 4 returns stupid results, like buy a single tank when you have 60 PUs to spend, so for now we are going to limit it with this
					AttackType = 4;
				else
					AttackType = 3;
			}
			if ((!isAmphib && minDistanceToEnemy >= 4 && Math.random() >= 0.10) || (minDistanceToEnemy >= 2 && Math.random() >= 0.85))
				AttackType = 5;
			// String attackString = AttackType == 1 ? "Best Attack" : AttackType == 2 ? "Best Defense" : AttackType == 3 ? "Best Max Units" : AttackType == 4 ? "Best Transport" : "Best Mobile";
			for (final ProductionRule rule1 : landProductionRules)
			{
				switch (AttackType)
				{
					case 1:
						buyThese = bestAttack.getInt(rule1);
						break;
					case 2:
						buyThese = bestDefense.getInt(rule1);
						break;
					case 3:
						buyThese = bestMaxUnits.getInt(rule1);
						break;
					case 4:
						buyThese = bestTransport.getInt(rule1);
						break;
					case 5:
						buyThese = bestMobileAttack.getInt(rule1);
				}
				final int cost = rule1.getCosts().getInt(pus);
				int numToBuy = 0;
				while (unitCount < totProd && leftToSpend >= 0 && leftToSpend >= cost && PULand >= cost && numToBuy < buyThese)
				{
					unitCount++;
					leftToSpend -= cost;
					PULand -= cost;
					numToBuy++;
				}
				purchase.add(rule1, numToBuy);
			}
		}
		// in case we exited from the loop before finishing purchase, this will purchase the first unit in land production quickly (do not remove this)
		if ((unitCount < totProd) && leftToSpend > 0)
		{
			for (final ProductionRule quickProd : rules)
			{
				final int quickCost = quickProd.getCosts().getInt(pus);
				if (leftToSpend < quickCost || unitCount >= totProd || quickCost < 1)
					continue;
				final UnitType intResults = (UnitType) quickProd.getResults().keySet().iterator().next();
				if (Matches.UnitTypeIsSeaOrAir.match(intResults) || Matches.UnitTypeIsInfrastructure.match(intResults) || Matches.UnitTypeIsAAforAnything.match(intResults))
					continue;
				if (quickCost <= leftToSpend && unitCount < totProd)
				{
					final int purchaseNum = totProd - unitCount;
					final int numLand = (leftToSpend / quickCost);
					int actualPNum = Math.min(purchaseNum, numLand);
					leftToSpend -= quickCost * actualPNum;
					while (leftToSpend < 0 && actualPNum > 0)
					{
						actualPNum--;
						leftToSpend += quickCost;
					}
					if (actualPNum > 0)
					{
						purchase.add(quickProd, actualPNum);
						unitCount += actualPNum;
					}
				}
			}
		}
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
		purchaseDelegate.purchase(purchase);
	}
	
	@Override
	protected void place(final boolean bid, final IAbstractPlaceDelegate placeDelegate, final GameData data, final PlayerID player)
	{
		// if we have purchased a factory, it will be a priority for placing units
		// should place most expensive on it
		// need to be able to handle AA purchase
		long now, last;
		last = System.currentTimeMillis();
		s_logger.fine("Doing Placement ");
		if (player.getUnits().isEmpty())
			return;
		setImpassableTerrs(player);
		final Collection<Territory> impassableTerrs = getImpassableTerrs();
		final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
		final boolean tFirst = transportsMayDieFirst();
		final CompositeMatch<Unit> ownedUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
		final CompositeMatch<Unit> attackUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.UnitIsNotTransport);
		final CompositeMatch<Unit> transUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport);
		final CompositeMatch<Unit> enemyUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data));
		final CompositeMatch<Unit> enemyAttackUnit = new CompositeMatchAnd<Unit>(attackUnit, enemyUnit);
		// CompositeMatch<Unit> enemyTransUnit = new CompositeMatchAnd<Unit>(transUnit, enemyUnit);
		final CompositeMatch<Unit> ourFactory = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitCanProduceUnits);
		final CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsLand, Matches.UnitIsNotInfrastructure, Matches.UnitCanNotProduceUnits);
		// CompositeMatch<Territory> ourLandTerr = new CompositeMatchAnd<Territory>(Matches.isTerritoryOwnedBy(player), Matches.TerritoryIsLand);
		final Territory capitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		final List<Territory> factoryTerritories = Match.getMatches(SUtils.findUnitTerr(data, player, ourFactory), Matches.isTerritoryOwnedBy(player));
		factoryTerritories.removeAll(impassableTerrs);
		/**
		 * Bid place with following criteria:
		 * 1) Has an enemy Neighbor
		 * 2) Has the largest combination value:
		 * a) enemy Terr
		 * b) our Terr
		 * c) other Terr neighbors to our Terr
		 * d) + 2 for each of these which are victory cities
		 */
		if (bid)
		{
			final List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
			final List<Territory> ourEnemyTerr = new ArrayList<Territory>();
			final List<Territory> ourSemiRankedBidTerrs = new ArrayList<Territory>();
			final List<Territory> ourTerrs = SUtils.allOurTerritories(data, player);
			ourTerrs.remove(capitol); // we'll check the cap last
			final HashMap<Territory, Float> rankMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, tFirst, false, true);
			final List<Territory> ourTerrWithEnemyNeighbors = SUtils.getTerritoriesWithEnemyNeighbor(data, player, false, false);
			SUtils.reorder(ourTerrWithEnemyNeighbors, rankMap, true);
			// ourFriendlyTerr.retainAll(ourTerrs);
			if (ourTerrWithEnemyNeighbors.contains(capitol))
			{
				ourTerrWithEnemyNeighbors.remove(capitol);
				ourTerrWithEnemyNeighbors.add(capitol); // move capitol to the end of the list, if it is touching enemies
			}
			Territory bidLandTerr = null;
			if (ourTerrWithEnemyNeighbors.size() > 0)
				bidLandTerr = ourTerrWithEnemyNeighbors.get(0);
			if (bidLandTerr == null)
				bidLandTerr = capitol;
			if (player.getUnits().someMatch(Matches.UnitIsSea))
			{
				Territory bidSeaTerr = null, bidTransTerr = null;
				// CompositeMatch<Territory> enemyWaterTerr = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data));
				final CompositeMatch<Territory> waterFactoryWaterTerr = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasOwnedNeighborWithOwnedUnitMatching(data, player,
							Matches.UnitCanProduceUnits));
				final List<Territory> enemySeaTerr = SUtils.findUnitTerr(data, player, enemyAttackUnit);
				final List<Territory> isWaterTerr = SUtils.onlyWaterTerr(data, enemySeaTerr);
				enemySeaTerr.retainAll(isWaterTerr);
				Territory maxEnemySeaTerr = null;
				int maxUnits = 0;
				for (final Territory seaTerr : enemySeaTerr)
				{
					final int unitCount = seaTerr.getUnits().countMatches(enemyAttackUnit);
					if (unitCount > maxUnits)
					{
						maxUnits = unitCount;
						maxEnemySeaTerr = seaTerr;
					}
				}
				final Route seaRoute = SUtils.findNearest(maxEnemySeaTerr, waterFactoryWaterTerr, Matches.TerritoryIsWater, data);
				if (seaRoute != null)
				{
					final Territory checkSeaTerr = seaRoute.getEnd();
					if (checkSeaTerr != null)
					{
						final float seaStrength = SUtils.getStrengthOfPotentialAttackers(checkSeaTerr, data, player, tFirst, false, null);
						final float aStrength = SUtils.strength(checkSeaTerr.getUnits().getUnits(), false, true, tFirst);
						final float bStrength = SUtils.strength(player.getUnits().getMatches(attackUnit), false, true, tFirst);
						final float totStrength = aStrength + bStrength;
						if (totStrength > 0.9F * seaStrength)
							bidSeaTerr = checkSeaTerr;
					}
				}
				for (final Territory factCheck : factoryTerritories)
				{
					if (bidSeaTerr == null)
						bidSeaTerr = SUtils.findASeaTerritoryToPlaceOn(factCheck, data, player, tFirst);
					if (bidTransTerr == null)
						bidTransTerr = SUtils.findASeaTerritoryToPlaceOn(factCheck, data, player, tFirst);
				}
				placeSeaUnits(bid, data, bidSeaTerr, bidSeaTerr, placeDelegate, player);
			}
			if (player.getUnits().someMatch(Matches.UnitIsNotSea)) // TODO: Match fighters with carrier purchase
			{
				ourSemiRankedBidTerrs.addAll(ourTerrWithEnemyNeighbors);
				ourTerrs.removeAll(ourTerrWithEnemyNeighbors);
				Collections.shuffle(ourTerrs);
				ourSemiRankedBidTerrs.addAll(ourTerrs);
				// need to remove places like greenland, iceland and west indies that have no route to the enemy, but somehow keep places like borneo, gibralter, etc.
				for (final Territory noRouteTerr : ourTerrs)
				{
					// do not place bids on areas that have no direct land access to an enemy, unless the value is 3 or greater
					if (SUtils.distanceToEnemy(noRouteTerr, data, player, false) < 1 && TerritoryAttachment.get(noRouteTerr).getProduction() < 3)
					{
						ourSemiRankedBidTerrs.remove(noRouteTerr);
					}
				}
				/* Currently the place delegate does not accept bids by the AI to territories that it does not own. If that gets fixed we can add the following code in order to bid to allied territories that contain our units (like Libya in ww2v3) (veqryn)
				for(Territory alliedTerr : ourFriendlyTerr)
				{
				    if(!Matches.isTerritoryOwnedBy(player).match(alliedTerr) && alliedTerr.getUnits().getMatches(Matches.unitIsOwnedBy(player)).size() > 0)
				    {
				    	ourSemiRankedBidTerrs.add(alliedTerr);
				    }
				}
				*/
				final List<Territory> isWaterTerr = SUtils.onlyWaterTerr(data, ourSemiRankedBidTerrs);
				ourSemiRankedBidTerrs.removeAll(isWaterTerr);
				ourSemiRankedBidTerrs.removeAll(impassableTerrs);
				// This will bid a max of 5 units to ALL territories except for the capitol. The capitol gets units last, and gets unlimited units (veqryn)
				final int maxBidPerTerritory = 5;
				int bidCycle = 0;
				while (!(player.getUnits().isEmpty()) && bidCycle < maxBidPerTerritory)
				{
					for (int i = 0; i <= ourSemiRankedBidTerrs.size() - 1; i++)
					{
						bidLandTerr = ourSemiRankedBidTerrs.get(i);
						placeAllWeCanOn(bid, data, null, bidLandTerr, placeDelegate, player);
					}
					bidCycle++;
				}
				if (!player.getUnits().isEmpty())
					placeAllWeCanOn(bid, data, null, capitol, placeDelegate, player);
			}
			return;
		}
		determineCapDanger(player, data);
		final Territory specSeaTerr = getSeaTerr();
		final boolean capDanger = getCapDanger();
		// boolean amphib = isAmphibAttack(player, true);
		// maybe we bought a factory
		final Territory factTerr = getFactory();
		if (factTerr != null)
			placeAllWeCanOn(bid, data, factTerr, factTerr, placeDelegate, player);
		if (capDanger && !impassableTerrs.contains(capitol))
			placeAllWeCanOn(bid, data, capitol, capitol, placeDelegate, player);
		// check for no factories, but still can place
		final RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTACHMENT_NAME);
		if ((ra != null && ra.getPlacementAnyTerritory()) || bid) // make them all available for placing
			factoryTerritories.addAll(SUtils.allOurTerritories(data, player));
		final List<Territory> cloneFactTerritories = new ArrayList<Territory>(factoryTerritories);
		for (final Territory deleteBad : cloneFactTerritories)
		{
			if (delegate.getBattleTracker().wasConquered(deleteBad))
				factoryTerritories.remove(deleteBad);
		}
		int minDist = 100;
		if (!factoryTerritories.contains(capitol)) // what if the capitol has no factory?
			factoryTerritories.add(capitol);
		factoryTerritories.removeAll(impassableTerrs);
		/*
		Here is plan: Place units at the factory which is closest to a bad guy Territory
					  Place transports at the factory which has the most land units
		              Place attack sea units at the factory closest to attack sea units
		              Tie goes to the capitol
		*/
		Territory seaPlaceAtTrans = null, seaPlaceAtAttack = null, landFactTerr = null;
		// float eStrength = 0.0F;
		final IntegerMap<Territory> landUnitFactories = new IntegerMap<Territory>();
		final IntegerMap<Territory> transportFactories = new IntegerMap<Territory>();
		final IntegerMap<Territory> seaAttackUnitFactories = new IntegerMap<Territory>();
		Route goRoute = new Route();
		// int landUnitCount = player.getUnits().countMatches(landUnit);
		final int transUnitCount = player.getUnits().countMatches(transUnit);
		final int seaAttackUnitCount = player.getUnits().countMatches(attackUnit);
		final int fighterUnitCount = player.getUnits().countMatches(Matches.UnitCanLandOnCarrier);
		final int carrierUnitCount = player.getUnits().countMatches(Matches.UnitIsCarrier);
		final List<Territory> transTerr = SUtils.findTersWithUnitsMatching(data, player, Matches.UnitIsTransport);
		final List<Territory> landNeighbors = new ArrayList<Territory>();
		for (final Territory tT : transTerr)
			landNeighbors.addAll(SUtils.getNeighboringLandTerritories(data, player, tT));
		landNeighbors.retainAll(factoryTerritories);
		int maxUnits = 0;
		Territory maxUnitTerr = null;
		for (final Territory unitFact : factoryTerritories)
		{
			final int thisFactUnitCount = unitFact.getUnits().countMatches(landUnit);
			if (thisFactUnitCount > maxUnits)
			{
				maxUnits = thisFactUnitCount;
				maxUnitTerr = unitFact;
			}
			if (SUtils.landRouteToEnemyCapital(unitFact, goRoute, data, player))
				landUnitFactories.put(unitFact, 1);
			else if (landNeighbors.contains(unitFact))
			{
				landUnitFactories.put(unitFact, 2);
				// why wouldn't you place more sea units where your current ones are? confused...
				// transportFactories.put(unitFact, 1);
				// seaAttackUnitFactories.put(unitFact, 1);
			}
			else
			{
				landUnitFactories.put(unitFact, 0);
				transportFactories.put(unitFact, 0);
				seaAttackUnitFactories.put(unitFact, 0);
			}
		}
		// case for Russia and Germany in WW2V2 and Italians in WW2V3
		if (transportFactories.size() == 0)
		{
			for (final Territory unitFact2 : factoryTerritories)
			{
				final int shipThreat = SUtils.shipThreatToTerr(unitFact2, data, player, tFirst);
				if (tFirst && shipThreat < 2 && unitFact2 == maxUnitTerr)
					transportFactories.put(unitFact2, 2);
				else if (!tFirst && shipThreat <= 0 && unitFact2 == maxUnitTerr)
					transportFactories.put(unitFact2, 2);
				else if (SUtils.isWaterAt(unitFact2, data) && !Matches.territoryHasEnemyLandNeighbor(data, player).match(unitFact2))
					transportFactories.put(unitFact2, 0);
			}
		}
		if (seaAttackUnitFactories.size() == 0)
		{
			for (final Territory unitFact3 : factoryTerritories)
			{
				if (SUtils.isWaterAt(unitFact3, data))
					seaAttackUnitFactories.put(unitFact3, 0);
			}
		}
		final Collection<Territory> landFactories = new ArrayList<Territory>(landUnitFactories.keySet());
		final List<Territory> landRouteFactories = new ArrayList<Territory>();
		for (final Territory landCheck : landFactories)
		{
			/*
			 * Rank by: 1) Threat 2) Proximity to enemy factories 3) Proximity to enemy capital 4) Proximity to enemy
			 */
			final float totThreat = SUtils.getStrengthOfPotentialAttackers(landCheck, data, player, tFirst, false, null);
			final float myStrength = SUtils.strength(landCheck.getUnits().getUnits(), false, false, tFirst);
			final boolean landRoute = SUtils.landRouteToEnemyCapital(landCheck, goRoute, data, player);
			if (landCheck == capitol && totThreat > myStrength) // basically the same as capDanger
			{
				landUnitFactories.put(landCheck, 4);
			}
			else if (landCheck != capitol && totThreat > myStrength && !capDanger)
				landUnitFactories.put(landCheck, 4);
			else if (totThreat > (myStrength + 5.0F))
				landUnitFactories.put(landCheck, 3);
			else if (totThreat - myStrength > -10.0F && totThreat > 8.0F) // only have a marginal advantage
			{
				landUnitFactories.put(landCheck, 1);
				landRouteFactories.add(landCheck);
			}
			else if (landRoute)
				landRouteFactories.add(landCheck);
		}
		Territory minTerr = null;
		// List<Territory> landRouteFactories2 = new ArrayList<Territory>();
		// check territories which have a land route to a capital but don't have a strong local threat
		for (final Territory landCheck2 : landRouteFactories)
		{
			final boolean landRoute2 = SUtils.landRouteToEnemyCapital(landCheck2, goRoute, data, player);
			goRoute = null;
			goRoute = SUtils.findNearest(landCheck2, Matches.territoryIsEnemyNonNeutralAndHasEnemyUnitMatching(data, player, Matches.UnitCanProduceUnits),
						Matches.TerritoryIsNotImpassableToLandUnits(player, data), data);
			if ((landRoute2 && goRoute != null))
			{
				final int lRDist = goRoute.getLength();
				if (lRDist < minDist)
				{
					landUnitFactories.put(landCheck2, 1);
					minDist = lRDist;
					minTerr = landCheck2;
				}
			}
			else if (goRoute != null)
				landUnitFactories.put(landCheck2, 1);
		}
		if (minTerr != null)
			landUnitFactories.put(minTerr, 3);
		// float strengthToOvercome = 0.0F;
		final Set<Territory> transFactories = transportFactories.keySet();
		float carrierFighterAddOn = 0.0F;
		if (carrierUnitCount > 0)
		{
			if (fighterUnitCount > 0)
			{
				carrierFighterAddOn += fighterUnitCount * 3.5F;
			}
		}
		if (transFactories.size() == 1 && seaAttackUnitFactories.size() == 1)
		{
			for (final Territory oneFact : transFactories)
			{
				final Territory checkFirst = SUtils.findASeaTerritoryToPlaceOn(oneFact, data, player, tFirst);
				seaPlaceAtTrans = SUtils.getSafestWaterTerr(oneFact, null, null, data, player, false, tFirst);
				if (checkFirst != null)
				{
					final float oneStrength = SUtils.getStrengthOfPotentialAttackers(checkFirst, data, player, tFirst, false, null);
					float ourOneStrength = SUtils.strength(checkFirst.getUnits().getUnits(), false, true, tFirst);
					ourOneStrength += SUtils.strength(player.getUnits().getMatches(attackUnit), false, true, tFirst);
					if (ourOneStrength > oneStrength)
						seaPlaceAtTrans = checkFirst;
				}
				seaPlaceAtAttack = seaPlaceAtTrans;
			}
		}
		else if (transFactories.size() > 0)
		{
			for (final Territory transCheck : transFactories)
			{
				final int unitsHere = transCheck.getUnits().countMatches(landUnit);
				final Territory dropHere = SUtils.getSafestWaterTerr(transCheck, null, null, data, player, false, tFirst);
				// Territory dropHere = SUtils.findASeaTerritoryToPlaceOn(transCheck, strengthToOvercome, data, player, tFirst);
				if (dropHere == null)
					continue;
				final float eSeaStrength = SUtils.getStrengthOfPotentialAttackers(dropHere, data, player, tFirst, true, null);
				if ((eSeaStrength == 0.0F && unitsHere > transUnitCount * 2) || (eSeaStrength > 5.0F && dropHere.getUnits().someMatch(Matches.UnitIsSea)))
				{
					seaPlaceAtTrans = dropHere;
					seaPlaceAtAttack = dropHere;
				}
				/*				if (strengthToOvercome > 0.0F)
								{
									strengthToOvercome -= transUnitCount*0.5F;
									strengthToOvercome -= seaAttackUnitCount*3.5F - carrierFighterAddOn;
								}
								if (strengthToOvercome <= 5.0F && unitsHere >= transUnitCount*2)
								{
									seaPlaceAtTrans = dropHere;
									if (eSeaStrength > 5.0F)
										seaPlaceAtAttack = dropHere; //want to drop trans with sea units if reasonable
								}
				*/
			}
		}
		Territory tempTerr = null;
		if (seaPlaceAtAttack == null) // TODO: Mixed searching sea locations between purchase and place...merge
		{
			final Set<Territory> seaAttackFactories = seaAttackUnitFactories.keySet();
			for (final Territory checkAgain : seaAttackFactories)
			{
				final int attackAdv = SUtils.shipThreatToTerr(checkAgain, data, player, tFirst);
				if (attackAdv > 0)
				{
					tempTerr = SUtils.getSafestWaterTerr(checkAgain, null, null, data, player, false, tFirst);
					// tempTerr = SUtils.findASeaTerritoryToPlaceOn(checkAgain, eStrength, data, player, tFirst);
					if (tempTerr != null && (attackAdv - 1) < seaAttackUnitCount + tempTerr.getUnits().getMatches(attackUnit).size())
						seaPlaceAtAttack = tempTerr;
				}
			}
		}
		Territory tmpSeaLoc = null;
		if (specSeaTerr != null)
		{// purchasing had a special place in mind
			tmpSeaLoc = SUtils.findASeaTerritoryToPlaceOn(specSeaTerr, data, player, tFirst);
			// tmpSeaLoc = SUtils.getSafestWaterTerr(specSeaTerr, null, null, data, player, false, tFirst);
		}
		if (tmpSeaLoc != null)
			seaPlaceAtAttack = tmpSeaLoc;
		if (!bid && capDanger)
			landUnitFactories.put(capitol, 3);
		landFactories.clear();
		landFactories.addAll(landUnitFactories.keySet());
		if (landUnitFactories.size() == 1)
		{
			for (final Territory theOne : landFactories)
			{
				landFactTerr = theOne;
				seaPlaceAtTrans = landFactTerr;
				seaPlaceAtAttack = landFactTerr;
				placeSeaUnits(bid, data, seaPlaceAtAttack, seaPlaceAtTrans, placeDelegate, player);
				placeAllWeCanOn(bid, data, null, landFactTerr, placeDelegate, player);
			}
		}
		else
		{
			for (int i = 4; i >= 0; i--)
			{
				for (final Territory whichOne : landFactories)
				{
					if (landUnitFactories.getInt(whichOne) == i)
					{
						landFactTerr = whichOne;
						if (seaPlaceAtTrans == null)
						{
							final Route whichRoute = SUtils.findNearest(whichOne, Matches.territoryHasEnemyLandNeighbor(data, player), Matches.TerritoryIsNotImpassableToLandUnits(player, data), data);
							if (!Matches.territoryHasEnemyLandNeighbor(data, player).match(whichOne) && whichRoute != null && whichRoute.getLength() < 4)
								seaPlaceAtTrans = whichOne;
							else
								seaPlaceAtTrans = capitol;
						}
						if (seaPlaceAtAttack == null)
							seaPlaceAtAttack = capitol;
						placeSeaUnits(bid, data, seaPlaceAtAttack, seaPlaceAtTrans, placeDelegate, player);
						placeAllWeCanOn(bid, data, null, landFactTerr, placeDelegate, player);
					}
				}
			}
		}
		// if we have some that we haven't placed
		Collections.shuffle(factoryTerritories);
		for (final Territory t : factoryTerritories)
		{
			Territory seaPlaceAt = SUtils.findASeaTerritoryToPlaceOn(t, data, player, tFirst);
			if (seaPlaceAt == null)
				seaPlaceAt = seaPlaceAtTrans;
			if (seaPlaceAt == null)
				seaPlaceAt = t; // just put something...maybe there are no sea factories
			placeSeaUnits(bid, data, seaPlaceAt, seaPlaceAt, placeDelegate, player);
			placeAllWeCanOn(bid, data, null, t, placeDelegate, player);
		}
		now = System.currentTimeMillis();
		s_logger.finest("Time Taken " + (now - last));
	}
	
	private void placeSeaUnits(final boolean bid, final GameData data, final Territory seaPlaceAttack, final Territory seaPlaceTrans, final IAbstractPlaceDelegate placeDelegate, final PlayerID player)
	{
		final CompositeMatch<Unit> attackUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.UnitIsNotTransport);
		final List<Unit> seaUnits = player.getUnits().getMatches(attackUnit);
		final List<Unit> transUnits = player.getUnits().getMatches(Matches.UnitIsTransport);
		final List<Unit> airUnits = player.getUnits().getMatches(Matches.UnitCanLandOnCarrier);
		final List<Unit> carrierUnits = player.getUnits().getMatches(Matches.UnitIsCarrier);
		if (carrierUnits.size() > 0 && airUnits.size() > 0 && (Properties.getProduceFightersOnCarriers(data) || Properties.getLHTRCarrierProductionRules(data) || bid))
		{
			int carrierSpace = 0;
			for (final Unit carrier1 : carrierUnits)
				carrierSpace += UnitAttachment.get(carrier1.getType()).getCarrierCapacity();
			final Iterator<Unit> airIter = airUnits.iterator();
			while (airIter.hasNext() && carrierSpace > 0)
			{
				final Unit airPlane = airIter.next();
				seaUnits.add(airPlane);
				carrierSpace -= UnitAttachment.get(airPlane.getType()).getCarrierCost();
			}
		}
		if (bid)
		{
			if (!seaUnits.isEmpty())
				doPlace(seaPlaceAttack, seaUnits, placeDelegate);
			if (!transUnits.isEmpty())
				doPlace(seaPlaceTrans, transUnits, placeDelegate);
			return;
		}
		if (seaUnits.isEmpty() && transUnits.isEmpty())
			return;
		if (seaPlaceAttack == seaPlaceTrans)
		{
			seaUnits.addAll(transUnits);
			transUnits.clear();
		}
		final PlaceableUnits pu = placeDelegate.getPlaceableUnits(seaUnits, seaPlaceAttack);
		int pLeft = 0;
		if (pu.getErrorMessage() != null)
			return;
		if (!seaUnits.isEmpty())
		{
			pLeft = pu.getMaxUnits();
			if (pLeft == -1)
				pLeft = Integer.MAX_VALUE;
			final int numPlace = Math.min(pLeft, seaUnits.size());
			pLeft -= numPlace;
			final Collection<Unit> toPlace = seaUnits.subList(0, numPlace);
			doPlace(seaPlaceAttack, toPlace, placeDelegate);
		}
		if (!transUnits.isEmpty())
		{
			final PlaceableUnits pu2 = placeDelegate.getPlaceableUnits(transUnits, seaPlaceTrans);
			if (pu2.getErrorMessage() != null)
				return;
			pLeft = pu2.getMaxUnits();
			if (pLeft == -1)
				pLeft = Integer.MAX_VALUE;
			final int numPlace = Math.min(pLeft, transUnits.size());
			final Collection<Unit> toPlace = transUnits.subList(0, numPlace);
			doPlace(seaPlaceTrans, toPlace, placeDelegate);
		}
	}
	
	private void placeAllWeCanOn(final boolean bid, final GameData data, final Territory factoryPlace, final Territory placeAt, final IAbstractPlaceDelegate placeDelegate, final PlayerID player)
	{
		final CompositeMatch<Unit> landOrAir = new CompositeMatchOr<Unit>(Matches.UnitIsAir, Matches.UnitIsLand);
		if (factoryPlace != null) // place a factory?
		{
			final Collection<Unit> toPlace = new ArrayList<Unit>(player.getUnits().getMatches(Matches.UnitCanProduceUnitsAndIsConstruction));
			if (toPlace.size() == 1) // only 1 may have been purchased...anything greater is wrong
			{
				doPlace(factoryPlace, toPlace, placeDelegate);
				setFactory(null);
				return;
			}
			else if (toPlace.size() > 1)
				return;
		}
		final List<Unit> landUnits = player.getUnits().getMatches(landOrAir);
		final Territory capitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
		final PlaceableUnits pu3 = placeDelegate.getPlaceableUnits(landUnits, placeAt);
		if (pu3.getErrorMessage() != null)
			return;
		int placementLeft3 = pu3.getMaxUnits();
		if (placementLeft3 == -1)
			placementLeft3 = Integer.MAX_VALUE;
		// allow placing only 1 unit per territory if a bid, unless it is the capitol (water is handled in placeseaunits)
		if (bid)
			placementLeft3 = 1;
		if (bid && (placeAt == capitol))
			placementLeft3 = 1000;
		if (!landUnits.isEmpty())
		{
			final int landPlaceCount = Math.min(placementLeft3, landUnits.size());
			placementLeft3 -= landPlaceCount;
			final Collection<Unit> toPlace = landUnits.subList(0, landPlaceCount);
			doPlace(placeAt, toPlace, placeDelegate);
		}
	}
	
	private void doPlace(final Territory where, final Collection<Unit> toPlace, final IAbstractPlaceDelegate del)
	{
		final String message = del.placeUnits(new ArrayList<Unit>(toPlace), where);
		if (message != null)
		{
			s_logger.fine(message);
			s_logger.fine("Attempt was at:" + where + " with:" + toPlace);
		}
		pause();
	}
	
	/*
	 *
	 *
	 * @see games.strategy.triplea.player.ITripleaPlayer#selectCasualties(java.lang.String,
	 *      java.util.Collection, java.util.Map, int, java.lang.String,
	 *      games.strategy.triplea.delegate.DiceRoll,
	 *      games.strategy.engine.data.PlayerID, java.util.List)
	 */
	@Override
	public CasualtyDetails selectCasualties(final Collection<Unit> selectFrom, final Map<Unit, Collection<Unit>> dependents, final int count, final String message, final DiceRoll dice,
				final PlayerID hit, final CasualtyList defaultCasualties, final GUID battleID, final Territory battlesite, final boolean allowMultipleHitsPerUnit)
	{
		if (defaultCasualties.size() != count)
			throw new IllegalStateException("Select Casualties showing different numbers for number of hits to take vs total size of default casualty selections");
		final GameData data = getPlayerBridge().getGameData();
		// TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		final List<Unit> rDamaged = new ArrayList<Unit>();
		final List<Unit> rKilled = new ArrayList<Unit>();
		int xCount = count; // how many the game is saying we should have
		xCount -= defaultCasualties.getDamaged().size();
		rDamaged.addAll(defaultCasualties.getDamaged());
		// rKilled.addAll(defaultCasualties.getKilled());
		/*for (Unit unitBB : selectFrom2)
		{
			if (Matches.UnitIsTwoHit.match(unitBB))
			{
				if (unitBB.getHits() == 0 && xCount > 0)
				{
					rDamaged.add(unitBB);
					xCount--;
				}
			}
		}*/
		if (xCount == 0)
		{
			if (count != rKilled.size() + rDamaged.size())
				throw new IllegalStateException("Moore AI selected wrong number of casualties");
			return new CasualtyDetails(rKilled, rDamaged, false);
		}
		if (xCount < 0)
		{
			throw new IllegalStateException("Can not choose more casualties than the number of hits");
		}
		if (xCount >= selectFrom.size())
		{
			rKilled.addAll(selectFrom);
			final CasualtyDetails m4 = new CasualtyDetails(rKilled, rDamaged, false);
			if (count != rKilled.size() + rDamaged.size())
				throw new IllegalStateException("Moore AI selected wrong number of casualties");
			return m4;
		}
		final boolean defending = !get_onOffense();
		final IntegerMap<UnitType> costs = BattleCalculator.getCostsForTUV(hit, data);
		final float canWinPercentage = 1.0F; // we need to run a battle calc or something to determine what the chance of us winning is
		final boolean bonus = (canWinPercentage > .8);
		final List<Unit> workUnits1 = new ArrayList<Unit>(BattleCalculator.sortUnitsForCasualtiesWithSupport(selectFrom, defending, hit, costs, TerritoryEffectHelper.getEffects(battlesite), data,
					bonus));
		final List<Unit> workUnits2 = new ArrayList<Unit>(DUtils.InterleaveUnits_CarriersAndPlanes(workUnits1, 0));
		for (int j = 0; j < xCount; j++)
		{
			rKilled.add(workUnits2.get(j));
		}
		final CasualtyDetails m2 = new CasualtyDetails(rKilled, rDamaged, false);
		if (count != rKilled.size() + rDamaged.size())
			throw new IllegalStateException("Moore AI selected wrong number of casualties");
		return m2;
		/* this is the worst casualty picker known to man:
		if (xCount > 0)
		{
			for (Unit unitx : selectFrom)
			{
				if (Matches.UnitIsArtillerySupportable.match(unitx))
					workUnits.add(unitx);
			}
			for (Unit unitx : selectFrom)
			{
				if (Matches.UnitIsArtillery.match(unitx))
					workUnits.add(unitx);
			}
			for (Unit unitx : selectFrom)
			{
				if (Matches.UnitCanBlitz.match(unitx))
					workUnits.add(unitx);
			}
			for (Unit unitx : selectFrom) // empty transport
			{
				if (!Properties.getTransportCasualtiesRestricted(data) && Matches.UnitIsTransport.match(unitx) && !tracker.isTransporting(unitx))
					workUnits.add(unitx);
			}
			for (Unit unitx : selectFrom)
			{
				if (Matches.UnitIsSub.match(unitx))
					workUnits.add(unitx);
			}
			for (Unit unitx : selectFrom)
			{
				if (Matches.UnitIsDestroyer.match(unitx))
					workUnits.add(unitx);
			}
			for (Unit unitx : selectFrom)
			{
				if (Matches.UnitIsCruiser.match(unitx))
					workUnits.add(unitx);
			}
			for (Unit unitx : selectFrom)
			{
				if (Matches.UnitIsStrategicBomber.match(unitx))
					workUnits.add(unitx);
			}
			for (Unit unitx : selectFrom)
			{
				if (Matches.UnitIsAir.match(unitx) && Matches.UnitIsNotStrategicBomber.match(unitx))
					workUnits.add(unitx);
			}
			for (Unit unitx : selectFrom) // loaded transport
			{
				if (!Properties.getTransportCasualtiesRestricted(data) && Matches.UnitIsTransport.match(unitx) && tracker.isTransporting(unitx))
					workUnits.add(unitx);
			}
			for (Unit unitx : selectFrom)
			{
				if (Matches.UnitIsCarrier.match(unitx))
					workUnits.add(unitx);
			}
			for (Unit unitx : selectFrom) // any other unit, but make sure trannys are last if necessary
			{
				if (Matches.UnitIsNotTransport.match(unitx) && !workUnits.contains(unitx) && !Matches.UnitIsTwoHit.match(unitx))
					workUnits.add(unitx);
			}
			for (Unit unitx : selectFrom)
			{
				if (Matches.UnitIsTwoHit.match(unitx))
					workUnits.add(unitx);
			}
			
			// add anything not selected above
			Set<Unit> remainder = new HashSet<Unit>(selectFrom);
			remainder.removeAll(workUnits);
			workUnits.addAll(remainder);
		}

		for (int j = 0; j < xCount; j++)
		{
			rKilled.add(workUnits.get(j));
		}
		
		CasualtyDetails m2 = new CasualtyDetails(rKilled, rDamaged, false);
		
		return m2;*/
		/* Order:
			SEA:
			0) 1st hit battleship
			1) Empty Transport
			2) Submarine
			3) Destroyer
			4) Bomber
			5) Fighter
			6) Loaded Transport
			7) Carrier (Fighter is better than 1 Carrier...need to check score in battle) implement later...
			8) Battleship
			9) anything else
		*/
	}
	
	/*
	 * @see games.strategy.triplea.player.ITripleaPlayer#shouldBomberBomb(games.strategy.engine.data.Territory)
	 */
	@Override
	public boolean shouldBomberBomb(final Territory territory)
	{
		// only if not needed in a battle
		final GameData data = getPlayerBridge().getGameData();
		final PlayerID ePlayer = territory.getOwner();
		final List<PlayerID> attackPlayers = SUtils.getEnemyPlayers(data, ePlayer); // list of players that could be the attacker
		boolean thisIsAnAttack = false;
		for (final PlayerID player : attackPlayers)
		{
			final CompositeMatch<Unit> noBomberUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsNotStrategicBomber);
			final List<Unit> allAttackUnits = territory.getUnits().getMatches(noBomberUnit);
			if (!allAttackUnits.isEmpty())
				thisIsAnAttack = true;
		}
		return !thisIsAnAttack;
	}
	
	@Override
	public Unit whatShouldBomberBomb(final Territory territory, final Collection<Unit> potentialTargets, final Collection<Unit> bombers)
	{
		if (potentialTargets == null || potentialTargets.isEmpty())
			return null;
		final Collection<Unit> factories = Match.getMatches(potentialTargets, Matches.UnitCanProduceUnitsAndCanBeDamaged);
		if (factories.isEmpty())
			return potentialTargets.iterator().next();
		return factories.iterator().next();
	}
	
	@Override
	public boolean selectAttackSubs(final Territory unitTerritory)
	{
		return true;
	}
	
	@Override
	public boolean selectAttackUnits(final Territory unitTerritory)
	{
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * @see games.strategy.triplea.baseAI.AbstractAI#selectAttackTransports(games.strategy.engine.data.Territory)
	 */
	@Override
	public boolean selectAttackTransports(final Territory territory)
	{
		return true;
	}
	
	/*
	 * @see games.strategy.triplea.player.ITripleaPlayer#getNumberOfFightersToMoveToNewCarrier(java.util.Collection, games.strategy.engine.data.Territory)
	 */
	@Override
	public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(final Collection<Unit> fightersThatCanBeMoved, final Territory from)
	{
		final List<Unit> rVal = new ArrayList<Unit>();
		for (final Unit fighter : fightersThatCanBeMoved)
			rVal.add(fighter);
		return rVal;
	}
	
	/*
	 * @see games.strategy.triplea.player.ITripleaPlayer#selectTerritoryForAirToLand(java.util.Collection, java.lang.String)
	 */
	@Override
	public Territory selectTerritoryForAirToLand(final Collection<Territory> candidates, final Territory currentTerritory, final String unitMessage)
	{
		// need to land in territory with infantry, especially if bomber
		return candidates.iterator().next();
	}
	
	@Override
	public boolean confirmMoveInFaceOfAA(final Collection<Territory> aaFiringTerritories)
	{
		return false;
	}
	
	/**
	 * Select the territory to bombard with the bombarding capable unit (eg battleship)
	 * 
	 * @param unit
	 *            - the bombarding unit
	 * @param unitTerritory
	 *            - where the bombarding unit is
	 * @param territories
	 *            - territories where the unit can bombard
	 * @param noneAvailable
	 * @return the Territory to bombard in, null if the unit should not bombard
	 */
	@Override
	public Territory selectBombardingTerritory(final Unit unit, final Territory unitTerritory, final Collection<Territory> territories, final boolean noneAvailable)
	{
		if (noneAvailable || territories.size() == 0)
			return null;
		else
		{
			for (final Territory t : territories)
				return t;
		}
		return null;
	}
	
	// private static int counter = 0;
	@Override
	public Territory retreatQuery(final GUID battleID, final boolean submerge, final Territory battleTerritory, final Collection<Territory> possibleTerritories, final String message)
	{
		if (battleTerritory == null)
			return null;
		// retreat anytime only air units are remaining
		// submerge anytime only subs against air units
		// don't understand how to use this routine
		final GameData data = getPlayerBridge().getGameData();
		// boolean iamOffense = get_onOffense();
		final boolean tFirst = transportsMayDieFirst();
		// TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		// BattleTracker bTracker = DelegateFinder.battleDelegate(data).getBattleTracker();
		final boolean attacking = true; // determine whether player is offense or defense
		// boolean subsCanSubmerge = games.strategy.triplea.Properties.getSubmersible_Subs(data);
		final PlayerID player = getPlayerID();
		// List<PlayerID> ePlayers = SUtils.getEnemyPlayers(data, player);
		final List<Unit> myUnits = battleTerritory.getUnits().getMatches(Matches.unitIsOwnedBy(player));
		final List<Unit> defendingUnits = battleTerritory.getUnits().getMatches(Matches.enemyUnit(player, data));
		if (Matches.TerritoryIsLand.match(battleTerritory))
		{
			final List<Unit> retreatUnits = new ArrayList<Unit>();
			final List<Unit> nonRetreatUnits = new ArrayList<Unit>();
			for (final Unit u : myUnits)
			{
				if (TripleAUnit.get(u).getWasAmphibious())
					nonRetreatUnits.add(u);
				else
					retreatUnits.add(u);
			}
			final float retreatStrength = SUtils.strength(retreatUnits, true, false, false);
			final float nonRetreatStrength = SUtils.strength(nonRetreatUnits, true, false, false);
			final float totalStrength = retreatStrength + nonRetreatStrength;
			final float enemyStrength = SUtils.strength(defendingUnits, false, false, false);
			if (totalStrength > enemyStrength * 1.05F)
			{
				return null;
			}
			else
			{
				Territory retreatTo = null;
				float retreatDiff = 0.0F;
				if (possibleTerritories.size() == 1)
					retreatTo = possibleTerritories.iterator().next();
				else
				{
					final List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
					final List<Territory> ourEnemyTerr = new ArrayList<Territory>();
					final HashMap<Territory, Float> rankMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, false, false, true);
					if (ourFriendlyTerr.containsAll(possibleTerritories))
						SUtils.reorder(ourFriendlyTerr, rankMap, true);
					ourFriendlyTerr.retainAll(possibleTerritories);
					final Territory myCapital = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
					for (final Territory capTerr : ourFriendlyTerr)
					{
						if (Matches.territoryIsAlliedAndHasAlliedUnitMatching(data, player, Matches.UnitCanProduceUnits).match(capTerr))
						{
							final boolean isMyCapital = myCapital.equals(capTerr);
							final float strength1 = SUtils.getStrengthOfPotentialAttackers(capTerr, data, player, false, true, null);
							float ourstrength = SUtils.strengthOfTerritory(data, capTerr, player, false, false, false, true);
							if (isMyCapital)
							{
								ourstrength = SUtils.strength(player.getUnits().getUnits(), false, false, false);
							}
							if (ourstrength < strength1 && (retreatTo == null || isMyCapital))
								retreatTo = capTerr;
						}
					}
					final Iterator<Territory> retreatTerrs = ourFriendlyTerr.iterator();
					if (retreatTo == null)
					{
						while (retreatTerrs.hasNext())
						{
							final Territory retreatTerr = retreatTerrs.next();
							final float existingStrength = SUtils.strength(retreatTerr.getUnits().getUnits(), false, false, false);
							final float eRetreatStrength = SUtils.getStrengthOfPotentialAttackers(retreatTerr, data, player, false, true, null);
							float firstDiff = eRetreatStrength - existingStrength;
							if (firstDiff < 0.0F)
							{
								firstDiff -= retreatStrength;
								if (firstDiff < 0.0F)
								{
									if (retreatDiff < firstDiff)
									{
										retreatTo = retreatTerr;
										retreatDiff = firstDiff;
									}
								}
								else if (retreatDiff > firstDiff || retreatTo == null)
								{
									retreatTo = retreatTerr;
									retreatDiff = firstDiff;
								}
							}
						}
					}
				}
				return retreatTo;
			}
		}
		else
		{
			// final CompositeMatch<Unit> mySub = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSub, Matches.unitIsNotSubmerged(data));
			final CompositeMatch<Unit> myShip = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSea, Matches.unitIsNotSubmerged(data));
			final CompositeMatch<Unit> myPlane = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir);
			final CompositeMatch<Unit> enemyAirUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsNotLand);
			final CompositeMatch<Unit> enemySeaUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsSea);
			final List<Unit> myShips = battleTerritory.getUnits().getMatches(myShip);
			final List<Unit> myPlanes = battleTerritory.getUnits().getMatches(myPlane);
			final float myShipStrength = SUtils.strength(myShips, attacking, true, tFirst);
			final float myPlaneStrength = SUtils.strength(myPlanes, attacking, true, tFirst);
			final float totalStrength = myShipStrength + myPlaneStrength;
			final List<Unit> enemyAirUnits = battleTerritory.getUnits().getMatches(enemyAirUnit);
			final List<Unit> enemySeaUnits = battleTerritory.getUnits().getMatches(enemySeaUnit);
			if (submerge && enemySeaUnits.isEmpty() && enemyAirUnits.size() > 0)
				return battleTerritory;
			
			final float enemyAirStrength = SUtils.strength(enemyAirUnits, !attacking, true, tFirst);
			final float enemySeaStrength = SUtils.strength(enemySeaUnits, !attacking, true, tFirst);
			final float enemyStrength = enemyAirStrength + enemySeaStrength;
			if (attacking && enemyStrength > (totalStrength + 1.0F))
			{
				Territory retreatTo = null;
				if (possibleTerritories.size() > 0)
					retreatTo = possibleTerritories.iterator().next();
				// TODO: Create a selection for best seaTerritory
				return retreatTo;
			}
		}
		return null;
	}
	
	/*public Collection<Unit> scrambleQuery(final GUID battleID, final Collection<Territory> possibleTerritories, final String message, final PlayerID player)
	{
		return null;
	}*/
	
	@Override
	public HashMap<Territory, Collection<Unit>> scrambleUnitsQuery(final Territory scrambleTo, final Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers)
	{
		return null;
	}
	
	@Override
	public Collection<Unit> selectUnitsQuery(final Territory current, final Collection<Unit> possible, final String message)
	{
		return null;
	}
	
	/* (non-Javadoc)
	 * @see games.strategy.triplea.player.ITripleaPlayer#selectFixedDice(int, java.lang.String)
	 */
	@Override
	public int[] selectFixedDice(final int numRolls, final int hitAt, final boolean hitOnlyIfEquals, final String message, final int diceSides)
	{
		final int[] dice = new int[numRolls];
		for (int i = 0; i < numRolls; i++)
		{
			dice[i] = (int) Math.ceil(Math.random() * diceSides);
		}
		return dice;
	}
	
	public static final Match<Unit> Transporting = new Match<Unit>()
	{
		@Override
		public boolean match(final Unit o)
		{
			return (TripleAUnit.get(o).getTransporting().size() > 0);
		}
	};
}
