package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.util.IntegerMap;

import java.io.Serializable;
import java.util.Collection;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company:
 * </p>
 * 
 * @author unascribed
 * @version 1.0
 */
public class BidPurchaseDelegate extends PurchaseDelegate
{
	private int m_bid;
	private int m_spent;
	private boolean m_hasBid = false;
	
	private static int getBidAmount(final GameData data, final PlayerID currentPlayer)
	{
		final String propertyName = currentPlayer.getName() + " bid";
		final int bid = Integer.parseInt(data.getProperties().get(propertyName, "0").toString());
		return bid;
	}
	
	public static boolean doesPlayerHaveBid(final GameData data, final PlayerID player)
	{
		return getBidAmount(data, player) != 0;
	}
	
	/**
	 * subclasses can over ride this method to use different restrictions as to what a player can buy
	 */
	@Override
	protected boolean canAfford(final IntegerMap<Resource> costs, final PlayerID player)
	{
		ResourceCollection bidCollection =  new ResourceCollection(getData());
		bidCollection.addResource(getData().getResourceList().getResource(Constants.PUS), m_bid); // TODO: allow bids to have more than just PUs
		return bidCollection.has(costs);
	}
	
	@Override
	public void start(final IDelegateBridge bridge)
	{
		super.start(bridge);
		if (m_hasBid)
			return;
		m_bid = getBidAmount(bridge.getData(), bridge.getPlayerID());
		m_spent = 0;
	}
	
	@Override
	protected String removeFromPlayer(final PlayerID player, final IntegerMap<Resource> resources, final CompositeChange change, final Collection<Unit> units)
	{
		m_spent = resources.getInt(super.getData().getResourceList().getResource(Constants.PUS));
		return (m_bid - m_spent) + " PU unused";
	}
	
	/**
	 * Called before the delegate will stop running.
	 */
	@Override
	public void end()
	{
		super.end();
		final int unspent = m_bid - m_spent;
		if (unspent == 0)
			return;
		m_bridge.getHistoryWriter().startEvent(m_bridge.getPlayerID().getName() + " retains " + unspent + " PUS not spent in bid phase");
		final Change unspentChange = ChangeFactory.changeResourcesChange(m_bridge.getPlayerID(), super.getData().getResourceList().getResource(Constants.PUS), unspent);
		m_bridge.addChange(unspentChange);
		m_hasBid = false;
	}
	
	@Override
	public Serializable saveState()
	{
		final BidPurchaseExtendedDelegateState state = new BidPurchaseExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		state.m_bid = m_bid;
		state.m_hasBid = m_hasBid;
		state.m_spent = this.m_spent;
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final BidPurchaseExtendedDelegateState s = (BidPurchaseExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
		m_bid = s.m_bid;
		m_spent = s.m_spent;
		m_hasBid = s.m_hasBid;
	}
}


@SuppressWarnings("serial")
class BidPurchaseExtendedDelegateState implements Serializable
{
	Serializable superState;
	// add other variables here:
	int m_bid;
	int m_spent;
	boolean m_hasBid;
}
