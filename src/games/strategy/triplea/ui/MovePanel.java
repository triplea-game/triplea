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
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.dataObjects.MustMoveWithDetails;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;
import games.strategy.util.Util;

import java.awt.Point;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class MovePanel extends ActionPanel
{
    
    private static final String MOVE_PANEL_CANCEL = "movePanel.cancel";

    private static final Logger s_logger = Logger.getLogger(MovePanel.class.getName());
    
    private TripleAFrame m_frame;
    private boolean m_listening = false;
    private JLabel m_actionLabel = new JLabel();
    private MoveDescription m_moveMessage;
    //access only through getter and setter!
    private Territory m_firstSelectedTerritory;
    private Territory m_mouseCurrentTerritory;
    private IPlayerBridge m_bridge;

    private List<Territory> m_forced;
    private boolean m_nonCombat;
    private UndoableMovesPanel m_undoableMovesPanel;
    
    private Point m_mouseSelectedPoint;
    private Point m_mouseCurrentPoint;
    
    //use a LinkedHashSet because we want to know the order
    private final Set<Unit> m_selectedUnits = new LinkedHashSet<Unit>();
    
    //the must move with details for the currently selected territory
    //note this is kep in sync because we do not modify m_selectedTerritory directly
    //instead we only do so through the private setter
    private MustMoveWithDetails m_mustMoveWithDetails = null;

    // cache this so we can update it only when territory/units change
    private Collection<Unit> m_unitsThatCanMoveOnRoute;

    private Collection<UnitType> m_unresolvedUnitTypes;

    private Image m_currentCursorImage;

    /** Creates new MovePanel */
    public MovePanel(GameData data, MapPanel map, TripleAFrame frame)
    {
        super(data, map);
        m_frame = frame;
        CANCEL_MOVE_ACTION.setEnabled(false);

        m_undoableMovesPanel = new UndoableMovesPanel(data, this);
        m_mouseCurrentTerritory = null;
        m_unitsThatCanMoveOnRoute = Collections.emptyList();
        m_unresolvedUnitTypes = Collections.emptyList();
        m_currentCursorImage = null;
    }

    private JComponent leftBox(JComponent c)
    {
        Box b = new Box(BoxLayout.X_AXIS);
        b.add(c);
        b.add(Box.createHorizontalGlue());
        return b;
    }

    public void display(final PlayerID id, final boolean nonCombat)
    {
        
        super.display(id);
        m_nonCombat = nonCombat;
        
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                removeAll();
                m_actionLabel.setText(id.getName() +
                                      (nonCombat ? " non combat" : " combat") + " move");
                add(leftBox(m_actionLabel));
                add(leftBox(new JButton(CANCEL_MOVE_ACTION)));
                add(leftBox(new JButton(DONE_MOVE_ACTION)));
                add(Box.createVerticalStrut(15));


                add(m_undoableMovesPanel);
                add(Box.createGlue());

                SwingUtilities.invokeLater(REFRESH);
        
            }
        
        });
        
    }

    private IMoveDelegate getDelegate()
    {
        return (IMoveDelegate) m_bridge.getRemote();
    }
    
    private void updateMoves()
    {
        m_undoableMovesPanel.setMoves(getDelegate().getMovesMade());
    }

    public MoveDescription waitForMove(IPlayerBridge bridge)
    {
        setUp(bridge);
        
        
        waitForRelease();
        
        cleanUp();
        
        MoveDescription rVal = m_moveMessage;
        m_moveMessage = null;
        return rVal;
        
    }

    private void setUp(final IPlayerBridge bridge)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                s_logger.fine("setup");
                
                setFirstSelectedTerritory(null);
                m_forced = null;
                m_bridge = bridge;
                updateMoves();

                if(m_listening)
                    throw new IllegalStateException("Not listening");
                m_listening = true;
                
                getMap().addMapSelectionListener(MAP_SELECTION_LISTENER);
                getMap().addUnitSelectionListener(UNIT_SELECTION_LISTENER);
                getMap().addMouseOverUnitListener(MOUSE_OVER_UNIT_LISTENER);
                
                String key = MOVE_PANEL_CANCEL;
                getRootPane().getActionMap().put(key, CANCEL_MOVE_ACTION);
                getRootPane().getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0), key);
            }
        
        });

        
    }

    private void cleanUp()
    {

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                s_logger.fine("cleanup");
                
                if(!m_listening)
                    throw new IllegalStateException("Not listening");
                m_listening = false;
                
                
                getMap().removeMapSelectionListener(MAP_SELECTION_LISTENER);
                getMap().removeUnitSelectionListener(UNIT_SELECTION_LISTENER);
                getMap().removeMouseOverUnitListener(MOUSE_OVER_UNIT_LISTENER);
                
                getMap().setUnitHighlight(null, null);
                
                m_bridge = null;
                m_selectedUnits.clear();
                updateRouteAndMouseShadowUnits(null);
                
                CANCEL_MOVE_ACTION.setEnabled(false);
                JComponent rootPane = getRootPane();
                if(rootPane != null)
                {
                    
                    rootPane.getInputMap().put(KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0),null);     
                }
                m_forced = null;
                
                removeAll();
                REFRESH.run();

            }
        
        });
        


    }


    private final AbstractAction doneMove = new AbstractAction("Done")
    {
        public void actionPerformed(ActionEvent e)
        {
            if(m_undoableMovesPanel.getCountOfMovesMade() == 0)
            {
                int rVal = JOptionPane.showConfirmDialog(JOptionPane.getFrameForComponent( MovePanel.this), "Are you sure you dont want to move?", "End Move", JOptionPane.YES_NO_OPTION);
                if(rVal != JOptionPane.YES_OPTION)
                {
                    return;
                }

            }
            
            m_moveMessage = null;
            release();
            
        }
    };
    
    private final Action DONE_MOVE_ACTION = new WeakAction("Done", doneMove);

    public void cancelMove()
    {
        CANCEL_MOVE_ACTION.actionPerformed(null);
    }

    public void setActive(boolean active)
    {
      super.setActive(active);
      SwingUtilities.invokeLater(new Runnable()
    {
    
        public void run()
        {
            CANCEL_MOVE_ACTION.actionPerformed(null);
        }
    
    });
      
    }

    
    private final Action cancelMove =  new AbstractAction(
        "Cancel")
    {
        public void actionPerformed(ActionEvent e)
        {
            setFirstSelectedTerritory(null);
            m_mouseCurrentTerritory = null;
            m_forced = null;
            m_selectedUnits.clear();
            m_unresolvedUnitTypes = Collections.emptyList();
            m_frame.clearStatusMessage();
            getMap().showMouseCursor();
            m_currentCursorImage = null;
            updateRouteAndMouseShadowUnits(null);
            
            this.setEnabled(false);
            getMap().setMouseShadowUnits(null);
            
        }};
    
    private final AbstractAction CANCEL_MOVE_ACTION = new WeakAction("Cancel", cancelMove);

    /**
     * Return the units that to be unloaded for this route.
     * If needed will ask the user what transports to unload.
     * This is needed because the user needs to be able to select what transports to unload
     * in the case where some transports have different movement, different
     * units etc
     */
    private Collection<Unit> getUnitsToUnload(Route route, Collection<Unit> unitsToMove)
    {
      List<Unit> candidateTransports = new ArrayList<Unit>();
      
      Collection<UnitCategory> unitsToMoveCategories = UnitSeperator.categorize(unitsToMove);
      
      //get the transports with units that we own
      //these transports can be unloaded
      Iterator<Unit> mustMoveWithIter = m_mustMoveWithDetails.getMustMoveWith().keySet().iterator();
      while (mustMoveWithIter.hasNext())
      {
        Unit transport = mustMoveWithIter.next();
        Collection<Unit> transporting = m_mustMoveWithDetails.getMustMoveWith().get(transport);
        if(transporting == null)
          continue;

        //are some of the transported units of the same type we want to unload?
        if(!Util.someIntersect(UnitSeperator.categorize(transporting), unitsToMoveCategories))
            continue;
        
        candidateTransports.add(transport);
      }

      if(candidateTransports.size() == 0)
          return Collections.emptyList();
      
      //just one transport, dont bother to ask
      if(candidateTransports.size() == 1)
          return unitsToMove;
      
      CompositeMatch<Unit> unloadable = getMovableMatch(route);
      
      if(unitsToMove.size() == route.getStart().getUnits().countMatches(unloadable))
          return unitsToMove;

      //are the transports all of the same type
      //if they are, then don't ask
      Collection<UnitCategory> categories = UnitSeperator.categorize(candidateTransports, m_mustMoveWithDetails.getMustMoveWith(), m_mustMoveWithDetails.getMovement() );
      if(categories.size() == 1)
          return unitsToMove;
     
      // choosing what transports to unload
      UnitChooser chooser = new UnitChooser(candidateTransports, m_mustMoveWithDetails.getMustMoveWith(), m_mustMoveWithDetails.getMovement(), m_bridge.getGameData(), getMap().getUIContext());
      chooser.setTitle("What transports do you want to unload");
            
      int option = JOptionPane.showOptionDialog(getTopLevelAncestor(),
                                                chooser, "What transports do you want to unload",
                                                JOptionPane.OK_CANCEL_OPTION,
                                                JOptionPane.PLAIN_MESSAGE, null, null, null);
       
      
      if(option != JOptionPane.OK_OPTION)
        return Collections.emptyList();

      Collection<Unit> choosenTransports = chooser.getSelected();
      
      Collection<Unit> allUnitsInSelectedTransports = new ArrayList<Unit>();
      Iterator choosenIter = choosenTransports.iterator();
      while (choosenIter.hasNext())
      {
        Unit transport = (Unit)choosenIter.next();
        Collection<Unit> transporting =  m_mustMoveWithDetails.getMustMoveWith().get(transport);
        if(transporting != null)
          allUnitsInSelectedTransports.addAll(transporting);
      }
      
      //all our units
      allUnitsInSelectedTransports = Match.getMatches(allUnitsInSelectedTransports, Matches.unitIsOwnedBy(getCurrentPlayer()));
      
      //we have selected some units before asking what transports to unload
      //now we have selected all our units in the transports we want to unload.
      //we must now find enough units in the selected transports to fulfill our 
      //original movement quota
      List<Unit> rVal = new ArrayList<Unit>();
      
      for(Unit selected : unitsToMove)
      {
          for(Unit candidate : allUnitsInSelectedTransports)
          {
              if(selected.getType().equals(candidate.getType()) &&
                 selected.getHits() == candidate.getHits())
              {
                  allUnitsInSelectedTransports.remove(candidate);
                  rVal.add(candidate);
                  break;
              }
              
          }
          
      }
      return rVal;
      
    }

    private CompositeMatch<Unit> getMovableMatch(final Route route)
    {
        CompositeMatch<Unit> movable = new CompositeMatchAnd<Unit>();
        movable.add(Matches.unitIsOwnedBy(getCurrentPlayer()));
        movable.add(Matches.UnitIsNotFactory);
        if(!m_nonCombat)
            movable.add(new InverseMatch<Unit>( Matches.UnitIsAA));
        if(route != null)
        {
            Match<Unit> enoughMovement = new Match<Unit>()
            {
                public boolean match(Unit u)
                {
                    return m_mustMoveWithDetails.getMovement().getInt(u) >= route.getLength();
                }

            };
            if(MoveValidator.isUnload(route) && route.getLength() == 1)
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
            if(water && !load)
                movable.add(Matches.UnitIsNotLand);
            if(!water)
                movable.add(Matches.UnitIsNotSea);
        }
        return movable;
    }
    
    /**
     * Allow the user to select specific units, if for example some units
     * have different movement
     */
    private boolean allowSpecificUnitSelection(Collection<Unit> units, final Route route, boolean mustQueryUser, Match<Collection<Unit>> matchCriteria)
    {
        Collection<Unit> ownedUnits = getFirstSelectedTerritory().getUnits().getMatches(getMovableMatch(route));
        
        if (!mustQueryUser)
        {
            Set<UnitCategory> categories = UnitSeperator.categorize(ownedUnits, m_mustMoveWithDetails.getMustMoveWith(), m_mustMoveWithDetails.getMovement());
            
            for(UnitCategory category1 : categories)
            {
                //we cant move these, dont bother to check
                if(category1.getMovement() == 0)
                    continue;
                
                for(UnitCategory category2 : categories)
                {
                    //we cant move these, dont bother to check
                    if(category2.getMovement() == 0)
                        continue;
                    
                    //if we find that two categories are compatable, and some units
                    //are selected from one category, but not the other
                    //then the user has to refine his selection
                    if(category1 != category2 &&
                       category1.getType() == category2.getType() &&
                       !category1.equalsIgnoreMovement(category2)
                       )
                    {
                        //if we are moving all the units from both categories, then nothing to choose
                        if(units.containsAll(category1.getUnits()) &&
                           units.containsAll(category2.getUnits()))
                            continue;
                        //if we are moving some of the units from either category, then we need to stop
                        if(!Util.intersection(category1.getUnits(), units).isEmpty() ||
                           !Util.intersection(category2.getUnits(), units).isEmpty() )
                        {
                            mustQueryUser = true;
                        }
                    }
                }
                
            }
        }
        
        if(mustQueryUser)
        {

            List<Unit> defaultSelections = new ArrayList<Unit>(units.size());

            if (MoveValidator.isLoad(route))
            {
                final Collection<Unit> transportsToLoad = new ArrayList<Unit>(getTransportsToLoad(route, units, false));

                defaultSelections.addAll(getDelegate().mapTransports(route, units, transportsToLoad).keySet());
            }
            else
            {
                defaultSelections.addAll(units);
            }

            CompositeMatch<Unit> rightUnitTypeMatch = new CompositeMatchOr<Unit>();
            for(Unit unit : units)
            {
                rightUnitTypeMatch.add(Matches.unitIsOfType(unit.getType()));
            }
            List<Unit> candidateUnits = Match.getMatches(ownedUnits, rightUnitTypeMatch);
            
            // sort units in preferred order
            sortByDecreasingTransportCapacityIncreasingMovement(defaultSelections, m_mustMoveWithDetails);
            UnitChooser chooser = new UnitChooser(candidateUnits, defaultSelections, m_mustMoveWithDetails.getMustMoveWith(),
                    m_mustMoveWithDetails.getMovement(), m_bridge.getGameData(), false, getMap().getUIContext(), matchCriteria);

            
            
            String text = "Select units to move from " + getFirstSelectedTerritory() + ".";
            int option = JOptionPane.showOptionDialog(getTopLevelAncestor(),
                                            chooser, text,
                                            JOptionPane.OK_CANCEL_OPTION,
                                            JOptionPane.PLAIN_MESSAGE, null, null, null);
            
            if(option != JOptionPane.OK_OPTION)
            {
                units.clear();
                return false;
            }
            
            
            units.clear();
            units.addAll(chooser.getSelected(false));
        }

        // add the dependent units
        List<Unit> unitsCopy = new ArrayList<Unit>(units);
        for(Unit unit : unitsCopy)
        {
            Collection<Unit> forced = m_mustMoveWithDetails.getMustMoveWith().get(unit);
            if(forced != null)
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

    private Route getRoute(Territory start, Territory end)
    {
        if (m_forced == null)
            return getRouteNonForced(start, end);
        else
            return getRouteForced(start, end);
    }

    /**
     * Get the route inculding the territories that we are forced to move through.
     */
    private Route getRouteForced(Territory start, Territory end)
    {
        if (m_forced == null || m_forced.size() == 0)
            throw new IllegalStateException("No forced territories:" + m_forced +
                                            " end:" + end + " start:" + start);

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
    @SuppressWarnings("unchecked")
    private Route getRouteNonForced(Territory start, Territory end)
    {
        Route defaultRoute = getData().getMap().getRoute(start, end);
        if (defaultRoute == null)
            throw new IllegalStateException("No route between:" + start +
                                            " and " + end);

        //if the start and end are in water, try and get a water route
        //dont force a water route, since planes may be moving
        if (start.isWater() && end.isWater())
        {
            Route waterRoute = getData().getMap().getRoute(start, end,
                Matches.TerritoryIsWater);
            if (waterRoute != null &&
                waterRoute.getLength() == defaultRoute.getLength())
                return waterRoute;
        }

        //ignore the end territory in our tests
        //it must be in the route, so it shouldn't affect the route choice
        Match<Territory> territoryIsEnd = Matches.territoryIs(end);

        // No aa guns on route predicate
        Match<Territory> noAA = new InverseMatch<Territory>(Matches.territoryHasEnemyAA(getCurrentPlayer(), getData()));
            

        // No neutral countries on route predicate
        Match<Territory> noNeutral = new InverseMatch<Territory>(new CompositeMatchAnd<Territory>(Matches.TerritoryIsNeutral));

        //no enemy units on the route predicate
        Match<Territory> noEnemy = new InverseMatch(Matches.territoryHasEnemyUnits(getCurrentPlayer(), getData()));
        
        
        //these are the conditions we would like the route to satisfy, starting
        //with the most important
        List<Match<Territory>> tests = new ArrayList( Arrays.asList(
                //best if no enemy and no neutral
                new CompositeMatchOr<Territory>(noEnemy, noNeutral),
                //we will be satisfied if no aa and no neutral
                new CompositeMatchOr<Territory>(noAA, noNeutral),
                //single matches
                noEnemy, noAA, noNeutral));
        
        
        //remove matches that already pass
        //ignore the end
        for(Iterator<Match<Territory>> iter = tests.iterator(); iter.hasNext(); ) {
            Match<Territory> current = iter.next();
            if(defaultRoute.allMatch(new CompositeMatchOr(current, territoryIsEnd))) {
                iter.remove();
            }
        }
        
        
        for(Match<Territory> t : tests) {            
            Route testRoute = getData().getMap().getRoute(start, end, new CompositeMatchOr<Territory>(t, territoryIsEnd));
            
            if(testRoute != null && testRoute.getLength() == defaultRoute.getLength())
                return testRoute;
        }
            
        
        
        return defaultRoute;
    }
    
    
    private void updateUnitsThatCanMoveOnRoute(Collection<Unit> units, final Route route)
    {
        m_unresolvedUnitTypes = Collections.emptyList();

        if(route.getLength() == 0)
        {
            m_frame.clearStatusMessage();
            getMap().showMouseCursor();
            m_currentCursorImage = null;
            m_unitsThatCanMoveOnRoute = new ArrayList<Unit>(units);
            return;
        }

        getMap().hideMouseCursor();

        List<Unit> bestUnits = new ArrayList<Unit>(units.size());

        CompositeMatch<Unit> movableMatch = getMovableMatch(route);

        // get candidate units at the start of the route
        List<Unit> candidateUnits = Match.getMatches(route.getStart().getUnits().getUnits(), movableMatch);

        // sort candidate units in increasing movement order
        sortByDecreasingTransportCapacityIncreasingMovement(candidateUnits, m_mustMoveWithDetails);

        // loop through selected units and replace with best candidate where possible
        for (Unit unit: units)
        {
            Unit unitToAdd = unit;
            Iterator<Unit> candidateIter = candidateUnits.iterator();
            // find first matching unit
            while (candidateIter.hasNext())
            {
                Unit checkUnit = (Unit)candidateIter.next();
                if(!unit.getType().equals(checkUnit.getType()))
                    continue;
                // type, owner, and movement matches, do the swap
                unitToAdd = checkUnit;
                candidateIter.remove();
                break;
            }
            bestUnits.add(unitToAdd);
        }
            
        // add the dependent units temporarily for validation
        Collection<Unit> forcedDependents = new ArrayList<Unit>();
        for(Unit unit : bestUnits)
        {
            Collection<Unit> forced = m_mustMoveWithDetails.getMustMoveWith().get(unit);
            if(forced != null)
            {
                // add dependent if necessary 
                for (Unit dependent : forced)
                {
                    if (bestUnits.indexOf(dependent) == -1)
                        forcedDependents.add(dependent);
                }
            }
        }
        bestUnits.addAll(forcedDependents);

        // getTransportsToLoad causes prompting... need to suppress that here
        MoveValidationResult result = getDelegate().validateMove(bestUnits,route,getCurrentPlayer(),getTransportsToLoad(route, bestUnits, true));

        // remove dependent units that we just added so they don't show up in shadow units
        bestUnits.removeAll(forcedDependents);
        
        Collection<Unit> resultUnits = new ArrayList<Unit>(bestUnits.size());

        if (result.isMoveValid())
        {
            // valid move
            m_frame.clearStatusMessage();
            m_currentCursorImage = null;
            resultUnits.addAll(bestUnits);
        }
        else
        {
            String errorMsg = null;
            String warningMsg = null;
            StringBuilder statusMsg = new StringBuilder(100);

            // invalid move
            if (result.hasError())
            {
                // a show stopper
                errorMsg = result.getError();
            }
            else
            {
                if (result.getDisallowedUnitWarnings().size() == 0
                        && result.getUnresolvedUnitWarnings().size() == 0)
                    throw new IllegalStateException("Uninitialized MoveValidationResult object!");

                // no show stoppers, but some units had problems 

                resultUnits.addAll(bestUnits);
                resultUnits.removeAll(result.getDisallowedUnits());

                // remove any units from unresolved list that are also in disallowed list
                result.removeAnyUnresolvedUnitsThatAreDisallowed();

                if (result.hasUnresolvedUnits())
                {
                    // for unresolved units, paint a question mark over the unit type
                    warningMsg = result.getUnresolvedUnitWarning(0);
                    m_unresolvedUnitTypes = result.getUnresolvedUnitTypes();
                }
                else if (result.hasDisallowedUnits())
                {
                    // if disallowed units, but no unresolved units, then just remove the disallowed units
                    if (resultUnits.isEmpty())
                    {
                        // no units left; show an error
                        errorMsg = result.getDisallowedUnitWarning(0);
                    }
                    else
                    {
                        // some units are left; show a warning 
                        warningMsg = result.getDisallowedUnitWarning(0);
                    }
                }
            }

            int numProblems = result.getTotalWarningCount() - (result.hasError() ? 0 : 1);

            String numWarningsMsg = numProblems > 0 ? ("; "+ numProblems + " other warning" + (numProblems==1 ? "" : "s") + " not shown") : "";
            if (errorMsg != null)
            {
                statusMsg.append(errorMsg).append(numWarningsMsg);
                m_frame.setStatusErrorMessage(statusMsg.toString());
                m_currentCursorImage = getMap().getErrorImage();
            }
            else if (warningMsg != null)
            {
                statusMsg.append(warningMsg).append(numWarningsMsg);
                m_frame.setStatusWarningMessage(statusMsg.toString());
                m_currentCursorImage = getMap().getWarningImage();
            }
        }

        m_unitsThatCanMoveOnRoute = resultUnits;

    }

    /**
     * Route can be null.
     */
    private void updateRouteAndMouseShadowUnits(final Route route)
    {
        getMap().setRoute(route, m_mouseSelectedPoint, m_mouseCurrentPoint, m_currentCursorImage);
        if(route == null)
            getMap().setMouseShadowUnits(null);
        else
        {
            getMap().setMouseShadowUnits(m_unitsThatCanMoveOnRoute, m_unresolvedUnitTypes);
        }
    }

    /**
     * Allow the user to select what transports to load.
     * 
     * If null is returned, the move should be cancelled.
     */
    private Collection<Unit> getTransportsToLoad(Route route, Collection<Unit> unitsToLoad, boolean disablePrompts)
    {
        Match<Unit> alliedTransports = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.alliedUnit(getCurrentPlayer(), m_bridge.getGameData()));
        Collection<Unit> transports = Match.getMatches(route.getEnd().getUnits().getUnits(), alliedTransports);
        //nothing to choose
        if(transports.isEmpty())
            return Collections.emptyList();
        //only one, no need to choose
        if(transports.size() == 1)
            return transports;

        MustMoveWithDetails endMustMoveWith = getDelegate().getMustMoveWith(route.getEnd(), route.getEnd().getUnits().getUnits());
      
        //all the same type, dont ask unless we have more than 1 unit type
        if(UnitSeperator.categorize(transports, endMustMoveWith.getMustMoveWith(), endMustMoveWith.getMovement()).size() == 1 
           && unitsToLoad.size() == 1     )
            return transports;
          
        int minTransportCost = 5;
        for(Unit unit : unitsToLoad)
        {
            minTransportCost = Math.min(minTransportCost, UnitAttachment.get(unit.getType()).getTransportCost());
        }
      
      
        List<Unit> candidateTransports = new ArrayList<Unit>();
        List<Unit> candidateAlliedTransports = new ArrayList<Unit>();

        // find the transports with space left
        Iterator transportIter = transports.iterator();
        while (transportIter.hasNext())
        {
            Unit transport = (Unit) transportIter.next();
            int capacity = getDelegate().getTransportTracker().getAvailableCapacity(transport);
            if(capacity >= minTransportCost)
            {
                if (transport.getOwner().equals(getCurrentPlayer()))
                    candidateTransports.add(transport);
                else
                    candidateAlliedTransports.add(transport);
            }
        }

        // Determine common-sense candidate transport load order.
        // For both combat and non-combat, always fill half-full transports first.
        // Next, for both combat and non-combat, transports that have been 
        //   moved within loading range this turn are likely candidates. 
        // Therefore, sort by (decreasing cost, increasing movement).
      
        sortByDecreasingTransportCapacityIncreasingMovement(candidateTransports, endMustMoveWith);
        sortByDecreasingTransportCapacityIncreasingMovement(candidateAlliedTransports, endMustMoveWith);
        // append allied transports to list - they are a last resort
        candidateTransports.addAll(candidateAlliedTransports);

        if(candidateTransports.size() <= 1)
            return candidateTransports;

        // m_selectedUnits are the units we are loading
        List<Unit> availableUnits = new ArrayList<Unit>(m_selectedUnits);

        // Make reasonable default selections based on candidate transport load order
        Collection<Unit> defaultSelections = new ArrayList<Unit>();
        transportIter = candidateTransports.iterator();
        while (availableUnits.size() > 0 && transportIter.hasNext())
        {
            // Use capacity/cost formulas to fill as few transports as possible.
            Unit transport = (Unit)transportIter.next();
            // no movement left; not a good candidate
            if (endMustMoveWith.getMovement().getInt(transport) == 0)
                continue;
            int capacity = getDelegate().getTransportTracker().getAvailableCapacity(transport);
            boolean isSelected = false;

            // Loop through selected units and assign them to transports.
            Iterator unitIter = availableUnits.iterator();
            while (unitIter.hasNext() && (capacity >= minTransportCost))
            {
                Unit unit = (Unit)unitIter.next();
                int unitCost = UnitAttachment.get(unit.getType()).getTransportCost();
                if(capacity >= unitCost)
                {
                    isSelected = true;
                    unitIter.remove();
                    capacity -= unitCost;
                }
            }
            if (isSelected)
                defaultSelections.add(transport);
        } 

        // return defaults if we aren't allowed to prompt
        if (disablePrompts)
            return defaultSelections;

        // If we've filled all the transports, then no user intervention is required.
        // It is possible to make "wrong" decisions if there are mixed unit types and
        //   mixed transport categories, but there is no UI to manage that anyway.
        //   Players will need to load incrementally in such cases.
        if (defaultSelections.containsAll(candidateTransports))
            return defaultSelections;

        // If there is only one candidate transport category, then don't prompt
        // This assumes we want to fill as few transports as possible, which is usually a reasonable assumption.
        // If you want a bunch of half-full transports than do them separately.
        if(UnitSeperator.categorize(candidateTransports, endMustMoveWith.getMustMoveWith(), endMustMoveWith.getMovement()).size() == 1)
            return defaultSelections;

        // choosing what units to LOAD.
        UnitChooser chooser = new UnitChooser(candidateTransports, defaultSelections, endMustMoveWith.getMustMoveWith(), endMustMoveWith.getMovement(), m_bridge.getGameData(), false, getMap().getUIContext());
        chooser.setTitle("What transports do you want to load");
        int option = JOptionPane.showOptionDialog(getTopLevelAncestor(),
                                                  chooser, "What transports do you want to load",
                                                  JOptionPane.OK_CANCEL_OPTION,
                                                  JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (option != JOptionPane.OK_OPTION)
            return null;


        return chooser.getSelected(false);
    }

    private void sortByDecreasingTransportCapacityIncreasingMovement(final List<Unit> units, final MustMoveWithDetails mustMoveWith)
    {
        if(units.isEmpty())
            return;
        
        Comparator<Unit> increasingMovement = new Comparator<Unit>()
        {
            public int compare(Unit u1, Unit u2)
            {
                Collection transporting1 = (Collection) mustMoveWith.getMustMoveWith().get(u1);
                Collection transporting2 = (Collection) mustMoveWith.getMustMoveWith().get(u2);
                int cost1 = MoveValidator.getTransportCost(transporting1);
                int cost2 = MoveValidator.getTransportCost(transporting2);

                if (transporting1 == null)
                    transporting1 = Collections.emptyList();
                if (transporting2 == null)
                    transporting2 = Collections.emptyList();

                // see if dependents are also selected
                int hasDepends1 = units.containsAll(transporting1) ? 1 : 0;
                int hasDepends2 = units.containsAll(transporting2) ? 1 : 0;
                if (hasDepends1 != hasDepends2)
                    return hasDepends1 - hasDepends2;

                if (cost1 != cost2)
                    return cost2 - cost1;

                int left1 = mustMoveWith.getMovement().getInt(u1);
                int left2 = mustMoveWith.getMovement().getInt(u2);

                return left1 - left2;                 
            }
        };
         
        Collections.sort(units,increasingMovement);
    }
    
    private final UnitSelectionListener UNIT_SELECTION_LISTENER = new UnitSelectionListener()
    {
    
        public void unitsSelected(List<Unit> units, Territory t, MouseDetails me)
        {
            if(!m_listening)
                return;
            
            //check if we can handle this event, are we active?
            if(!getActive())
                return;
            if(t == null)
                return;
            
            boolean rightMouse = me.isRightButton();
            boolean noSelectedTerritory = (m_firstSelectedTerritory == null);
            boolean isFirstSelectedTerritory = (m_firstSelectedTerritory == t);

            //de select units            
            if(rightMouse && !noSelectedTerritory)
                deselectUnits(units, t, me);
            //select units            
            else if(!rightMouse && ( noSelectedTerritory || isFirstSelectedTerritory))
                selectUnitsToMove(units, t, me);
            else if(!rightMouse && me.isControlDown() && !isFirstSelectedTerritory)
                selectWayPoint(t);
            else if(!rightMouse && !noSelectedTerritory && !isFirstSelectedTerritory)
                selectEndPoint(t);
            
        }

        private void selectUnitsToMove(List<Unit> units, Territory t, MouseDetails me)
        {
            
            //are any of the units ours, note - if no units selected thats still ok
            for(Unit unit : units)
            {
                if(!unit.getOwner().equals(getCurrentPlayer()))
                {
                    return;
                }
            }
            // null route for basic match criteria only
            CompositeMatch<Unit> unitsToMoveMatch = getMovableMatch(null);
            
            if(units.isEmpty() && m_selectedUnits.isEmpty())
            {
                if(!me.isShiftDown())
                {
                    
                    List<Unit> unitsToMove = t.getUnits().getMatches(unitsToMoveMatch);
                    
                    if(unitsToMove.isEmpty())
                        return;
                    
                    String text = "Select units to move from " + t.getName();
                    
                    //MustMoveWithDetails mustMoveWith = getDelegate().getMustMoveWith(t, unitsToMove);
                    UnitChooser chooser = new UnitChooser(unitsToMove, m_selectedUnits,
                            null, null 
                            ,getData(),  false, getMap().getUIContext() );
                                        
                    int option = JOptionPane.showOptionDialog(getTopLevelAncestor(),
                            chooser, text,
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.PLAIN_MESSAGE, null, null, null);
                    
                    if (option != JOptionPane.OK_OPTION)
                        return;
                    if(chooser.getSelected(false).isEmpty())
                        return;
                    
                    m_selectedUnits.addAll(chooser.getSelected(false));
                }

            }
            
            
            if(getFirstSelectedTerritory() == null)
            {
                setFirstSelectedTerritory(t);
                
                m_mouseSelectedPoint = me.getMapPoint();
                m_mouseCurrentPoint = me.getMapPoint();
                
                CANCEL_MOVE_ACTION.setEnabled(true);                
            }

            if(!getFirstSelectedTerritory().equals(t))
                throw new IllegalStateException("Wrong selected territory");
            
            //add all
            if(me.isShiftDown())
            {
                CompositeMatch<Unit> ownedNotFactory = unitsToMoveMatch;
                m_selectedUnits.addAll(t.getUnits().getMatches(ownedNotFactory));
            }
            else if(me.isControlDown())
            {
                m_selectedUnits.addAll(units);
            }
            //add one
            else
            {
                // best candidate unit for route is chosen dynamically later
                // check for alt key - add 1/10 of total units (useful for splitting large armies)
                int iterCount = (me.isAltDown()) ? (int)Math.max(1, Math.floor(units.size() / 10)) : 1;
                int addCount = 0;
                
                for(Unit unit : units)
                {
                    if(!m_selectedUnits.contains(unit))
                    {
                        m_selectedUnits.add(unit);
                        addCount++;
                        if (addCount >= iterCount) break;
                    }
                }
            }

            Route route = getRoute(getFirstSelectedTerritory(), t);
            // update unitsThatCanMoveOnRoute
            updateUnitsThatCanMoveOnRoute(m_selectedUnits, route);
            
            updateRouteAndMouseShadowUnits(route);
        }

        private void deselectUnits(List<Unit> units, Territory t, MouseDetails me)
        {         

            Collection<Unit> unitsToRemove = new ArrayList<Unit>(m_selectedUnits.size());
            
            //we have right clicked on a unit stack in a different territory
            if(!getFirstSelectedTerritory().equals(t))
                units = Collections.emptyList();
            
            //remove the dependent units so we don't have to micromanage them
            List<Unit> unitsWithoutDependents = new ArrayList<Unit>(m_selectedUnits);
            for(Unit unit : m_selectedUnits)
            {
                Collection<Unit> forced = m_mustMoveWithDetails.getMustMoveWith().get(unit);
                if(forced != null)
                    unitsWithoutDependents.removeAll(forced);
            }

            //no unit selected, remove the most recent, but skip dependents
            if(units.isEmpty())
            {
                if(me.isControlDown())
                    m_selectedUnits.clear();
                else {
                    // check for alt key - remove 1/10 of total units (useful for splitting large armies)
                    int iterCount = (me.isAltDown()) ? (int)Math.max(1, Math.floor(unitsWithoutDependents.size() / 10)) : 1;
                    
                    //remove the last n elements
                    for (int i=0; i<iterCount; i++) 
                        unitsToRemove.add( unitsWithoutDependents.get(unitsWithoutDependents.size() -1 ) );
                }
            }
            //we have actually clicked on a specific unit
            else
            {
                //remove all if control is down
                if(me.isControlDown())
                {
                    unitsToRemove.addAll(units);
                }
                //remove one
                else
                {
                    if(!getFirstSelectedTerritory().equals(t))
                        throw new IllegalStateException("Wrong selected territory");
                    
                    // doesn't matter which unit we remove since units are assigned to routes later
                    // check for alt key - remove 1/10 of total units (useful for splitting large armies)
                    int iterCount = (me.isAltDown()) ? (int)Math.max(1, Math.floor(units.size() / 10)) : 1;
                    int remCount = 0;
                    
                    for(Unit unit : units)
                    {
                        if(m_selectedUnits.contains(unit))
                        {
                            unitsToRemove.add(unit);
                            remCount++;
                            if (remCount >= iterCount) break;
                        }
                    }
                }
            }

            // perform the remove
            m_selectedUnits.removeAll(unitsToRemove);

            if(m_selectedUnits.isEmpty())
            {
                //nothing left, cancel move
                CANCEL_MOVE_ACTION.actionPerformed(null);
            }
            else
            {
                // update unitsThatCanMoveOnRoute
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

            updateRouteAndMouseShadowUnits(getRoute(getFirstSelectedTerritory(),
                                 getFirstSelectedTerritory()));
        }
        
        private void selectEndPoint(Territory territory)
        {
            final Route route = getRoute(getFirstSelectedTerritory(), territory);
            Collection<Unit> units = m_unitsThatCanMoveOnRoute;
            if (units.isEmpty())
            {
                CANCEL_MOVE_ACTION.actionPerformed(null);
                return;
            }
    
            Collection<Unit> transports = null;
            if(MoveValidator.isLoad(route) && Match.someMatch(units, Matches.UnitIsLand))
            {
              transports = getTransportsToLoad(route, units, false);
              if(transports == null)
                  return;

              if (! m_unresolvedUnitTypes.isEmpty())
              {
                  Match<Unit> unresolvedUnitMatch = new Match<Unit>()
                  {
                      public boolean match(Unit u)
                      {
                          return m_unresolvedUnitTypes.contains(u.getType());
                      }
                  };

                  final Collection<Unit> transportsToLoad = new ArrayList<Unit>(transports);
                  Match<Collection<Unit>> enoughTransportsMatch = new Match<Collection<Unit>>()
                  {
                      public boolean match(Collection<Unit> units)
                      {
                          Map<Unit,Unit> unitsToTransports = getDelegate().mapTransports(route, units, transportsToLoad);
                          return unitsToTransports.keySet().containsAll(units);
                      }
                  };
                  
                  Collection<Unit> unresolvedUnits = Match.getMatches(units, unresolvedUnitMatch);

                  Collection<Unit> newUnits = new ArrayList<Unit>(units);
                  newUnits.removeAll(unresolvedUnits);
                  if (!allowSpecificUnitSelection(unresolvedUnits,route, true, enoughTransportsMatch))
                      return;

                  newUnits.addAll(unresolvedUnits);
                  units.retainAll(newUnits);

                  if (units.isEmpty())
                      return;
              }

            }
            else if(MoveValidator.isUnload(route) && Match.someMatch(units, Matches.UnitIsLand))
            {
                CompositeMatch<Unit> unloadableMatch = getMovableMatch(route);
                List<Unit> unloadAble = Match.getMatches(m_selectedUnits,unloadableMatch);
                
                Collection<Unit> canMove = new ArrayList<Unit>(getUnitsToUnload(route, unloadAble));
                canMove.addAll(Match.getMatches(m_selectedUnits, new InverseMatch<Unit>(unloadableMatch)));
                if(canMove.isEmpty())
                {
                    CANCEL_MOVE_ACTION.actionPerformed(null);
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
                allowSpecificUnitSelection(units, route, false, null);
                
                if(units.isEmpty())
                {
                    CANCEL_MOVE_ACTION.actionPerformed(null);
                    return;
                }
                
            }

            MoveDescription message = new MoveDescription(units, route, transports);
            m_moveMessage = message;
            setFirstSelectedTerritory(null);
            m_mouseCurrentTerritory = null;
            m_forced = null;
            updateRouteAndMouseShadowUnits(null);
            release();
        }
    
    };
    
    
    private final MouseOverUnitListener MOUSE_OVER_UNIT_LISTENER = new MouseOverUnitListener()
    {

        public void mouseEnter(List<Unit> units, Territory territory, MouseDetails me)
        {
            if(!m_listening)
                return;
            
            boolean someOwned = Match.someMatch(units, Matches.unitIsOwnedBy(getCurrentPlayer()));
            boolean isCorrectTerritory = m_firstSelectedTerritory == null || m_firstSelectedTerritory == territory;
            if(someOwned && isCorrectTerritory)
                getMap().setUnitHighlight(units, territory);
            else
                getMap().setUnitHighlight(null, null);
        }
        
    };

    
    private final MapSelectionListener MAP_SELECTION_LISTENER = new
        DefaultMapSelectionListener()
    {
        public void territorySelected(Territory territory, MouseDetails me)
        {
           
        }
        
        public void mouseMoved(Territory territory, MouseDetails me)
        {
            if(!m_listening)
                return;
            
            if (getFirstSelectedTerritory() != null && territory != null)
            {
                m_mouseCurrentPoint= me.getMapPoint();
                Route route = getRoute(getFirstSelectedTerritory(), territory);
                if (m_mouseCurrentTerritory == null || !m_mouseCurrentTerritory.equals(territory))
                    updateUnitsThatCanMoveOnRoute(m_selectedUnits, route);

                updateRouteAndMouseShadowUnits(route);
            }
            else
            {
                //m_frame.clearStatusMessage();
                //getMap().setCursor(Cursor.getDefaultCursor());
            }
            m_mouseCurrentTerritory = territory;
        }
    };

    public String toString()
    {
        return "MovePanel";
    }

    public void undoMove(int moveIndex)
    {
        //clean up any state we may have
        CANCEL_MOVE_ACTION.actionPerformed(null);
        getMap().setRoute(null);

        //undo the move
        String error = getDelegate().undoMove(moveIndex);
        if (error != null)
        {
            JOptionPane.showMessageDialog(getTopLevelAncestor(),
                                          error,
                                          "Could not undo move",
                                          JOptionPane.ERROR_MESSAGE);
        }
        else
        {
            updateMoves();
        }

    }

    private void setFirstSelectedTerritory(Territory firstSelectedTerritory)
    {
        if(firstSelectedTerritory == m_firstSelectedTerritory)
            return;
        
        m_firstSelectedTerritory = firstSelectedTerritory;
        if(m_firstSelectedTerritory == null)
        {
            m_mustMoveWithDetails = null;
        }
        else
        {
            m_mustMoveWithDetails = getDelegate().getMustMoveWith(firstSelectedTerritory, firstSelectedTerritory.getUnits().getUnits());
        }
    }

    private Territory getFirstSelectedTerritory()
    {
        return m_firstSelectedTerritory;
    }

}





/**
 * Avoid holding a strong reference to the action
 * fixes a memory leak in swing.  
 */
class WeakAction extends AbstractAction
{
    private final WeakReference<Action> m_delegate;
    
    WeakAction(String name,Action delegate)
    {
        super(name);
        m_delegate = new WeakReference<Action>(delegate);
    }
    
    
    public void actionPerformed(ActionEvent e)
    {
        Action a = m_delegate.get();
        if(a != null)
            a.actionPerformed(e);
        
    }
    
}
