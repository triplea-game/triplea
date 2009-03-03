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
 * ActionButtons.java
 *
 * Created on November 7, 2001, 5:49 PM
 */

package games.strategy.triplea.ui;

import games.strategy.engine.data.*;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.triplea.delegate.dataObjects.*;
import games.strategy.util.IntegerMap;

import java.awt.CardLayout;
import java.util.Collection;
import java.util.HashMap;

import javax.swing.*;

/**
 * 
 * Root panel for all action buttons in a triplea game. <br>
 * 
 * @author Sean Bridges
 */
public class ActionButtons extends JPanel
{

    private CardLayout m_layout = new CardLayout();

    private BattlePanel m_battlePanel;

    private MovePanel m_movePanel;

    private PurchasePanel m_purchasePanel;
    
    private RepairPanel m_repairPanel;

    private PlacePanel m_placePanel;

    private TechPanel m_techPanel;

    private EndTurnPanel m_endTurnPanel;
    private ActionPanel m_current;

    /** Creates new ActionPanel */
    public ActionButtons(GameData data, MapPanel map, TripleAFrame parent)
    {
        m_battlePanel = new BattlePanel(data, map);
        m_movePanel = new MovePanel(data, map, parent);
        m_purchasePanel = new PurchasePanel(data, map);
        m_repairPanel = new RepairPanel(data, map);
        m_placePanel = new PlacePanel(data, map);
        m_techPanel = new TechPanel(data, map);
        m_endTurnPanel = new EndTurnPanel(data, map);
        m_current = m_techPanel;

        setLayout(m_layout);

        add(new JLabel(""), "");
        add(m_battlePanel, m_battlePanel.toString());
        add(m_movePanel, m_movePanel.toString());
        add(m_repairPanel, m_repairPanel.toString());
        add(m_purchasePanel, m_purchasePanel.toString());
        add(m_placePanel, m_placePanel.toString());
        add(m_techPanel, m_techPanel.toString());
        add(m_endTurnPanel, m_endTurnPanel.toString());
        
        
        //this should not be necceessary
        //but it makes tracking down garbage leaks easier
        //in the profiler
        //since it removes a lot of links
        //between objects
        //
        //and if there is a memory leak
        //this will minimize the damage
        map.getUIContext().addActive(new Active()
        {        
            public void deactivate()
            {
               removeAll();
               m_current = null;
               
               m_battlePanel.removeAll();
               m_movePanel.removeAll();
               m_repairPanel.removeAll();
               m_purchasePanel.removeAll();
               m_placePanel.removeAll();
               m_techPanel.removeAll();
               m_endTurnPanel.removeAll();
               m_battlePanel = null;
               m_movePanel = null;
               m_repairPanel = null;
               m_purchasePanel = null;
               m_placePanel = null;
               m_techPanel = null;
               m_endTurnPanel = null;

               
            }
        });
        
    }

    public void changeToMove(final PlayerID id, final boolean nonCombat)
    {
        m_current.setActive(false);
        m_current = m_movePanel;
        m_movePanel.display(id, nonCombat);
        
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                
                m_layout.show(ActionButtons.this, m_movePanel.toString());
            }
        });

    }
//TODO COMCO added this
    public void changeToRepair(final PlayerID id)
    {
        m_current.setActive(false);
        m_current = m_repairPanel;
        m_repairPanel.display(id);
    
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                m_layout.show(ActionButtons.this, m_repairPanel.toString());
            }
       });
    }

    public void changeToProduce(final PlayerID id)
    {
        m_current.setActive(false);
        m_current = m_purchasePanel;
        m_purchasePanel.display(id);
    
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                m_layout.show(ActionButtons.this, m_purchasePanel.toString());
            }
       });
    }

    public void changeToPlace(PlayerID id)
    {
        m_current.setActive(false);
        m_current = m_placePanel;
        m_placePanel.display(id);
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                m_layout.show(ActionButtons.this, m_placePanel.toString());
            }
        
        });
        
    }

    public void changeToBattle(PlayerID id, Collection<Territory> battles,
            Collection<Territory> bombing)
    {
        m_current.setActive(false);
        m_current = m_battlePanel;
        m_battlePanel.display(id, battles, bombing);
        
        SwingUtilities.invokeLater(new Runnable()
        {
        
            public void run()
            {
                m_layout.show(ActionButtons.this, m_battlePanel.toString());
            }
        
        });
    }

    public void changeToTech(PlayerID id)
    {
        m_current.setActive(false);
        m_current = m_techPanel;
        m_techPanel.display(id);
        
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                m_layout.show(ActionButtons.this, m_techPanel.toString());
            }
        });
     }

    public void changeToEndTurn(PlayerID id)
    {
        m_current.setActive(false);
        m_current = m_endTurnPanel;
        m_endTurnPanel.display(id);
        SwingUtilities.invokeLater(new Runnable() {

            public void run()
            {
                m_layout.show(ActionButtons.this, m_endTurnPanel.toString());
            }
        });
    }

    /**
     * Blocks until the user selects their purchase.
     * 
     * @return null if no move was made.
     */
    public IntegerMap<ProductionRule> waitForPurchase(boolean bid)
    {
        return m_purchasePanel.waitForPurchase(bid);
    }

    /**
     * Blocks until the user selects their purchase.
     * 
     * @return null if no move was made.
     */
    public HashMap<Territory, IntegerMap<RepairRule>> waitForRepair(boolean bid)
    {
        return m_repairPanel.waitForRepair(bid);
    }

    /**
     * Blocks until the user moves units.
     * 
     * @return null if no move was made.
     */
    public MoveDescription waitForMove(IPlayerBridge bridge)
    {
        return m_movePanel.waitForMove(bridge);
    }

    /**
     * Blocks until the user selects the number of tech rolls.
     * 
     * @return null if no tech roll was made.
     */
    public TechRoll waitForTech()
    {
        return m_techPanel.waitForTech();
    }

    /**
     * Blocks until the user selects units to place.
     * 
     * @return null if no placement was made.
     */
    public PlaceData waitForPlace(boolean bid, IPlayerBridge bridge)
    {
        return m_placePanel.waitForPlace(bid, bridge);
    }

    /**
     * Blocks until the user selects an end-of-turn action
     * 
     * @return null if no action was made.
     */
    public void waitForEndTurn(TripleAFrame frame, IPlayerBridge bridge)
    {
        m_endTurnPanel.waitForEndTurn(frame, bridge);
    }

    /**
     * Blocks until the user selects a battle to fight.
     */
    public FightBattleDetails waitForBattleSelection()
    {
        return m_battlePanel.waitForBattleSelection();
    }


    public ActionPanel getCurrent()
    {
        return m_current;
    }
    
    
    public BattlePanel getBattlePanel()
    {
        return m_battlePanel;
    }
    public MovePanel getMovePanel()
    {
        return m_movePanel;
    }
    public PlacePanel getPlacePanel()
    {
        return m_placePanel;
    }
    public PurchasePanel getPurchasePanel()
    {
        return m_purchasePanel;
    }
    public TechPanel getTechPanel()
    {
        return m_techPanel;
    }
    public EndTurnPanel getEndTurnPanel()
    {
        return m_endTurnPanel;
    }
}
