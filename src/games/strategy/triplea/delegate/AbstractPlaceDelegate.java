/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/**
 * PlaceDelegate.java
 * 
 * Overriding
 * 
 * Subclasses can over ride one of these methods to change the way this class
 * works. playerHasEnoughUnits(...), canProduce(...), canUnitsBePlaced(...)
 * 
 * For a simpler way you can override getProduction(...) which is called in the
 * default canProduce(...) method
 * 
 * Created on November 2, 2001, 12:29 PM
 */

package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attatchments.RulesAttachment;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 
 * Logic for placing units.
 * <p>
 * 
 * @author Sean Bridges
 * @version 1.0
 * 
 * Known limitations.
 * 
 * Doesnt take into account limits on number of factories that can be produced.
 * 
 * The situation where one has two non original factories a,b each with
 * production 2. If sea zone e neighbors a,b and sea zone f neighbors b. Then
 * producing 2 in e could make it such that you cannot produce in f. The reason
 * is that the production in e could be assigned to the factory in b, leaving no
 * capacity to produce in f. If anyone ever accidently runs into this situation
 * then they can undo the production, produce in f first, and then produce in e.
 */
public abstract class AbstractPlaceDelegate implements IDelegate, IAbstractPlaceDelegate
{

    private String m_name;

    private String m_displayName;

    private IDelegateBridge m_bridge;

    //maps Territory-> Collection of units
    protected Map<Territory, Collection<Unit>> m_produced = new HashMap<Territory, Collection<Unit>>();

    private PlayerID m_player;

    //a list of CompositeChanges
    private List<UndoPlace> m_placements = new ArrayList<UndoPlace>();

    protected GameData m_data; // protected to allow access by subclasses

    public void initialize(String name)
    {

        initialize(name, name);
    }

    public void initialize(String name, String displayName)
    {

        m_name = name;
        m_displayName = displayName;
    }

    private Collection<Unit> getAlreadyProduced(Territory t)
    {

        if (m_produced.containsKey(t))
            return m_produced.get(t);
        return new ArrayList<Unit>();
    }

    /**
     * Called before the delegate will run.
     */
    public void start(IDelegateBridge aBridge, GameData gameData)
    {

        m_bridge = aBridge;
        m_data = gameData;
        m_player = aBridge.getPlayerID();
    }

    public String getName()
    {

        return m_name;
    }

    public String getDisplayName()
    {

        return m_displayName;
    }

    public int getPlacementsMade()
    {
        return m_placements.size();
    }
    

    void setProduced(Map<Territory, Collection<Unit>> produced)
    {

        m_produced = produced;
    }

    public void undoLastPlacement()
    {
        int lastChange = m_placements.size() - 1;
        UndoPlace undoPlace = m_placements.get(lastChange);
        undoPlace.undo(m_data, m_bridge, this);
        m_placements.remove(lastChange);
    }

    
    public String placeUnits(Collection<Unit> units, Territory at)
    {
        String error = isValidPlacement(units, at, m_player);
        if (error != null)
            return error;

        performPlace(new ArrayList<Unit>(units), at, m_player);
        return null;
    }

    public PlaceableUnits getPlaceableUnits(Collection<Unit> units, Territory to)
    {
        String error = canProduce(to, units, m_player);
        if (error != null)
            return new PlaceableUnits(error);

        Collection<Unit> placeableUnits = getUnitsToBePlaced(to, units, m_player);
        int maxUnits = getMaxUnitsToBePlaced(units, to, m_player);
        return new PlaceableUnits(placeableUnits, maxUnits);
    }

    /**
     * Subclasses can over ride this to change the way placements are made.
     * 
     * @return null if placement is valid
     */
    protected String isValidPlacement(Collection<Unit> units, Territory at, PlayerID player)
    {
        //do we hold enough units
        String error = playerHasEnoughUnits(units, at, player);
        if (error != null)
            return error;

        //can we produce that much
        error = canProduce(at, units, player);
        if (error != null)
            return error;

        //can we produce that much
        error = checkProduction(at, units, player);
        if (error != null)
            return error;

        //can we place it
        error = canUnitsBePlaced(at, units, player);
        if (error != null)
            return error;

        return null;
    }

