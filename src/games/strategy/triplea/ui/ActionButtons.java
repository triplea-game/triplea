/*
 * ActionButtons.java
 *
 * Created on November 7, 2001, 5:49 PM
 */

package games.strategy.triplea.ui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import games.strategy.util.*;
import games.strategy.engine.data.*;
import games.strategy.engine.message.Message;
import games.strategy.engine.gamePlayer.PlayerBridge;
import games.strategy.engine.data.events.*;

import games.strategy.triplea.delegate.message.*;

/**
 *
 * Root panel for all action buttons in a triplea game. <br>
 * 
 * @author  Sean Bridges
 * @version 1.0
 */
public class ActionButtons extends JPanel
{
	
	private CardLayout m_layout = new CardLayout();
	
	private JLabel actionLabel = new JLabel("Action");
	
	private BattlePanel m_battlePanel;
	private MovePanel m_movePanel;
	private PurchasePanel m_purchasePanel;
	private PlacePanel m_placePanel;
	private TechPanel m_techPanel;
	
	/** Creates new ActionPanel */
    public ActionButtons(GameData data, MapPanel map) 
	{
		m_battlePanel = new BattlePanel(data, map);
		m_movePanel = new MovePanel(data, map);
		m_purchasePanel = new PurchasePanel(data, map);
		m_placePanel = new PlacePanel(data, map);
		m_techPanel = new TechPanel(data, map);
		
		setLayout(m_layout);
		
		add(new JLabel(""), "");
		add(m_battlePanel, m_battlePanel.toString());
		add(m_movePanel, m_movePanel.toString());
		add(m_purchasePanel, m_purchasePanel.toString());
		add(m_placePanel, m_placePanel.toString());
		add(m_techPanel, m_techPanel.toString());
    }

	public void changeToMove(PlayerID id)
	{
		m_movePanel.display(id);
		m_layout.show(this, m_movePanel.toString());		
	}
	
	public void changeToProduce(PlayerID id)
	{
		m_purchasePanel.display(id);
		m_layout.show(this, m_purchasePanel.toString());
	}

	public void changeToPlace(PlayerID id)
	{
		m_placePanel.display(id);
		m_layout.show(this, m_placePanel.toString());
	}
	
	public void changeToBattle(PlayerID id, Collection battles, Collection bombing)
	{
		m_battlePanel.display(id, battles, bombing);
		m_layout.show(this, m_battlePanel.toString());
	}

	public void changeToTech(PlayerID id)
	{
		m_techPanel.display(id);
		m_layout.show(this, m_techPanel.toString());
	}


	/** 
	 * Blocks until the user selects their purchase.
	 * @return null if no move was made.
	 */
	public IntegerMap waitForPurchase()
	{
		return m_purchasePanel.waitForPurchase();
	}
	
	/**
	 * Blocks until the user moves units.
	 * @return null if no move was made.
	 */
	public MoveMessage waitForMove(PlayerBridge bridge)
	{
		return m_movePanel.waitForMove(bridge);
	}
	
	/** 
	 * Blocks until the user selects the number of tech rolls.
	 * @return null if no tech roll was made.
	 */
	public IntegerMessage waitForTech()
	{
		return m_techPanel.waitForTech();
	}

	public Message listBattle(BattleStepMessage msg)
	{
		return m_battlePanel.listBattle(msg);
	}

	/** 
	 * Blocks until the user selects units to place.
	 * @return null if no placement was made.
	 */
	public PlaceMessage waitForPlace()
	{
		return m_placePanel.waitForPlace();
	}

	/** 
	 * Blocks until the user selects a battle to fight.
	 */
	public FightBattleMessage waitForBattleSelection()
	{
		return m_battlePanel.waitForBattleSelection();
	}
	
	public SelectCasualtyMessage getCasualties(PlayerID player, SelectCasualtyQueryMessage msg)
	{
		return m_battlePanel.getCasualties(player, msg);
	}
	
	public Message battleStringMessage(BattleStringMessage message)
	{
		return m_battlePanel.battleStringMessage(message);
	}
	
	
	/**
	 * Blocks until the user selects a country to retreat to.
	 * @return null if user doesnt retreat.
	 */
	public RetreatMessage getRetreat(RetreatQueryMessage rqm)
	{
		return 	m_battlePanel.getRetreat(rqm);
	}	
}
