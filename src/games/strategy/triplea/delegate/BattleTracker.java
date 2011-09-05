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
 * BattleTracker.java
 *
 * Created on November 15, 2001, 11:18 AM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.PlayerAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Sean Bridges
 * @version 1.0
 *
 * Used to keep track of where battles have occurred
 */
@SuppressWarnings("serial")
public class BattleTracker implements java.io.Serializable
{
    //List of pending battles
    private Set<Battle> m_pendingBattles = new HashSet<Battle>();

    //List of battle dependencies
    //maps blocked -> Collection of battles that must precede
    private Map<Battle, HashSet<Battle>> m_dependencies = new HashMap<Battle, HashSet<Battle>>();

    //enemy and neutral territories that have been conquered
    //blitzed is a subset of this
    private Set<Territory> m_conquered = new HashSet<Territory>();

    //blitzed territories
    private Set<Territory> m_blitzed = new HashSet<Territory>();

    //territories where a battle occurred
    private Set<Territory> m_foughBattles = new HashSet<Territory>();

    //these territories have had battleships bombard during a naval invasion
    //used to make sure that the same battleship doesn't bombard twice
    private Set<Territory> m_bombardedFromTerritories = new HashSet<Territory>();

    /**
     * @param t referring territory
     * @param bombing
     * @return whether a battle is to be fought in the given territory
     */
    public boolean hasPendingBattle(Territory t, boolean bombing)
    {
        return getPendingBattle(t, bombing) != null;
    }

    /**
     * add to the conquered.
     */
    void addToConquered(Collection<Territory> territories)
    {
        m_conquered.addAll(territories);
    }

    void addToConquered(Territory territory)
    {
        m_conquered.add(territory);
    }

    /**
     * @param t referring territory
     * @return whether territory was conquered
     */
    public boolean wasConquered(Territory t)
    {
        return m_conquered.contains(t);
    }

    /**
     * @param t referring territory
     * @return whether territory was conquered by blitz
     */
    public boolean wasBlitzed(Territory t)
    {
        return m_blitzed.contains(t);
    }

    public boolean wasBattleFought(Territory t)
    {
        return m_foughBattles.contains(t);
    }

    public void undoBattle(Route route, Collection<Unit> units, PlayerID player, GameData data, IDelegateBridge bridge)
    {
        Iterator<Battle> battleIter = new ArrayList<Battle>(m_pendingBattles).iterator();
        while (battleIter.hasNext())
        {
            Battle battle = battleIter.next();
            if (battle.getTerritory().equals(route.getEnd()))
            {
                battle.removeAttack(route, units);
                if (battle.isEmpty())
                {
                    removeBattleForUndo(battle);
                }
            }
        }

        //if we have no longer conquered it, clear the blitz state
        Iterator<Territory> terrIter = route.getTerritories().iterator();
        while (terrIter.hasNext())
        {
            Territory current = terrIter.next();
            if (!data.getRelationshipTracker().isAllied(current.getOwner(), player) && m_conquered.contains(current))
            {
                m_conquered.remove(current);
                m_blitzed.remove(current);
            }
        }
      //say they weren't in combat
        CompositeChange change = new CompositeChange();
    	Iterator <Unit> attackIter = units.iterator();

    	while (attackIter.hasNext())
    		{
    			change.add(ChangeFactory.unitPropertyChange(attackIter.next(), false, TripleAUnit.WAS_IN_COMBAT));
    		}
    	bridge.addChange(change);

    }

    private void removeBattleForUndo(Battle battle)
    {
	    m_pendingBattles.remove(battle);
        m_dependencies.remove(battle);
        Iterator<HashSet<Battle>> iter = m_dependencies.values().iterator();
        while (iter.hasNext())
        {
            Collection<Battle> battles = iter.next();
            battles.remove(battle);
        }

    }

    public void addBattle(Route route, Collection<Unit> units, boolean bombing, PlayerID id, GameData data,
            IDelegateBridge bridge, UndoableMove changeTracker)
    {
        if (bombing)
        {
            addBombingBattle(route, units, id, data);
            //say they were in combat
            markWasInCombat(units, bridge, changeTracker);
        }
        else
        {
            Change change = addMustFightBattleChange(route, units, id, data);
            bridge.addChange(change);
            if(changeTracker != null)
            {
                changeTracker.addChange(change);
            }
            //battles resulting from
            //emerging subs cant be neutral or empty
            if (route.getLength() != 0)
            {
                if(games.strategy.util.Match.someMatch(units, Matches.UnitIsLand) || games.strategy.util.Match.someMatch(units, Matches.UnitIsSea))
                    addEmptyBattle(route, units, id, data, bridge, changeTracker);
            }
        }
    }