    /**
     * Make sure the player has enough in hand to place the units.
     */
    String playerHasEnoughUnits(Collection<Unit> units, Territory at, PlayerID player)
    {
        //make sure the player has enough units in hand to place
        if (!player.getUnits().getUnits().containsAll(units))
            return "Not enough units";
        return null;
    }

    private boolean canProduceFightersOnCarriers()
    {
        return m_data.getProperties().get(
                Constants.CAN_PRODUCE_FIGHTERS_ON_CARRIERS, false);
    }

    /**
     * The rule is that new fighters can be produced on new carriers. This does
     * not allow for fighters to be produced on old carriers.
     */
    private String validateNewAirCanLandOnCarriers(Territory to, Collection<Unit> units)
    {
        int cost = MoveValidator.carrierCost(units);
        int capacity = MoveValidator.carrierCapacity(units);
        capacity += MoveValidator.carrierCapacity(to.getUnits().getUnits());

        if (cost > capacity)
            return "Not enough new carriers to land all the fighters";

        return null;
    }

    public String canUnitsBePlaced(Territory to, Collection<Unit> units,
            PlayerID player)
    {

        Collection<Unit> allowedUnits = getUnitsToBePlaced(to, units, player);
        if (allowedUnits == null || !allowedUnits.containsAll(units))
        {
            return "Cannot place these units in " + to;
        }
//TODO Comco incorporate this into a rule (place_only_in_capital_restricted)
        // can only place chinese units in pacific edition on capitol
        if(m_data.getProperties().get(Constants.PACIFIC_EDITION, false))
            if(TerritoryAttachment.getCapital(player, m_data) != to && player.getName().equals("Chinese"))
                return "Cannot place these units outside of the capital";

        

        
        if (to.isWater())
        {
            String canLand = validateNewAirCanLandOnCarriers(to, units);
            if (canLand != null)
                return canLand;
        } else
        {
            //make sure we own the territory
            if (!to.getOwner().equals(player))
                return "You don't own " + to.getName();
            //make sure all units are land
            if (!Match.allMatch(units, Matches.UnitIsNotSea))
                return "Cant place sea units on land";
        }

        return null;
    }

    private Collection<Unit> getUnitsToBePlaced(Territory to, Collection<Unit> units,
            PlayerID player)
    {
        if (to.isWater())
        {
            return getUnitsToBePlacedSea(to, units, player);
        } else
        //if land
        {
            return getUnitsToBePlacedLand(to, units, player);
        }
    }

    protected Collection<Unit> getUnitsToBePlacedSea(Territory to, Collection<Unit> units,
            PlayerID player)
    {

        Collection<Unit> placeableUnits = new ArrayList<Unit>();

        //Land units wont do
        placeableUnits.addAll(Match.getMatches(units, Matches.UnitIsSea));
        Territory producer = getProducer(to, player);
        Collection<Unit> allProducedUnits = new ArrayList<Unit>(units);
        allProducedUnits.addAll(getAlreadyProduced(producer));
        if (canProduceFightersOnCarriers() &&
                (Match.someMatch(allProducedUnits, Matches.UnitIsCarrier) || Match.someMatch(to.getUnits().getUnits(), Matches.UnitIsCarrier)))
        {
            CompositeMatch<Unit> airThatCanLandOnCarrier = new CompositeMatchAnd<Unit>();
            airThatCanLandOnCarrier.add(Matches.UnitIsAir);
            airThatCanLandOnCarrier.add(Matches.UnitCanLandOnCarrier);

            placeableUnits.addAll(Match.getMatches(units,
                    airThatCanLandOnCarrier));
        }

        if ((!isFourthEdition() && !isUnitPlacementInEnemySeas())
                && to.getUnits().someMatch(Matches.enemyUnit(player, m_data)))
            return null;

        return placeableUnits;
    }

