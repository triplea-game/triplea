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
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveDelegate;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.UndoableMove;
import games.strategy.triplea.delegate.UnitComparator;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.dataObjects.MoveValidationResult;
import games.strategy.triplea.delegate.dataObjects.MustMoveWithDetails;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.util.UnitAutoChooser;
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
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
    private Territory m_selectedEndpointTerritory;
    private Territory m_mouseCurrentTerritory;
    private IPlayerBridge m_bridge;

    private List<Territory> m_forced;
    private boolean m_nonCombat;
    private UndoableMovesPanel m_undoableMovesPanel;
    
    private Point m_mouseSelectedPoint;
    private Point m_mouseCurrentPoint;
    
    //use a LinkedHashSet because we want to know the order
    private final Set<Unit> m_selectedUnits = new LinkedHashSet<Unit>();

    private UnitAutoChooser m_unitAutoChooser = null;
    
    private TreeMap<MoveValidationResult, Integer> m_unitsThatCanMoveResultMap = null;

    //the must move with details for the currently selected territory
    //note this is kep in sync because we do not modify m_selectedTerritory directly
    //instead we only do so through the private setter
    private MustMoveWithDetails m_mustMoveWithDetails = null;

    // cache this so we can update it only when territory/units change
    private List<Unit> m_unitsThatCanMoveOnRoute;

    private Collection<UnitCategory> m_unresolvedUnitCategories;

    private Image m_currentCursorImage;
    private TransportTracker m_transportTracker = null;
    private List<UndoableMove> m_undoableMoves;

    /** Creates new MovePanel */
    public MovePanel(GameData data, MapPanel map, TripleAFrame frame)
    {
        super(data, map);
        m_frame = frame;
        CANCEL_MOVE_ACTION.setEnabled(false);

        m_undoableMovesPanel = new UndoableMovesPanel(data, this);
        m_mouseCurrentTerritory = null;
        m_unitsThatCanMoveOnRoute = Collections.emptyList();
        m_unresolvedUnitCategories = Collections.emptyList();
        m_undoableMoves = Collections.emptyList();
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

        m_transportTracker = new TransportTracker();
        
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
        m_undoableMoves = getDelegate().getMovesMade();
        m_undoableMovesPanel.setMoves(m_undoableMoves);
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
            setSelectedEndpointTerritory(null);
            m_mouseCurrentTerritory = null;
            m_forced = null;
            m_selectedUnits.clear();
            m_unresolvedUnitCategories = Collections.emptyList();
            m_frame.clearStatusMessage();
            getMap().showMouseCursor();
            m_currentCursorImage = null;
            updateRouteAndMouseShadowUnits(null);
            
            this.setEnabled(false);
            getMap().setMouseShadowUnits(null);
            
        }};
    
    private final AbstractAction CANCEL_MOVE_ACTION = new WeakAction("Cancel", cancelMove);


    /**
     * Sort the specified units in preferred movement or unload order.
     */
    private void sortUnitsToMove(final List<Unit> units, final Route route)
    {
        if(units.isEmpty())
            return;
        

        // sort units based on which transports are allowed to unload
        if (MoveValidator.isUnload(route) && Match.someMatch(units, Matches.UnitIsLand))
        {

            Collections.sort(units, UnitComparator.getUnloadableUnitsComparator(units, route, getCurrentPlayer(), true));
        }
        else
        {
            Collections.sort(units, UnitComparator.getMovableUnitsComparator(units, route, getCurrentPlayer(), true));
        }
    }
    
    /**
     * Sort the specified transports in preferred load order.
     */
    private void sortTransportsToLoad(final List<Unit> transports, final Route route)
    {
        if(transports.isEmpty())
            return;

        Collections.sort(transports, UnitComparator.getLoadableTransportsComparator(transports, route, getCurrentPlayer(), true));
    }
    
    /**
     * Sort the specified transports in preferred unload order.
     */
    private void sortTransportsToUnload(List<Unit> transports, Route route)
    {
        if(transports.isEmpty())
            return;
        
        Collections.sort(transports, UnitComparator.getUnloadableTransportsComparator(transports, route, getCurrentPlayer(), true));
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

      if(unitsToUnload.size() == candidateUnits.size())
          return unitsToUnload;

      List<Unit> candidateTransports = Match.getMatches(allUnits, Matches.unitIsTransportingSomeCategories(candidateUnits));
      
      // remove all incapable transports
      Collection<Unit> incapableTransports = Match.getMatches(candidateTransports, Matches.transportCannotUnload(route.getEnd()));
      candidateTransports.removeAll(incapableTransports);

      if(candidateTransports.size() == 0)
          return Collections.emptyList();
      
      //just one transport, dont bother to ask
      if(candidateTransports.size() == 1)
          return unitsToUnload;
      
      //are the transports all of the same type
      //if they are, then don't ask
      Collection<UnitCategory> categories = UnitSeperator.categorize(candidateTransports, true, true, false);
      if(categories.size() == 1)
          return unitsToUnload;
      
      sortTransportsToUnload(candidateTransports, route);

      // unitsToUnload are actually dependents, but need to select transports
      Map<Unit,Unit> unitsToTransports = MoveDelegate.mapTransports(route, unitsToUnload, candidateTransports);
      Set<Unit> defaultSelections = new HashSet<Unit>(unitsToTransports.values());

      // match criteria to ensure that chosen transports will match selected units
      Match<Collection<Unit>> transportsToUnloadMatch = new Match<Collection<Unit>>()
      {
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
              do {
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
                          if(Util.someIntersect(transCategories, unitCategory))
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
              } while (availableUnits.size() > 0 && hasChanged);

              // if we haven't seen all of the transports (and removed them)
              // then there are extra transports that don't fit.
              return (sortedTransports.size() == 0);
          }
      };

      // choosing what transports to unload
      // TODO: stop using mustMoveWith once carrier dependents are supported in TripleAUnit
      UnitChooser chooser = new UnitChooser(candidateTransports, 
                                            defaultSelections, 
                                            m_mustMoveWithDetails.getMustMoveWith(), 
                                            /*categorizeMovement*/ true, 
                                            m_bridge.getGameData(), 
                                            /*allowTwoHit*/ false, 
                                            getMap().getUIContext(), 
                                            transportsToUnloadMatch);      
      chooser.setTitle("What transports do you want to unload");
            
      int option = JOptionPane.showOptionDialog(getTopLevelAncestor(),
                                                chooser, "What transports do you want to unload",
                                                JOptionPane.OK_CANCEL_OPTION,
                                                JOptionPane.PLAIN_MESSAGE, null, null, null);
       
      
      if(option != JOptionPane.OK_OPTION)
          return Collections.emptyList();

      Collection<Unit> choosenTransports = Match.getMatches(chooser.getSelected(), Matches.UnitIsTransport);
      
      List<Unit> allUnitsInSelectedTransports = new ArrayList<Unit>();
      for (Unit transport : choosenTransports)
      {
          Collection<Unit> transporting =  TripleAUnit.get(transport).getTransporting();
          if(transporting != null)
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
                  if(selected.getType().equals(candidate.getType()) &&
                     selected.getHits() == candidate.getHits())
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
          while(candidateIter.hasNext())
          {
              Unit candidate = candidateIter.next();
              if(selected.getType().equals(candidate.getType()) &&
                 selected.getHits() == candidate.getHits())
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
                    return TripleAUnit.get(u).getMovementLeft() >= route.getLength();
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
        if (units != null && !units.isEmpty())
        {
            CompositeMatch<Unit> rightUnitTypeMatch = new CompositeMatchOr<Unit>();
            for(Unit unit : units)
                rightUnitTypeMatch.add(Matches.unitIsOfType(unit.getType()));
            movable.add(rightUnitTypeMatch);
        }
        return movable;
    }
    
    /**
     * Allow the user to select specific units, if for example some units
     * have different movement
     * Units are sorted in preferred order, so units represents the default selections.
     */
    private Collection<Unit> allowSpecificUnitSelection(UnitAutoChooser autoChooser, final Route route)
    {
        List<Unit> units = new ArrayList<Unit>();

        if (autoChooser.solutionCount() == 1)
        {
            units.addAll(autoChooser.getSolution(0, true));
        }
        else
        {
            // multiple solutions or no single exact solution found
            
            Match<Collection<Unit>> unitCategoryCountMatch = autoChooser.getChooserBoundaryMatch();
            CompositeMatch<Collection<Unit>> unitChooserMatch = new CompositeMatchAnd<Collection<Unit>>(unitCategoryCountMatch);
            Match<Collection<Unit>> enoughMovementMatch = new Match<Collection<Unit>>()
            {
                public boolean match(Collection<Unit> c)
                {
                    for (Unit u : c)
                        if (TripleAUnit.get(u).getMovementLeft() < route.getLength())
                            return false;
                    return true;
                }
            };
            unitChooserMatch.add(enoughMovementMatch);

            // create new UnitAutoChooser with movement categorized
            UnitAutoChooser newAutoChooser = new UnitAutoChooser(autoChooser.getCandidateUnits(true),
                                                                 autoChooser.getChosenUnits(),
                                                                 true, true);


            // TODO: stop using mustMoveWith once carrier dependents are supported in TripleAUnit
            UnitChooser chooser = new UnitChooser(newAutoChooser, 
                                                  m_mustMoveWithDetails.getMustMoveWith(), 
                                                  /*categorizeMovement*/ true,
                                                  m_bridge.getGameData(), 
                                                  /*allowTwoHit*/ false, 
                                                  getMap().getUIContext(), 
                                                  unitChooserMatch);

            String text = "Move from " + getFirstSelectedTerritory();
            int option = JOptionPane.showOptionDialog(getTopLevelAncestor(),
                                            chooser, text,
                                            JOptionPane.OK_CANCEL_OPTION,
                                            JOptionPane.PLAIN_MESSAGE, null, null, null);
            
            if(option != JOptionPane.OK_OPTION)
            {
                return units;
            }
            
            
            units.addAll(chooser.getSelected(true));
        }

        return units;
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
        Match<Territory> noEnemy = new InverseMatch<Territory>(Matches.territoryHasEnemyUnits(getCurrentPlayer(), getData()));
        
        
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
        m_unresolvedUnitCategories = Collections.emptyList();

        if(route.getLength() == 0)
        {
            m_frame.clearStatusMessage();
            getMap().showMouseCursor();
            m_currentCursorImage = null;
            m_unitsThatCanMoveOnRoute = new ArrayList<Unit>(units);
            return;
        }

        getMap().hideMouseCursor();

        // get movable units at the start of the route using basic match criteria
        List<Unit> movableUnits = getFirstSelectedTerritory().getUnits().getMatches(getMovableMatch(null, null));
        // sort candidate units in preferred movement order
        sortUnitsToMove(movableUnits, route);

        // create a new UnitAutoChooser to find movable solutions
        m_unitAutoChooser = new UnitAutoChooser(movableUnits, units, true, false);
        Set<Unit> bestUnits = null;

        // sort results in Comparable<MoveValidationResult> order
        m_unitsThatCanMoveResultMap = new TreeMap<MoveValidationResult, Integer>();


        MoveValidationResult result;

        int startIndex, maxCount;

        startIndex = 0;
        maxCount = m_unitAutoChooser.solutionCount();
        for (int i=startIndex; i < maxCount; i++)
        {
            bestUnits = m_unitAutoChooser.getSolution(i, true);
            // getTransportsToLoad causes prompting normally. Suppress that here.
            result = MoveValidator.validateMove(bestUnits,
                                                route,
                                                getCurrentPlayer(),
                                                getTransportsToLoad(route, bestUnits, true),
                                                m_nonCombat,
                                                m_undoableMoves,
                                                getData());
            m_unitsThatCanMoveResultMap.put(result, Integer.valueOf(i));
        }
        // choose best result
        result = m_unitsThatCanMoveResultMap.firstKey();
        int bestSolutionIndex = m_unitsThatCanMoveResultMap.get(result).intValue();
        // remove dependent units that we just added so they don't show up in shadow units
        bestUnits = m_unitAutoChooser.getSolution(bestSolutionIndex, false);

        List<Unit> resultUnits = new ArrayList<Unit>(bestUnits.size());

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
                    m_unresolvedUnitCategories = result.getUnresolvedUnitCategories();
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

        m_selectedUnits.clear();
        m_selectedUnits.addAll(bestUnits);
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
            getMap().setMouseShadowUnits(m_unitsThatCanMoveOnRoute, m_unresolvedUnitCategories);
        }
    }

    /**
     * Allow the user to select what transports to load.
     * 
     * If null is returned, the move should be cancelled.
     */
    private Collection<Unit> getTransportsToLoad(final Route route, final Collection<Unit> unitsToLoad, boolean disablePrompts)
    {
        if (!MoveValidator.isLoad(route))
            return Collections.emptyList();

        Collection<Unit> endOwnedUnits = route.getEnd().getUnits().getUnits();

        MustMoveWithDetails endMustMoveWith = MoveDelegate.getMustMoveWith(route.getEnd(), endOwnedUnits, getData(), getCurrentPlayer());
      
        int minTransportCost = 5;
        for(Unit unit : unitsToLoad)
        {
            minTransportCost = Math.min(minTransportCost, UnitAttachment.get(unit.getType()).getTransportCost());
        }
      
        CompositeMatch<Unit> candidateTransportsMatch = new CompositeMatchAnd<Unit>();
        candidateTransportsMatch.add(Matches.UnitIsTransport);
        candidateTransportsMatch.add(Matches.alliedUnit(getCurrentPlayer(), m_bridge.getGameData()));

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

        //nothing to choose
        if(candidateTransports.isEmpty())
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
        //   - owned, capable transports are chosen first; attempt to fill them
        //   - allied, capable transports are chosen next; attempt to fill them
        //   - finally, incapable transports are chosen last (will generate errors)
        // Note that if any allied transports qualify as defaults, we will always prompt with a 
        // UnitChooser later on so that it is obvious to the player.
        //
        
        boolean useAlliedTransports = false;
        Collection<Unit> capableTransports = new ArrayList<Unit>(candidateTransports);

        // only allow incapable transports for updateUnitsThatCanMoveOnRoute
        //  so that we can have a nice UI error shown if these transports 
        //  are selected, since it may not be obvious
        Collection<Unit> incapableTransports = Match.getMatches(capableTransports, Matches.transportCannotUnload(route.getEnd()));
        capableTransports.removeAll(incapableTransports);

        Match<Unit> alliedMatch = new Match<Unit>() 
        {
            public boolean match(Unit transport)
            {
                return (!transport.getOwner().equals(getCurrentPlayer()));
            }
        };
        Collection<Unit> alliedTransports = Match.getMatches(capableTransports, alliedMatch);
        capableTransports.removeAll(alliedTransports);

        // First, load capable transports
        Map<Unit,Unit> unitsToCapableTransports = MoveDelegate.mapTransports(route, availableUnits, capableTransports);
        for (Unit unit : unitsToCapableTransports.keySet())
        {
            Unit transport = unitsToCapableTransports.get(unit);
            int unitCost = UnitAttachment.get(unit.getType()).getTransportCost();
            availableCapacityMap.add(transport, (-1 * unitCost));
            defaultSelections.add(transport);
        }
        availableUnits.removeAll(unitsToCapableTransports.keySet());

        // Next, load allied transports
        Map<Unit,Unit> unitsToAlliedTransports = MoveDelegate.mapTransports(route, availableUnits, alliedTransports);
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
        //  so that we can have a nice UI error shown if these transports 
        //  are selected, since it may not be obvious
        if (getSelectedEndpointTerritory() == null)
        {
            Map<Unit,Unit> unitsToIncapableTransports = MoveDelegate.mapTransports(route, availableUnits, incapableTransports);
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
            if(candidateTransports.size() == 1)
                return candidateTransports;

            //all the same type, dont ask unless we have more than 1 unit type
            if(UnitSeperator.categorize(candidateTransports, true, true, false).size() == 1 
               && unitsToLoad.size() == 1     )
                return candidateTransports;
          
            // If we've filled all transports, then no user intervention is required.
            // It is possible to make "wrong" decisions if there are mixed unit types and
            //   mixed transport categories, but there is no UI to manage that anyway.
            //   Players will need to load incrementally in such cases.
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
            public boolean match(Collection<Unit> units)
            {
                Collection<Unit> transports = Match.getMatches(units, Matches.UnitIsTransport);
                // prevent too many transports from being selected
                return (transports.size() <= Math.min(unitsToLoad.size(), candidateTransports.size()));
            }
        };

        // TODO: stop using mustMoveWith once carrier dependents are supported in TripleAUnit
        UnitChooser chooser = new UnitChooser(candidateTransports, 
                                              defaultSelections, 
                                              endMustMoveWith.getMustMoveWith(), 
                                              /*categorizeMovement*/ true, 
                                              m_bridge.getGameData(), 
                                              /*allowTwoHit*/ false, 
                                              getMap().getUIContext(), 
                                              transportsToLoadMatch);

        chooser.setTitle("What transports do you want to load");
        int option = JOptionPane.showOptionDialog(getTopLevelAncestor(),
                                                  chooser, "What transports do you want to load",
                                                  JOptionPane.OK_CANCEL_OPTION,
                                                  JOptionPane.PLAIN_MESSAGE, null, null, null);
        if (option != JOptionPane.OK_OPTION)
            return Collections.emptyList();


        return chooser.getSelected(false);
    }

    private TransportTracker getTransportTracker()
    {
        return m_transportTracker;
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
            // basic match criteria only
            CompositeMatch<Unit> unitsToMoveMatch = getMovableMatch(null, null);
            
            if(units.isEmpty() && m_selectedUnits.isEmpty())
            {
                if(!me.isShiftDown())
                {
                    
                    List<Unit> unitsToMove = t.getUnits().getMatches(unitsToMoveMatch);
                    
                    if(unitsToMove.isEmpty())
                        return;
                    
                    String text = "Select units to move from " + t.getName();
                    
                    UnitChooser chooser = new UnitChooser(unitsToMove,
                                                          m_selectedUnits,
                                                          /*mustMoveWith*/ null, 
                                                          /*categorizeMovement*/ false,
                                                          getData(), 
                                                          /*allowTwoHit*/ false, 
                                                          getMap().getUIContext() );
                                        
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
                m_selectedUnits.addAll(Match.getMatches(units, unitsToMoveMatch));
            }
            //add one
            else
            {
                // best candidate unit for route is chosen dynamically later
                // check for alt key - add 1/10 of total units (useful for splitting large armies)
                Collection<Unit> unitsToMove = Match.getMatches(units, unitsToMoveMatch);
                int iterCount = (me.isAltDown()) ? (int)Math.max(1, Math.floor(unitsToMove.size() / 10)) : 1;
                int addCount = 0;
                
                for(Unit unit : unitsToMove)
                {
                    if(!m_selectedUnits.contains(unit))
                    {
                        m_selectedUnits.add(unit);
                        addCount++;
                        if (addCount >= iterCount) break;
                    }
                }
            }

            if (!m_selectedUnits.isEmpty())
            {
                Route route = getRoute(getFirstSelectedTerritory(), t);
                updateUnitsThatCanMoveOnRoute(m_selectedUnits, route);
                updateRouteAndMouseShadowUnits(route);
            }
            else
                setFirstSelectedTerritory(null);
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
                // TODO: change to use TripleAUnit.getDependents() when carrier dependents are supported
                Collection<Unit> forced = m_mustMoveWithDetails.getMustMoveWith().get(unit);
                if(forced != null)
                    unitsWithoutDependents.removeAll(forced);
            }

            //no unit selected, remove the most recent, but skip dependents
            if(units.isEmpty())
            {
                if(me.isControlDown())
                    m_selectedUnits.clear();
                else if (!unitsWithoutDependents.isEmpty())
                {
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
            final List<Unit> units = m_unitsThatCanMoveOnRoute;
            setSelectedEndpointTerritory(territory);
            if (units.isEmpty())
            {
                CANCEL_MOVE_ACTION.actionPerformed(null);
                return;
            }

            Collection<Unit> transports = null;
            if(MoveValidator.isLoad(route) && Match.someMatch(units, Matches.UnitIsLand))
            {
              transports = getTransportsToLoad(route, units, false);
              if(transports.isEmpty())
              {
                  CANCEL_MOVE_ACTION.actionPerformed(null);
                  return;
              }

              if (! m_unresolvedUnitCategories.isEmpty())
              {
                  Match<Unit> unresolvedUnitMatch = new Match<Unit>()
                  {
                      public boolean match(Unit u)
                      {
                          return m_unresolvedUnitCategories.contains(new UnitCategory(u, false, false));
                      }
                  };

                  final Collection<Unit> transportsToLoad = new ArrayList<Unit>(transports);
                  // this match will make sure we have enough transports
                  // to load the chosen units
                  Match<Collection<Unit>> enoughTransportsMatch = new Match<Collection<Unit>>()
                  {
                      public boolean match(Collection<Unit> units)
                      {
                          Map<Unit,Unit> unitsToTransports = MoveDelegate.mapTransports(route, units, transportsToLoad);
                          return unitsToTransports.keySet().containsAll(units);
                      }
                  };

                  List<Unit> candidateUnits = m_unitAutoChooser.getCandidateUnits(false);
                  List<Unit> candidateUnresolvedUnits = Match.getMatches(candidateUnits, unresolvedUnitMatch);
                  List<Unit> unresolvedUnits = Match.getMatches(units, unresolvedUnitMatch);

                  Collection<Unit> newUnits = new ArrayList<Unit>(units);
                  newUnits.removeAll(unresolvedUnits);

                  // create new UnitAutoChooser with movement categorized
                  UnitAutoChooser newAutoChooser = new UnitAutoChooser(candidateUnresolvedUnits,
                                                                       unresolvedUnits,
                                                                       true, true);

                  CompositeMatch<Collection<Unit>> unitChooserMatch = new CompositeMatchAnd<Collection<Unit>>(enoughTransportsMatch);

                  // make sure that default selections are legal
                  Collection<Unit> defaultSelections = new ArrayList<Unit>();
                  for (Unit unit : unresolvedUnits)
                  {
                      Collection<Unit> newSelections = new ArrayList<Unit>(defaultSelections);
                      newSelections.add(unit);
                      if (!unitChooserMatch.match(newSelections))
                          break;
                      defaultSelections.add(unit);
                  }
                  
                  // TODO: stop using mustMoveWith once carrier dependents are supported in TripleAUnit
                  UnitChooser chooser = new UnitChooser(candidateUnresolvedUnits,
                                                        defaultSelections,
                                                        m_mustMoveWithDetails.getMustMoveWith(), 
                                                        /*categorizeMovement*/ true,
                                                        getData(), 
                                                        /*allowTwoHit*/ false, 
                                                        getMap().getUIContext(), 
                                                        unitChooserMatch);

                  String text = "Load from " + getFirstSelectedTerritory();
                  int option = JOptionPane.showOptionDialog(getTopLevelAncestor(),
                                                  chooser, text,
                                                  JOptionPane.OK_CANCEL_OPTION,
                                                  JOptionPane.PLAIN_MESSAGE, null, null, null);
                  
                  if(option != JOptionPane.OK_OPTION)
                  {
                      CANCEL_MOVE_ACTION.actionPerformed(null);
                      return;
                  }
              
                  Collection<Unit> chosenUnits = chooser.getSelected(true);
                  if (chosenUnits.isEmpty())
                  {
                      CANCEL_MOVE_ACTION.actionPerformed(null);
                      return;
                  }
                  
                  newUnits.addAll(chosenUnits);
                  units.retainAll(newUnits);

                  if (units.isEmpty())
                  {
                      CANCEL_MOVE_ACTION.actionPerformed(null);
                      return;
                  }
              }

            }
            else if(MoveValidator.isUnload(route) && Match.someMatch(units, Matches.UnitIsLand))
            {
                // get unloadable land units
                CompositeMatch<Unit> unloadableMatch = getUnloadableMatch(route, units);
                List<Unit> unloadableUnits = Match.getMatches(units,unloadableMatch);
                Collection<Unit> unitsToUnload = new ArrayList<Unit>(getUnitsToUnload(route, unloadableUnits));
                // add air units too
                unitsToUnload.addAll(Match.getMatches(units, new InverseMatch<Unit>(Matches.UnitIsLand)));
                if(unitsToUnload.isEmpty())
                {
                    CANCEL_MOVE_ACTION.actionPerformed(null);
                    return; 
                }
                else
                {
                    units.clear();
                    units.addAll(unitsToUnload);
                }
            }
            else
            {
                // re-run auto chooser if disallowed units have been filtered
                UnitAutoChooser autoChooser = null;
                if (m_selectedUnits.size() == units.size())
                    autoChooser = m_unitAutoChooser;
                else
                    autoChooser = new UnitAutoChooser(m_unitAutoChooser.getCandidateUnits(false), units, true, false);

                units.clear();
                units.addAll(allowSpecificUnitSelection(autoChooser, route));
                
                if(units.isEmpty())
                {
                    CANCEL_MOVE_ACTION.actionPerformed(null);
                    return;
                }
                
            }

            MoveDescription message = new MoveDescription(units, route, transports);
            m_moveMessage = message;
            setFirstSelectedTerritory(null);
            setSelectedEndpointTerritory(null);
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
                if (m_mouseCurrentTerritory == null || !m_mouseCurrentTerritory.equals(territory)) {
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

                updateRouteAndMouseShadowUnits(route);
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
            m_mustMoveWithDetails = MoveDelegate.getMustMoveWith(firstSelectedTerritory, 
                                                                 firstSelectedTerritory.getUnits().getUnits(), 
                                                                 getData(),
                                                                 getCurrentPlayer());
        }
    }

    private Territory getFirstSelectedTerritory()
    {
        return m_firstSelectedTerritory;
    }

    private void setSelectedEndpointTerritory(Territory selectedEndpointTerritory)
    {
        m_selectedEndpointTerritory = selectedEndpointTerritory;
    }

    private Territory getSelectedEndpointTerritory()
    {
        return m_selectedEndpointTerritory;
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
