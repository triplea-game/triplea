/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/*
 * ActionButtons.java
 * 
 * Created on November 7, 2001, 5:49 PM
 */
package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.triplea.attatchments.PoliticalActionAttachment;
import games.strategy.triplea.delegate.dataObjects.FightBattleDetails;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.dataObjects.TechRoll;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.util.IntegerMap;

import java.awt.CardLayout;
import java.util.Collection;
import java.util.HashMap;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * 
 * Root panel for all action buttons in a triplea game. <br>
 * 
 * @author Sean Bridges
 */
public class ActionButtons extends JPanel
{
	private final CardLayout m_layout = new CardLayout();
	private BattlePanel m_battlePanel;
	private MovePanel m_movePanel;
	private PurchasePanel m_purchasePanel;
	private RepairPanel m_repairPanel;
	private PlacePanel m_placePanel;
	private TechPanel m_techPanel;
	private EndTurnPanel m_endTurnPanel;
	private ActionPanel m_current;
	private PoliticsPanel m_politicsPanel;
	
	/** Creates new ActionPanel */
	public ActionButtons(final GameData data, final MapPanel map, final TripleAFrame parent)
	{
		m_battlePanel = new BattlePanel(data, map);
		m_movePanel = new MovePanel(data, map, parent);
		m_purchasePanel = new PurchasePanel(data, map);
		m_repairPanel = new RepairPanel(data, map);
		m_placePanel = new PlacePanel(data, map, parent);
		m_techPanel = new TechPanel(data, map);
		m_endTurnPanel = new EndTurnPanel(data, map);
		m_politicsPanel = new PoliticsPanel(data, map, parent);
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
		add(m_politicsPanel, m_politicsPanel.toString());
		// this should not be necceessary
		// but it makes tracking down garbage leaks easier
		// in the profiler
		// since it removes a lot of links
		// between objects
		//
		// and if there is a memory leak
		// this will minimize the damage
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
				m_politicsPanel.removeAll();
				m_battlePanel = null;
				m_movePanel = null;
				m_repairPanel = null;
				m_purchasePanel = null;
				m_placePanel = null;
				m_techPanel = null;
				m_endTurnPanel = null;
				m_politicsPanel = null;
			}
		});
	}
	
	public void changeToMove(final PlayerID id, final boolean nonCombat)
	{
		m_movePanel.setNonCombat(nonCombat);
		changeTo(id, m_movePanel);
	}
	
	public void changeToRepair(final PlayerID id)
	{
		changeTo(id, m_repairPanel);
	}
	
	public void changeToProduce(final PlayerID id)
	{
		
		changeTo(id, m_purchasePanel);
	}
	
	public void changeToPlace(final PlayerID id)
	{
		changeTo(id, m_placePanel);
	}
	
	public void changeToBattle(final PlayerID id, final Collection<Territory> battles, final Collection<Territory> bombing)
	{
		m_battlePanel.setBattlesAndBombing(battles, bombing);
		changeTo(id, m_battlePanel);
	}
	
	public void changeToPolitics(final PlayerID id)
	{
		
		changeTo(id, m_politicsPanel);
	}
	
	public void changeToTech(final PlayerID id)
	{
		
		changeTo(id, m_techPanel);
	}
	
	public void changeToEndTurn(final PlayerID id)
	{
		
		changeTo(id, m_endTurnPanel);
	}
	
	private void changeTo(final PlayerID id, final ActionPanel newCurrent)
	{
		m_current.setActive(false);
		m_current = newCurrent;
		m_current.display(id);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_layout.show(ActionButtons.this, m_current.toString());
			}
		});
	}
	
	/**
	 * Blocks until the user selects their purchase.
	 * 
	 * @return null if no move was made.
	 */
	public IntegerMap<ProductionRule> waitForPurchase(final boolean bid)
	{
		return m_purchasePanel.waitForPurchase(bid);
	}
	
	/**
	 * Blocks until the user selects their purchase.
	 * 
	 * @return null if no move was made.
	 */
	public HashMap<Unit, IntegerMap<RepairRule>> waitForRepair(final boolean bid)
	{
		return m_repairPanel.waitForRepair(bid);
	}
	
	/**
	 * Blocks until the user moves units.
	 * 
	 * @return null if no move was made.
	 */
	public MoveDescription waitForMove(final IPlayerBridge bridge)
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
	 * Blocks until the user selects a political action to attempt
	 * 
	 * @param firstRun
	 * 
	 * @return null if no action was picked.
	 */
	public PoliticalActionAttachment waitForPoliticalAction(final boolean firstRun, final IPoliticsDelegate iPoliticsDelegate)
	{
		return m_politicsPanel.waitForPoliticalAction(firstRun, iPoliticsDelegate);
	}
	
	/**
	 * Blocks until the user selects units to place.
	 * 
	 * @return null if no placement was made.
	 */
	public PlaceData waitForPlace(final boolean bid, final IPlayerBridge bridge)
	{
		return m_placePanel.waitForPlace(bid, bridge);
	}
	
	/**
	 * Blocks until the user selects an end-of-turn action
	 * 
	 * @return null if no action was made.
	 */
	public void waitForEndTurn(final TripleAFrame frame, final IPlayerBridge bridge)
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
	
	public AbstractMovePanel getMovePanel()
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
