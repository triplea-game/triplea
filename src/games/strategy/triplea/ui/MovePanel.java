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
 * MovePanel.java
 *
 * Created on December 4, 2001, 6:59 PM
 */

package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.EditDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveDelegate;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.UnitComparator;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.delegate.dataObjects.MustMoveWithDetails;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.IntegerMap;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;
import games.strategy.util.Util;

import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

/**
 * 
 * @author Sean Bridges, modified by Erik von der Osten
 * @version 1.1
 */
@SuppressWarnings("serial")
public class MovePanel extends AbstractMovePanel
{
    
    private static final int s_defaultMinTransportCost = 5;
    /*
     * @param s_deselectNumber remove 1/s_deselectNumber of total units (useful for splitting large armies)
     */
    private static final int s_deselectNumber = 10;
    // access only through getter and setter!
    private Territory m_firstSelectedTerritory;
    private Territory m_selectedEndpointTerritory;
    private Territory m_mouseCurrentTerritory;
    private List<Territory> m_forced;
    private boolean m_nonCombat;
    private Point m_mouseSelectedPoint;
    private Point m_mouseCurrentPoint;
    private Point m_mouseLastUpdatePoint;
    
    // use a LinkedHashSet because we want to know the order
    private final Set<Unit> m_selectedUnits = new LinkedHashSet<Unit>();
    
    private static Map<Unit, Collection<Unit>> s_dependentUnits = new HashMap<Unit, Collection<Unit>>();
    
    // the must move with details for the currently selected territory
    // note this is kept in sync because we do not modify m_selectedTerritory directly
    // instead we only do so through the private setter
    private MustMoveWithDetails m_mustMoveWithDetails = null;
    
    // cache this so we can update it only when territory/units change
    private List<Unit> m_unitsThatCanMoveOnRoute;
    
    private Image m_currentCursorImage;
    private TransportTracker m_transportTracker = null;
    /** Creates new MovePanel */
    public MovePanel(GameData data, MapPanel map, TripleAFrame frame)
    {
        super(data, map, frame);

        m_undoableMovesPanel = new UndoableMovesPanel(data, this);
        m_mouseCurrentTerritory = null;
        m_unitsThatCanMoveOnRoute = Collections.emptyList();
        
        m_currentCursorImage = null;
    }
    
    public static Map<Unit, Collection<Unit>> getDependents()
    {
        return s_dependentUnits;
    }
    
    public static void clearDependents(Collection<Unit> units)
    {
        for (Unit unit : units)
        {
            if (Matches.UnitIsAirTransport.match(unit))
            {
                s_dependentUnits.remove(unit);
            }
        }
    }
    
    private PlayerID getUnitOwner(Collection<Unit> units)
    {
        if (EditDelegate.getEditMode(getData()) && units != null && !units.isEmpty())
            return units.iterator().next().getOwner();
        else
            return getCurrentPlayer();
    }
    
    /**
     * Sort the specified units in preferred movement or unload order.
     */
    private void sortUnitsToMove(final List<Unit> units, final Route route)
    {
        if (units.isEmpty())
            return;
        
        // sort units based on which transports are allowed to unload
        if (MoveValidator.isUnload(route) && Match.someMatch(units, Matches.UnitIsLand))
        {
            
            Collections.sort(units, UnitComparator.getUnloadableUnitsComparator(units, route, getUnitOwner(units), true));
        }
        else
        {
            Collections.sort(units, UnitComparator.getMovableUnitsComparator(units, route, getUnitOwner(units), true));
        }
    }
    
    /**
     * Sort the specified transports in preferred load order.
     */
    private void sortTransportsToLoad(final List<Unit> transports, final Route route)
    {
        if (transports.isEmpty())
            return;
        
        Collections.sort(transports, UnitComparator.getLoadableTransportsComparator(transports, route, getUnitOwner(transports), true));
    }
    
    /**
     * Sort the specified transports in preferred unload order.
     */
    private void sortTransportsToUnload(List<Unit> transports, Route route)
    {
        if (transports.isEmpty())
            return;
        
        Collections.sort(transports, UnitComparator.getUnloadableTransportsComparator(transports, route, getUnitOwner(transports), true));
    }
    
    /**
     * Return the units that are to be unloaded for this route.
     * If needed will ask the user what transports to unload.
     * This is needed because the user needs to be able to select what transports to unload
     * in the case where some transports have different movement, different
     * units etc
     */
    private Collection<Unit> getUnitsToUnload(final Route route, final Collection<Unit> unitsToUnload)
    {
        Collection<Unit> allUnits = getFirstSelectedTerritory().getUnits().getUnits();
        List<Unit> candidateUnits = Match.getMatches(allUnits, getUnloadableMatch(route, unitsToUnload));
        
        if (unitsToUnload.size() == candidateUnits.size())
            return unitsToUnload;
        
        List<Unit> candidateTransports = Match.getMatches(allUnits, Matches.unitIsTransportingSomeCategories(candidateUnits));
        
        // remove all incapable transports
        Collection<Unit> incapableTransports = Match.getMatches(candidateTransports, Matches.transportCannotUnload(route.getEnd()));
        candidateTransports.removeAll(incapableTransports);
        
        if (candidateTransports.size() == 0)
            return Collections.emptyList();
        
        // just one transport, dont bother to ask
        if (candidateTransports.size() == 1)
            return unitsToUnload;
        
        // are the transports all of the same type
        // if they are, then don't ask
        Collection<UnitCategory> categories = UnitSeperator.categorize(candidateTransports, m_mustMoveWithDetails.getMustMoveWith(), true, false);
        if (categories.size() == 1)
            return unitsToUnload;
        
        sortTransportsToUnload(candidateTransports, route);
        
        // unitsToUnload are actually dependents, but need to select transports
        Map<Unit, Unit> unitsToTransports = MoveDelegate.mapTransports(route, unitsToUnload, candidateTransports);
        Set<Unit> defaultSelections = new HashSet<Unit>(unitsToTransports.values());
        
        // match criteria to ensure that chosen transports will match selected units
        Match<Collection<Unit>> transportsToUnloadMatch = new Match<Collection<Unit>>()
        {
            @Override
			public boolean match(Collection<Unit> units)
            {
                List<Unit> sortedTransports = Match.getMatches(units, Matches.UnitIsTransport);
                Collection<Unit> availableUnits = new ArrayList<Unit>(unitsToUnload);
                // track the changing capacities of the transports as we assign units
                final IntegerMap<Unit> capacityMap = new IntegerMap<Unit>();
                for (Unit transport : sortedTransports)
                {
                    Collection<Unit> transporting = TripleAUnit.get(transport).getTransporting();
                    capacityMap.add(transport, MoveValidator.getTransportCost(transporting));
                }
                boolean hasChanged = false;
                Comparator<Unit> increasingCapacityComparator = UnitComparator.getIncreasingCapacityComparator(sortedTransports);
                
                // This algorithm will ensure that it is actually possible to distribute
                // the selected units amongst the current selection of chosen transports.
                do
                {
                    hasChanged = false;
                    // sort transports by increasing capacity
                    Collections.sort(sortedTransports, increasingCapacityComparator);
                    
                    // try to remove one unit from each transport, in succession
                    Iterator<Unit> transportIter = sortedTransports.iterator();
                    while (transportIter.hasNext())
                    {
                        Unit transport = transportIter.next();
                        Collection<Unit> transporting = TripleAUnit.get(transport).getTransporting();
                        
                        if (transporting == null)
                            continue;
                        Collection<UnitCategory> transCategories = UnitSeperator.categorize(transporting);
                        Iterator<Unit> unitIter = availableUnits.iterator();
                        while (unitIter.hasNext())
                        {
                            Unit unit = unitIter.next();
                            Collection<UnitCategory> unitCategory = UnitSeperator.categorize(Collections.singleton(unit));
                            // is one of the transported units of the same type we want to unload?
                            if (Util.someIntersect(transCategories, unitCategory))
                            {
                                // unload the unit, remove the transport from our list, and continue
                                hasChanged = true;
                                unitIter.remove();
                                transportIter.remove();
                                break;
                            }
                        }
                    }
                    // repeat until there are no units left or no changes occur
                }
                while (availableUnits.size() > 0 && hasChanged);
                
                // if we haven't seen all of the transports (and removed them)
                // then there are extra transports that don't fit.
                return (sortedTransports.size() == 0);
            }
        };
        
        // choosing what transports to unload
        UnitChooser chooser = new UnitChooser(candidateTransports, defaultSelections, m_mustMoveWithDetails.getMustMoveWith(),
        /* categorizeMovement */true,
        /* categorizeTransportCost */false, getGameData(),
        /* allowTwoHit */false, getMap().getUIContext(), transportsToUnloadMatch);
        chooser.setTitle("What transports do you want to unload");
        
        int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), chooser, "What transports do you want to unload", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
        