    public void addBattle(Route route, Collection<Unit> units, boolean bombing, PlayerID id, GameData data,
            IDelegateBridge bridge, UndoableMove changeTracker, Collection<Unit> targets)
    {
        if (bombing)
        {
            addBombingBattle(route, units, id, data, targets);
            //say they were in combat
            markWasInCombat(units, bridge, changeTracker);
        }
        else
        {
            Change change = addMustFightBattleChange(route, units, id, data);
            bridge.addChange(change);
            if(changeTracker != null)
            {
                changeTracker.addChange(change);
            }
            //battles resulting from
            //emerging subs cant be neutral or empty
            if (route.getLength() != 0)
            {
                if(games.strategy.util.Match.someMatch(units, Matches.UnitIsLand) || games.strategy.util.Match.someMatch(units, Matches.UnitIsSea))
                    addEmptyBattle(route, units, id, data, bridge, changeTracker);
            }
        }
    }

	private void markWasInCombat(Collection<Unit> units, IDelegateBridge bridge, UndoableMove changeTracker)
	{
		if(units == null)
			return;

		CompositeChange change = new CompositeChange();
		Iterator <Unit> attackIter = units.iterator();

		while (attackIter.hasNext())
		{
			change.add(ChangeFactory.unitPropertyChange(attackIter.next(), true, TripleAUnit.WAS_IN_COMBAT));
		}
		bridge.addChange(change);
		if(changeTracker != null)
		{
			changeTracker.addChange(change);
		}
	}

    private void addBombingBattle(Route route, Collection<Unit> units, PlayerID attacker, GameData data)
    {
        Battle battle = getPendingBattle(route.getEnd(), true);
        if (battle == null)
        {
            battle = new StrategicBombingRaidBattle(route.getEnd(), data, attacker, route.getEnd().getOwner(), this);
            m_pendingBattles.add(battle);
        }

        Change change = battle.addAttackChange(route, units);
        //when state is moved to the game data, this will change
        if(!change.isEmpty())
        {
            throw new IllegalStateException("Non empty change");
        }

        //dont let land battles in the same territory occur before bombing battles
        Battle dependent = getPendingBattle(route.getEnd(), false);
        if (dependent != null)
            addDependency(dependent, battle);
    }


    private void addBombingBattle(Route route, Collection<Unit> units, PlayerID attacker, GameData data, Collection<Unit> targets)
    {
        Battle battle = getPendingBattle(route.getEnd(), true);
        if (battle == null)
        {
            battle = new StrategicBombingRaidBattle(route.getEnd(), data, attacker, route.getEnd().getOwner(), this, targets);
            m_pendingBattles.add(battle);
        }

        Change change = battle.addAttackChange(route, units);
        //when state is moved to the game data, this will change
        if(!change.isEmpty())
        {
            throw new IllegalStateException("Non empty change");
        }

        //dont let land battles in the same territory occur before bombing battles
        Battle dependent = getPendingBattle(route.getEnd(), false);
        if (dependent != null)
            addDependency(dependent, battle);
    }
    /**
     * No enemies.
     */
    private void addEmptyBattle(Route route, Collection<Unit> units, final PlayerID id, final GameData data,
            IDelegateBridge bridge, UndoableMove changeTracker)
    {
        //find the territories that are considered blitz
        Match<Territory> canBlitz = new Match<Territory>()
        {
            public boolean match(Territory territory)
            {
                return MoveValidator.isBlitzable(territory, data, id);
            }
        };

        CompositeMatch<Territory> conquerable = new CompositeMatchAnd<Territory>();
        conquerable.add(Matches.territoryIsEmptyOfCombatUnits(data, id));

        CompositeMatch<Territory> blitzable = new CompositeMatchOr<Territory>();
        blitzable.add(Matches.TerritoryIsBlitzable(id, data));
        blitzable.add(Matches.isTerritoryEnemyAndNotUnownedWaterOrImpassibleOrRestricted(id, data));

        conquerable.add(blitzable);

        Collection<Territory> conquered = route.getMatches(conquerable);

        //we handle the end of the route later
        conquered.remove(route.getEnd());
        Collection<Territory> blitzed = Match.getMatches(conquered, canBlitz);

        m_blitzed.addAll(blitzed);
        m_conquered.addAll(conquered);

        Iterator<Territory> iter = conquered.iterator();
        while (iter.hasNext())
        {
            Territory current = iter.next();

            takeOver(current, id, bridge, data, changeTracker, units);
        }

        //check the last territory
        if (conquerable.match(route.getEnd()))
        {
            Battle precede = getDependentAmphibiousAssault(route);
            if(precede == null) {
                precede = getPendingBattle(route.getEnd(), true);
            }
            if (precede == null)
            {
                if (canBlitz.match(route.getEnd()))
                {
                    m_blitzed.add(route.getEnd());
                }
                takeOver(route.getEnd(), id, bridge, data, changeTracker, units);
                m_conquered.add(route.getEnd());
            } else
            {
                Battle nonFight = getPendingBattle(route.getEnd(), false);
                if (nonFight == null)
                {
                    nonFight = new NonFightingBattle(route.getEnd(), id, this, true, data);
                    m_pendingBattles.add(nonFight);
                }

                Change change = nonFight.addAttackChange(route, units);
                bridge.addChange(change);
                if(changeTracker != null)
                {
                    changeTracker.addChange(change);
                }
                addDependency(nonFight, precede);
            }
        }

    }

