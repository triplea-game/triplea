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
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.triplea.attatchments.UnitAttatchment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.dataObjects.MustMoveWithDetails;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.CompositeMatchOr;
import games.strategy.util.InverseMatch;
import games.strategy.util.Match;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
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
    private JLabel m_actionLabel = new JLabel();
    private MoveDescription m_moveMessage;
    private Territory m_firstSelectedTerritory;
    private IPlayerBridge m_bridge;

    private List<Territory> m_forced;
    private boolean m_nonCombat;
    private UndoableMovesPanel m_undableMovesPanel;
    
    private Point m_mouseSelectedPoint;
    private Point m_mouseCurrentPoint;
    
    //use a LinkedHashSet because we want to know the order
    private final Set<Unit> m_selectedUnits = new LinkedHashSet<Unit>();

    /** Creates new MovePanel */
    public MovePanel(GameData data, MapPanel map)
    {
        super(data, map);
        CANCEL_MOVE_ACTION.setEnabled(false);


        registerKeyboardAction(CANCEL_MOVE_ACTION,
                        KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0),
                        WHEN_IN_FOCUSED_WINDOW
                        );
         m_undableMovesPanel = new UndoableMovesPanel(data, this);
    }

    private JComponent leftBox(JComponent c)
    {
        Box b = new Box(BoxLayout.X_AXIS);
        b.add(c);
        b.add(Box.createHorizontalGlue());
        return b;
    }

    public void display(PlayerID id, boolean nonCombat)
    {
        super.display(id);
        m_nonCombat = nonCombat;
        removeAll();
        m_actionLabel.setText(id.getName() +
                              (nonCombat ? " non combat" : " combat") + " move");
        this.add(leftBox(m_actionLabel));
        this.add(leftBox(new JButton(CANCEL_MOVE_ACTION)));
        this.add(leftBox(new JButton(DONE_MOVE_ACTION)));
        this.add(Box.createVerticalStrut(15));


        this.add(m_undableMovesPanel);
        this.add(Box.createGlue());

        SwingUtilities.invokeLater(REFRESH);
    }

    private IMoveDelegate getDelegate()
    {
        return (IMoveDelegate) m_bridge.getRemote();
    }
    
    private void updateMoves()
    {
        m_undableMovesPanel.setMoves(getDelegate().getMovesMade());
    }

    public MoveDescription waitForMove(IPlayerBridge bridge)
    {
        setUp(bridge);
        updateMoves();
        synchronized (getLock())
        {
            try
            {
                getLock().wait();
            }
            catch (InterruptedException ie)
            {
                cleanUp();
                return waitForMove(bridge);
            }
            cleanUp();
            removeAll();
            SwingUtilities.invokeLater(REFRESH);
            return m_moveMessage;
        }
    }

    private void setUp(IPlayerBridge bridge)
    {
        m_firstSelectedTerritory = null;
        m_forced = null;
        m_bridge = bridge;
        getMap().addMapSelectionListener(MAP_SELECTION_LISTENER);
        getMap().addUnitSelectionListener(UNIT_SELECTION_LISTENER);
    }

    private void cleanUp()
    {
        getMap().removeMapSelectionListener(MAP_SELECTION_LISTENER);
        getMap().removeUnitSelectionListener(UNIT_SELECTION_LISTENER);
        m_bridge = null;
        updateRoute(null);
        CANCEL_MOVE_ACTION.setEnabled(false);
        m_forced = null;
        m_selectedUnits.clear();
    }


    private final AbstractAction DONE_MOVE_ACTION = new AbstractAction("Done")
    {
        public void actionPerformed(ActionEvent e)
        {
            synchronized (getLock())
            {
                m_moveMessage = null;
                getLock().notify();
            }
        }
    };

    public void cancelMove()
    {
        CANCEL_MOVE_ACTION.actionPerformed(null);
    }

    public void setActive(boolean active)
    {
      super.setActive(active);
      CANCEL_MOVE_ACTION.actionPerformed(null);
    }

    private final AbstractAction CANCEL_MOVE_ACTION = new AbstractAction(
        "Cancel")
    {
        public void actionPerformed(ActionEvent e)
        {
            m_firstSelectedTerritory = null;
            m_forced = null;
            updateRoute(null);
            m_selectedUnits.clear();
            this.setEnabled(false);
            getMap().setMouseShadowUnits(null);
            
        }
    };

    /**
     * Return the units that to be unloaded for this route.
     * If needed will ask the user what transports to unload.
     * This is needed because the user needs to be able to select what transports to unload
     * in the case where some transports have different movement, different
     * units etc
     */
    private Collection<Unit> getUnitsToUnload(Route route)
    {
      List<Unit> candidateTransports = new ArrayList<Unit>();
      
      MustMoveWithDetails mustMoveWith = getDelegate().getMustMoveWith(route.getStart(), route.getStart().getUnits().getUnits());
      
      //get the transports with units that we own
      //these transports can be unloaded
      Match<Unit> owned = Matches.unitIsOwnedBy(getCurrentPlayer());
      Iterator<Unit> mustMoveWithIter = mustMoveWith.getMustMoveWith().keySet().iterator();
      while (mustMoveWithIter.hasNext())
      {
        Unit transport = mustMoveWithIter.next();
        Collection<Unit> transporting = mustMoveWith.getMustMoveWith().get(transport);
        if(transporting == null)
          continue;
        if(Match.someMatch(transporting, owned))
          candidateTransports.add(transport);
      }

      if(candidateTransports.size() == 0)
          throw new IllegalStateException("No transports to unload");      
      
      //just one transport, dont bother to ask
      if(candidateTransports.size() == 1)
          return m_selectedUnits;
      
      //are we unloading everything? if we are then we dont need to select the transports
      CompositeMatch<Unit> unloadable = new CompositeMatchAnd<Unit>();
      unloadable.add(Matches.unitIsOwnedBy(getCurrentPlayer()));
      unloadable.add(Matches.UnitIsLand);
      if(m_nonCombat)
          unloadable.add(new InverseMatch<Unit>(Matches.UnitIsAA));
      
      if(m_selectedUnits.size() == route.getStart().getUnits().countMatches(unloadable))
          return m_selectedUnits;

      //are the transports all of the same type
      //if they are, then don't ask
      Collection<UnitCategory> categories = UnitSeperator.categorize(candidateTransports, mustMoveWith.getMustMoveWith(), mustMoveWith.getMovement() );
      if(categories.size() == 1)
          return m_selectedUnits;
     
      // choosing what transports to unload
      UnitChooser chooser = new UnitChooser(candidateTransports, mustMoveWith.getMustMoveWith(), mustMoveWith.getMovement(), m_bridge.getGameData());
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
        Collection<Unit> transporting =  mustMoveWith.getMustMoveWith().get(transport);
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
      
      for(Unit selected : m_selectedUnits)
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

//    private Collection<Unit> getUnitsToMove(Route route)
//    {
//        CompositeMatch<Unit> ownedNotFactory = new CompositeMatchAnd<Unit>();
//        ownedNotFactory.add(Matches.UnitIsNotFactory);
//        ownedNotFactory.add(Matches.unitIsOwnedBy(getCurrentPlayer()));
//        if(!m_nonCombat)
//            ownedNotFactory.add(new InverseMatch<Unit>( Matches.UnitIsAA));
//
//        Collection<Unit> owned = route.getStart().getUnits().getMatches(ownedNotFactory);
//
//	boolean transportsAtEnd = route.getEnd().getUnits().getMatches(Matches.UnitCanTransport).size() > 0;
//
//        if (route.getStart().isWater())
//        {
//            if (route.getEnd().isWater())
//            {
//                owned = Match.getMatches(owned, Matches.UnitIsNotLand);
//            }
//            //unloading
//            if (!route.getEnd().isWater())
//            {
//                owned = Match.getMatches(owned, Matches.UnitIsAir);
//                owned.addAll(getUnitsThatCanBeUnload(route));
//            }
//        } else if (route.crossesWater() ||
//	            (route.getEnd().isWater() && !transportsAtEnd)) {
//	  // Eliminate land units if starting from land, crossing water, and
//	  // back to land or if going into water and no transports there
//	  owned = Match.getMatches(owned, Matches.UnitIsAir);
//	}
//
//	return getUnitsChosen(owned, route);
//    }

//    private Collection<Unit> getUnitsChosen(Collection<Unit> units, Route route)
//    {
//        
//        MustMoveWithDetails mustMoveWith = getDelegate().getMustMoveWith(route.getStart(), units);
//
//        //unit movement counts when the unit is loaded
//        //this fixes the case where a unit is loaded and then unloaded in the same turn
//        Collection<Unit> canMove;
//        if (MoveValidator.isUnload(route))
//            canMove = units;
//        else
//            canMove = Match.getMatches(units,
//                                       Matches.unitHasEnoughMovement(route.
//                                       getLength(), mustMoveWith.getMovement()));
//
//	Collection<Unit> movedUnits = new ArrayList<Unit>();
//	Map<Unit, Collection<Unit>> mustMoveWithMap = mustMoveWith.getMustMoveWith();
//        if (canMove.isEmpty()) {
//            JOptionPane.showMessageDialog(getTopLevelAncestor(), "No units can move that far", "No units", JOptionPane.INFORMATION_MESSAGE);
//            return Collections.emptyList();
//	} else if (canMove.size() == 1) {
//          Unit singleUnit = canMove.iterator().next();
//	  movedUnits.add(singleUnit);
//	  // Add dependents if necessary
//	  Collection<Unit> dependents = mustMoveWithMap.get(singleUnit);
//	  if (dependents != null) {
//	    movedUnits.addAll(dependents);
//	  }
//	  return movedUnits;
//	}
//
//	// choosing what units to MOVE
//        UnitChooser chooser = new UnitChooser(canMove, mustMoveWithMap,
//					      mustMoveWith.getMovement(), m_bridge.getGameData());
//
//
//        String text = "Select units to move from " + route.getStart().getName() + ".";
//        int option = JOptionPane.showOptionDialog(getTopLevelAncestor(),
//                                                  chooser, text,
//                                                  JOptionPane.OK_CANCEL_OPTION,
//                                                  JOptionPane.PLAIN_MESSAGE, null, null, null);
//
//        if (option != JOptionPane.OK_OPTION)
//            return Collections.emptyList();
//
//        return chooser.getSelected();
//    }

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

        Territory last = m_firstSelectedTerritory;
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


	// No aa guns on route predicate
        CompositeMatch<Territory> noAA = new CompositeMatchOr<Territory>();
        noAA.add(new InverseMatch<Territory>(Matches.territoryHasEnemyAA(
            getCurrentPlayer(), getData())));
        //ignore the destination
        noAA.add(Matches.territoryIs(end));

	// No neutral countries on route predicate
        Match<Territory> noEmptyNeutral = new InverseMatch<Territory>(new CompositeMatchAnd<Territory>(Matches.TerritoryIsNuetral, Matches.TerritoryIsEmpty));

	// No neutral countries nor AA guns on route predicate
	Match<Territory> noNeutralOrAA = new CompositeMatchAnd<Territory>(noAA, noEmptyNeutral);

	// Try to avoid both AA guns and neutral territories
	Route noAAOrNeutralRoute = getData().getMap().getRoute(start, end, noNeutralOrAA);
        if (noAAOrNeutralRoute != null &&
            noAAOrNeutralRoute.getLength() == defaultRoute.getLength())
	  return noAAOrNeutralRoute;

        // Try to avoid aa guns
        Route noAARoute = getData().getMap().getRoute(start, end, noAA);
        if (noAARoute != null &&
            noAARoute.getLength() == defaultRoute.getLength())
	  return noAARoute;

	// Try to avoid neutral countries
        Route noNeutralRoute = getData().getMap().getRoute(start, end, noEmptyNeutral);
        if (noNeutralRoute != null &&
            noNeutralRoute.getLength() == defaultRoute.getLength())
	  return noNeutralRoute;

        return defaultRoute;
    }

    /**
     * Route can be null.
     */
    private void updateRoute(Route route)
    {
        getMap().setRoute(route, m_mouseSelectedPoint, m_mouseCurrentPoint);
    }

    /**
     * Allow the user to select what transports to load.
     */
    private Collection<Unit> getTransportsToLoad(Route route)
    {
      Match<Unit> alliedTransports = new CompositeMatchAnd<Unit>(Matches.UnitIsTransport, Matches.alliedUnit(getCurrentPlayer(), m_bridge.getGameData()));
      Collection<Unit> transports = Match.getMatches(route.getEnd().getUnits().getUnits(), alliedTransports);
      //nothing to choose
      if(transports.isEmpty())
        return Collections.emptyList();
      //only one, no need to choose
      if(transports.size() == 1)
        return transports;

      //find out what they are transporting
      MustMoveWithDetails mustMoveWith = getDelegate().getMustMoveWith(route.getEnd(), transports);

      //all the same type, dont ask
      if(UnitSeperator.categorize(transports, mustMoveWith.getMustMoveWith(), mustMoveWith.getMovement()).size() == 1)
          return transports;
          
      
      List<Unit> candidateTransports = new ArrayList<Unit>();

      //find the transports with space left
      Iterator transportIter = transports.iterator();
      while (transportIter.hasNext())
      {
        Unit transport = (Unit) transportIter.next();
        Collection transporting = (Collection) mustMoveWith.getMustMoveWith().get(transport);
        int cost = MoveValidator.getTransportCost(transporting);
        int capacity = UnitAttatchment.get(transport.getType()).getTransportCapacity();
        if(capacity > cost)
          candidateTransports.add(transport);
      }

      if(candidateTransports.size() <= 1)
        return candidateTransports;


      // choosing what units to LOAD.
      UnitChooser chooser = new UnitChooser(candidateTransports, mustMoveWith.getMustMoveWith(), mustMoveWith.getMovement(), m_bridge.getGameData());
      chooser.setTitle("What transports do you want to load");
      int option = JOptionPane.showOptionDialog(getTopLevelAncestor(),
                                                chooser, "What transports do you want to load",
                                                JOptionPane.OK_CANCEL_OPTION,
                                                JOptionPane.PLAIN_MESSAGE, null, null, null);
      if (option != JOptionPane.OK_OPTION)
        return Collections.emptyList();


      return chooser.getSelected(false);
    }

    private final UnitSelectionListener UNIT_SELECTION_LISTENER = new UnitSelectionListener()
    {
    
        public void unitsSelected(List<Unit> units, Territory t, MouseEvent me)
        {
            if(!getActive())
                return;
           
            if ( (me.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
            {
                rightButtonSelection(units, t,me);
            }
            else
            {
                leftButtonSelection(units, t,me);
            }
            
    
        }

        private void leftButtonSelection(List<Unit> units, Territory t, MouseEvent me)
        {
            if(t == null)
                return;
            
            if(m_firstSelectedTerritory != null && !t.equals(m_firstSelectedTerritory))
                return;
            
            if(m_firstSelectedTerritory == null)
            {
                m_firstSelectedTerritory = t;
                m_mouseSelectedPoint = me.getPoint();
                m_mouseCurrentPoint = me.getPoint();
                adjustPointForMapOffset(m_mouseSelectedPoint);
                adjustPointForMapOffset(m_mouseCurrentPoint);
                
                
                CANCEL_MOVE_ACTION.setEnabled(true);
                updateRoute(getRoute(m_firstSelectedTerritory,
                                     m_firstSelectedTerritory));
                
            }
            
            //add all
            if(me.isControlDown())
            {
                m_selectedUnits.addAll(units);
            }
            //add one
            else
            {
                for(Unit unit : units)
                {
                    if(!m_selectedUnits.contains(unit))
                    {
                        m_selectedUnits.add(unit);
                        break;
                    }
                        
                }
            }
            getMap().setMouseShadowUnits(m_selectedUnits);
            
            
            
        }

        private void rightButtonSelection(List<Unit> units, Territory t, MouseEvent me)
        {         
            if(m_selectedUnits.isEmpty())
                return;
            
            if(m_firstSelectedTerritory == null)
                return;
            
            //no unit selected, remove the most recent
            if(units.isEmpty())
            {
                if(me.isControlDown())
                    m_selectedUnits.clear();
                else
                    //remove the last element
                    m_selectedUnits.remove( new ArrayList<Unit>(m_selectedUnits).get(m_selectedUnits.size() -1 ) );
            }
            //we have actually clicked on a specific unit
            else
            {
                //remove all if control is down
                if(me.isControlDown())
                {
                    m_selectedUnits.removeAll(units);
                }
                //remove one
                else
                {
                    for(Unit unit : units)
                    {
                        if(m_selectedUnits.contains(unit))
                        {
                            m_selectedUnits.remove(unit);
                            break;
                        }
                    }
                }
            }
            
            //nothing left, cancel move
            if(m_selectedUnits.isEmpty())
                CANCEL_MOVE_ACTION.actionPerformed(null);
            else
                getMap().setMouseShadowUnits(m_selectedUnits);
        
                
        }
    
    };
    
    
    /**
     * 
     */
    private  void adjustPointForMapOffset(Point p)
    {
        p.x +=  getMap().getXOffset();
        p.y += getMap().getYOffset();
    }
    
    private final MapSelectionListener MAP_SELECTION_LISTENER = new
        DefaultMapSelectionListener()
    {
        public void territorySelected(Territory territory, MouseEvent me)
        {
            if(!getActive())
              return;

            if ( (me.getModifiers() & InputEvent.BUTTON3_MASK) != 0)
            {
                rightButtonSelection(territory);
            }
            else
            {
                leftButtonSelection(territory, me);
            }
        }

        private void rightButtonSelection(Territory territory)
        {

        }

        private void leftButtonSelection(Territory territory, MouseEvent e)
        {

            if (e.isControlDown())
            {
                if (m_firstSelectedTerritory == null)
                    return;

                if (m_firstSelectedTerritory.equals(territory))
                    return;

                if (m_forced == null)
                    m_forced = new ArrayList<Territory>();

                if (!m_forced.contains(territory))
                    m_forced.add(territory);

                updateRoute(getRoute(m_firstSelectedTerritory,
                                     m_firstSelectedTerritory));
            }
            //we are selecting the territory to move to
            else if (m_firstSelectedTerritory != null &&  m_firstSelectedTerritory != territory)
            {
                Route route = getRoute(m_firstSelectedTerritory, territory);
                Collection<Unit> units = new ArrayList<Unit>(m_selectedUnits);
                if (units.size() == 0)
                {
                    CANCEL_MOVE_ACTION.actionPerformed(null);
                    return;
                }
                else
                {

                    Collection<Unit> transports = null;
                    if(MoveValidator.isLoad(route) && Match.someMatch(units, Matches.UnitIsLand))
                    {
                      transports = getTransportsToLoad(route);
                    }
                    else if(MoveValidator.isUnload(route))
                    {
                        Collection<Unit> canBeUnloaded = getUnitsToUnload(route);
                        if(canBeUnloaded.isEmpty())
                        {
                            CANCEL_MOVE_ACTION.actionPerformed(null);
                            return; 
                        }
                        else
                        {
                            m_selectedUnits.clear();
                            m_selectedUnits.addAll(canBeUnloaded);
                        }
                    }
                    else
                    {
                        MustMoveWithDetails mustMoveWIth = getDelegate().getMustMoveWith(m_firstSelectedTerritory, units);
                        Collection<Collection<Unit>> allForcedMoves = mustMoveWIth.getMustMoveWith().values();
                        for(Collection<Unit> forcedMove : allForcedMoves)
                        {
                            if(forcedMove != null)
                                units.addAll(forcedMove);
                        }
                        
                        
                    }

                    MoveDescription message = new MoveDescription(units, route, transports);
                    m_moveMessage = message;
                    m_firstSelectedTerritory = null;
                    m_forced = null;
                    updateRoute(null);
                    synchronized (getLock())
                    {
                        getLock().notifyAll();
                    }
                }
            }
        }



        public void mouseMoved(Territory territory, MouseEvent me)
        {
            if (m_firstSelectedTerritory != null && territory != null)
            {
                m_mouseCurrentPoint= me.getPoint();
                adjustPointForMapOffset(m_mouseCurrentPoint);
                
                updateRoute(getRoute(m_firstSelectedTerritory, territory));
            }
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

}