        if (option != JOptionPane.OK_OPTION)
            return Collections.emptyList();
        
        String title = "unload";
        String action = "unload";
        Collection<Unit> chosenUnits = UserChooseUnits(defaultSelections, transportsToUnloadMatch, candidateTransports, title, action);
        Collection<Unit> choosenTransports = Match.getMatches(chosenUnits, Matches.UnitIsTransport);
        // Collection<Unit> choosenTransports = Match.getMatches(chooser.getSelected(), Matches.UnitIsTransport);
        
        List<Unit> allUnitsInSelectedTransports = new ArrayList<Unit>();
        for (Unit transport : choosenTransports)
        {
            Collection<Unit> transporting = TripleAUnit.get(transport).getTransporting();
            if (transporting != null)
                allUnitsInSelectedTransports.addAll(transporting);
        }
        allUnitsInSelectedTransports.retainAll(candidateUnits);
        sortUnitsToMove(allUnitsInSelectedTransports, route);
        
        List<Unit> rVal = new ArrayList<Unit>();
        List<Unit> sortedTransports = new ArrayList<Unit>(choosenTransports);
        Collections.sort(sortedTransports, UnitComparator.getIncreasingCapacityComparator(sortedTransports));
        
        Collection<Unit> selectedUnits = new ArrayList<Unit>(unitsToUnload);
        
        // first pass: choose one unit from each selected transport
        for (Unit transport : sortedTransports)
        {
            boolean hasChanged = false;
            Iterator<Unit> selectedIter = selectedUnits.iterator();
            while (selectedIter.hasNext())
            {
                Unit selected = selectedIter.next();
                Collection<Unit> transporting = TripleAUnit.get(transport).getTransporting();
                
                for (Unit candidate : transporting)
                {
                    if (selected.getType().equals(candidate.getType()) && selected.getOwner().equals(candidate.getOwner()) && selected.getHits() == candidate.getHits())
                    {
                        hasChanged = true;
                        rVal.add(candidate);
                        allUnitsInSelectedTransports.remove(candidate);
                        selectedIter.remove();
                        break;
                    }
                }
                if (hasChanged)
                    break;
            }
        }
        
        // now fill remaining slots in preferred unit order
        for (Unit selected : selectedUnits)
        {
            Iterator<Unit> candidateIter = allUnitsInSelectedTransports.iterator();
            while (candidateIter.hasNext())
            {
                Unit candidate = candidateIter.next();
                if (selected.getType().equals(candidate.getType()) && selected.getOwner().equals(candidate.getOwner()) && selected.getHits() == candidate.getHits())
                {
                    rVal.add(candidate);
                    candidateIter.remove();
                    break;
                }
            }
        }
        