    @SuppressWarnings({ "unchecked" })
    protected void takeOver(Territory territory, final PlayerID id, IDelegateBridge bridge, GameData data, UndoableMove changeTracker, Collection<Unit> arrivingUnits)
    {
        OriginalOwnerTracker origOwnerTracker = DelegateFinder.battleDelegate(data).getOriginalOwnerTracker();

        // If this is a convoy (we wouldn't be in this method otherwise) check to make sure attackers
        // have more than just transports
        if(territory.isWater() && arrivingUnits != null)
        {
            int totalMatches = 0;

            // 0 production waters aren't to be taken over
            TerritoryAttachment ta = TerritoryAttachment.get(territory);
            if(ta == null)
                return;

            //Total Attacking Sea units = all units - land units - air units - submerged subs
            //Also subtract transports & subs (if they can't control sea zones)
            totalMatches= arrivingUnits.size() - Match.countMatches(arrivingUnits, Matches.UnitIsLand) -
                Match.countMatches(arrivingUnits, Matches.UnitIsAir) -
                Match.countMatches(arrivingUnits, Matches.unitIsSubmerged(data));

            //If transports are restricted from controlling sea zones, subtract them
            CompositeMatch<Unit> transportsCanNotControl = new CompositeMatchAnd<Unit>();
            transportsCanNotControl.add(Matches.UnitIsTransportAndNotDestroyer);
            transportsCanNotControl.add(Matches.UnitIsTransportButNotCombatTransport);
            if(!games.strategy.triplea.Properties.getTransportControlSeaZone(data))
            	totalMatches -= Match.countMatches(arrivingUnits, transportsCanNotControl);
            //TODO check if istrn and NOT isDD

            //If subs are restricted from controlling sea zones, subtract them
            if(games.strategy.triplea.Properties.getSubControlSeaZoneRestricted(data))
            	totalMatches -= Match.countMatches(arrivingUnits, Matches.UnitIsSub);

            if (totalMatches == 0)
                 return;
        }

        // If it was a Convoy Route- check ownership of the associated neighboring territory and set message
        if(TerritoryAttachment.get(territory).isConvoyRoute())
        {
            //Determine if both parts of the convoy route are owned by the attacker or allies
            boolean ownedConvoyRoute =  data.getMap().getNeighbors(territory, Matches.territoryHasConvoyOwnedBy(id, data, territory)).size() > 0;
            int PUCharge = TerritoryAttachment.get(territory).getProduction();
            Territory valuedTerritory = data.getMap().getTerritory(territory.getName());

            //If the captured territory is water, get the associated land territory for reporting.
            if (territory.isWater())
            {
                valuedTerritory = data.getMap().getTerritory(TerritoryAttachment.get(territory).getConvoyAttached());
                PUCharge = TerritoryAttachment.get(valuedTerritory).getProduction();
            }

            if(ownedConvoyRoute)
            {
                bridge.getHistoryWriter().addChildToEvent(
                        valuedTerritory.getOwner() + " gain " + PUCharge + " production for liberating the convoy route in " + territory.getName());
            }
            else
            {
                bridge.getHistoryWriter().addChildToEvent(
                        valuedTerritory.getOwner() + " lose " + PUCharge + " production due to the capture of the convoy route in " + territory.getName());
            }
        }

        //if neutral, we may charge money to enter
        if (territory.getOwner().isNull() && !territory.isWater() && games.strategy.triplea.Properties.getNeutralCharge(data) != 0)
        {
        	Resource PUs = data.getResourceList().getResource(Constants.PUS);
        	int PUChargeIdeal = -games.strategy.triplea.Properties.getNeutralCharge(data);
        	int PUChargeReal = Math.min(0, Math.max(PUChargeIdeal, -id.getResources().getQuantity(PUs)));
        	Change neutralFee = ChangeFactory.changeResourcesChange(id, PUs, PUChargeReal);
        	bridge.addChange(neutralFee);
        	if (changeTracker != null)
        		changeTracker.addChange(neutralFee);
        	if (PUChargeIdeal == PUChargeReal)
        	{
        		bridge.getHistoryWriter().addChildToEvent(id.getName() + " loses " + -PUChargeReal + " "
           			 + MyFormatter.pluralize("PU", -PUChargeReal) + " for violating " + territory.getName()
           			+ "s neutrality.");
        	}
        	else
        	{
        		bridge.getHistoryWriter().addChildToEvent(id.getName() + " loses " + -PUChargeReal + " "
           			 + MyFormatter.pluralize("PU", -PUChargeReal) + " for violating " + territory.getName()
           			+ "s neutrality.  Correct amount to charge is: " + -PUChargeIdeal
           			+ ".  Player should not have been able to make this attack!");
        	}
        }

        //if its a capital we take the money
        //NOTE: this is not checking to see if it is an enemy. instead it is relying on the fact that the capital should be owned by the person it is attached to
        TerritoryAttachment ta = TerritoryAttachment.get(territory);
        if (ta.getCapital() != null)
        {
            //if the capital is owned by the capitols player
            //take the money
            PlayerID whoseCapital = data.getPlayerList().getPlayerID(ta.getCapital());
            PlayerAttachment pa = PlayerAttachment.get(id);
            PlayerAttachment paWhoseCapital = PlayerAttachment.get(whoseCapital);
            List<Territory> capitalsList = new ArrayList<Territory>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(whoseCapital, data));
            if (paWhoseCapital != null && paWhoseCapital.getRetainCapitalNumber() < capitalsList.size()) // we are losing one right now, so it is < not <=
            {
            	// do nothing, we keep our money since we still control enough capitals
            	bridge.getHistoryWriter().addChildToEvent(id.getName() + " captures one of " + whoseCapital.getName() + " capitals");
            }
            else if (whoseCapital.equals(territory.getOwner()))
            {
                Resource PUs = data.getResourceList().getResource(Constants.PUS);
                int capturedPUCount = whoseCapital.getResources().getQuantity(PUs);
                if(pa != null)
                {
                    if(isPacificTheater(data))
                    {
                        Change changeVP = ChangeFactory.attachmentPropertyChange(pa, (Integer.valueOf(capturedPUCount + Integer.parseInt(pa.getCaptureVps()))).toString(), "captureVps");
                        bridge.addChange(changeVP);
                    }
                }
                Change remove = ChangeFactory.changeResourcesChange(whoseCapital, PUs, -capturedPUCount);
                bridge.addChange(remove);
    			if (paWhoseCapital != null && paWhoseCapital.getDestroysPUs())
    			{
    				bridge.getHistoryWriter().addChildToEvent(
                        id.getName() + " destroys " + capturedPUCount + MyFormatter.pluralize("PU", capturedPUCount) + " while taking "
                                + whoseCapital.getName() + " capital");
                    if (changeTracker != null)
                        changeTracker.addChange(remove);
    			}
                else
                {
                	bridge.getHistoryWriter().addChildToEvent(
                                id.getName() + " captures " + capturedPUCount + MyFormatter.pluralize("PU", capturedPUCount) + " while taking "
                                + whoseCapital.getName() + " capital");
                    if (changeTracker != null)
                        changeTracker.addChange(remove);
                	Change add = ChangeFactory.changeResourcesChange(id, PUs, capturedPUCount);
	                bridge.addChange(add);
	                if (changeTracker != null)
	                    changeTracker.addChange(add);
                }
              //remove all the tokens  of the captured player
                Resource tokens = data.getResourceList().getResource(Constants.TECH_TOKENS);
                if(tokens != null)
                {
                    int m_currTokens = whoseCapital.getResources().getQuantity(Constants.TECH_TOKENS);
                    Change removeTokens = ChangeFactory.changeResourcesChange(whoseCapital, tokens, -m_currTokens);

                    bridge.addChange(removeTokens);
                    if (changeTracker != null)
                        changeTracker.addChange(removeTokens);
                }
            }
        }

