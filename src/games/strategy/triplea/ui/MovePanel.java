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

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.gamePlayer.PlayerBridge;
import games.strategy.engine.data.events.*;

import games.strategy.triplea.delegate.Matches;
import games.strategy.engine.message.Message;

import games.strategy.triplea.delegate.message.*;

/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class MovePanel extends ActionPanel
{
  private JLabel m_actionLabel = new JLabel();
  private MoveMessage m_moveMessage;
  private Territory m_firstSelectedTerritory;
  private PlayerBridge m_bridge;

  private List m_forced;
  private JLabel m_moveLabel = new JLabel();

  private String[] m_currentMoves = null;


  /** Creates new MovePanel */
    public MovePanel(GameData data, MapPanel map)
  {
    super(data, map);
    CANCEL_MOVE_ACTION.setEnabled(false);
    }

  public void display(PlayerID id, boolean nonCombat)
  {
    super.display(id);
    removeAll();
    m_actionLabel.setText(id.getName() + (nonCombat ? " non combat" : " combat") + " move" );
    this.add(m_actionLabel);
    this.add(new JButton(CANCEL_MOVE_ACTION));
    this.add(new JButton(DONE_MOVE_ACTION));
    this.add(new JLabel("  "));
    this.add(m_moveLabel);
    this.add(new JButton(SHOW_MOVES_ACTION));
    this.add(new JButton(UNDO_MOVE_ACTION));

    SwingUtilities.invokeLater(REFRESH);
  }

  private void updateMoveLabel()
  {
    MoveCountReplyMessage moves = (MoveCountReplyMessage) m_bridge.sendMessage(new MoveCountRequestMessage());
    int moveCount = moves.getMoveCount();
    m_moveLabel.setText(moveCount + ( moveCount == 1 ? " Move" : " Moves"));
    m_currentMoves = moves.getMoves();
    UNDO_MOVE_ACTION.setEnabled(moveCount != 0);
    SHOW_MOVES_ACTION.setEnabled(moveCount != 0);
  }

  public MoveMessage waitForMove(PlayerBridge bridge)
  {
    setUp(bridge);
    updateMoveLabel();
    synchronized(getLock())
    {
      try
      {
        getLock().wait();
      } catch(InterruptedException ie)
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

  private void setUp(PlayerBridge bridge)
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
    getCursorComponent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    CANCEL_MOVE_ACTION.setEnabled(false);
    m_forced = null;
  }

  private JComponent getCursorComponent()
  {
    return getMap();
  }

  private final AbstractAction DONE_MOVE_ACTION = new AbstractAction("Done")
  {
    public void actionPerformed(ActionEvent e)
    {
      synchronized(getLock())
      {
        m_moveMessage = null;
        getLock().notify();
      }
    }
  };

  private final AbstractAction UNDO_MOVE_ACTION = new AbstractAction("Undo last move")
  {
    public void actionPerformed(ActionEvent e)
    {
        StringMessage results = (StringMessage) m_bridge.sendMessage(new UndoMoveMessage());
        if(results != null && results.isError())
        {
          JOptionPane.showMessageDialog( getTopLevelAncestor(), results.getMessage(), "Could not undo move", JOptionPane.ERROR_MESSAGE);
        }
        else
        {
          updateMoveLabel();
        }

    }
  };

  private final AbstractAction CANCEL_MOVE_ACTION = new AbstractAction("Cancel")
  {
    public void actionPerformed(ActionEvent e)
    {
      getCursorComponent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
      m_firstSelectedTerritory = null;
      m_forced = null;
      updateRoute(null);
      this.setEnabled(false);
    }
  };


  private Collection getUnitsToMove(Territory start, Territory end)
  {
    CompositeMatch ownedNotFactory = new CompositeMatchAnd();
    ownedNotFactory.add(Matches.UnitIsNotFactory);
    ownedNotFactory.add(Matches.unitIsOwnedBy(getCurrentPlayer()));

    Collection owned = start.getUnits().getMatches(ownedNotFactory);

    if(start.isWater())
    {
      if(end.isWater())
      {
        owned = Match.getMatches(owned, Matches.UnitIsNotLand);
      }
      if(!end.isWater())
      {
        owned = Match.getMatches(owned, Matches.UnitIsNotSea);
      }
    }

    UnitChooser chooser = getUnitChooser(owned, start);
    String text = "Select units to move from "	+ start.getName() + ".";
    int option = JOptionPane.showOptionDialog(getTopLevelAncestor(), chooser, text, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,null,null,null );

    if(option != JOptionPane.OK_OPTION)
      return Collections.EMPTY_LIST;

    return chooser.getSelected();
  }

  private UnitChooser getUnitChooser(Collection units, Territory start)
  {
    Message msg = new MustMoveWithQuery(units, start);
    Message response = m_bridge.sendMessage(msg);
    if(!(response instanceof MustMoveWithReply))
      throw new IllegalStateException("Message of wrong type:" + response);

    MustMoveWithReply mustMoveWith = (MustMoveWithReply) response;

    return new UnitChooser(units, mustMoveWith.getMustMoveWith(), mustMoveWith.getMovement(), m_bridge.getGameData());
  }

  private Route getRoute(Territory start, Territory end)
  {
    if(m_forced == null)
      return getRouteNonForced(start, end);
    else
      return getRouteForced(start, end);
  }

  /**
   * Get the route inculdin g the territories that we are forced to move through.
   */
  private Route getRouteForced(Territory start, Territory end)
  {
    if(m_forced == null || m_forced.size() == 0)
      throw new IllegalStateException("No forced territories:" + m_forced + " end:" + end + " start:" + start);

    Iterator iter = m_forced.iterator();

    Territory last = m_firstSelectedTerritory;
    Territory current = null;

    Route total = new Route();
    total.setStart(last);

    while(iter.hasNext())
    {
      current = (Territory) iter.next();
      Route add = getData().getMap().getRoute(last, current);

      Route newTotal = Route.join(total, add);
      if(newTotal == null)
        return total;

      total = newTotal;
      last = current;
    }

    if(! end.equals(last))
    {

      Route add = getData().getMap().getRoute(last, end);
      Route newTotal = Route.join(total, add);
      if(newTotal != null)
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
    if(defaultRoute == null)
      throw new IllegalStateException("No route between:" + start + " and " + end);

    //if the start and end are in water, try and get a water route
    //dont force a water route, since planes may be moving
    if(start.isWater() && end.isWater())
    {
      Route waterRoute = getData().getMap().getRoute(start, end, Matches.TerritoryIsWater);
      if(waterRoute != null && waterRoute.getLength() == defaultRoute.getLength())
        return waterRoute;
    }

    //try to avoid aa guns if we can
    //this fails if the end has an enemy aa gun (eg britian -> Germany)
    //TODO - get this to ignore the last territory
    Route noAARoute = getData().getMap().getRoute(start, end, new InverseMatch( Matches.territoryHasEnemyAA(getCurrentPlayer(), getData())));
    if(noAARoute != null && noAARoute.getLength() == defaultRoute.getLength())
      return noAARoute;

    return defaultRoute;
  }

  /**
   * Route can be null.
   */
  private void updateRoute(Route route)
  {
    getMap().setRoute(route);
  }

  private void append(Route route, StringBuffer buf)
  {
    for(int i = 0; i < route.getLength(); i++)
    {
      buf.append(route.at(i));
      if(i + 1 < route.getLength())
        buf.append("->");
      buf.append("\n");
    }
  }

  private final Action SHOW_MOVES_ACTION = new AbstractAction("Show Moves...")
  {
    public void actionPerformed(ActionEvent e)
    {

      JEditorPane editorPane = new JEditorPane();
      editorPane.setEditable(false);
      editorPane.setContentType("text/html");

      StringBuffer text = new StringBuffer();
      for(int i =0; i < m_currentMoves.length; i++)
      {
        text.append("<b>");
        text.append(i + 1);
        text.append(")</b> ");
        text.append(m_currentMoves[i]);
        text.append(".<br>");
      }
      editorPane.setText(text.toString());

      JScrollPane scroll = new JScrollPane(editorPane);
      scroll.setPreferredSize(new Dimension(350,200));
      scroll.setMaximumSize(new Dimension(350,200));

      JOptionPane.showMessageDialog(MovePanel.this, scroll, "Moves Made", JOptionPane.PLAIN_MESSAGE);




    }
  };

  private final MapSelectionListener MAP_SELECTION_LISTENER = new DefaultMapSelectionListener()
  {
    public void territorySelected(Territory territory, MouseEvent me)
    {
      if((me.getModifiers() & MouseEvent.BUTTON3_MASK) != 0)
      {
        rightButtonSelection(territory);
      }
      else
      {
        leftButtonSelection(territory);
      }
    }


    private void rightButtonSelection(Territory territory)
    {
      if(m_firstSelectedTerritory == null)
        return;

      if(m_firstSelectedTerritory.equals(territory))
        return;

      if(m_forced == null)
        m_forced = new ArrayList();

      if(!m_forced.contains(territory))
        m_forced.add(territory);

      updateRoute( getRoute(m_firstSelectedTerritory,m_firstSelectedTerritory));
    }

    private void leftButtonSelection(Territory territory)
    {
      if(m_firstSelectedTerritory == null)
      {
        if(!territory.getUnits().someMatch(Matches.unitIsOwnedBy(getCurrentPlayer())))
          return;

        m_firstSelectedTerritory = territory;
        CANCEL_MOVE_ACTION.setEnabled(true);
        updateRoute( getRoute(m_firstSelectedTerritory,m_firstSelectedTerritory));
        return;
      }
      else if(m_firstSelectedTerritory != territory)
      {
        Collection units = getUnitsToMove(m_firstSelectedTerritory, territory);
        if(units.size() == 0)
        {
          m_firstSelectedTerritory = null;
          m_forced = null;
          updateRoute(null);
          CANCEL_MOVE_ACTION.setEnabled(false);
          return;
        }
        else
        {
          Route route = getRoute(m_firstSelectedTerritory, territory);
          MoveMessage message = new MoveMessage(units, route);
          m_moveMessage = message;
          m_firstSelectedTerritory = null;
          m_forced = null;
          updateRoute(null);
          synchronized(getLock())
          {
            getLock().notifyAll();
          }
        }
      }
    }

    public void mouseEntered(Territory territory)
    {
      if(m_firstSelectedTerritory != null && territory != null)
      {

        updateRoute(getRoute(m_firstSelectedTerritory, territory));
      }
    }
  };

  public String toString()
  {
    return "MovePanel";
  }
}
