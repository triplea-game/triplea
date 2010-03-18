package games.strategy.triplea.strongAI;

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

import games.strategy.engine.data.*;
import games.strategy.engine.gamePlayer.*;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.*;
import games.strategy.triplea.baseAI.*;
import games.strategy.triplea.delegate.*;
import games.strategy.triplea.delegate.dataObjects.*;
import games.strategy.triplea.delegate.remote.*;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.Properties;
import games.strategy.util.*;

import java.util.*;
import java.util.logging.Logger;

/*
 *
 * A stronger AI, based on some additional complexity, used the weak AI as a blueprint.<p>
 * This still needs work. Known Issues:
 *    1) *fixed* if a group of fighters on water needs 1 to land on island, it doesn't...works correctly if there is only 1 fighter
 *    2) *fixed* if Germany is winning, it doesn't switch to buying up transports
 *    3) *fixed* if a transport is at an owned territory with a factory, it won't leave unless it has units
 *    4) *fixed* Germany doesn't guard the shoreline well
 *    5) Ships are moving 1 territory too close to a large pack of ships (better analysis)
 *    6) *fixed* Ships make themselves vulnerable to plane attack
 *    7) *partial* No submerging or retreating has been implemented
 *    8) *fixed* Planes still occasionally stop in an unoccupied territory which is open to invasion with cheap units
 *    9) Need to analyze 1 territory further and delay attack if it brings a set of units under overwhelming odds
 *   10) *fixed* Units are still loaded onto transports from Southern Europe/Western US even when enemy forces are in neighboring terr
 *   11) *fixed* Still putting transport on a factory that is closest to enemy cap
 *   12) *fixed* Transports should scope out available units and go to them
 *   13) *fixed* When allied forces are nearby, occasionally attacks are with deficient forces
 *   14) *fixed* Occasionally air is attacking large units without accompanying land forces
 *   15) *fixed* AI needs to protect countries nearest its capital
 *   16) *fixed* AI allows England to be invaded because purchases that focus on navy are not buying any infantry
 *   17) *fixed* AI needs to keep fighters home when attackers are near the capital
 *
 *
 * @author Kevin Moore
 *         2008-2009
 */
public class StrongAI extends AbstractAI implements IGamePlayer, ITripleaPlayer
{
	//Amphib Route will hold the water Terr, Land Terr combination for unloading units
    private final static Logger s_logger = Logger.getLogger(StrongAI.class.getName());
    private Territory m_factTerr = null, m_seaTerr = null, m_battleTerr = null; //determine the target Territory during Purchase and Save it
    private boolean m_AE = false, m_transports_may_die = true, m_zero_combat_attack = true, m_cap_danger = false, m_natObjective = false;
    private boolean m_bought_Attack_Ships = false, m_keep_Ships_At_Base = false;
    private boolean m_onOffense = false;
    private HashMap<Territory, Territory> amphibMap= new HashMap<Territory, Territory>();
    private HashMap<Territory, Collection<Unit>> shipsMovedMap = new HashMap<Territory, Collection<Unit>>();
    private Collection<Territory> m_seaTerrAttacked = new ArrayList<Territory>();
    private Collection<Territory> m_landTerrAttacked = new ArrayList<Territory>();
    private Collection<Territory> m_impassableTerrs = new ArrayList<Territory>();

    /** Creates new TripleAPlayer */
    public StrongAI(String name)
    {
        super(name);

    }

    private void set_onOffense(boolean value)
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
//		m_AE = games.strategy.triplea.Properties.getWW2V3(data);
		m_transports_may_die = !games.strategy.triplea.Properties.getTransportCasualtiesRestricted(data);
		m_zero_combat_attack = games.strategy.triplea.Properties.getHariKariUnits(data);
		m_natObjective = games.strategy.triplea.Properties.getNationalObjectives(data);
	}

	private void setBattleInfo(Territory bTerr)
	{
		m_battleTerr = bTerr;
	}
	
	private Territory getBattleTerritory()
	{
		return m_battleTerr;
	}
	
	private void setImpassableTerrs(PlayerID player)
	{
		GameData data = getPlayerBridge().getGameData();
		for (Territory t : data.getMap().getTerritories())
		{
			if (Matches.TerritoryIsPassableAndNotRestricted( player).invert().match(t) && Matches.TerritoryIsLand.match(t))
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

	private void setAttackShipPurchase(boolean doBuyAttackShip)
	{
		m_bought_Attack_Ships = doBuyAttackShip;
	}
	
	private boolean getAttackShipPurchase()
	{
		return m_bought_Attack_Ships;
	}
	
	private void setSeaTerrAttacked(Collection<Territory> seaTerr)
	{
		m_seaTerrAttacked.addAll(seaTerr);
	}
	
	private List<Territory> getSeaTerrAttacked()
	{
		List<Territory> seaTerr = new ArrayList<Territory>(m_seaTerrAttacked);
		return seaTerr;
	}

	private void setLandTerrAttacked(Collection<Territory> landTerr)
	{
		m_landTerrAttacked.addAll(landTerr);
	}
	
	private List<Territory> getLandTerrAttacked()
	{
		List<Territory> landTerr = new ArrayList<Territory>(m_landTerrAttacked);
		return landTerr;
	}

	private void setSeaTerr(Territory seaTerr)
	{
		m_seaTerr = seaTerr;
	}
	
	private Territory getSeaTerr()
	{
		return m_seaTerr;
	}
	
	private void setKeepShipsAtBase(boolean keep)
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

	private void setFactory(Territory t)
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
	
	private void setAmphibMap(HashMap<Territory, Territory> xAmphibMap)
	{
		amphibMap = xAmphibMap;
	}
	
	private HashMap<Territory, Collection<Unit>> getShipsMovedMap()
	{
		return shipsMovedMap;
	}
	
	private void setShipsMovedMap(HashMap<Territory, Collection<Unit>> xMovedMap)
	{
		shipsMovedMap = xMovedMap;
	}

	private void setCapDanger(Boolean danger)
	{
		m_cap_danger = danger; //use this to track when determined the capital is in danger
	}

	private boolean getCapDanger()
	{
		return m_cap_danger;
	}

	private boolean getNationalObjectives()
	{
		return m_natObjective;
	}

    protected void tech(ITechDelegate techDelegate, GameData data, PlayerID player)
    {          
    	Territory myCapitol = TerritoryAttachment.getCapital(player, data);
    	float eStrength = SUtils.getStrengthOfPotentialAttackers(myCapitol, data, player, false, true, null);
    	float myStrength = SUtils.strength(myCapitol.getUnits().getUnits(), false, false, false);
    	List<Territory> areaStrength = SUtils.getNeighboringLandTerritories(data, player, myCapitol);
    	for (Territory areaTerr : areaStrength)
    		myStrength += SUtils.strength(areaTerr.getUnits().getUnits(), false, false, false)*0.75F;
    	boolean capDanger = myStrength < (eStrength*1.25F + 3.0F);
    	if (capDanger)
    		return;
    	if(games.strategy.triplea.Properties.getWW2V3TechModel(data)) 
    	{ 
    		Resource pus = data.getResourceList().getResource(Constants.PUS); 
    		int PUs = player.getResources().getQuantity(pus ); 
    		Resource techtokens = data.getResourceList().getResource(Constants.TECH_TOKENS); 
    		int TechTokens = player.getResources().getQuantity(techtokens); 
    		int TokensToBuy=0; 
    		if(TechTokens < 3 && PUs > Math.random()*160) 
    			TokensToBuy=1; 
    		if (TechTokens > 0 || TokensToBuy > 0) 
    		{ 
    			if (Math.random()>0.35) 
    				techDelegate.rollTech(TechTokens+TokensToBuy,TechAdvance.LAND_PRODUCTION_ADVANCES,TokensToBuy); 
    			else 
    				techDelegate.rollTech(TechTokens+TokensToBuy,TechAdvance.AIR_NAVAL_ADVANCES,TokensToBuy); 
    		} 
    	} 
    }

    private Route getAmphibRoute(final PlayerID player, final boolean nonCombat)
    {
        if(!isAmphibAttack(player, false))
            return null;

        final GameData data = getPlayerBridge().getGameData();

        Territory ourCapitol = TerritoryAttachment.getCapital(player, data);
        Match<Territory> endMatch = new Match<Territory>()
        {
            @Override
            public boolean match(Territory o)
            {
                boolean impassable = TerritoryAttachment.get(o) != null &&  TerritoryAttachment.get(o) .isImpassible();
                return  !impassable && !o.isWater() && SUtils.hasLandRouteToEnemyOwnedCapitol(o, player, data) && (nonCombat == Matches.isTerritoryOwnedBy(player).match(o));
            }

        };

        Match<Territory> routeCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data));

        Route withNoEnemy = SUtils.findNearest(ourCapitol, endMatch, routeCond, data);
        if(withNoEnemy != null && withNoEnemy.getLength() > 0)
            return withNoEnemy;

        //this will fail if our capitol is not next to water, c'est la vie.
        Route route =  SUtils.findNearest(ourCapitol, endMatch, Matches.TerritoryIsWater, data);
        if(route != null && route.getLength() == 0) {
            return null;
        }
        return route;
    }

    private boolean isAmphibAttack(PlayerID player, boolean requireWaterFactory)
    {
    	GameData data = getPlayerBridge().getGameData();
        Territory capitol = TerritoryAttachment.getCapital(player, getPlayerBridge().getGameData());
        if(capitol == null || !capitol.getOwner().equals(player))
            return false;
        
        if (requireWaterFactory)
        {
        	List<Territory> factories = SUtils.findCertainShips(data, player, Matches.UnitIsFactory);
        	List<Territory> waterFactories = SUtils.stripLandLockedTerr(data, factories);
        	if (waterFactories.isEmpty())
        		return false;
        }
        //find a land route to an enemy territory from our capitol
        boolean amphibPlayer = !SUtils.hasLandRouteToEnemyOwnedCapitol(capitol, player, data);
    	int totProduction = 0, allProduction = 0;
        if (amphibPlayer)
        {
        	List<Territory> allFactories = SUtils.findCertainShips(data, player, Matches.UnitIsFactory);
//        	allFactories.remove(capitol);
        	for (Territory checkFactory : allFactories)
        	{
        		boolean isLandRoute = SUtils.hasLandRouteToEnemyOwnedCapitol(checkFactory, player, data);
        		int factProduction = TerritoryAttachment.get(checkFactory).getProduction();
        		allProduction += factProduction;
        		if (isLandRoute)
        			totProduction += factProduction;
        	}
        }
        // if the land based production is greater than one-third of all factory production, turn off amphib
        // works better on NWO where Brits start with factories in North Africa
        
        amphibPlayer = amphibPlayer ? (totProduction < allProduction/3) : false;
        return amphibPlayer;
    }

    //determine danger to the capital and set the capdanger variable...other routines can just get the CapDanger var
    //should be called prior to combat, non-combat and purchasing units to assess the current situation
    private HashMap<Territory, Float> determineCapDanger(PlayerID player, GameData data)
    {
    	float threatFactor = 1.05F, enemyStrength = 0.0F, ourStrength = 0.0F;
    	boolean capDanger = false;
    	HashMap<Territory, Float> factMap = new HashMap<Territory, Float>();
        Territory myCapital = TerritoryAttachment.getCapital(player, data);
        if (myCapital == null)
        	return factMap;
        List<Territory> factories = SUtils.findCertainShips(data, player, Matches.UnitIsFactory);
        if (!factories.contains(myCapital))
        	factories.add(myCapital);
  	
        boolean tFirst = transportsMayDieFirst(); 
        for (Territory factory : factories)
        {
        	enemyStrength = SUtils.getStrengthOfPotentialAttackers(myCapital, data, player, tFirst, false, null);
        	factMap.put(factory, enemyStrength);
        }
        float capPotential = factMap.get(myCapital);
		ourStrength = SUtils.strength(myCapital.getUnits().getUnits(), false, false, tFirst);
		ourStrength += 2.0F; //assume new units on the way...3 infantry ~= 8.1F

		if (capPotential > ourStrength*threatFactor) //we are in trouble
			capDanger = true;

		setCapDanger(capDanger); //save this for unit placement
		return factMap;
    }
    
    protected void move(boolean nonCombat, IMoveDelegate moveDel, GameData data, PlayerID player)
    {
        if(nonCombat)
            doNonCombatMove(moveDel, player);
        else
            doCombatMove(moveDel, player);

        pause();
    }

    private void doNonCombatMove(IMoveDelegate moveDel, PlayerID player)
    {
        GameData data = getPlayerBridge().getGameData();

        List<Collection<Unit>> moveUnits = new ArrayList<Collection<Unit>>();
        List<Route> moveRoutes = new ArrayList<Route>();
        List<Collection<Unit>> transportsToLoad = new ArrayList<Collection<Unit>>();
        s_logger.fine("Start NonCombat for: " + player.getName());
        s_logger.fine("populateTransportLoad");
        populateTransportLoad(true, data, moveUnits, moveRoutes, transportsToLoad, player);
        doMove(moveUnits, moveRoutes, transportsToLoad, moveDel);
        moveRoutes.clear();
        moveUnits.clear();
        transportsToLoad.clear();
        
        s_logger.fine("protectOurAllies");
        protectOurAllies(true, data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);
        moveRoutes.clear();
        moveUnits.clear();

        s_logger.fine("planesToCarriers");
        planesToCarriers(data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);
        moveRoutes.clear();
        moveUnits.clear();

        s_logger.fine("bomberNonComMove");
        bomberNonComMove(data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);
        moveRoutes.clear();
        moveUnits.clear();

        determineCapDanger(player, data);

        s_logger.fine("populateNonComTransportMove");
        populateNonComTransportMove(data, moveUnits, moveRoutes, player);
//        simulatedNonCombatTransportMove(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveRoutes.clear();
		moveUnits.clear();

        //unload the transports that can be unloaded
        s_logger.fine("populateTransportUnLoadNonCom");
        populateTransportUnloadNonCom(true, data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);
        moveUnits.clear();
        moveRoutes.clear();

        //check to see if we have missed any transports
        s_logger.fine("checkUnMovedTransports");
        checkUnmovedTransports(data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);
        moveUnits.clear();
        moveRoutes.clear();

        s_logger.fine("bringShipsToTransports");
        bringShipsToTransports(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveUnits.clear();
		moveRoutes.clear();
		
        s_logger.fine("populateNonCombatSea");
        populateNonCombatSea(true, data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);
        moveUnits.clear();
        moveRoutes.clear();

        s_logger.fine("stopBlitzAttack");
        stopBlitzAttack(data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);
        moveUnits.clear();
        moveRoutes.clear();
        
        s_logger.fine("populateNonCombat");
        populateNonCombat(data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);
        moveUnits.clear();
        moveRoutes.clear();
        
        s_logger.fine("movePlanesHomeNonCom");
        movePlanesHomeNonCom(moveUnits, moveRoutes, player, data); //combine this with checkPlanes at some point
        doMove(moveUnits, moveRoutes, null, moveDel);
        moveUnits.clear();
        moveRoutes.clear();

        //check to see if we have vulnerable planes
        s_logger.fine("CheckPlanes");
        CheckPlanes(data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);
        moveUnits.clear();
        moveRoutes.clear();
        transportsToLoad.clear();

        s_logger.fine("secondLookSea");
        secondLookSea(data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);
        moveUnits.clear();
        moveRoutes.clear();
        
        s_logger.fine("populateTransportLoad");
        populateTransportLoad(true, data, moveUnits, moveRoutes, transportsToLoad, player); //another pass on loading
        doMove(moveUnits, moveRoutes, transportsToLoad, moveDel);
        moveRoutes.clear();
        moveUnits.clear();
        transportsToLoad.clear();

        //unload the transports that can be unloaded
        s_logger.fine("populateTransportUnloadNonCom");
        populateTransportUnloadNonCom(false, data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);
        moveUnits.clear();
        moveRoutes.clear();
        
        s_logger.fine("nonCombatPlanes");
		nonCombatPlanes(data, player, moveUnits, moveRoutes);
		doMove(moveUnits, moveRoutes, null, moveDel);
        moveUnits.clear();
        moveRoutes.clear();
        
        s_logger.fine("secondNonCombat");
        secondNonCombat(moveUnits, moveRoutes, player, data);
        doMove(moveUnits, moveRoutes, null, moveDel);
        moveUnits.clear();
        moveRoutes.clear();

        s_logger.fine("populateFinalTransportUnload");
        populateFinalTransportUnload(data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);
        s_logger.fine("Finished NonCombat for: " + player.getName());

    }

    private void doCombatMove(IMoveDelegate moveDel, PlayerID player)
    {
        GameData data = getPlayerBridge().getGameData();
        getEdition();
        setImpassableTerrs(player);

        List<Collection<Unit>> moveUnits = new ArrayList<Collection<Unit>>();
        List<Route> moveRoutes = new ArrayList<Route>();
        List<Collection<Unit>> transportsToLoad = new ArrayList<Collection<Unit>>();
        determineCapDanger(player, data);
        s_logger.fine("Start Combat for: "+player.getName());
        
        //let sea battles occur before we load transports
        populateCombatMoveSea(data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);
        moveUnits.clear();
        moveRoutes.clear();

        s_logger.fine("populateTransportLoad");
        populateTransportLoad(false, data, moveUnits, moveRoutes, transportsToLoad, player);
        doMove(moveUnits, moveRoutes, transportsToLoad, moveDel);
		moveUnits.clear();
		moveRoutes.clear();

		s_logger.fine("protectOurAllies");
		protectOurAllies(true, data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);
        moveRoutes.clear();
        moveUnits.clear();

        s_logger.fine("Special Transport Move");
        specialTransportMove(data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);
        moveRoutes.clear();
        moveUnits.clear();
        
        s_logger.fine("Quick transport Unload");
        quickTransportUnload(data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);
        moveRoutes.clear();
        moveUnits.clear();

		s_logger.fine("Amphib Map Unload");
		amphibMapUnload(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveRoutes.clear();
		moveUnits.clear();

        s_logger.fine("firstTransportMove"); //probably don't need this anymore
        firstTransportMove(data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);
        moveRoutes.clear();
        moveUnits.clear();
        
        s_logger.fine("Populate Transport Move");
        populateTransportMove(data, moveUnits, moveRoutes, player);
//        simulatedTransportMove(data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);
		moveUnits.clear();
		moveRoutes.clear();
		
		s_logger.fine("Amphib Map Unload");
		amphibMapUnload(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
		moveRoutes.clear();
		moveUnits.clear();

		s_logger.fine("Populate Transport Unload");
		populateTransportUnload(data, moveUnits, moveRoutes, player);
		doMove(moveUnits, moveRoutes, null, moveDel);
        moveRoutes.clear();
        moveUnits.clear();

        //find second amphib target
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
        populateCombatMove(data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);
		moveUnits.clear();
		moveRoutes.clear();

        //any planes left for an overwhelming attack?
        specialPlaneAttack(data, moveUnits, moveRoutes, player);
        doMove(moveUnits, moveRoutes, null, moveDel);

    }

    protected void battle(IBattleDelegate battleDelegate, GameData data, PlayerID player)
    {
        //generally all AI's will follow the same logic.  
        
        //loop until all battles are fought.
        //rather than try to analyze battles to figure out which must be fought before others
        //as in the case of a naval battle preceding an amphibious attack,
        //keep trying to fight every battle
        while (true)
        {
    
            BattleListing listing = battleDelegate.getBattles();

            //all fought
            if(listing.getBattles().isEmpty() && listing.getStrategicRaids().isEmpty())
                return;
            
            Iterator<Territory> raidBattles = listing.getStrategicRaids().iterator();

            //fight strategic bombing raids
            while(raidBattles.hasNext())
            {
                Territory current = raidBattles.next();
                String error = battleDelegate.fightBattle(current, true);
                if(error != null)
                    s_logger.fine(error);
            }
            
            
            Iterator<Territory> nonRaidBattles = listing.getBattles().iterator();

            //fight normal battles
            while(nonRaidBattles.hasNext())
            {
                Territory current = nonRaidBattles.next();
                setBattleInfo(current);
                set_onOffense(true);
                String error = battleDelegate.fightBattle(current, false);
                set_onOffense(false);
                if(error != null)
                    s_logger.fine(error);
            }
            setBattleInfo(null);
        }
    }
    
    private void planesToCarriers(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {
    	CompositeMatch<Unit> ownedAC = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsCarrier);
    	CompositeMatch<Unit> nonTransportSeaUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSea, Matches.UnitIsNotTransport);
    	CompositeMatch<Unit> fighterUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanLandOnCarrier);
    	CompositeMatch<Unit> seaAttackUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.UnitIsNotTransport, HasntMoved);
    	CompositeMatch<Unit> seaAirAttackUnitNotMoved = new CompositeMatchOr<Unit>(seaAttackUnit, fighterUnit);
    	CompositeMatch<Territory> noNeutralOrAA = new CompositeMatchAnd<Territory>(SUtils.TerritoryIsNotImpassableToAirUnits(data), Matches.territoryHasEnemyAA(player, data).invert());
    	List<Territory> ACTerrs = SUtils.findCertainShips(data, player, Matches.UnitIsCarrier);
    	List<Territory> myFighterTerr = SUtils.findCertainShips(data, player, Matches.UnitCanLandOnCarrier);
    	List<Unit> alreadyMoved = new ArrayList<Unit>();
    	final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
    	for (Territory ACTerr : ACTerrs)
    	{
    		//are there fighters over water that we can help land?
    		List<Unit> ACUnits = ACTerr.getUnits().getMatches(ownedAC);
    		int carrierSpace = 0;
    		for (Unit carrier1 : ACUnits)
    		{
    			carrierSpace += UnitAttachment.get(carrier1.getType()).getCarrierCapacity();
    		}
    		List<Unit> ACFighters = ACTerr.getUnits().getMatches(fighterUnit);
    		int fighterSpaceUsed = 0;
    		for (Unit fighter1 : ACFighters)
    			fighterSpaceUsed += UnitAttachment.get(fighter1.getType()).getCarrierCost();
			int availSpace = carrierSpace - fighterSpaceUsed;
			ACUnits.removeAll(alreadyMoved);
			if (ACUnits.size() == 0 || availSpace <= 0)
				continue;
    		for (Territory f : myFighterTerr)
    		{
    			if (f == ACTerr)
    				continue;
				if (Matches.TerritoryIsLand.match(f) && SUtils.doesLandExistAt(f, data, false))
				{
					Set<Territory> nextNeighbors = data.getMap().getNeighbors(f, Matches.territoryHasNoAlliedUnits(player, data).invert());
					for (Territory nTerr : nextNeighbors)
					{
						float strength = SUtils.strength(nTerr.getUnits().getUnits(), false, false, true);
						float eStrength = SUtils.getStrengthOfPotentialAttackers(nTerr, data, player, true, true, null);
						if (strength > eStrength)
							continue;
					}	
				}
				Territory fTerr = f;
				if (Matches.TerritoryIsLand.match(f) && delegate.getBattleTracker().wasBattleFought(f)) //might be an island
					fTerr = SUtils.getClosestWaterTerr(f, ACTerr, data, player, false);

//				Route fACRoute = SUtils.getMaxSeaRoute(data, ACTerr, fTerr, player, false);
				Route fACRoute = data.getMap().getWaterRoute(ACTerr, fTerr);
				if (fACRoute == null)
					continue;
				List<Unit> myFighters = f.getUnits().getMatches(fighterUnit);
				myFighters.removeAll(alreadyMoved);
				if (myFighters.isEmpty())
					continue;
				IntegerMap<Unit> fighterMoveMap = new IntegerMap<Unit>();
				for (Unit f1 : myFighters)
				{
					fighterMoveMap.put(f1, TripleAUnit.get(f1).getMovementLeft());
				}
				SUtils.reorder(myFighters, fighterMoveMap, false);
				int fACDist = fACRoute.getLength();
				int fightMove = MoveValidator.getMaxMovement(myFighters);
				if (MoveValidator.canLand(myFighters, ACTerr, player, data))
				{
					Route fACRoute2 = data.getMap().getRoute(f, ACTerr, noNeutralOrAA);
					if (fACRoute2 != null)
					{
						moveUnits.add(myFighters);
						moveRoutes.add(fACRoute2);
						alreadyMoved.addAll(myFighters);
						alreadyMoved.addAll(ACUnits);
					}
					continue;
				}
				int ACMove = MoveValidator.getLeastMovement(ACUnits);
				Route fACRoute2 = SUtils.getMaxSeaRoute(data, ACTerr, fTerr, player, false, ACMove);
				if (fACRoute2 == null || fACRoute2.getEnd() == null)
					continue;
				Territory targetTerr = fACRoute2.getEnd();
				Route fighterRoute = data.getMap().getRoute(f, targetTerr, noNeutralOrAA);
				if (fighterRoute == null)
					continue;
				if (fACDist <= (fightMove + ACMove)) //move carriers and fighters
				{
					Iterator<Unit> fighterIter = myFighters.iterator();
					List<Unit> fighters = new ArrayList<Unit>();
					while (fighterIter.hasNext())
					{
						Unit fighter = fighterIter.next();
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

    private void checkUnmovedTransports(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {
    	Collection<Territory> impassableTerrs = getImpassableTerrs();
        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		//Simply: Move Transports Back Toward a Factory
		CompositeMatch<Unit> transUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport);
		CompositeMatch<Unit> ourTransUnit = new CompositeMatchAnd<Unit>(transUnit, Matches.unitIsOwnedBy(player), Matches.transportIsNotTransporting(), HasntMoved);
		List<Territory> transTerr = SUtils.findCertainShips(data, player, ourTransUnit);
		CompositeMatch<Unit> ourTransUnit2 = new CompositeMatchAnd<Unit>(transUnit, Matches.unitIsOwnedBy(player), Matches.transportIsTransporting());
		List<Territory> ourFactories = SUtils.findCertainShips(data, player, Matches.UnitIsFactory);
		List<Territory> ourSeaSpots = new ArrayList<Territory>();
		CompositeMatch<Unit> ourLandUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand, Matches.UnitIsNotFactory);
		CompositeMatch<Unit> escortUnit = new CompositeMatchOr<Unit>(Matches.UnitIsSea, Matches.alliedUnit(player, data), Matches.UnitIsNotTransport);
		CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsAir);
		CompositeMatch<Unit> escortAirUnit = new CompositeMatchOr<Unit>(escortUnit, airUnit);
		CompositeMatch<Territory> enemyLand = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotNeutral, Matches.territoryHasEnemyUnits(player, data), Matches.TerritoryHasProductionValueAtLeast(1));
		CompositeMatch<Territory> noenemyWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data));
//		List<Territory> unmovedTransportTerrs = SUtils.findCertainShips(data, player, ourTransUnit2);
		if (transTerr.isEmpty())
			return;
		List<Unit> alreadyMoved = new ArrayList<Unit>();
    	boolean tFirst = transportsMayDieFirst();
    	List<PlayerID> ePlayers = SUtils.getEnemyPlayers(data, player);
    	List<Territory> ourEnemyTerr = new ArrayList<Territory>();
    	List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
    	HashMap<Territory, Float> landTerrMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, tFirst, true, true);
    	List<Territory> moveToTerr = new ArrayList<Territory>();
    	moveToTerr.addAll(ourEnemyTerr);
    	moveToTerr.addAll(ourFriendlyTerr);
		IntegerMap <Territory> ourFacts = new IntegerMap<Territory>();
		HashMap <Territory, Territory> connectTerr = new HashMap<Territory, Territory>();
		for (Territory xT : ourFactories)
		{
			int numUnits = xT.getUnits().getMatches(ourLandUnit).size() + TerritoryAttachment.get(xT).getProduction();
			Set<Territory> factNeighbors = data.getMap().getNeighbors(xT, Matches.TerritoryIsWater);
			ourFacts.put(xT, numUnits);
			for (Territory factTest : factNeighbors)
				connectTerr.put(factTest, xT);
			ourSeaSpots.addAll(factNeighbors);
		}
		//check for locations with units and no threat or real way to use them
		List<Territory> allTerr = SUtils.allAlliedTerritories(data, player);
		allTerr.removeAll(ourFactories);
		List<Territory> otherNonSeaSpots = new ArrayList<Territory>();
		for (Territory xT2 : allTerr)
		{
			boolean hasWater = SUtils.isWaterAt(xT2, data);
			Route nearestRoute = SUtils.findNearest(xT2, Matches.territoryHasEnemyUnits(player, data), Matches.isTerritoryAllied(player, data), data);
			if (hasWater && (nearestRoute == null || nearestRoute.getLength() > 4 || nearestRoute.crossesWater())) //bad guys are far away...
				otherNonSeaSpots.add(xT2);
		}

		int minDist = 100;
		Territory closestT = null;
		for (Territory t : transTerr)
		{
			List<Unit> ourTransports = t.getUnits().getMatches(ourTransUnit);
			ourTransports.removeAll(alreadyMoved);
			if (ourTransports.isEmpty())
				continue;
			int maxTransDistance = MoveValidator.getLeastMovement(ourTransports);
			IntegerMap<Territory> distMap = new IntegerMap<Territory>();
			IntegerMap<Territory> unitsMap = new IntegerMap<Territory>(); 
			if (!ourSeaSpots.contains(t))
			{
				for (Territory t2 : ourSeaSpots)
				{
					Route thisRoute = SUtils.getMaxSeaRoute(data, t, t2, player, false, maxTransDistance);
					int thisDist = 0;
					if (thisRoute == null)
						thisDist = 100;
					else
						thisDist = thisRoute.getLength();
					int numUnits = t2.getUnits().getMatches(ourLandUnit).size()+6; //assume new units on the factory
					distMap.put(t2, thisDist);
					unitsMap.put(t2, numUnits);
				}
			}
			for (Territory checkAnother : otherNonSeaSpots)
			{
				int thisDist = 0;
				int numUnits = checkAnother.getUnits().getMatches(ourLandUnit).size();
				Territory qTerr = SUtils.getClosestWaterTerr(checkAnother, t, data, player, false);
				thisDist = data.getMap().getWaterDistance(t, qTerr);
				if (thisDist == -1)
					thisDist = 100;
				connectTerr.put(qTerr, checkAnother);
				distMap.put(qTerr, thisDist);
				unitsMap.put(qTerr, numUnits);
			}
			Set<Territory> allWaterTerr = distMap.keySet();
			int score = 0, bestScore = 0;
			for (Territory waterTerr : allWaterTerr)
			{
				//figure out the best Territory to send it to
				score = unitsMap.getInt(waterTerr) - distMap.getInt(waterTerr);
				int numTrans = waterTerr.getUnits().getMatches(ourTransUnit).size();
				if (waterTerr == t)
				{
					Territory landTerr = connectTerr.get(t);
					int moveNum = (numTrans*2 - unitsMap.getInt(waterTerr))/2;
					int score2 = 0, bestScore2 = 0;
					Territory newTerr = null;
					for (Territory waterTerr2 : allWaterTerr)
					{
						if (waterTerr2 == waterTerr || landTerr == connectTerr.get(waterTerr2))
							continue;
						score2 = unitsMap.getInt(waterTerr2)-distMap.getInt(waterTerr2);
						if (score2 > bestScore2)
						{
							bestScore2 = score2;
							newTerr = waterTerr2;
						}
					}
					Route goRoute = SUtils.getMaxSeaRoute(data, t, newTerr, player, false, maxTransDistance);
					if (goRoute == null)
						continue;
					List<Unit> tmpTrans = new ArrayList<Unit>();
					for (int i=0; i < Math.min(moveNum, numTrans); i++)
					{
						Unit trans2 = ourTransports.get(i);
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
				int maxTDist = MoveValidator.getLeastMovement(ourTransports);
				Route ourRoute = SUtils.getMaxSeaRoute(data, t, closestT, player, false, maxTDist);
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
*/				moveUnits.add(ourTransports);
				moveRoutes.add(ourRoute);
			}
			minDist = 100;
			closestT = null;
		}
	}

    private void populateTransportLoad(boolean nonCombat, GameData data,
    		List<Collection<Unit>> moveUnits, List<Route> moveRoutes, List<Collection<Unit>> transportsToLoad, PlayerID player)
    {

        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();

        CompositeMatch<Unit> owned = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));

        CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(owned, Matches.UnitCanBeTransported, Matches.UnitIsNotAA, Matches.UnitIsNotFactory);
        CompositeMatch<Unit> transUnit = new CompositeMatchAnd<Unit>(owned, Matches.UnitIsTransport);
        CompositeMatch<Unit> factoryUnit = new CompositeMatchAnd<Unit>(owned, Matches.UnitIsFactory);
        CompositeMatch<Unit> enemyFactoryUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsFactory);
        List<Territory> myTerritories = SUtils.allAlliedTerritories(data, player);
        CompositeMatch<Territory> enemyAndNoWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player), Matches.isTerritoryEnemyAndNotNuetralWater(player, data));
        CompositeMatch<Territory> noEnemyOrWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player), Matches.isTerritoryAllied(player, data));
        List<Territory> transTerr = SUtils.findCertainShips(data, player, Matches.UnitIsTransport);
        if (transTerr.isEmpty())
        	return;
        Territory capitol = TerritoryAttachment.getCapital(player, data);
        boolean ownMyCapitol = capitol.getOwner().equals(player);
        Unit transport = null;

		int badGuyDist = 0, badGuyFactDist = 0;
        //start at our factories
        List<Territory> factTerr = SUtils.findUnitTerr(data, player, factoryUnit);
        if (!factTerr.contains(capitol) && ownMyCapitol)
        	factTerr.add(capitol);
        List<Unit> transportsFilled = new ArrayList<Unit>();
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
*/		for (Territory checkThis : myTerritories)
		{
		   Route xRoute = null;
		   boolean landRoute = SUtils.landRouteToEnemyCapital(checkThis, xRoute, data, player);
		   boolean isLand = SUtils.doesLandExistAt(checkThis, data, true);
           List<Unit> unitsTmp = checkThis.getUnits().getMatches(landUnit);
           List<Unit> unitsToLoad = SUtils.sortTransportUnits(unitsTmp);
           if (unitsToLoad.size() == 0)
               continue;
           int maxMovement = MoveValidator.getMaxMovement(unitsToLoad);
           List<Territory> blockThese = new ArrayList<Territory>();
		   Set<Territory> xNeighbors = data.getMap().getNeighbors(checkThis);
		   if (!isLand)
		   {
			  for (Territory islandWaterTerr : xNeighbors)
			  {
				  
//              Territory myIsland = xNeighbors.get(0);
				  List<Unit> units = new ArrayList<Unit>();
				  List<Unit> transportUnits = islandWaterTerr.getUnits().getMatches(transUnit);
				  transportUnits.removeAll(transportsFilled);
				  List<Unit> finalTransUnits = new ArrayList<Unit>();
				  if (transportUnits.size() == 0)
					  continue;
				  int tCount = transportUnits.size();
				  for (int t1=tCount-1; t1>=0; t1--)
				  {
					  Unit trans1 = transportUnits.get(t1);
					  int tFree = tracker.getAvailableCapacity(trans1);
					  if (tFree <=0)
					  {
						  transportUnits.remove(t1);
						  tCount--;
						  continue;
					  }
					  Iterator<Unit> tIter = unitsToLoad.iterator();
					  boolean moveOne = false;
					  while (tIter.hasNext() && tFree > 0)
					  {
						  Unit current = tIter.next();
						  UnitAttachment ua = UnitAttachment.get(current.getType());
						  int howMuch = ua.getTransportCost();
						  if (tFree < howMuch)
							  continue;
						  tIter.remove();
						  tFree -= howMuch;
						  units.add(current);
						  moveOne = true;
					  }
					  if (moveOne)
						  finalTransUnits.add(trans1);
				  }
				  if(units.size() > 0)
				  {
					  Route route = data.getMap().getRoute(checkThis, islandWaterTerr);
					  moveUnits.add(units );
					  moveRoutes.add(route);
					  transportsToLoad.add( finalTransUnits);
					  transportsFilled.addAll( finalTransUnits);
				  }
				  continue;
			  }
		   }
		   if (isLand)
		   {
			   CompositeMatch<Territory> noWaterEnemy = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player), Matches.isTerritoryEnemyAndNotNuetralWater(player, data));
			   CompositeMatch<Territory> noWaterAllied = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassableToLandUnits(player), Matches.isTerritoryAllied(player, data));
			   Route badGuyDR = SUtils.findNearest(checkThis, noWaterEnemy, noWaterAllied, data);
			   Route badGuyFactory = SUtils.findNearest(checkThis, Matches.territoryHasEnemyFactory(data, player), Matches.TerritoryIsNotImpassableToLandUnits(player), data);
			   boolean noWater = true;
			   if (badGuyFactory == null)
				   badGuyFactDist = 100;
			   else
				   badGuyFactDist = badGuyFactory.getLength();
			   if (badGuyDR == null)
				   badGuyDist = 100;
			   else
			   {
				   noWater = SUtils.RouteHasNoWater(badGuyDR);
				   badGuyDist = badGuyDR.getLength();
			   }
			   if (landRoute)
				   badGuyDist--; //less likely if we have a land Route to Capital from here
			   if (badGuyFactDist <= 4*maxMovement && badGuyDist <= 3*maxMovement)
				   continue;
		   }
		   //TODO: Track transports that only received 1 unit
           for(Territory neighbor : xNeighbors)
           {
			  if (!neighbor.isWater())
			     continue;
              List<Unit> units = new ArrayList<Unit>();
              List<Unit> transportUnits = neighbor.getUnits().getMatches(transUnit);
              transportUnits.removeAll(transportsFilled);
              List<Unit> finalTransUnits = new ArrayList<Unit>();

              if (transportUnits.size()==0)
                 continue;
		      int transCount = transportUnits.size();
              for (int j=transCount-1; j >= 0; j--)
              {
		         transport = transportUnits.get(j);
                 int free = tracker.getAvailableCapacity(transport);
                 if (free <= 0)
                 {
		            transportUnits.remove(j);
					transCount--;
           	        continue;
		         }
                 Iterator<Unit> iter = unitsToLoad.iterator();
                 boolean addOne = false;
                 while(iter.hasNext() && free > 0)
                 {
                    Unit current = iter.next();
           	        UnitAttachment ua = UnitAttachment.get(current.getType());
           	        if(ua.isAir())
           	            continue;
           	        if(ua.getTransportCost() <= free)
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

	          if(units.size() > 0)
	          {
			     Route route = data.getMap().getRoute(checkThis, neighbor);
	             moveUnits.add(units );
	             moveRoutes.add(route);
	             transportsToLoad.add( finalTransUnits);
	             transportsFilled.addAll( finalTransUnits);
	             unitsToLoad.removeAll(units);
	          }
		   }
        }
    }
    

    private void populateNonComTransportMove(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {
    	boolean tFirst = transportsMayDieFirst();
        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		CompositeMatch<Unit> enemyUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data));
		CompositeMatch<Unit> landAndEnemy = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, enemyUnit);
		CompositeMatch<Unit> airEnemyUnit = new CompositeMatchAnd<Unit>(enemyUnit, Matches.UnitIsAir);
		CompositeMatch<Unit> transUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport);
		CompositeMatch<Unit> ourTransUnit = new CompositeMatchAnd<Unit>(transUnit, Matches.unitIsOwnedBy(player), HasntMoved);
		CompositeMatch<Unit> escortUnits = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSea, Matches.UnitIsNotTransport);

		List<Territory> transTerr2 = SUtils.findCertainShips(data, player, Matches.UnitIsTransport);
		if (transTerr2.isEmpty())
			return;
		HashMap<Territory, Float> rankMap = SUtils.rankAmphibReinforcementTerritories(data, null, player, tFirst);
//		s_logger.fine("Amphib Terr Rank: "+rankMap);
		List<Territory> targetTerrs = new ArrayList<Territory>(rankMap.keySet());
		List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
		List<PlayerID> ePlayers = SUtils.getEnemyPlayers(data, player);
		Route amphibRoute = getAmphibRoute(player, true);
		boolean isAmphib = isAmphibAttack(player, false);
		if (isAmphib && amphibRoute != null && amphibRoute.getEnd() != null)
		{
			Territory quickDumpTerr = amphibRoute.getEnd();
			float remainingStrengthNeeded = 1000.0F;
			SUtils.inviteTransports(true, quickDumpTerr, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, tFirst, false, null);
		}
		PlayerID ePlayer = ePlayers.get(0);
		float distanceFactor = 0.85F;
	    //Target allied territories next to a bad guy
		List<Territory> allAlliedWithEnemyNeighbor = SUtils.getTerritoriesWithEnemyNeighbor(data, player, true, false);
		allAlliedWithEnemyNeighbor.retainAll(targetTerrs);
		SUtils.reorder(allAlliedWithEnemyNeighbor, rankMap, true);
		for (Territory aT : allAlliedWithEnemyNeighbor)
		{
			SUtils.inviteTransports(true, aT, 1000.0F, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, tFirst, false, null);
		}
		if (amphibRoute != null)
		{
			for (Territory tT : transTerr2)
			{
				Territory amphibDockTerr = amphibRoute.at(amphibRoute.getLength()-1);
				if (amphibDockTerr != null)
				{
					List<Unit> transUnits = tT.getUnits().getMatches(Matches.transportIsTransporting());
					transUnits.removeAll(unitsAlreadyMoved);
					if (transUnits.isEmpty())
						continue;
					int tDist = MoveValidator.getMaxMovement(transUnits);
					Route dockSeaRoute = SUtils.getMaxSeaRoute(data, tT, amphibDockTerr, player, false, tDist);
					if (dockSeaRoute == null)
						continue;
					Iterator<Unit> tIter = transUnits.iterator();
					List<Unit> addUnits = new ArrayList<Unit>();
					while (tIter.hasNext())
					{
						Unit transport = tIter.next();
						if (MoveValidator.hasEnoughMovement(transport, dockSeaRoute))
						{
							addUnits.add(transport);
							addUnits.addAll(tracker.transporting(transport));
						}
					}
					moveUnits.add(addUnits);
					moveRoutes.add(dockSeaRoute);
					unitsAlreadyMoved.addAll(addUnits);
				}
			}
		}
		
		for (Territory t : transTerr2)
		{
			/*
			 * 1) Determine our available loaded units
			 */
			int distanceToEnemy = 100;
			Route enemyDistanceRoute = SUtils.findNearestNotEmpty(t, Matches.isTerritoryEnemyAndNotNeutral(player, data), Matches.TerritoryIsNotImpassable, data);
			if (enemyDistanceRoute != null)
				distanceToEnemy = enemyDistanceRoute.getLength()+2; //give it some room
			List<Unit> ourLandingUnits = new ArrayList<Unit>();
            List<Unit> mytrans = t.getUnits().getMatches(ourTransUnit);
            mytrans.removeAll(unitsAlreadyMoved);
            Iterator<Unit> mytransIter = mytrans.iterator();
            while (mytransIter.hasNext())
            {
            	Unit thisTrans = mytransIter.next();
            	if (tracker.isTransporting(thisTrans))
            		ourLandingUnits.addAll(tracker.transporting(thisTrans));
            	else
            	{
            		mytransIter.remove();
            	}
            }
			if (mytrans.isEmpty())
				continue;
			HashMap<Territory, Float> rankMap2 = new HashMap<Territory, Float>(rankMap);
			for (Territory target : targetTerrs)
			{
				Float rankValue = rankMap2.get(target);
				Territory newGoTerr = SUtils.getSafestWaterTerr(target, t, null, data, player, false, tFirst);
				if (newGoTerr == null)
					continue;
				int thisDist = data.getMap().getWaterDistance(t, newGoTerr);
				float multiplier = (float)Math.exp(distanceFactor*(thisDist - 2));
				
				if (thisDist > 2)
				{
						rankValue -= rankValue*multiplier;
						rankMap2.put(target, rankValue);
				}
			}
			SUtils.reorder(targetTerrs, rankMap2, true);
			Territory targetCap = SUtils.closestEnemyCapital(t, data, player);
			int tDistance = MoveValidator.getMaxMovement(mytrans);
			Iterator<Territory> tTIter = targetTerrs.iterator();
			while (tTIter.hasNext() && !mytrans.isEmpty())
			{
				Territory target = tTIter.next();
				Set<Territory> targetNeighbors = data.getMap().getNeighbors(target, Matches.TerritoryIsWater);
				if (targetNeighbors.contains(t))
				{
					unitsAlreadyMoved.addAll(mytrans);
					mytrans.clear();
					continue;
				}
				Territory seaTarget = SUtils.getSafestWaterTerr(target, t, null, data, player, false, tFirst);
				if (seaTarget == null)
					continue;
				Route seaRoute = SUtils.getMaxSeaRoute(data, t, seaTarget, player, false, tDistance);
				if (seaRoute == null)
					continue;
				Iterator<Unit> transIter = mytrans.iterator();
				List<Unit> moveTransports = new ArrayList<Unit>();
				List<Unit> moveLoad = new ArrayList<Unit>();
				while (transIter.hasNext())
				{
					Unit transport = transIter.next();
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
				}
			}
		}
	}
    /**
     * Unloads a transport into an unoccupied enemy Territory
     * Useful to update AI on true enemy territories for other transport moves
     * @param data
     * @param moveUnits
     * @param moveRoutes
     * @param player
     */
    
    private void quickTransportUnload(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {
        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
    	CompositeMatch<Unit> loadedTransport = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsTransport, Matches.transportIsTransporting());
    	CompositeMatch<Territory> friendlyWaterTerr = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasUnitsOwnedBy(player));
    	CompositeMatch<Territory> landPassable = new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsPassableAndNotRestricted( player));
    	CompositeMatch<Territory> enemyLandWithWater = new CompositeMatchAnd<Territory>(landPassable, Matches.isTerritoryEnemy(player, data), Matches.territoryHasWaterNeighbor(data));
    	CompositeMatch<Unit> myBlitzUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBlitz);
    	CompositeMatch<Unit> myLandUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand);
    	List<Territory> emptyEnemyTerrs = new ArrayList<Territory>();
    	List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
    	for (Territory emptyEnemyTerr : data.getMap().getTerritories())
    	{
    		if (enemyLandWithWater.match(emptyEnemyTerr) && 
    				Matches.territoryIsEmptyOfCombatUnits(data, player).match(emptyEnemyTerr) && 
    				Matches.TerritoryIsNotImpassable.match(emptyEnemyTerr))
    					emptyEnemyTerrs.add(emptyEnemyTerr);
    	}
    	for (Territory enemyTerr : emptyEnemyTerrs)
    	{
    		Set<Territory> transTerrs = data.getMap().getNeighbors(enemyTerr, friendlyWaterTerr);
    		List<Territory> landNeighborTerrs = SUtils.getNeighboringLandTerritories(data, player, enemyTerr);
    		Iterator<Territory> lNIter = landNeighborTerrs.iterator();
    		while (lNIter.hasNext())
    		{
    			Territory thisTerr = lNIter.next();
    			float eAttackStrength = SUtils.getStrengthOfPotentialAttackers(enemyTerr, data, player, false, true, null);
    			float defenseStrength = SUtils.strength(thisTerr.getUnits().getUnits(), false, false, false);
    			if (eAttackStrength > (defenseStrength*1.10F - 2.0F) || 
    					(Matches.territoryHasAlliedFactory(data, player).match(thisTerr) && eAttackStrength > defenseStrength))
    			{
    				lNIter.remove();
    				continue;
    			}
    			else if (Matches.territoryHasAlliedFactoryNeighbor(data, player).match(thisTerr))
    			{
    				Set<Territory> myFactNeighbors = data.getMap().getNeighbors(thisTerr, Matches.territoryHasAlliedFactory(data, player));
    				for (Territory newTerr : myFactNeighbors)
    				{
    					float eAttackStrength2 = SUtils.getStrengthOfPotentialAttackers(newTerr, data, player, false, true, null);
    					float defenseStrength2 = SUtils.strength(newTerr.getUnits().getUnits(), false, false, false);
    					if (eAttackStrength2 > defenseStrength2) {
    						lNIter.remove();
    						break;
    					}
    				}
    			}
    		}
    		Iterator<Territory> lNIter2 = landNeighborTerrs.iterator();
    		boolean attacked = false;
    		//Land Blitz Unit preferred
    		while (lNIter2.hasNext() && !attacked)
    		{
    			Territory thisTerr = lNIter2.next();
    			List<Unit> myBlitzers =  thisTerr.getUnits().getMatches(myBlitzUnit);
    			myBlitzers.removeAll(unitsAlreadyMoved);
    			if (!myBlitzers.isEmpty())
    			{
    				Unit blitzUnit = myBlitzers.get(0);
    				Route goRoute = data.getMap().getRoute(thisTerr, enemyTerr);
    				if (goRoute != null)
    				{
    					moveUnits.add(Collections.singletonList(blitzUnit));
    					moveRoutes.add(goRoute);
    					unitsAlreadyMoved.add(blitzUnit);
     					attacked = true;
    				}
    			}    			
    		}
    		//one more pass for non-blitz Unit
    		Iterator<Territory> lNIter3 = landNeighborTerrs.iterator();
    		while (lNIter3.hasNext() && !attacked)
    		{
    			Territory thisTerr = lNIter3.next();
    			List<Unit> myAttackers = thisTerr.getUnits().getMatches(myLandUnit);
    			myAttackers.removeAll(unitsAlreadyMoved);
    			if (!myAttackers.isEmpty())
    			{
    				Unit attackUnit = myAttackers.get(0);
    				Route goRoute = data.getMap().getRoute(thisTerr, enemyTerr);
    				if (goRoute != null)
    				{
    					moveUnits.add(Collections.singletonList(attackUnit));
    					moveRoutes.add(goRoute);
    					unitsAlreadyMoved.add(attackUnit);
    					attacked = true;
    				}
    			}
    		}
    		Iterator<Territory> transIter = transTerrs.iterator();
    		while (transIter.hasNext() && !attacked)
    		{
    			Territory transTerr = transIter.next();
    			List<Unit> loadedTransports = transTerr.getUnits().getMatches(loadedTransport);
    			loadedTransports.removeAll(unitsAlreadyMoved);
    			if (!loadedTransports.isEmpty())
    			{
    				Unit transport = loadedTransports.get(0);
    				Collection<Unit> loadedUnits = TripleAUnit.get(transport).getTransporting();
    				Route dumpRoute = data.getMap().getRoute(transTerr, enemyTerr);
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
     * @param data
     * @param moveUnits
     * @param moveRoutes
     * @param player
     */
    private void specialTransportMove(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {
//    	Collection<Territory> impassableTerrs = getImpassableTerrs();
        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		HashMap<Territory, Territory> amphibMap = new HashMap<Territory, Territory>();
        CompositeMatch<Unit> transUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.transportIsTransporting());
    	CompositeMatch<Territory> landPassable = new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsPassableAndNotRestricted( player));
    	CompositeMatch<Territory> enemyEmpty = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemy(player, data),Matches.territoryHasEnemyLandUnits(player, data).invert(), landPassable, Matches.territoryHasWaterNeighbor(data));
        CompositeMatch<Territory> enemyTarget = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemy(player, data), Matches.territoryHasWaterNeighbor(data), Matches.TerritoryIsNotImpassable);
    	List<Territory> transTerr = SUtils.findCertainShips(data, player, transUnit);
    	if (transTerr.isEmpty())
    	{
    		s_logger.fine("Transports not found on entire map for player: "+player.getName());
    		return;
    	}
    	List<Territory> seaTerrAttacked = getSeaTerrAttacked();
    	boolean tFirst = transportsMayDieFirst();
    	List<Territory> inRangeTerr = new ArrayList<Territory>();
    	List<Territory> oneUnitTerr = new ArrayList<Territory>();
    	for (Territory startTerr : transTerr)
    	{
    		Set<Territory> enemyTerr = data.getMap().getNeighbors(startTerr, 3);
    		for (Territory eCheck : enemyTerr)
    		{
    			if (!inRangeTerr.contains(eCheck) && enemyEmpty.match(eCheck))
    	    		inRangeTerr.add(eCheck);
    			else if (!oneUnitTerr.contains(eCheck) && enemyTarget.match(eCheck) && eCheck.getUnits().countMatches(Matches.UnitIsLand) < 3 && !SUtils.doesLandExistAt(eCheck, data, false))
    				oneUnitTerr.add(eCheck); //islands are easy for the ranking system to miss
    		}
    	}
    	if (inRangeTerr.isEmpty() && oneUnitTerr.isEmpty())
    		return;
    	List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
    	List<Territory> ourEnemyTerr = new ArrayList<Territory>();
    	HashMap<Territory, Float> rankMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, seaTerrAttacked, player, tFirst, false, false);

    	Set<Territory> rankTerr = rankMap.keySet();
    	inRangeTerr.retainAll(rankTerr); //should clear out any neutrals that are not common between the two
    	oneUnitTerr.retainAll(rankTerr);
//    	s_logger.fine("Attackable Territories: "+inRangeTerr);
//    	s_logger.fine("Invadable Territories: "+oneUnitTerr);
    	SUtils.reorder(inRangeTerr, rankMap, true);
    	/*
    	 * RankTerritories heavily emphasizes the land based attacks. One Unit Terr will get the amphib attacker
    	 * going after easy to take islands...using RankTerr, but not comparing to territories which have a direct
    	 * line to an enemy cap. Adjust the number of units allowed in the count for landUnits.
    	 */
    	SUtils.reorder(oneUnitTerr, rankMap, true);
    	List<Territory> landTerrConquered = new ArrayList<Territory>();
    	List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
    	Route goRoute = new Route();
    	for (Territory landTerr : inRangeTerr)
    	{
    		float eAttackPotential = SUtils.getStrengthOfPotentialAttackers(landTerr, data, player, tFirst, true, landTerrConquered);
    		PlayerID ePlayer = landTerr.getOwner();
    		float myAttackPotential = SUtils.getStrengthOfPotentialAttackers(landTerr, data, ePlayer, tFirst, true, null);
    		myAttackPotential += TerritoryAttachment.get(landTerr).getProduction()*2;
//    		if (myAttackPotential < (0.75F*eAttackPotential - 3.0F))
//    			continue;
    		for (Territory sourceTerr : transTerr)
    		{
    			Territory goTerr = SUtils.getSafestWaterTerr(landTerr, sourceTerr, seaTerrAttacked, data, player, false, tFirst);
    			if (goTerr == null)
    			{
    				goTerr = SUtils.getClosestWaterTerr(landTerr, sourceTerr, data, player, false);
    				if (goTerr == null)
    					continue;
    			}
    			List<Unit> transports = sourceTerr.getUnits().getMatches(transUnit);
    			transports.removeAll(unitsAlreadyMoved);
    			if (transports.isEmpty())
    				continue;
    			int tDist = MoveValidator.getMaxMovement(transports);
    			goRoute = SUtils.getMaxSeaRoute(data, sourceTerr, goTerr, player, false, tDist);
    			if (goRoute == null || goRoute.getEnd() != goTerr)
    				continue;
    			Collection<Unit> unitsToMove = new ArrayList<Unit>();
    			if (Matches.territoryHasEnemyFactoryNeighbor(data, player).match(goTerr) || Matches.territoryHasEnemyFactoryNeighbor(data, player).match(landTerr))
    			{
    				unitsToMove.addAll(transports);
    				for (Unit transport : transports)
    					unitsToMove.addAll(tracker.transporting(transport));
    			}
    			else
    			{
    				Unit oneTransport = transports.get(0);
    				unitsToMove.addAll(tracker.transporting(oneTransport));
        			unitsToMove.add(oneTransport);
    			}
    			landTerrConquered.add(landTerr);
    			moveUnits.add(unitsToMove);
    			moveRoutes.add(goRoute);
    			unitsAlreadyMoved.addAll(unitsToMove);
    		}
    	}
    	for (Territory easyTerr : oneUnitTerr)
    	{
    		float eStrength = SUtils.strength(easyTerr.getUnits().getMatches(Matches.enemyUnit(player, data)), false, false, tFirst);
    		float strengthNeeded = eStrength*1.35F + 3.0F;
    		List<Collection<Unit>> xMoveUnits = new ArrayList<Collection<Unit>>();
    		List<Route> xMoveRoutes = new ArrayList<Route>();
    		List<Unit> xAlreadyMoved = new ArrayList<Unit>(unitsAlreadyMoved);
    		float transStrength = SUtils.inviteTransports(false, easyTerr, strengthNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, tFirst, true, seaTerrAttacked);
    		int transCount = 0;
    		for (Collection<Unit> xCollection : xMoveUnits)
    		{
    			transCount += xCollection.size();
    		}
    		int routeNumbers = xMoveRoutes.size();
    		Territory landingTerr = null;
    		//for now, just target the last route's endpoint
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
    		float myAttackStrength = BBStrength + transStrength + planeStrength;
    		if (myAttackStrength > (eStrength + 1.0F))
    		{
    			moveUnits.addAll(xMoveUnits);
    			moveRoutes.addAll(xMoveRoutes);
    			Iterator<Route> routeIter = moveRoutes.iterator();
    			while (routeIter.hasNext())
    			{
    				Route checkRoute = routeIter.next();
    				Territory tTerr = checkRoute.getEnd();
    				amphibMap.put(tTerr, easyTerr);
    			}
    			unitsAlreadyMoved.addAll(xAlreadyMoved);
    			landTerrConquered.add(easyTerr);
    			if (landingTerr != null)
    			{
    				float seaPotential = SUtils.getStrengthOfPotentialAttackers(landingTerr, data, player, tFirst, false, seaTerrAttacked);
    				float mySeaStrength = tFirst ? transCount : 0;
    				mySeaStrength += BBStrength;
    				if (mySeaStrength < seaPotential*0.80F - 2.0F)
    				{ //don't subtract 2 if we have no strength yet...it might leave transports with no protection
    					float remainingStrengthNeeded = mySeaStrength > 0.50F ? seaPotential*0.80F - 2.0F - mySeaStrength : seaPotential*0.80F;
    					SUtils.inviteShipAttack(landingTerr, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, false, tFirst, false);
    				}
    			}
    		}
    	}
    	setAmphibMap(amphibMap);
    	setLandTerrAttacked(landTerrConquered);
    }
    private void firstTransportMove(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {
        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		CompositeMatch<Unit> transUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport);
		CompositeMatch<Unit> transportingUnit = new CompositeMatchAnd<Unit>(transUnit, Matches.unitIsOwnedBy(player), Matches.transportIsTransporting());
		CompositeMatch<Unit> escortUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsNotTransport, Matches.UnitIsCarrier.invert());
    	CompositeMatch<Territory> landPassable = new CompositeMatchAnd<Territory>(Matches.TerritoryIsLand, Matches.TerritoryIsPassableAndNotRestricted( player));
		CompositeMatch<Territory> endOfRoute = new CompositeMatchAnd<Territory>(landPassable, Matches.territoryHasRouteToEnemyCapital(data, player));
		CompositeMatch<Territory> routeCondition = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater);
		List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
		Territory capitol = TerritoryAttachment.getCapital(player, data);
		
		if (!isAmphibAttack(player, false) || !Matches.territoryHasWaterNeighbor(data).match(capitol))
			return;
		
		Set<Territory> waterCapNeighbors = data.getMap().getNeighbors(capitol, Matches.TerritoryIsWater);
		if (waterCapNeighbors.isEmpty()) //should not happen, but just in case on some wierd map
			return;
		Territory waterCap = waterCapNeighbors.iterator().next();
		Route goRoute = SUtils.findNearest(waterCap, Matches.isTerritoryEnemyAndNotNuetralWater(player, data), routeCondition, data);
//		s_logger.fine("First Transport move Route: "+ goRoute);
		if (goRoute == null || goRoute.getEnd() == null)
			return;
		Territory endTerr = goRoute.getEnd();
		List<Territory> waterTerrs = SUtils.getExactNeighbors(endTerr, 6, player, false);
		Set<Territory> xWaterTerrs = data.getMap().getNeighbors(endTerr, 3);
		waterTerrs.removeAll(xWaterTerrs);
		Iterator<Territory> wIter = waterTerrs.iterator();
		while (wIter.hasNext()) //clean out land and empty territories
		{
			Territory waterTerr = wIter.next();
			if (Matches.TerritoryIsLand.match(waterTerr) || Matches.territoryHasNoAlliedUnits(player, data).match(waterTerr))
				wIter.remove();
		}
		for (Territory myTerr : waterTerrs)
		{ //don't move transports which are next to a good spot
//			Set<Territory> neighborList = data.getMap().getNeighbors(myTerr, Matches.territoryHasRouteToEnemyCapital(data, player));
			Set<Territory> neighborList = data.getMap().getNeighbors(myTerr, landPassable);
			Iterator<Territory> nIter = neighborList.iterator();
			while (nIter.hasNext())
			{
				Territory nTerr = nIter.next();
				if (!SUtils.hasLandRouteToEnemyOwnedCapitol(nTerr, player, data))
					nIter.remove();
			}
			if (!neighborList.isEmpty())
				continue;
			List<Territory> neighborList2 = SUtils.getExactNeighbors(myTerr, 2, player, true);
			boolean skipTerr = false;
			for (Territory nTerr : neighborList2)
			{
//				if (Matches.territoryHasRouteToEnemyCapital(data, player).match(nTerr))
				if (SUtils.hasLandRouteToEnemyOwnedCapitol(nTerr, player, data))
					skipTerr = true;
			}
			if (skipTerr)
				continue;
			Territory closeTerr = SUtils.getClosestWaterTerr(endTerr, myTerr, data, player, true);
			List<Unit> transUnits = myTerr.getUnits().getMatches(transportingUnit);
			transUnits.removeAll(unitsAlreadyMoved);
			if (transUnits.isEmpty())
				continue;
			int maxDistance = MoveValidator.getMaxMovement(transUnits);
			Route seaRoute = SUtils.getMaxSeaRoute(data, myTerr, closeTerr, player, true, maxDistance);
			if (seaRoute == null)
				continue;
			List<Unit> transportedUnits = new ArrayList<Unit>();
			for (Unit transport : transUnits)
				transportedUnits.addAll(tracker.transporting(transport));
			transUnits.addAll(transportedUnits);
			moveUnits.add(transUnits);
			moveRoutes.add(seaRoute);
		}	
    }
    
    private void populateTransportMove(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {
    	/*
    	 * Want to initiate attack on any territory within range of transports by:
    	 * 1) Determining a general ranking of territories
    	 * 2) Finding a safe location for invading
    	 * 3) Leaving transports at the base factory if not
    	 * 		a) Allow the non-combat transport move to determine a target farther away
    	 */
    	Collection<Territory> impassableTerrs = getImpassableTerrs();
		boolean tFirst = transportsMayDieFirst();
		boolean isAmphib = isAmphibAttack(player, false);
		Route amphibRoute = getAmphibRoute(player, false);
		boolean aggressive = SUtils.determineAggressiveAttack(data, player, 1.4F);
        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		CompositeMatch<Unit> transUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport);
		CompositeMatch<Unit> transportingUnit = new CompositeMatchAnd<Unit>(transUnit, Matches.unitIsOwnedBy(player), Matches.transportIsTransporting());
		CompositeMatch<Unit> escortUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsNotTransport, Matches.UnitIsCarrier.invert(), HasntMoved);
		List<Territory> dontMoveFrom = new ArrayList<Territory>();
		List<Territory> alreadyAttacked = getLandTerrAttacked();
		List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
		HashMap<Territory, Territory> amphibMap = new HashMap<Territory, Territory>();
		/**
		 * First determine if attack ships have been purchased and limit moves at that factory
		 */
		boolean attackShipsBought = getKeepShipsAtBase();
		Territory baseFactory = getSeaTerr();
/*		if (attackShipsBought && baseFactory != null)
		{
			Set<Territory> baseTerrs = data.getMap().getNeighbors(baseFactory, Matches.TerritoryIsWater);
			dontMoveFrom.addAll(baseTerrs);
			for (Territory baseTerr : baseTerrs)
			{
				unitsAlreadyMoved.addAll(baseTerr.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
			}	
		}
*/		List<Territory> seaTerrAttacked = getSeaTerrAttacked();
		alreadyAttacked.addAll(seaTerrAttacked);
		Territory amphibAttackTerr = null;
		//go back to the amphib route
		if (amphibRoute != null)
		{
			amphibAttackTerr = amphibRoute.getEnd();
		}
		List<Territory> occTransTerr = SUtils.findCertainShips(data, player, transportingUnit);
		occTransTerr.removeAll(dontMoveFrom);
		if (occTransTerr == null || occTransTerr.isEmpty())
			return;
		IntegerMap<Territory> transCountMap = new IntegerMap<Territory>();
		for (Territory transTerr : occTransTerr)
			transCountMap.put(transTerr, transTerr.getUnits().countMatches(transportingUnit));
		SUtils.reorder(occTransTerr, transCountMap, true);
		
		List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
		List<Territory> ourEnemyTerr = new ArrayList<Territory>();
		HashMap<Territory, Float> landTerrMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, seaTerrAttacked, player, tFirst, true, false);

		IntegerMap<Territory> targetMap = SUtils.targetTerritories(data, player, 4);
		Collection<Territory> targetTerritories = targetMap.keySet();
/*		for (Territory tT : targetTerritories)
		{
			if (landTerrMap.containsKey(tT))
			{
				Float lTValue = landTerrMap.get(tT);
				lTValue += targetMap.getInt(tT)*2;
				landTerrMap.put(tT, lTValue);
			}
		}
*/		List<PlayerID> enemyplayers = SUtils.getEnemyPlayers(data, player);
		PlayerID ePlayer = enemyplayers.get(0);
		List<Territory> checkForInvasion = new ArrayList<Territory>();
		checkForInvasion.addAll(ourEnemyTerr);
		checkForInvasion.addAll(ourFriendlyTerr);
		checkForInvasion.removeAll(impassableTerrs);
		for (Territory transTerr : occTransTerr)
		{
			List<Unit> ourTransports = transTerr.getUnits().getMatches(transportingUnit);
			ourTransports.removeAll(unitsAlreadyMoved);
			if (ourTransports.isEmpty())
				continue;
			int tmpDistance = MoveValidator.getMaxMovement(ourTransports);
			Set<Territory> transTerrNeighbors = data.getMap().getNeighbors(transTerr, tmpDistance+1);
			List<Territory> tmpTerrList = new ArrayList<Territory>(transTerrNeighbors);
			Iterator<Territory> tTIter = tmpTerrList.iterator();
			while (tTIter.hasNext())
			{
				Territory tmpTerr = tTIter.next();
				if (Matches.TerritoryIsWater.match(tmpTerr) || Matches.TerritoryIsPassableAndNotRestricted( player).invert().match(tmpTerr) || Matches.territoryHasWaterNeighbor(data).invert().match(tmpTerr))
					tTIter.remove();

			}
			HashMap<Territory, Float> landTerrMap2 = new HashMap<Territory, Float> (landTerrMap);
			int minDist = 0;
			Iterator<Territory> cFIter = tmpTerrList.iterator();
			while (cFIter.hasNext())
			{
				Territory lT = cFIter.next();
				if (Matches.isTerritoryAllied(player, data).match(lT) && Matches.territoryHasEnemyLandNeighbor(data, player).invert().match(lT))
				{
					float testPotential = SUtils.getStrengthOfPotentialAttackers(lT, data, player, tFirst, true, null);
					if (testPotential <= 1.0F)
					{
						cFIter.remove();
						continue;
					}
				}
				else if (Matches.isTerritoryAllied(player, data).match(lT))
				{
					cFIter.remove(); //List contains enemy Territories & Territories with EnemyLand Neighbor
					continue;
				}
				Territory safeTerr = SUtils.getClosestWaterTerr(lT, transTerr, data, player, tFirst);
				if (safeTerr == null || !landTerrMap2.containsKey(lT))
				{
					cFIter.remove();
					continue;
				}
				minDist = data.getMap().getWaterDistance(transTerr, safeTerr);
				if (minDist > tmpDistance || minDist == -1)
				{
					cFIter.remove();
					continue;
				}
				if (lT.equals(amphibAttackTerr))
				{
					Float amphibValue = landTerrMap2.get(lT) + 20.00F;
					landTerrMap2.put(lT, amphibValue);
				}
//				Float newVal = landTerrMap2.get(lT) - (minDist-1)*(targetTerritories.contains(lT) ? 1 : 2);
//				landTerrMap2.put(lT, newVal);
			}
			SUtils.reorder(tmpTerrList, landTerrMap2, true);
			if (ourTransports.isEmpty())
				continue;
			//first pass
			List<Territory> tTerrNeighbors = new ArrayList<Territory>(data.getMap().getNeighbors(transTerr));
			tTerrNeighbors.removeAll(impassableTerrs);
			int tDist = MoveValidator.getMaxMovement(ourTransports);
			Iterator<Territory> targetIter = tmpTerrList.iterator();
			boolean movedTransports = false;
			while (targetIter.hasNext())
			{
				Territory targetTerr = targetIter.next();
				List<Collection<Unit>> xUnits = new ArrayList<Collection<Unit>>();
				List<Route> xRoutes = new ArrayList<Route>();
				List<Unit> xMoved = new ArrayList<Unit>(unitsAlreadyMoved);

				float defendingStrength = 0.0F, planeStrength = 0.0F, blitzStrength = 0.0F, landStrength = 0.0F, BBStrength = 0.0F;
				float ourShipStrength = 0.0F;
				Territory targetTerr2 = null;
				ourTransports.removeAll(unitsAlreadyMoved);
				Iterator<Unit> tIter = ourTransports.iterator();
				if (tTerrNeighbors.contains(targetTerr) && Matches.isTerritoryEnemy(player, data).match(targetTerr))
				{
					Route shortRoute = data.getMap().getRoute(transTerr, targetTerr);
					if (shortRoute == null)
					{ //discourage invasions that don't have staying potential...add 1/4 of return potential attack
						float eShortPotential = SUtils.getStrengthOfPotentialAttackers(targetTerr, data, player, tFirst, true, alreadyAttacked);
						List<Unit> tLoadUnits = new ArrayList<Unit>();
						float eShortStrength = SUtils.strength(targetTerr.getUnits().getMatches(Matches.enemyUnit(player, data)), false, false, tFirst);
						float goStrength = (eShortStrength+0.25F*eShortPotential)*1.45F + 2.0F;
						float ourQuickStrength = 0.0F;
						List<Unit> qtransMoved = new ArrayList<Unit>();
						while (tIter.hasNext() && ourQuickStrength < goStrength)
						{
							Unit transport = tIter.next();
							List<Unit> qUnits = TripleAUnit.get(transport).getTransporting();
							tLoadUnits.addAll(qUnits);
							ourQuickStrength += SUtils.strength(qUnits, true, false, tFirst);
							qtransMoved.add(transport);
						}
						if (ourQuickStrength == 0.0F)
							continue;
						List<Unit> qAMoved = new ArrayList<Unit>();
						List<Collection<Unit>> qunitMoved = new ArrayList<Collection<Unit>>();
						List<Route> qRoutes = new ArrayList<Route>();
						float qPlaneStrength = 0.0F;
						float qBBStrength = 0.0F;
						float qBlitzStrength = 0.0F;
						float qLandStrength = 0.0F;
						if (ourQuickStrength < (eShortStrength + 0.25F*eShortPotential))
						{
							qBBStrength = SUtils.inviteBBEscort(targetTerr, goStrength - ourQuickStrength, qAMoved, qunitMoved, qRoutes, data, player);
							ourQuickStrength += qBBStrength;
							qPlaneStrength = SUtils.invitePlaneAttack(false, false, targetTerr, goStrength - ourQuickStrength, qAMoved, qunitMoved, qRoutes, data, player);
							ourQuickStrength += qPlaneStrength;
							qBlitzStrength = SUtils.inviteBlitzAttack(false, targetTerr, goStrength - ourQuickStrength, qAMoved, qunitMoved, qRoutes, data, player, true, true);
							ourQuickStrength += qBlitzStrength;
							qLandStrength = SUtils.inviteLandAttack(false, targetTerr, goStrength - ourQuickStrength, qAMoved, qunitMoved, qRoutes, data, player, false, Matches.territoryHasEnemyFactoryNeighbor(data, player).match(targetTerr), alreadyAttacked);
							ourQuickStrength += qLandStrength;
						}
						if (ourQuickStrength >= (eShortStrength*1.05F + 2.0F + eShortPotential))
						{
							if (qBBStrength + qPlaneStrength + qBlitzStrength + qLandStrength > 0.0F)
							{
								moveUnits.addAll(qunitMoved);
								moveRoutes.addAll(qRoutes);
								for (Collection<Unit> goUnit : qunitMoved)
									unitsAlreadyMoved.addAll(goUnit);
							}
							Route qRoute = data.getMap().getRoute(transTerr, targetTerr);
							moveUnits.add(tLoadUnits);
							moveRoutes.add(qRoute);
							unitsAlreadyMoved.addAll(tLoadUnits);
							unitsAlreadyMoved.addAll(qtransMoved);
							List<Unit> escorts = transTerr.getUnits().getMatches(escortUnit);
							escorts.removeAll(unitsAlreadyMoved);
							if (!escorts.isEmpty())
							{
								moveUnits.add(escorts);
								moveRoutes.add(qRoute);
								unitsAlreadyMoved.addAll(escorts);
							}
						}
					}
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
							unitsAlreadyMoved.addAll(qLoadUnits);
							unitsAlreadyMoved.addAll(qTransUnits);
							if (qBBStrength + qPlaneStrength + qBlitzStrength + qLandStrength > 0.0F)
							{
								moveUnits.addAll(qunitMoved);
								moveRoutes.addAll(qRoutes);
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
								}
							}
						}
					}
				}
				else
					targetTerr2 = SUtils.getSafestWaterTerr(targetTerr, transTerr, seaTerrAttacked, data, player, false, tFirst);
				if (targetTerr2 == null || data.getMap().getWaterDistance(transTerr, targetTerr2) > 2)
					continue;
				Route targetRoute = SUtils.getMaxSeaRoute(data, transTerr, targetTerr2, player, false, tDist);
				if (targetRoute == null || targetRoute.getEnd() == null)
				{
					continue;
				}
				List<Unit> defendingUnits = new ArrayList<Unit>();
				float enemyStrengthAtTarget = 0.0F;
				if (tFirst)
					ourShipStrength = ourTransports.size()*1.0F;
				List<Unit> landUnits = new ArrayList<Unit>();
				Iterator<Unit> transIter = ourTransports.iterator();
				while (transIter.hasNext())
				{
					Unit transport = transIter.next();
					if (tracker.isTransporting(transport))
						landUnits.addAll(tracker.transporting(transport));
					else
						transIter.remove();
				}
				float ourInvasionStrength = SUtils.strength(landUnits, true, false, tFirst);
				float xDefendingPotential = 0.0F;
				if (Matches.isTerritoryAllied(player, data).match(targetTerr))
				{
					defendingStrength = SUtils.getStrengthOfPotentialAttackers(targetTerr, data, player, tFirst, true, alreadyAttacked);
					float alliedStrength = SUtils.strength(targetTerr.getUnits().getUnits(), false, false, tFirst);
					ourInvasionStrength += alliedStrength*1.35F + 6.0F; //want to move in even when they have advantage
				}
				else
				{
					defendingUnits.addAll(targetTerr.getUnits().getMatches(Matches.enemyUnit(player, data)));
					defendingStrength = SUtils.strength(defendingUnits, false, false, tFirst);
					xDefendingPotential = SUtils.getStrengthOfPotentialAttackers(targetTerr, data, player, tFirst, true, alreadyAttacked);
					float minStrengthNeeded = (defendingStrength*1.65F + 3.0F) - ourInvasionStrength + xDefendingPotential*0.25F;
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
				boolean weAttacked = false;
				boolean weCanWin = ourInvasionStrength > (defendingStrength*1.10F + Math.max(0.25F*xDefendingPotential, 2.0F));
/*				if (!weCanWin)
				{
					List<Unit> calcUnits = new ArrayList<Unit>(landUnits);
					for (Collection<Unit> xU : xUnits)
						calcUnits.addAll(xU);
					HashMap<PlayerID, IntegerMap<UnitType>> costMap = SUtils.getPlayerCostMap(data);
					weCanWin = SUtils.calculateTUVDifference(targetTerr, calcUnits, defendingUnits, costMap, player, data, aggressive, Properties.getAirAttackSubRestricted(data));
				}
*/				if (weCanWin)
				{
					ourShipStrength += BBStrength*2.0F;
					List<Unit> shipsAtTarget = targetTerr2.getUnits().getMatches(Matches.alliedUnit(player, data));
					ourShipStrength += SUtils.strength(shipsAtTarget, false, true, tFirst);
					float strengthDiff = enemyStrengthAtTarget - ourShipStrength;
					ourShipStrength += SUtils.inviteShipAttack(targetTerr2, strengthDiff*2.5F, xMoved, xUnits, xRoutes, data, player, false, tFirst, false);
					float compareStrength = ourShipStrength*1.25F + (ourShipStrength > 2.0F ? 3.0F : 0.0F);
					if (enemyStrengthAtTarget <= compareStrength || enemyStrengthAtTarget < 2.0F)
					{//TODO: Limit transports to what is needed for amphibious attack
						alreadyAttacked.add(targetTerr); //consider as if we finished off targetTerr
						unitsAlreadyMoved.addAll(ourTransports);
						weAttacked = true;
						amphibMap.put(targetTerr2, targetTerr);
						if (transTerr != targetTerr2)
						{
							ourTransports.addAll(landUnits);
							moveUnits.add(ourTransports);
							moveRoutes.add(targetRoute);
						}
						if (xUnits.size() > 0)
						{
							for (Collection<Unit> x1 : xUnits)
								moveUnits.add(x1);
							moveRoutes.addAll(xRoutes);
							unitsAlreadyMoved.addAll(xMoved);
						}
						if (enemyStrengthAtTarget > 2.0F)
						{
							float checkStrength = ourShipStrength - SUtils.strength(shipsAtTarget, false, true, tFirst);
							List<Unit> markUnits = new ArrayList<Unit>();
							Iterator<Unit> shipIter = shipsAtTarget.iterator();
							while (shipIter.hasNext() && (checkStrength*1.20F + 2.0F) < enemyStrengthAtTarget)
							{
								Unit unitX = shipIter.next();
								checkStrength += SUtils.uStrength(unitX, false, true, tFirst);
								markUnits.add(unitX);
							}
							if (markUnits.size() > 0)
								unitsAlreadyMoved.addAll(markUnits);
						}
						movedTransports = true;
					}
				}
				if (!weAttacked)
					alreadyAttacked.remove(targetTerr);
			}
		}
		setAmphibMap(amphibMap);
/*		if (isAmphib)
		{
			s_logger.fine("Player: "+player.getName());
			s_logger.fine("Units: "+moveUnits);
			s_logger.fine("Routes: "+moveRoutes);
		}
*/    }
    
    private void amphibMapUnload(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {
        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
    	CompositeMatch<Unit> transportingUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsTransport, Matches.transportIsTransporting());
    	HashMap<Territory, Territory> amphibMap = getAmphibMap();
    	Set<Territory> invadeFrom = amphibMap.keySet();
    	for (Territory transTerr : invadeFrom)
    	{
    		Territory targetTerr = amphibMap.get(transTerr);
    		List<Unit> transports = transTerr.getUnits().getMatches(transportingUnit);
    		List<Unit> tUnits = new ArrayList<Unit>();
    		for (Unit transport : transports)
    			tUnits.addAll(tracker.transporting(transport));
    		Route tRoute = data.getMap().getRoute(transTerr, targetTerr);
    		if (tRoute != null && tRoute.getLength()==1)
    		{
    			moveUnits.add(tUnits);
    			moveRoutes.add(tRoute);
    		}
    	}
    }

    /**
     * Unload Transports in Non-combat phase
     * Setup for first pass to unload transports which have moved
     * @param onlyMoved - only transports which moved previously and are loaded
     * @param nonCombat
     * @param data
     * @param moveUnits
     * @param moveRoutes
     * @param player
     */

    private void populateTransportUnloadNonCom(boolean onlyMoved, GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {
        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
    	CompositeMatch<Unit> transUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsTransport);
    	if (onlyMoved)
    		transUnit = new CompositeMatchAnd<Unit>(transUnit, HasntMoved.invert());
    	CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand);
		List<Territory> transTerr = SUtils.findCertainShips(data, player, transUnit);
		if (transTerr.isEmpty())
			return;
		List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
        Territory capitol = TerritoryAttachment.getCapital(player, data);
        boolean capDanger = getCapDanger();
        boolean tFirst = transportsMayDieFirst();
        List<Territory> threats = new ArrayList<Territory>();
        boolean alliedCapDanger = SUtils.threatToAlliedCapitals(data, player, threats, tFirst);
        List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
        List<Territory> ourEnemyTerr = new ArrayList<Territory>();
//        HashMap<Territory, Float> rankMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, tFirst, true, true);
        HashMap<Territory, Float> rankMap = SUtils.rankAmphibReinforcementTerritories(data, null, player, tFirst);
        if (!capDanger)
        {
        	rankMap.remove(capitol);
        	ourFriendlyTerr.remove(capitol);
        }
        List<Territory> ourTerrNextToEnemyTerr = SUtils.getTerritoriesWithEnemyNeighbor(data, player, true, false);
        SUtils.removeNonAmphibTerritories(ourTerrNextToEnemyTerr, data);
        if (ourTerrNextToEnemyTerr.size() > 1)
        	SUtils.reorder(ourTerrNextToEnemyTerr, rankMap, true);
        for (Territory xT : transTerr)
        {
        	List<Territory> xTNeighbors = new ArrayList<Territory>(data.getMap().getNeighbors(xT, Matches.isTerritoryAllied(player, data)));
        	xTNeighbors.retainAll(ourTerrNextToEnemyTerr);
        	if (xTNeighbors.isEmpty())
        		continue;
        	SUtils.reorder(xTNeighbors, rankMap, true);
        	Territory landingTerr = xTNeighbors.get(0); //put them all here... TODO: check for need
        	Route landingRoute = data.getMap().getRoute(xT, landingTerr);
        	List<Unit> transUnits = xT.getUnits().getMatches(transUnit);
        	Iterator<Unit> tIter = transUnits.iterator();
        	List<Unit> landingUnits = new ArrayList<Unit>();
        	while (tIter.hasNext())
        	{
        		Unit transport = tIter.next();
        		landingUnits.addAll(tracker.transporting(transport));
        	}
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

    private void populateTransportUnload(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {
    	Collection<Territory> impassableTerrs = getImpassableTerrs();
		boolean tFirst = transportsMayDieFirst();
		TransportTracker tTracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		Territory eTerr[] = new Territory[data.getMap().getTerritories().size()] ; //revised game has 79 territories and 64 sea zones
		float eStrength[] = new float[data.getMap().getTerritories().size()];
		float eS = 0.00F;

		CompositeMatch<Unit> enemyUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data));
		CompositeMatch<Unit> landAndOwned = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.unitIsOwnedBy(player));
		CompositeMatch<Unit> landAndEnemy = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, enemyUnit);
		CompositeMatch<Unit> airEnemyUnit = new CompositeMatchAnd<Unit>(enemyUnit, Matches.UnitIsAir);
		CompositeMatch<Unit> landOrAirEnemy = new CompositeMatchOr<Unit>(landAndEnemy, airEnemyUnit);
		CompositeMatch<Unit> transportingUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsTransport, Transporting);
		CompositeMatch<Unit> enemyfactories = new CompositeMatchAnd<Unit>(Matches.UnitIsFactory, enemyUnit);

        float remainingStrength = 100.0F;

		List<Territory>transTerr = SUtils.findCertainShips(data, player, Matches.UnitIsTransport);
		if (transTerr.isEmpty())
			return;
		List<Territory>enemyCaps = SUtils.findUnitTerr(data, player, enemyfactories);
		List<Territory>tempECaps = new ArrayList<Territory>(enemyCaps);
		List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
		List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
		List<Territory> ourEnemyTerr =new ArrayList<Territory>();
		List<Territory> alreadyAttacked = new ArrayList<Territory>();
		HashMap<Territory, Float> rankMap =	SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, tFirst, true, false);

		List<Territory> goTerr = new ArrayList<Territory>(rankMap.keySet());
		SUtils.reorder(goTerr, rankMap, true);
		CompositeMatch<Territory> enemyLand = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemy(player, data), Matches.TerritoryIsLand, Matches.TerritoryIsNotImpassable);
		for (Territory qT : tempECaps) //add all neighbors
		{
			Set<Territory>nTerr = data.getMap().getNeighbors(qT, enemyLand);
			if (nTerr.size() > 0)
				enemyCaps.addAll(nTerr);
		}
		int maxCap = enemyCaps.size() - 2;
//		if (maxPasses < maxCap)
//			maxPasses=1;
		Territory tempTerr=null, tempTerr2 = null;
		enemyCaps.retainAll(goTerr);
		SUtils.reorder(enemyCaps, rankMap, true);
		/*
		 * Search list: Production Value...capitals all have high values
		 * if friendly, dump there first
		 * if enemy, but we are stronger...dump next
		 * enemy capital will always get the first look
		 */
		List<Unit> xAlreadyMoved = new ArrayList<Unit>();
		List<Collection<Unit>> xMoveUnits = new ArrayList<Collection<Unit>>();
		List<Route> xMoveRoutes = new ArrayList<Route>();
		for (Territory eC : enemyCaps) //priority...send units into capitals & capneighbors when possible
//		for (Territory eC : goTerr)
		{
			Set<Territory> neighborTerr = data.getMap().getNeighbors(eC, Matches.TerritoryIsWater);
			List<Unit> capUnits = eC.getUnits().getMatches(landOrAirEnemy);
			float capStrength = SUtils.strength(capUnits, false, false, tFirst);
			float invadeStrength = SUtils.strength(eC.getUnits().getMatches(Matches.unitIsOwnedBy(player)), true, false, tFirst);
			if (Matches.isTerritoryFriendly(player, data).match(eC))
			{
				for (Territory nF : neighborTerr)
				{
					List<Unit> quickLandingUnits = new ArrayList<Unit>();
					List<Unit> nFTrans = nF.getUnits().getMatches(transportingUnit);
					Iterator<Unit> nFIter = nFTrans.iterator();
					while (nFIter.hasNext())
					{
						Unit transport = nFIter.next();
						quickLandingUnits.addAll(tTracker.transporting(transport));
					}
					Route quickLandRoute = new Route();
					quickLandRoute.setStart(nF);
					quickLandRoute.add(eC);
					if (quickLandRoute != null)
					{
						moveUnits.add(quickLandingUnits);
						moveRoutes.add(quickLandRoute);
						unitsAlreadyMoved.addAll(quickLandingUnits);
						if (transTerr.contains(nF))
							transTerr.remove(nF);
					}
				}
			}
			for (Territory nT : neighborTerr)
			{
				if (nT.getUnits().someMatch(transportingUnit))
				{
					List<Unit> specialLandUnits = nT.getUnits().getMatches(landAndOwned);
					specialLandUnits.removeAll(unitsAlreadyMoved);
					if (specialLandUnits.isEmpty())
						continue;
					invadeStrength = SUtils.strength(specialLandUnits, true, false, tFirst);
					Set<Territory> attackNeighbors = data.getMap().getNeighbors(eC, Matches.isTerritoryFriendly(player, data));
					float localStrength = 0.0F;
					for (Territory aN : attackNeighbors)
					{
						if (aN.isWater()) //don't count anything from water
							continue;
						List<Unit> localUnits = aN.getUnits().getMatches(landAndOwned);
						localUnits.removeAll(unitsAlreadyMoved);
						localStrength += SUtils.strength(localUnits, true, false, tFirst);
						xMoveUnits.add(localUnits);
						Route localRoute = data.getMap().getLandRoute(aN, eC);
						xMoveRoutes.add(localRoute);
						xAlreadyMoved.addAll(localUnits);
					}
					float ourStrength = invadeStrength + localStrength;
					remainingStrength = (capStrength*2.20F + 5.00F) - ourStrength;
					xAlreadyMoved.addAll(unitsAlreadyMoved);
					float blitzStrength =SUtils.inviteBlitzAttack(false, eC, remainingStrength, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, true, true);
					remainingStrength -= blitzStrength;
					float planeStrength = SUtils.invitePlaneAttack(false, false, eC, remainingStrength, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player);
					remainingStrength -= planeStrength;
					List<Territory> alliedTerr = SUtils.getNeighboringLandTerritories(data, player, eC);
					float alliedStrength = 0.0F;
					CompositeMatch<Unit> alliedButNotMyUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player).invert(), Matches.alliedUnit(player, data));
					for (Territory aCheck : alliedTerr)
						alliedStrength += SUtils.strength(aCheck.getUnits().getMatches(alliedButNotMyUnit), true, false, tFirst);
					float attackFactor = (alliedStrength > 0.75F*capStrength) ? 0.92F : 1.04F; //let retreat handle this
					if ((invadeStrength + localStrength + blitzStrength + planeStrength) >= attackFactor*capStrength)
					{
						Route specialRoute = data.getMap().getRoute(nT, eC);
						moveUnits.add(specialLandUnits);
						moveRoutes.add(specialRoute);
						unitsAlreadyMoved.addAll(specialLandUnits);
						moveUnits.addAll(xMoveUnits);
						moveRoutes.addAll(xMoveRoutes);
						for (Collection<Unit> xCollect : xMoveUnits)
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

		for (Territory t : transTerr) //complete check
		{
			List<Unit> transUnits = t.getUnits().getMatches(transportingUnit);
			transUnits.removeAll(unitsAlreadyMoved);
			List<Unit> units = t.getUnits().getMatches(landAndOwned);
			units.removeAll(unitsAlreadyMoved);
			float ourStrength = SUtils.strength(units, true, false, tFirst);
			if (units.size() == 0)
				continue;
			List<Territory> enemy=SUtils.getNeighboringEnemyLandTerritories(data, player, t);
			List<Territory> enemyCopy = new ArrayList<Territory>(enemy);
			List<Unit> alreadyOut = new ArrayList<Unit>();
			//quick check for empty territories
			Map<Unit, Collection<Unit>> transMap = tTracker.transporting(transUnits);

			int i=0;
			for (Territory t2 : enemy) //find strength of all enemy terr (defensive)
			{
				eTerr[i]=t2;
				eStrength[i]=SUtils.strength(t2.getUnits().getMatches(landOrAirEnemy), false, false, tFirst);
				eStrength[i]-= SUtils.strength(t2.getUnits().getMatches(Matches.unitIsOwnedBy(player)), true, false, tFirst);
				i++;
			}
			float tmpStrength = 0.0F;
			Territory tmpTerr = null;
			for (int j2=0; j2<i-1; j2++) //sort the territories by strength
			{
				tmpTerr = eTerr[j2];
				tmpStrength = eStrength[j2];
				Set<Territory>badFactTerr = data.getMap().getNeighbors(tmpTerr, Matches.territoryHasEnemyFactory(data, player));
				if ((badFactTerr.size() > 0) && (tmpStrength*1.10F + 5.00F) <= eStrength[j2+1])
					continue; //if it is next to a factory, don't move it down
				if (tmpStrength < eStrength[j2+1])
				{
					eTerr[j2]=eTerr[j2+1];
					eStrength[j2]=eStrength[j2+1];
					eTerr[j2+1]=tmpTerr;
					eStrength[j2+1]=tmpStrength;
				}
			}
			// Consideration: There might be a land based invasion of an empty terr available
			for (Territory x : enemyCopy)
			{
				if (Matches.isTerritoryEnemy(player, data).match(x) && Matches.territoryIsEmptyOfCombatUnits(data, player).match(x))
				{
					float topStrength = eStrength[0];
					float winStrength = 0.0F;
					float newStrength = ourStrength;
					for (int jC = 0; jC < enemy.size()-1; jC++)
					{
						if (!enemy.contains(eTerr[jC]))
							continue;
						topStrength = eStrength[jC];
						if (newStrength > topStrength && winStrength == 0.0F) //what we can currently win
							winStrength = topStrength;
					}
					Iterator<Unit> transIter = transUnits.iterator();
					boolean gotOne = false;
					while (transIter.hasNext() && !gotOne)
					{
						Unit transport = transIter.next();
						if (!tTracker.isTransporting(transport) || transport == null)
							continue;
						Collection<Unit> transportUnits = transMap.get(transport);
						if (transportUnits == null)
							continue;
						float minusStrength = SUtils.strength(transportUnits, true, false, tFirst);
						if ((newStrength - minusStrength) > winStrength )
						{
							Route xRoute = data.getMap().getRoute(t, x);
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

			for (int j=0; j<i; j++) //just find the first terr we can invade
			{
				units.removeAll(unitsAlreadyMoved);
				xAlreadyMoved.addAll(unitsAlreadyMoved);
				float ourStrength2 = ourStrength;
                eS = eStrength[j];
				Territory invadeTerr = eTerr[j];
				if (!enemy.contains(invadeTerr))
					continue;
                float strengthNeeded = 2.15F*eS + 3.00F;
 				float airStrength = 0.0F;
				ourStrength2 += airStrength;
				float rStrength = strengthNeeded - ourStrength2;
				float planeStrength = SUtils.invitePlaneAttack(true, false, invadeTerr, rStrength, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player);
				rStrength -= planeStrength;
				float blitzStrength = SUtils.inviteBlitzAttack(false, invadeTerr, rStrength, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, true, false);
				rStrength -= blitzStrength;
				float landStrength = SUtils.inviteLandAttack(false, invadeTerr, rStrength, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, true, false, alreadyAttacked);
/*				List<Unit> aBattleUnit = new ArrayList<Unit>(units);
				for (Collection<Unit> qUnits : xMoveUnits)
					aBattleUnit.addAll(qUnits);
				aBattleUnit.addAll(invadeTerr.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
				List<Unit> dBattleUnit = invadeTerr.getUnits().getMatches(landOrAirEnemy);
				IntegerMap<UnitType> aMap = SUtils.convertListToMap(aBattleUnit);
				IntegerMap<UnitType> dMap = SUtils.convertListToMap(dBattleUnit);
				boolean weWin = SUtils.quickBattleEstimator(aMap, dMap, player, invadeTerr.getOwner(), false, Properties.getAirAttackSubRestricted(data));
*/				boolean weWin = (planeStrength + blitzStrength + landStrength + ourStrength) > (eS*1.15F + 2.0F);
				/**
				 * Invade if we should win...or we should barely lose (enemy projected to only have 1 remaining defender)
				 */
//				if (weWin || (dMap.totalValues()==1 && dBattleUnit.size() > 3))
				if (weWin)
                {
					Route route = new Route();
					route.setStart(t);
					route.add(invadeTerr);
					moveUnits.add(units);
					moveRoutes.add(route);
					unitsAlreadyMoved.addAll(units);
					moveUnits.addAll(xMoveUnits);
					moveRoutes.addAll(xMoveRoutes);
					for (Collection<Unit> xCollect : xMoveUnits)
						unitsAlreadyMoved.addAll(xCollect);
					alreadyAttacked.add(invadeTerr);
				}
				xAlreadyMoved.clear();
				xMoveUnits.clear();
				xMoveRoutes.clear();
			}
		}
	}


    private List<Unit> load2Transports( boolean reload, GameData data, List<Unit> transportsToLoad, Territory loadFrom, PlayerID player)
    {
        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
    	List<Unit> units = new ArrayList<Unit>();
        for(Unit transport : transportsToLoad)
        {
        	Collection<Unit> landunits = tracker.transporting(transport);
        	for(Unit u : landunits) {
        		units.add(u);
        	}
        }
    	return units;
    }

    private void doMove(List<Collection<Unit>> moveUnits, List<Route> moveRoutes, List<Collection<Unit>> transportsToLoad, IMoveDelegate moveDel)
    {

        for(int i =0; i < moveRoutes.size(); i++)
        {
            pause();

            if(moveRoutes.get(i) == null || moveRoutes.get(i).getEnd() == null || moveRoutes.get(i).getStart() == null)
            {
                s_logger.fine("Route not valid" + moveRoutes.get(i) + " units:" + moveUnits.get(i));
                continue;
            }

            String result;

            if(transportsToLoad == null)
            {
                result = moveDel.move(moveUnits.get(i), moveRoutes.get(i)  );
			}
            else
                result = moveDel.move(moveUnits.get(i), moveRoutes.get(i) , transportsToLoad.get(i) );

            if(result != null)
            {
                s_logger.fine("could not move " + moveUnits.get(i) + " over " + moveRoutes.get(i) + " because : " + result+"\n");
            }
        }
    }
    private boolean markFactoryUnits(final GameData data, PlayerID player, Collection<Unit> unitsAlreadyMoved)
    {
    	HashMap<Territory, Float> capMap = determineCapDanger(player, data);
    	Territory myCapital = TerritoryAttachment.getCapital(player, data);
    	List<Unit> myCapUnits = myCapital.getUnits().getMatches(Matches.unitIsOwnedBy(player));
    	List<Unit> alliedCapUnits = myCapital.getUnits().getMatches(Matches.alliedUnit(player, data));
    	float alliedCapStrength = SUtils.strength(alliedCapUnits, false, false, true);
    	Iterator<Unit> capUnitIter = myCapUnits.iterator();
    	float actualAlliedStrength = alliedCapStrength - SUtils.strength(myCapUnits, false, false, true);
    	float capStrengthNeeded = capMap.get(myCapital) - actualAlliedStrength;
    	while (capUnitIter.hasNext() && capStrengthNeeded > 0.0F)
    	{
    		Unit capUnit = capUnitIter.next();
    		capStrengthNeeded -= SUtils.uStrength(capUnit, false, false, true);
    		unitsAlreadyMoved.add(capUnit);
    	}
    	boolean capDanger = capStrengthNeeded > 0.0F;
    	return capDanger;

    }
    /**
     * Add all ships around a factory into a group which is not be moved
     * @param data
     * @param player
     * @param alreadyMoved - List of units to be modified
     */
    private void markBaseShips(final GameData data, PlayerID player, List<Unit> alreadyMoved)
    {
    	if (getKeepShipsAtBase() && getSeaTerr() != null)
    	{
    		Set<Territory> baseTerrs = data.getMap().getNeighbors(getSeaTerr(), Matches.TerritoryIsWater);
    		for (Territory bT : baseTerrs)
    		{
    			alreadyMoved.addAll(bT.getUnits().getMatches(Matches.unitIsOwnedBy(player)));
    		}
    	}
    }

    private void specialPlaneAttack(final GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {
    	Collection<Territory> impassableTerrs = getImpassableTerrs();
        final Collection<Unit> alreadyMoved = new HashSet<Unit>();
        Territory myCapital = TerritoryAttachment.getCapital(player, data);

		boolean tFirst = transportsMayDieFirst();
        Match<Unit> notAlreadyMoved =new CompositeMatchAnd<Unit>(new Match<Unit>()
		   {
				public boolean match(Unit o)
				{
					return !alreadyMoved.contains(o);
				}
			});
		Match<Unit> ownedUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
		Match<Unit> HasntMoved2 = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), HasntMoved, notAlreadyMoved);
       	Match<Unit> ownedAndNotMoved = new CompositeMatchAnd<Unit>(ownedUnit, HasntMoved);
       	Match<Unit> airAttackUnit = new CompositeMatchAnd<Unit>(ownedAndNotMoved, Matches.UnitIsAir);
       	Match<Unit> enemySubUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsSub);
       	Match<Unit> fighterUnit = new CompositeMatchAnd<Unit>(Matches.UnitCanLandOnCarrier, HasntMoved2);
       	Match<Unit> bomberUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsStrategicBomber, HasntMoved2);
       	Match<Unit> destroyerUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsDestroyer);

       	CompositeMatch<Territory> noEnemyAA = new CompositeMatchAnd<Territory>(Matches.territoryHasEnemyAA(player, data).invert(), Matches.TerritoryIsNotImpassable);
       	//Check to see if we have total air superiority...4:1 or greater...if so, let her rip

       	List<Territory> myAirTerr = SUtils.findUnitTerr(data, player, airAttackUnit);
       	List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
       	float planeStrength = 0.0F, shipStrength = 0.0F;
       	for (Territory AttackFrom : myAirTerr)
       	{
			List<Unit> myFighters = AttackFrom.getUnits().getMatches(fighterUnit);
			int fighterCount = myFighters.size();
			float myFighterStrength = SUtils.strength(myFighters, true, false, false);
			List<Unit> myBombers = AttackFrom.getUnits().getMatches(bomberUnit);
			float myBomberStrength = SUtils.strength(myBombers, true, false, false);
			int bomberCount = myBombers.size();
			float myTotalStrength = myFighterStrength + myBomberStrength;
			Set<Territory> myNeighbors = data.getMap().getNeighbors(AttackFrom);
			Set<Territory> enemyNeighbors = data.getMap().getNeighbors(AttackFrom, Matches.territoryHasEnemyUnits(player, data));
			for (Territory check2 : myNeighbors)
			{
				Set<Territory> check2Terr = data.getMap().getNeighbors(check2, Matches.territoryHasEnemyUnits(player, data));
				if (check2Terr != null && check2Terr.size() > 0)
				{
					for (Territory enemyOnly : check2Terr)
					{
						if (!enemyNeighbors.contains(enemyOnly))
							enemyNeighbors.add(enemyOnly);
					}
				}
			}
			List<Territory> waterEnemies = new ArrayList<Territory>();
			for (Territory w : enemyNeighbors)
			{
				if (w.isWater())
				{
					waterEnemies.add(w);
					List<Unit> eUnits = w.getUnits().getMatches(Matches.enemyUnit(player, data));
					float waterStrength = SUtils.strength(eUnits, false, true, tFirst);
					float ourWaterStrength = 0.0F;
					if (w.getUnits().allMatch(enemySubUnit) && Properties.getAirAttackSubRestricted(data))
					{ //need a destroyer
						List<Territory> destroyerTerr = SUtils.findOurShips(w, data, player, destroyerUnit);
						boolean dAttacked = false;
						float dStrength = 0.0F;
						if (destroyerTerr.size() > 0)
						{
							for (Territory dT : destroyerTerr)
							{
								List<Unit> destroyers = dT.getUnits().getMatches(destroyerUnit);
								int dDist = MoveValidator.getLeastMovement(destroyers);
								Route dRoute = SUtils.getMaxSeaRoute(data, dT, w, player, true, dDist);
								if (dRoute == null || dRoute.getLength() > 2)
									continue;
								List<Unit> dUnits = new ArrayList<Unit>();
								for (Unit d : destroyers)
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
							float stillNeeded = waterStrength*2.25F + 4.00F - ourWaterStrength;
							planeStrength = SUtils.invitePlaneAttack(false, false, w, stillNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player);
							stillNeeded -= planeStrength;
						}
					}
					else
					{
						float stillNeeded = waterStrength*2.25F + 4.00F;
						List<Collection<Unit>> xMoveUnits = new ArrayList<Collection<Unit>>();
						List<Route> xMoveRoutes = new ArrayList<Route>();
						List<Unit> xMoved = new ArrayList<Unit>(unitsAlreadyMoved);
						planeStrength = SUtils.invitePlaneAttack(false, false, w, stillNeeded, xMoved, xMoveUnits, xMoveRoutes, data, player);
						stillNeeded -= planeStrength;
						shipStrength = SUtils.inviteShipAttack(w, stillNeeded, xMoved, xMoveUnits, xMoveRoutes, data, player, true, tFirst, false);
						stillNeeded -= shipStrength;
						if (stillNeeded <= 1.0F)
						{
							moveUnits.addAll(xMoveUnits);
							moveRoutes.addAll(xMoveRoutes);
							for (Collection<Unit> qUnits : xMoveUnits)
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
			fighterCount = myFighters.size();
			bomberCount = myBombers.size();
			if (enemyNeighbors != null)
			{ 
				for (Territory badGuys : enemyNeighbors)
				{
					List<Unit> enemyUnits = badGuys.getUnits().getMatches(Matches.enemyUnit(player, data));
					float badGuyStrength = 0.0F;
					if (badGuys.isWater())
						badGuyStrength = SUtils.strength(enemyUnits, false, true, tFirst);
					else
						badGuyStrength = SUtils.strength(enemyUnits, false, false, tFirst);
					int badGuyCount = enemyUnits.size();
					float needStrength = 2.4F*badGuyStrength + 3.00F;
					float actualStrength = 0.0F;
					List<Unit> myAttackers = new ArrayList<Unit>();
					List<Unit> myAttackers2 = new ArrayList<Unit>();
					List<Unit> allUnits = new ArrayList<Unit>();
					allUnits.addAll(myFighters);
					allUnits.addAll(myBombers);
					IntegerMap<UnitType> attackTypes = SUtils.convertListToMap(allUnits);
					IntegerMap<UnitType> badTypes = SUtils.convertListToMap(enemyUnits);
					HashMap<PlayerID, IntegerMap<UnitType>> costMap = SUtils.getPlayerCostMap(data);
					boolean weWinTUV = SUtils.calculateTUVDifference(badGuys, allUnits, enemyUnits, costMap, player, data, false, Properties.getAirAttackSubRestricted(data));
					if (myTotalStrength > needStrength && weWinTUV )
					{
						int actualAttackers = 0;
						Route myRoute = data.getMap().getRoute(AttackFrom, badGuys, noEnemyAA);
						if (myRoute == null || myRoute.getEnd() == null)
							continue;
						if (!myFighters.isEmpty() && MoveValidator.canLand(myFighters, myRoute.getEnd(), player, data))
						{
							for (Unit f : myFighters)
							{
								if (actualStrength < needStrength)
								{
									myAttackers.add(f);
									actualStrength += SUtils.airstrength(f, true);
									actualAttackers++;
								}
							}
							if (actualAttackers > 0 && myRoute != null && actualStrength > needStrength)
							{
								moveUnits.add(myAttackers);
								moveRoutes.add(myRoute);
								alreadyMoved.addAll(myAttackers);
								myFighters.removeAll(myAttackers);
							}
						}
						if ((actualStrength > needStrength && (actualAttackers > badGuyCount+1)) || myBombers.size() == 0 || myRoute == null || myRoute.getEnd() == null)
							continue;
						if (!myBombers.isEmpty() && MoveValidator.canLand(myBombers, myRoute.getEnd(), player, data))
						{
							for (Unit b : myBombers)
							{
								if (actualStrength < needStrength || (actualAttackers <= badGuyCount+1) )
								{
									myAttackers2.add(b);
									actualStrength += SUtils.airstrength(b, true);
								}
							}
							if (myAttackers.size() > 0 && myRoute != null)
							{	
								moveUnits.add(myAttackers2);
								moveRoutes.add(myRoute);
								alreadyMoved.addAll(myAttackers2);
								myBombers.removeAll(myAttackers2);
							}
						}
					}
				}
			}
		}
	}

    private void protectOurAllies(boolean nonCombat, final GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {
 		CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand, Matches.UnitIsNotAA, Matches.UnitIsNotFactory);
 		CompositeMatch<Unit> carrierUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsCarrier);
 		CompositeMatch<Unit> fighterUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanLandOnCarrier);
        List<Territory> threats = new ArrayList<Territory>();
        boolean tFirst = transportsMayDieFirst(), noncombat = true;
        List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
        boolean capDanger = markFactoryUnits(data, player, unitsAlreadyMoved);
        
    	Territory myCapital = TerritoryAttachment.getCapital(player, data);
        boolean alliedCapDanger = SUtils.threatToAlliedCapitals(data, player, threats, tFirst);
        List<Territory> seaTerrAttacked = getSeaTerrAttacked();
        List<Territory> alreadyAttacked = Collections.emptyList();
        if (alliedCapDanger)
        {
        	List<Territory> threatRemoved = new ArrayList<Territory>();
        	//first, can we take out any of the threats?
        	float planeStrength = 0.0F;
        	for (Territory threatTerr : threats)
        	{
        		Set<Territory> allThreats = data.getMap().getNeighbors(threatTerr, Matches.isTerritoryEnemyAndNotNuetralWater(player, data));
        		HashMap<Territory, Float> threatMap = new HashMap<Territory, Float>();
        		for (Territory checkThreat : allThreats)
        		{
        			float eStrength = SUtils.strength(checkThreat.getUnits().getMatches(Matches.enemyUnit(player, data)), false, false, tFirst);
        			threatMap.put(checkThreat, eStrength);
        		}
        		List<Territory> allThreatTerr = new ArrayList<Territory>(allThreats);
        		SUtils.reorder(allThreatTerr, threatMap, true);
        		List<Collection<Unit>> xMovesKeep = new ArrayList<Collection<Unit>>();
        		List<Route> xRoutesKeep = new ArrayList<Route>();
        		List<Unit> xMovedKeep = new ArrayList<Unit>();
        		for (Territory checkThreat : allThreatTerr)
        		{
        			float eStrength = threatMap.get(checkThreat);
        			List<Collection<Unit>> xMoves = new ArrayList<Collection<Unit>>();
        			List<Route> xRoutes = new ArrayList<Route>();
        			List<Unit> xMovedUnits = new ArrayList<Unit>();
        			xMovedUnits.addAll(xMovedKeep);
        			float needStrength = eStrength*1.25F + 3.0F;
        			float totStrength = 0.0F;
        			needStrength = SUtils.inviteLandAttack(false, checkThreat, needStrength, xMovedUnits, xMoves, xRoutes, data, player, true, true, alreadyAttacked);
        			needStrength = SUtils.inviteTransports(false, checkThreat, needStrength, xMovedUnits, xMoves, xRoutes, data, player, tFirst, false, seaTerrAttacked);
        			needStrength = SUtils.inviteBlitzAttack(false, checkThreat, needStrength, xMovedUnits, xMoves, xRoutes, data, player, true, true);
        			float thisPlaneStrength = SUtils.invitePlaneAttack(false, false, checkThreat, needStrength, xMovedUnits, xMoves, xRoutes, data, player);
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
        		float newThreat = SUtils.getStrengthOfPotentialAttackers(threatTerr, data, player, tFirst, true, threatRemoved);
        		float alliedStrength = SUtils.strength(threatTerr.getUnits().getUnits(), false, false, tFirst) + planeStrength;
        		if (alliedStrength < newThreat) //commit to the attacks
        		{
        			for (Collection<Unit> x1 : xMovesKeep)
        				moveUnits.add(x1);
        			moveRoutes.addAll(xRoutesKeep);
        			unitsAlreadyMoved.addAll(xMovedKeep);
        		}
        	}
            if (SUtils.shipThreatToTerr(myCapital, data, player, tFirst) > 2)
            {
            	//don't use fighters on AC near capital if there is a strong threat to ships
            	List<Territory> fighterTerr = SUtils.findOnlyMyShips(myCapital, data, player, carrierUnit);
            	for (Territory fT : fighterTerr)
            	{
            		List<Unit> fighterUnits = fT.getUnits().getMatches(fighterUnit);
            		fighterUnits.removeAll(unitsAlreadyMoved);
            		unitsAlreadyMoved.addAll(fighterUnits);
            	}
            	List<Territory> transportTerr = SUtils.findOnlyMyShips(myCapital, data, player, Matches.UnitIsTransport);
            	if (tFirst) //if transports have no value in ship fight...let them go...we can catch up to them in nonCombat
            	{
            		for (Territory tranTerr : transportTerr)
            		{
            			unitsAlreadyMoved.addAll(tranTerr.getUnits().getMatches(Matches.UnitIsTransport));
            		}
            	}
            
            }
        	for (Territory testCap : threats)
        	{
        		float remainingStrengthNeeded = SUtils.getStrengthOfPotentialAttackers(testCap, data, player, tFirst, true, null);
        		remainingStrengthNeeded -= SUtils.strength(testCap.getUnits().getUnits(), false, false, tFirst);
        		float blitzStrength = SUtils.inviteBlitzAttack(true, testCap, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, false, true);
        		remainingStrengthNeeded -= blitzStrength;
        		planeStrength = SUtils.invitePlaneAttack(true, false, testCap, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player);
        		remainingStrengthNeeded -= planeStrength;
        		Set<Territory> copyOne = data.getMap().getNeighbors(testCap, 1);
        		for (Territory moveFrom : copyOne)
        		{
        			if (!moveFrom.isWater() && moveFrom.getUnits().someMatch(landUnit))
        			{
        				List<Unit> helpUnits = moveFrom.getUnits().getMatches(landUnit);
        				Route aRoute = data.getMap().getRoute(moveFrom, testCap, Matches.territoryHasEnemyAA(player, data).invert());
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
        						List<Unit> workList = new ArrayList<Unit>();
        						for (Unit goUnit : helpUnits)
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
        		//only use seaTerrAttacked if this is in the combat loop...noncombat will know the results of combat moves
        		if (SUtils.isWaterAt(testCap, data))
        			SUtils.inviteTransports(noncombat, testCap, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, tFirst, false, noncombat ? null : seaTerrAttacked);
        		else
        		{
        			Set<Territory> testCapNeighbors = data.getMap().getNeighbors(testCap, Matches.isTerritoryAllied(player, data));
        			for (Territory tCN : testCapNeighbors)
        			{
        				SUtils.inviteTransports(noncombat, tCN, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, tFirst, false, noncombat ? null : seaTerrAttacked);
        			}
        		}
          	}
        }
     }


    private void bringShipsToTransports(final GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {

       boolean tFirst = transportsMayDieFirst();
       final Collection<Unit> alreadyMoved = new HashSet<Unit>();
       Territory myCapital = TerritoryAttachment.getCapital(player, data);

	   Match<Unit> notAlreadyMoved =new CompositeMatchAnd<Unit>(new Match<Unit>()
		   {
				public boolean match(Unit o)
				{
					return !alreadyMoved.contains(o);
				}
			});
	   Match<Unit> ownedUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
	   Match<Unit> mySeaAirUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsNotTransport);
	   Match<Unit> myCarrierUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsCarrier);
	   Match<Unit> myAirUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitCanLandOnCarrier);
	   Match<Unit> myCarrierGroup = new CompositeMatchOr<Unit>(myCarrierUnit, myAirUnit);
       Match<Unit> alliedTransport = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsTransport);
       Match<Unit> alliedSeaAttackUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsSea, Matches.UnitIsNotTransport);
       Match<Unit> alliedAirAttackUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsAir);
       Match<Unit> alliedSeaAirAttackUnit = new CompositeMatchOr<Unit>(alliedSeaAttackUnit, alliedAirAttackUnit);
       HashMap<Territory, Collection<Unit>> shipsMap = new HashMap<Territory, Collection<Unit>>();
       int allShips = 0;
       int enemyShips = 0;
       List <PlayerID> ePlayers = SUtils.getEnemyPlayers(data, player);
       PlayerID ePTemp = ePlayers.get(0);
       List <PlayerID> alliedPlayers = SUtils.getEnemyPlayers(data, ePTemp);
       for (PlayerID ePlayer : ePlayers)
    	   enemyShips += countSeaUnits(data, ePlayer);
       for (PlayerID aPlayer : alliedPlayers)
    	   allShips += countSeaUnits(data, aPlayer);
       float targetFactor = 0.55F;
//       if (allShips > enemyShips*2)
//    	   targetFactor = 0.45F;
       List<Territory> alliedTransTerr = SUtils.findUnitTerr(data, player, alliedTransport);
       HashMap<Territory, Float> attackAtTrans = new HashMap<Territory, Float>();
       Iterator<Territory> aTIter = alliedTransTerr.iterator();
       while (aTIter.hasNext())
       {
    	   Territory aT = aTIter.next();
    	   float aTEStrength = SUtils.getStrengthOfPotentialAttackers(aT, data, player, tFirst, false, null);
    	   if (aTEStrength < 2.0F)
    		   aTIter.remove();
    	   else
    		   attackAtTrans.put(aT, aTEStrength);
       }
       SUtils.reorder(alliedTransTerr, attackAtTrans, true);
       for (Territory sendToTrans : alliedTransTerr)
       {
    	   float enemyStrength =  attackAtTrans.get(sendToTrans);
    	   float targetStrength = enemyStrength*1.25F + (enemyStrength > 2.0F ? 3.00F : 0.0F);
    	   float strengthAdded = 0.0F;
    	   if (tFirst)
    		   strengthAdded += SUtils.strength(sendToTrans.getUnits().getMatches(Matches.UnitIsTransport), false, true, tFirst);
    	   List<Unit> mySeaUnits = sendToTrans.getUnits().getMatches(mySeaAirUnit);
    	   mySeaUnits.removeAll(alreadyMoved);
    	   List<Unit> alliedSeaUnits = sendToTrans.getUnits().getMatches(alliedSeaAirAttackUnit);
    	   alliedSeaUnits.removeAll(mySeaUnits);
		   float alliedStrength = SUtils.strength(alliedSeaUnits, false, true, tFirst);
		   targetStrength -= alliedStrength;
		   strengthAdded += alliedStrength;
		   if (targetStrength <= 0.0F)
			   continue;
           List<Collection<Unit>> xUnits = new ArrayList<Collection<Unit>>();
           List<Unit> xMoved = new ArrayList<Unit>(alreadyMoved);
           List<Route> xRoutes = new ArrayList<Route>();
		   Iterator<Unit> mySeaIter = mySeaUnits.iterator();
		   while (mySeaIter.hasNext() && targetStrength <= 0.0F)
		   {
			   Unit myUnit = mySeaIter.next();
			   if (myAirUnit.match(myUnit))
				   continue;
			   float uStrength = 0.0F;
			   if (Matches.UnitIsCarrier.match(myUnit))
			   {
				   List<Unit> carrierGroup = new ArrayList<Unit>(sendToTrans.getUnits().getMatches(myCarrierGroup));
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
    	   float shipStrength = SUtils.inviteShipAttack(sendToTrans, targetStrength, xMoved, xUnits, xRoutes, data, player, false, tFirst, false);
    	   strengthAdded += shipStrength;
    	   moveUnits.addAll(xUnits);
    	   moveRoutes.addAll(xRoutes);
    	   alreadyMoved.addAll(xMoved);
       }
	   int totShipMoves = moveUnits.size();
	   for (int i=0; i < totShipMoves; i++)
	   {
		   Collection<Unit> newUnits = moveUnits.get(i);
		   Route thisRoute = moveRoutes.get(i);
		   Territory endTerr = thisRoute.getEnd();
		   if (shipsMap.containsKey(endTerr))
			   newUnits.addAll(shipsMap.get(endTerr));
		   shipsMap.put(endTerr, newUnits);
		   
	   }
	   setShipsMovedMap(shipsMap);
    }
    
    private void secondLookSea(final GameData data,List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {
    	Match<Unit> ownedUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
        Match<Unit> enemySeaUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.enemyUnit(player, data));
        Match<Unit> seaAttackUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsSea, Matches.UnitIsNotTransport);
        Match<Unit> transportUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsTransport);
        Match<Unit> airAttackUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsAir);
        Match<Unit> seaAirAttackUnit = new CompositeMatchOr<Unit>(seaAttackUnit, airAttackUnit);

        Match<Unit> alliedSeaAttackUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsSea, Matches.unitIsOwnedBy(player).invert());
        Match<Unit> alliedAirAttackUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsAir, Matches.unitIsOwnedBy(player).invert());
        Match<Unit> alliedTransport = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsTransport, Matches.unitIsOwnedBy(player).invert());
        Match<Unit> alliedSeaAirAttackUnit = new CompositeMatchOr<Unit>(alliedSeaAttackUnit, alliedAirAttackUnit);

        Match<Territory> routeCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data));
        Match<Territory> endCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data));
        List <Territory> seaAttackTerr = SUtils.findCertainShips(data, player, seaAttackUnit);
        boolean tFirst = transportsMayDieFirst();
        HashMap<Territory, Collection<Unit>> shipsMap = getShipsMovedMap();
        List<Unit> alreadyMoved = new ArrayList<Unit>();
        for (Territory moveTerr : seaAttackTerr)
        {
        	if (shipsMap.containsKey(moveTerr))
        		alreadyMoved.addAll(shipsMap.get(moveTerr));
        	List<Unit> attackUnits = moveTerr.getUnits().getMatches(seaAirAttackUnit);
        	attackUnits.removeAll(alreadyMoved);
        	if (attackUnits.isEmpty())
        		continue;
        	int moveDist = MoveValidator.getLeastMovement(attackUnits);
        	if (moveDist == 0)
        		continue;
        	List<Unit> transportUnits = moveTerr.getUnits().getMatches(transportUnit);
        	boolean transportUnitsPresent = transportUnits.size() > 0;
        	List<Unit> alliedTransports = moveTerr.getUnits().getMatches(alliedTransport);
        	float thisThreat = SUtils.getStrengthOfPotentialAttackers(moveTerr, data, player, tFirst, false, null);
        	float myStrength = SUtils.strength(attackUnits, false, true, tFirst);
        	float alliedStrength = SUtils.strength(moveTerr.getUnits().getMatches(alliedSeaAirAttackUnit), false, true, tFirst);
        	boolean alliedUnitsPresent = alliedStrength > 0.0F || alliedTransports.size() > 0;
        	if ((alliedUnitsPresent && alliedStrength > thisThreat*0.75F) || thisThreat == 0.0F 
        			|| (!alliedUnitsPresent && !transportUnitsPresent)) //don't need us here
        	{
       			int maxUnits = 100;
       			//Route eRoute = SUtils.findNearest(moveTerr, endCond, routeCond, data);
       			Route eRoute = SUtils.findNearestMaxContaining(moveTerr, endCond, routeCond, enemySeaUnit, maxUnits, data);
       			if (eRoute == null)
       				continue;
       			if (MoveValidator.validateCanal(eRoute, player, data) == null)
       			{
       				if (eRoute.getLength() > moveDist)
       				{
       					Route changeRoute = new Route();
       					changeRoute.setStart(moveTerr);
       					for (int i = 1; i <= moveDist; i++)
       						changeRoute.add(eRoute.getTerritories().get(i));
       					eRoute = changeRoute;
       				}
       			}
       			if (MoveValidator.validateCanal(eRoute, player, data) == null) //check again
       				continue;
       			Route eRoute2 = SUtils.getMaxSeaRoute(data, moveTerr, eRoute.getEnd(), player, false, moveDist);
       			if (eRoute2 == null || eRoute2.getEnd() == null)
       				continue;
       			float endStrength = SUtils.getStrengthOfPotentialAttackers(eRoute2.getEnd(), data, player, tFirst, false, null);
       			Route xRoute = new Route();
       			if (myStrength > endStrength)
       				xRoute = eRoute2;
       			else
       			{
       				eRoute2.getTerritories().remove(eRoute2.getEnd());
       				float endStrength2 = SUtils.getStrengthOfPotentialAttackers(eRoute2.getEnd(), data, player, tFirst, false, null);
       				float myStrength2 = SUtils.strength(eRoute2.getEnd().getUnits().getMatches(Matches.alliedUnit(player, data)), false, true, tFirst);
       				myStrength2 += myStrength;
       				if (myStrength2 > endStrength2*0.65F)
       					xRoute = eRoute2;
       				else
       					xRoute = null;
       			}
        		if (xRoute != null)
        		{
    				List<Unit> tUnits = new ArrayList<Unit>();
        			if (MoveValidator.hasEnoughMovement(attackUnits, xRoute))
        				tUnits.addAll(attackUnits);
        			else
        			{
        				for (Unit moveUnit : attackUnits)
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
	 * @param maxTrans -
	 *            if -1 unlimited
	 */
    private void populateNonCombatSea(boolean nonCombat, final GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {

       boolean tFirst = transportsMayDieFirst();
       Collection<Territory> impassableTerrs = getImpassableTerrs();
       final Collection<Unit> alreadyMoved = new HashSet<Unit>();
       Territory myCapital = TerritoryAttachment.getCapital(player, data);
       HashMap<Territory, Collection<Unit>> shipsMovedMap = getShipsMovedMap();
	   Match<Unit> notAlreadyMoved =new CompositeMatchAnd<Unit>(new Match<Unit>()
		   {
				public boolean match(Unit o)
				{
					return !alreadyMoved.contains(o);
				}
			});
	   Match<Unit> ownedUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
	   Match<Unit> ownedAC = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsCarrier);
	   Match<Unit> HasntMoved2 = new CompositeMatchOr<Unit>(HasntMoved, notAlreadyMoved);
       Match<Unit> enemySeaUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.enemyUnit(player, data));
       Match<Unit> enemyAirUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsAir, Matches.enemyUnit(player, data));
       Match<Unit> enemyLandUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.enemyUnit(player, data));
       Match<Unit> landOrAirEnemy = new CompositeMatchOr<Unit>(enemyAirUnit, enemyLandUnit);
       Match<Unit> seaAttackUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsSea, Matches.UnitIsNotTransport);
       Match<Unit> airAttackUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsAir);
       Match<Unit> subUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsSub);
       Match<Unit> seaAirAttackUnit = new CompositeMatchOr<Unit>(seaAttackUnit, airAttackUnit);
	   Match<Unit> seaAirAttackUnitNotMoved = new CompositeMatchAnd<Unit>(seaAirAttackUnit, HasntMoved2);
       Match<Unit> fighterUnit = new CompositeMatchAnd<Unit>(Matches.UnitCanLandOnCarrier, ownedUnit, HasntMoved2);
       Match<Unit> fighterUnit2 = new CompositeMatchAnd<Unit>(Matches.UnitCanLandOnCarrier, ownedUnit);
       Match<Unit> bomberUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsStrategicBomber, ownedUnit, HasntMoved2);

       Match<Unit> alliedSeaAttackUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsSea);
       Match<Unit> alliedAirAttackUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsAir);
       Match<Unit> alliedSeaAirAttackUnit = new CompositeMatchOr<Unit>(alliedSeaAttackUnit, alliedAirAttackUnit);
       
       Match<Territory> noNeutralOrAA = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassable, Matches.territoryHasEnemyAA(player, data).invert());
       Match<Territory> noEnemyWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data));
       Match<Territory> enemyWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data));

       List <Territory> seaAttackTerr = SUtils.findCertainShips(data, player, seaAttackUnit);
       List <Territory> enemySeaTerr = SUtils.findUnitTerr(data, player, enemySeaUnit);
       List <Territory> mySubTerr = SUtils.findUnitTerr(data, player, subUnit);
       List <Territory> myFighterTerr = SUtils.findUnitTerr(data, player, fighterUnit2);
       List <Territory> skippedTerr = new ArrayList<Territory>();
       /**
        * First determine if attack ships have been purchased and limit moves at that factory
        */
       List<Unit> xMoved = new ArrayList<Unit>();
       markBaseShips(data, player, xMoved);
       Territory seaFactTerr = getSeaTerr();
       /*
        * If we are locking down ships around capital, find the strongest point to combine ships
        * Make it the favorite for placing ships
        */
       if (xMoved.size() > 0)
       {
    	   Set<Territory> neighborList = data.getMap().getNeighbors(myCapital, Matches.TerritoryIsWater);
    	   List<Route> xR = new ArrayList<Route>();
    	   List<Collection<Unit>> xM = new ArrayList<Collection<Unit>>();
    	   List<Unit> xAM = new ArrayList<Unit>();
    	   int maxShips = 0;
    	   Territory maxShipTerr = null, maxStrengthTerr = null;
    	   float maxStrength = 0.0F;
    	   float goStrength = 1000.0F;
    	   for (Territory nT : neighborList)
    	   {
    		   float thisStrength = SUtils.inviteShipAttack(nT, goStrength, xAM, xM, xR, data, player, false, tFirst, true);
    		   int unitCount = xAM.size();
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
//TODO: incorporate intelligence between maxStrength & maxShip
    	   if (maxStrengthTerr != null)
    	   {
    		   SUtils.inviteShipAttack(maxStrengthTerr, goStrength, alreadyMoved, moveUnits, moveRoutes, data, player, false, tFirst, true);
    		   seaFactTerr = maxStrengthTerr;
    		   setSeaTerr(seaFactTerr);
    	   }
    	   else if (maxShipTerr != null)
    	   {
    		   SUtils.inviteShipAttack(maxShipTerr, goStrength, alreadyMoved, moveUnits, moveRoutes, data, player, false, tFirst, true);
    		   seaFactTerr = maxShipTerr;
    		   setSeaTerr(seaFactTerr);
    	   }
    		   
       }
       else if (seaFactTerr != null)
       {
    	   float seaFactStrength = SUtils.getStrengthOfPotentialAttackers(seaFactTerr, data, player, tFirst, false, null);
    	   List<Unit> seaUnitsPurchased = player.getUnits().getMatches(Matches.UnitIsSea);
    	   seaFactStrength -= SUtils.strength(seaUnitsPurchased, false, true, tFirst);
    	   if (seaFactStrength > 0.0F)
    	   {
    		   SUtils.inviteShipAttack(seaFactTerr, seaFactStrength, alreadyMoved, moveUnits, moveRoutes, data, player, false, tFirst, true);
    	   }
       }

       alreadyMoved.addAll(xMoved);
       List<Territory> transTerr = SUtils.findCertainShips(data, player, Matches.UnitIsTransport);
       IntegerMap<Territory> transMap = new IntegerMap<Territory>();
       HashMap<Territory, Float> transStrengthMap = new HashMap<Territory, Float>();
       for (Territory tT : transTerr)
       {
    	   float tStrength = SUtils.getStrengthOfPotentialAttackers(tT, data, player, tFirst, false, null);
    	   int tUnits = tT.getUnits().countMatches(Matches.UnitIsTransport);
    	   transMap.put(tT, tUnits);
    	   transStrengthMap.put(tT, tStrength);
    	   
       }
       SUtils.reorder(transTerr, transMap, true);
       List<Territory> transTerr2 = new ArrayList<Territory>(transTerr);
       for (Territory trans : transTerr2)
       {
    	   Collection<Unit> ourAttackUnits = trans.getUnits().getUnits();
    	   float ourTStrength = SUtils.strength(ourAttackUnits, false, true, tFirst);
    	   float eStrength = transStrengthMap.get(trans).floatValue();
    	   if (eStrength < 0.50F) //enemy has nothing here
    	   {
    		   transTerr.remove(trans);
    	   }
    	   //lock down enough units to protect
    	   float strengthNeeded = eStrength;
    	   List<Unit> alreadyCounted = new ArrayList<Unit>();
    	   for (Unit aUnit : ourAttackUnits)
    	   { //only allow fighters to be counted with carriers...otherwise they have to land somewhere else
    		   UnitType uT = aUnit.getType();
    		   if (strengthNeeded <= 0.0F || alreadyCounted.contains(aUnit) || Matches.UnitTypeCanLandOnCarrier.match(uT))
    			   continue;
    		   if (Matches.UnitTypeIsCarrier.match(uT))
    		   {
    			   strengthNeeded -= SUtils.uStrength(aUnit, false, true, tFirst);
    			   int numFighters = UnitAttachment.get(uT).getCarrierCapacity();
    			   for (Unit aUnit2 : ourAttackUnits)
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
    	   alreadyMoved.addAll(alreadyCounted);
       }
	   int maxUnits = 0;
	   Route eShipRoute = SUtils.findNearest(myCapital, enemyWater, noEnemyWater, data);
       Territory goHere = null, seaTarget = null;
	   if (eShipRoute != null && eShipRoute.getLength() <=5)
		   goHere = eShipRoute.getEnd();
	   
       float alliedStrength = 0.0F, badGuyStrength = 0.0F, ownedStrength = 0.0F;
       //first check our attack ship territories
       for (Territory myTerr : seaAttackTerr)
       {
		  List <Unit> myAttackUnits = myTerr.getUnits().getMatches(seaAirAttackUnit);
		  List <Unit> alliedAttackUnits = myTerr.getUnits().getMatches(alliedSeaAirAttackUnit);
		  if (shipsMovedMap.containsKey(myTerr))
			  alreadyMoved.addAll(shipsMovedMap.get(myTerr));
	      boolean keepGoing = true;
	      badGuyStrength = SUtils.getStrengthOfPotentialAttackers(myTerr, data, player, tFirst, false, null);
		  ownedStrength = SUtils.strength(myAttackUnits, false, true, tFirst);
		  alliedStrength = SUtils.strength(alliedAttackUnits, false, true, tFirst);
		  if ((alliedStrength > 1.00F && alliedStrength + 6.00F > badGuyStrength*0.65F) && (badGuyStrength > 2.00F))
		  { //where is the source of the attack?
			  Set<Territory> bgSourceTerr = data.getMap().getNeighbors(myTerr, 2);
			  bgSourceTerr.removeAll(impassableTerrs);
			  Territory mainSourceTerr = null;
			  for (Territory bgSource : bgSourceTerr)
			  {
				  if (Matches.TerritoryIsWater.match(bgSource) && Matches.territoryHasEnemyUnits(player, data).match(bgSource))
				  {
					  List<Unit> bgUnits = bgSource.getUnits().getMatches(Matches.enemyUnit(player, data));
					  float bgTerrStrength = SUtils.strength(bgUnits, true, true, tFirst);
					  if (bgTerrStrength > 0.5F*badGuyStrength)
						  mainSourceTerr = bgSource;
				  }
			  }
			  if (mainSourceTerr != null)
			  {
				  Set<Territory> sourceNeighbors = data.getMap().getNeighbors(mainSourceTerr, 2);
				  sourceNeighbors.removeAll(impassableTerrs);
				  float maxStrength = 0.0F;
				  Territory maxStrengthTerr = null;
				  for (Territory sN : sourceNeighbors)
				  {
					  if (Matches.TerritoryIsWater.match(sN) && Matches.territoryHasNoAlliedUnits(player, data).invert().match(sN) && !skippedTerr.contains(sN))
					  {
						  List<Unit> sNUnits = sN.getUnits().getMatches(Matches.alliedUnit(player, data));
						  sNUnits.removeAll(alreadyMoved);
						  if (sNUnits.size() == 0)
							  continue;
						  float quickStrength = SUtils.strength(sNUnits, false, true, tFirst);
						  if (quickStrength > maxStrength)
						  {
							  maxStrength = quickStrength;
							  maxStrengthTerr = sN;
						  }
					  }
				  }
				  if (maxStrengthTerr != null)
				  {
					  float newBadGuyStrength = (badGuyStrength*0.75F - maxStrength);
					  SUtils.inviteShipAttack(maxStrengthTerr, newBadGuyStrength, alreadyMoved, moveUnits, moveRoutes, data, player, false, tFirst, false);
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
 //This overrides everything below, but it gets the ships moving...obviously we may be sacrificing them...
		  Route quickRoute = null;
		  int minSeaDist = 100;
		  int moveDist = MoveValidator.getLeastMovement(myAttackUnits);
		  if (badGuyStrength > alliedStrength*1.65F+3.0F)
		  {
			  Set<Territory> myMoveNeighbors = data.getMap().getNeighbors(myTerr, 2);
			  myMoveNeighbors.removeAll(impassableTerrs);
			  HashMap<Territory, Float> MNmap = new HashMap<Territory, Float>();
			  for (Territory MNterr : myMoveNeighbors)
			  {
				  if (!MNterr.isWater() || Matches.territoryHasEnemyUnits(player, data).match(MNterr))
					  continue;
				  float enemyStrength = SUtils.getStrengthOfPotentialAttackers(MNterr, data, player, tFirst, true, null);
				  float MNStrength = SUtils.strength(MNterr.getUnits().getMatches(Matches.alliedUnit(player, data)), false, true, tFirst);
				  MNStrength += ownedStrength;
				  MNmap.put(MNterr, enemyStrength - MNStrength);
			  }
			  Set<Territory> MNterrs = MNmap.keySet();
			  List<Territory> MNterrs2 = new ArrayList<Territory>(MNterrs);
			  SUtils.reorder(MNterrs2, MNmap, true);
			  Iterator<Territory> MNIter = MNterrs2.iterator();
			  boolean MNdone = false;
			  goHere = null;
			  while (MNIter.hasNext() && !MNdone)
			  {
				  Territory MNterr = MNIter.next();
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
				  moveUnits.add(myAttackUnits);
				  moveRoutes.add(quickRoute);
				  alreadyMoved.addAll(myAttackUnits);
				  continue;
			  }
		  }
		  for (Territory badSeaTerr : enemySeaTerr)
		  {
			  Route seaCheckRoute = SUtils.getMaxSeaRoute(data, myTerr, badSeaTerr, player, false, moveDist);
			  if (seaCheckRoute == null)
				  continue;
			  int newDist = seaCheckRoute.getLength();
			  if (newDist < minSeaDist)
			  {
				  goHere = badSeaTerr;
				  minSeaDist = newDist;
				  quickRoute = seaCheckRoute;
			  }
		  }
		  myAttackUnits.removeAll(alreadyMoved);
		  Iterator<Unit> checkIter = myAttackUnits.iterator();
		  while (checkIter.hasNext())
		  {
			  Unit checkOne = checkIter.next();
			  if (!HasntMoved.match(checkOne))
				  checkIter.remove();
		  }
		  if (myAttackUnits.size() > 0 && goHere != null && quickRoute != null)
		  {
			  float goHereStrength = SUtils.getStrengthOfPotentialAttackers(goHere, data, player, tFirst, false, null);
			  float ourStrength = SUtils.strength(myAttackUnits, false, true, tFirst) + SUtils.strength(goHere.getUnits().getMatches(alliedSeaAirAttackUnit), false, true, tFirst);
			  if (ourStrength >= goHereStrength*0.75F)
			  {
				  moveUnits.add(myAttackUnits);
				  moveRoutes.add(quickRoute);
				  alreadyMoved.addAll(myAttackUnits);
			  }
			  else
				  skippedTerr.add(myTerr);
		  }
		  else
			  skippedTerr.add(myTerr);
	      goHere=null;
	      if (badGuyStrength == 0.0F)
	      {
	    	  Route eRoute = SUtils.findNearest(myTerr, enemyWater, noEnemyWater, data);
	    	  if (eRoute != null)
	    	  {
	    		  int eLength = eRoute.getLength();
	    		  if (eRoute.getEnd() != null)
	    		  {
	    			  boolean moveForward = false;
    				  List<Unit> canGoUnits = new ArrayList<Unit>(myAttackUnits);
    				  canGoUnits.removeAll(alreadyMoved);
    				  ownedStrength = SUtils.strength(canGoUnits, false, true, tFirst);
    				  Territory theTarget = null;
	    			  if (eLength <= 4)
	    			  {
	    				  Territory endTerr = eRoute.getEnd();
	    				  float eStrength = SUtils.strength(endTerr.getUnits().getUnits(), false, true, tFirst);
	    				  float xtraEStrength = SUtils.getStrengthOfPotentialAttackers(endTerr, data, player, tFirst, false, null);
	    				  float potentialStrength = eStrength*0.75F + 0.25F*xtraEStrength;
	    				  if (ownedStrength > potentialStrength)
	    				  {
	    					  theTarget = eRoute.getTerritories().get(eRoute.getLength()-1);
	    					  moveForward = true;
	    				  }
	    			  }
	    			  else
	    			  {
	    				  theTarget = eRoute.getTerritories().get(2);
	    				  float eStrength = SUtils.getStrengthOfPotentialAttackers(theTarget, data, player, tFirst, false, null);
	    				  if (ownedStrength > eStrength*0.65F)
	    					  moveForward = true;
	    				  else
	    				  {
	    					  theTarget = eRoute.getTerritories().get(1);
		    				  float xEStrength = SUtils.getStrengthOfPotentialAttackers(theTarget, data, player, tFirst, false, null);
		    				  if (ownedStrength > xEStrength*0.45F)
		    					  moveForward = true;
	    				  }
	    				  
	    			  }
	    			  if (moveForward)
	    			  {
	    				  moveDist = MoveValidator.getLeastMovement(canGoUnits);
	    				  Route canGoRoute = SUtils.getMaxSeaRoute(data, myTerr, theTarget, player, false, moveDist);
	    				  moveUnits.add(canGoUnits);
	    				  moveRoutes.add(canGoRoute);
	    				  alreadyMoved.addAll(canGoUnits);
	    			  }
	    		  }
	    	  }
	      }

       }
	   HashMap<Territory, Float> enemyMap = new HashMap<Territory, Float>();
	   List<Territory> enemyTerr = SUtils.findUnitTerr(data, player, enemySeaUnit);
	   int numTerr=enemyTerr.size();
	   for (Territory t2 : enemyTerr) //find strength of all enemy terr (defensive)
	   {
	       enemyMap.put(t2, SUtils.strength(t2.getUnits().getMatches(enemySeaUnit), false, true, tFirst));
	   }
	   SUtils.reorder(enemyTerr, enemyMap, true);
	   for (Territory enemy : enemyTerr)
	   {
		   List<Territory> ourShipTerrs = SUtils.findOurShips(enemy, data, player);
		   for (Territory shipTerr : ourShipTerrs)
		   {
			   if (!shipTerr.isWater())
				   continue;
			   if (data.getMap().getNeighbors(shipTerr, enemyWater).size() > 0)
			   {
				   skippedTerr.add(shipTerr);
				   continue;
			   }
			   List<Territory> Neighbors2 = SUtils.getExactNeighbors(shipTerr, 2, player, false);
			   boolean continueOn = true;
			   for (Territory N2 : Neighbors2)
			   {
				   if (enemyWater.match(N2))
					   continueOn = false;
			   }
			   if (!continueOn)
			   {
				   skippedTerr.add(shipTerr);
				   continue;
			   }
			   float eS1 = SUtils.getStrengthOfPotentialAttackers(shipTerr, data, player, tFirst, true, null);
			   Set<Territory> lookAroundTerr = data.getMap().getNeighbors(shipTerr, 5);
			   lookAroundTerr.removeAll(impassableTerrs);
			   List<Territory> hasEnemyShips = new ArrayList<Territory>();
			   for (Territory eShipTerr : lookAroundTerr)
			   {
				   if (enemyWater.match(eShipTerr))
					   hasEnemyShips.add(eShipTerr);
			   }
			   
			   List<Unit> moveableUnits = shipTerr.getUnits().getMatches(seaAirAttackUnitNotMoved);
			   moveableUnits.removeAll(alreadyMoved);
			   Iterator<Unit> mUIter = moveableUnits.iterator();
			   while (mUIter.hasNext())
			   {
				   Unit mU = mUIter.next();
				   if (!MoveValidator.hasEnoughMovement(mU, 1))
					   mUIter.remove();
			   }
			   List<Unit> unMoveableUnits = shipTerr.getUnits().getMatches(Matches.unitHasMoved);
			   float unmoveableStrength = SUtils.strength(unMoveableUnits, false, true,  tFirst);
			   if (unmoveableStrength < eS1*.65F) //can we leave a ship behind and protect it?
			   {
				   float testStrength = unmoveableStrength;
				   List<Unit> leaveUnits = new ArrayList<Unit>();
				   for (Unit leaveUnit : moveableUnits)
				   {
					   if (testStrength < eS1*0.65F)
					   {
						   float addOn = SUtils.uStrength(leaveUnit, false, true, tFirst);
						   leaveUnits.add(leaveUnit);
						   testStrength += addOn;
					   }
				   }
				   moveableUnits.removeAll(leaveUnits);
			   }
			   if (moveableUnits.size() > 0 && hasEnemyShips.size() == 1)
			   {
				   float moveableStrength = SUtils.strength(moveableUnits, false, true, tFirst);
				   Territory enemyShipTerr = hasEnemyShips.get(0);
				   Route nRoute = data.getMap().getWaterRoute(shipTerr, enemyShipTerr);
				   if (nRoute == null)
					   continue;
				   int moveDist = MoveValidator.getLeastMovement(moveableUnits);
				   if (MoveValidator.validateCanal(nRoute, player, data) != null)
				   {
					   nRoute = SUtils.getMaxSeaRoute(data, shipTerr, enemyShipTerr, player, false, moveDist);
					   if (nRoute == null)
						   continue;
				   }
				   else
				   {
					   Route nRoute2 = new Route();
					   int goLength = nRoute.getLength();
					   Territory goPoint = (moveDist >= goLength) ? nRoute.getEnd() : nRoute.getTerritories().get(moveDist); 
					   float goPointStrength = SUtils.getStrengthOfPotentialAttackers(goPoint, data, player, tFirst, false, null);
					   if (goPoint != nRoute.getEnd())
					   {
						   nRoute2.setStart(shipTerr);
						   for (int i=1; i <= moveDist; i++)
							   nRoute2.add(nRoute.getTerritories().get(i));
						   nRoute = nRoute2;
					   }
					   if (goPointStrength*0.55F < moveableStrength)
					   {
						   moveUnits.add(moveableUnits);
						   moveRoutes.add(nRoute2);
						   alreadyMoved.addAll(moveableUnits);
					   }
				   }
				   
			   }
		   }
	   }
	   //check the skipped Territories...see if there are ships we can combine
	   List<Territory> dontMoveFrom = new ArrayList<Territory>();
	   for (Territory check1 : skippedTerr)
	   {
		   for (Territory check2 : skippedTerr)
		   {
			   if (check1 == check2 || dontMoveFrom.contains(check2))
			      continue;
			   List<Territory> beenThere = new ArrayList<Territory>();
			   int check1Dist = SUtils.distanceToEnemy(check1, beenThere, data, player, true);
			   int check2Dist = SUtils.distanceToEnemy(check2, beenThere, data, player, true);
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
			   List<Unit> swapUnits = start.getUnits().getMatches(seaAirAttackUnitNotMoved);
			   swapUnits.removeAll(alreadyMoved);
			   if (swapUnits.isEmpty())
				   continue;
			   int swapDist = MoveValidator.getLeastMovement(swapUnits);
			   Route swapRoute = SUtils.getMaxSeaRoute(data, start, stop, player, false, swapDist);
			   if (swapRoute != null)
			   {
				   moveUnits.add(swapUnits);
				   moveRoutes.add(swapRoute);
				   alreadyMoved.addAll(swapUnits);
				   dontMoveFrom.add(stop); //make sure check1 is blocked on the 2nd pass...ships are moving to it
			   }
		   }
	   }
	   List<Territory> fTerr = SUtils.findUnitTerr(data, player, fighterUnit);
	   List<Territory> bTerr = SUtils.findUnitTerr(data, player, bomberUnit);
	   List<Territory> allTerr = new ArrayList<Territory>();
	   if (fTerr != null)
	   	  allTerr.addAll(fTerr);
	   if (bTerr != null)
	      allTerr.addAll(bTerr);

 	   if (nonCombat)
	   {
		  for (Territory newTerr : allTerr)
		  {
			 boolean enemyFound = false;
			 Set<Territory> sNewTerr = data.getMap().getNeighbors(newTerr, 2);
			 sNewTerr.removeAll(impassableTerrs);
			 for (Territory cEnemyTerr : sNewTerr)
			 {
				 if (Matches.territoryHasEnemyUnits(player, data).match(cEnemyTerr))
				 	enemyFound = true;
			 }
			 if (enemyFound)
			     continue;
			 Territory capTerr = null;
			 int minDist = 0;
			 Territory goPoint = SUtils.getAlliedLandTerrNextToEnemyCapital(minDist, capTerr, newTerr, data, player);
			 Route capRoute = data.getMap().getRoute(newTerr, goPoint, noNeutralOrAA);
		     if (capRoute == null)
			     	continue;

		  	 int cRLen = capRoute.getLength();
		  	 boolean foundit = false;
			 Territory BtargetTerr = null;
			 Territory FtargetTerr = null;
			 List<Territory> cRTerrs = capRoute.getTerritories();
			 Iterator<Territory> cRIter = cRTerrs.iterator();
			 for (int i=cRLen-1; i >= 0; i-- )
			 {
				 goPoint = cRTerrs.get(i);
				 float testStrength = SUtils.getStrengthOfPotentialAttackers(goPoint, data, player, tFirst, true, null);
				 float ourStrength = SUtils.strength(goPoint.getUnits().getMatches(Matches.alliedUnit(player, data)), false, false, tFirst);
				 if (ourStrength > 0.65F*testStrength && i <= 4 && Matches.isTerritoryAllied(player, data).match(goPoint))
				 {
					 FtargetTerr = goPoint;
					 foundit = true;
				 }
				 if (ourStrength > 0.65F*testStrength && i <= 6 && Matches.isTerritoryAllied(player, data).match(goPoint))
				 {
					 BtargetTerr = goPoint;
					 foundit = true;
				 }
				 
			 }
			if (foundit)
			{
				List <Unit> fAirUnits = newTerr.getUnits().getMatches(fighterUnit);
				fAirUnits.removeAll(alreadyMoved);
				List <Unit> bombUnits = newTerr.getUnits().getMatches(bomberUnit);
				bombUnits.removeAll(alreadyMoved);
				Route BcapRoute = data.getMap().getRoute(newTerr, BtargetTerr, noNeutralOrAA);
				Route FcapRoute = data.getMap().getRoute(newTerr, FtargetTerr, noNeutralOrAA);
				if (BcapRoute != null && bombUnits.size() > 0 && MoveValidator.canLand(bombUnits, BtargetTerr, player, data))
				{
					boolean canLand = true;
					for (Unit b1 : bombUnits)
					{
						if (canLand)
							canLand = SUtils.airUnitIsLandable(b1, newTerr, BtargetTerr, player, data);
					}
					if (canLand)
					{
						moveRoutes.add(BcapRoute);
						moveUnits.add(bombUnits);
						alreadyMoved.addAll(bombUnits);
					}
				}
				if (FcapRoute != null && fAirUnits.size() > 0 && !newTerr.getUnits().someMatch(ownedAC) && MoveValidator.canLand(fAirUnits, FtargetTerr, player, data))
				{
					boolean canLand = true;
					for (Unit f1 : fAirUnits)
					{
						if (canLand)
							canLand = SUtils.airUnitIsLandable(f1, newTerr, FtargetTerr, player, data);
					}
					if (canLand)
					{
						moveRoutes.add(FcapRoute);
						moveUnits.add(fAirUnits);
						alreadyMoved.addAll(fAirUnits);
					}
				}
			}
		  }
	   }
	   //other planes...move toward the largest enemy mass of units


      for (Territory subTerr : mySubTerr)
      {
		if (!subTerr.isWater() || seaTarget == null || subTerr == seaTarget)
			continue;
		List<Unit> allMyUnits = subTerr.getUnits().getMatches(ownedUnit);
		allMyUnits.removeAll(alreadyMoved);
		if (allMyUnits.isEmpty())
			continue;
		int unitDist = MoveValidator.getMaxMovement(allMyUnits);
		Route myRoute = SUtils.getMaxSeaRoute(data, subTerr, seaTarget, player, false, unitDist);
		if (myRoute == null)
			continue;
		List<Unit> moveThese = new ArrayList<Unit>();
		for (Unit sendUnit : allMyUnits)
		{
			if (MoveValidator.hasEnoughMovement(sendUnit, myRoute))
				moveThese.add(sendUnit);
		}
		moveUnits.add(moveThese);
		moveRoutes.add(myRoute);
	  }
//      SUtils.verifyMoves(moveUnits, moveRoutes, data, player);
    }

	private void nonCombatPlanes(final GameData data, final PlayerID player, List<Collection<Unit>> moveUnits, List<Route> moveRoutes)
	{
	  //specifically checks for available Carriers and finds a place for plane
        final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
	    Match<Unit> ownedUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
        Match<Unit> ACOwned = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsCarrier);
        Match<Unit> ACAllied = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsCarrier);
        Match<Unit> fighterAndAllied = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitCanLandOnCarrier);
        Match<Unit> fighterAndOwned = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitCanLandOnCarrier);
        Match<Unit> alliedUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data));
        List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
        CompositeMatch<Territory> notNeutralOrAA = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassable, Matches.territoryHasEnemyAA(player, data).invert());
        Territory myCapital = TerritoryAttachment.getCapital(player, data);
        boolean capDanger = markFactoryUnits(data, player, unitsAlreadyMoved);
        boolean tFirst = transportsMayDieFirst();
        List<Territory> fighterTerr = SUtils.findCertainShips(data, player, fighterAndOwned);
        List<Territory> alliedThreats = new ArrayList<Territory>();
        boolean alliedDanger = SUtils.threatToAlliedCapitals(data, player, alliedThreats, tFirst);
        if (alliedDanger)
        {
        	for (Territory aThreat : alliedThreats)
        	{
        		if (aThreat.getUnits().someMatch(fighterAndOwned))
        			unitsAlreadyMoved.addAll(aThreat.getUnits().getMatches(fighterAndOwned));
        	}
        }
        List <Territory> acTerr1 = SUtils.ACTerritory(player, data);
        if (acTerr1.size() == 0)
        {
        	return;
        }
        IntegerMap<Territory> acSpaceMap = new IntegerMap<Territory>();
        HashMap<Territory, Float> acAttackMap = new HashMap<Territory, Float>();
        for (Territory ACMap : acTerr1)
        {
        	float ACMapStrength = SUtils.getStrengthOfPotentialAttackers(ACMap, data, player, tFirst, false, null);
        	acAttackMap.put(ACMap, ACMapStrength);
        }
        SUtils.reorder(acTerr1, acAttackMap, true);
        for (Territory ACMap : acTerr1)
        {
        	List<Unit> ACMapUnits = ACMap.getUnits().getMatches(ACOwned);
        	int ownedCarrierSpace = 0;
        	for (Unit carrier1 : ACMapUnits)
        		ownedCarrierSpace += UnitAttachment.get(carrier1.getType()).getCarrierCapacity();
        	List<Unit> ACAlliedMapUnits = ACMap.getUnits().getMatches(ACAllied);
        	int alliedCarrierSpace = 0;
        	for (Unit carrier1 : ACAlliedMapUnits)
        		alliedCarrierSpace += UnitAttachment.get(carrier1.getType()).getCarrierCapacity();
        	List<Unit> ACfighterUnits = ACMap.getUnits().getMatches(fighterAndOwned);
        	List<Unit> ACAlliedfighterUnits = ACMap.getUnits().getMatches(fighterAndAllied);
        	int xAlliedSpace = Math.max(ACAlliedfighterUnits.size() - alliedCarrierSpace, 0);
        	int aSpace = ownedCarrierSpace - ACfighterUnits.size() - xAlliedSpace;
        	acSpaceMap.put(ACMap, aSpace);
        }
        List <Territory> myFighterTerr = SUtils.findCertainShips(data, player, Matches.UnitCanLandOnCarrier);
        myFighterTerr.removeAll(acTerr1);
		for (Territory t : myFighterTerr)
		{
			List<Unit> tPlanes = t.getUnits().getMatches(fighterAndOwned);
			if (tPlanes.size() <= 0)
				continue;
		   for (Territory acT : acTerr1)
		   {
			   Route acRoute = data.getMap().getRoute(t, acT, notNeutralOrAA);
		   	   if (acRoute == null)
		   		   continue;
		   	   List<Unit> fMoveUnits = new ArrayList<Unit>();
		   	   for (Unit fUnit : tPlanes)
		   	   {
		   		   if (MoveValidator.hasEnoughMovement(fUnit, acRoute))
		   			   fMoveUnits.add(fUnit);
		   	   }
		   	   if (fMoveUnits.size() == 0)
		   		   continue;
		   	   int availSpace = acSpaceMap.getInt(acT);
			   List<Unit> tempUnits = new ArrayList<Unit>();
			   if (availSpace > 0)
			   {
				   Iterator <Unit> fIter = fMoveUnits.iterator();
				   while (availSpace > 0 && fIter.hasNext())
				   {
					   Unit fMoveUnit = fIter.next();
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
			   else if ( availSpace < 0 && t.isWater() || delegate.getBattleTracker().wasBattleFought(t)) //need to move something off
			   {
				   List<Unit> alreadyMoved = new ArrayList<Unit>();
				   List<Unit> myFighters = acT.getUnits().getMatches(fighterAndOwned);
				   int maxPass = 0;
				   int fightersNum = myFighters.size();
				   while (availSpace < 0 && maxPass <= fightersNum)
				   {
					   int max = 0;
					   maxPass++;
					   Iterator<Unit> iter = myFighters.iterator();
					   Unit moveIt = null;
					   while(iter.hasNext())
					   {
						   Unit unit = iter.next();
						   if (alreadyMoved.contains(unit))
							   continue;
						   int left = TripleAUnit.get(unit).getMovementLeft();
						   if (left >= max)
						   {
							   max = left;
							   moveIt = unit;
						   }
					   }
					   if (moveIt == null) //no planes can move!!!
						   continue;
					   Route nearRoute = SUtils.findNearest(acT, Matches.isTerritoryAllied(player, data), notNeutralOrAA, data);
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

    private void populateCombatMoveSea(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {
    	boolean isAmphib = isAmphibAttack(player, false);
    	boolean attackShipsPurchased = getAttackShipPurchase();
        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		boolean tFirst = transportsMayDieFirst();
        final Collection<Unit> unitsAlreadyMoved = new HashSet<Unit>();

        List<Collection<Unit>> attackUnits = new ArrayList<Collection<Unit>>();
        Collection <Territory> allBomberTerr = new ArrayList<Territory>();

	    Match<Unit> notAlreadyMoved =new CompositeMatchAnd<Unit>(new Match<Unit>()
		    {
				public boolean match(Unit o)
				{
					return !unitsAlreadyMoved.contains(o);
				}
		 	});
	    Match<Unit> ownedUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
		CompositeMatch<Unit> seaUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsSea, notAlreadyMoved);
		CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsAir, notAlreadyMoved);
		CompositeMatch<Unit> seaAirUnit = new CompositeMatchOr<Unit>(seaUnit, airUnit);
		CompositeMatch<Unit> alliedSeaUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsSea);
		CompositeMatch<Unit> alliedAirUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data),Matches.UnitIsAir);
		CompositeMatch<Unit> alliedSeaAirUnit = new CompositeMatchOr<Unit>(alliedAirUnit, alliedSeaUnit);
        CompositeMatch<Unit> attackable = new CompositeMatchAnd<Unit>(ownedUnit, notAlreadyMoved);
        CompositeMatch<Unit> enemySeaUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsSea);
        CompositeMatch<Unit> enemyAirUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsAir);
        CompositeMatch<Unit> enemyAirSeaUnit = new CompositeMatchOr<Unit>(enemySeaUnit, enemyAirUnit);
        CompositeMatch<Unit> enemyNonTransport = new CompositeMatchAnd<Unit>(enemySeaUnit, Matches.UnitIsNotTransport);
        CompositeMatch<Unit> enemySub = new CompositeMatchAnd<Unit>(enemySeaUnit, Matches.UnitIsSub);
        CompositeMatch<Unit> carrierUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsCarrier);
        CompositeMatch<Unit> myDestroyer = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsDestroyer);
        List<Territory> seaTerrAttacked = new ArrayList<Territory>();

        List<Route> attackRoute = new ArrayList<Route>();
		float attackFactor = 1.68F; //adjust to make attacks more or less likely (1.05 is too low)
		if (isAmphib)
			attackFactor = 1.68F; //if amphibious assume tendency to buy more ships
		HashMap<Territory, Float> sortTerritories = new HashMap<Territory, Float>();
		int numTerr = 0;
		List<Territory> enemyTerr = new ArrayList<Territory>();
		for (Territory t: data.getMap().getTerritories())
		{
			if (t.isWater() && t.getUnits().someMatch(enemySeaUnit))
			{
				sortTerritories.put(t, SUtils.strength(t.getUnits().getMatches(Matches.enemyUnit(player, data)), false, true, tFirst));
				enemyTerr.add(t);
				numTerr++;
			}
		}
		SUtils.reorder(enemyTerr, sortTerritories, true);
		//Find Bombers
        for (Territory bTerr: data.getMap())
        {
			if (bTerr.getUnits().someMatch(Matches.UnitIsStrategicBomber))
				allBomberTerr.add(bTerr);
		}
        int maxShipCount =0;
		Territory maxShipsTerr = null, seaPlaceFact = null;
		boolean seaTerrSet = false;
		Territory myCapital = TerritoryAttachment.getCapital(player, data);
		HashMap <Territory, Float> checkForMorePlanes = new HashMap<Territory, Float>();
		/**
		 * If ships were purchased because of large ship disadvantage, bring close ships to the spot
		 * unless we can take out the ships at a point next to our capital
		 * Can we take out the largest group and reduce enemy to a manageable size?
		 * Steps: 1) Find largest group
		 *        2) Figure out the remaining Strength
		 *        3) See if units purchased is > remaining Strength {attack!}
		 *        4)     if NOT, see how many units are remaining if we win battle
		 *        5) Add those to purchased set and see if > remaining Enemy Strength {attack!}
		 */
	//	if (attackShipsPurchased)
	//	{
			List<Collection<Unit>> xMoves2 = new ArrayList<Collection<Unit>>();
			List<Route> xRoutes2 = new ArrayList<Route>();
			List<Unit> xAlreadyMoved2 = new ArrayList<Unit>(unitsAlreadyMoved);
			seaPlaceFact = getSeaTerr();
			if (seaPlaceFact == null)
			{ //if purchasing didn't specify the factory, figure it out
				for (Territory bestTarget : SUtils.findCertainShips(data, player, Matches.UnitIsFactory))
				{
					int thisShipCount = SUtils.shipThreatToTerr(bestTarget, data, player, tFirst);
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
				seaTerrSet = true;
			}
			boolean attackGroup = false;
			if (myCapital != null && seaPlaceFact == myCapital)
			{ 
				List<Territory> eSTerr = SUtils.findUnits(myCapital, data, enemySeaUnit, 3);
				float totStrengthEnemyShips = 0.0F;
				int maxUnitCount = 0, totUnitCount = 0;
				float maxStrength = 0.0F;
				Territory largestGroupTerr = null;
				List<Unit> largestGroup = new ArrayList<Unit>();
				for (Territory eST : eSTerr)
				{
					List<Unit> enemyGroup = eST.getUnits().getMatches(enemySeaUnit);
					totUnitCount += enemyGroup.size();
					float thisGroupStrength = SUtils.strength(enemyGroup, false, true, tFirst );
					totStrengthEnemyShips += thisGroupStrength;
					if (thisGroupStrength > maxStrength)
					{
						maxStrength = thisGroupStrength;
						largestGroupTerr = eST;
						maxUnitCount = enemyGroup.size();
						largestGroup.addAll(enemyGroup);
					}
				}
				if (largestGroupTerr != null)
				{
					float remainingStrength = totStrengthEnemyShips*1.25F + 2.0F;
					float shipStrength = SUtils.inviteShipAttack(largestGroupTerr, remainingStrength, xAlreadyMoved2, xMoves2, xRoutes2, data, player, true, tFirst, tFirst);
//					s_logger.fine("Attacking: "+largestGroupTerr.getName()+"; Ship Strength: "+shipStrength);
					remainingStrength -= shipStrength;
					float planeStrength = SUtils.invitePlaneAttack(false, false, largestGroupTerr, remainingStrength, xAlreadyMoved2, xMoves2, xRoutes2, data, player);
					remainingStrength -= planeStrength;
					float thisAttackStrength = shipStrength + planeStrength;
					if (thisAttackStrength > maxStrength) //what happens if we knock out the biggest group?
					{
						float newStrength = totStrengthEnemyShips - thisAttackStrength;
						int remainingUnitCount = totUnitCount - maxUnitCount;
						List<Unit> ourShips = new ArrayList<Unit>();
						for (Collection<Unit> shipGroup : xMoves2)
						{
							ourShips.addAll(shipGroup);
						}
						IntegerMap<UnitType> ourUnits = SUtils.convertListToMap(ourShips);
						int ourOriginalCount = ourUnits.totalValues();
						IntegerMap<UnitType> enemyUnits = SUtils.convertListToMap(largestGroup);
						List<PlayerID> ePlayers = SUtils.getEnemyPlayers(data, player);
						PlayerID ePlayer = ePlayers.get(0);
						boolean weWin = SUtils.quickBattleEstimator(ourUnits, enemyUnits, player, ePlayer, true, Properties.getAirAttackSubRestricted(data));
						float adjustedStrength = SUtils.strength(player.getUnits().getMatches(seaAirUnit), true, true, tFirst);
						if (newStrength < adjustedStrength) //ATTACK!
							attackGroup = true;
						else if (weWin)
						{
							int remainingShips =  totUnitCount - ourUnits.totalValues() + player.getUnits().getMatches(seaAirUnit).size();
							if (remainingShips <= 2) //ATTACK!
								attackGroup = true;
						}
						if (attackGroup)
						{
							moveUnits.addAll(xMoves2);
							moveRoutes.addAll(xRoutes2);
							unitsAlreadyMoved.addAll(xAlreadyMoved2);
							seaTerrAttacked.add(largestGroupTerr);
						}
					}
				}
				int localMax = 0, localShipCount = 0;
				for (Territory x : data.getMap().getNeighbors(seaPlaceFact, Matches.TerritoryIsWater))
				{
					localShipCount = x.getUnits().countMatches(alliedSeaUnit);
					if (localShipCount > localMax)
					{
						maxShipsTerr = x;
						localMax = localShipCount;
					}
				}
				List<Territory> shipTerrAtCapitol = SUtils.findOnlyMyShips(seaPlaceFact, data, player, Matches.UnitIsSea);
				for (Territory xT : shipTerrAtCapitol)
				{
					List<Unit> myShips = xT.getUnits().getMatches(Matches.unitIsOwnedBy(player));
					myShips.removeAll(unitsAlreadyMoved);
					if (myShips.isEmpty())
						continue;
					int shipDist = MoveValidator.getLeastMovement(myShips);
					Route localShipRoute = SUtils.getMaxSeaRoute(data, xT, maxShipsTerr, player, true, shipDist);
					if (localShipRoute != null)
					{
						if (myShips.size() > 0)
						{
							moveUnits.add(myShips);
							moveRoutes.add(localShipRoute);
							unitsAlreadyMoved.addAll(myShips);
							seaTerrAttacked.add(maxShipsTerr);
							enemyTerr.remove(maxShipsTerr);
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
//		}
*/        for (Territory t2 : enemyTerr)
        {
			List<Collection<Unit>> xMoves = new ArrayList<Collection<Unit>>();
			List<Route> xRoutes = new ArrayList<Route>();
			List<Unit> xAlreadyMoved = new ArrayList<Unit>(unitsAlreadyMoved);
			List<Collection<Unit>> xPMoves = new ArrayList<Collection<Unit>>();
			List<Route> xPRoutes = new ArrayList<Route>();
			List<Unit> xPAlreadyMoved = new ArrayList<Unit>(unitsAlreadyMoved);

            Territory enemy = t2;

            float enemyStrength = sortTerritories.get(enemy).floatValue();
            List<Unit> enemySubs = t2.getUnits().getMatches(enemySub);
            float subStrength = SUtils.strength(enemySubs, false, true, tFirst);
            float strengthNeeded = attackFactor*enemyStrength + 3.0F;
            float ourStrength = 0.0F, alliedStrength = 0.0F;
            float maxStrengthNeeded = 2.4F * enemyStrength + 3.0F;
//            float minStrengthNeeded = Math.min(strengthNeeded + 5.0F, maxStrengthNeeded);
            float minStrengthNeeded = strengthNeeded;
            float starterStrength = minStrengthNeeded;
			if (tFirst && enemyStrength == 0.0F)
				continue;

            attackUnits.clear();
            attackRoute.clear();
            float planeStrength = 0.0F;
            boolean AttackShipsPresent = enemy.getUnits().someMatch(enemyNonTransport);
            /**
             * If only transports:
             * 1) What is the potential Attack @ t
             * 2) Do we have a ship unit block large enough to take it out?
             * 3) If not, can we send planes without moving there and stay away from danger where we are?
             * Remember that this will be low on the strength list, so already looked at major attacks
             */
            boolean shipsAttacked = false;
            if (!AttackShipsPresent) //all transports
            { 
                if (!tFirst)
                {
                	minStrengthNeeded = 3.0F;
                	maxStrengthNeeded = 3.0F;
                }
    			planeStrength = SUtils.invitePlaneAttack(false, false, enemy, minStrengthNeeded, xPAlreadyMoved, xPMoves, xPRoutes, data, player);
    			maxStrengthNeeded -= planeStrength;
    			minStrengthNeeded -= planeStrength;
				boolean nonTransport = false;
    			if (maxStrengthNeeded > 0.0F)
    			{
    				int thisTerrThreat = SUtils.shipThreatToTerr(enemy, data, player, tFirst);
					float realStrength = SUtils.getStrengthOfPotentialAttackers(enemy, data, player, tFirst, true, seaTerrAttacked);
					float strengthNow = maxStrengthNeeded;
					float shipStrength = SUtils.inviteShipAttack(enemy, maxStrengthNeeded, xAlreadyMoved, xMoves, xRoutes, data, player, true, tFirst, tFirst);
					if (planeStrength == 0.0F)
					{
						for (Collection<Unit> xUnits : xMoves)
						{
							for (Unit thisUnit : xUnits)
							{
								if (Matches.UnitIsNotTransport.match(thisUnit))
									nonTransport = true;
							}
						}
					}
					else
						nonTransport = true;
					minStrengthNeeded -= shipStrength;
					maxStrengthNeeded -= shipStrength;
    			}
    			boolean planesOk = !tFirst ? true :  (planeStrength > (attackFactor*enemyStrength + 3.0F) ? true : false);
    			if (nonTransport && (minStrengthNeeded < 0.0F && planesOk))//TODO: check this formula again
    			{
    				seaTerrAttacked.add(enemy);
    				moveRoutes.addAll(xRoutes);
    				for (Collection<Unit> xUnits : xMoves)
    					moveUnits.add(xUnits);
    				unitsAlreadyMoved.addAll(xAlreadyMoved);
    				if (maxStrengthNeeded > 0.0F)
    					checkForMorePlanes.put(enemy, maxStrengthNeeded);
    			}
    			continue;
            }
            starterStrength = minStrengthNeeded;
            boolean subNeedsDestroyer = Properties.getAirAttackSubRestricted(data);
            boolean enemySubsOnly = enemy.getUnits().allMatch(Matches.UnitIsSub);
            float shipStrength = 0.0F, destroyerStrength = 0.0F;

            if (enemySubsOnly &&  !subNeedsDestroyer)
            {
                planeStrength = SUtils.invitePlaneAttack(false, false, enemy, minStrengthNeeded, xAlreadyMoved, xMoves, xRoutes, data, player);
                minStrengthNeeded -= planeStrength;
                if (planeStrength <= 0.0F)
                {
                	shipStrength = SUtils.inviteShipAttack(enemy, minStrengthNeeded, xAlreadyMoved, xMoves, xRoutes, data, player, true, tFirst, tFirst);
                	minStrengthNeeded -= shipStrength;
                }
            }
            else
            {
            	if (subNeedsDestroyer)
            	{
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
            	}
            	shipStrength = SUtils.inviteShipAttack(enemy, minStrengthNeeded, xAlreadyMoved, xMoves, xRoutes, data, player, true, tFirst, tFirst);
            	minStrengthNeeded -= shipStrength;
            	shipStrength += destroyerStrength;
            	
                planeStrength = SUtils.invitePlaneAttack(false, false, enemy, minStrengthNeeded, xAlreadyMoved, xMoves, xRoutes, data, player);
                minStrengthNeeded -= planeStrength;

            }
        	if (shipStrength > 0.0F)
        		shipsAttacked = true;
        	ourStrength += shipStrength;
        	alliedStrength += shipStrength;
            ourStrength += planeStrength;
        	alliedStrength += ourStrength;
            if (planeStrength > strengthNeeded && !shipsAttacked && !enemySubsOnly) //good chance of losing a plane
            {
            	starterStrength += 3.0F;
            	minStrengthNeeded = Math.max(minStrengthNeeded, 3.0F);
            }
			if (minStrengthNeeded > 0.0F)
			{
				Set<Territory> alliedCheck = data.getMap().getNeighbors(enemy, 2); //TODO: assumption of distance = 2
				for (Territory qAlliedCheck : alliedCheck)
				{
					List<Unit> qAlliedUnits = qAlliedCheck.getUnits().getMatches(alliedSeaAirUnit);
					qAlliedUnits.removeAll(unitsAlreadyMoved);
					alliedStrength += SUtils.strength(qAlliedUnits, true, true, tFirst);
				}
			   if (alliedStrength > strengthNeeded)
				  minStrengthNeeded -= (strengthNeeded*0.15F);
			}
			boolean considerSubStrength = true;
			boolean destroyerAttacked = destroyerStrength > 0.0F;
			if (!subNeedsDestroyer && !shipsAttacked) //only planes...enemy sub strength doesn't matter
			{
				strengthNeeded -= subStrength;
				minStrengthNeeded -= subStrength*attackFactor;
				maxStrengthNeeded -= subStrength*2.4F;
				considerSubStrength = false;
			}
			if (minStrengthNeeded > 0.0F)
			{
				starterStrength = maxStrengthNeeded;
				maxStrengthNeeded -= ourStrength;
				float newPlaneStrength = SUtils.invitePlaneAttack(false, false, enemy, minStrengthNeeded, xAlreadyMoved, xMoves, xRoutes, data, player);
				ourStrength += newPlaneStrength;
				planeStrength += newPlaneStrength;
				maxStrengthNeeded -= newPlaneStrength;
				shipStrength = SUtils.inviteShipAttack(enemy, maxStrengthNeeded, xAlreadyMoved, xMoves, xRoutes, data, player, true, tFirst, tFirst);
				if (shipStrength > 0.0F)
					shipsAttacked = true;
				ourStrength += shipStrength;
				minStrengthNeeded -= shipStrength;
			}
			if (!considerSubStrength && shipsAttacked)
				strengthNeeded += subStrength;
			boolean weCanAttack = (subNeedsDestroyer && enemySubsOnly) ? (destroyerAttacked) : true;
			boolean alliedSuperiority = alliedStrength > strengthNeeded;
			if (weCanAttack && ((ourStrength > strengthNeeded) || (alliedSuperiority && ourStrength > 0.86F*strengthNeeded)))
            {
				seaTerrAttacked.add(enemy);
				moveRoutes.addAll(xRoutes);
				for (Collection<Unit> xUnits : xMoves)
					moveUnits.add(xUnits);
				unitsAlreadyMoved.addAll(xAlreadyMoved);
				maxStrengthNeeded -= ourStrength;
				if (maxStrengthNeeded > 0.0F)
					checkForMorePlanes.put(enemy, maxStrengthNeeded);
            }
            shipsAttacked = false;
        }
        setSeaTerrAttacked(seaTerrAttacked);
//        SUtils.verifyMoves(moveUnits, moveRoutes, data, player);
    }

    private Route getAlternativeAmphibRoute(final PlayerID player)
    {
        if(!isAmphibAttack(player, false))
            return null;

        final GameData data = getPlayerBridge().getGameData();
        Match<Territory> routeCondition = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasNoEnemyUnits(player, data));

        // should select all territories with loaded transports
        Match<Territory> transportOnSea = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasLandUnitsOwnedBy(player));
        Route altRoute = null;
        int length=Integer.MAX_VALUE;
        for(Territory t : data.getMap())
        {
        	if(! transportOnSea.match(t)) continue;
            CompositeMatchAnd<Unit> ownedTransports = new CompositeMatchAnd<Unit>(Matches.UnitCanTransport, Matches.unitIsOwnedBy(player),HasntMoved);
            CompositeMatchAnd<Territory> enemyTerritory = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemy(player, data), Matches.TerritoryIsLand,new InverseMatch<Territory>(Matches.TerritoryIsNeutral),Matches.TerritoryIsEmpty);
        	int trans = t.getUnits().countMatches(ownedTransports);
        	if(trans>0) {
        		Route newRoute = SUtils.findNearest(t, enemyTerritory, routeCondition, data);
        		if(newRoute != null && length > newRoute.getLength()) {
        			altRoute = newRoute;
        		}
        	}
        }
        return altRoute;
	}

    /**
     * check for planes that need to land
     */
	private void CheckPlanes(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
	//check for planes that need to move
	//don't let planes stay in territory alone if it can be attacked
	//we've already check Carriers in moveNonComPlanes
	{
        final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
		Match<Territory> canLand = new CompositeMatchAnd<Territory>(
                Matches.isTerritoryAllied(player, data),
                new Match<Territory>()
                {
                    @Override
                    public boolean match(Territory o)
                    {
                        return !delegate.getBattleTracker().wasConquered(o);
                    }
                });
		Match<Unit> bomberUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsStrategicBomber);
        Match<Territory> routeCondition = new CompositeMatchAnd<Territory>(
                Matches.territoryHasEnemyAA(player, data).invert(), Matches.TerritoryIsPassableAndNotRestricted( player));
        Match<Unit> fighterUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanLandOnCarrier);

        Territory myCapital = TerritoryAttachment.getCapital(player, data);
		List <Territory> planeTerr = new ArrayList<Territory>();
		planeTerr = SUtils.TerritoryOnlyPlanes(data, player);
		planeTerr.remove(myCapital);
		if (planeTerr.size() == 0) //skip...no loner planes
			return;

		for (Territory t : planeTerr)
		{
			List<Unit> airUnits = t.getUnits().getMatches(fighterUnit);
			List<Unit> bombUnits = t.getUnits().getMatches(bomberUnit);
			List<Unit> sendFighters = new ArrayList<Unit>();
			List<Unit> sendBombers = new ArrayList<Unit>();

			Route route2 = SUtils.findNearestNotEmpty(t, canLand, routeCondition, data);
			Territory endTerr = route2.getTerritories().get(route2.getLength());
			int sendNum = 0;
			for (Unit f : airUnits)
			{
				if (MoveValidator.hasEnoughMovement(f, route2))
				{
					sendFighters.add(f);
					sendNum++;
				}
			}

			if (sendNum>0)
			{
				moveUnits.add(sendFighters);
				moveRoutes.add(route2);
			}
			sendNum = 0;
			for (Unit b : bombUnits)
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

	private void stopBlitzAttack(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
	{
		Collection<Territory> impassableTerrs = getImpassableTerrs();
		CompositeMatch<Unit> myUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsInfantry, HasntMoved);
		CompositeMatch<Unit> alliedUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsLand);
		CompositeMatch<Unit> blitzBlocker = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsNotFactory, Matches.UnitIsNotAA);
		CompositeMatch<Unit> anyUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsNotFactory, Matches.UnitIsNotAA);
        boolean capDanger = getCapDanger(); //do not mark units at capital for non-movement
        boolean tFirst = transportsMayDieFirst();
        Territory myCapital = TerritoryAttachment.getCapital(player, data);
        List<Route> blitzTerrRoutes = new ArrayList<Route>();
		float enemyStrength = SUtils.getStrengthOfPotentialAttackers(myCapital, data, player, tFirst, false, null);
		float ourStrength = SUtils.strength(myCapital.getUnits().getUnits(), false, false, tFirst);
		List <Territory> TerrToBlock = new ArrayList<Territory>();
		List <Territory> possBlitzTerr = SUtils.possibleBlitzTerritories(myCapital, data, player);
		List <Territory> cantBlockList = new ArrayList<Territory>();
		for (Territory pB : possBlitzTerr)
		{
			if (Matches.isTerritoryEnemy(player, data).match(pB))
			{
				cantBlockList.addAll(data.getMap().getNeighbors(pB, Matches.territoryHasEnemyBlitzUnits(player, data)));
			}
		}
		cantBlockList.removeAll(possBlitzTerr); //could be overlaps here
		float blitzStrength = SUtils.determineEnemyBlitzStrength(myCapital, blitzTerrRoutes, null, data, player);
		boolean noChangeOnPass = blitzStrength > 0.0F;
		while (noChangeOnPass)
		{
			boolean listChanged = false; 
			for (Route bRoute : blitzTerrRoutes)
			{
				if (bRoute != null && !cantBlockList.contains(bRoute.getStart()))
				{
					Territory midTerr = bRoute.getTerritories().get(1);
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
		if (enemyStrength - blitzStrength < ourStrength) //removing blitzers eliminates the threat to cap
			capDanger = false; //do everything to clear them out
        List<Territory> capNeighbors = SUtils.getNeighboringLandTerritories(data, player, myCapital);
        List<Territory> capDoNotUse = new ArrayList<Territory>();
        Iterator<Territory> capIter = capNeighbors.iterator();
        while (capIter.hasNext())
        {
        	Territory thisCapTerr = capIter.next();
        	if (thisCapTerr.getUnits().countMatches(blitzBlocker) <= 1)
        	{
        		capIter.remove();
        		capDoNotUse.add(thisCapTerr);
        	}
        }
        List<Unit> alreadyMoved = new ArrayList<Unit>();
        List<Territory> goBlockTerr = new ArrayList<Territory>();
    	HashMap<Territory, Float> blockTerrMap = new HashMap<Territory, Float>();
        for (Territory blockTerr : TerrToBlock)
        {
        	List<Territory> myNeighbors = SUtils.getNeighboringLandTerritories(data, player, blockTerr);
        	myNeighbors.removeAll(capDoNotUse);
        	myNeighbors.removeAll(goBlockTerr);
        	for (Territory myTerr : myNeighbors)
        	{
        		float attackStrength = SUtils.getStrengthOfPotentialAttackers(myTerr, data, player, tFirst, true, null);
        		blockTerrMap.put(myTerr, attackStrength);
        	}
        	goBlockTerr.addAll(myNeighbors);
        }
        SUtils.reorder(goBlockTerr, blockTerrMap, false);
        if (capDanger)
        	goBlockTerr.remove(myCapital);
        for (Territory moveFrom : goBlockTerr)
        {
        	List<Unit> ourUnits = moveFrom.getUnits().getMatches(anyUnit);
        	List<Unit> alliedUnits = moveFrom.getUnits().getMatches(Matches.alliedUnit(player, data));
        	float eStrength = blockTerrMap.get(moveFrom);
        	float aStrength = SUtils.strength(alliedUnits, false, false, true);
			Set<Territory> moveFromNeighbors = data.getMap().getNeighbors(moveFrom, Matches.territoryHasNoEnemyUnits(player, data));
			moveFromNeighbors.removeAll(impassableTerrs);
			moveFromNeighbors.retainAll(TerrToBlock);
			if (moveFromNeighbors.isEmpty())
				continue;
			Iterator<Territory> neighborIter = moveFromNeighbors.iterator();
        	if (aStrength > eStrength && ourUnits.size() > 1)
        	{
        		Iterator<Unit> ourIter = ourUnits.iterator();
        		while (ourIter.hasNext() && ourUnits.size() > 1 && aStrength > eStrength && neighborIter.hasNext())
        		{
        			Unit unit = ourIter.next();
        			Territory moveHere = neighborIter.next();
        			Route moveRoute = data.getMap().getLandRoute(moveFrom, moveHere);
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
        if (TerrToBlock.size() > 0) //still more to move
        {
        	Iterator<Territory> bIter = TerrToBlock.iterator();
        	while (bIter.hasNext())
        	{
        		float strengthNeeded = 1.0F;
        		Territory moveHere = bIter.next();
        		float bStrength = SUtils.inviteBlitzAttack(true, moveHere, strengthNeeded, alreadyMoved, moveUnits, moveRoutes, data, player, false, capDanger);
        		if (bStrength > 0.0F)
        			bIter.remove();
        	}
        }
        if (TerrToBlock.size() > 0) //still more
        {
        	for (Territory xTerr : goBlockTerr)
        	{
        		Set<Territory> goBTerrs = data.getMap().getNeighbors(xTerr, Matches.territoryHasNoEnemyUnits(player, data));
        		goBTerrs.removeAll(impassableTerrs);
        		goBTerrs.retainAll(TerrToBlock);
        		if (goBTerrs.isEmpty())
        			continue;
        		List<Unit> ourUnits = xTerr.getUnits().getMatches(anyUnit);
        		List<Unit> alliedUnits = xTerr.getUnits().getMatches(alliedUnit);
        		boolean canGo = (alliedUnits.size() > ourUnits.size()) || (alliedUnits.size() > 1);
        		ourUnits.removeAll(alreadyMoved);
        		if (!canGo || ourUnits.size() == 0)
        			continue;
        		if (canGo)
        		{
        			Iterator<Territory> goBIter = goBTerrs.iterator();
        			Iterator<Unit> unitIter = ourUnits.iterator();
        			boolean movedIn = false;
        			while(unitIter.hasNext() && !movedIn && goBIter.hasNext() && alliedUnits.size() > 1)
        			{
        				Territory goTerr = goBIter.next();
        				Route unitRoute = data.getMap().getLandRoute(xTerr, goTerr);
        				if (unitRoute == null)
        					continue;
        				Unit nextUnit = unitIter.next();
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
        	for (Territory checkAgain : TerrToBlock)
        	{
        		float strengthNeeded = 1.0F;
        		SUtils.invitePlaneAttack(true, false, checkAgain, strengthNeeded, alreadyMoved, moveUnits, moveRoutes, data, player);
        	}
        }
	}
	
	private void SetCapGarrison(Territory myCapital, PlayerID player, float totalInvasion, Collection<Unit> alreadyMoved)
	{
		// Make sure we keep enough units in the capital for defense.
		CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsNotSea, Matches.UnitIsNotAA, Matches.UnitIsNotFactory, HasntMoved.invert());
		List<Unit> myCapUnits = myCapital.getUnits().getMatches(landUnit);

		float capGarrisonStrength = 0.0F;
		for (Unit x : myCapUnits)
		{
			if ((capGarrisonStrength * 0.9F - 3F) <= totalInvasion)
			{
				capGarrisonStrength += SUtils.uStrength(x, false, false, false);
				alreadyMoved.add(x);
			}
		}
		if (capGarrisonStrength < totalInvasion)
		{
			CompositeMatch<Unit> landUnit2 = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsNotSea, Matches.UnitIsNotAA, Matches.UnitIsNotFactory, HasntMoved);
			List<Unit> myCapUnits2 = myCapital.getUnits().getMatches(landUnit2);

			for (Unit x : myCapUnits2)
			{
				if ((capGarrisonStrength * 0.9F - 3F) <= totalInvasion)
				{
					capGarrisonStrength += SUtils.uStrength(x, false, false, false);
					alreadyMoved.add(x);
				}
			}
		}
	}

    private void populateNonCombat(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {
		float ourStrength = 0.0F, attackerStrength=0.0F;
		float totalInvasion = 0.0F, ourCapStrength=0.0F;
		boolean capDanger = false;
		boolean tFirst = transportsMayDieFirst();
        Collection<Territory> territories = data.getMap().getTerritories();
        List<Territory> emptiedTerr = new ArrayList<Territory>();
        List<Territory> fortifiedTerr = new ArrayList<Territory>();
        List<Territory> alliedTerr = SUtils.allAlliedTerritories(data, player);

        Territory myCapital = TerritoryAttachment.getCapital(player, data);
		List<Territory> movedInto = new ArrayList<Territory>();
        List<Unit> alreadyMoved = new ArrayList<Unit>();

        CompositeMatchAnd<Territory> moveThrough = new CompositeMatchAnd<Territory>(Matches.TerritoryIsPassableAndNotRestricted( player),
            Matches.TerritoryIsNotNeutral, Matches.TerritoryIsLand);

		CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsNotSea, Matches.UnitIsNotAA, Matches.UnitIsNotFactory, HasntMoved);
		CompositeMatch<Unit> infantryUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsInfantry);
		CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir);
		CompositeMatch<Unit> alliedUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsLand);
		CompositeMatch<Unit> myTransportUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsTransport);

		List<Territory> alreadyAttacked = Collections.emptyList();
		//Look at Capital before anything else
        float dangerFactor = 1.05F;
		ourCapStrength = SUtils.strength(myCapital.getUnits().getUnits(), false, false, tFirst);
		totalInvasion = SUtils.getStrengthOfPotentialAttackers(myCapital, data, player, tFirst, true, null);
		StrengthEvaluator capStrEval = StrengthEvaluator.evalStrengthAt(data, player, myCapital, false, true, tFirst, true);
		boolean directCapDanger = totalInvasion > (ourCapStrength*0.95F - 3.00F);
		capDanger = capStrEval.inDanger(dangerFactor); 
		List<Territory> badNeighbors = SUtils.getNeighboringEnemyLandTerritories(data, player, myCapital);
		List<Territory> myNeighbors = SUtils.getNeighboringLandTerritories(data, player, myCapital);
		for (Territory neighborTerr : myNeighbors)
		{
			List<Territory> nextNeighbors = SUtils.getNeighboringEnemyLandTerritories(data, player, neighborTerr);
			nextNeighbors.removeAll(badNeighbors);
			if (nextNeighbors.size() > 0)
			{
				List<Unit> myUnits = neighborTerr.getUnits().getMatches(landUnit);
				if (myUnits.size() > 0) //make sure we have units
				{
					int leastMove = MoveValidator.getLeastMovement(myUnits); //Are there units that cannot be moved
					if (leastMove > 0)
					{
						List<Unit> infUnits = neighborTerr.getUnits().getMatches(infantryUnit);
						if (infUnits.size() > 0)
						{
							Unit oneUnit = infUnits.get(0);
							alreadyMoved.add(oneUnit);
						}
						else
						{
							Unit oneUnit = myUnits.get(0);
							alreadyMoved.add(oneUnit);
						}
					}
				}
			}
		}
		HashMap<Territory, Float> SNeighbor = new HashMap<Territory, Float>();
		for (Territory xNeighbor : myNeighbors)
		{
			SNeighbor.put(xNeighbor, SUtils.getStrengthOfPotentialAttackers(xNeighbor, data, player, tFirst, true, null));
		}
		SUtils.reorder(myNeighbors, SNeighbor, false);
		HashMap<Territory, Float> addStrength = new HashMap<Territory, Float>();
		for (Territory qT : alliedTerr)
		{
			addStrength.put(qT, 0.0F);
		}
		if (directCapDanger) //borrow some units
		{
			/*
			 * First pass, only remove units necessary, but maintain a strong defense
			 * Second pass, ignore defense of neighbors and protect capitol
			 */
			for (Territory tx3 : myNeighbors)
			{
				if ((ourCapStrength*dangerFactor-3.00F) > totalInvasion)
					continue;
				float stayAboveStrength = SNeighbor.get(tx3).floatValue()*0.75F;
				List<Unit> allUnits = tx3.getUnits().getMatches(landUnit);
				float currStrength = SUtils.strength(allUnits, false, false, tFirst);
				if (currStrength < stayAboveStrength)
					continue;
				List<Unit> sendIn = new ArrayList<Unit>();
				Iterator<Unit> uIter = allUnits.iterator();
				while (uIter.hasNext() && (((ourCapStrength*dangerFactor-3.00F) <= totalInvasion) && currStrength > stayAboveStrength))
				{
					Unit x = uIter.next();
					float uStrength = SUtils.uStrength(x, false, false, tFirst);
					ourCapStrength += uStrength;
					currStrength -= uStrength;
					sendIn.add(x);
				}
				Route quickRoute = data.getMap().getLandRoute(tx3, myCapital);
				moveUnits.add(sendIn);
				moveRoutes.add(quickRoute);
				alreadyMoved.addAll(sendIn);
			}
			float remainingStrengthNeeded = (totalInvasion - (ourCapStrength*dangerFactor - 3.00F))*1.05F;
			float blitzStrength = 0.0F, planeStrength = 0.0F, transStrength = 0.0F, landStrength = 0.0F;
			if (remainingStrengthNeeded > 0.0F)
				blitzStrength = SUtils.inviteBlitzAttack(true, myCapital, remainingStrengthNeeded, alreadyMoved, moveUnits, moveRoutes, data, player, false, true);
			remainingStrengthNeeded -= blitzStrength;
			if (remainingStrengthNeeded > 0.0F)
				planeStrength = SUtils.invitePlaneAttack(true, false, myCapital, remainingStrengthNeeded, alreadyMoved, moveUnits, moveRoutes, data, player);
			remainingStrengthNeeded -= planeStrength;
			// Go Back to the neighbors and pull in what is needed
			if (remainingStrengthNeeded > 0.0F)
				landStrength -=SUtils.inviteLandAttack(true, myCapital, remainingStrengthNeeded, alreadyMoved, moveUnits, moveRoutes, data, player, false, true, alreadyAttacked);
			remainingStrengthNeeded -= landStrength;
//			if (remainingStrengthNeeded > 0.0F)
//				transStrength = SUtils.inviteTransports(true, myCapital, remainingStrengthNeeded, alreadyMoved, moveUnits, moveRoutes, data, player, tFirst, false, null);
			List<Unit> myCapUnits = myCapital.getUnits().getMatches(landUnit);
			alreadyMoved.addAll(myCapUnits);
			ourCapStrength += blitzStrength + planeStrength + landStrength + transStrength;
			capDanger = totalInvasion > ourCapStrength;
		}
		if (capDanger) //see if we have units 3/2 away from capitol that we can bring back
		{
			List<Territory> outerTerrs = SUtils.getExactNeighbors(myCapital, 3, player, false);
			Iterator<Territory> outerIter = outerTerrs.iterator();
			HashMap<Territory, Float> outerMap = new HashMap<Territory, Float>();
			HashMap<Territory, Float> outerEMap = new HashMap<Territory, Float>();
			while (outerIter.hasNext())
			{
				Territory outerTerr = outerIter.next();
				if (outerTerr.isWater() || Matches.isTerritoryAllied(player, data).match(outerTerr) || data.getMap().getLandRoute(outerTerr, myCapital)==null)
					outerIter.remove();
				float myStrength = SUtils.strength(outerTerr.getUnits().getMatches(landUnit), false, false, tFirst);
				float outerEStrength = SUtils.getStrengthOfPotentialAttackers(myCapital, data, player, tFirst, true, null);
				outerMap.put(outerTerr, myStrength);
				outerEMap.put(outerTerr, outerEStrength);
			}
			SUtils.reorder(outerTerrs, outerEMap, false); //try based on enemy strength...lowest first
			float strengthNeeded = capStrEval.strengthMissing(dangerFactor); 
			for (Territory outerTerr : outerTerrs) //need combination of closest to capital and least likely to get mauled
			{
				List<Territory> oTNeighbors = SUtils.getNeighboringLandTerritories(data, player, outerTerr);
				IntegerMap<Territory> distMap = new IntegerMap<Territory>();
				HashMap<Territory, Float> oTNMap = new HashMap<Territory, Float>();
				int checkDist = data.getMap().getLandDistance(outerTerr, myCapital);
				Iterator<Territory> oTNIter = oTNeighbors.iterator();
				while (oTNIter.hasNext())
				{
					Territory oTN = oTNIter.next();
					int oTNDist = data.getMap().getLandDistance(oTN, myCapital);
					float oTNEStrength = SUtils.getStrengthOfPotentialAttackers(oTN, data, player, tFirst, true, null);
					if (checkDist > oTNDist)
						oTNMap.put(oTN, oTNEStrength);
					else
						oTNIter.remove();
				}
				SUtils.reorder(oTNeighbors, oTNMap, false);
				float outerEStrength = SUtils.getStrengthOfPotentialAttackers(outerTerr, data, player, tFirst, true, null);
				List<Unit> ourOuterUnits = outerTerr.getUnits().getMatches(landUnit);
				ourOuterUnits.removeAll(alreadyMoved);
				List<Unit> ourPlanes = outerTerr.getUnits().getMatches(airUnit);
				ourPlanes.removeAll(alreadyMoved);
				float thisTerrStrength = SUtils.strength(outerTerr.getUnits().getUnits(), false, false, tFirst);
				float diffStrength = outerEStrength - thisTerrStrength;
				boolean EAdvantage = diffStrength > 1.5F*thisTerrStrength;
				for (Territory oTN : oTNeighbors)
				{
					//check and make sure we are not killing this territory
					Route oTNRoute = data.getMap().getLandRoute(outerTerr, oTN);
					if (oTNRoute == null)
						continue;
					if (EAdvantage && ourOuterUnits.size() > 1 && strengthNeeded > 0.0F) //move all but 1
					{
						moveUnits.add(ourOuterUnits);
						moveRoutes.add(oTNRoute);
						alreadyMoved.addAll(ourOuterUnits);
						float strengthAdded =SUtils.strength(ourOuterUnits, false, false, tFirst); 
						strengthNeeded -= strengthAdded;
						diffStrength += strengthAdded;
					}
					else if (ourOuterUnits.size() > 1)
					{
						//move some units in to reduce strengthNeeded
						Iterator<Unit> oIter = ourOuterUnits.iterator();
						List<Unit> addUnits = new ArrayList<Unit>();
						
						while (diffStrength < 0.0F && strengthNeeded > 0.0F && oIter.hasNext())
						{
							Unit oUnit = oIter.next();
							float oStrength = SUtils.uStrength(oUnit, false, false, tFirst);
							addUnits.add(oUnit);
							strengthNeeded -= oStrength;
							diffStrength += oStrength;
						}
						if (addUnits.size() > 1)
						{
							moveUnits.add(addUnits);
							moveRoutes.add(oTNRoute);
							alreadyMoved.addAll(addUnits);
						}
					}
				}
			}
			
		}
		else // Make sure we keep enough units in the capital for defense.
			SetCapGarrison(myCapital, player, totalInvasion, alreadyMoved);

		//eliminate blitz territories first
        List<Territory> possBlitzTerr = SUtils.possibleBlitzTerritories(myCapital, data, player);
        for (Territory pB : possBlitzTerr)
        {
         	if (Matches.isTerritoryEnemy(player, data).match(pB))
        		continue;
    		List<Unit> ourUnits = pB.getUnits().getMatches(landUnit);
    		if (ourUnits.isEmpty())
    		{
        		float strengthNeeded = 1.0F;
        		SUtils.inviteLandAttack(true, pB, strengthNeeded, alreadyMoved, moveUnits, moveRoutes, data, player, false, false, alreadyAttacked);
        		continue;
        	}
        	if (MoveValidator.getLeastMovement(ourUnits) > 0)
        	{
        		Unit dontMoveUnit = ourUnits.get(0);
        		alreadyMoved.add(dontMoveUnit);
        	}
       }

        List<Territory> beenThere = new ArrayList<Territory>();
        for(Territory t : alliedTerr)
        {
            if(Matches.TerritoryIsWater.match(t) || !Matches.territoryHasLandUnitsOwnedBy(player).match(t))
                continue;
            //check for blitzable units
            CompositeMatch<Unit> blitzUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBlitz);
    		CompositeMatch<Territory> enemyPassableNotWater = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemy(player, data), Matches.TerritoryIsPassableAndNotRestricted( player), Matches.TerritoryIsLand);
            CompositeMatch<Territory> enemyPassableNotWaterNotNeutral = new CompositeMatchAnd<Territory>(enemyPassableNotWater, Matches.TerritoryIsNotNeutral);
            CompositeMatch<Territory> routeCondition = new CompositeMatchAnd<Territory>(Matches.TerritoryIsPassableAndNotRestricted( player), Matches.isTerritoryAllied(player, data));
            List<Unit> blitzUnits = t.getUnits().getMatches(blitzUnit);
            blitzUnits.removeAll(alreadyMoved);
        	Route goRoute = SUtils.findNearest(t, enemyPassableNotWater, routeCondition, data);
        	if (goRoute != null)
        	{
        		Territory endTerr = goRoute.getEnd();
        		if (Matches.TerritoryIsNeutral.match(endTerr))
        		{
                	float pValue = TerritoryAttachment.get(endTerr).getProduction();
                	float enemyStrength = SUtils.strength(endTerr.getUnits().getUnits(), false, false, tFirst);
                  	Route xRoute = SUtils.findNearest(t, enemyPassableNotWaterNotNeutral, routeCondition, data);
                  	if (enemyStrength > pValue*9) //why bother...focus on enemies
                    {
                    	goRoute = xRoute;
                    }
                    else
                    { //make sure going in this direction is preferred
                    	int neutralDist = goRoute.getLength()+1;
                    	if (xRoute != null && xRoute.getEnd() != null)
                    	{
                    		Territory realEnemy = xRoute.getEnd();
                    		float eValue = TerritoryAttachment.get(realEnemy).getProduction();
                    		int enemyDist = xRoute.getLength();
                    		Set<Territory> neutralNeighbors = data.getMap().getNeighbors(endTerr, enemyPassableNotWater);
                    		for (Territory nTerr : neutralNeighbors)
                    		{
                    			int xValue = TerritoryAttachment.get(nTerr).getProduction();
                    			if (Matches.TerritoryIsNeutral.match(nTerr))
                    			{
                    				float testStrength = SUtils.strength(endTerr.getUnits().getUnits(), false, false, tFirst);
                    				if (testStrength > xValue)
                    					xValue = 0; //not a neutral we will invade
                    			}
                    			pValue += xValue;
                    		}
                       		Set<Territory> enemyNeighbors = data.getMap().getNeighbors(realEnemy, enemyPassableNotWater);
                       		
                    		for (Territory nTerr : enemyNeighbors)
                    		{
                    			TerritoryAttachment ta = TerritoryAttachment.get(nTerr);
                    			if(ta != null)
                    				eValue += ta.getProduction();
                    		}
                    		if (pValue < eValue)
                    			goRoute = xRoute;
                    	}
                    	Territory lastTerr = goRoute.getEnd();
                    	if (Matches.isTerritoryEnemy(player, data).match(lastTerr))
                    	{
                    		lastTerr = goRoute.getTerritories().get(goRoute.getLength()-1);
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
            //these are the units we can move
            CompositeMatch<Unit> moveOfType = new CompositeMatchAnd<Unit>();

            moveOfType.add(Matches.unitIsOwnedBy(player));
            moveOfType.add(Matches.UnitIsNotAA);

            moveOfType.add(Matches.UnitIsNotFactory);
            moveOfType.add(Matches.UnitIsLand);

            List<Unit> units = t.getUnits().getMatches(moveOfType);
            units.removeAll(alreadyMoved);
            if(units.size() == 0)
                continue;

            int minDistance = Integer.MAX_VALUE;
            Territory to = null;
            Collection<Unit> unitsHere = t.getUnits().getMatches(moveOfType);
//          realStrength = SUtils.strength(unitsHere, false, false, tFirst) - SUtils.allairstrength(unitsHere, false);

            ourStrength = SUtils.strength(t.getUnits().getUnits(), false, false, tFirst) + addStrength.get(t);
            attackerStrength = SUtils.getStrengthOfPotentialAttackers( t, data, player, tFirst, false, null);

            if ((t.getUnits().someMatch(Matches.UnitIsFactory) || t.getUnits().someMatch(Matches.UnitIsAA)) && t != myCapital)
            { //protect factories...rather than just not moving, plan to move some in if necessary
              //Don't worry about units that have been moved toward capital
                if (attackerStrength > (ourStrength + 5.0F))
				{
					List<Territory> myNeighbors2 = SUtils.getNeighboringLandTerritories(data, player, t);
					if (capDanger)
						myNeighbors2.remove(myCapital);
					for (Territory t3 : myNeighbors2)
					{ //get everything
						List<Unit> allUnits = t3.getUnits().getMatches(moveOfType);
						List<Unit> sendIn2 = new ArrayList<Unit>();
						for (Unit x2 : allUnits)
						{
							if ((ourStrength - 5.0F) < attackerStrength && !alreadyMoved.contains(x2))
							{
								ourStrength += SUtils.uStrength(x2, false, false, tFirst);
								sendIn2.add(x2);
							}
						}

						Route quickRoute = data.getMap().getLandRoute(t3, t);
						addStrength.put(t, addStrength.get(t) + SUtils.strength(sendIn2, false, false, tFirst));
						moveUnits.add(sendIn2);
						moveRoutes.add(quickRoute);
						alreadyMoved.addAll(sendIn2);
						movedInto.add(t);
					}
				}
                float tmpStrength = 0.0F;
                List<Unit> tUnits = t.getUnits().getMatches(Matches.unitIsOwnedBy(player));
                List<Collection<Unit>> tGoUnits = new ArrayList<Collection<Unit>>();
                SUtils.breakUnitsBySpeed(tGoUnits, data, player, tUnits);
                for (Collection<Unit> tUnits2 : tGoUnits)
                {
                	Iterator<Unit> tUnitIter = tUnits2.iterator();
                	while (tmpStrength < attackerStrength && tUnitIter.hasNext())
                	{
                		Unit xUnit = tUnitIter.next();
                		alreadyMoved.add(xUnit); //lock them down
                		tmpStrength += SUtils.uStrength(xUnit, false, false, tFirst);
                	}
                }
			}
			//if an emminent attack on capital, pull back toward capital
            List <Territory> myENeighbors = SUtils.getNeighboringEnemyLandTerritories(data, player, t);
            
            Route retreatRoute = null;
            if (myENeighbors.size() == 0 && ((ourCapStrength*1.08F + 5.0F) < totalInvasion))
            { //this territory has no enemy neighbors...pull back toward capital
            	if (t != myCapital && data.getMap().getLandRoute(t, myCapital) != null)
            	{ 
            		List<Territory> myNeighbors3 = SUtils.getNeighboringLandTerritories(data, player, t);
            		myNeighbors3.remove(myCapital);
            		int minCapDist = 100;
            		Territory targetTerr = null;
            		for (Territory myTerr : myNeighbors3)
            		{
						int thisCapDist = data.getMap().getLandDistance(myTerr, myCapital);
						if (thisCapDist < minCapDist)
						{
							minCapDist = thisCapDist;
							targetTerr = myTerr;
							retreatRoute = data.getMap().getLandRoute(t, myTerr);
						}
					}
					if (retreatRoute != null)
					{
						List<Unit> myMoveUnits = t.getUnits().getMatches(landUnit);
						myMoveUnits.removeAll(alreadyMoved);
						int totUnits2 = myMoveUnits.size();
						List<Unit> myRealMoveUnits = new ArrayList<Unit>();
						for (int i3=0; i3 < totUnits2; i3++)
						{
							if ((ourCapStrength*1.08F + 5.0F) < totalInvasion)
							{
								Unit moveThisUnit = myMoveUnits.get(i3);
								ourCapStrength += 0.5F*SUtils.uStrength(moveThisUnit, true, false, tFirst);
								myRealMoveUnits.add(moveThisUnit);
							}
						}
						if (myRealMoveUnits.size() > 0)
						{
							addStrength.put(targetTerr, addStrength.get(targetTerr) + SUtils.strength(myRealMoveUnits, false, false, tFirst));
							moveRoutes.add(retreatRoute);
							moveUnits.add(myRealMoveUnits);
							alreadyMoved.addAll(myRealMoveUnits);
							movedInto.add(targetTerr);
						}
					}
				}
			}
			if (fortifiedTerr.contains(t)) //don't move...we've joined units
			    continue;
//			float newInvasion = SUtils.getStrengthOfPotentialAttackers(t, data, player, tFirst, true, null);
			if (attackerStrength > (ourStrength*1.85F + 6.0F) && !movedInto.contains(t)) //overwhelming attack...look to retreat
			{
				List<Territory> myFriendTerr = SUtils.getNeighboringLandTerritories(data, player, t);
				int maxUnits = 0, thisUnits = 0;
				Territory mergeTerr = null;
				for (Territory fTerr : myFriendTerr)
				{
					if (emptiedTerr.contains(fTerr))
					    continue; //don't move somewhere we have already retreated from
					List<Territory> badGuysTerr = SUtils.getNeighboringEnemyLandTerritories(data, player, fTerr);
					if (badGuysTerr.size() > 0) //give preference to the front
					    thisUnits += 4;
					float fTerrAttackers = SUtils.getStrengthOfPotentialAttackers(fTerr, data, player, tFirst, true, null);
		    		StrengthEvaluator strEval = StrengthEvaluator.evalStrengthAt(data, player, fTerr, false, true, tFirst, true);
					if (fTerrAttackers > 8.0F && fTerrAttackers < (strEval.getAlliedStrengthInRange() + ourStrength)*1.05F)
					    thisUnits += 4;
					else if (fTerrAttackers > 0.0F && fTerrAttackers < (strEval.getAlliedStrengthInRange() + ourStrength)*1.05F)
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
					Route myRetreatRoute = data.getMap().getLandRoute(t, mergeTerr);
					List <Unit> myRetreatUnits = t.getUnits().getMatches(landUnit);
					int totUnits3 = myRetreatUnits.size();
					for (int i4=totUnits3-1; i4>=0; i4--)
					{
						Unit u4 = myRetreatUnits.get(i4);
						if (alreadyMoved.contains(u4))
							myRetreatUnits.remove(u4);
					}
					addStrength.put(mergeTerr, addStrength.get(mergeTerr) + SUtils.strength(myRetreatUnits, false, false, tFirst));
					moveUnits.add(myRetreatUnits);
					moveRoutes.add(myRetreatRoute);
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
            if (goRoute == null) //just advance to nearest enemy owned factory
            {
            	goRoute = SUtils.findNearest(t, Matches.territoryHasEnemyFactory(data, player), routeCondition, data);
            	if (goRoute != null)
            	{
            		Territory endGoTerr = goRoute.getTerritories().get(goRoute.getLength()-1);
            		goRoute = data.getMap().getRoute(t, endGoTerr, routeCondition);
            	}
            }

            boolean isAmphib = isAmphibAttack(player, false);
            if(goRoute == null && isAmphib) //move toward the largest contingent of transports
            {
            	if (Matches.territoryHasAlliedFactory(data, player).match(t))
            		continue;
            	else
            	{
            		List<Territory> transportTerrs = SUtils.findOnlyMyShips(t, data, player, Matches.UnitIsTransport);
            		if (transportTerrs.size() > 0)
            		{
            			IntegerMap<Territory> transMap = new IntegerMap<Territory>();
            			for (Territory xTransTerr : transportTerrs)
            				transMap.put(xTransTerr, xTransTerr.getUnits().countMatches(myTransportUnit));
            			SUtils.reorder(transportTerrs, transMap, true);
            		}
            		for (Territory tTerr : transportTerrs)
            		{
            			List<Territory> myLandTerrs = SUtils.getNeighboringLandTerritories(data, player, tTerr);
            			boolean goodRoute = false;
            			Iterator<Territory> mLIter = myLandTerrs.iterator();
            			while (mLIter.hasNext() && !goodRoute)
            			{
            				Territory mLT = mLIter.next();
            				goRoute = data.getMap().getRoute(t, mLT, Matches.TerritoryIsNotImpassableToLandUnits(player));
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
            int newDistance = goRoute.getLength();
            List<Collection<Unit>> unitsBySpeed = new ArrayList<Collection<Unit>>();
            SUtils.breakUnitsBySpeed(unitsBySpeed, data, player, units);
            if (to != null && minDistance <= (newDistance + 1))
            {
                if(units.size() > 0)
                {
                    Route rC = data.getMap().getRoute(t, to, moveThrough);
                    if (rC != null)
                    	goRoute = rC;
                }
            }
            for (Collection<Unit> goUnits : unitsBySpeed)
            {
            	int maxDist = MoveValidator.getMaxMovement(goUnits);
            	if (maxDist == 0)
            	{
            		alreadyMoved.addAll(goUnits);
            		continue;
            	}
            	Route newRoute2 = new Route(); //don't modify newRoute
            	if (goRoute.getLength() < maxDist)
            		newRoute2 = goRoute;
            	else
            	{
            		Iterator<Territory> newIter = goRoute.iterator();
            		newRoute2.setStart(t);
            		newIter.next();
            		while(newIter.hasNext() && newRoute2.getLength() <= maxDist)
            		{
            			Territory oneTerr = newIter.next();
            			if (Matches.isTerritoryAllied(player, data).match(oneTerr))
            				newRoute2.add(oneTerr);
            		}
            	}
            	moveUnits.add(goUnits);
            	moveRoutes.add(newRoute2);
            	alreadyMoved.addAll(goUnits);
            	Territory endPoint = newRoute2.getEnd();
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
*/        }
    }
    /*
     * A second pass at the set of nonCombat Land Units
     * Ignores all neutrals (for now)
     * 
     */
    private void secondNonCombat(List<Collection<Unit>> moveUnits, List<Route> moveRoutes, final PlayerID player, GameData data)
    {
    	List<Unit> alreadyMoved = new ArrayList<Unit>();
		boolean tFirst = transportsMayDieFirst();
    	CompositeMatch<Unit> unMovedLand = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.UnitIsNotAA, Matches.UnitIsNotFactory);
    	CompositeMatch<Unit> ourUnMovedLand = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), unMovedLand, HasntMoved);
    	
    	List<Territory> unMovedLandTerr = SUtils.findCertainShips(data, player, ourUnMovedLand);
    	HashMap<Territory, Float> landMap = new HashMap<Territory, Float>();
    	List<Territory> ourOwnedTerr = SUtils.allAlliedTerritories(data, player);
    	for (Territory ourTerr : ourOwnedTerr)
    	{
    		float eStrength = SUtils.getStrengthOfPotentialAttackers(ourTerr, data, player, tFirst, true, null);
    		float myStrength = SUtils.strength(ourTerr.getUnits().getUnits(), false, false, tFirst);
    		float diffStrength = eStrength - myStrength;
    		landMap.put(ourTerr, diffStrength);
    	}
    	List<Territory> myFactories = SUtils.findCertainShips(data, player, Matches.UnitIsFactory);
    	for (Territory factTerr : myFactories)
    	{
    		float diffStrength = landMap.get(factTerr).floatValue();
    		if (diffStrength > 0.0F) //we need other units
    		{
    			List<Territory> landNeighbors = SUtils.getNeighboringLandTerritories(data, player, factTerr);
    			List<Territory> grabFromTerr = new ArrayList<Territory>();
    			for (Territory lN : landNeighbors)
    			{
    				if (unMovedLandTerr.contains(lN))
    					grabFromTerr.add(lN);
    			}
    			SUtils.reorder(grabFromTerr, landMap, false);
    			Iterator<Territory> grabIter = grabFromTerr.iterator();
    			while (grabIter.hasNext() && diffStrength > 0.0F)
    			{
    				Territory availTerr = grabIter.next();
    				float availStrength = - landMap.get(availTerr).floatValue();
    				List<Unit> availUnits = SUtils.sortTransportUnits(availTerr.getUnits().getMatches(ourUnMovedLand));
    				availUnits.removeAll(alreadyMoved);
    				Iterator<Unit> availIter = availUnits.iterator();
    				List<Unit> moveThese = new ArrayList<Unit>();
    				while (availIter.hasNext() && diffStrength > 0.0F && availStrength > 0.0F)
    				{
    					Unit moveOne = availIter.next();
    					float thisUnitStrength = SUtils.uStrength(moveOne, false, false, tFirst); 
    					diffStrength -= thisUnitStrength;
    					availStrength -= thisUnitStrength;
    					moveThese.add(moveOne);
    				}
    				landMap.put(availTerr, -availStrength);
    				Route aRoute = data.getMap().getLandRoute(availTerr, factTerr);
    				moveUnits.add(moveThese);
    				moveRoutes.add(aRoute);
    				alreadyMoved.addAll(moveThese);
    			}
        		landMap.put(factTerr, diffStrength);
    		}
    	}
    	CompositeMatch<Territory> endCondition = new CompositeMatchAnd<Territory>(Matches.territoryHasEnemyUnits(player, data), Matches.TerritoryIsNotNeutral, Matches.TerritoryIsLand);
    	for (Territory ownedTerr : unMovedLandTerr)
    	{//TODO: find another territory to join if possible
    		//TODO: for some reason, unMovedLandTerr is containing conflicted territories where combat didn't 
    		// complete- causing the need for the ownedTerr check below
    		if (ownedTerr.isWater() || !ownedTerr.getOwner().equals(player))
    			continue;
    		float diffStrength = - landMap.get(ownedTerr).floatValue();
    		if (diffStrength > 0.0F && ownedTerr.getUnits().getMatches(unMovedLand).size()> 1 && data.getMap().getNeighbors(ownedTerr, endCondition).isEmpty()) 
    		{
    			Route closestERoute = SUtils.findNearest(ownedTerr, endCondition, Matches.TerritoryIsNotImpassableToLandUnits(player), data);
    			if (closestERoute == null || closestERoute.getEnd() == null)
    				continue;
    			List<Unit> ourOwnedUnits = ownedTerr.getUnits().getMatches(ourUnMovedLand);
    			ourOwnedUnits.removeAll(alreadyMoved);
    			if (ourOwnedUnits.isEmpty())
    				continue;
    			List<Collection<Unit>> ourOwnedUnits2 = new ArrayList<Collection<Unit>>();
    			SUtils.breakUnitsBySpeed(ourOwnedUnits2, data, player, ourOwnedUnits);
    			Territory targetTerr = closestERoute.getEnd();
    			
//    			List<Unit> moveTheseUnits = SUtils.sortTransportUnits(ourOwnedUnits);
//    			Territory goTerr = closestERoute.getTerritories().get(1);
    			float goTerrStrength = SUtils.strength(targetTerr.getUnits().getMatches(Matches.alliedUnit(player, data)), false, false, tFirst);
    			float goTerrEStrength = SUtils.getStrengthOfPotentialAttackers(targetTerr, data, player, tFirst, true, null);
    			float goDiffStrength = goTerrEStrength - goTerrStrength;
    			if (goDiffStrength - diffStrength > 8.0F)
    				continue;
    			for (Collection<Unit> goUnits : ourOwnedUnits2)
    			{
    				Iterator<Unit> unitIter = goUnits.iterator();
    				int moveDist = MoveValidator.getLeastMovement(goUnits);
    				List<Unit> moveThese = new ArrayList<Unit>();
					Territory realTargetTerr = closestERoute.getTerritories().get(closestERoute.getLength() - 1);
					Route targetRoute = new Route();
    				if (moveDist < closestERoute.getLength())
    					realTargetTerr = closestERoute.getTerritories().get(moveDist);
					targetRoute = data.getMap().getRoute(ownedTerr, realTargetTerr, Matches.isTerritoryAllied(player, data));
					while (unitIter.hasNext() && diffStrength > 0.0F)
					{
						Unit oneUnit = unitIter.next();
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

    @SuppressWarnings("unchecked")
    private void movePlanesHomeNonCom(List<Collection<Unit>> moveUnits, List<Route> moveRoutes, final PlayerID player, GameData data)
    {
		// planes are doing silly things like landing in territories that can be invaded with cheap units
		// we want planes to find an aircraft carrier
        IMoveDelegate delegateRemote = (IMoveDelegate) getPlayerBridge().getRemote();
        CompositeMatch<Unit> alliedFactory = new CompositeMatchAnd<Unit> (Matches.alliedUnit(player, data), Matches.UnitIsFactory);
        CompositeMatch<Unit> fighterUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanLandOnCarrier);
        CompositeMatch<Unit> bomberUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanLandOnCarrier.invert(), Matches.UnitIsAir);
        CompositeMatch<Unit> alliedFighterUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitCanLandOnCarrier);
        CompositeMatch<Unit> carrierUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsCarrier);
        CompositeMatch<Unit> alliedACUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsCarrier);
        CompositeMatch<Unit> alliedBomberUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitCanLandOnCarrier.invert(), Matches.UnitIsAir);
        CompositeMatch<Territory> noEnemyNeighbor = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), Matches.territoryHasEnemyLandNeighbor(data, player).invert());
        CompositeMatch<Territory> noNeutralOrAA = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotNeutral, Matches.territoryHasEnemyAA(player, data).invert(), Matches.TerritoryIsNotImpassable);
        List<Unit> alreadyMoved = new ArrayList<Unit>();
        List<Territory> alreadyCheck = new ArrayList<Territory>();

        final BattleDelegate delegate = DelegateFinder.battleDelegate(data);

        Match<Territory> canLand = new CompositeMatchAnd<Territory>(

                Matches.isTerritoryAllied(player, data),
                new Match<Territory>()
                {
                    @Override
                    public boolean match(Territory o)
                    {
                        return !delegate.getBattleTracker().wasConquered(o);
                    }
                }
                );

        Match<Territory> routeCondition = new CompositeMatchAnd<Territory>(Matches.territoryHasEnemyAA(player, data).invert(),
                Matches.TerritoryIsPassableAndNotRestricted(player));

        Territory myCapital = TerritoryAttachment.getCapital(player, data);
        List<Territory> alliedFactories = SUtils.findUnitTerr(data, player, alliedFactory);

        for (Territory tBomb : delegateRemote.getTerritoriesWhereAirCantLand()) //move bombers to capital first
        {
			List<Unit> bomberUnits = tBomb.getUnits().getMatches(bomberUnit);
			bomberUnits.removeAll(alreadyMoved);
	        List<Unit> sendBombers = new ArrayList<Unit>();
			alreadyCheck.add(tBomb);
			for (Unit bU : bomberUnits)
			{
				if (bU == null)
					continue;
				boolean landable = SUtils.airUnitIsLandable(bU, tBomb, myCapital, player, data);
				if (landable)
					sendBombers.add(bU);
			}
			if (sendBombers.size() > 0 && tBomb != myCapital)
			{
				Route bomberRoute = data.getMap().getRoute(tBomb, myCapital, noNeutralOrAA);
				if (MoveValidator.canLand(sendBombers, bomberRoute.getEnd(), player, data))
				{
					moveRoutes.add(bomberRoute);
					moveUnits.add(sendBombers);
					alreadyMoved.addAll(sendBombers);
				}
			}
			bomberUnits.removeAll(sendBombers); //see if there are any left
			Iterator<Unit> bUIter = bomberUnits.iterator();
			while (bUIter.hasNext())
			{
				boolean landedOne = false;
				Unit bU = bUIter.next();
				if (bU == null)
					continue;
				for (Territory aFactory : alliedFactories)
				{
					Route bomberFactoryRoute = data.getMap().getRoute(tBomb, aFactory, noNeutralOrAA);
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
				Route goodBomberRoute = SUtils.findNearest(tBomb, noEnemyNeighbor, noNeutralOrAA, data);
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
				Route bomberRoute2 = SUtils.findNearestNotEmpty(tBomb, canLand, routeCondition, data);
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
				Route bomberRoute3 = SUtils.findNearest(tBomb, canLand, noNeutralOrAA, data);
				if (bomberRoute3 != null && MoveValidator.hasEnoughMovement(bU, bomberRoute3))
				{
					List<Unit> qAdd = new ArrayList<Unit>();
					qAdd.add(bU);
				    moveRoutes.add(bomberRoute3);
				    moveUnits.add(qAdd);
				    alreadyMoved.add(bU);
				    bUIter.remove();
				}
			}
		}
        CompositeMatch<Territory> avoidTerr = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotNeutral, Matches.TerritoryIsNotImpassable, Matches.territoryHasEnemyAA(player, data).invert());
        List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
        List<Territory> ourEnemyTerr = new ArrayList<Territory>();
        HashMap<Territory, Float> rankMap = new HashMap<Territory, Float>();
        rankMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, false, false, true);
        Collection<Territory> badPlaneTerrs =delegateRemote.getTerritoriesWhereAirCantLand();
//        s_logger.fine("Player: "+player.getName()+"Planes Need to Move From: "+badPlaneTerrs);
		for (Territory tFight : badPlaneTerrs)
		{
			List<Unit> fighterUnits = new ArrayList<Unit>(tFight.getUnits().getMatches(fighterUnit));
			fighterUnits.removeAll(alreadyMoved);
//			s_logger.fine("Territory: "+tFight+"; Planes: "+fighterUnits);
			if (fighterUnits.isEmpty())
				continue;
			List<Unit> ACUnits = new ArrayList<Unit>(tFight.getUnits().getMatches(carrierUnit));
			int carrierSpace = 0;
			for (Unit carrier1 : ACUnits)
				carrierSpace += UnitAttachment.get(carrier1.getType()).getCarrierCapacity();
			List<Unit> alliedACUnits = new ArrayList<Unit>(tFight.getUnits().getMatches(alliedACUnit));
			int alliedCarrierSpace = 0;
			for (Unit carrier1 : alliedACUnits)
				alliedCarrierSpace += UnitAttachment.get(carrier1.getType()).getCarrierCapacity();
			List<Unit> alliedFighters = new ArrayList<Unit>(tFight.getUnits().getMatches(alliedFighterUnit));
			alliedFighters.removeAll(fighterUnits);
			int maxUnits = carrierSpace;
			int alliedACMax = alliedCarrierSpace;
			int totFighters = fighterUnits.size(), alliedTotFighters = alliedFighters.size();
			int fighterSpace = 0, alliedFighterSpace = 0;
			for (Unit fighter1 : fighterUnits)
				totFighters += UnitAttachment.get(fighter1.getType()).getCarrierCost();
			for (Unit fighter1 : alliedFighters)
				alliedFighterSpace += UnitAttachment.get(fighter1.getType()).getCarrierCost();
			List<Collection<Unit>> fighterList = new ArrayList<Collection<Unit>>();
			SUtils.breakUnitsBySpeed(fighterList, data, player, fighterUnits);
			int needToLand = fighterUnits.size();
			if (carrierSpace > 0)
			{
				Iterator<Collection<Unit>> fIter = fighterList.iterator();
				while (fIter.hasNext() && carrierSpace > 0)
				{
					Collection<Unit> newFighters = fIter.next();
					Iterator<Unit> nFIter = newFighters.iterator();
					while (nFIter.hasNext() && carrierSpace > 0)
					{
						Unit markFighter = nFIter.next();
						carrierSpace -= UnitAttachment.get(markFighter.getType()).getCarrierCost();
						nFIter.remove(); //plane is marked to stay on the carrier
						needToLand--;
					}
				}
			}
			
			Iterator<Collection<Unit>> fIter = fighterList.iterator();
			while (needToLand > 0 && fIter.hasNext())
			{//each group will have an identical movement
				Collection<Unit> fighterGroup = fIter.next();
				if (fighterGroup.isEmpty())
					continue;
				int flightDistance = MoveValidator.getMaxMovement(fighterGroup);
				Set<Territory> allTerr = data.getMap().getNeighbors(tFight, flightDistance);
				List<Territory> landingZones = CompositeMatch.getMatches(allTerr, canLand);
				SUtils.reorder(landingZones, rankMap, false);
				Iterator<Territory> lzIter = landingZones.iterator();
				while(needToLand > 0 && lzIter.hasNext() && !fighterGroup.isEmpty())
				{
					Territory landingZone = lzIter.next();
					if (Matches.TerritoryIsPassableAndNotRestricted(player).match(landingZone) && 
							MoveValidator.canLand(fighterGroup, landingZone, player, data))
					{
						Route landingRoute = data.getMap().getRoute(tFight, landingZone, noNeutralOrAA);
						if (landingRoute != null && MoveValidator.hasEnoughMovement(fighterGroup, landingRoute))
						{
							Iterator<Unit> fIter2 = fighterGroup.iterator();
							List<Unit> landThese = new ArrayList<Unit>();
							boolean landSome = false;
							while (needToLand > 0 &&  fIter2.hasNext())
							{
								Unit fighter = fIter2.next();
								landThese.add(fighter);
								fIter2.remove();
								needToLand --;
								landSome = true;
								s_logger.fine("Added: "+fighter+"; Left To Land: "+needToLand);
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
    
    private void bomberNonComMove(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {
        final BattleDelegate delegate = DelegateFinder.battleDelegate(data);
    	CompositeMatch<Unit> myBomberUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsStrategicBomber);
    	CompositeMatch<Unit> alliedFactory = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsFactory);
    	CompositeMatch<Unit> enemyFactory = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsFactory);
    	CompositeMatch<Territory> waterOrLand = new CompositeMatchOr<Territory>(Matches.TerritoryIsWater, Matches.TerritoryIsLand);
    	List<Territory> alliedFactories = SUtils.findUnitTerr(data, player, alliedFactory);
    	List<Territory> enemyFactories = SUtils.findUnitTerr(data, player, enemyFactory);
    	List<Territory> bomberTerrs = SUtils.findCertainShips(data, player, Matches.UnitIsStrategicBomber);
    	if (bomberTerrs.isEmpty())
    		return;
    	Iterator<Territory> bTerrIter = bomberTerrs.iterator();
    	while (bTerrIter.hasNext())
    	{
    		Territory bTerr = bTerrIter.next();
    		Route bRoute = SUtils.findNearest(bTerr, Matches.territoryHasEnemyUnits(player, data), waterOrLand, data);
    		if (bRoute == null || bRoute.getLength() < 4 && Matches.TerritoryIsLand.match(bTerr) && !delegate.getBattleTracker().wasBattleFought(bTerr))
    		{
    			bTerrIter.remove();
    		}

    	}
    	if (bomberTerrs.isEmpty())
    		return;
    	List<Unit> unitsAlreadyMoved = new ArrayList<Unit>();
    	IntegerMap<Territory> shipMap = new IntegerMap<Territory>();
    	HashMap<Territory, Float> strengthMap = new HashMap<Territory, Float>();
    	for (Territory aF : alliedFactories)
    	{
    		if (delegate.getBattleTracker().wasConquered(aF))
    			continue; //don't allow planes to move toward a just conquered factory
    		Route distToEnemy = SUtils.findNearest(aF, Matches.territoryHasEnemyUnits(player, data), waterOrLand, data);
    		if (distToEnemy == null || distToEnemy.getEnd() == null)
    			continue;
    		int eDist = distToEnemy.getLength();
    		//int shipThreat = SUtils.shipThreatToTerr(aF, data, player, true);
    		shipMap.put(aF, eDist);
    		float eStrength = SUtils.getStrengthOfPotentialAttackers(aF, data, player, true, false, null);
    		strengthMap.put(aF, eStrength);
    	}
    	List<Territory> checkTerrs = new ArrayList<Territory>(shipMap.keySet());
    	if (!checkTerrs.isEmpty())
    	{
    		SUtils.reorder(checkTerrs, shipMap, false);
    		for (Territory checkTerr : checkTerrs)
    		{
    			for (Territory bTerr : bomberTerrs)
    			{
    				Route bRoute = data.getMap().getRoute(bTerr, checkTerr, waterOrLand);
    				if (bRoute == null || bRoute.getEnd() == null)
    					continue;
    				List<Unit> bUnits = bTerr.getUnits().getMatches(myBomberUnit);
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
    					Iterator<Unit> bIter = bUnits.iterator();
    					while (bIter.hasNext())
    					{
    						Unit bomber = bIter.next();
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
    		for (Territory checkTerr : checkTerrs)
    		{
    			for (Territory bTerr : bomberTerrs)
    			{
    				Route bRoute = data.getMap().getRoute(bTerr, checkTerr, SUtils.TerritoryIsNotImpassableToAirUnits(data));
    				if (bRoute == null || bRoute.getEnd() == null)
    					continue;
    				List<Unit> bUnits = bTerr.getUnits().getMatches(myBomberUnit);
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
    					Iterator<Unit> bIter = bUnits.iterator();
    					while (bIter.hasNext())
    					{
    						Unit bomber = bIter.next();
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
//TODO: Rework combat move into 3 separate phases. This will give us a better look at the real potential attackers after a set of moves.
    private void populateCombatMove(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {
		/* Priorities:
		** 1) We lost our capital
		** 2) Units placed next to our capital
		** 3) Enemy players capital
		** 4) Enemy players factories
		*/
    	Collection<Territory> impassableTerrs = getImpassableTerrs();
    	HashMap<PlayerID, IntegerMap<UnitType>> costMap = SUtils.getPlayerCostMap(data);
    	
    	boolean aggressive = SUtils.determineAggressiveAttack(data, player, 1.4F);
    	float maxAttackFactor = 2.00F;
    	float attackFactor = 1.76F;
        float attackFactor2 = 1.11F; //emergency attack...weaken enemy
        final Collection<Unit> unitsAlreadyMoved = new HashSet<Unit>();
		List<Territory> enemyOwned = SUtils.getNeighboringEnemyLandTerritories(data, player, true);
		enemyOwned.removeAll(impassableTerrs);
		
        // Include neutral territories that are worth attacking.  
		//enemyOwned.addAll(SUtils.getNeighboringNeutralLandTerritories(data, player, true));
		
		boolean tFirst = transportsMayDieFirst();
		List<Territory> alreadyAttacked = getLandTerrAttacked();
        Territory myCapital = TerritoryAttachment.getCapital(player, data);
        float eCapStrength = SUtils.getStrengthOfPotentialAttackers(myCapital, data, player, tFirst, false, null);
        float ourStrength = SUtils.strength(myCapital.getUnits().getUnits(), false, false, tFirst);
        boolean capDanger = eCapStrength > ourStrength;
        List<Territory> capitalNeighbors = SUtils.getNeighboringLandTerritories(data, player, myCapital);
        boolean ownMyCapital = myCapital.getOwner() == player;
        List<Territory> emptyBadTerr= new ArrayList<Territory>();
		float remainingStrengthNeeded = 0.0F;
		List<Territory>enemyCaps = SUtils.getEnemyCapitals(data, player);

        CompositeMatch<Unit> attackable = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsLand,
            Matches.UnitIsNotAA, Matches.UnitIsNotFactory,
                new Match<Unit>()
                {
                    public boolean match(Unit o)
                    {
                        return !unitsAlreadyMoved.contains(o);
                    }
                 });

        CompositeMatch<Unit> airUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir);
        CompositeMatch<Unit> alliedAirUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsAir);
        CompositeMatch<Unit> alliedLandUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data),
        											Matches.UnitIsLand, Matches.UnitIsNotAA, Matches.UnitIsNotFactory);
        CompositeMatch<Unit> alliedAirLandUnit = new CompositeMatchOr<Unit>(alliedAirUnit, alliedLandUnit);
        CompositeMatch<Unit> blitzUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBlitz);
        CompositeMatch<Unit> enemyLandAirUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsNotSea, Matches.UnitIsNotAA, Matches.UnitIsNotFactory);
        CompositeMatch<Territory> emptyEnemyTerr = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemyAndNotNeutral(player, data), Matches.TerritoryIsNotImpassableToLandUnits(player));

        if (!ownMyCapital) //We lost our capital
        {
			attackFactor = 0.78F; //attack like a maniac, maybe we will win
			enemyOwned.remove(myCapital); //handle the capital directly
		}

        List<Territory> bigProblem2 = SUtils.getNeighboringEnemyLandTerritories(data, player, myCapital); //attack these guys first
        HashMap<Territory, Float> sortTerritories = new HashMap<Territory, Float>();
        HashMap<Territory, Float> sortProblems = new HashMap<Territory, Float>();
		int numTerr = 0, numTerrProblem = 0, realProblems = 0;
		float xStrength = 0.0F;
		Territory xTerr = null;
		HashMap<Territory, Float> enemyMap = new HashMap<Territory, Float>();
		Territory maxAttackTerr = SUtils.landAttackMap(data, player, enemyMap);
		if(maxAttackTerr == null) {
			return;
		}
		SUtils.reorder(enemyOwned, enemyMap, true);
		numTerr = enemyMap.size();
		float aggregateStrength = 0.0F;
		Iterator<Territory> bPIter = bigProblem2.iterator();
		while (bPIter.hasNext())
		{
			Territory bPTerr = bPIter.next();
			if (Matches.TerritoryIsNeutral.match(bPTerr)) //don't worry about neutrals in the Big Problems
				bPIter.remove();
		}
 		for (Territory tProb: bigProblem2) //rank the problems
		{
            if(!tProb.getUnits().someMatch(Matches.enemyUnit(player, data) ) )
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
		List<Territory> seaTerrAttacked = getSeaTerrAttacked();

        List<Collection<Unit>> xMoveUnits = new ArrayList<Collection<Unit>>();
        List<Route> xMoveRoutes = new ArrayList<Route>();
        List<Unit> xAlreadyMoved = new ArrayList<Unit>();

		if (!ownMyCapital) //attack the capital with everything we have
		{
            Collection<Territory> attackFrom = SUtils.getNeighboringLandTerritories(data, player, myCapital);
            float badCapStrength = SUtils.strength(myCapital.getUnits().getUnits(), false, false, tFirst);
            float capStrength = 0.0F;
            for (Territory checkCap : attackFrom)
            {
                List<Unit> units = checkCap.getUnits().getMatches(attackable);
                capStrength += SUtils.strength(units, true, false, tFirst);
			}
            float xRS = 1000.0F;
            xRS -= SUtils.inviteBlitzAttack(false, myCapital, xRS, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, true, true);
            boolean groundUnits = ((1000.0F - xRS) > 1.0F);
            xRS -= SUtils.invitePlaneAttack(false, false, myCapital, xRS, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player);
            xRS -= SUtils.inviteTransports(false, myCapital, xRS, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, tFirst, false, seaTerrAttacked);
            capStrength += 1000.0F - xRS;
			if (capStrength > badCapStrength*0.78F) //give us a chance...
			{
				for(Territory owned : attackFrom )
            	{
            	    List<Unit> units = owned.getUnits().getMatches(attackable);
            	    if(units.size() > 0)
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
    		List<Unit> myCapUnits = myCapital.getUnits().getMatches(Matches.unitIsOwnedBy(player));
    		List<Territory> eCapTerrs = SUtils.getNeighboringEnemyLandTerritories(data, player, myCapital);
    		Iterator<Territory> eCIter = eCapTerrs.iterator();
    		while (eCIter.hasNext())
    		{
    			Territory noNeutralTerr = eCIter.next();
    			if (Matches.TerritoryIsNeutral.match(noNeutralTerr))
    				eCIter.remove();
    		}
    		float totECapStrength = SUtils.getStrengthOfPotentialAttackers(myCapital, data, player, tFirst, true, alreadyAttacked);
    		HashMap<Territory, Float> eCapMap = new HashMap<Territory, Float>();
    		float maxStrength = 0.0F;
    		Territory maxSTerr = null;
    		for (Territory eCapTerr : eCapTerrs)
    		{
    			List<Unit> eCapUnits = eCapTerr.getUnits().getMatches(Matches.enemyUnit(player, data));
    			float eStrength = SUtils.strength(eCapUnits, false, false, tFirst);
    			eCapMap.put(eCapTerr, eStrength);
    			if (eStrength > maxStrength)
    			{
    				maxStrength = eStrength;
    				maxSTerr = eCapTerr;
    			}
    		}
    		SUtils.reorder(eCapTerrs, eCapMap, true);
    		List<Collection<Unit>> tempMoves = new ArrayList<Collection<Unit>>();
    		List<Route> tempRoutes = new ArrayList<Route>();
    		List<Unit> tempAMoved = new ArrayList<Unit>();
    		float totStrengthEliminated = 0.0F;
    		List<Territory> capThreatElim = new ArrayList<Territory>(alreadyAttacked);
    		xAlreadyMoved.addAll(unitsAlreadyMoved);
    		float strengthForAttack = 0.0F;
    		float capAttackFactor = 1.45F;
    		if (eCapTerrs.size() > 1)
    			capAttackFactor = 1.25F;
    		for (Territory killTerr : eCapTerrs)
    		{
    			float realEStrength = eCapMap.get(killTerr);
    			float sNeeded = realEStrength*capAttackFactor + 5.0F;
    			float blitzS = SUtils.inviteBlitzAttack(false, killTerr, sNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, true, true);
    			sNeeded -= blitzS;
    			float planeS = SUtils.invitePlaneAttack(false, false, killTerr, sNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player);
    			sNeeded -= planeS;
    			float landS = SUtils.inviteLandAttack(false, killTerr, sNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, true, true, alreadyAttacked);
    			sNeeded -= landS;
    			float transS = SUtils.inviteTransports(false, killTerr, sNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, tFirst, false, seaTerrAttacked);
    			sNeeded -= transS;
    			strengthForAttack = blitzS + planeS + landS + transS;
    			if (strengthForAttack > (realEStrength*0.92F + 2.0F)) //we can retreat into the capital
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
    		float xCapThreat = SUtils.getStrengthOfPotentialAttackers(myCapital, data, player, tFirst, true, capThreatElim);
    		Collection<Unit> alliedCapUnits = myCapital.getUnits().getUnits();
    		Collection<Unit> purchaseUnits = player.getUnits().getUnits();
    		float newStrength = SUtils.strength(purchaseUnits, false, false, tFirst);
    		for (Collection<Unit> xUnits : tempMoves)
    		{
    			alliedCapUnits.removeAll(xUnits);
    		}
    		float strengthLeft = SUtils.strength(alliedCapUnits, false, false, tFirst);
    		newStrength += strengthLeft;
    		boolean hasBombers = false;
    		if (newStrength > xCapThreat*0.92F)
    		{
    			alreadyAttacked.addAll(capThreatElim);
    			for (Collection<Unit> tM : tempMoves)
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
    				Collection<Unit> allFactoryUnits = myCapital.getUnits().getUnits();
    				float myTotalStrength = SUtils.strength(allFactoryUnits, false, false, tFirst);
    				Collection<Unit> newUnits = player.getUnits().getUnits();
    				myTotalStrength += SUtils.strength(newUnits, false, false, tFirst)*0.75F; //play it safe
    				if (myTotalStrength < eCapStrength*1.25F)
    				{
    					float addStrength = eCapStrength*1.25F - myTotalStrength;
    					float landStrength = SUtils.inviteLandAttack(false, myCapital, addStrength + 2.0F, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, false, true, alreadyAttacked);
    				}
    			}
    		}
    	}
		else // Make sure we keep enough units in the capital for defense.
			// TODO: Consider what is being taken out by attack and reduce strength
			SetCapGarrison(myCapital, player, eCapStrength, unitsAlreadyMoved);
    		
    	xMoveUnits.clear();
    	xMoveRoutes.clear();
    	xAlreadyMoved.clear();
    	List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
    	List<Territory> ourEnemyTerr = new ArrayList<Territory>();
    	HashMap<Territory, Float> rankMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, tFirst, false, false);

    	SUtils.reorder(enemyCaps, rankMap, true);
		for (Territory badCapitol : enemyCaps)
		{
			xAlreadyMoved.addAll(unitsAlreadyMoved);
			Collection <Unit> badCapUnits = badCapitol.getUnits().getUnits();
			float badCapStrength = SUtils.strength(badCapUnits, false, false, tFirst);
			float alliedCapStrength = 0.0F;
			float ourXStrength = 0.0F;
			List <Territory> alliedCapTerr = SUtils.getNeighboringLandTerritories(data, player, badCapitol);
//			if (alliedCapTerr == null || alliedCapTerr.isEmpty())
//				continue;
			List<Unit> alliedCUnits = new ArrayList<Unit>();
			List<Unit> ourCUnits = new ArrayList<Unit>();
			if (!alliedCapTerr.isEmpty())
			{
				for (Territory aT : alliedCapTerr)
				{ //alliedCUnits contains ourCUnits
					alliedCUnits.addAll(aT.getUnits().getMatches(alliedAirLandUnit));
					ourCUnits.addAll(aT.getUnits().getMatches(attackable));
				}
				ourCUnits.removeAll(unitsAlreadyMoved);
				alliedCapStrength += SUtils.strength(alliedCUnits, true, false, tFirst);
				ourXStrength += SUtils.strength(ourCUnits, true, false, tFirst);
			}
			remainingStrengthNeeded = badCapStrength*2.5F + 8.0F; //bring everything to get the capital
			float origSNeeded = remainingStrengthNeeded;
			float blitzStrength = SUtils.inviteBlitzAttack(false, badCapitol, remainingStrengthNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, true, true);
			remainingStrengthNeeded -= blitzStrength;
			float transStrength = SUtils.inviteTransports(false, badCapitol, remainingStrengthNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, tFirst, false, seaTerrAttacked);
			remainingStrengthNeeded -= transStrength;
			float airStrength = SUtils.invitePlaneAttack(false, false, badCapitol, remainingStrengthNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player);
			remainingStrengthNeeded -= airStrength;
			float addLandStrength = blitzStrength + transStrength;
			if (ourXStrength < 1.0F && addLandStrength < 1.0F)
				continue;
			float additionalStrength = addLandStrength + airStrength;
//			alliedCapStrength += additionalStrength;
			ourXStrength += additionalStrength;
			Collection<Unit> invasionUnits = ourCUnits;
			for (Collection<Unit> invaders : xMoveUnits)
				invasionUnits.addAll(invaders);
			boolean weWin = SUtils.calculateTUVDifference(badCapitol, invasionUnits, badCapUnits, costMap, player, data, aggressive, Properties.getAirAttackSubRestricted(data));
			if (weWin || (alliedCapStrength > (badCapStrength*1.10F + 3.0F) && (ourXStrength > (0.82F*badCapStrength + 3.0F))))
			{
//				s_logger.fine("Player: "+player.getName() + "; Bad Cap: "+badCapitol.getName() + "; Our Attack Units: "+ ourCUnits);
//				s_logger.fine("Allied Cap Strength: "+alliedCapStrength+"; Bad Cap Strength: "+badCapStrength+"; Our Strength: "+ourXStrength);
				enemyOwned.remove(badCapitol); //no need to attack later
				for (Territory aT2 : alliedCapTerr)
				{
					List <Unit> ourCUnits2 = aT2.getUnits().getMatches(attackable);
					ourCUnits2.removeAll(unitsAlreadyMoved);
					moveUnits.add(ourCUnits2);
					Route aR = data.getMap().getLandRoute(aT2, badCapitol);
					moveRoutes.add(aR);
					unitsAlreadyMoved.addAll(ourCUnits2);
				}
				moveUnits.addAll(xMoveUnits);
				moveRoutes.addAll(xMoveRoutes);
				unitsAlreadyMoved.addAll(xAlreadyMoved);
				alreadyAttacked.add(badCapitol);
			}
			xMoveUnits.clear();
			xMoveRoutes.clear();
			xAlreadyMoved.clear();
		}

        //find the territories we can just walk into
		enemyOwned.removeAll(alreadyAttacked);
		enemyOwned.retainAll(rankMap.keySet());
		SUtils.reorder(enemyOwned, rankMap, true);
        for(Territory enemy : enemyOwned)
        {
        	xAlreadyMoved.addAll(unitsAlreadyMoved);
			float eStrength = SUtils.strength(enemy.getUnits().getUnits(), true, false, tFirst);
            if( eStrength < 0.50F)
            {
                //only take it with 1 unit
                boolean taken = false;
                Set<Territory> nextTerrs = data.getMap().getNeighbors(enemy, emptyEnemyTerr);
                Iterator<Territory> nTIter = nextTerrs.iterator();
                while (nTIter.hasNext())
                {
                	Territory nTcheck = nTIter.next();
                	if (Matches.TerritoryIsImpassableToLandUnits(player).match(nTcheck))
                		nTIter.remove();
                }
                HashMap<Territory, Float> canBeTaken = new HashMap<Territory, Float>();
                for (Territory nextOne : nextTerrs)
                {
                	List<Territory> myGoodNeighbors = SUtils.getNeighboringLandTerritories(data, player, nextOne);
                	if (myGoodNeighbors.size()>0) //we own the neighbors...let them handle bringing blitz units in
                		continue;
                	List<Unit> totUnits = nextOne.getUnits().getMatches(enemyLandAirUnit);
                	float thisStrength = SUtils.strength(totUnits, false, false, tFirst);
                	canBeTaken.put(nextOne, thisStrength);
                }
                Set<Territory> blitzTerrs = canBeTaken.keySet();
                for(Territory attackFrom : data.getMap().getNeighbors(enemy, Matches.territoryHasLandUnitsOwnedBy(player)))
                {
                    if(taken)
                        break;
					//just get an infantry at the top of the queue
                    List<Unit> aBlitzUnits = attackFrom.getUnits().getMatches(blitzUnit);
                    aBlitzUnits.removeAll(unitsAlreadyMoved);
                    Territory findOne = null;
                    if (canBeTaken.size() > 0) //we have another terr we can take
                    {
                    	for (Territory attackTo : blitzTerrs)
                    	{
                    		if (canBeTaken.get(attackTo) < 1.0F)
                    			findOne = attackTo;
                    	}
                    }
                    if (findOne != null && !aBlitzUnits.isEmpty()) //use a tank
                    {
                    	for (Territory bT : blitzTerrs)
                    	{
                    		if (canBeTaken.get(bT) < 4.0F)
                    		{
                            	Route newRoute = new Route();
                    			newRoute.setStart(attackFrom);
                    			newRoute.add(enemy);
                    			newRoute.add(bT);
                    			Unit deleteThisOne = null;
                    			for (Unit tank : aBlitzUnits)
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
                    else //use an infantry
                    {
                    	List<Unit> unitsSorted = SUtils.sortTransportUnits(attackFrom.getUnits().getMatches(attackable));
                    	unitsSorted.removeAll(unitsAlreadyMoved);
                    	for(Unit unit : unitsSorted)
                    	{
                    		moveRoutes.add(data.getMap().getRoute(attackFrom, enemy));

                    		if(attackFrom.isWater())
                    		{
                    			List<Unit> units2 = attackFrom.getUnits().getMatches(Matches.unitIsLandAndOwnedBy(player));
                    			moveUnits.add( Util.difference(units2, unitsAlreadyMoved) );
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
 		boolean weAttacked = false, weAttacked2 = false;
 		int EinfArtCount = 0, OinfArtCount = 0;
 		bigProblem2.removeAll(alreadyAttacked);
 		SUtils.reorder(bigProblem2, rankMap, true);
//TODO: Rewrite this section. It could be much cleaner.
 		xAlreadyMoved.clear();
 		xMoveUnits.clear();
 		xMoveRoutes.clear();
 		for (Territory badTerr : bigProblem2)
		{
			weAttacked = false;
			Collection<Unit> enemyUnits = badTerr.getUnits().getUnits();
			badStrength = SUtils.strength(enemyUnits, false, false, tFirst);

            if(badStrength > 0.0F)
            {
                ourStrength = 0.0F;
                List<Territory> capitalAttackTerr = new ArrayList<Territory>(data.getMap().getNeighbors(badTerr, Matches.territoryHasLandUnitsOwnedBy(player)));
                List<Unit> capSaverUnits = new ArrayList<Unit>();
                for(Territory capSavers : capitalAttackTerr )
                {
                	capSaverUnits.addAll(capSavers.getUnits().getMatches(attackable));
                }
            	capSaverUnits.removeAll(unitsAlreadyMoved);
                ourStrength += SUtils.strength(capSaverUnits, true, false, tFirst);
                remainingStrengthNeeded = badStrength*attackFactor + 4.0F - ourStrength;
                xAlreadyMoved.addAll(capSaverUnits);
                xAlreadyMoved.addAll(unitsAlreadyMoved);
				float blitzStrength = SUtils.inviteBlitzAttack(false, badTerr, remainingStrengthNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, true, false);
				remainingStrengthNeeded -= blitzStrength;
				
				float seaStrength = SUtils.inviteTransports(false, badTerr, remainingStrengthNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, tFirst, false, seaTerrAttacked);
				remainingStrengthNeeded -= seaStrength;
				weAttacked = (ourStrength + blitzStrength) > 0.0F; //land Units confirmed
				float planeStrength = SUtils.invitePlaneAttack(false, false, badTerr, remainingStrengthNeeded, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player);
				ourStrength += blitzStrength + seaStrength;
				if (ourStrength < 1.0F)
					continue;
				else
					ourStrength += planeStrength;
				List<Unit> allMyUnits = new ArrayList<Unit>(capSaverUnits);
				for (Collection<Unit> xUnits : xMoveUnits)
					allMyUnits.addAll(xUnits);
				boolean weWin = SUtils.calculateTUVDifference(badTerr, allMyUnits, enemyUnits, costMap, player, data, aggressive, Properties.getAirAttackSubRestricted(data));
                if (weWin) 
                {
                    if (bigProblem2.size() > 1)
                    	maxAttackFactor = 1.57F;//concerned about overextending if more than 1 territory
                    remainingStrengthNeeded = (maxAttackFactor * badStrength) + 3.0F;
                    float landStrength = SUtils.inviteLandAttack(false, badTerr, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, 	player, true, false, alreadyAttacked);
                    remainingStrengthNeeded -= landStrength;
                    blitzStrength = SUtils.inviteBlitzAttack(false, badTerr, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, true, false);
                    remainingStrengthNeeded -= blitzStrength;
                    planeStrength = SUtils.invitePlaneAttack(false, false, badTerr, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player);
                    remainingStrengthNeeded -= planeStrength;
                    seaStrength = SUtils.inviteTransports(false, badTerr, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, tFirst, false, seaTerrAttacked);
                    remainingStrengthNeeded -= seaStrength;
                    weAttacked = true;
                }
/* This is causing bad results
	            remainingStrengthNeeded += 2.0F;
	            if (weAttacked && remainingStrengthNeeded > 0.0F)
	            {
	            	remainingStrengthNeeded -= SUtils.inviteBlitzAttack(false, badTerr, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, true, false);
	            	remainingStrengthNeeded -= SUtils.invitePlaneAttack(false, false, badTerr, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player);
	            	remainingStrengthNeeded -= SUtils.inviteTransports(false, badTerr, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, tFirst, false, seaTerrAttacked);
	            }
*/			}
            xMoveUnits.clear();
            xMoveRoutes.clear();
            xAlreadyMoved.clear();
		}

 		/**
 		 * Thought here is to organize units that are aggressive in a large bunch
 		 */
 		float maxEStrength = enemyMap.get(maxAttackTerr);
 		PlayerID maxAPlayer = maxAttackTerr.getOwner();
 		float myAttackStrength = SUtils.getStrengthOfPotentialAttackers(maxAttackTerr, data, maxAPlayer, tFirst, false, null);
 		//estimate the target route (most likely the closest allied factory)
 		List<Territory> ourTerr = SUtils.getNeighboringLandTerritories(data, player, maxAttackTerr);
 		Route enemyRoute = SUtils.findNearest(maxAttackTerr, Matches.territoryHasAlliedFactory(data, player), Matches.TerritoryIsNotImpassableToLandUnits(player), data);
 		if (enemyRoute != null)
 		{
 			if (myAttackStrength > maxEStrength)
 			{
 			//do we need to consolidate units?
 			}
 			else
 			{
 			//figure out how to pull units toward the attacking group
 			}
 		}
 			//find the territories we can reasonably expect to take
 		float alliedStrength = 0.0F;
		StrengthEvaluator capStrEval = StrengthEvaluator.evalStrengthAt(data, player, myCapital, false, true, tFirst, true);
    	boolean useCapNeighbors = true;
    	if (capStrEval.inDanger(0.90F)) //TODO: really evaluate the territories around the capitol
    		useCapNeighbors = false; //don't use the capital neighbors to attack terr which are not adjacent to cap
    	SUtils.reorder(enemyOwned, rankMap, true);
    	enemyOwned.removeAll(alreadyAttacked);
        for(Territory enemy : enemyOwned)
        {
			Collection<Unit> eUnits = enemy.getUnits().getUnits();
            float enemyStrength = SUtils.strength(eUnits, false, false, tFirst);
        	TerritoryAttachment ta = TerritoryAttachment.get(enemy);
        	float pValue = ta.getProduction();
            if (Matches.TerritoryIsNeutral.match(enemy) && enemyStrength > pValue*9) //why bother...focus on enemies
            	continue; //TODO: Strengthen this determination
            if(enemyStrength > 0.0F)
            {
                ourStrength = 0.0F;
                alliedStrength = 0.0F;
				Set<Territory> attackFrom = data.getMap().getNeighbors(enemy, Matches.territoryHasNoEnemyUnits(player, data));
				attackFrom.removeAll(impassableTerrs);
				HashMap<Territory, Float> strengthMap = new HashMap<Territory, Float>();
				alreadyAttacked.add(enemy);
				for (Territory aCheck : attackFrom)
					strengthMap.put(aCheck, SUtils.getStrengthOfPotentialAttackers(aCheck, data, player, tFirst, true, alreadyAttacked));
				List<Unit> dontMoveWithUnits = new ArrayList<Unit>();
				List<Territory> attackList = new ArrayList<Territory>(attackFrom);
				SUtils.reorder(attackList, strengthMap, false); //order our available terr by weakest enemy potential
				List<Unit> myAUnits = new ArrayList<Unit>();
                for (Territory checkTerr2 : attackList)
                {
                	float strengthLimit = 0.0F;
                	if (Matches.territoryHasAlliedFactory(data, player).match(checkTerr2))
                	{
                		strengthLimit = strengthMap.get(checkTerr2);
                		List<Unit> ourFactUnits = checkTerr2.getUnits().getMatches(Matches.UnitIsNotSea);
                		ourFactUnits.removeAll(unitsAlreadyMoved);
                		float factStrength = SUtils.strength(ourFactUnits, false, false, tFirst);
                		if (strengthLimit*0.5F > (factStrength+TerritoryAttachment.get(checkTerr2).getProduction()*3.0F))
                			strengthLimit = 0.0F; //won't matter if we stay here
                	}
                	List<Unit> goodUnits = checkTerr2.getUnits().getMatches(attackable);
                	goodUnits.removeAll(unitsAlreadyMoved);
                	Iterator<Unit> goodUIter = goodUnits.iterator();
                	Route gRoute = data.getMap().getLandRoute(checkTerr2, enemy);
                	if (gRoute == null || goodUnits.isEmpty())
                		continue;
                	while (goodUIter.hasNext())
                	{
                		Unit goodUnit = goodUIter.next();
                		if (!MoveValidator.hasEnoughMovement(goodUnit, gRoute) || strengthLimit > 0.0F)
                		{
                			goodUIter.remove();
                			dontMoveWithUnits.add(goodUnit); //block these off later on
                			strengthLimit -= SUtils.uStrength(goodUnit, false, false, tFirst);
                		}
                	}
                	ourStrength += SUtils.strength(goodUnits, true, false, tFirst);
                	List<Unit> aUnits = checkTerr2.getUnits().getMatches(alliedAirLandUnit);
                	aUnits.removeAll(goodUnits);
                	aUnits.removeAll(unitsAlreadyMoved);
                	alliedStrength += SUtils.strength(aUnits, true, false, tFirst);
                	myAUnits.addAll(goodUnits);
				}
				float xRS = 1000.0F;
				xAlreadyMoved.addAll(unitsAlreadyMoved);
				xAlreadyMoved.addAll(myAUnits);
				float blitzStrength = SUtils.inviteBlitzAttack(false, enemy, xRS, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player, true, false);
				xRS -= blitzStrength;
            	float planeStrength = SUtils.invitePlaneAttack(false, false, enemy, xRS, xAlreadyMoved, xMoveUnits, xMoveRoutes, data, player);
            	xRS -= planeStrength;
            	ourStrength += blitzStrength;
            	if (ourStrength < 1.0F)
            		continue;
            	else
            		ourStrength += planeStrength;
				List<Unit> allMyUnits = new ArrayList<Unit>(myAUnits);
				for (Collection<Unit> xUnits : xMoveUnits)
					allMyUnits.addAll(xUnits);
				boolean weWin = false;
				if (Matches.TerritoryIsNeutral.match(enemy))
				{
					if (ourStrength > (attackFactor2 * enemyStrength + 3.0F))
						weWin = true;
				}
				else
					weWin = SUtils.calculateTUVDifference(enemy, allMyUnits, eUnits, costMap, player, data, aggressive, Properties.getAirAttackSubRestricted(data));
                if (!weWin)
                {
                	alreadyAttacked.remove(enemy);
                	continue;
                }

                remainingStrengthNeeded = (attackFactor * enemyStrength) + 4.0F; //limit the attackers
				if (attackFrom.size() == 1) //if we have 1 big attacker
				{
					xTerr = attackList.get(0);
					List<Territory> enemyLandTerr = SUtils.getNeighboringEnemyLandTerritories(data, player, xTerr);
					if (enemyLandTerr.size() == 1) //the only enemy territory is the one we are attacking
					    remainingStrengthNeeded = (maxAttackFactor * enemyStrength) + 4.0F; //blow it away

				}
				float landStrength2 = SUtils.inviteLandAttack(false, enemy, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, true, false, alreadyAttacked);
				remainingStrengthNeeded -= landStrength2;
				float blitzStrength2 = SUtils.inviteBlitzAttack(false, enemy, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, true, false);
				remainingStrengthNeeded -= blitzStrength2;
				float planeStrength2 = SUtils.invitePlaneAttack(false, false, enemy, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player);
//            	deleteStrength = SUtils.verifyPlaneAttack(data, xMoveUnits, xMoveRoutes, player, alreadyAttacked);
//            	planeStrength -= deleteStrength;
				remainingStrengthNeeded -= planeStrength2;
				float seaStrength2 = SUtils.inviteTransports(false, enemy, remainingStrengthNeeded, unitsAlreadyMoved, moveUnits, moveRoutes, data, player, tFirst, false, seaTerrAttacked);
				remainingStrengthNeeded -= seaStrength2;
            }

            weAttacked2 = false;
            xMoveUnits.clear();
            xMoveRoutes.clear();
            xAlreadyMoved.clear();
        }

        populateBomberCombat(data, unitsAlreadyMoved, moveUnits, moveRoutes, player);
    }
    /**
     * Push all remaining loaded units onto the best possible land location
     * @param data
     * @param unitsAlreadyMoved
     * @param moveUnits
     * @param moveRoutes
     * @param player
     */
    private void populateFinalTransportUnload(GameData data, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {
    	Collection<Territory> impassableTerrs = getImpassableTerrs();
        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
		boolean tFirst = transportsMayDieFirst();
    	Match<Unit> myTransport = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsTransport);
    	List<Territory> transTerr = SUtils.findCertainShips(data, player, Matches.UnitIsTransport);
    	if (transTerr.isEmpty())
    		return;
    	List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
    	List<Territory> ourEnemyTerr = new ArrayList<Territory>();
    	HashMap<Territory, Float> rankMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, tFirst, true, true);

    	for (Territory t : transTerr)
    	{
    		List<Unit> myTransports = t.getUnits().getMatches(myTransport);
    		List<Unit> loadedUnits = new ArrayList<Unit>();
    		Iterator<Unit> tIter = myTransports.iterator();
    		while (tIter.hasNext())
    		{
    			Unit transport = tIter.next();
    			if (!tracker.isTransporting(transport))
    				tIter.remove();
    			else
    				loadedUnits.addAll(tracker.transporting(transport));
    		}
    		if (myTransports.isEmpty() || loadedUnits.isEmpty())
    			continue;
    		List<Territory> unloadTerr = SUtils.getNeighboringLandTerritories(data, player, t);
    		if (unloadTerr.isEmpty())
    			continue;
    		SUtils.reorder(unloadTerr, rankMap, true);
    		Territory landOn = unloadTerr.get(0);
    		Route landRoute = data.getMap().getRoute(t, landOn);
    		if (landRoute != null)
    		{
    			moveUnits.add(loadedUnits);
    			moveRoutes.add(landRoute);
    		}
    		
    	}
    }

    private void populateBomberCombat(GameData data, Collection<Unit> unitsAlreadyMoved, List<Collection<Unit>> moveUnits, List<Route> moveRoutes, PlayerID player)
    {
		//bombers will be more involved in attacks...if they are still available, then bomb
        Match<Unit> ownBomber = new CompositeMatchAnd<Unit>(Matches.UnitIsStrategicBomber, Matches.unitIsOwnedBy(player), HasntMoved);
		Match<Territory> routeCond = new CompositeMatchAnd<Territory>(Matches.TerritoryIsNotImpassable, Matches.territoryHasEnemyAA(player, data).invert());
        List<Unit> alreadyMoved = new ArrayList<Unit>();
        IntegerMap<Territory> bomberImpactMap = new IntegerMap<Territory>();
        List<Territory> enemyFactories = new ArrayList<Territory>();
        boolean unitProduction = Properties.getSBRAffectsUnitProduction(data); 
        for (Territory xT : data.getMap().getTerritories())
        {
        	if (Matches.territoryHasEnemyFactory(data, player).match(xT))
        		enemyFactories.add(xT);
        }
        int factProduction = 0;
        for (Territory eFact : enemyFactories)
        {
        	factProduction = unitProduction ? TerritoryAttachment.get(eFact).getUnitProduction() : TerritoryAttachment.get(eFact).getProduction();
        	bomberImpactMap.put(eFact, factProduction);
        }
        SUtils.reorder(enemyFactories, bomberImpactMap, true);
    	List<Territory> bomberTerrs = SUtils.findCertainShips(data, player, Matches.UnitIsStrategicBomber);
        for(Territory t: enemyFactories)
        {
        	int bombable = bomberImpactMap.getInt(t)*(unitProduction ? 2 : 1); //WW2V3 model TODO: build all current game models into method
        	int bombersDeployed = 0;
        	for (Territory bombTerr : bomberTerrs)
        	{
        		Collection<Unit> bombers = t.getUnits().getMatches(ownBomber);
        		bombers.removeAll(alreadyMoved);
        		if(bombers.isEmpty())
        			continue;
        		Match<Territory> routeCondOrEnd = new CompositeMatchOr<Territory>(routeCond, Matches.territoryIs(t));
        		Route bombRoute = data.getMap().getRoute(t, bombTerr, routeCondOrEnd);
        		if (bombRoute == null || bombRoute.getEnd() == null || bombable <= 0)
        			continue;
        		Iterator<Unit> bIter = bombers.iterator();
        		while (bIter.hasNext() && bombable > 0)
        		{
        			Unit bomber = bIter.next();
        			if (bomber == null)
        				continue;
        			if (MoveValidator.canLand(Collections.singleton(bomber), bombTerr, player, data))
        			{
        				moveUnits.add(Collections.singleton(bomber));
        				moveRoutes.add(bombRoute);
        				alreadyMoved.add(bomber);
        				UnitAttachment bA = UnitAttachment.get(bomber.getUnitType());
        				bombersDeployed ++;
        				if (bombersDeployed % 6 != 0)
        					bombable -= bA.getAttackRolls(player)*3; //assume every 6th bomber is shot down
        			}
        		}
        	}
        }
    }


    private int countTransports(GameData data, PlayerID player)
    {
        CompositeMatchAnd<Unit> ownedTransport = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.unitIsOwnedBy(player));
        int sum = 0;
        for(Territory t : data.getMap())
        {
            sum += t.getUnits().countMatches(ownedTransport );
        }
        return sum;
    }

    private int countLandUnits(GameData data, PlayerID player)
    {
        CompositeMatchAnd<Unit> ownedLandUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsLand, Matches.unitIsOwnedBy(player));
        int sum = 0;
        for(Territory t : data.getMap())
        {
            sum += t.getUnits().countMatches(ownedLandUnit );
        }
        return sum;
    }

    /**
     * Count everything except transports
     * @param data
     * @param player
     * @return
     */
    private int countSeaUnits(GameData data, PlayerID player)
    {
        CompositeMatchAnd<Unit> ownedSeaUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.unitIsOwnedBy(player), Matches.UnitIsNotTransport);
        int sum = 0;
        for(Territory t : data.getMap())
        {
            sum += t.getUnits().countMatches(ownedSeaUnit );
        }
        return sum;
    }

    protected void purchase(boolean purcahseForBid, int PUsToSpend, IPurchaseDelegate purchaseDelegate, GameData data, PlayerID player)
    {   //TODO: lot of tweaks have gone into this routine without good organization...need to cleanup
        //breakdown Rules by type and cost
    	int currentRound = data.getSequence().getRound();
		int highPrice = 0;
        List<ProductionRule> rules = player.getProductionFrontier().getRules();
        IntegerMap<ProductionRule> purchase = new IntegerMap<ProductionRule>();
		List<ProductionRule> landProductionRules = new ArrayList<ProductionRule>();
		List<ProductionRule> airProductionRules = new ArrayList<ProductionRule>();
		List<ProductionRule> seaProductionRules = new ArrayList<ProductionRule>();
		List<ProductionRule> transportProductionRules = new ArrayList<ProductionRule>();
		List<ProductionRule> subProductionRules = new ArrayList<ProductionRule>();
		IntegerMap<ProductionRule> bestAttack = new IntegerMap<ProductionRule>();
		IntegerMap<ProductionRule> bestDefense = new IntegerMap<ProductionRule>();
		IntegerMap<ProductionRule> bestTransport = new IntegerMap<ProductionRule>();
		IntegerMap<ProductionRule> bestMaxUnits = new IntegerMap<ProductionRule>();
		IntegerMap<ProductionRule> bestMobileAttack = new IntegerMap<ProductionRule>();
        ProductionRule highRule = null;
        ProductionRule carrierRule = null, fighterRule = null;
        int carrierFighterLimit = 0, maxFighterAttack = 0;
        Resource pus = data.getResourceList().getResource(Constants.PUS);
        boolean isAmphib = isAmphibAttack(player, true);
        
        for (ProductionRule ruleCheck : rules)
		{
			int costCheck = ruleCheck.getCosts().getInt(pus);
			UnitType x = (UnitType) ruleCheck.getResults().keySet().iterator().next();
			if (Matches.UnitTypeIsAir.match(x))
			{
			    airProductionRules.add(ruleCheck);
			}
			else if (Matches.UnitTypeIsSea.match(x))
			{
				seaProductionRules.add(ruleCheck);
			}
			else if (!Matches.UnitTypeIsAAOrFactory.match(x))
			{
				if (costCheck > highPrice)
				{
					highPrice = costCheck;
					highRule = ruleCheck;
				}
				landProductionRules.add(ruleCheck);
			}
			if (Matches.UnitTypeCanTransport.match(x) && Matches.UnitTypeIsSea.match(x))
				transportProductionRules.add(ruleCheck);
			if (Matches.UnitTypeIsSub.match(x))
				subProductionRules.add(ruleCheck);
			if (Matches.UnitTypeIsCarrier.match(x)) //might be more than 1 carrier rule...use the one which will hold the most fighters
			{
				int thisFighterLimit = UnitAttachment.get(x).getCarrierCapacity();
				if (thisFighterLimit >= carrierFighterLimit)
				{
					carrierRule = ruleCheck;
					carrierFighterLimit = thisFighterLimit;
				}
			}
			if (Matches.UnitTypeCanLandOnCarrier.match(x)) //might be more than 1 fighter...use the one with the best attack
			{
				int thisFighterAttack = UnitAttachment.get(x).getAttack(player);
				if (thisFighterAttack > maxFighterAttack)
				{
					fighterRule = ruleCheck;
					maxFighterAttack = thisFighterAttack;
				}
			}
		}

        if (purcahseForBid)
        {
            int buyLimit = PUsToSpend / 3;
            if (buyLimit == 0)
            	buyLimit = 1;
            boolean landPurchase = true, alreadyBought = false, goTransports = false;
            List<Territory> enemyTerritoryBorderingOurTerrs = SUtils.getNeighboringEnemyLandTerritories(data, player);
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
            		buyLimit = PUsToSpend / 5; //assume a larger threshhold
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
            	if (Math.random() > 0.55)
            	{//force a carrier purchase if enough available $$ for it and at least 1 fighter
            		int cost = carrierRule.getCosts().getInt(pus);
        			int fighterCost = fighterRule.getCosts().getInt(pus);
            		if ((cost+fighterCost) <= PUsToSpend)
            		{
            			purchase.add(carrierRule, 1);
            			purchase.add(fighterRule, 1);
            			carrierFighterLimit--;
            			PUsToSpend -= (cost+fighterCost);
            			while ((PUsToSpend >= fighterCost) && carrierFighterLimit > 0)
            			{ //max out the carrier
            				purchase.add(fighterRule, 1);
            				carrierFighterLimit--;
            				PUsToSpend-=fighterCost;
            			}
            		}
            	}
            	int airPUs = PUsToSpend/6;
            	SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, airProductionRules, airPUs, buyLimit, data, player, 2);
            	boolean buyAttack = Math.random() > 0.50;
            	for (ProductionRule rule1 : airProductionRules)
            	{
            		int buyThese = bestAttack.getInt(rule1);
            		int cost = rule1.getCosts().getInt(pus);
            		if (!buyAttack)
            			buyThese = bestDefense.getInt(rule1); 
            		PUsToSpend -= cost*buyThese;
            		while (PUsToSpend < 0 && buyThese > 0)
            		{
            			buyThese--;
            			PUsToSpend += cost;
            		}
            		if (buyThese > 0)
            			purchase.add(rule1, buyThese);
            	}
            	int landPUs = PUsToSpend;
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
        		buyLimit = PUsToSpend / 8; //assume higher end purchase
        		seaProductionRules.addAll(airProductionRules);
        		if (Math.random() > 0.45)
        			SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, seaProductionRules, PUsToSpend, buyLimit, data, player, 2);
        		else
        		{
        			goTransports = true;
        		}
        	}
            List<ProductionRule> processRules = new ArrayList<ProductionRule>();
            if (landPurchase)
            	processRules.addAll(landProductionRules);
            else
            {
            	if (goTransports)
            		processRules.addAll(transportProductionRules);
            	else
            		processRules.addAll(seaProductionRules);
            }
            boolean buyAttack = Math.random() > 0.25;
            int buyThese = 0, numBought = 0;
            for (ProductionRule rule1 : processRules)
            {
            	int cost = rule1.getCosts().getInt(pus);
            	if (goTransports)
            		buyThese = PUsToSpend/cost;
            	else if (buyAttack) 
            		buyThese = bestAttack.getInt(rule1);
            	else if (Math.random() <= 0.25)
            		buyThese = bestDefense.getInt(rule1);
            	else
            		buyThese = bestMaxUnits.getInt(rule1);
            	PUsToSpend -= cost*buyThese;
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
            if (PUsToSpend > 0) //verify a run through the land units
            {
            	buyLimit = PUsToSpend/2;
        		SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, landProductionRules, PUsToSpend, buyLimit, data, player, 2);
            	for (ProductionRule rule2 : landProductionRules)
            	{
            		int cost = rule2.getCosts().getInt(pus);
            		buyThese = bestDefense.getInt(rule2);
            		PUsToSpend -= cost*buyThese;
            		while (buyThese > 0 && PUsToSpend < 0)
            		{
            			buyThese --;
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
//        s_logger.fine("Player: "+ player.getName()+"; PUs: "+PUsToSpend);
 		boolean tFirst = transportsMayDieFirst();
 		boolean shipCapitalThreat = false;
 		isAmphib = isAmphibAttack(player, false);
        CompositeMatch<Unit> enemyUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data));
        CompositeMatch<Unit> attackShip = new CompositeMatchAnd<Unit>(Matches.UnitIsNotTransport, Matches.UnitIsSea);
        CompositeMatch<Unit> enemyAttackShip = new CompositeMatchAnd<Unit>(enemyUnit, attackShip);
        CompositeMatch<Unit> enemyFighter = new CompositeMatchAnd<Unit>(enemyUnit, Matches.UnitCanLandOnCarrier);
        CompositeMatch<Unit> ourAttackShip = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), attackShip);
        CompositeMatch<Unit> alliedAttackShip = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), attackShip);
        CompositeMatch<Unit> enemyTransport = new CompositeMatchAnd<Unit>(enemyUnit, Matches.UnitIsTransport);
        CompositeMatch<Unit> ourFactories = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsFactory);
        CompositeMatch<Unit> transUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.unitIsOwnedBy(player));
        CompositeMatch<Unit> fighter = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanLandOnCarrier);
        CompositeMatch<Unit> alliedFighter = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitCanLandOnCarrier);
        CompositeMatch<Unit> transportableUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitCanBeTransported, Matches.UnitIsNotAA);
        CompositeMatch<Unit> ACUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsCarrier);
        CompositeMatch<Territory> enemyAndNoWater = new CompositeMatchAnd<Territory>(Matches.isTerritoryEnemyAndNotNuetralWater(player, data), Matches.TerritoryIsNotImpassableToLandUnits(player));
        CompositeMatch<Territory> noEnemyOrWater = new CompositeMatchAnd<Territory>(Matches.isTerritoryAllied(player, data), Matches.TerritoryIsNotImpassableToLandUnits(player));
        CompositeMatch<Territory> enemyOnWater = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data));
        
        Territory myCapital = TerritoryAttachment.getCapital(player, data);
        boolean factPurchased = false;
		boolean isLand = SUtils.doesLandExistAt(myCapital, data, false); //gives different info than isamphib
	    boolean skipShips = false, buyTransports = true;
		boolean buyPlanesOnly = false, buyOnePlane=false, buyBattleShip = false, buySub = false, buyOneShip = false, buyCarrier = false;

		List<Territory> factories = SUtils.findUnitTerr(data, player, ourFactories);
     	List<Territory> waterFactories = SUtils.stripLandLockedTerr(data, factories);
     	List<Territory> enemyAttackShipTerr = SUtils.findUnitTerr(data, player, enemyAttackShip);
        List<Territory> ourAttackShipTerr = SUtils.findUnitTerr(data, player, alliedAttackShip);
        List<Territory> enemyTransportTerr = SUtils.findUnitTerr(data, player, enemyTransport);
        int capUnitCount = myCapital.getUnits().countMatches(transportableUnit);
        Set<Territory> capNeighbors = data.getMap().getNeighbors(myCapital, Matches.TerritoryIsWater);
        for (Territory capN : capNeighbors)
        	capUnitCount -= capN.getUnits().countMatches(transUnit)*2;
        int EASCount = 0, OASCount = 0, ETTCount = 0;
        int factoryCount = factories.size();
        int totTransports = countTransports(data, player);
        int totAttackSeaUnits = countSeaUnits(data, player);
        int totLandUnits = countLandUnits(data, player);
        int totEAttackSeaUnits = 0;
        List<PlayerID> enemyPlayers = SUtils.getEnemyPlayers(data, player);
        for (PlayerID ePlayer : enemyPlayers)
        	totEAttackSeaUnits += countSeaUnits(data, ePlayer);
        boolean seaPlaneThreat = false;
        float avgSeaThreat = 0.0F;
    	float ourLocalSeaProtection = 0.0F;
    	int waterProduction = 0;
    	Iterator<Territory> wIter = waterFactories.iterator();
    	while (wIter.hasNext())
    	{
    		Territory wFact = wIter.next();
    		waterProduction += TerritoryAttachment.get(wFact).getProduction();
    	}
    	if (isAmphib && waterProduction < 6) //we don't have enough factories through which to launch attack
    	{
    		List<Territory> allMyTerrs = SUtils.allOurTerritories(data, player);
    		float risk = 0.0F;
    		Territory waterFact = SUtils.findFactoryTerritory(data, player, risk, true, true);    		
    		if (waterFact != null)
    		{
    			waterProduction += TerritoryAttachment.get(waterFact).getProduction();//might want to buy 2
    			for (ProductionRule factoryRule : rules)
    			{
    				int cost = factoryRule.getCosts().getInt(pus);
    				UnitType factoryType = (UnitType) factoryRule.getResults().keySet().iterator().next();
    				if (Matches.UnitTypeIsFactory.match(factoryType))
    				{
    					if (PUsToSpend >= cost && !factPurchased)
    					{
							setFactory(waterFact);
							purchase.add(factoryRule, 1);
							PUsToSpend -=cost;
							factPurchased = true;
						}
					}
				}
    			if (factPurchased)
					purchaseDelegate.purchase(purchase); //This is all we will purchase

				return;
			}
    	}
        if (isAmphib && !waterFactories.isEmpty())
        { //figure out how much protection we need
        	Territory safeTerr = null;
        	Territory closestEnemyCapitol = SUtils.closestEnemyCapital(myCapital, data, player); //find the closest factory to our cap
        	int capEDist = data.getMap().getDistance(myCapital, closestEnemyCapitol);
        	Territory myClosestFactory = SUtils.closestToEnemyCapital(waterFactories, data, player, false); //this is probably our attack base
        	int cFactEDist = data.getMap().getDistance(myClosestFactory, closestEnemyCapitol);
        	if (cFactEDist >= capEDist) //make sure that we use the capitol if it is equidistance
        		myClosestFactory = myCapital;
        	s_logger.fine("Capital: "+myCapital + "; Closest Enemy Capitol: "+ closestEnemyCapitol+"; Closest Factory: "+myClosestFactory);
        	int distFromFactoryToECap = data.getMap().getDistance(closestEnemyCapitol, myClosestFactory);
        	distFromFactoryToECap = Math.max(distFromFactoryToECap, 3);
        	List<Territory> cap3Neighbors = new ArrayList<Territory>(data.getMap().getNeighbors(myClosestFactory, distFromFactoryToECap));
        	Iterator<Territory> nIter = cap3Neighbors.iterator();
        	while (nIter.hasNext())
        	{
        		Territory thisTerr = nIter.next();
        		if (Matches.TerritoryIsLand.match(thisTerr))
        		{
        			nIter.remove();
        			continue;
        		}
        		int distToFactory = data.getMap().getDistance(myClosestFactory, thisTerr);
        		int distToECap = data.getMap().getDistance(closestEnemyCapitol, thisTerr);
        		if ((distToECap + distToFactory) > (distFromFactoryToECap + 2) && distToFactory > 1) //always include all factory neighbors
        		{
        			nIter.remove();
        		}
        	}
        	List<Unit> ourUnits = new ArrayList<Unit>();
        	int seaCapCount = cap3Neighbors.size();
        	float totSeaThreat = 0.0F;
        	for (Territory seaCapTerr : cap3Neighbors)
        	{
        		ourUnits.addAll(seaCapTerr.getUnits().getMatches(alliedAttackShip));
        		totSeaThreat += SUtils.getStrengthOfPotentialAttackers(seaCapTerr, data, player, tFirst, false, null);
        	}
        	avgSeaThreat = totSeaThreat/seaCapCount;
        	ourLocalSeaProtection = SUtils.strength(ourUnits, false, true, tFirst);
        }
        //negative of this is that it assumes all ships in same general area
        //Brits and USA start with ships in two theaters
        for (Territory EAST : enemyAttackShipTerr)
        {
			EASCount += EAST.getUnits().countMatches(enemyAttackShip);
			EASCount += EAST.getUnits().countMatches(enemyFighter);
        }
		for (Territory OAST : ourAttackShipTerr)
		{
			OASCount += OAST.getUnits().countMatches(alliedAttackShip);
			OASCount += OAST.getUnits().countMatches(alliedFighter);
		}
		for (Territory ETT : enemyTransportTerr)
			ETTCount += ETT.getUnits().countMatches(enemyTransport); //# of enemy transports
		boolean doBuyAttackShips = false;
		Territory factCheckTerr = myCapital;
		if (Matches.territoryHasWaterNeighbor(data).invert().match(myCapital))
		{//TODO: This is a weak way of looking at it...need to localize...the problem is a player in two theaters (USA, UK)
			if (EASCount > (OASCount + 2))
				doBuyAttackShips = true;
			if (EASCount > (OASCount*2))
				buyPlanesOnly = true;
			Iterator<Territory> wFIter = waterFactories.iterator();
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
			Territory myCapWaterTerr = SUtils.findASeaTerritoryToPlaceOn(factCheckTerr, data, player, tFirst);
			if (myCapWaterTerr != null)
			{
				strength1 = SUtils.getStrengthOfPotentialAttackers(myCapWaterTerr, data, player, tFirst, false, null);
				strength2 = SUtils.getStrengthOfPotentialAttackers(myCapWaterTerr, data, player, tFirst, true, null);
				airPotential = strength1 - strength2;
			}
		}
		List<Territory> myShipTerrs = SUtils.findOnlyMyShips(myCapital, data, player, alliedAttackShip);
		int shipCount = 0;
		for (Territory shipT : myShipTerrs)
			shipCount += shipT.getUnits().countMatches(alliedAttackShip);
        int totPU = 0, totProd = 0, PUSea = 0, PULand = 0;
		float purchaseT;
		String error = null;
		boolean localShipThreat = false;
		int maxShipThreat = 0, currShipThreat = 0, minDistanceToEnemy = 1000;
		Territory localShipThreatTerr = null;
		boolean nonCapitolFactoryThreat = false;
		boolean seaAdvantageEnemy = ((tFirst ? totTransports : 0)*10 + totAttackSeaUnits*10) < (totEAttackSeaUnits*9 + (tFirst ? ETTCount*5 : 0));
        for (Territory fT : factories)
        {
        	int thisFactProduction = TerritoryAttachment.get(fT).getProduction(); 
			totPU += thisFactProduction;
			totProd += TerritoryAttachment.get(fT).getUnitProduction();
			if (!useProductionData())
				totProd = totPU;
			if (isAmphib)
			{
				 currShipThreat = SUtils.shipThreatToTerr(fT, data, player, tFirst);
				 if ((currShipThreat > 3 && !seaAdvantageEnemy) || (currShipThreat > 2 && seaAdvantageEnemy)) //TODO: Emphasis is exclusively on capital: needs to be expanded to handle pacific Jap fleet
				 {
					 localShipThreat = true;
					 if (fT == myCapital)
					 {
						 setSeaTerr(myCapital);
						 shipCapitalThreat = true;
					 }
					 if (currShipThreat > maxShipThreat)
					 {
						 maxShipThreat = currShipThreat;
						 localShipThreatTerr = fT;
					 }
				 }
			}
			else
			{
				//determine minimum ground distance to enemy
				Route minDistRoute = SUtils.findNearest(fT, Matches.isTerritoryEnemyAndNotNuetralWater(player, data), Matches.TerritoryIsNotImpassableToLandUnits(player), data);
				int thisMinDist = 1000;
				if (minDistRoute != null)
					thisMinDist = minDistRoute.getLength();
				minDistanceToEnemy = Math.min(thisMinDist, minDistanceToEnemy);
			}
			currShipThreat = 0;
			float factThreat = SUtils.getStrengthOfPotentialAttackers(fT, data, player, tFirst, true, null);
			float factStrength = SUtils.strength(fT.getUnits().getUnits(), false, false, tFirst);
			if (factThreat > factStrength)
				nonCapitolFactoryThreat = true;
		} 
        //maximum # of units
		int unitCount=0;
        int leftToSpend = PUsToSpend;
        totPU = leftToSpend;
		purchaseT=1.00F;
		if (isAmphib)
			purchaseT=0.50F;
		List<Territory> ACTerrs = SUtils.ACTerritory(player, data);
		int ACCount = 0, fighterCount = 0;
		for (Territory ACTerr : ACTerrs)
			ACCount += ACTerr.getUnits().countMatches(ACUnit);
		List<Territory> fighterTerrs = SUtils.findCertainShips(data, player, Matches.UnitCanLandOnCarrier);
		for (Territory fighterTerr : fighterTerrs)
			fighterCount += fighterTerr.getUnits().countMatches(fighter);
		//If other factors allow, buy one plane
		if (ACCount > fighterCount)
			buyOnePlane = true;
        List<RepairRule> rrules = Collections.emptyList();
        if(player.getRepairFrontier() != null) // figure out if anything needs to be repaired
        {
            rrules = player.getRepairFrontier().getRules();
            IntegerMap<RepairRule> repairMap = new IntegerMap<RepairRule>();
            HashMap<Territory, IntegerMap<RepairRule>> repair = new HashMap<Territory, IntegerMap<RepairRule>>();
            Boolean repairs = false;
            int diff = 0;
		    for (RepairRule rrule : rrules)
            {
                for (Territory fixTerr : factories)
                {
                    if (!Matches.territoryHasOwnedFactory(data, player).match(fixTerr))
            	 	    continue;
        		    TerritoryAttachment ta = TerritoryAttachment.get(fixTerr);
            	    diff = ta.getProduction() - ta.getUnitProduction();
            	    diff = Math.min(diff, totPU/2);
                    if(diff > 0)
                    {
                        repairMap.add(rrule, diff);
                        repair.put(fixTerr, repairMap);
                        repairs = true;
					}
				}
        	}
            if (repairs)
            {
                error = purchaseDelegate.purchaseRepair(repair);
                leftToSpend -= diff;
            }
    	}

        //determine current land risk to the capitol
        float realSeaThreat = 0.0F, realLandThreat = 0.0F;
        determineCapDanger(player, data);
		StrengthEvaluator capStrEvalLand = StrengthEvaluator.evalStrengthAt(data, player, myCapital, true, true, tFirst, true);
		
        //boolean capDanger = capStrEvalLand.inDanger(0.85F);
		boolean capDanger = getCapDanger();
//        s_logger.fine("Player: "+player.getName()+"; Capital Danger: "+capDanger);
 
		int fighterPresent = 0;
		if (capDanger) //focus on Land Units and buy before any other decisions are made
		{
			landProductionRules.addAll(airProductionRules); //just in case we have a lot of PU
			SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, landProductionRules, leftToSpend, totProd, data, player, 0);
			for (ProductionRule rule1 : landProductionRules)
			{
				int buyThese = bestDefense.getInt(rule1);
				int cost = rule1.getCosts().getInt(pus);
				leftToSpend -= cost*buyThese;
//				s_logger.fine("Cap Danger"+"; Player: "+player.getName()+"Left To Spend: "+leftToSpend);
				while (leftToSpend < 0 && buyThese > 0)
				{
					buyThese--;
					leftToSpend+= cost;
				}
				if (buyThese <= 0)
					continue;
				purchase.add(rule1, buyThese);
				unitCount += buyThese;
			}
	        purchaseDelegate.purchase(purchase);
	        return;
		}

        float PUNeeded = 0.0F;
//        s_logger.fine("Ship Capital Threat: "+shipCapitalThreat+"; Max Ship Threat: "+maxShipThreat);
        if (shipCapitalThreat) //don't panic on a small advantage
        {
        	if (games.strategy.triplea.Properties.getWW2V3(data))  //cheaper naval units
        		PUNeeded = (float) maxShipThreat*5.5F;
        	else
        		PUNeeded = (float) maxShipThreat*6.5F;
        }	//Every 10.0F advantage needs about 7 PU to stop (TODO: Build function for PU needed for ships)
/*        else
        { //force a transport purchase early in the game
        }
*/
        
		realLandThreat = capStrEvalLand.strengthMissing(0.85F); 
		boolean noCapitalThreat = capStrEvalLand.getEnemyStrengthInRange() < 0.50F;
		if ((totEAttackSeaUnits + 2) < totAttackSeaUnits) //override above if we have more on the map
			doBuyAttackShips = false;
		if (isAmphib && shipCapitalThreat && (noCapitalThreat || realLandThreat < -4.0F))
		{ //want to buy ships when we are overwhelmed
			if (Math.random() > 0.80)
				buyOnePlane = true;
			if (nonCapitolFactoryThreat && Math.random() <= 0.70)
			{
				buyPlanesOnly = true;
				doBuyAttackShips = false;
			}
			else
			{
				buyBattleShip=true;
				doBuyAttackShips = true;
			}
			if (!tFirst)
				buyTransports=false;
		}
		else if (!isAmphib && (noCapitalThreat)) 
		{
			if (Math.random() > 0.50)
			{
				buyOnePlane = true;
			}
			Route dRoute = SUtils.findNearest(myCapital, enemyAndNoWater, noEnemyOrWater, data);
			if (shipCapitalThreat && dRoute.getLength() > 3)
				buyBattleShip = false;
		}
		else if (!isAmphib && !isLand && (realLandThreat < -8.0F || noCapitalThreat)) //Britain or Japan with mainland factories...don't let units pile up on capitol
		{
			if ((tFirst && maxShipThreat > 3) || (!tFirst && maxShipThreat > 2))
			{
				doBuyAttackShips = true;
				purchaseT = 0.15F;
				buyTransports = false;
			}
			else if (capUnitCount > 15) //units piling up on capital
        	{
        		buyTransports = true;
        		skipShips = false;
        		purchaseT = 0.25F;
        	}
		}
		if (isAmphib && isLand)
		{
			purchaseT =0.60F;
			if (maxShipThreat > 3 || (doBuyAttackShips && !tFirst))
			{
				buyTransports=false;
				purchaseT = 0.14F;
			}
		}
        if (isAmphib && doBuyAttackShips && realLandThreat < 15.0F) //we are overwhelmed on sea
			purchaseT = 0.00F;
        else if (isAmphib && !isLand) 
        {
    		if (currentRound < 6 && Math.random() < 0.55F)
    			buyOneShip = true; //will be overridden by doBuyAttackShips
        	if (realLandThreat < 2.0F)
        	{
        		buyTransports = true;
        		skipShips = false;
        	}
        	if (capUnitCount > 15 && realLandThreat < 0.0F) //units piling up on capital
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
        	else if (totTransports*3 > totLandUnits) //we have plenty of transports
        	{
        		buyTransports = false;
        		if (!doBuyAttackShips)
        			skipShips = true;
        		purchaseT = 1.00F;
        	}
        	else
        		purchaseT =0.64F;
        }
		if (isAmphib && (PUNeeded > leftToSpend)) //they have major advantage, let's wait another turn
		{
			Territory safeTerr = SUtils.getSafestWaterTerr(myCapital, null, null, data, player, false, tFirst);
			leftToSpend = Math.min(leftToSpend, (int) realLandThreat);
			purchaseT = 1.00F;
			buyTransports = false;
		}
		if (PUNeeded > 0.60F*leftToSpend)
		{
			buyTransports=false;
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
			boolean noWater = !SUtils.isWaterAt(myCapital, data);
			purchaseT = 0.82F;
			if (Math.random() < 0.88 || realLandThreat > 4.0F)
				purchaseT = 1.00F;
			if (noWater)
			{
				purchaseT =1.00F;
				skipShips = true;
			}
		}
		float fSpend = (float) leftToSpend;
        PUSea = (int) (fSpend*(1.00F-purchaseT));
        PULand = leftToSpend - PUSea;
        int minCost = Integer.MAX_VALUE;

		//Test for how badly we want transports
		//If we have a land route to enemy capital...forget about it
		//If we have land units close to us...forget about it
		//If we have a ton of units in our capital, then let's buy transports
	    int transConstant = 2;

    	List <Territory> xy = SUtils.findOnlyMyShips(myCapital, data, player, Matches.UnitIsTransport);
    	List <Unit> capTrans = new ArrayList<Unit>();
    	for (Territory xyz : xy)
    		capTrans.addAll(xyz.getUnits().getMatches(transUnit));
    	List <Unit> capUnits = myCapital.getUnits().getMatches(Matches.UnitCanBeTransported);
    	if (isAmphib & !doBuyAttackShips)
    	{
    		int transportableUnits = 0, transportUnits = 0;
    		List<Territory> myTerritories = SUtils.allAlliedTerritories(data, player);
    		for (Territory xTerr : myTerritories)
    		{
    			if (!Matches.territoryHasEnemyLandNeighbor(data, player).match(xTerr))
    				transportableUnits += xTerr.getUnits().countMatches(transportableUnit);
    		}
    		transportUnits = countTransports(data, player)*2;
    		if (transportUnits > transportableUnits)
    		{
    			skipShips = true;
    			PULand = leftToSpend;
    			PUSea = 0;
    			buyTransports = false;
    		}
    		else if (transportableUnits > 2*transportUnits)
    		{
    			transConstant = 1;
    			buyTransports = true;
    			PULand = leftToSpend - 12;
    			PULand = Math.max(PULand, 0);
    			PUSea = leftToSpend - PULand;
    		}
    	}
		//Purchase land units first
    	/**
    	 * Determine ships/planes within 6 territories/sea zones of capital and around the amphib route endpoint
    	 */
		boolean removeSubs = false;
    	if (isAmphib)
    	{
    		CompositeMatch<Unit> ourAirUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir);
    		CompositeMatch<Unit> enemyAirUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsAir);
    		CompositeMatch<Unit> alliedAirUnit = new CompositeMatchAnd<Unit>(Matches.alliedUnit(player, data), Matches.UnitIsAir);
    		Set<Territory> myTerr = data.getMap().getNeighbors(myCapital, 6);
    		Route amphibRoute = getAmphibRoute(player, false);
    		if (amphibRoute != null && amphibRoute.getEnd() != null)
    		{
    			Territory amphibTerr = amphibRoute.getEnd();
    			Set<Territory> amphibTerrs = data.getMap().getNeighbors(amphibTerr, 3);
    			myTerr.addAll(amphibTerrs);
    		}
    		List<Unit> myShipsPlanes = new ArrayList<Unit>();
    		List<Unit> enemyShipsPlanes = new ArrayList<Unit>();
    		List<Unit> alliedShipsPlanes = new ArrayList<Unit>();
    		int enemyShipCount = 0, enemyPlaneCount = 0;
    		for (Territory checkTerr : myTerr)
    		{
    			if (Matches.TerritoryIsWater.match(checkTerr))
    			{//only count planes which are actually defending sea units
        			myShipsPlanes.addAll(checkTerr.getUnits().getMatches(ourAttackShip));
    				myShipsPlanes.addAll(checkTerr.getUnits().getMatches(ourAirUnit));
        			alliedShipsPlanes.addAll(checkTerr.getUnits().getMatches(alliedAirUnit));
        			alliedShipsPlanes.addAll(checkTerr.getUnits().getMatches(alliedAttackShip));
        			List<Unit> enemyShips = checkTerr.getUnits().getMatches(enemyAttackShip);
        			enemyShipsPlanes.addAll(enemyShips);
        			enemyShipCount += enemyShips.size();
        			alliedShipsPlanes.removeAll(myShipsPlanes);
    			}
    			List<Unit> enemyPlanes = checkTerr.getUnits().getMatches(enemyAirUnit);
    			enemyShipsPlanes.addAll(enemyPlanes);
    			enemyPlaneCount += enemyPlanes.size();
    		}
			int myTotUnits = myShipsPlanes.size() + alliedShipsPlanes.size()/2;
			int enemyTotUnits = enemyShipsPlanes.size();
			if (enemyTotUnits > 0 && myTotUnits < enemyTotUnits + 2)
			{
				doBuyAttackShips = true;
				PUSea = leftToSpend;
				PULand = 0;
				if (enemyPlaneCount > enemyShipCount)
					removeSubs = true;
			}		
    	}
		int landConstant = 2; //we want to loop twice to spread out our purchase
		boolean highPriceLandUnits = false;
		if (leftToSpend > 10*(totProd-2) && !doBuyAttackShips) //if not buying ships, buy planes
		{
			if (Math.random() <= 0.85)
				buyPlanesOnly = true;
			buyCarrier = true;
			buyBattleShip = false;
		}
		else if (leftToSpend > 5*(totProd-1))
			buyOnePlane = true;

		boolean extraPUonPlanes = false;
        if (capDanger) //capital in trouble...purchase units accordingly...most expensive available
        {
        	if (!isLand && !doBuyAttackShips) //try to balance the need for Naval units here
        	{
        		PULand = leftToSpend;
        		PUSea = 0;
        	}
	        
	        extraPUonPlanes = true;
	        buyTransports = false;
        }

        highPriceLandUnits = (highPrice*totProd) < PULand;

		boolean buyfactory = false, buyExtraLandUnits = true; //fix this later...we might want to save PUs
        int maxPurch = leftToSpend/3;
		if (maxPurch > (totPU + 3)) //more money than places to put units...buy more expensive units & a Factory
		{
			buyfactory = true;
			landConstant = 2;
			buyOnePlane = true;
			highPriceLandUnits=true;
		}
		if (realLandThreat <= 0.0F && !doBuyAttackShips && !buyTransports)
			highPriceLandUnits = true;


		if (landConstant !=1 || (highPriceLandUnits && PULand >= 15))
		{
			buyfactory = true;
			int numFactory = 0;
			
		}
		if (isAmphib && !doBuyAttackShips && totTransports <= 15) //TODO: look at deleting this...12 is arbitrary
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
*/		landProductionRules.addAll(airProductionRules); //Try this...add planes into the mix and let the purchase routine handle
		}
		int maxBuy = (totProd - unitCount);
		maxBuy = (purchaseT < 0.70F) ? (maxBuy*3)/4 : (doBuyAttackShips ? 2 : 0);
		if (buyOnePlane)
			maxBuy--;
		if (isAmphib && !doBuyAttackShips)
		{
			List<Territory> myFTerrs = SUtils.findCertainShips(data, player, Matches.UnitCanLandOnCarrier);
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
		List<ProductionRule> newSeaProductionRules = new ArrayList<ProductionRule>(seaProductionRules);
		
		if (buyOneShip)
		{
			if (Math.random() > 0.25) //no subs 75% of the time if buying only 1 ship
			{
				Iterator<ProductionRule> subRule = newSeaProductionRules.iterator();
				while (subRule.hasNext())
				{
					ProductionRule checkRule = subRule.next();
					UnitType x = (UnitType) checkRule.getResults().keySet().iterator().next();
					if (Matches.UnitTypeIsSub.match(x))
						subRule.remove();
					else if (Matches.UnitTypeIsSea.match(x) && Matches.unitTypeCanAttack(player).invert().match(x))
						subRule.remove(); //want to purchase an attacking unit
				}
			}
			ProductionRule maxRule = null;
			int maxCost = 0;
			if (Math.random() <= 0.5) //take out battleships 50% of the time
			{
				Iterator<ProductionRule> BBRule = newSeaProductionRules.iterator();
				while (BBRule.hasNext())
				{
					ProductionRule checkRule = BBRule.next();
					UnitType x = (UnitType) checkRule.getResults().keySet().iterator().next();
					if (Matches.UnitTypeIsBB.match(x))
						BBRule.remove();
				}
			}
			for (ProductionRule shipRule : newSeaProductionRules)
			{ //random purchase
				UnitType x = (UnitType) shipRule.getResults().keySet().iterator().next();
				int shipcost = shipRule.getCosts().getInt(pus);
				if (maxRule == null && shipcost < PUSea && (Math.random() < 0.20 || shipRule.equals(newSeaProductionRules.get(newSeaProductionRules.size()-1))))
				{
					maxCost = shipcost;
					maxRule = shipRule;
				}
			}
			if (maxRule != null && maxCost <= leftToSpend && unitCount < totProd)
			{//buy as many as possible
				int buyThese = PUSea / maxCost;
				buyThese = (unitCount + buyThese) <= totProd ? buyThese : totProd - unitCount;
				leftToSpend -= maxCost*buyThese;
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
					unitCount+= buyThese;
				}
			}
		}
			
//		s_logger.fine("Player: "+player.getName()+"; Is Amphib: "+isAmphib+"; IsLand: "+isLand+"; DoBuyAttackShips: "+doBuyAttackShips+"; Buy Transports: "+buyTransports);
//		s_logger.fine("PUs: "+leftToSpend+"; PU Land: "+PULand+"; PU Sea: "+PUSea+"; TotProduction: "+totProd+"; Current Unit Count: "+unitCount);
		if (PUSea > 0 && (doBuyAttackShips || buyBattleShip) && maxBuy > 0 && unitCount < totProd) //attack oriented sea units
		{
			if (isAmphib && !capDanger &&  maxShipThreat > 2)
				PUSea = leftToSpend;
			if (unitCount < 2)
				setAttackShipPurchase(true);
			fighterPresent = myCapital.getUnits().countMatches(Matches.UnitCanLandOnCarrier);
			if (PUSea > 0)
			{
				int buyThese = 0;
				int AttackType = 1; //bestAttack
				if (Math.random() <= 0.45) //for ships, focus on defense set most of the time
					AttackType = 2;
				if (Math.random() >= 0.65 && factoryCount == 1 && PUNeeded > 0.75*leftToSpend) //50% maxUnits when need a lot of ships 
					AttackType = 3;
				Route eShipRoute = SUtils.findNearest(myCapital, enemyOnWater, Matches.TerritoryIsWater, data);
				int enemyShipDistance = 0;
				if (eShipRoute != null)
					enemyShipDistance = eShipRoute.getLength();
				if (enemyShipDistance > 3)
					AttackType = 5;
				if (buyBattleShip)
				{
					for (ProductionRule BBRule : seaProductionRules)
					{
		                UnitType results = (UnitType) BBRule.getResults().keySet().iterator().next();
		                if (Matches.UnitTypeIsBB.match(results))
		                {
		                	int BBcost = BBRule.getCosts().getInt(pus);
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
					for (ProductionRule CarrierRule : seaProductionRules)
					{
		                UnitType results = (UnitType) CarrierRule.getResults().keySet().iterator().next();
		                if (Matches.UnitTypeIsCarrier.match(results))
		                {
		                	int Carriercost = CarrierRule.getCosts().getInt(pus);
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
					if (carrierBought && leftToSpend > 0 && unitCount < totProd)
					{
						boolean fighterBought = false;
		                UnitType results = (UnitType) fighterRule.getResults().keySet().iterator().next();
		                if (!fighterBought)
		                {
		                	int fighterCost = fighterRule.getCosts().getInt(pus);
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
						Iterator<ProductionRule> sPIter = seaProductionRules.iterator();
						while (sPIter.hasNext())
						{
							ProductionRule shipRule = sPIter.next();
							UnitType subUnit = (UnitType) shipRule.getResults().keySet().iterator().next();
							if (Matches.UnitTypeIsSub.match(subUnit))
								sPIter.remove();
						}
					}
					SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, seaProductionRules, PUSea, maxBuy, data, player, fighterPresent);
					for (ProductionRule rule1 : seaProductionRules)
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
						int cost = rule1.getCosts().getInt(pus);
						int numToBuy = 0;
		                UnitType results = (UnitType) rule1.getResults().keySet().iterator().next();
						while (unitCount < totProd && leftToSpend >= cost && PUSea >= cost && numToBuy < buyThese)
						{
							unitCount++;
							leftToSpend -= cost;
							PUSea -= cost;
							numToBuy++;
			                if (Matches.UnitTypeIsCarrier.match(results)) //attempt to add a fighter to every carrier purchased
			                {
			                	boolean fighterBought = false;
		                		if (!fighterBought)
		                		{
			                		int fighterCost = fighterRule.getCosts().getInt(pus);
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
					}
				}
			}
			bestAttack.clear();
			bestDefense.clear();
			bestTransport.clear();
			bestMaxUnits.clear();
		}
		if (!doBuyAttackShips && leftToSpend >= 15) //determine factory first to make sure enough PU...doesn't count toward units
		{
			int numFactory = 0;
			for (Territory fT2 : factories)
			{
				if (SUtils.hasLandRouteToEnemyOwnedCapitol(fT2, player, data))
					numFactory++;
				if (!SUtils.doesLandExistAt(fT2, data, false))
					continue;
				List<Territory> enemyFactoriesInRange = new ArrayList<Territory>(data.getMap().getNeighbors(fT2, 3));
				Iterator<Territory> eFIter = enemyFactoriesInRange.iterator();
				while (eFIter.hasNext())
				{//count enemy factory which is close enough to take
					Territory factTerr = eFIter.next();
					if (Matches.territoryHasEnemyFactory(data, player).invert().match(factTerr) || data.getMap().getLandRoute(fT2, factTerr) == null)
						eFIter.remove();
				}
				numFactory += enemyFactoriesInRange.size();
				
			}
			if (numFactory >= 2)
				buyfactory = false; //allow 2 factories on the same continent
			if (!buyfactory)
			{
				int minDistToEnemy = 100;
				for (Territory fT3 : factories) //what is the minimum distance to the enemy?
				{
					Route landDistRoute = SUtils.findNearest(fT3, Matches.isTerritoryEnemyAndNotNeutral(player, data), Matches.TerritoryIsNotImpassable, data);
					if (landDistRoute != null && landDistRoute.getLength() < minDistToEnemy)
						minDistToEnemy = landDistRoute.getLength();
				}
				if (minDistToEnemy > 5) //even if a lot of factories...build a factory closer to enemy
					buyfactory = true;
			}
			/*
			 * Watch out for having a good distance to enemy, but laying factories back at your base
			 * Goal is to get Germany building factories as the invasion of Russia takes place in NWO
			 */
			if (buyfactory)
			{
				for (ProductionRule factoryRule : rules)
				{
					int cost = factoryRule.getCosts().getInt(pus);
					UnitType factoryType = (UnitType) factoryRule.getResults().keySet().iterator().next();
					if (Matches.UnitTypeIsFactory.match(factoryType))
					{
						if (leftToSpend >= cost && !factPurchased)
						{
							float riskFactor = 1.0F;
							Territory factTerr = SUtils.findFactoryTerritory(data, player, riskFactor, buyfactory, false);

							if (factTerr != null)
							{
								setFactory(factTerr);
								purchase.add(factoryRule, 1);
								leftToSpend -=cost;
								PULand -= cost;
								factPurchased = true;
								if (PULand < 0)
									PUSea = leftToSpend;
							}
						}
					}
				}
			}
		} //done buying factories...only buy 1
		maxBuy = (totProd - unitCount);
		maxBuy = (purchaseT > 0.25) ? maxBuy/2 : maxBuy;
		PUSea = Math.min(PUSea, leftToSpend - PULand);
		if (buyTransports && maxBuy > 0 && !transportProductionRules.isEmpty())
		{ //assume a single transport rule
			ProductionRule tRule = transportProductionRules.get(0);
			int cost = tRule.getCosts().getInt(pus);
			int numTrans = leftToSpend/cost;
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
			{ //exchange a transport for a destroyer
				numToBuy--;
				leftToSpend += cost;
				PUSea += cost;
				unitCount--;
				for (ProductionRule destroyerRule : seaProductionRules)
				{
					UnitType d = (UnitType) destroyerRule.getResults().keySet().iterator().next();
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
		}
		maxBuy = totProd - unitCount;
		maxBuy = buyOnePlane ? (maxBuy - 1) : maxBuy;
		bestAttack.clear();
		bestDefense.clear();
		bestTransport.clear();
		bestMaxUnits.clear();
		bestMobileAttack.clear();
		if (!buyPlanesOnly && maxBuy > 0) //attack oriented land units
		{
			SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, landProductionRules, PULand, maxBuy, data, player, fighterPresent);
			int buyThese = 0;
			int AttackType = 1; //bestAttack
			if (Math.random() <= 0.5 || (isLand && Math.random() > 0.080)) //just to switch it up...every once in a while, buy the defensive set
				AttackType = 2;
			if ((Math.random() >= 0.25 && factoryCount >= 2) || (nonCapitolFactoryThreat) ) //if we have a lot of factories, use the max Unit set most of the time
				AttackType = 3;
			if (isAmphib || Math.random() < 0.25)
				AttackType = 4;
			if (!isAmphib && minDistanceToEnemy >= 4 && Math.random() >= 0.10)
				AttackType = 5;
			String attackString = AttackType == 1 ? "Best Attack" : AttackType == 2 ? "Best Defense" : AttackType == 3 ? "Best Max Units" : AttackType == 4 ? "Best Transport" : "Best Mobile"; 
			for (ProductionRule rule1 : landProductionRules)
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
				int cost = rule1.getCosts().getInt(pus);
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
		if ((buyPlanesOnly || buyOnePlane) && maxBuy > 0)
		{
			maxBuy = (buyOnePlane && !buyPlanesOnly) ? 1 : maxBuy;
			SUtils.findPurchaseMix(bestAttack, bestDefense, bestTransport, bestMaxUnits, bestMobileAttack, airProductionRules, leftToSpend, maxBuy, data, player, fighterPresent);
			
			int buyThese = 0;
			int AttackType = 1; //bestAttack
			if (Math.random() <= 0.50) 
				AttackType = 2;
			if (Math.random() >= 0.50 && factoryCount > 2) 
				AttackType = 3;
			else if (Math.random() > 0.25)
				AttackType = 5;
			for (ProductionRule rule1 : airProductionRules)
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
				int cost = rule1.getCosts().getInt(pus);
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
			PUSea = leftToSpend; //go ahead and make them available TODO: make sure that it is worth buying a transport
        	if (currentRound <= 8 && ! transportProductionRules.isEmpty())
        	{
        		ProductionRule transRule = transportProductionRules.get(0);
        		int cost = transRule.getCosts().getInt(pus);
        		maxBuy = leftToSpend/cost;
        		maxBuy = Math.max(1, maxBuy-1);
        		if (cost*maxBuy <= leftToSpend)
        		{
        			purchase.add(transRule, maxBuy);
        			leftToSpend -= cost*maxBuy;
        		}
        	}
		}			
		if ((unitCount < totProd) && buyExtraLandUnits && leftToSpend > 0)
		{
			for (ProductionRule quickProd : rules)
			{
                int quickCost = quickProd.getCosts().getInt(pus);

                if (leftToSpend < quickCost || unitCount >= totProd)
                	continue;

                UnitType intResults = (UnitType) quickProd.getResults().keySet().iterator().next();
                if (Matches.UnitTypeIsSeaOrAir.match(intResults) || Matches.UnitTypeIsAAOrFactory.match(intResults))
					continue;

				if (quickCost <= leftToSpend && unitCount < totProd)
				{
					int purchaseNum = totProd - unitCount;
					int numLand = (int) (leftToSpend / quickCost);
					int actualPNum = Math.min(purchaseNum, numLand);
					leftToSpend -= quickCost * actualPNum;
					while (leftToSpend < 0 && actualPNum > 0)
					{
						actualPNum--;
						leftToSpend+= quickCost;
					}
					if (actualPNum > 0)
					{
						purchase.add(quickProd, actualPNum);
						unitCount += actualPNum;
					}
				}
			}
		}
		if (leftToSpend > 0 && (unitCount < totProd) && extraPUonPlanes)
		{
			for (ProductionRule planeProd : rules)
			{
				int planeCost = planeProd.getCosts().getInt(pus);
				if (leftToSpend < planeCost || unitCount >= totProd)
					continue;
				UnitType plane = (UnitType) planeProd.getResults().keySet().iterator().next();
				if (Matches.UnitTypeIsAir.match(plane))
				{
					if (capDanger && !Matches.unitTypeCanBombard(player).match(plane)) //buy best defensive plane
					{
						int maxPlanes = totProd - unitCount;
						int costPlanes = leftToSpend/planeCost;
						int buyThese = Math.min(maxPlanes, costPlanes);
						leftToSpend -= maxPlanes*planeCost;
//						s_logger.fine("Extra Air"+"; Player: "+player.getName()+"Left To Spend: "+leftToSpend);
						while (leftToSpend < 0 && buyThese > 0)
						{
							buyThese--;
							leftToSpend+= planeCost;
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
							
        purchaseDelegate.purchase(purchase);
    }


    protected void place(boolean bid, IAbstractPlaceDelegate placeDelegate, GameData data, PlayerID player)
    {
        //if we have purchased a factory, it will be a priority for placing units
        //should place most expensive on it
        //need to be able to handle AA purchase
        if (player.getUnits().size() == 0)
            return;
        Collection<Territory> impassableTerrs = getImpassableTerrs();
        final BattleDelegate delegate = DelegateFinder.battleDelegate(data);

		boolean tFirst = transportsMayDieFirst();
        CompositeMatch<Unit> ownedUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player));
        CompositeMatch<Unit> attackUnit = new CompositeMatchOr<Unit>(Matches.UnitIsSea, Matches.UnitIsNotTransport);
        CompositeMatch<Unit> transUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport);
        CompositeMatch<Unit> enemyUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data));
        CompositeMatch<Unit> enemyAttackUnit = new CompositeMatchAnd<Unit>(attackUnit, enemyUnit);
        CompositeMatch<Unit> enemyTransUnit = new CompositeMatchAnd<Unit>(transUnit, enemyUnit);
        CompositeMatch<Unit> ourFactory = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsFactory);
        CompositeMatch<Unit> landUnit = new CompositeMatchAnd<Unit>(ownedUnit, Matches.UnitIsLand, Matches.UnitIsNotFactory);
        CompositeMatch<Territory> ourLandTerr = new CompositeMatchAnd<Territory>(Matches.isTerritoryOwnedBy(player), Matches.TerritoryIsLand);
        Territory capitol =  TerritoryAttachment.getCapital(player, data);
        List<Territory> factoryTerritories = SUtils.findUnitTerr(data, player, ourFactory);

        /**
         * Bid place with following criteria:
         * 1) Has an enemy Neighbor
         * 2) Has the largest combination value:
         *    a) enemy Terr
         *    b) our Terr
         *    c) other Terr neighbors to our Terr
         *    d) + 2 for each of these which are victory cities
         */
        if (bid)
        {
        	List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
        	List<Territory> ourEnemyTerr = new ArrayList<Territory>();
        	List<Territory> ourTerrs = SUtils.allOurTerritories(data, player);
        	ourTerrs.remove(capitol); //we'll check the cap last
        	HashMap<Territory, Float> rankMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, tFirst, false, true);
        	List<Territory> ourTerrWithEnemyNeighbors = SUtils.getTerritoriesWithEnemyNeighbor(data, player, false, false);
        	SUtils.reorder(ourTerrWithEnemyNeighbors, rankMap, true);
//        	ourFriendlyTerr.retainAll(ourTerrs);
        	Territory bidLandTerr = null;
        	if (ourTerrWithEnemyNeighbors.size() > 0)
        		bidLandTerr = ourTerrWithEnemyNeighbors.get(0);
        	if (bidLandTerr == null)
        		bidLandTerr = capitol;
        	if (player.getUnits().someMatch(Matches.UnitIsSea))
        	{
        		Territory bidSeaTerr = null, bidTransTerr = null;
        		CompositeMatch<Territory> enemyWaterTerr = new CompositeMatchAnd<Territory>(Matches.TerritoryIsWater, Matches.territoryHasEnemyUnits(player, data));
        		List<Territory> enemySeaTerr = SUtils.findUnitTerr(data, player, enemyAttackUnit);
        		Territory maxEnemySeaTerr = null;
        		int maxUnits = 0;
        		for (Territory seaTerr : enemySeaTerr)
        		{
        			int unitCount = seaTerr.getUnits().countMatches(enemyAttackUnit);
        			if (unitCount > maxUnits)
        			{
        				maxUnits = unitCount;
        				maxEnemySeaTerr = seaTerr;
        			}
        		}
        		Route seaRoute = SUtils.findNearest(maxEnemySeaTerr, Matches.TerritoryIsWater, Matches.territoryHasNoAlliedUnits(player, data).invert(), data);
        		if (seaRoute != null)
        		{
        			Territory checkSeaTerr = seaRoute.getEnd();
        			if (checkSeaTerr != null)
        			{
        				float seaStrength = SUtils.getStrengthOfPotentialAttackers(checkSeaTerr, data, player, tFirst, false, null);
        				float aStrength = SUtils.strength(checkSeaTerr.getUnits().getUnits(), false, true, tFirst);
        				if (aStrength > 0.85F*seaStrength)
        					bidSeaTerr = checkSeaTerr;
        			}
        		}
        		for (Territory factCheck : factoryTerritories)
        		{
        			if (bidSeaTerr == null)
        				bidSeaTerr = SUtils.findASeaTerritoryToPlaceOn(factCheck, data, player, tFirst);
        			if (bidTransTerr == null)
        				bidTransTerr = SUtils.findASeaTerritoryToPlaceOn(factCheck, data, player, tFirst);
        		}
        		placeSeaUnits(bid, data, bidSeaTerr, bidSeaTerr, placeDelegate, player);
        	}
        	if (player.getUnits().someMatch(Matches.UnitIsNotSea)) //TODO: Match fighters with carrier purchase
        		placeAllWeCanOn(bid, data, null, bidLandTerr, placeDelegate, player);
        	return;
        }
        determineCapDanger(player, data);
        Territory specSeaTerr = getSeaTerr();
        boolean capDanger = getCapDanger();
        boolean amphib = isAmphibAttack(player, true);
        //maybe we bought a factory
        Territory factTerr = getFactory();
        if (factTerr != null)
        	placeAllWeCanOn(bid, data, factTerr, factTerr, placeDelegate, player);
        if (capDanger)
        	placeAllWeCanOn(bid, data, capitol, capitol, placeDelegate, player);
        //check for no factories, but still can place
        RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTATCHMENT_NAME);
        if ((ra != null && ra.getPlacementAnyTerritory()) || bid) //make them all available for placing
           factoryTerritories.addAll(SUtils.allOurTerritories(data, player));
        List<Territory> cloneFactTerritories = new ArrayList<Territory>(factoryTerritories);
        for (Territory deleteBad : cloneFactTerritories)
        {
			if (delegate.getBattleTracker().wasConquered(deleteBad))
				factoryTerritories.remove(deleteBad);
		}

        int minDist = 100;
        if (!factoryTerritories.contains(capitol))
        	factoryTerritories.add(capitol);
        /*
        Here is plan: Place units at the factory which is closest to a bad guy Territory
        			  Place transports at the factory which has the most land units
                      Place attack sea units at the factory closest to attack sea units
                      Tie goes to the capitol
        */
        Territory seaPlaceAtTrans = null, seaPlaceAtAttack = null, landFactTerr = null;
		float eStrength = 0.0F;
		IntegerMap<Territory> landUnitFactories = new IntegerMap<Territory>();
		IntegerMap<Territory> transportFactories = new IntegerMap<Territory>();
		IntegerMap<Territory> seaAttackUnitFactories = new IntegerMap<Territory>();
		Route goRoute = new Route();
		
		int landUnitCount = player.getUnits().countMatches(landUnit);
		int transUnitCount = player.getUnits().countMatches(transUnit);
		int seaAttackUnitCount = player.getUnits().countMatches(attackUnit);
		int fighterUnitCount = player.getUnits().countMatches(Matches.UnitCanLandOnCarrier);
		int carrierUnitCount = player.getUnits().countMatches(Matches.UnitIsCarrier);
		List<Territory> transTerr = SUtils.findCertainShips(data, player, Matches.UnitIsTransport);
		List<Territory> landNeighbors = new ArrayList<Territory>();
		for (Territory tT : transTerr)
			landNeighbors.addAll(SUtils.getNeighboringLandTerritories(data, player, tT));
		landNeighbors.retainAll(factoryTerritories);
		int maxUnits = 0;
		Territory maxUnitTerr = null;
		for (Territory unitFact : factoryTerritories)
		{
			int thisFactUnitCount = unitFact.getUnits().countMatches(landUnit);
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
			}
			else
			{
				landUnitFactories.put(unitFact, 0);
				transportFactories.put(unitFact, 0);
				seaAttackUnitFactories.put(unitFact, 0);
			}
		}
		//case for Russia and Germany in WW2V2 and Italians in WW2V3
		if (transportFactories.size() == 0)
		{
			for (Territory unitFact2 : factoryTerritories)
			{
				int shipThreat = SUtils.shipThreatToTerr(unitFact2, data, player, tFirst);
				if (tFirst && shipThreat < 2 && unitFact2 == maxUnitTerr)
					transportFactories.put(unitFact2, 2);
				else if (!tFirst &&  shipThreat <= 0 && unitFact2 == maxUnitTerr)
					transportFactories.put(unitFact2, 2);
				else if (SUtils.isWaterAt(unitFact2, data) && !Matches.territoryHasEnemyLandNeighbor(data, player).match(unitFact2))
					transportFactories.put(unitFact2, 0);
			}
		}
		if (seaAttackUnitFactories.size() == 0)
		{
			for (Territory unitFact3 : factoryTerritories)
			{
				if (SUtils.isWaterAt(unitFact3, data))
					seaAttackUnitFactories.put(unitFact3, 0);
			}
		}
		Collection<Territory> landFactories = landUnitFactories.keySet();
		List<Territory> landRouteFactories = new ArrayList<Territory>();
		for (Territory landCheck : landFactories)
		{
			/*
			 * Rank by: 1) Threat 2) Proximity to enemy factories 3) Proximity to enemy capital 4) Proximity to enemy
			 */
			float totThreat = SUtils.getStrengthOfPotentialAttackers(landCheck, data, player, tFirst, false, null);
			float myStrength = SUtils.strength(landCheck.getUnits().getUnits(), false, false, tFirst);
			boolean landRoute = SUtils.landRouteToEnemyCapital(landCheck, goRoute, data, player);
			if (landCheck == capitol && totThreat > myStrength) //basically the same as capDanger
			{
				landUnitFactories.put(landCheck, 4);
			}
			else if (landCheck != capitol && totThreat > myStrength && !capDanger)
				landUnitFactories.put(landCheck, 4);
			else if (totThreat > (myStrength + 5.0F))
				landUnitFactories.put(landCheck, 3);
			else if (totThreat - myStrength > -10.0F && totThreat > 8.0F) //only have a marginal advantage
			{
				landUnitFactories.put(landCheck, 1);
				landRouteFactories.add(landCheck);
			}
			else if (landRoute)
				landRouteFactories.add(landCheck);
		}
		Territory minTerr = null;
//		List<Territory> landRouteFactories2 = new ArrayList<Territory>();
		//check territories which have a land route to a capital but don't have a strong local threat
		for (Territory landCheck2 : landRouteFactories)
		{
			boolean landRoute2 = SUtils.landRouteToEnemyCapital(landCheck2, goRoute, data, player);
			goRoute = null;
			goRoute = SUtils.findNearest(landCheck2, Matches.territoryHasEnemyFactory(data, player), Matches.TerritoryIsNotImpassableToLandUnits(player), data);
			if ((landRoute2 && goRoute != null))
			{
				int lRDist =  goRoute.getLength();
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
		float strengthToOvercome = 0.0F;
		Set<Territory> transFactories = transportFactories.keySet();
		float carrierFighterAddOn = 0.0F;
		if (carrierUnitCount > 0)
		{
			if (fighterUnitCount > 0)
			{
				carrierFighterAddOn += fighterUnitCount*3.5F;
			}
		}
		if (transFactories.size() == 1 && seaAttackUnitFactories.size() == 1)
		{
			for (Territory oneFact : transFactories)
			{
				Territory checkFirst = SUtils.findASeaTerritoryToPlaceOn(oneFact, data, player, tFirst);
				seaPlaceAtTrans = SUtils.getSafestWaterTerr(oneFact, null, null, data, player, false, tFirst);
				if (checkFirst != null)
				{
					float oneStrength = SUtils.getStrengthOfPotentialAttackers(checkFirst, data, player, tFirst, false, null);
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
			for (Territory transCheck : transFactories)
			{
				int unitsHere = transCheck.getUnits().countMatches(landUnit);
				Territory dropHere = SUtils.getSafestWaterTerr(transCheck, null, null, data, player, false, tFirst);
				//Territory dropHere = SUtils.findASeaTerritoryToPlaceOn(transCheck, strengthToOvercome, data, player, tFirst);
				if (dropHere == null)
					continue;
				float eSeaStrength = SUtils.getStrengthOfPotentialAttackers(dropHere, data, player, tFirst, true, null);
				if ((eSeaStrength == 0.0F && unitsHere > transUnitCount*2) || (eSeaStrength > 5.0F && dropHere.getUnits().someMatch(Matches.UnitIsSea)))
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
*/			}
		}
		Territory tempTerr = null;
		if (seaPlaceAtAttack == null) //TODO: Mixed searching sea locations between purchase and place...merge
		{
			Set<Territory> seaAttackFactories = seaAttackUnitFactories.keySet();
			for (Territory checkAgain : seaAttackFactories)
			{
				int attackAdv = SUtils.shipThreatToTerr(checkAgain, data, player, tFirst);
				if (attackAdv > 0)
				{
					tempTerr = SUtils.getSafestWaterTerr(checkAgain, null, null, data, player, false, tFirst);
					//tempTerr = SUtils.findASeaTerritoryToPlaceOn(checkAgain, eStrength, data, player, tFirst);
					if (tempTerr != null && (attackAdv - 1) < seaAttackUnitCount + tempTerr.getUnits().getMatches(attackUnit).size())
						seaPlaceAtAttack = tempTerr;
				}
			}
		}
		Territory tmpSeaLoc = null;
		if (specSeaTerr != null)
		{//purchasing had a special place in mind
			tmpSeaLoc = SUtils.findASeaTerritoryToPlaceOn(specSeaTerr, data, player, tFirst);
			//tmpSeaLoc = SUtils.getSafestWaterTerr(specSeaTerr, null, null, data, player, false, tFirst);
		}
		if (tmpSeaLoc != null)
			seaPlaceAtAttack = tmpSeaLoc;
		if (!bid && capDanger)
			landUnitFactories.put(capitol, 3);

		landFactories.clear();
		landFactories.addAll(landUnitFactories.keySet());
		if (landUnitFactories.size() == 1)
		{
			for (Territory theOne : landFactories)
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
			for (int i=4; i >= 0; i--)
			{
				for (Territory whichOne : landFactories)
				{
					if (landUnitFactories.getInt(whichOne) == i)
					{
						landFactTerr = whichOne;
						if (seaPlaceAtTrans == null)
						{
							Route whichRoute = SUtils.findNearest(whichOne, Matches.territoryHasEnemyLandNeighbor(data, player), Matches.TerritoryIsNotImpassableToLandUnits(player), data);
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
		//if we have some that we haven't placed
        Collections.shuffle(factoryTerritories);
        for(Territory t : factoryTerritories)
        {
			Territory seaPlaceAt = SUtils.findASeaTerritoryToPlaceOn(t, data, player, tFirst);
			if (seaPlaceAt == null)
				seaPlaceAt = seaPlaceAtTrans;
			if (seaPlaceAt == null)
				seaPlaceAt = t; //just put something...maybe there are no sea factories
			placeSeaUnits(bid, data, seaPlaceAt, seaPlaceAt, placeDelegate, player);
            placeAllWeCanOn(bid, data, null, t, placeDelegate, player);
        }
    }
    
    private void placeSeaUnits(boolean bid, GameData data, Territory seaPlaceAttack, Territory seaPlaceTrans, IAbstractPlaceDelegate placeDelegate, PlayerID player)
    {
        CompositeMatch<Unit> attackUnit = new CompositeMatchAnd<Unit>(Matches.UnitIsSea, Matches.UnitIsNotTransport);
       	List<Unit> seaUnits = player.getUnits().getMatches(attackUnit);
    	List<Unit> transUnits = player.getUnits().getMatches(Matches.UnitIsTransport);
    	List<Unit> airUnits = player.getUnits().getMatches(Matches.UnitCanLandOnCarrier);
    	List<Unit> carrierUnits = player.getUnits().getMatches(Matches.UnitIsCarrier);
    	if (carrierUnits.size() > 0 && airUnits.size() > 0 && (Properties.getProduce_Fighters_On_Carriers(data) || bid))
    	{
    		int carrierSpace = 0;
    		for (Unit carrier1 : carrierUnits)
    			carrierSpace += UnitAttachment.get(carrier1.getType()).getCarrierCapacity();
    		Iterator<Unit> airIter = airUnits.iterator();
    		while (airIter.hasNext() && carrierSpace > 0)
    		{
    			Unit airPlane = airIter.next();
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
    	PlaceableUnits pu = placeDelegate.getPlaceableUnits(seaUnits, seaPlaceAttack);
    	int pLeft = 0;
    	if (pu.getErrorMessage() != null)
    		return;
    	if (seaPlaceAttack == seaPlaceTrans)
    	{
    		seaUnits.addAll(transUnits);
    		transUnits.clear();
    	}
    	if (!seaUnits.isEmpty())
    	{
    		pLeft = pu.getMaxUnits();
    		if (pLeft == -1)
    			pLeft = Integer.MAX_VALUE;
    		int numPlace = Math.min(pLeft, seaUnits.size());
    		pLeft -= numPlace;
    		Collection<Unit> toPlace = seaUnits.subList(0, numPlace);
    		doPlace(seaPlaceAttack, toPlace, placeDelegate);
    	}
    	if (!transUnits.isEmpty())
    	{
    		PlaceableUnits pu2 = placeDelegate.getPlaceableUnits(transUnits, seaPlaceTrans);
    		if (pu2.getErrorMessage() != null)
    			return;
			pLeft = pu2.getMaxUnits();
			if (pLeft == -1)
				pLeft = Integer.MAX_VALUE;
    		int numPlace = Math.min(pLeft, transUnits.size());
    		Collection<Unit> toPlace = transUnits.subList(0, numPlace);
    		doPlace(seaPlaceTrans, toPlace, placeDelegate);
    	}
    	
    }


    private void placeAllWeCanOn(boolean bid, GameData data, Territory factoryPlace, Territory placeAt, IAbstractPlaceDelegate placeDelegate, PlayerID player)
    {
        CompositeMatch<Unit> landOrAir = new CompositeMatchOr<Unit>(Matches.UnitIsAir, Matches.UnitIsLand);

		if (factoryPlace != null ) //place a factory?
		{
			Collection<Unit> toPlace = new ArrayList<Unit>(player.getUnits().getMatches(Matches.UnitIsFactory));
			if (toPlace.size() == 1) //only 1 may have been purchased...anything greater is wrong
			{
				doPlace(factoryPlace, toPlace, placeDelegate);
				return;
			}
			else if (toPlace.size() > 1)
				return;
		}

        List<Unit> landUnits = player.getUnits().getMatches(landOrAir);

        PlaceableUnits pu3 = placeDelegate.getPlaceableUnits(landUnits , placeAt);
        if(pu3.getErrorMessage() != null)
            return;
        int placementLeft3 =  pu3.getMaxUnits();
        if(placementLeft3 == -1)
            placementLeft3 = Integer.MAX_VALUE;
        if (bid)
        	placementLeft3 = 1000;

        if(!landUnits.isEmpty())
        {
            int landPlaceCount = Math.min(placementLeft3, landUnits.size());
            placementLeft3 -= landPlaceCount;
            Collection<Unit> toPlace = landUnits.subList(0, landPlaceCount);

            doPlace(placeAt, toPlace, placeDelegate);
        }

    }

    private void doPlace(Territory where, Collection<Unit> toPlace, IAbstractPlaceDelegate del)
    {
        String message = del.placeUnits(new ArrayList<Unit>(toPlace), where);
        if(message != null)
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

    public CasualtyDetails selectCasualties(Collection<Unit> selectFrom, Map<Unit, Collection<Unit>> dependents, int count, String message, DiceRoll dice, PlayerID hit, List<Unit> defaultCasualties, GUID battleID)
    {
        final GameData data = getPlayerBridge().getGameData();
        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();

        List<Unit> rDamaged = new ArrayList<Unit>();
        List<Unit> rKilled = new ArrayList<Unit>();
        List<Unit> workUnits = new ArrayList<Unit>();
		int xCount = count; //how many the game is saying we should have
		for (Unit unitBB : selectFrom)
		{
			if (Matches.UnitIsTwoHit.match(unitBB))
			{
				if (unitBB.getHits()==0 && xCount > 0)
				{
					rDamaged.add(unitBB);
					xCount--;
				}
			}
		}
		if (xCount == 0)
		{
			return new CasualtyDetails(rKilled, rDamaged, false);
		}
		if (xCount >= selectFrom.size())
		{
			rKilled.addAll(selectFrom);
			CasualtyDetails m4 = new CasualtyDetails(rKilled, rDamaged, false);
			return m4;
		}

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
			for (Unit unitx : selectFrom) //empty transport
			{
				if (!Properties.getTransportCasualtiesRestricted(data) && Matches.UnitIsTransport.match(unitx) && !tracker.isTransporting(unitx) )
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
			for (Unit unitx : selectFrom) //loaded transport
			{
				if (!Properties.getTransportCasualtiesRestricted(data) && Matches.UnitIsTransport.match(unitx) && tracker.isTransporting(unitx) )
					workUnits.add(unitx);
			}
			for (Unit unitx : selectFrom)
			{
				if (Matches.UnitIsCarrier.match(unitx))
					workUnits.add(unitx);
			}
			for (Unit unitx : selectFrom) //any other unit, but make sure trannys are last if necessary
			{
				if (Matches.UnitIsNotTransport.match(unitx) && !workUnits.contains(unitx) && !Matches.UnitIsTwoHit.match(unitx))
				    workUnits.add(unitx);
			}
			for (Unit unitx : selectFrom)
			{
				if (Matches.UnitIsTwoHit.match(unitx))
					workUnits.add(unitx);
			}

			//add anything not selected above
			Set<Unit> remainder =new HashSet<Unit>(selectFrom);
			remainder.removeAll(workUnits);
			workUnits.addAll(remainder);
		}
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

		for (int j=0; j < xCount; j++)
		{
			rKilled.add(workUnits.get(j));
		}

        CasualtyDetails m2 = new CasualtyDetails(rKilled, rDamaged, false);
        return m2;
    }

    /*
     * @see games.strategy.triplea.player.ITripleaPlayer#shouldBomberBomb(games.strategy.engine.data.Territory)
     */
    public boolean shouldBomberBomb(Territory territory)
    {
		//only if not needed in a battle
    	GameData data = getPlayerBridge().getGameData();
    	PlayerID ePlayer = territory.getOwner();
    	List<PlayerID> attackPlayers = SUtils.getEnemyPlayers(data, ePlayer); //list of players that could be the attacker
    	boolean thisIsAnAttack = false;
    	for (PlayerID player : attackPlayers)
    	{
    		CompositeMatch<Unit> noBomberUnit = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsNotStrategicBomber);
    		List<Unit> allAttackUnits = territory.getUnits().getMatches(noBomberUnit);
    		if (!allAttackUnits.isEmpty())
    			thisIsAnAttack = true;
    	}
    	return !thisIsAnAttack;
    }
    
    public boolean selectAttackSubs(Territory unitTerritory)
    {
        return true;
    }

    public boolean selectAttackUnits(Territory unitTerritory)
    {
        return true;
    }

    /*
     * (non-Javadoc)
     * @see games.strategy.triplea.baseAI.AbstractAI#selectAttackTransports(games.strategy.engine.data.Territory)
     */
    public boolean selectAttackTransports(Territory territory)
    {
    	return true;
    }

    /*
     * @see games.strategy.triplea.player.ITripleaPlayer#getNumberOfFightersToMoveToNewCarrier(java.util.Collection, games.strategy.engine.data.Territory)
     */
    public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(Collection<Unit> fightersThatCanBeMoved, Territory from)
    {
        List<Unit> rVal = new ArrayList<Unit>();
        for(Unit fighter : fightersThatCanBeMoved)
            rVal.add(fighter);
        return rVal;
    }

    /*
     * @see games.strategy.triplea.player.ITripleaPlayer#selectTerritoryForAirToLand(java.util.Collection, java.lang.String)
     */
    public Territory selectTerritoryForAirToLand(Collection candidates)
    {
		//need to land in territory with infantry, especially if bomber
       return (Territory) candidates.iterator().next();
    }

    public boolean confirmMoveInFaceOfAA(Collection aaFiringTerritories)
    {
        return false;
    }

    /**
     * Select the territory to bombard with the bombarding capable unit (eg battleship)
     *
     * @param unit - the bombarding unit
     * @param unitTerritory - where the bombarding unit is
     * @param territories - territories where the unit can bombard
     * @param noneAvailable
     * @return the Territory to bombard in, null if the unit should not bombard
     */
    public Territory selectBombardingTerritory(Unit unit,Territory unitTerritory, Collection<Territory> territories, boolean noneAvailable )
    {
		if (noneAvailable || territories.size() == 0)
			return null;
		else
		{
			for (Territory t : territories)
				return t;
		}
		return null;
	}

    public Territory retreatQuery(GUID battleID, boolean submerge, Collection<Territory> possibleTerritories, String message)
    {
		//retreat anytime only air units are remaining
		//submerge anytime only subs against air units
		//don't understand how to use this routine
        final GameData data = getPlayerBridge().getGameData();
        boolean iamOffense = get_onOffense();
		boolean tFirst = transportsMayDieFirst();
        TransportTracker tracker = DelegateFinder.moveDelegate(data).getTransportTracker();
        BattleTracker bTracker = DelegateFinder.battleDelegate(data).getBattleTracker();
        Territory battleTerr = getBattleTerritory();
        boolean attacking = false; //determine whether player is offense or defense
        boolean subsCanSubmerge = games.strategy.triplea.Properties.getSubmersible_Subs(data);
        if (battleTerr == null)
        	return null;
        PlayerID player = getWhoAmI();
        List<PlayerID> ePlayers = SUtils.getEnemyPlayers(data, player);
        
    	List<Unit> myUnits = battleTerr.getUnits().getMatches(Matches.unitIsOwnedBy(player));
    	List<Unit> defendingUnits = battleTerr.getUnits().getMatches(Matches.enemyUnit(player, data));
        if (Matches.TerritoryIsLand.match(battleTerr))
        {
        	List<Unit> retreatUnits = new ArrayList<Unit>();
        	List<Unit> nonRetreatUnits = new ArrayList<Unit>();
        	for (Unit u : myUnits)
        	{
        		if (TripleAUnit.get(u).getWasAmphibious())
        			nonRetreatUnits.add(u);
        		else
        			retreatUnits.add(u);
        	}
        	float retreatStrength = SUtils.strength(retreatUnits, true, false, false);
        	float nonRetreatStrength = SUtils.strength(nonRetreatUnits, true, false, false);
        	float totalStrength = retreatStrength + nonRetreatStrength;
        	float enemyStrength = SUtils.strength(defendingUnits, false, false, false);
        	if (totalStrength > enemyStrength*1.05F)
        		return null;
        	else
        	{
        		Territory retreatTo = null;
        		float retreatDiff = 0.0F;
        		if (possibleTerritories.size() == 1)
        			retreatTo = possibleTerritories.iterator().next();
        		else
        		{
            		List<Territory> ourFriendlyTerr = new ArrayList<Territory>();
            		List<Territory> ourEnemyTerr = new ArrayList<Territory>();
            		HashMap<Territory, Float> rankMap = SUtils.rankTerritories(data, ourFriendlyTerr, ourEnemyTerr, null, player, false, false, true);
            		if (ourFriendlyTerr.containsAll(possibleTerritories))
            			SUtils.reorder(ourFriendlyTerr, rankMap, true);
            		ourFriendlyTerr.retainAll(possibleTerritories);
            		Territory myCapital = TerritoryAttachment.getCapital(player, data);
            		for (Territory capTerr : ourFriendlyTerr)
            		{
            			if (Matches.territoryHasAlliedFactory(data, player).match(capTerr))
            			{
            				boolean isMyCapital = myCapital.equals(capTerr);
            				float strength1 = SUtils.getStrengthOfPotentialAttackers(capTerr, data, player, false, true, null);
            				float ourstrength = SUtils.strengthOfTerritory(data, capTerr, player, false, false, false, true);
            				if (isMyCapital)
            				{
            					ourstrength = SUtils.strength(player.getUnits().getUnits(), false, false, false);
            				}
            				if (ourstrength < strength1 && (retreatTo == null || isMyCapital))
            					retreatTo = capTerr;
            			}
            		}
        			Iterator<Territory> retreatTerrs =  ourFriendlyTerr.iterator();
        			if (retreatTo == null)
        			{
        				while (retreatTerrs.hasNext())
        				{
        					Territory retreatTerr = retreatTerrs.next();
        					float existingStrength = SUtils.strength(retreatTerr.getUnits().getUnits(), false, false, false);
        					float eRetreatStrength = SUtils.getStrengthOfPotentialAttackers(retreatTerr, data, player, false, true, null);
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
        	CompositeMatch<Unit> mySub = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSub, Matches.unitIsNotSubmerged(data));
        	CompositeMatch<Unit> myShip = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsSea, Matches.unitIsNotSubmerged(data));
        	CompositeMatch<Unit> myPlane = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(player), Matches.UnitIsAir);
        	CompositeMatch<Unit> enemyAirUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsNotLand);
        	CompositeMatch<Unit> enemySeaUnit = new CompositeMatchAnd<Unit>(Matches.enemyUnit(player, data), Matches.UnitIsSea);
        	CompositeMatch<Unit> alliedShip = new CompositeMatchAnd<Unit>(Matches.isUnitAllied(player, data), Matches.unitIsOwnedBy(player).invert(), Matches.UnitIsNotLand	);
        	
    		List<Unit> myShips = battleTerr.getUnits().getMatches(myShip);
    		List<Unit> myPlanes = battleTerr.getUnits().getMatches(myPlane);
    		float myShipStrength = SUtils.strength(myShips, attacking, true, tFirst);
    		float myPlaneStrength = SUtils.strength(myPlanes, attacking, true, tFirst);
    		float totalStrength = myShipStrength + myPlaneStrength;
            PlayerID ePlayer = ePlayers.get(0); //just be arbitrary on ocean
    		
    		List<Unit> enemyAirUnits = battleTerr.getUnits().getMatches(enemyAirUnit);
    		List<Unit> enemySeaUnits = battleTerr.getUnits().getMatches(enemySeaUnit);
    		float enemyAirStrength = SUtils.strength(enemyAirUnits, !attacking, true, tFirst);
    		float enemySeaStrength = SUtils.strength(enemySeaUnits, !attacking, true, tFirst);
    		float enemyStrength = enemyAirStrength + enemySeaStrength;
    		IntegerMap<UnitType> myUnitList = SUtils.convertListToMap(myShips);
    		myUnitList.add(SUtils.convertListToMap(myPlanes));
    		HashMap<PlayerID, IntegerMap<UnitType>> unitCost = SUtils.getPlayerCostMap(data);
    		int myTUV = BattleCalculator.getTUV(myShips, unitCost.get(player)) + BattleCalculator.getTUV(myPlanes, unitCost.get(player));
    		int eTUV = BattleCalculator.getTUV(enemySeaUnits, unitCost.get(ePlayer)) + BattleCalculator.getTUV(enemyAirUnits, unitCost.get(ePlayer));
        	//Create submersible part here
        	if (battleTerr.getUnits().someMatch(mySub) && enemyStrength > (totalStrength + 1.0F))
        	{
        	}
        	//if (attacking && myTUV <= eTUV)
        	if (attacking && enemyStrength > (totalStrength + 1.0F))
        	{ //TODO: Create a selection for best seaTerritory
        		Territory retreatTo = possibleTerritories.iterator().next();
        		return retreatTo;
        	}
        }
        return null;
    }

    /* (non-Javadoc)
     * @see games.strategy.triplea.player.ITripleaPlayer#selectFixedDice(int, java.lang.String)
     */
    public int[] selectFixedDice(int numRolls, int hitAt, boolean hitOnlyIfEquals, String message)
    {
        int[] dice = new int[numRolls];
        for (int i=0; i<numRolls; i++)
        {
            dice[i] = (int)Math.ceil(Math.random() * 6);
        }
        return dice;
    }

    // some additional matches
    public static final Match<Unit> HasntMoved = new Match<Unit>()
    {
    	public boolean match(Unit o)
        {
            return TripleAUnit.get(o).getAlreadyMoved() == 0;
        }
    };


    public static final Match<Unit> Transporting = new Match<Unit>()
    {
    	public boolean match(Unit o)
        {
            return (TripleAUnit.get(o).getTransporting().size() > 0);
        }
    };

}
