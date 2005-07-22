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

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.engine.framework.GameObjectStreamFactory;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attatchments.TerritoryAttatchment;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.util.*;

import java.io.*;
import java.util.*;

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
public abstract class AbstractPlaceDelegate implements ISaveableDelegate,
        IAbstractPlaceDelegate
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
    
    /**
     * 
     * only for testing. Shouldonly be called by unit tests
     */
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

        performPlace(units, at, m_player);
        return null;
    }

    public PlaceableUnits getPlaceableUnits(Collection<Unit> units, Territory to)
    {
        String error = canProduce(to, units, m_player);
        if (error != null)
            return new PlaceableUnits(error);

        Collection<Unit> placeableUnits = getUnitsToBePlaced(to, units, m_player);
        int maxUnits = getMaxUnitsToBePlaced(to, m_player);
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
    String playerHasEnoughUnits(Collection units, Territory at, PlayerID player)
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
    private String validateNewAirCanLandOnNewCarriers(Territory to,
            Collection units)
    {
        int cost = MoveValidator.carrierCost(units);
        int capacity = MoveValidator.carrierCapacity(units);

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

        if (to.isWater())
        {
            Territory producer = getProducer(to, player);
            String canLand = validateNewAirCanLandOnNewCarriers(
                    producer, units);
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
        if (canProduceFightersOnCarriers()
                && Match.someMatch(allProducedUnits, Matches.UnitIsCarrier))
        {
            CompositeMatch<Unit> airThatCanLandOnCarrier = new CompositeMatchAnd<Unit>();
            airThatCanLandOnCarrier.add(Matches.UnitIsAir);
            airThatCanLandOnCarrier.add(Matches.UnitCanLandOnCarrier);

            placeableUnits.addAll(Match.getMatches(units,
                    airThatCanLandOnCarrier));
        }

        if (!isFourthEdition()
                && to.getUnits().someMatch(Matches.enemyUnit(player, m_data)))
            return null;

        return placeableUnits;
    }

    protected Collection<Unit> getUnitsToBePlacedLand(Territory to, Collection<Unit> units,
            PlayerID player)
    {
        Collection<Unit> placeableUnits = new ArrayList<Unit>();

        if (hasFactory(to))
        {
            //make sure only 1 AA in territory for classic
            if (isFourthEdition())
            {
                placeableUnits
                        .addAll(Match.getMatches(units, Matches.UnitIsAA));
            } else
            {
                //allow 1 AA to be placed if none already exists
                if (!to.getUnits().someMatch(Matches.UnitIsAA))
                    placeableUnits.addAll(Match.getNMatches(units, 1,
                            Matches.UnitIsAA));
            }

            CompositeMatch<Unit> groundUnits = new CompositeMatchAnd<Unit>();
            groundUnits.add(Matches.UnitIsLand);
            groundUnits.add(new InverseMatch<Unit>(Matches.UnitIsAAOrFactory));
            placeableUnits.addAll(Match.getMatches(units, groundUnits));
            placeableUnits.addAll(Match.getMatches(units, Matches.UnitIsAir));

        }

        //make sure only max Factories
        if (Match.countMatches(units, Matches.UnitIsFactory) >= 1)
        {
            //if its an original factory then unlimited production
            TerritoryAttatchment ta = TerritoryAttatchment.get(to);

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
    protected int getMaxUnitsToBePlaced(Territory to, PlayerID player)
    {
        Territory producer = getProducer(to, player);

        //if its an original factory then unlimited production
        TerritoryAttatchment ta = TerritoryAttatchment.get(producer);
        Collection factoryUnits = producer.getUnits().getMatches(
                Matches.UnitIsFactory);
        boolean originalFactory = ta.isOriginalFactory();
        boolean playerIsOriginalOwner = factoryUnits.size() > 0 ? m_player
                .equals(getOriginalFactoryOwner(producer)) : false;

        if (originalFactory && playerIsOriginalOwner)
            return -1;

        //a factory can produce the same number of units as the number of ipcs
        // the territroy generates each turn
        int unitCount = getAlreadyProduced(producer).size();
        int production = getProduction(producer);
        if (production == 0)
            production = 1; //if it has a factory then it can produce at least
        // 1
        return production - unitCount;
    }

    /**
     * @return gets the production of the territory, ignores wether the
     *         territory was an original factory
     */
    protected int getProduction(Territory territory)
    {

        throw new UnsupportedOperationException("Not implemented");
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
        if (wasConquered(producer))
            return producer.getName() + " was conquered this turn and cannot produce till next turn";
                   

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
                   

        return null;
    }

    /**
     * Test whether or not the territory has the factory resources to support
     * the placement. AlreadyProduced maps territory->units already produced
     * this turn by that territory.
     */
    protected String checkProduction(Territory to, Collection units,
            PlayerID player)
    {
        Territory producer = getProducer(to, player);

        //if its an original factory then unlimited production
        TerritoryAttatchment ta = TerritoryAttatchment.get(producer);

        //4th edition, you cant place factories in territories with no
        // production
        if (isFourthEdition() && ta.getProduction() == 0)
        {
            return "Cant place factory, that territory cant produce any units";
                   
        }

        int maxUnitsToBePlaced = getMaxUnitsToBePlaced(to, player);
        if ((maxUnitsToBePlaced != -1) && (maxUnitsToBePlaced < units.size()))
            return "Cannot place " + units.size()
                    + " more units in " + producer.getName();

        return null;
    }

    protected boolean isFourthEdition()
    {

        return m_data.getProperties().get(Constants.FOURTH_EDITION, false);
    }

    private boolean wasConquered(Territory t)
    {

        BattleTracker tracker = DelegateFinder.battleDelegate(m_data)
                .getBattleTracker();
        return tracker.wasConquered(t);
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
        TerritoryAttatchment t1a = TerritoryAttatchment.get(t1);
        if (t1a.isOriginalFactory() && isOriginalOwner(t1, player))
            return t1;
        TerritoryAttatchment t2a = TerritoryAttatchment.get(t2);
        if (t2a.isOriginalFactory() && isOriginalOwner(t2, player))
            return t2;

        //which can produce the most
        if (getProduction(t1) - getAlreadyProduced(t1).size() > getProduction(t2)
                - getAlreadyProduced(t1).size())
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
        Iterator iter = m_data.getMap().getNeighbors(to).iterator();
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

        Collection factoryUnits = territory.getUnits().getMatches(
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
        
        Collection factoryAndAA = Match.getMatches(units,
                Matches.UnitIsAAOrFactory);
        DelegateFinder.battleDelegate(m_data).getOriginalOwnerTracker()
                .addOriginalOwner(factoryAndAA, m_player);

        String transcriptText = MyFormatter.unitsToTextNoOwner(units)
                + " placed in " + at.getName();
        m_bridge.getHistoryWriter().startEvent(transcriptText);
        m_bridge.getHistoryWriter().setRenderingData(units);

        Change remove = ChangeFactory.removeUnits(player, units);
        Change place = ChangeFactory.addUnits(at, units);

        CompositeChange change = new CompositeChange();
        change.add(remove);
        change.add(place);

        //can we move planes to land there
        moveAirOntoNewCarriers(at, units, player, change);
        m_bridge.addChange(change);
        m_placements.add(new UndoPlace(m_data, this, change));

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

    private void moveAirOntoNewCarriers(Territory territory, Collection<Unit> units,
            PlayerID player, CompositeChange placeChange)
    {
        //not water, dont bother
        if (!territory.isWater())
            return;
        //not enabled
        if (!m_data.getProperties().get(
                Constants.CAN_PRODUCE_FIGHTERS_ON_CARRIERS, false))
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

        Collection neighbors = m_data.getMap().getNeighbors(territory, 1);
        Iterator iter = neighbors.iterator();
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
        if (!units.isEmpty())
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
    }

    /**
     * Can the delegate be saved at the current time.
     * 
     * @arg message, a String[] of size 1, hack to pass an error message back.
     */
    public boolean canSave(String[] message)
    {

        return true;
    }

    /**
     * Returns the state of the Delegate.
     */
    public Serializable saveState()
    {

        PlaceState state = new PlaceState();
        state.m_produced = m_produced;
        return state;
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
    public void loadState(Serializable aState)
    {

        PlaceState state = (PlaceState) aState;
        m_produced = state.m_produced;
    }

    protected GameData getData()
    {

        return m_data;
    }
}

class UndoPlace
{

    private CompositeChange m_change;

    private byte[] m_data;

    public UndoPlace(GameData data, AbstractPlaceDelegate delegate,
            CompositeChange change)
    {

        m_change = change;
        try
        {
            //capture the save state of the move and save delegates
            GameObjectStreamFactory factory = new GameObjectStreamFactory(data);

            ByteArrayOutputStream sink = new ByteArrayOutputStream(2000);
            ObjectOutputStream out = factory.create(sink);

            out.writeObject(delegate.saveState());
            out.flush();
            out.close();

            m_data = sink.toByteArray();
        } catch (IOException ex)
        {
            ex.printStackTrace();
            throw new IllegalStateException(ex.getMessage());
        }
    }

    public void undo(GameData data, IDelegateBridge bridge,
            AbstractPlaceDelegate delegate)
    {

        try
        {
            GameObjectStreamFactory factory = new GameObjectStreamFactory(data);
            ObjectInputStream in = factory.create(new ByteArrayInputStream(
                    m_data));
            PlaceState placeState = (PlaceState) in.readObject();
            delegate.loadState(placeState);

            bridge.getHistoryWriter().startEvent(
                    bridge.getPlayerID().getName()
                            + " undoes his last placement.");

            //undo any changes to the game data
            bridge.addChange(m_change.invert());

        } catch (ClassNotFoundException ex)
        {
            ex.printStackTrace();
        } catch (IOException ex)
        {
            ex.printStackTrace();
        }

    }

}

class PlaceState implements Serializable
{

    public Map<Territory, Collection<Unit>> m_produced;
}