        return rVal;
        
    }
    
    private CompositeMatch<Unit> getUnloadableMatch(final Route route, final Collection<Unit> units)
    {
        CompositeMatch<Unit> unloadable = new CompositeMatchAnd<Unit>();
        unloadable.add(getMovableMatch(route, units));
        unloadable.add(Matches.UnitIsLand);
        return unloadable;
    }
    
    private CompositeMatch<Unit> getMovableMatch(final Route route, final Collection<Unit> units)
    {
        CompositeMatch<Unit> movable = new CompositeMatchAnd<Unit>();
        if (!EditDelegate.getEditMode(getData()))
            movable.add(Matches.unitIsOwnedBy(getCurrentPlayer()));
        
        /*
         * if you do not have selection of zero-movement units enabled,
         * this will restrict selection to units with 1 or more movement
         */
        if (!games.strategy.triplea.Properties.getSelectableZeroMovementUnits(getData()))
            movable.add(Matches.UnitCanMove);
        
        if (!m_nonCombat)
            movable.add(new InverseMatch<Unit>(Matches.UnitIsAAorIsAAmovement));
        if (route != null)
        {
            Match<Unit> enoughMovement = new Match<Unit>()
            {
                @Override
				public boolean match(Unit u)
                {
                    if (EditDelegate.getEditMode(getData()))
                        return true;
                    return TripleAUnit.get(u).getMovementLeft() >= route.getLength();
                }
                
            };
            if (MoveValidator.isUnload(route) && route.getLength() == 1)
            {
                CompositeMatch<Unit> landOrCanMove = new CompositeMatchOr<Unit>();
                landOrCanMove.add(Matches.UnitIsLand);
                CompositeMatch<Unit> notLandAndCanMove = new CompositeMatchAnd<Unit>();
                notLandAndCanMove.add(enoughMovement);
                notLandAndCanMove.add(Matches.UnitIsNotLand);
                landOrCanMove.add(notLandAndCanMove);
                movable.add(landOrCanMove);
            }
            else
                movable.add(enoughMovement);
        }
        
        if (route != null && route.getEnd() != null)
        {
            boolean water = route.getEnd().isWater();
            boolean load = MoveValidator.isLoad(route);
            if (water && !load)
                movable.add(Matches.UnitIsNotLand);
            if (!water)
                movable.add(Matches.UnitIsNotSea);
        }
        if (units != null && !units.isEmpty())
        {
            // force all units to have the same owner in edit mode
            PlayerID owner = getUnitOwner(units);
            if (EditDelegate.getEditMode(getData()))
                movable.add(Matches.unitIsOwnedBy(owner));
            CompositeMatch<Unit> rightUnitTypeMatch = new CompositeMatchOr<Unit>();
            for (Unit unit : units)
            {
                if (unit.getOwner().equals(owner))
                    rightUnitTypeMatch.add(Matches.unitIsOfType(unit.getType()));
            }
            movable.add(rightUnitTypeMatch);
        }
        return movable;
    }
    
    private Route getRoute(Territory start, Territory end)
    {
        getData().acquireReadLock();
        try
        {
            if (m_forced == null)
                return getRouteNonForced(start, end);
            else
                return getRouteForced(start, end);
        }
        finally
        {
            getData().releaseReadLock();
        }
    }
    
    /**
     * Get the route including the territories that we are forced to move through.
     */
    private Route getRouteForced(Territory start, Territory end)
    {
        if (m_forced == null || m_forced.size() == 0)
            throw new IllegalStateException("No forced territories:" + m_forced + " end:" + end + " start:" + start);
        
        Iterator<Territory> iter = m_forced.iterator();
        
        Territory last = getFirstSelectedTerritory();
        Territory current = null;
        
        Route total = new Route();
        total.setStart(last);
        
        while (iter.hasNext())
        {
            current = iter.next();
            Route add = getData().getMap().getRoute(last, current);
            
            Route newTotal = Route.join(total, add);
            if (newTotal == null)
                return total;
            
            total = newTotal;
            last = current;
        }
        
        if (!end.equals(last))
        {
            
            Route add = getRouteNonForced(last, end);
            Route newTotal = Route.join(total, add);
            if (newTotal != null)
                total = newTotal;
        }
        
        return total;
    }
    
    /**
     * Get the route ignoring forced territories
     */
    private Route getRouteNonForced(Territory start, Territory end)
    {
        // can't rely on current player being the unit owner in Edit Mode
        // look at the units being moved to determine allies and enemies
        PlayerID owner = getUnitOwner(m_selectedUnits);
        
        return MoveValidator.getBestRoute(start, end, getData(), owner, m_selectedUnits);
    }
    
    private void updateUnitsThatCanMoveOnRoute(Collection<Unit> units, final Route route)
    {
        if (route == null || route.getLength() == 0)
        {
            clearStatusMessage();
            getMap().showMouseCursor();
            m_currentCursorImage = null;
            m_unitsThatCanMoveOnRoute = new ArrayList<Unit>(units);
            return;
        }
        
        getMap().hideMouseCursor();
        // TODO kev check for already loaded airTransports
        Collection<Unit> transportsToLoad = Collections.emptyList();
        if (MoveValidator.isLoad(units, route, getData(), getCurrentPlayer()))
        {
            transportsToLoad = route.getEnd().getUnits().getMatches(new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.alliedUnit(getCurrentPlayer(), getData())));
        }
        
        List<Unit> best = new ArrayList<Unit>(units);
        // if the player selects a land unit and other units
        // when the
        // only consider the non land units
        if (route.getStart().isWater() && route.getEnd() != null && route.getEnd().isWater() && !MoveValidator.isLoad(route))
        {
            best = Match.getMatches(best, new InverseMatch<Unit>(Matches.UnitIsLand));
        }
        
        sortUnitsToMove(best, route);
        Collections.reverse(best);
        
        List<Unit> bestWithDependents = addMustMoveWith(best);
        
        MoveValidationResult allResults;
        getData().acquireReadLock();
        try
        {
            allResults = MoveValidator.validateMove(bestWithDependents, route, getCurrentPlayer(), transportsToLoad, m_nonCombat, getUndoableMoves(), getData());
        }
        finally
        {
            getData().releaseReadLock();
        }
        MoveValidationResult lastResults = allResults;
        
        if (!allResults.isMoveValid())
        {
            // if the player is invading only consider units that can invade
            if (!m_nonCombat && MoveValidator.isUnload(route) && Matches.isTerritoryEnemy(getCurrentPlayer(), getData()).match(route.getEnd()))
            {
            	best = Match.getMatches(best, Matches.UnitCanInvade);
                bestWithDependents = addMustMoveWith(best);
                lastResults = MoveValidator.validateMove(bestWithDependents, route, getCurrentPlayer(), transportsToLoad, m_nonCombat, getUndoableMoves(), getData());
            }
            
            while (!best.isEmpty() && !lastResults.isMoveValid())
            {
                best = best.subList(1, best.size());
                bestWithDependents = addMustMoveWith(best);
                lastResults = MoveValidator.validateMove(bestWithDependents, route, getCurrentPlayer(), transportsToLoad, m_nonCombat, getUndoableMoves(), getData());
            }
        }
        
        if (allResults.isMoveValid())
        {
            // valid move
            clearStatusMessage();
            m_currentCursorImage = null;
        }
        else
        {
            String message = allResults.getError();
            if (message == null)
            {
                message = allResults.getDisallowedUnitWarning(0);
            }
            if (message == null)
            {
                message = allResults.getUnresolvedUnitWarning(0);
            }
            
            if (!lastResults.isMoveValid())
            {
                setStatusErrorMessage(message);
                m_currentCursorImage = getMap().getErrorImage();
            }
            else
            {
                setStatusWarningMessage(message);
                m_currentCursorImage = getMap().getWarningImage();
            }
        }
        
        if (m_unitsThatCanMoveOnRoute.size() != new HashSet<Unit>(m_unitsThatCanMoveOnRoute).size())
        {
            cancelMove();
            return;
        }
        
        m_unitsThatCanMoveOnRoute = new ArrayList<Unit>(bestWithDependents);
    }
    
    private List<Unit> addMustMoveWith(List<Unit> best)
    {
        List<Unit> bestWithDependents = new ArrayList<Unit>(best);
        for (Unit u : best)
        {
            if (m_mustMoveWithDetails.getMustMoveWith().containsKey(u))
            {
                Collection<Unit> mustMoveWith = m_mustMoveWithDetails.getMustMoveWith().get(u);
                if (mustMoveWith != null)
                {
                    for (Unit m : mustMoveWith)
                    {
                        if (!bestWithDependents.contains(m))
                        {
                            bestWithDependents.addAll(mustMoveWith);
                        }
                    }
                }
                
            }
        }
        return bestWithDependents;
    }
    
    /**
     * Route can be null.
     */
    final void updateRouteAndMouseShadowUnits(final Route route)
    {
        getMap().setRoute(route, m_mouseSelectedPoint, m_mouseCurrentPoint, m_currentCursorImage);
        if (route == null)
            getMap().setMouseShadowUnits(null);
        else
        {
            getMap().setMouseShadowUnits(m_unitsThatCanMoveOnRoute);
        }
    }
    
    /**
     * Allow the user to select what transports to load.
     * 
     * If null is returned, the move should be canceled.
     */
    
    private Collection<Unit> getTransportsToLoad(final Route route, final Collection<Unit> unitsToLoad, boolean disablePrompts)
    {
        if (!MoveValidator.isLoad(route))
            return Collections.emptyList();
        if (Match.someMatch(unitsToLoad, Matches.UnitIsAir)) // Air units should never be asked to select transport
            return Collections.emptyList();
        Collection<Unit> endOwnedUnits = route.getEnd().getUnits().getUnits();
        
        final PlayerID unitOwner = getUnitOwner(unitsToLoad);
        MustMoveWithDetails endMustMoveWith = MoveValidator.getMustMoveWith(route.getEnd(), endOwnedUnits, getData(), unitOwner);
        
        int minTransportCost = s_defaultMinTransportCost;
        for (Unit unit : unitsToLoad)
        {
            minTransportCost = Math.min(minTransportCost, UnitAttachment.get(unit.getType()).getTransportCost());
        }
        
        CompositeMatch<Unit> candidateTransportsMatch = new CompositeMatchAnd<Unit>();
        candidateTransportsMatch.add(Matches.UnitIsTransport);
        candidateTransportsMatch.add(Matches.alliedUnit(unitOwner, getGameData()));
        
        final List<Unit> candidateTransports = Match.getMatches(endOwnedUnits, candidateTransportsMatch);
        
        // remove transports that don't have enough capacity
        Iterator<Unit> transportIter = candidateTransports.iterator();
        while (transportIter.hasNext())
        {
            Unit transport = transportIter.next();
            int capacity = getTransportTracker().getAvailableCapacity(transport);
            if (capacity < minTransportCost)
                transportIter.remove();
        }
        
        // nothing to choose
        if (candidateTransports.isEmpty())
            return Collections.emptyList();
        
        // sort transports in preferred load order
        sortTransportsToLoad(candidateTransports, route);
        
        List<Unit> availableUnits = new ArrayList<Unit>(unitsToLoad);
        
        IntegerMap<Unit> availableCapacityMap = new IntegerMap<Unit>();
        for (Unit transport : candidateTransports)
        {
            int capacity = getTransportTracker().getAvailableCapacity(transport);
            availableCapacityMap.put(transport, capacity);
        }
        
        Set<Unit> defaultSelections = new HashSet<Unit>();
        
        // Algorithm to choose defaultSelections (transports to load)
        //
        // This algorithm uses mapTransports(), except mapTransports operates on
        // transports that have already been selected for loading.
        // We are trying to determine which transports are the best defaults to select for loading,
        // and so we need a modified algorithm based strictly on candidateTransports order:
        // - owned, capable transports are chosen first; attempt to fill them
        // - allied, capable transports are chosen next; attempt to fill them
        // - finally, incapable transports are chosen last (will generate errors)
        // Note that if any allied transports qualify as defaults, we will always prompt with a
        // UnitChooser later on so that it is obvious to the player.
        //
        
        boolean useAlliedTransports = false;
        Collection<Unit> capableTransports = new ArrayList<Unit>(candidateTransports);
        
        // only allow incapable transports for updateUnitsThatCanMoveOnRoute
        // so that we can have a nice UI error shown if these transports
        // are selected, since it may not be obvious
        Collection<Unit> incapableTransports = Match.getMatches(capableTransports, Matches.transportCannotUnload(route.getEnd()));
        capableTransports.removeAll(incapableTransports);
        
        Match<Unit> alliedMatch = new Match<Unit>()
        {
            @Override
			public boolean match(Unit transport)
            {
                return (!transport.getOwner().equals(unitOwner));
            }
        };
        Collection<Unit> alliedTransports = Match.getMatches(capableTransports, alliedMatch);
        capableTransports.removeAll(alliedTransports);
        
        // First, load capable transports
        Map<Unit, Unit> unitsToCapableTransports = MoveDelegate.mapTransports(route, availableUnits, capableTransports);
        for (Unit unit : unitsToCapableTransports.keySet())
        {
            Unit transport = unitsToCapableTransports.get(unit);
            int unitCost = UnitAttachment.get(unit.getType()).getTransportCost();
            availableCapacityMap.add(transport, (-1 * unitCost));
            defaultSelections.add(transport);
        }
        availableUnits.removeAll(unitsToCapableTransports.keySet());
        
        // Next, load allied transports
        Map<Unit, Unit> unitsToAlliedTransports = MoveDelegate.mapTransports(route, availableUnits, alliedTransports);
        for (Unit unit : unitsToAlliedTransports.keySet())
        {
            Unit transport = unitsToAlliedTransports.get(unit);
            int unitCost = UnitAttachment.get(unit.getType()).getTransportCost();
            availableCapacityMap.add(transport, (-1 * unitCost));
            defaultSelections.add(transport);
            useAlliedTransports = true;
        }
        availableUnits.removeAll(unitsToAlliedTransports.keySet());
        
        // only allow incapable transports for updateUnitsThatCanMoveOnRoute
        // so that we can have a nice UI error shown if these transports
        // are selected, since it may not be obvious
        if (getSelectedEndpointTerritory() == null)
        {
            Map<Unit, Unit> unitsToIncapableTransports = MoveDelegate.mapTransports(route, availableUnits, incapableTransports);
            for (Unit unit : unitsToIncapableTransports.keySet())
            {
                Unit transport = unitsToIncapableTransports.get(unit);
                int unitCost = UnitAttachment.get(unit.getType()).getTransportCost();
                availableCapacityMap.add(transport, (-1 * unitCost));
                defaultSelections.add(transport);
            }
            availableUnits.removeAll(unitsToIncapableTransports.keySet());
        }
        else
        {
            candidateTransports.removeAll(incapableTransports);
        }
        
        // return defaults if we aren't allowed to prompt
        if (disablePrompts)
            return defaultSelections;
        
        // force UnitChooser to pop up if we are choosing allied transports
        if (!useAlliedTransports)
        {
            if (candidateTransports.size() == 1)
                return candidateTransports;
            
            // all the same type, dont ask unless we have more than 1 unit type
            if (UnitSeperator.categorize(candidateTransports, endMustMoveWith.getMustMoveWith(), true, false).size() == 1 && unitsToLoad.size() == 1)
                return candidateTransports;
            
            // If we've filled all transports, then no user intervention is required.
            // It is possible to make "wrong" decisions if there are mixed unit types and
            // mixed transport categories, but there is no UI to manage that anyway.
            // Players will need to load incrementally in such cases.
            if (defaultSelections.containsAll(candidateTransports))
            {
                boolean spaceLeft = false;
                for (Unit transport : candidateTransports)
                {
                    int capacity = availableCapacityMap.getInt(transport);
                    if (capacity >= minTransportCost)
                    {
                        spaceLeft = true;
                        break;
                    }
                }
                if (!spaceLeft)
                    return candidateTransports;
            }
        }
        
        // the match criteria to ensure that chosen transports will match selected units
        Match<Collection<Unit>> transportsToLoadMatch = new Match<Collection<Unit>>()
        {
            @Override
			public boolean match(Collection<Unit> units)
            {
                Collection<Unit> transports = Match.getMatches(units, Matches.UnitIsTransport);
                // prevent too many transports from being selected
                return (transports.size() <= Math.min(unitsToLoad.size(), candidateTransports.size()));
            }
        };
        
        UnitChooser chooser = new UnitChooser(candidateTransports, defaultSelections, endMustMoveWith.getMustMoveWith(),
        /* categorizeMovement */true,
        /* categorizeTransportCost */false, getGameData(),
        /* allowTwoHit */false, getMap().getUIContext(), transportsToLoadMatch);
        
        chooser.setTitle("What transports do you want to load");
        int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), chooser, "What transports do you want to load", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (option != JOptionPane.OK_OPTION)
            return Collections.emptyList();
        
        return chooser.getSelected(false);
    }
    
    private TransportTracker getTransportTracker()
    {
        return m_transportTracker;
    }
    
    private final UnitSelectionListener m_UNIT_SELECTION_LISTENER = new UnitSelectionListener()
    {
        
        @Override
		public void unitsSelected(List<Unit> units, Territory t, MouseDetails me)
        {
            if (!getListening())
                return;
            
            // check if we can handle this event, are we active?
            if (!getActive())
                return;
            if (t == null)
                return;
            
            boolean rightMouse = me.isRightButton();
            boolean noSelectedTerritory = (m_firstSelectedTerritory == null);
            boolean isFirstSelectedTerritory = (m_firstSelectedTerritory == t);
            
            // select units
            final GameData data = getData();
            data.acquireReadLock();
            try
            {
                // de select units
                if (rightMouse && !noSelectedTerritory)
                    deselectUnits(units, t, me);
                else if (!rightMouse && (noSelectedTerritory || isFirstSelectedTerritory))
                    selectUnitsToMove(units, t, me);
                else if (!rightMouse && me.isControlDown() && !isFirstSelectedTerritory)
                    selectWayPoint(t);
                else if (!rightMouse && !noSelectedTerritory && !isFirstSelectedTerritory)
                    selectEndPoint(t);
            }
            finally
            {
                data.releaseReadLock();
            }
            
        }
        
        private void selectUnitsToMove(List<Unit> units, Territory t, MouseDetails me)
        {
            
            // are any of the units ours, note - if no units selected thats still ok
            if (!EditDelegate.getEditMode(getData()) || !m_selectedUnits.isEmpty())
            {
                for (Unit unit : units)
                {
                    if (!unit.getOwner().equals(getUnitOwner(m_selectedUnits)))
                    {
                        return;
                    }
                }
            }
            // basic match criteria only
            CompositeMatch<Unit> unitsToMoveMatch = getMovableMatch(null, null);
            
            Match<Collection<Unit>> ownerMatch = new Match<Collection<Unit>>()
            {
                @Override
				public boolean match(Collection<Unit> unitsToCheck)
                {
                    PlayerID owner = unitsToCheck.iterator().next().getOwner();
                    for (Unit unit : unitsToCheck)
                    {
                        if (!owner.equals(unit.getOwner()))
                            return false;
                    }
                    return true;
                }
            };
            
            if (units.isEmpty() && m_selectedUnits.isEmpty())
            {
                if (!me.isShiftDown())
                {
                    
                    List<Unit> unitsToMove = t.getUnits().getMatches(unitsToMoveMatch);
                    
                    if (unitsToMove.isEmpty())
                        return;
                    
                    String text = "Select units to move from " + t.getName();
                    
                    UnitChooser chooser;
                    
                    if (EditDelegate.getEditMode(getData()) && !Match.getMatches(unitsToMove, Matches.unitIsOwnedBy(getUnitOwner(unitsToMove))).containsAll(unitsToMove))
                    {
                        // use matcher to prevent units of different owners being chosen
                        chooser = new UnitChooser(unitsToMove, m_selectedUnits,
                        /* mustMoveWith */null,
                        /* categorizeMovement */false,
                        /* categorizeTransportCost */false, getData(),
                        /* allowTwoHit */false, getMap().getUIContext(), ownerMatch);
                    }
                    else
                    {
                        chooser = new UnitChooser(unitsToMove, m_selectedUnits,
                        /* mustMoveWith */null,
                        /* categorizeMovement */false,
                        /* categorizeTransportCost */false, getData(),
                        /* allowTwoHit */false, getMap().getUIContext());
                    }
                    
                    int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), chooser, text, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
                    
                    if (option != JOptionPane.OK_OPTION)
                        return;
                    if (chooser.getSelected(false).isEmpty())
                        return;
                    
                    m_selectedUnits.addAll(chooser.getSelected(false));
                }
                
            }
            
            if (getFirstSelectedTerritory() == null)
            {
                setFirstSelectedTerritory(t);
                
                m_mouseSelectedPoint = me.getMapPoint();
                m_mouseCurrentPoint = me.getMapPoint();
                
                enableCancelButton();
            }
            
            if (!getFirstSelectedTerritory().equals(t))
                throw new IllegalStateException("Wrong selected territory");
            
            // add all
            if (me.isShiftDown())
            {
                // prevent units of multiple owners from being chosen in edit mode
                CompositeMatch<Unit> ownedNotFactory = new CompositeMatchAnd<Unit>();
                if (!EditDelegate.getEditMode(getData()))
                {
                    ownedNotFactory.add(unitsToMoveMatch);
                }
                else if (!m_selectedUnits.isEmpty())
                {
                    ownedNotFactory.add(unitsToMoveMatch);
                    ownedNotFactory.add(Matches.unitIsOwnedBy(getUnitOwner(m_selectedUnits)));
                }
                else
                {
                    ownedNotFactory.add(unitsToMoveMatch);
                    ownedNotFactory.add(Matches.unitIsOwnedBy(getUnitOwner(t.getUnits().getUnits())));
                }
                m_selectedUnits.addAll(t.getUnits().getMatches(ownedNotFactory));
            }
            else if (me.isControlDown())
            {
                m_selectedUnits.addAll(Match.getMatches(units, unitsToMoveMatch));
            }
            // add one
            else
            {
                // best candidate unit for route is chosen dynamically later
                // check for alt key - add 1/10 of total units (useful for splitting large armies)
                List<Unit> unitsToMove = Match.getMatches(units, unitsToMoveMatch);
                Collections.sort(unitsToMove, UnitComparator.getIncreasingMovementComparator());
                int iterCount = (me.isAltDown()) ? (int) Math.max(1, Math.floor(unitsToMove.size() / s_deselectNumber)) : 1;
                int addCount = 0;
                
                for (Unit unit : unitsToMove)
                {
                    if (!m_selectedUnits.contains(unit))
                    {
                        m_selectedUnits.add(unit);
                        addCount++;
                        if (addCount >= iterCount)
                            break;
                    }
                }
            }
            
            if (!m_selectedUnits.isEmpty())
            {
                m_mouseLastUpdatePoint = me.getMapPoint();
                Route route = getRoute(getFirstSelectedTerritory(), t);
                
                // Load Bombers with paratroops
                if ((!m_nonCombat || IsParatroopersCanMoveDuringNonCombat(getData())) && isParatroopers(getCurrentPlayer()) && Match.someMatch(m_selectedUnits, new CompositeMatchAnd<Unit>(Matches.UnitIsAirTransport, Matches.unitHasNotMoved)))
                {
                    final PlayerID player = getCurrentPlayer();
                    
                    // TODO Transporting allied units
                    // Get the potential units to load
                    CompositeMatch<Unit> unitsToLoadMatch = new CompositeMatchAnd<Unit>();
                    unitsToLoadMatch.add(Matches.UnitIsAirTransportable);
                    unitsToLoadMatch.add(Matches.unitIsOwnedBy(player));
                    unitsToLoadMatch.add(Matches.unitHasNotMoved);
                    Collection<Unit> unitsToLoad = Match.getMatches(route.getStart().getUnits().getUnits(), unitsToLoadMatch);
                    unitsToLoad.removeAll(m_selectedUnits);
                    for (Unit u : s_dependentUnits.keySet())
                    {
                        unitsToLoad.removeAll(s_dependentUnits.get(u));
                    }
                    
                    // Get the potential air transports to load
                    CompositeMatch<Unit> candidateAirTransportsMatch = new CompositeMatchAnd<Unit>();
                    candidateAirTransportsMatch.add(Matches.UnitIsAirTransport);
                    candidateAirTransportsMatch.add(Matches.unitIsOwnedBy(player));
                    candidateAirTransportsMatch.add(Matches.unitHasNotMoved);
                    candidateAirTransportsMatch.add(Matches.transportIsNotTransporting());
                    Collection<Unit> candidateAirTransports = Match.getMatches(t.getUnits().getMatches(unitsToMoveMatch), candidateAirTransportsMatch);
                    // candidateAirTransports.removeAll(m_selectedUnits);
                    candidateAirTransports.removeAll(s_dependentUnits.keySet());
                    
                    if (unitsToLoad.size() > 0 && candidateAirTransports.size() > 0)
                    {
                        Collection<Unit> airTransportsToLoad = getAirTransportsToLoad(candidateAirTransports);
                        m_selectedUnits.addAll(airTransportsToLoad);
                        if (!airTransportsToLoad.isEmpty())
                        {
                            Collection<Unit> loadedAirTransports = getLoadedAirTransports(route, unitsToLoad, airTransportsToLoad, player);
                            m_selectedUnits.addAll(loadedAirTransports);
                            MoveDescription message = new MoveDescription(loadedAirTransports, route, airTransportsToLoad);
                            setMoveMessage(message);
                        }
                    }
                }
                
                updateUnitsThatCanMoveOnRoute(m_selectedUnits, route);
                updateRouteAndMouseShadowUnits(route);
            }
            else
                setFirstSelectedTerritory(null);
        }
        
        public Collection<Unit> getAirTransportsToLoad(final Collection<Unit> candidateAirTransports)
        {
            Set<Unit> defaultSelections = new HashSet<Unit>();
            
            // prevent too many bombers from being selected
            Match<Collection<Unit>> transportsToLoadMatch = new Match<Collection<Unit>>()
            {
                @Override
				public boolean match(Collection<Unit> units)
                {
                    Collection<Unit> airTransports = Match.getMatches(units, Matches.UnitIsAirTransport);
                    return (airTransports.size() <= candidateAirTransports.size());
                }
            };
            // Allow player to select which to load.
            UnitChooser chooser = new UnitChooser(candidateAirTransports, defaultSelections, s_dependentUnits,
            /* categorizeMovement */true,
            /* categorizeTransportCost */false, getGameData(),
            /* allowTwoHit */false, getMap().getUIContext(), transportsToLoadMatch);
            
            chooser.setTitle("Select air transports to load");
            int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), chooser, "What transports do you want to load", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
            if (option != JOptionPane.OK_OPTION)
                return Collections.emptyList();
            
            return chooser.getSelected(true);
        }
        
        /**
         * Allow the user to select what units to load.
         * 
         * If null is returned, the move should be canceled.
         */
        
        public Collection<Unit> getLoadedAirTransports(final Route route, final Collection<Unit> capableUnitsToLoad, final Collection<Unit> capableTransportsToLoad, final PlayerID player)
        {
            // Get the minimum transport cost of a candidate unit
            int minTransportCost = Integer.MAX_VALUE;
            for (Unit unit : capableUnitsToLoad)
            {
                minTransportCost = Math.min(minTransportCost, UnitAttachment.get(unit.getType()).getTransportCost());
            }
            
            final Collection<Unit> airTransportsToLoad = new ArrayList<Unit>();
            int ttlCapacity = 0;
            for (Unit bomber : capableTransportsToLoad)
            {
                int capacity = getTransportTracker().getAvailableCapacity(bomber);
                if (capacity >= minTransportCost)
                {
                    airTransportsToLoad.add(bomber);
                    ttlCapacity += capacity;
                }
            }
            
            // If no airTransports can be loaded, return the empty set
            if (airTransportsToLoad.isEmpty())
                return airTransportsToLoad;
            
            Set<Unit> defaultSelections = new HashSet<Unit>();
            
            // Check to see if there's room for the selected units
            Match<Collection<Unit>> unitsToLoadMatch = new Match<Collection<Unit>>()
            {
                @Override
				public boolean match(Collection<Unit> units)
                {
                    Collection<Unit> unitsToLoad = Match.getMatches(units, Matches.UnitIsAirTransportable);
                    Map<Unit, Unit> unitMap = MoveDelegate.mapTransports(route, unitsToLoad, airTransportsToLoad, true, player);
                    boolean ableToLoad = true;
                    for (Unit unit : unitsToLoad)
                    {
                        if (!unitMap.keySet().contains(unit))
                            ableToLoad = false;
                    }
                    return ableToLoad;
                }
            };
            
            List<Unit> loadedUnits = new ArrayList<Unit>(capableUnitsToLoad);
            if (!airTransportsToLoad.isEmpty())
            {
                // Get a list of the units that could be loaded on the transport (based upon transport capacity)
                List<Unit> unitsToLoad = MoveDelegate.mapAirTransportPossibilities(route, capableUnitsToLoad, airTransportsToLoad, true, player);
                String title = "Load air transports";
                String action = "load";
                
                loadedUnits = UserChooseUnits(defaultSelections, unitsToLoadMatch, unitsToLoad, title, action);
                
                Map<Unit, Unit> mapping = MoveDelegate.mapTransports(route, loadedUnits, airTransportsToLoad, true, player);
                
                for (Unit unit : mapping.keySet())
                {
                    Collection<Unit> unitsColl = new ArrayList<Unit>();
                    unitsColl.add(unit);
                    
                    Unit airTransport = mapping.get(unit);
                    if (s_dependentUnits.containsKey(airTransport))
                    {
                        unitsColl.addAll(s_dependentUnits.get(airTransport));
                    }
                    s_dependentUnits.put(airTransport, unitsColl);
                    m_mustMoveWithDetails = MoveValidator.getMustMoveWith(route.getStart(), route.getStart().getUnits().getUnits(), getData(), player);
                }
            }
            
            return loadedUnits;
        }
        
        private void deselectUnits(List<Unit> units, Territory t, MouseDetails me)
        {
            
            Collection<Unit> unitsToRemove = new ArrayList<Unit>(m_selectedUnits.size());
            
            // we have right clicked on a unit stack in a different territory
            if (!getFirstSelectedTerritory().equals(t))
                units = Collections.emptyList();
            
            // remove the dependent units so we don't have to micromanage them
            List<Unit> unitsWithoutDependents = new ArrayList<Unit>(m_selectedUnits);
            for (Unit unit : m_selectedUnits)
            {
                Collection<Unit> forced = m_mustMoveWithDetails.getMustMoveWith().get(unit);
                if (forced != null)
                {
                    unitsWithoutDependents.removeAll(forced);
                }
            }
            
            // no unit selected, remove the most recent, but skip dependents
            if (units.isEmpty())
            {
                if (me.isControlDown())
                {
                    m_selectedUnits.clear();
                    
                    // Clear the stored dependents for AirTransports
                    if (!s_dependentUnits.isEmpty())
                    {
                        s_dependentUnits.clear();
                        /*
                         * for (Unit airTransport : unitsWithoutDependents)
                         * {
                         * if (s_dependentUnits.containsKey(airTransport))
                         * {
                         * unitsToRemove.addAll(s_dependentUnits.get(airTransport));
                         * s_dependentUnits.remove(airTransport);
                         * }
                         * }
                         */
                    }
                }
                else if (!unitsWithoutDependents.isEmpty())
                {
                    // check for alt key - remove 1/10 of total units (useful for splitting large armies)
                    int iterCount = (me.isAltDown()) ? (int) Math.max(1, Math.floor(unitsWithoutDependents.size() / s_deselectNumber)) : 1;
                    
                    // remove the last iterCount elements
                    for (int i = 0; i < iterCount; i++)
                    {
                        unitsToRemove.add(unitsWithoutDependents.get(unitsWithoutDependents.size() - 1));
                        // Clear the stored dependents for AirTransports
                        if (!s_dependentUnits.isEmpty())
                        {
                            for (Unit airTransport : unitsWithoutDependents)
                            {
                                if (s_dependentUnits.containsKey(airTransport))
                                {
                                    unitsToRemove.addAll(s_dependentUnits.get(airTransport));
                                    s_dependentUnits.remove(airTransport);
                                }
                            }
                        }
                    }
                }
            }
            // we have actually clicked on a specific unit
            else
            {
                // remove all if control is down
                if (me.isControlDown())
                {
                    unitsToRemove.addAll(units);
                    
                    // Clear the stored dependents for AirTransports
                    if (!s_dependentUnits.isEmpty())
                    {
                        for (Unit airTransport : unitsWithoutDependents)
                        {
                            if (s_dependentUnits.containsKey(airTransport))
                            {
                                unitsToRemove.addAll(s_dependentUnits.get(airTransport));
                                s_dependentUnits.remove(airTransport);
                            }
                        }
                    }
                }
                // remove one
                else
                {
                    if (!getFirstSelectedTerritory().equals(t))
                        throw new IllegalStateException("Wrong selected territory");
                    
                    // doesn't matter which unit we remove since units are assigned to routes later
                    // check for alt key - remove 1/10 of total units (useful for splitting large armies)
                    int iterCount = (me.isAltDown()) ? (int) Math.max(1, Math.floor(units.size() / s_deselectNumber)) : 1;
                    int remCount = 0;
                    
                    for (Unit unit : units)
                    {
                        if (m_selectedUnits.contains(unit) && !unitsToRemove.contains(unit))
                        {
                            unitsToRemove.add(unit);
                            // Clear the stored dependents for AirTransports
                            if (!s_dependentUnits.isEmpty())
                            {
                                for (Unit airTransport : unitsWithoutDependents)
                                {
                                    if (s_dependentUnits.containsKey(airTransport))
                                    {
                                        s_dependentUnits.get(airTransport).remove(unit);
                                    }
                                }
                            }
                            remCount++;
                            if (remCount >= iterCount)
                                break;
                        }
                    }
                }
            }
            
            // perform the remove
            m_selectedUnits.removeAll(unitsToRemove);
            
            if (m_selectedUnits.isEmpty())
            {
                // nothing left, cancel move
                cancelMove();
            }
            else
            {
                m_mouseLastUpdatePoint = me.getMapPoint();
                updateUnitsThatCanMoveOnRoute(m_selectedUnits, getRoute(getFirstSelectedTerritory(), t));
                updateRouteAndMouseShadowUnits(getRoute(getFirstSelectedTerritory(), t));
            }
        }
        
        private void selectWayPoint(Territory territory)
        {
            if (m_forced == null)
                m_forced = new ArrayList<Territory>();
            
            if (!m_forced.contains(territory))
                m_forced.add(territory);
            
            updateRouteAndMouseShadowUnits(getRoute(getFirstSelectedTerritory(), getFirstSelectedTerritory()));
        }
        
        private CompositeMatch<Unit> getUnloadableMatch()
        {
            // are we unloading everything? if we are then we dont need to select the transports
            CompositeMatch<Unit> unloadable = new CompositeMatchAnd<Unit>();
            unloadable.add(Matches.unitIsOwnedBy(getCurrentPlayer()));
            unloadable.add(Matches.UnitIsLand);
            if (m_nonCombat)
                unloadable.add(new InverseMatch<Unit>(Matches.UnitIsAAorIsAAmovement));
            return unloadable;
        }
        
        private void selectEndPoint(Territory territory)
        {
            final Route route = getRoute(getFirstSelectedTerritory(), territory);
            final List<Unit> units = m_unitsThatCanMoveOnRoute;
            setSelectedEndpointTerritory(territory);
            if (units.isEmpty())
            {
                cancelMove();
                return;
            }
            
            Collection<Unit> transports = null;
            CompositeMatch<Unit> paratroopNBombers = new CompositeMatchAnd<Unit>();
            paratroopNBombers.add(Matches.UnitIsAirTransport);
            paratroopNBombers.add(Matches.UnitIsAirTransportable);
            boolean paratroopsLanding = Match.someMatch(units, paratroopNBombers);
            
            if (MoveValidator.isLoad(route) && Match.someMatch(units, Matches.UnitIsLand))
            {
                transports = getTransportsToLoad(route, units, false);
                if (transports.isEmpty())
                {
                    cancelMove();
                    return;
                }
            }
            else if ((MoveValidator.isUnload(route) && Match.someMatch(units, Matches.UnitIsLand)) || paratroopsLanding)
            {
                List<Unit> unloadAble = Match.getMatches(m_selectedUnits, getUnloadableMatch());
                
                Collection<Unit> canMove = new ArrayList<Unit>(getUnitsToUnload(route, unloadAble));
                canMove.addAll(Match.getMatches(m_selectedUnits, new InverseMatch<Unit>(getUnloadableMatch())));
                if (paratroopsLanding)
                    transports = canMove;
                if (canMove.isEmpty())
                {
                    cancelMove();
                    return;
                }
                else
                {
                    m_selectedUnits.clear();
                    m_selectedUnits.addAll(canMove);
                }
            }
            else
            {
                // keep a map of the max number of each eligible unitType that can be chosen
                final IntegerMap<UnitType> maxMap = new IntegerMap<UnitType>();
                for (Unit unit : units)
                    maxMap.add(unit.getType(), 1);
                // this match will make sure we can't select more units
                // of a specific type then we had originally selected
                
                Match<Collection<Unit>> unitTypeCountMatch = new Match<Collection<Unit>>()
                {
                    @Override
					public boolean match(Collection<Unit> units)
                    {
                        IntegerMap<UnitType> currentMap = new IntegerMap<UnitType>();
                        for (Unit unit : units)
                            currentMap.add(unit.getType(), 1);
                        return maxMap.greaterThanOrEqualTo(currentMap);
                    }
                };
                
                allowSpecificUnitSelection(units, route, false, unitTypeCountMatch);
                
                if (units.isEmpty())
                {
                    cancelMove();
                    return;
                }
            }
            
            MoveDescription message = new MoveDescription(units, route, transports);
            setMoveMessage(message);
            setFirstSelectedTerritory(null);
            setSelectedEndpointTerritory(null);
            m_mouseCurrentTerritory = null;
            m_forced = null;
            updateRouteAndMouseShadowUnits(null);
            release();
        }
        
    };
    
    /**
     * Allow the user to select specific units, if for example some units
     * have different movement
     * Units are sorted in preferred order, so units represents the default selections.
     */
    private boolean allowSpecificUnitSelection(Collection<Unit> units, final Route route, boolean mustQueryUser, Match<Collection<Unit>> matchCriteria)
    {
        List<Unit> candidateUnits = getFirstSelectedTerritory().getUnits().getMatches(getMovableMatch(route, units));
        
        if (!mustQueryUser)
        {
            Set<UnitCategory> categories = UnitSeperator.categorize(candidateUnits, m_mustMoveWithDetails.getMustMoveWith(), true, false);
            
            for (UnitCategory category1 : categories)
            {
                // we cant move these, dont bother to check
                if (category1.getMovement() == 0)
                    continue;
                
                for (UnitCategory category2 : categories)
                {
                    // we cant move these, dont bother to check
                    if (category2.getMovement() == 0)
                        continue;
                    
                    // if we find that two categories are compatable, and some units
                    // are selected from one category, but not the other
                    // then the user has to refine his selection
                    if (category1 != category2 && category1.getType() == category2.getType() && !category1.equals(category2))
                    {
                        // if we are moving all the units from both categories, then nothing to choose
                        if (units.containsAll(category1.getUnits()) && units.containsAll(category2.getUnits()))
                            continue;
                        // if we are moving some of the units from either category, then we need to stop
                        if (!Util.intersection(category1.getUnits(), units).isEmpty() || !Util.intersection(category2.getUnits(), units).isEmpty())
                        {
                            mustQueryUser = true;
                        }
                    }
                }
                
            }
        }
        
        if (mustQueryUser)
        {
            
            List<Unit> defaultSelections = new ArrayList<Unit>(units.size());
            
            if (MoveValidator.isLoad(route))
            {
                final Collection<Unit> transportsToLoad = new ArrayList<Unit>(getTransportsToLoad(route, units, false));
                defaultSelections.addAll(MoveDelegate.mapTransports(route, units, transportsToLoad).keySet());
            }
            else
            {
                defaultSelections.addAll(units);
            }
            
            // sort candidateUnits in preferred order
            sortUnitsToMove(candidateUnits, route);
            UnitChooser chooser = new UnitChooser(candidateUnits, defaultSelections, m_mustMoveWithDetails.getMustMoveWith(), true, false, getGameData(), false, getMap().getUIContext(), matchCriteria);
            
            String text = "Select units to move from " + getFirstSelectedTerritory() + ".";
            int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), chooser, text, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
            
            if (option != JOptionPane.OK_OPTION)
            {
                units.clear();
                return false;
            }
            
            units.clear();
            units.addAll(chooser.getSelected(false));
        }
        
        // add the dependent units
        List<Unit> unitsCopy = new ArrayList<Unit>(units);
        for (Unit unit : unitsCopy)
        {
            Collection<Unit> forced = m_mustMoveWithDetails.getMustMoveWith().get(unit);
            if (forced != null)
            {
                // add dependent if necessary
                for (Unit dependent : forced)
                {
                    if (unitsCopy.indexOf(dependent) == -1)
                        units.add(dependent);
                }
            }
        }
        
        return true;
    }
    
    private final MouseOverUnitListener m_MOUSE_OVER_UNIT_LISTENER = new MouseOverUnitListener()
    {
        
        @Override
		public void mouseEnter(List<Unit> units, Territory territory, MouseDetails me)
        {
            if (!getListening())
                return;
            Match<Unit> match = new CompositeMatchAnd<Unit>(Matches.unitIsOwnedBy(getUnitOwner(m_selectedUnits)), Matches.UnitIsNotFactory);
            boolean someOwned = Match.someMatch(units, match);
            boolean isCorrectTerritory = m_firstSelectedTerritory == null || m_firstSelectedTerritory == territory;
            if (someOwned && isCorrectTerritory)
                getMap().setUnitHighlight(units, territory);
            else
                getMap().setUnitHighlight(null, null);
        }
        
    };
    
    private final MapSelectionListener m_MAP_SELECTION_LISTENER = new DefaultMapSelectionListener()
    {
        @Override
		public void territorySelected(Territory territory, MouseDetails me)
        {
            
        }
        
        @Override
		public void mouseMoved(Territory territory, MouseDetails me)
        {
            if (!getListening())
                return;
            
            if (getFirstSelectedTerritory() != null && territory != null)
            {
                Route route = getRoute(getFirstSelectedTerritory(), territory);
                if (m_mouseCurrentTerritory == null || !m_mouseCurrentTerritory.equals(territory) || m_mouseCurrentPoint.equals(m_mouseLastUpdatePoint))
                {
                    getData().acquireReadLock();
                    try
                    {
                        updateUnitsThatCanMoveOnRoute(m_selectedUnits, route);
                    }
                    finally
                    {
                        getData().releaseReadLock();
                    }
                    
                }
                
                m_mouseCurrentPoint = me.getMapPoint();
                updateRouteAndMouseShadowUnits(route);
            }
            m_mouseCurrentTerritory = territory;
        }
    };
    
    @Override
	public final String toString()
    {
        return "MovePanel";
    }
    
    final void setFirstSelectedTerritory(Territory firstSelectedTerritory)
    {
        if (firstSelectedTerritory == m_firstSelectedTerritory)
            return;
        
        m_firstSelectedTerritory = firstSelectedTerritory;
        if (m_firstSelectedTerritory == null)
        {
            m_mustMoveWithDetails = null;
        }
        else
        {
            m_mustMoveWithDetails = MoveValidator.getMustMoveWith(firstSelectedTerritory, firstSelectedTerritory.getUnits().getUnits(), getData(), getCurrentPlayer());
        }
    }
    
    private Territory getFirstSelectedTerritory()
    {
        return m_firstSelectedTerritory;
    }
    
    final void setSelectedEndpointTerritory(Territory selectedEndpointTerritory)
    {
        m_selectedEndpointTerritory = selectedEndpointTerritory;
    }
    
    private Territory getSelectedEndpointTerritory()
    {
        return m_selectedEndpointTerritory;
    }
    
    private static boolean isParatroopers(PlayerID player)
    {
        TechAttachment ta = (TechAttachment) player.getAttachment(Constants.TECH_ATTACHMENT_NAME);
        if (ta == null)
            return false;
        return ta.hasParatroopers();
    }
    
    private static boolean IsParatroopersCanMoveDuringNonCombat(GameData data)
    {
        return games.strategy.triplea.Properties.getParatroopersCanMoveDuringNonCombat(data);
    }
    
    public final List<Unit> UserChooseUnits(Set<Unit> defaultSelections, Match<Collection<Unit>> unitsToLoadMatch, List<Unit> unitsToLoad, String title, String action)
    {
        // Allow player to select which to load.
        UnitChooser chooser = new UnitChooser(unitsToLoad, defaultSelections, s_dependentUnits,
        /* categorizeMovement */false,
        /* categorizeTransportCost */true, getGameData(),
        /* allowTwoHit */false, getMap().getUIContext(), unitsToLoadMatch);
        
        chooser.setTitle(title);
        int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), chooser, "What units do you want to " + action, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (option != JOptionPane.OK_OPTION)
            return Collections.emptyList();
        
        return chooser.getSelected(true);
    }
    
    /**
     * @see AbstraceMovePanel#cleanUpSpecific()
     */
    @Override
	protected final void cleanUpSpecific()
    {
        getMap().removeMapSelectionListener(m_MAP_SELECTION_LISTENER);
        getMap().removeUnitSelectionListener(m_UNIT_SELECTION_LISTENER);
        getMap().removeMouseOverUnitListener(m_MOUSE_OVER_UNIT_LISTENER);
        getMap().setUnitHighlight(null, null);
        m_selectedUnits.clear();
        updateRouteAndMouseShadowUnits(null);
        m_forced = null;
    }
    
    /**
     * @see AbstraceMovePanel#cancelMoveSpecific()
     */
    @Override
	protected final void cancelMoveAction()
    {
        setFirstSelectedTerritory(null);
        setSelectedEndpointTerritory(null);
        m_mouseCurrentTerritory = null;
        m_forced = null;
        m_selectedUnits.clear();
        m_currentCursorImage = null;
        updateRouteAndMouseShadowUnits(null);
        getMap().showMouseCursor();
        getMap().setMouseShadowUnits(null);
    }
    
    /**
     * @see AbstraceMovePanel#undoMoveSpecific()
     */
    @Override
	protected final void undoMoveSpecific()
    {
        getMap().setRoute(null);
    }
    
    public final void display(final PlayerID id, final boolean nonCombat)
    {
        m_nonCombat = nonCombat;
        m_transportTracker = new TransportTracker();
        super.display(id, (nonCombat ? " non combat" : " combat") + " move");
    }
    
    /**
     * @see AbstraceMovePanel#setUpSpecific()
     */
    @Override
	protected final void setUpSpecific()
    {
        setFirstSelectedTerritory(null);
        m_forced = null;
        getMap().addMapSelectionListener(m_MAP_SELECTION_LISTENER);
        getMap().addUnitSelectionListener(m_UNIT_SELECTION_LISTENER);
        getMap().addMouseOverUnitListener(m_MOUSE_OVER_UNIT_LISTENER);
    }
    
    @Override
    protected boolean doneMoveAction()
    {
        // TODO Auto-generated method stub
        if (m_undoableMovesPanel.getCountOfMovesMade() == 0)
        {
            int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent(MovePanel.this), "Are you sure you dont want to move?", "End Move", JOptionPane.YES_NO_OPTION);
            if (rVal != JOptionPane.YES_OPTION)
            {
                return false;
            }
            else
                return true;
            
        }
        return true;
    }
    
    @Override
    protected boolean setCancelButton()
    {
        // TODO Auto-generated method stub
        return true;
    }
    
}

/**
 * Avoid holding a strong reference to the action
 * fixes a memory leak in swing.
 */
@SuppressWarnings("serial")
class WeakAction extends AbstractAction
{
    private final WeakReference<Action> m_delegate;
    
    WeakAction(String name, Action delegate)
    {
        super(name);
        m_delegate = new WeakReference<Action>(delegate);
    }
    
    @Override
	public void actionPerformed(ActionEvent e)
    {
        Action a = m_delegate.get();
        if (a != null)
            a.actionPerformed(e);
        
    }
    
}