    protected Collection<Unit> getUnitsToBePlacedLand(Territory to, Collection<Unit> units,
            PlayerID player)
    {    	
        Collection<Unit> placeableUnits = new ArrayList<Unit>();

        if (hasFactory(to) || isPlayerAllowedToPlaceAnywhere(player))
        {
        
            //make sure only 1 AA in territory for classic
            if (isFourthEdition() || isAnniversaryEdition())
            {
                placeableUnits.addAll(Match.getMatches(units, Matches.UnitIsAA));
            } else
            {
                //allow 1 AA to be placed if none already exists
                if (!to.getUnits().someMatch(Matches.UnitIsAA))
                    placeableUnits.addAll(Match.getNMatches(units, 1, Matches.UnitIsAA));
            }

            CompositeMatch<Unit> groundUnits = new CompositeMatchAnd<Unit>();
            groundUnits.add(Matches.UnitIsLand);
            groundUnits.add(new InverseMatch<Unit>(Matches.UnitIsAAOrFactory));
            placeableUnits.addAll(Match.getMatches(units, groundUnits));
            placeableUnits.addAll(Match.getMatches(units, Matches.UnitIsAir));

        }
        //Were any factories built
        if (Match.countMatches(units, Matches.UnitIsFactory) >= 1)
        {
            //if its an original factory then unlimited production
            TerritoryAttachment ta = TerritoryAttachment.get(to);

            //TODO COMCO extract rule from 4th ed into individual rule
            //4th edition, you cant place factories in territories with no
            // production
            if (!(isFourthEdition() && ta.getProduction() == 0))
            {
                //this is how many factories exist now
                int factoryCount = to.getUnits().getMatches(
                        Matches.UnitIsFactory).size();

                //max factories allowed
                int maxFactory = games.strategy.triplea.Properties
                        .getFactoriesPerCountry(m_data);

                placeableUnits.addAll(Match.getNMatches(units, maxFactory
                        - factoryCount, Matches.UnitIsFactory));
            }
        }

        return placeableUnits;
    }

    // Returns -1 if can place unlimited units
    protected int getMaxUnitsToBePlaced(Collection<Unit> units, Territory to, PlayerID player)
    {
        Territory producer = getProducer(to, player);
        
        if(producer == null)
        	return 0;

        //if its an original factory then unlimited production
        TerritoryAttachment ta = TerritoryAttachment.get(producer);
        Collection<Unit> factoryUnits = producer.getUnits().getMatches(
                Matches.UnitIsFactory);
        boolean limitSBRDamageToUnitProd = isSBRAffectsUnitProduction();
        boolean placementRestrictedByFactory = isPlacementRestrictedByFactory();
        boolean unitPlacementPerTerritoryRestricted = isUnitPlacementPerTerritoryRestricted();
        boolean originalFactory = ta.isOriginalFactory();
        boolean playerIsOriginalOwner = factoryUnits.size() > 0 ? m_player
                .equals(getOriginalFactoryOwner(producer)) : false;


        if (originalFactory && playerIsOriginalOwner && !placementRestrictedByFactory && !unitPlacementPerTerritoryRestricted)
            return -1;
        
    	//Restricts the STARTING number of units in a territory
        if (unitPlacementPerTerritoryRestricted)
        {
        	RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTATCHMENT_NAME);
        	if(ra != null && ra.getPlacementPerTerritory() > 0)
        	{
        		int allowedPlacement = ra.getPlacementPerTerritory();
        		int ownedUnitsInTerritory = Match.countMatches(to.getUnits().getUnits(), Matches.unitIsOwnedBy(player));
        		
        		if (ownedUnitsInTerritory >= allowedPlacement)
        			return 0;
        		
        		return -1;
        	}        		
        }
        //TODO comco look here
        //a factory can produce the same number of units as the number of ipcs
        // the territroy generates each turn
        int unitCount = getAlreadyProduced(producer).size();
        int production = 0;
        int territoryValue = getProduction(producer);
        