        //is this an allied territory
        //revert to original owner if it is, unless they dont own there captital
        PlayerID terrOrigOwner;

    	terrOrigOwner = ta.getOccupiedTerrOf();
        if (terrOrigOwner == null)
            terrOrigOwner = origOwnerTracker.getOriginalOwner(territory);

        PlayerID newOwner;
        if (terrOrigOwner != null && data.getRelationshipTracker().isAllied(terrOrigOwner, id))
        {
        	if (territory.equals(TerritoryAttachment.getCapital(terrOrigOwner, data)))
        		newOwner = terrOrigOwner;
        	else
        	{
        		List<Territory> capitalsListOwned = new ArrayList<Territory>(TerritoryAttachment.getAllCurrentlyOwnedCapitals(terrOrigOwner, data));
            	if (!capitalsListOwned.isEmpty())
            		newOwner = terrOrigOwner;
            	else
            	{
            		List<Territory> capitalsListOriginal = new ArrayList<Territory>(TerritoryAttachment.getAllCapitals(terrOrigOwner, data));
                	Iterator<Territory> iter = capitalsListOriginal.iterator();
                    newOwner = id;
                    while(iter.hasNext())
                    {
                        Territory current = iter.next();
                        if (current.getOwner().equals(PlayerID.NULL_PLAYERID))
                        	newOwner = terrOrigOwner; // if a neutral controls our capital, our territories get liberated (ie: china in ww2v3)
                    }
            	}
        	}
        }
        else
            newOwner = id;

