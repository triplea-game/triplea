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

package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.dataObjects.MustMoveWithDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;


public class EditPanel extends ActionPanel
{
    
    private TripleAFrame m_frame;
    private Action m_addUnitsAction;
    private Action m_delUnitsAction;
    private Action m_changeIPCsAction;
    //TODO COMCO  private Action m_changeTechTokensAction;
    private Action m_changeTerritoryOwnerAction;
    private Action m_currentAction = null;
    private JLabel m_actionLabel;
    private boolean m_active = false;

    private Point m_mouseSelectedPoint;
    private Point m_mouseCurrentPoint;
    
    //use a LinkedHashSet because we want to know the order
    private final Set<Unit> m_selectedUnits = new LinkedHashSet<Unit>();
    private Territory m_selectedTerritory = null;
    private Territory m_currentTerritory = null;

    public EditPanel(GameData data, MapPanel map, TripleAFrame frame)
    {
        super(data, map);
        m_frame = frame;
        m_actionLabel = new JLabel();

        m_addUnitsAction = new AbstractAction("Add Units") {
            public void actionPerformed(ActionEvent event)
            {
                m_currentAction = this;
                setWidgetActivation();

                // TODO: change cursor to select territory
                
                // continued in territorySelected() handler below
                
            }
        };

        m_delUnitsAction = new AbstractAction("Remove Selected Units") {
            public void actionPerformed(ActionEvent event)
            {
                m_currentAction = this;
                setWidgetActivation();

                List<Unit> allUnits = new ArrayList<Unit>(m_selectedTerritory.getUnits().getUnits());
                sortUnitsToRemove(allUnits, m_selectedTerritory);

                MustMoveWithDetails mustMoveWithDetails = MoveValidator.getMustMoveWith(m_selectedTerritory, 
                        allUnits,
                        getData(),
                        getCurrentPlayer());


                
                boolean mustChoose = false;
                if(m_selectedUnits.containsAll(allUnits)) {
                    mustChoose = false;
                } else  {
                    
                    //if the unit choice is ambiguous then ask the user to clarify which units to remove
                    //an ambiguous selection would be if the user selects 1 of 2 tanks, but
                    //the tanks have different movement.
                    
                    final Set<UnitType> selectedUnitTypes = new HashSet<UnitType>();
                    for(Unit u : m_selectedUnits) {
                        selectedUnitTypes.add(u.getType());
                    }
                    List<Unit> allOfCorrectType = Match.getMatches(allUnits, new Match<Unit>()
                    {
                    
                        @Override
                        public boolean match(Unit o)
                        {
                            return selectedUnitTypes.contains(o.getType());
                        }
                    });
                    
                    int allCategories = UnitSeperator.categorize(allOfCorrectType, mustMoveWithDetails.getMustMoveWith(), true,true).size();
                    int selectedCategories = UnitSeperator.categorize(m_selectedUnits, mustMoveWithDetails.getMustMoveWith(), true,true).size();

                    mustChoose = (allCategories != selectedCategories);                     
                }

                Collection<Unit> bestUnits;
                if(mustChoose) {
                    
                    String chooserText = "Remove units from " + m_selectedTerritory + ":";
    
                    UnitChooser chooser = new UnitChooser(allUnits,m_selectedUnits,
                                                          mustMoveWithDetails.getMustMoveWith(),  true,                                                          
                                                          getData(), 
                                                          /*allowTwoHit=*/ false, 
                                                          getMap().getUIContext());
    
                    int option = JOptionPane.showOptionDialog(getTopLevelAncestor(),
                                                    chooser, chooserText,
                                                    JOptionPane.OK_CANCEL_OPTION,
                                                    JOptionPane.PLAIN_MESSAGE, null, null, null);
                    
                    if(option != JOptionPane.OK_OPTION)
                    {
                        CANCEL_EDIT_ACTION.actionPerformed(null);
                        return;
                    }
    
                    bestUnits = chooser.getSelected(true);
                } else {
                    bestUnits = new ArrayList<Unit>(m_selectedUnits);
                }
                 

                String result = m_frame.getEditDelegate().removeUnits(m_selectedTerritory, bestUnits);
                if (result != null)
                    JOptionPane.showMessageDialog(getTopLevelAncestor(),
                                                  result,
                                                  MyFormatter.pluralize("Could not remove unit", m_selectedUnits.size()),
                                                  JOptionPane.ERROR_MESSAGE);
                CANCEL_EDIT_ACTION.actionPerformed(null);
            }
        };

        m_changeTerritoryOwnerAction = new AbstractAction("Change Territory Owner") {
            public void actionPerformed(ActionEvent event)
            {
                m_currentAction = this;
                setWidgetActivation();

                // TODO: change cursor to select territory

                // continued in territorySelected() handler below
            }
        };

        m_changeIPCsAction = new AbstractAction("Change IPCs") {
            public void actionPerformed(ActionEvent event)
            {
                m_currentAction = this;
                setWidgetActivation();

                PlayerChooser playerChooser = new PlayerChooser(getData().getPlayerList(), getMap().getUIContext(), false);
                int option;
                option = JOptionPane.showOptionDialog(getTopLevelAncestor(), playerChooser, 
                                                "Select owner of ipcs to change",
                                                JOptionPane.OK_CANCEL_OPTION,
                                                JOptionPane.PLAIN_MESSAGE, null, null, null);
                if(option != JOptionPane.OK_OPTION)
                    return;

                PlayerID player = playerChooser.getSelected();

                Resource ipcs = getData().getResourceList().getResource(Constants.IPCS);
                int oldTotal = player.getResources().getQuantity(ipcs);
                int newTotal = oldTotal;

                JTextField ipcsField = new JTextField(String.valueOf(oldTotal), 4);
                ipcsField.setMaximumSize(ipcsField.getPreferredSize());

                option = JOptionPane.showOptionDialog(getTopLevelAncestor(), new JScrollPane(ipcsField), 
                                                "Select new number of ipcs",
                                                JOptionPane.OK_CANCEL_OPTION,
                                                JOptionPane.PLAIN_MESSAGE, null, null, null);
                if(option != JOptionPane.OK_OPTION)
                    return;

                try 
                {
                    newTotal = Integer.parseInt(ipcsField.getText());
                } catch (Exception e)
                {
                }

                String result = m_frame.getEditDelegate().changeIPCs(player, newTotal);
                if (result != null)
                    JOptionPane.showMessageDialog(getTopLevelAncestor(),
                                                  result,
                                                  "Could not perform edit",
                                                  JOptionPane.ERROR_MESSAGE);

                CANCEL_EDIT_ACTION.actionPerformed(null);
            }
        };


        m_actionLabel.setText("Edit Mode Actions");

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(5, 5, 0, 0));