        if(limitSBRDamageToUnitProd)
        {            
        	production = ta.getUnitProduction();
            //If there's NO factory, allow placement of the factory
            if(production <= 0 && producer.getUnits().getMatches(Matches.UnitIsFactory).size() == 0)
            {
                return 1;
            }
            
            //Increase production if have industrial technology
            if(isIncreasedFactoryProduction(player) && territoryValue > 2)
                production += 2;
            
            //return 0 if less than 0
            if(production < 0)
                return 0;
        }
        else
        {
        	production = territoryValue;
        
        	if (production == 0)
        		production = 1; //if it has a factory then it can produce at least        
        }
        
        return production - unitCount;
    }

    /**
     * @return gets the production of the territory, ignores wether the
     *         territory was an original factory
     */
    protected int getProduction(Territory territory)
    {
        TerritoryAttachment ta = TerritoryAttachment.get(territory);
        if(ta != null)
            return ta.getProduction();
        return 0; 

//        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Test whether or not the territory has the factory resources to support
     * the placement. AlreadyProduced maps territory->units already produced
     * this turn by that territory.
     */
    protected String canProduce(Territory to, Collection<Unit> units,
            PlayerID player)
    {
        Territory producer = getProducer(to, player);
        //the only reason to could be null is if its water and no
        //territories adjacent have factories
        if (producer == null)
            return "No factory adjacent to " + to.getName();

        //make sure the territory wasnt conquered this turn
        if (wasConquered(producer) && !isPlacementAllowedInCapturedTerritory(player))
            return producer.getName() + " was conquered this turn and cannot produce till next turn";
                   
        if(isPlayerAllowedToPlaceAnywhere(player))
        	return null;

        //make sure there is a factory
        if (!hasFactory(producer))
        {
            //check to see if we are producing a factory
            if (Match.someMatch(units, Matches.UnitIsFactory))
                return null;
            else
                return "No Factory in " + producer.getName();
        }

        //check we havent just put a factory there
        if (Match.someMatch(getAlreadyProduced(to), Matches.UnitIsFactory))
            return "Factories cant produce until 1 turn after they are created";
                   
        
        if (to.isWater() && (!isFourthEdition() && !isUnitPlacementInEnemySeas())
                && to.getUnits().someMatch(Matches.enemyUnit(player, m_data)))
            return "Cannot place sea units with enemy naval units";

        return null;
    }

    /**
     * Test whether or not the territory has the factory resources to support
     * the placement. AlreadyProduced maps territory->units already produced
     * this turn by that territory.
     */
    protected String checkProduction(Territory to, Collection<Unit> units,
            PlayerID player)
    {
        Territory producer = getProducer(to, player);
        if(producer == null)
            return "No factory adjacent to " + to.getName();

        //if its an original factory then unlimited production
        TerritoryAttachment ta = TerritoryAttachment.get(producer);

        //4th edition, you cant place factories in territories with no
        // production
        if (isFourthEdition() && ta.getProduction() == 0)
        {
            return "Cant place factory, that territory cant produce any units";                   
        }

        int maxUnitsToBePlaced = getMaxUnitsToBePlaced(units, to, player);
        if ((maxUnitsToBePlaced != -1) && (maxUnitsToBePlaced < units.size()))
            return "Cannot place " + units.size() + " more units in " + producer.getName();

        return null;
    }

    protected boolean isFourthEdition()    
    {
        return games.strategy.triplea.Properties.getFourthEdition(m_data);
    }

    private boolean isAnniversaryEdition()    
    {
        return games.strategy.triplea.Properties.getAnniversaryEdition(m_data);
    }
    
    protected boolean isUnitPlacementInEnemySeas()    
    {
        return games.strategy.triplea.Properties.getUnitPlacementInEnemySeas(m_data);
    }
    
    private boolean wasConquered(Territory t)
    {
        BattleTracker tracker = DelegateFinder.battleDelegate(m_data).getBattleTracker();
        return tracker.wasConquered(t);
    }

    private boolean isPlaceInAnyTerritory()    
    {
        return games.strategy.triplea.Properties.getPlaceInAnyTerritory(m_data);
    }

    private boolean isSBRAffectsUnitProduction()    
    {
        return games.strategy.triplea.Properties.getSBRAffectsUnitProduction(m_data);
    }

    private boolean isPlacementRestrictedByFactory()    
    {
        return games.strategy.triplea.Properties.getPlacementRestrictedByFactory(m_data);
    }
    
    private boolean isUnitPlacementPerTerritoryRestricted()    
    {
        return games.strategy.triplea.Properties.getUnitPlacementPerTerritoryRestricted(m_data);
    }

    private boolean isIncreasedFactoryProduction(PlayerID player)    
    {
        TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTATCHMENT_NAME);
        if(ta == null)
        	return false;
        return ta.hasIncreasedFactoryProduction();
    }
    

    
    /**
     * Returns the better producer of the two territories, either of which can
     * be null.
     */
    private Territory getBetterProducer(Territory t1, Territory t2,
            PlayerID player)
    {
        //anything is better than nothing
        if (t1 == null)
            return t2;
        if (t2 == null)
            return t1;

        //conquered cant produce
        if (wasConquered(t1))
            return t2;
        if (wasConquered(t2))
            return t1;

        //original factories are good
        TerritoryAttachment t1a = TerritoryAttachment.get(t1);
        if (t1a.isOriginalFactory() && isOriginalOwner(t1, player))
            return t1;
        TerritoryAttachment t2a = TerritoryAttachment.get(t2);
        if (t2a.isOriginalFactory() && isOriginalOwner(t2, player))
            return t2;

        //which can produce the most
        if (getProduction(t1) - getAlreadyProduced(t1).size() > getProduction(t2)
                - getAlreadyProduced(t2).size())
            return t1;
        return t2;
    }

    private boolean isOriginalOwner(Territory t, PlayerID id)
    {

        OriginalOwnerTracker tracker = DelegateFinder.battleDelegate(m_data)
                .getOriginalOwnerTracker();
        return tracker.getOriginalOwner(t).equals(id);
    }

    private boolean hasFactory(Territory to)
    {

        return to.getUnits().someMatch(Matches.UnitIsFactory);
    }

    /**
     * Returns the territory that would do the producing if units are to be
     * placed in a given territory. Returns null if no suitable territory could
     * be found.
     */
    private Territory getProducer(Territory to, PlayerID player)
    {

        //if not water then must produce in that territory
        if (!to.isWater())
            return to;

        Territory neighborFactory = null;
        Iterator<Territory> iter = m_data.getMap().getNeighbors(to).iterator();
        while (iter.hasNext())
        {
            Territory current = (Territory) iter.next();
            if (hasFactory(current)
                    && !Match.someMatch(getAlreadyProduced(current),
                            Matches.UnitIsFactory)
                    && current.getOwner().equals(m_player))
            {
                neighborFactory = getBetterProducer(current, neighborFactory,
                        player);
            }
        }
        return neighborFactory;
    }

    /**
     * There must be a factory in the territotory or an illegal state exception
     * will be thrown. return value may be null.
     */
    private PlayerID getOriginalFactoryOwner(Territory territory)
    {

        Collection<Unit> factoryUnits = territory.getUnits().getMatches(
                Matches.UnitIsFactory);
        if (factoryUnits.size() == 0)
            throw new IllegalStateException("No factory in territory:"
                    + territory);

        Unit factory = (Unit) factoryUnits.iterator().next();
        return DelegateFinder.battleDelegate(m_data).getOriginalOwnerTracker()
                .getOriginalOwner(factory);
    }

    private void performPlace(Collection<Unit> units, Territory at, PlayerID player)
    {
        
        Map<Territory, Collection<Unit>> placedBeforeProduce = new HashMap<Territory, Collection<Unit>>(m_produced);
        
        CompositeChange change = new CompositeChange();
        
        Collection<Unit> factoryAndAA = Match.getMatches(units,
                Matches.UnitIsAAOrFactory);
        change.add(DelegateFinder.battleDelegate(m_data).getOriginalOwnerTracker()
                .addOriginalOwnerChange(factoryAndAA, m_player));
       
        String transcriptText = MyFormatter.unitsToTextNoOwner(units)
                + " placed in " + at.getName();
        m_bridge.getHistoryWriter().startEvent(transcriptText);
        m_bridge.getHistoryWriter().setRenderingData(units);

        Change remove = ChangeFactory.removeUnits(player, units);
        Change place = ChangeFactory.addUnits(at, units);

        change.add(remove);
        change.add(place);
        
        if(Match.someMatch(units, Matches.UnitIsFactory))
        {
            Change unitProd = ChangeFactory.changeUnitProduction(at, getProduction(at));
            change.add(unitProd);
        }

        //can we move planes to land there
        
        moveAirOntoNewCarriers(at, units, player, change);
        m_bridge.addChange(change);
        m_placements.add(new UndoPlace(m_data, this, change, placedBeforeProduce));

        Territory producer = getProducer(at, player);
        Collection<Unit> produced = new ArrayList<Unit>();
        produced.addAll(getAlreadyProduced(producer));
        produced.addAll(units);

        m_produced.put(producer, produced);
    }
    
    private ITripleaPlayer getRemotePlayer()
    {
        return (ITripleaPlayer) m_bridge.getRemote();
    }
