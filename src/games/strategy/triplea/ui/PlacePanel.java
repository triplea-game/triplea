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
 * PlacePanel.java
 *
 * Created on December 4, 2001, 7:45 PM
 */

package games.strategy.triplea.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.data.events.*;
import games.strategy.triplea.Constants;
import games.strategy.triplea.image.UnitIconImageFactory;

import games.strategy.triplea.delegate.message.*;
import games.strategy.engine.gamePlayer.*;
import games.strategy.engine.message.Message;
import games.strategy.triplea.delegate.*;
import games.strategy.triplea.util.*;


/**
 *
 * @author  Sean Bridges
 * @version 1.0
 */
public class PlacePanel extends ActionPanel
{

  private JLabel actionLabel = new JLabel();
  private PlaceMessage m_placeMessage;
  private SimpleUnitPanel m_unitsToPlace = new SimpleUnitPanel();
  private PlayerBridge m_bridge;

  /** Creates new PlacePanel */
    public PlacePanel(GameData data, MapPanel map)
  {
    super(data, map);
    }

  public void display(PlayerID id)
  {
    super.display(id);

    removeAll();
    actionLabel.setText(id.getName() + " place");
    add(actionLabel);
    add(new JButton(UNDO_PLACE_ACTION));
    add(new JButton(DONE_PLACE_ACTION));
    SwingUtilities.invokeLater(REFRESH);

    add(new JLabel("Units left to place:"));
    add(m_unitsToPlace);
    updateUnits();

  }

  private void refreshUndoButton() throws NumberFormatException
  {
    String reply = ((StringMessage) m_bridge.sendMessage(new PlaceCountQueryMessage())).getMessage();
    int placeCount = Integer.parseInt( reply);
    UNDO_PLACE_ACTION.setEnabled(placeCount > 0);
  }

  private void refreshActionLabelText(boolean bid)
  {
    actionLabel.setText(getCurrentPlayer().getName() + " place" + (bid ? " for bid" : ""));
  }

  public PlaceMessage waitForPlace(boolean bid, PlayerBridge bridge)
  {
    m_bridge = bridge;
    refreshActionLabelText(bid);
    refreshUndoButton();
    try
    {
      synchronized(getLock())
      {
        getMap().addMapSelectionListener(PLACE_MAP_SELECTION_LISTENER);
        getLock().wait();
        getMap().removeMapSelectionListener(PLACE_MAP_SELECTION_LISTENER);
      }
    } catch(InterruptedException ie)
    {
      return waitForPlace(bid, bridge);
    }

    removeAll();
    m_bridge = null;
    SwingUtilities.invokeLater(REFRESH);
    return m_placeMessage;
  }

  private final AbstractAction UNDO_PLACE_ACTION = new AbstractAction("Undo Last Placement")
  {
    public void actionPerformed(ActionEvent e)
    {
       m_bridge.sendMessage(new UndoPlaceMessage());
       refreshUndoButton();
       updateUnits();
       validate();
    }
  };


  private final AbstractAction DONE_PLACE_ACTION = new AbstractAction("Done")
  {
    public void actionPerformed(ActionEvent e)
    {
      if (getCurrentPlayer().getUnits().size() > 0) {
        int option = JOptionPane.showConfirmDialog((JFrame) getTopLevelAncestor(),"You have not placed all your units yet.  Are you sure you want to end your turn?", "TripleA", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.NO_OPTION)
          return;
      }

      synchronized(getLock())
      {
        m_placeMessage = null;
        getLock().notify();
      }
    }
  };

  private boolean canProduceFightersOnCarriers()
  {
      Boolean property = (Boolean) getData().getProperties().get(Constants.CAN_PRODUCE_FIGHTERS_ON_CARRIERS);
      if(property == null)
          return false;
      return property.booleanValue();
  }
  
  private final MapSelectionListener PLACE_MAP_SELECTION_LISTENER = new DefaultMapSelectionListener()
  {
    public void territorySelected(Territory territory, MouseEvent e)
    {
      if(!getActive())
        return;

      int maxUnits[] = new int[1];
      Collection units = getUnitsToPlace(territory, maxUnits);
      if (units.isEmpty())
          return;

      UnitChooser chooser = new UnitChooser(units, Collections.EMPTY_MAP, getData(), false);
      String messageText = "Place units in " + territory.getName();
      if (maxUnits[0] > 0)
          chooser.setMax(maxUnits[0]);
      
      int option = JOptionPane.showOptionDialog( (JFrame) getTopLevelAncestor(), chooser, messageText, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
      if(option == JOptionPane.OK_OPTION)
      {
        Collection choosen = chooser.getSelected();
        PlaceMessage message = new PlaceMessage(choosen, territory);
        m_placeMessage = message;
        updateUnits();
        synchronized(getLock())
        {
          getLock().notify();
        }
      }
    }
  };

  private Collection getUnitsToPlace(Territory territory, int maxUnits[])
  {
      //not our territory
      if(!territory.isWater() && !territory.getOwner().equals(getCurrentPlayer()))
          return Collections.EMPTY_LIST;

      //get the units that can be placed on this territory.
      Collection units = getCurrentPlayer().getUnits().getUnits();
      if(territory.isWater())
      {
          if(!canProduceFightersOnCarriers())
              units = Match.getMatches(units, Matches.UnitIsSea);
          else
          {
              CompositeMatch unitIsSeaOrCanLandOnCarrier = new CompositeMatchOr(Matches.UnitIsSea, Matches.UnitCanLandOnCarrier);
              units = Match.getMatches(units, unitIsSeaOrCanLandOnCarrier);
          }
      }
       else
           units = Match.getMatches(units, Matches.UnitIsNotSea);

       if(units.isEmpty())
           return Collections.EMPTY_LIST;
      
       Message msg = new ProductionRequestMessage(units, territory);
       Message response = m_bridge.sendMessage(msg);
       
       if (! (response instanceof ProductionResponseMessage))
           throw new IllegalStateException("Message of wrong type:" + response);

       ProductionResponseMessage production = (ProductionResponseMessage) response;
       if (production.isError())
       {
           JOptionPane.showMessageDialog(getTopLevelAncestor(), production.getMessage(), "No units", JOptionPane.INFORMATION_MESSAGE);
           return Collections.EMPTY_LIST;
       }
       
       maxUnits[0] = production.getMaxUnits();
       return production.getUnits();
  }
  
  private void updateUnits()
  {
    Collection unitCategories = UnitSeperator.categorize(getCurrentPlayer().getUnits().getUnits());
    m_unitsToPlace.setUnitsFromCategories(unitCategories, getData());
  }

  public String toString()
  {
    return "PlacePanel";
  }



}