        Change takeOver = ChangeFactory.changeOwner(territory, newOwner);
        bridge.getHistoryWriter().addChildToEvent(takeOver.toString());
        bridge.addChange(takeOver);
        if (changeTracker != null)
        {
            changeTracker.addChange(takeOver);
            changeTracker.addToConquered(territory);
        }

        //destroy any units that should be destroyed on capture
        if(games.strategy.triplea.Properties.getUnitsCanBeDestroyedInsteadOfCaptured(data))
        {
        	CompositeMatch<Unit> enemyToBeDestroyed = new CompositeMatchAnd<Unit>(Matches.enemyUnit(id, data), Matches.UnitDestroyedWhenCapturedBy(id));
            Collection<Unit> destroyed = territory.getUnits().getMatches(enemyToBeDestroyed);
            if (!destroyed.isEmpty())
            {
                Change destroyUnits = ChangeFactory.removeUnits(territory, destroyed);
                bridge.getHistoryWriter().addChildToEvent(id.getName() + " destroys some non-combat units", destroyed);
                bridge.addChange(destroyUnits);
                if (changeTracker != null)
                    changeTracker.addChange(destroyUnits);
            }
        }

        //destroy any capture on entering units, IF the property to destroy them instead of capture is turned on
        if(games.strategy.triplea.Properties.getOnEnteringUnitsDestroyedInsteadOfCaptured(data))
        {
        	Collection<Unit> destroyed = territory.getUnits().getMatches(Matches.UnitCanBeCapturedOnEnteringToInThisTerritory(id, territory, data));
        	if (!destroyed.isEmpty())
        	{
        		Change destroyUnits = ChangeFactory.removeUnits(territory, destroyed);
                bridge.getHistoryWriter().addChildToEvent(id.getName() + " destroys some units instead of capturing them", destroyed);
                bridge.addChange(destroyUnits);
                if (changeTracker != null)
                    changeTracker.addChange(destroyUnits);
        	}
        }