//TODO Comco here's the spot for special air placement rules
    private void moveAirOntoNewCarriers(Territory territory, Collection<Unit> units,
            PlayerID player, CompositeChange placeChange)
    {
        //not water, dont bother
        if (!territory.isWater())
            return;
        //not enabled
        if (!canProduceFightersOnCarriers())
            return;
        //lhtr rules dont allow this
        if(AirThatCantLandUtil.isLHTRCarrierProduction(getData()))
            return;
        
        if (Match.noneMatch(units, Matches.UnitIsCarrier))
            return;

        //do we have any spare carrier capacity
        int capacity = MoveValidator.carrierCapacity(units);
        //subtract fighters that have already been produced with this carrier
        // this turn.
        capacity -= MoveValidator.carrierCost(units);
        if (capacity <= 0)
            return;

        Collection<Territory> neighbors = m_data.getMap().getNeighbors(territory, 1);
        Iterator<Territory> iter = neighbors.iterator();
        CompositeMatch<Unit> ownedFactories = new CompositeMatchAnd<Unit>(
                Matches.UnitIsFactory, Matches.unitIsOwnedBy(player));
        CompositeMatch<Unit> ownedFighters = new CompositeMatchAnd<Unit>(
                Matches.UnitCanLandOnCarrier, Matches.unitIsOwnedBy(player));

        while (iter.hasNext())
        {
            Territory neighbor = (Territory) iter.next();
            if (neighbor.isWater())
                continue;
            //check to see if we have a factory, only fighters from territories
            // that could
            //have produced the carrier can move there
            if (!neighbor.getUnits().someMatch(ownedFactories))
                continue;
            //are there some fighers there that can be moved?
            if (!neighbor.getUnits().someMatch(ownedFighters))
                continue;
            if (wasConquered(neighbor))
                continue;
            if (Match.someMatch(getAlreadyProduced(neighbor),
                    Matches.UnitIsFactory))
                continue;

            List<Unit> fighters = neighbor.getUnits().getMatches(ownedFighters);
            while (fighters.size() > 0
                    && MoveValidator.carrierCost(fighters) > capacity)
            {
                fighters.remove(0);
            }

            if (fighters.size() == 0)
                continue;

            Collection<Unit> movedFighters = getRemotePlayer().getNumberOfFightersToMoveToNewCarrier(fighters, neighbor);

            Change change = ChangeFactory.moveUnits(neighbor, territory,
                    movedFighters);
            placeChange.add(change);
            m_bridge.getHistoryWriter().addChildToEvent(
                    MyFormatter.unitsToTextNoOwner(movedFighters)
                            + "  moved from " + neighbor.getName() + " to "
                            + territory);

            //only allow 1 movement
            //technically only the territory that produced the
            //carrier should be able to move fighters to the new
            //territory
            break;
        }

    }

    /**
     * Called before the delegate will stop running.
     */
    public void end()
    {

        PlayerID player = m_bridge.getPlayerID();
        //clear all units not placed
        Collection<Unit> units = player.getUnits().getUnits();
        if (!Properties.getUnplacedUnitsLive(m_data) && !units.isEmpty())
        {
            m_bridge.getHistoryWriter().startEvent(
                    MyFormatter.unitsToTextNoOwner(units)
                            + " were produced but were not placed");
            m_bridge.getHistoryWriter().setRenderingData(units);

            Change change = ChangeFactory.removeUnits(player, units);
            m_bridge.addChange(change);
        }

        //reset ourseleves for next turn
        m_produced = new HashMap<Territory, Collection<Unit>>();
        m_placements.clear();
        
        //only for lhtr rules
        new AirThatCantLandUtil(m_data, m_bridge).removeAirThatCantLand(m_player, false);
    }
    
    /**
     * Get what air units must move before the end of the players turn
     * @return a list of Territories with air units that must move
     */
    public Collection<Territory> getTerritoriesWhereAirCantLand()
    {
        return new AirThatCantLandUtil(m_data, m_bridge).getTerritoriesWhereAirCantLand(m_player);
    }

    /**
     * Returns the state of the Delegate.
     */
    public final Serializable saveState()
    {

        PlaceState state = new PlaceState();
        state.m_produced = m_produced;
        state.m_placements = m_placements;
        return state;
    }
    
    private boolean isPlayerAllowedToPlaceAnywhere(PlayerID player)
    {
    	if(isPlaceInAnyTerritory())
    	{
    		RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTATCHMENT_NAME);
        	if(ra != null && ra.getPlacementAnyTerritory())
        	{
        		return true;
        	}      
    	}
    
    	return false;
    }
    

    private boolean isPlacementAllowedInCapturedTerritory(PlayerID player)
    {
            RulesAttachment ra = (RulesAttachment) player.getAttachment(Constants.RULES_ATTATCHMENT_NAME);
            if(ra != null && ra.getPlacementCapturedTerritory())
            {
                return true;
            }      
    
        return false;
    }

    /*
     * @see games.strategy.engine.delegate.IDelegate#getRemoteType()
     */
    public Class<? extends IRemote> getRemoteType()
    {
        return IAbstractPlaceDelegate.class;
    }

    /**
     * Loads the delegates state
     */
    public final void loadState(Serializable aState)
    {

        PlaceState state = (PlaceState) aState;
        m_produced = state.m_produced;
        m_placements = state.m_placements;
    }

    protected GameData getData()
    {

        return m_data;
    }
    
}

class UndoPlace implements Serializable
{

    private final CompositeChange m_change;
    private final Map<Territory, Collection<Unit>> m_produced;
    
    public UndoPlace(GameData data, AbstractPlaceDelegate delegate,
            CompositeChange change, Map<Territory, Collection<Unit>> produced)
    {
        m_change = change;
        m_produced = new HashMap<Territory, Collection<Unit>>(produced);      
    }

    public void undo(GameData data, IDelegateBridge bridge,
            AbstractPlaceDelegate delegate)
    {
        //undo any changes to the game data
        bridge.addChange(m_change.invert());
        delegate.setProduced(new HashMap<Territory, Collection<Unit>>(m_produced));
    }

}

class PlaceState implements Serializable
{

    public Map<Territory, Collection<Unit>> m_produced;
    public List<UndoPlace> m_placements;
}
