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

import games.strategy.engine.data.*;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.triplea.attatchments.UnitAttatchment;
import games.strategy.triplea.delegate.*;
import games.strategy.triplea.delegate.message.*;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.util.*;

import java.awt.Point;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

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

    private List m_forced;
    private boolean m_nonCombat;
    private UndoableMovesPanel m_undableMovesPanel;
    
    private Point m_mouseSelectedPoint;
    private Point m_mouseCurrentPoint;

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
    }

    private void cleanUp()
    {
        getMap().removeMapSelectionListener(MAP_SELECTION_LISTENER);
        m_bridge = null;
        updateRoute(null);
        CANCEL_MOVE_ACTION.setEnabled(false);
        m_forced = null;
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
            this.setEnabled(false);
        }
    };

    /**
     * Return the units that can be unloaded for this routs.
     * If needed will ask the user what transports to unload.
     * This is needed because the user needs to be able to select what transports to unload
     */
    private Collection getUnitsThatCanBeUnload(Route route)
    {
      //get the allied transports
      Match alliedTransports = new CompositeMatchAnd(Matches.UnitIsTransport, Matches.alliedUnit(getCurrentPlayer(), m_bridge.getGameData()));
      Collection transports = Match.getMatches(route.getStart().getUnits().getUnits(), alliedTransports);
      if(transports.isEmpty())
        return Collections.EMPTY_LIST;

      //find out what they are transporting
      MustMoveWithDetails mustMoveWith = getDelegate().getMustMoveWith(route.getStart(), transports);
      
      List candidateTransports = new ArrayList();

      //get the transports with units that we own
      //these transports can be unloaded
      Match owned = Matches.unitIsOwnedBy(getCurrentPlayer());
      Iterator mustMoveWithIter = mustMoveWith.getMustMoveWith().keySet().iterator();
      while (mustMoveWithIter.hasNext())
      {
        Unit transport = (Unit) mustMoveWithIter.next();
        Collection transporting = (Collection) mustMoveWith.getMustMoveWith().get(transport);
        if(transporting == null)
          continue;
        if(Match.someMatch(transporting, owned))
          candidateTransports.add(transport);
      }

      if(candidateTransports.isEmpty())
        return Collections.EMPTY_LIST;
      //only one, no choice
      if(candidateTransports.size() == 1)
      {
        return (Collection) mustMoveWith.getMustMoveWith().get(candidateTransports.get(0));
      }

      // choosing what units to UNLOAD
      UnitChooser chooser = new UnitChooser(candidateTransports, mustMoveWith.getMustMoveWith(), mustMoveWith.getMovement(), m_bridge.getGameData());
      chooser.setTitle("What transports do you want to unload");
      int option = JOptionPane.showOptionDialog(getTopLevelAncestor(),
                                                chooser, "What transports do you want to unload",
                                                JOptionPane.OK_CANCEL_OPTION,
                                                JOptionPane.PLAIN_MESSAGE, null, null, null);
      if(option != JOptionPane.OK_OPTION)
        return Collections.EMPTY_LIST;

      Collection choosenTransports = chooser.getSelected();

      Collection toMove = new ArrayList();
      Iterator choosenIter = choosenTransports.iterator();
      while (choosenIter.hasNext())
      {
        Unit transport = (Unit)choosenIter.next();
        Collection transporting = (Collection) mustMoveWith.getMustMoveWith().get(transport);
        if(transporting != null)
          toMove.addAll(transporting);
      }

      return toMove;
    }

    private Collection getUnitsToMove(Route route)
    {
        CompositeMatch ownedNotFactory = new CompositeMatchAnd();
        ownedNotFactory.add(Matches.UnitIsNotFactory);
        ownedNotFactory.add(Matches.unitIsOwnedBy(getCurrentPlayer()));
        if(!m_nonCombat)
            ownedNotFactory.add(new InverseMatch( Matches.UnitIsAA));

        Collection owned = route.getStart().getUnits().getMatches(ownedNotFactory);

	boolean transportsAtEnd = route.getEnd().getUnits().getMatches(Matches.UnitCanTransport).size() > 0;

        if (route.getStart().isWater())
        {
            if (route.getEnd().isWater())
            {
                owned = Match.getMatches(owned, Matches.UnitIsNotLand);
            }
            //unloading
            if (!route.getEnd().isWater())
            {
                owned = Match.getMatches(owned, Matches.UnitIsAir);
                owned.addAll(getUnitsThatCanBeUnload(route));
            }
        } else if (route.crossesWater() ||
	            (route.getEnd().isWater() && !transportsAtEnd)) {
	  // Eliminate land units if starting from land, crossing water, and
	  // back to land or if going into water and no transports there
	  owned = Match.getMatches(owned, Matches.UnitIsAir);
	}

	return getUnitsChosen(owned, route);
    }

    private Collection getUnitsChosen(Collection units, Route route)
    {
        
        MustMoveWithDetails mustMoveWith = getDelegate().getMustMoveWith(route.getStart(), units);

        //unit movement counts when the unit is loaded
        //this fixes the case where a unit is loaded and then unloaded in the same turn
        Collection canMove;
        if (MoveValidator.isUnload(route))
            canMove = units;
        else
            canMove = Match.getMatches(units,
                                       Matches.unitHasEnoughMovement(route.
                                       getLength(), mustMoveWith.getMovement()));

	Collection movedUnits = new ArrayList();
	Map mustMoveWithMap = mustMoveWith.getMustMoveWith();
        if (canMove.isEmpty()) {
            JOptionPane.showMessageDialog(getTopLevelAncestor(), "No units can move that far", "No units", JOptionPane.INFORMATION_MESSAGE);
            return Collections.EMPTY_LIST;
	} else if (canMove.size() == 1) {
          Object singleUnit = canMove.iterator().next();
	  movedUnits.add(singleUnit);
	  // Add dependents if necessary
	  Collection dependents = (Collection) mustMoveWithMap.get(singleUnit);
	  if (dependents != null) {
	    movedUnits.addAll(dependents);
	  }
	  return movedUnits;
	}

	// choosing what units to MOVE
        UnitChooser chooser = new UnitChooser(canMove, mustMoveWithMap,
					      mustMoveWith.getMovement(), m_bridge.getGameData());


        String text = "Select units to move from " + route.getStart().getName() + ".";
        int option = JOptionPane.showOptionDialog(getTopLevelAncestor(),
                                                  chooser, text,
                                                  JOptionPane.OK_CANCEL_OPTION,
                                                  JOptionPane.PLAIN_MESSAGE, null, null, null);

        if (option != JOptionPane.OK_OPTION)
            return Collections.EMPTY_LIST;

        return chooser.getSelected();
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

        Iterator iter = m_forced.iterator();

        Territory last = m_firstSelectedTerritory;
        Territory current = null;

        Route total = new Route();
        total.setStart(last);

        while (iter.hasNext())
        {
            current = (Territory) iter.next();
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
        CompositeMatch noAA = new CompositeMatchOr();
        noAA.add(new InverseMatch(Matches.territoryHasEnemyAA(
            getCurrentPlayer(), getData())));
        //ignore the destination
        noAA.add(Matches.territoryIs(end));

	// No neutral countries on route predicate
        Match noEmptyNeutral = new InverseMatch(new CompositeMatchAnd(Matches.TerritoryIsNuetral, Matches.TerritoryIsEmpty));

	// No neutral countries nor AA guns on route predicate
	Match noNeutralOrAA = new CompositeMatchAnd(noAA, noEmptyNeutral);

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
    private Collection getTransportsToLoad(Route route)
    {
      Match alliedTransports = new CompositeMatchAnd(Matches.UnitIsTransport, Matches.alliedUnit(getCurrentPlayer(), m_bridge.getGameData()));
      Collection transports = Match.getMatches(route.getEnd().getUnits().getUnits(), alliedTransports);
      //nothing to choose
      if(transports.isEmpty())
        return Collections.EMPTY_LIST;
      //only one, no need to choose
      if(transports.size() == 1)
        return transports;

      //find out what they are transporting
      MustMoveWithDetails mustMoveWith = getDelegate().getMustMoveWith(route.getEnd(), transports);

      List candidateTransports = new ArrayList();

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
        return Collections.EMPTY_LIST;


      return chooser.getSelected(false);
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
            if (m_firstSelectedTerritory == null)
                return;

            if (m_firstSelectedTerritory.equals(territory))
                return;

            if (m_forced == null)
                m_forced = new ArrayList();

            if (!m_forced.contains(territory))
                m_forced.add(territory);

            updateRoute(getRoute(m_firstSelectedTerritory,
                                 m_firstSelectedTerritory));
        }

        private void leftButtonSelection(Territory territory, MouseEvent e)
        {
            if (m_firstSelectedTerritory == null)
            {
                
                if (!territory.getUnits().someMatch(Matches.unitIsOwnedBy(
                    getCurrentPlayer())))
                    return;

                m_firstSelectedTerritory = territory;
                m_mouseSelectedPoint = e.getPoint();
                adjustPointForMapOffset(m_mouseSelectedPoint);
                
                
                CANCEL_MOVE_ACTION.setEnabled(true);
                updateRoute(getRoute(m_firstSelectedTerritory,
                                     m_firstSelectedTerritory));
                return;
            }
            else if (m_firstSelectedTerritory != territory)
            {
                Route route = getRoute(m_firstSelectedTerritory, territory);
                Collection units = getUnitsToMove(route);
                if (units.size() == 0)
                {
                    m_firstSelectedTerritory = null;
                    m_forced = null;
                    updateRoute(null);
                    CANCEL_MOVE_ACTION.setEnabled(false);
                    return;
                }
                else
                {

                   Collection transports = null;
                    if(MoveValidator.isLoad(route) && Match.someMatch(units, Matches.UnitIsLand))
                    {
                      transports = getTransportsToLoad(route);
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

        /**
         * 
         */
        private  void adjustPointForMapOffset(Point p)
        {
            p.x +=  getMap().getXOffset();
            p.y += getMap().getYOffset();
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