        //destroy any disabled units owned by the enemy that are NOT infrastructure or factories
        if(true)
        {
        	CompositeMatch<Unit> enemyToBeDestroyed = new CompositeMatchAnd<Unit>(Matches.enemyUnit(id, data), Matches.UnitIsDisabled(), Matches.UnitIsInfrastructure.invert(), Matches.UnitIsFactory.invert());
        	Collection<Unit> destroyed = territory.getUnits().getMatches(enemyToBeDestroyed);
            if (!destroyed.isEmpty())
            {
                Change destroyUnits = ChangeFactory.removeUnits(territory, destroyed);
                bridge.getHistoryWriter().addChildToEvent(id.getName() + " destroys some disabled combat units", destroyed);
                bridge.addChange(destroyUnits);
                if (changeTracker != null)
                    changeTracker.addChange(destroyUnits);
            }
        }

        //take over non combatants
        CompositeMatch<Unit> enemyCapturable = new CompositeMatchOr<Unit>(Matches.UnitIsAAOrFactory, Matches.UnitIsInfrastructure); // UnitIsAAOrIsFactoryOrIsInfrastructure
        CompositeMatch<Unit> enemyNonCom = new CompositeMatchAnd<Unit>(Matches.enemyUnit(id, data), enemyCapturable);
        CompositeMatch<Unit> willBeCaptured = new CompositeMatchOr<Unit>(enemyNonCom, Matches.UnitCanBeCapturedOnEnteringToInThisTerritory(id, territory, data));

        Collection<Unit> nonCom = territory.getUnits().getMatches(willBeCaptured);
        Change noMovementChange = ChangeFactory.markNoMovementChange(nonCom);
        bridge.addChange(noMovementChange);
        if(changeTracker != null)
            changeTracker.addChange(noMovementChange);

        //non coms revert to their original owner if once allied
        //unless their capital is not owned
        for (Unit currentUnit : nonCom)
        {
            //TODO: check if following line is necessary
            //PlayerID originalOwner = origOwnerTracker.getOriginalOwner(currentUnit);

            PlayerID terrOwner = newOwner;

            Change capture = ChangeFactory.changeOwner(currentUnit, terrOwner, territory);
            bridge.addChange(capture);
            if (changeTracker != null)
                changeTracker.addChange(capture);
        }

        //Remove any bombing raids against captured territory
        if(Match.someMatch(nonCom, Matches.UnitIsFactoryOrCanBeDamaged))
        {
            Battle bombingBattle = getPendingBattle(territory, true);
            if(bombingBattle != null)
            {
                removeBattle(bombingBattle);
            }
        }

        //is this territory our capitol or a capitol of our ally
        //Also check to make sure playerAttachment even HAS a capital to fix abend
        if (terrOrigOwner != null && ta.getCapital() != null && TerritoryAttachment.getCapital(terrOrigOwner, data).equals(territory)
                && data.getRelationshipTracker().isAllied(terrOrigOwner, id))
        {
            //if it is give it back to the original owner
            Collection<Territory> originallyOwned = origOwnerTracker.getOriginallyOwned(data, terrOrigOwner);

            // necessary as Matches.IsTerritory is a Match<Object> and alliedOccupiedTerritories is used as Match<Territory> later
            @SuppressWarnings("rawtypes")
            CompositeMatch alliedOccupiedTerritories = new CompositeMatchAnd();
            alliedOccupiedTerritories.add(Matches.IsTerritory);
            alliedOccupiedTerritories.add(Matches.isTerritoryAllied(terrOrigOwner, data));
            List<Territory> friendlyTerritories = Match.getMatches(originallyOwned, alliedOccupiedTerritories);

            //give back the factories as well.
            for (Territory item : friendlyTerritories)
            {
                if (item.getOwner() == terrOrigOwner)
                    continue;
                Change takeOverFriendlyTerritories = ChangeFactory.changeOwner(item, terrOrigOwner);
                bridge.addChange(takeOverFriendlyTerritories);
                bridge.getHistoryWriter().addChildToEvent(takeOverFriendlyTerritories.toString());
                if (changeTracker != null)
                    changeTracker.addChange(takeOverFriendlyTerritories);
                // TODO: do we need to add in infrastructure here?
                Collection<Unit> units = Match.getMatches(item.getUnits().getUnits(), Matches.UnitIsAAOrIsFactoryOrIsInfrastructure);
                if (!units.isEmpty())
                {
                    Change takeOverNonComUnits = ChangeFactory.changeOwner(units, terrOrigOwner, territory);
                    bridge.addChange(takeOverNonComUnits);
                    if (changeTracker != null)
                        changeTracker.addChange(takeOverNonComUnits);
                }
            }
        }