        add(m_actionLabel);
        add(new JButton(m_addUnitsAction));
        add(new JButton(m_delUnitsAction));
        add(new JButton(m_changeTerritoryOwnerAction));
        add(new JButton(m_changeIPCsAction));
        add(Box.createVerticalStrut(15));

        setWidgetActivation();

    }

    private void sortUnitsToRemove(final List<Unit> units, /*final MustMoveWithDetails mustMoveWith,*/ final Territory territory)
    {
        if(units.isEmpty())
            return;

        // sort units based on which transports are allowed to unload
        Collections.sort(units,getRemovableUnitsOrder(units, /*mustMoveWith,*/ territory, true));
    }

    public static Comparator<Unit> getRemovableUnitsOrder(final List<Unit> units, 
                                                          final Territory territory, 
                                                          final boolean noTies)
    {
        Comparator<Unit> removableUnitsOrder = new Comparator<Unit>()
        {
            public int compare(Unit unit1, Unit unit2)
            {
                TripleAUnit u1 = TripleAUnit.get(unit1);
                TripleAUnit u2 = TripleAUnit.get(unit2);

                if (UnitAttachment.get(u1.getType()).getTransportCapacity() != -1)
                {
                    // sort transports
                    
                    Collection<Unit> transporting1 = u1.getTransporting();
                    Collection<Unit> transporting2 = u2.getTransporting();
                    if (transporting1 == null)
                        transporting1 = Collections.emptyList();
                    if (transporting2 == null)
                        transporting2 = Collections.emptyList();

                    // sort by decreasing transport capacity
                    int cost1 = MoveValidator.getTransportCost(transporting1);
                    int cost2 = MoveValidator.getTransportCost(transporting2);
                    if (cost1 != cost2)
                        return cost2 - cost1;
                }

                // sort by increasing movement left
                int left1 = u1.getMovementLeft();
                int left2 = u2.getMovementLeft();
                if (left1 != left2)
                    return left1 - left2;                 

                // if noTies is set, sort by hashcode so that result is deterministic
                if (noTies)                 
                    return u1.hashCode() - u2.hashCode();                 
                else               
                    return 0;                 
            }
        };
        return removableUnitsOrder;
    }

    private void setWidgetActivation()
    {
        if (m_frame.getEditDelegate() == null)
        {
            // current turn belongs to remote player or AI player
            m_addUnitsAction.setEnabled(false);
            m_delUnitsAction.setEnabled(false);
            m_changeTerritoryOwnerAction.setEnabled(false);
            m_changeIPCsAction.setEnabled(false);
        }
        else
        {
            m_addUnitsAction.setEnabled(m_currentAction == null && m_selectedUnits.isEmpty());
            m_delUnitsAction.setEnabled(!m_selectedUnits.isEmpty());
            m_changeTerritoryOwnerAction.setEnabled(m_currentAction == null && m_selectedUnits.isEmpty());
            m_changeIPCsAction.setEnabled(m_currentAction == null && m_selectedUnits.isEmpty());
        }
    }

    public String toString()
    {
        return "EditPanel";
    }

    public void setActive(boolean active)
    {
        if (m_frame.getEditDelegate() == null)
        {
            // current turn belongs to remote player or AI player
            getMap().removeMapSelectionListener(MAP_SELECTION_LISTENER);
            getMap().removeUnitSelectionListener(UNIT_SELECTION_LISTENER);
            getMap().removeMouseOverUnitListener(MOUSE_OVER_UNIT_LISTENER);
            setWidgetActivation();
        }
        else if (!m_active && active)
        {
            getMap().addMapSelectionListener(MAP_SELECTION_LISTENER);
            getMap().addUnitSelectionListener(UNIT_SELECTION_LISTENER);
            getMap().addMouseOverUnitListener(MOUSE_OVER_UNIT_LISTENER);
            setWidgetActivation();
        }
        else if (!active && m_active)
        {
            getMap().removeMapSelectionListener(MAP_SELECTION_LISTENER);
            getMap().removeUnitSelectionListener(UNIT_SELECTION_LISTENER);
            getMap().removeMouseOverUnitListener(MOUSE_OVER_UNIT_LISTENER);
            CANCEL_EDIT_ACTION.actionPerformed(null);
        }
        m_active = active;
    }

    public boolean getActive()
    {
        return m_active;
    }

    private final UnitSelectionListener UNIT_SELECTION_LISTENER = new UnitSelectionListener()
    {
    
        public void unitsSelected(List<Unit> units, Territory t, MouseDetails md)
        {
            //check if we can handle this event, are we active?
            if(!getActive())
                return;
            
            if(t == null)
                return;

            if (m_currentAction != null)
                return;
          
            boolean rightMouse = md.isRightButton();

            if (!m_selectedUnits.isEmpty() && !(m_selectedTerritory == t))
            {
                deselectUnits(new ArrayList<Unit>(m_selectedUnits), t, md);
                m_selectedTerritory = null;
            }

            if(rightMouse && (m_selectedTerritory == t))
            {
                deselectUnits(units, t, md);
            }

            if (!rightMouse && (m_currentAction == m_addUnitsAction))
            {
                // clicking on unit or territory selects territory
                m_selectedTerritory = t;
                MAP_SELECTION_LISTENER.territorySelected(t, md);
            }
            else if (!rightMouse)
            {
                // delete units
                selectUnitsToRemove(units, t, md);
            }

            setWidgetActivation();
        }

        private void deselectUnits(List<Unit> units, Territory t, MouseDetails md)
        {         
            // no unit selected, deselect the most recent
            if(units.isEmpty())
            {
                if(md.isControlDown() || t != m_selectedTerritory)
                    m_selectedUnits.clear();
                else
                    //remove the last element
                    m_selectedUnits.remove( new ArrayList<Unit>(m_selectedUnits).get(m_selectedUnits.size() -1 ) );
            }
            // user has clicked on a specific unit
            else
            {
                // deselect all if control is down
                if(md.isControlDown() || t != m_selectedTerritory)
                {
                    m_selectedUnits.removeAll(units);
                }
                // deselect one
                else
                {
                    //remove those with the least movement first
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
            
            // nothing left, cancel edit
            if(m_selectedUnits.isEmpty())
                CANCEL_EDIT_ACTION.actionPerformed(null);
            else
                getMap().setMouseShadowUnits(m_selectedUnits);
        }

        private void selectUnitsToRemove(List<Unit> units, Territory t, MouseDetails md)
        {
            if(units.isEmpty() && m_selectedUnits.isEmpty())
            {
                if(!md.isShiftDown())
                {
                    
                    Collection<Unit> unitsToMove = t.getUnits().getUnits();
                    
                    if(unitsToMove.isEmpty())
                        return;
                    
                    String text = "Remove from " + t.getName();
                    
                    UnitChooser chooser = new UnitChooser(unitsToMove, m_selectedUnits,
                            null, false, getData(),  false, getMap().getUIContext() );
                                        
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
            
            if(m_selectedTerritory == null)
            {
                m_selectedTerritory = t;
                
                m_mouseSelectedPoint = md.getMapPoint();
                m_mouseCurrentPoint = md.getMapPoint();
                
                CANCEL_EDIT_ACTION.setEnabled(true);                
            }
            
            // select all
            if(md.isShiftDown())
            {
                m_selectedUnits.addAll(t.getUnits().getUnits());
            }
            else if(md.isControlDown())
            {
                m_selectedUnits.addAll(units);
            }
            // select one
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

            Route defaultRoute = getData().getMap().getRoute(m_selectedTerritory, m_selectedTerritory);
            getMap().setRoute(defaultRoute, m_mouseSelectedPoint, m_mouseCurrentPoint, null);
            getMap().setMouseShadowUnits(m_selectedUnits);
        }

    };
    
    private final MouseOverUnitListener MOUSE_OVER_UNIT_LISTENER = new MouseOverUnitListener()
    {

        public void mouseEnter(List<Unit> units, Territory territory, MouseDetails md)
        {
            if(!getActive())
                return;

            if (m_currentAction != null)
                return;

            if(!units.isEmpty())
                getMap().setUnitHighlight(units, territory);
            else
                getMap().setUnitHighlight(null, null);
        }
        
    };

    private final MapSelectionListener MAP_SELECTION_LISTENER = new
        DefaultMapSelectionListener()
    {
        public void territorySelected(Territory territory, MouseDetails md)
        {
            if (territory == null)
                return;

            if (m_currentAction == m_changeTerritoryOwnerAction)
            {
                PlayerID defaultPlayer = TerritoryAttachment.get(territory).getOriginalOwner();
                PlayerChooser playerChooser = new PlayerChooser(getData().getPlayerList(), defaultPlayer, getMap().getUIContext(), true);
                int option;
                option = JOptionPane.showOptionDialog(getTopLevelAncestor(), playerChooser, 
                                                "Select new owner for territory",
                                                JOptionPane.OK_CANCEL_OPTION,
                                                JOptionPane.PLAIN_MESSAGE, null, null, null);

                PlayerID player = playerChooser.getSelected();

                if(option == JOptionPane.OK_OPTION && player != null)
                {

                    String result = m_frame.getEditDelegate().changeTerritoryOwner(territory, player);
                    if (result != null)
                        JOptionPane.showMessageDialog(getTopLevelAncestor(),
                                                      result,
                                                      "Could not perform edit",
                                                      JOptionPane.ERROR_MESSAGE);

                }

                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        CANCEL_EDIT_ACTION.actionPerformed(null);
                    }
                });
            }
            else if (m_currentAction == m_addUnitsAction)
            {
                PlayerChooser playerChooser = new PlayerChooser(getData().getPlayerList(), getMap().getUIContext(), false);
                int option;
                option = JOptionPane.showOptionDialog(getTopLevelAncestor(), playerChooser, 
                                                "Select owner for new units",
                                                JOptionPane.OK_CANCEL_OPTION,
                                                JOptionPane.PLAIN_MESSAGE, null, null, null);

                PlayerID player = playerChooser.getSelected();

                if(option == JOptionPane.OK_OPTION && player != null)
                {

                    // open production panel for adding new units
                    IntegerMap<ProductionRule> production = EditProductionPanel.getProduction(player, m_frame, getData(), getMap().getUIContext());

                    Collection<Unit> units = new ArrayList<Unit>();
                    for (ProductionRule productionRule : production.keySet())
                    {
                        int quantity = production.getInt(productionRule);
                        UnitType type = (UnitType) productionRule.getResults().keySet().iterator().next();
                        units.addAll(type.create(quantity, player));
                    }

                    String result = m_frame.getEditDelegate().addUnits(territory, units);
                    if (result != null)
                        JOptionPane.showMessageDialog(getTopLevelAncestor(),
                                                      result,
                                                      "Could not perform edit",
                                                      JOptionPane.ERROR_MESSAGE);
                }

                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        CANCEL_EDIT_ACTION.actionPerformed(null);
                    }
                });
            }
        }
        
        public void mouseMoved(Territory territory, MouseDetails md)
        {
            if(!getActive())
                return;

            if (territory != null)
            {
                if (m_currentAction == null && m_selectedTerritory != null)
                {
                    m_mouseCurrentPoint= md.getMapPoint();
                    
                    getMap().setMouseShadowUnits(m_selectedUnits);
                }

                // highlight territory
                if (m_currentAction == m_changeTerritoryOwnerAction || m_currentAction == m_addUnitsAction)
                {
                    if(m_currentTerritory != territory) 
                    {
                        if (m_currentTerritory != null)
                            getMap().clearTerritoryOverlay(m_currentTerritory);
                        m_currentTerritory = territory;
                        getMap().setTerritoryOverlay(m_currentTerritory, Color.WHITE, 200);
                        getMap().repaint();
                    }
                }
            }
            
        }
    };

    private final AbstractAction CANCEL_EDIT_ACTION = new AbstractAction(
        "Cancel")
    {
        public void actionPerformed(ActionEvent e)
        {
            m_selectedTerritory = null;
            m_selectedUnits.clear();
            
            this.setEnabled(false);
            getMap().setRoute(null, m_mouseSelectedPoint, m_mouseCurrentPoint, null);
            getMap().setMouseShadowUnits(null);
            if (m_currentTerritory != null)
                getMap().clearTerritoryOverlay(m_currentTerritory);
            m_currentTerritory = null;
            m_currentAction = null;

            setWidgetActivation();
        }
    };
}