        //say they were in combat
        //if the territory being taken over is water, then do not say any land units were in combat (they may want to unload from the transport and attack)
        if (Matches.TerritoryIsWater.match(territory))
        	arrivingUnits.removeAll(Match.getMatches(arrivingUnits, Matches.UnitIsLand));

        markWasInCombat(arrivingUnits, bridge, changeTracker);
    }

    private Change addMustFightBattleChange(Route route, Collection<Unit> units, PlayerID id, GameData data)
    {
        //it is possible to add a battle with a route that is just
        //the start territory, ie the units did not move into the country
        //they were there to start with
        //this happens when you have submerged subs emerging
        Territory site = route.getEnd();
        if (site == null)
            site = route.getStart();

        //this will be taken care of by the non fighting battle
        if (!Matches.territoryHasEnemyUnits(id, data).match(site))
            return ChangeFactory.EMPTY_CHANGE;

        //if just an enemy factory &/or AA then no battle
        Collection<Unit> enemyUnits = Match.getMatches(site.getUnits().getUnits(), Matches.enemyUnit(id, data));
        if (route.getEnd() != null && Match.allMatch(enemyUnits, Matches.UnitIsAAOrIsFactoryOrIsInfrastructure))
            return ChangeFactory.EMPTY_CHANGE;

        Battle battle = getPendingBattle(site, false);
        //If there are no pending battles- add one for units already in the combat zone
        if (battle == null)
        {
        	battle = new MustFightBattle(site, id, data, this);
        	m_pendingBattles.add(battle);
        }

        //Add the units that moved into the battle
        Change change = battle.addAttackChange(route, units);

        //make amphibious assaults dependent on possible naval invasions

        //its only a dependency if we are unloading
        Battle precede = getDependentAmphibiousAssault(route);
        if (precede != null && Match.someMatch(units, Matches.UnitIsLand))
        {
            addDependency(battle, precede);
        }

        //dont let land battles in the same territory occur before bombing
        // battles
        Battle bombing = getPendingBattle(route.getEnd(), true);
        if (bombing != null)
            addDependency(battle, bombing);
        return change;
    }

    private Battle getDependentAmphibiousAssault(Route route)
    {
        if (!MoveValidator.isUnload(route))
            return null;
        return getPendingBattle(route.getStart(), false);
    }

    public Battle getPendingBattle(Territory t, boolean bombing)
    {
        Iterator<Battle> iter = m_pendingBattles.iterator();
        while (iter.hasNext())
        {
            Battle battle = iter.next();
            if (battle.getTerritory().equals(t) && battle.isBombingRun() == bombing)
                return battle;
        }
        return null;
    }

    /**
     * @param bombing whether only battles where there is bombing
     * @return a collection of territories where battles are pending
     */
    public Collection<Territory> getPendingBattleSites(boolean bombing)
    {
        Collection<Territory> battles = new ArrayList<Territory>(m_pendingBattles.size());
        Iterator<Battle> iter = m_pendingBattles.iterator();
        while (iter.hasNext())
        {
            Battle battle = iter.next();
            if (!battle.isEmpty() && battle.isBombingRun() == bombing)
                battles.add(battle.getTerritory());

        }
        return battles;
    }

    /**
     * @param blocked the battle that is blocked
     * @return the battle that must occur before dependent can occur
     */
    public Collection<Battle> getDependentOn(Battle blocked)
    {
        Collection<Battle> dependent = m_dependencies.get(blocked);

        if (dependent == null)
            return Collections.emptyList();

        return Match.getMatches(dependent, new InverseMatch<Battle>(Matches.BattleIsEmpty));
    }

    /**
     * @param blocking the battle that is blocking the other battles
     * @return the battles that cannot occur until the given battle occurs
     */
    public Collection<Battle> getBlocked(Battle blocking)
    {
        Iterator<Battle> iter = m_dependencies.keySet().iterator();
        Collection<Battle> allBlocked = new ArrayList<Battle>();
        while (iter.hasNext())
        {
            Battle current = iter.next();
            Collection<Battle> currentBlockedBy = getDependentOn(current);
            if (currentBlockedBy.contains(blocking))
                allBlocked.add(current);
        }
        return allBlocked;
    }

    private void addDependency(Battle blocked, Battle blocking)
    {
        if (m_dependencies.get(blocked) == null)
        {
            m_dependencies.put(blocked, new HashSet<Battle>());
        }
        m_dependencies.get(blocked).add(blocking);
    }

    private void removeDependency(Battle blocked, Battle blocking)
    {
        Collection<Battle> dependencies = m_dependencies.get(blocked);
        dependencies.remove(blocking);
        if (dependencies.isEmpty())
        {
            m_dependencies.remove(blocked);
        }
    }

    public void removeBattle(Battle battle)
    {
    	if (battle != null)
    	{
    		Iterator<Battle> blocked = getBlocked(battle).iterator();
    		while (blocked.hasNext())
    		{
    			Battle current = blocked.next();
    			removeDependency(current, battle);
    		}

    		m_pendingBattles.remove(battle);
    		m_foughBattles.add(battle.getTerritory());
        }
    }

    /**
     * Marks the set of territories as having been the source of a naval
     * bombardment.
     * @param territories a collection of territories
     */
    public void addPreviouslyNavalBombardmentSource(Collection<Territory> territories)
    {
        m_bombardedFromTerritories.addAll(territories);
    }

    public boolean wasNavalBombardmentSource(Territory territory)
    {
        return m_bombardedFromTerritories.contains(territory);
    }

    private boolean isPacificTheater(GameData data)
    {
        return data.getProperties().get(Constants.PACIFIC_THEATER, false);
    }

    public void clear()
    {
        m_bombardedFromTerritories.clear();
        m_pendingBattles.clear();
        m_blitzed.clear();
        m_foughBattles.clear();
        m_conquered.clear();
        m_dependencies.clear();
    }

    public String toString()
    {
        return "BattleTracker:" + "\n" + "Conquered:" + m_conquered + "\n" + "Blitzed:" + m_blitzed + "\n" + "Fought:" + m_foughBattles + "\n"
                + "Pending:" + m_pendingBattles;
    }

    /*
     //TODO: never used, should it be removed?
    private void addNeutralBattle(Route route, Collection<Unit> units, final PlayerID id, final GameData data, IDelegateBridge bridge,
            UndoableMove changeTracker)
    {
        //TODO check for pre existing battles at the sight
        //here and in empty battle

        Collection<Territory> neutral = route.getMatches(Matches.TerritoryIsNeutral);
        neutral = Match.getMatches(neutral, Matches.TerritoryIsEmpty);
        //deal with the end seperately
        neutral.remove(route.getEnd());

        m_conquered.addAll(neutral);

        Iterator iter = neutral.iterator();
        while (iter.hasNext())
        {
            Territory current = (Territory) iter.next();
            takeOver(current, id, bridge, data, changeTracker, units);
        }

        //deal with end territory, may be the case that
        //a naval battle must precede there
        //Also check if there are only factory/AA units left in the neutral territory.
        Collection<Unit> endUnits = route.getEnd().getUnits().getUnits();
        if (Matches.TerritoryIsNeutral.match(route.getEnd()) && (Matches.TerritoryIsEmpty.match(route.getEnd()) ||
                Match.allMatch(endUnits, Matches.UnitIsAAOrIsFactoryOrIsInfrastructure)))
        {
            Battle precede = getDependentAmphibiousAssault(route);
            if (precede == null)
            {
                m_conquered.add(route.getEnd());
                takeOver(route.getEnd(), id, bridge, data, changeTracker, units);
            } else
            {
                Battle nonFight = getPendingBattle(route.getEnd(), false);
                if (nonFight == null)
                {
                    nonFight = new NonFightingBattle(route.getEnd(), id, this, true, data);
                    m_pendingBattles.add(nonFight);
                }

                Change change = nonFight.addAttackChange(route, units);
                bridge.addChange(change);
                if(changeTracker != null)
                {
                    changeTracker.addChange(change);
                }
                addDependency(nonFight, precede);
            }
        }
    }
*/
}
