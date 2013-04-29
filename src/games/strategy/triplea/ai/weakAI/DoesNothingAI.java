package games.strategy.triplea.ai.weakAI;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.ai.AbstractAI;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;

public class DoesNothingAI extends AbstractAI
{
	public DoesNothingAI(final String name, final String type)
	{
		super(name, type);
	}
	
	@Override
	protected void purchase(final boolean purchaseForBid, final int PUsToSpend, final IPurchaseDelegate purchaseDelegate, final GameData data, final PlayerID player)
	{
		// spend whatever we have
		if (!player.getResources().isEmpty())
			(new WeakAI(this.getName(), this.getType())).purchase(purchaseForBid, PUsToSpend, purchaseDelegate, data, player);
		pause();
	}
	
	@Override
	protected void tech(final ITechDelegate techDelegate, final GameData data, final PlayerID player)
	{
		pause();
	}
	
	@Override
	protected void move(final boolean nonCombat, final IMoveDelegate moveDel, final GameData data, final PlayerID player)
	{
		pause();
	}
	
	@Override
	protected void place(final boolean placeForBid, final IAbstractPlaceDelegate placeDelegate, final GameData data, final PlayerID player)
	{
		// place whatever we have
		if (!player.getUnits().isEmpty())
			(new WeakAI(this.getName(), this.getType())).place(placeForBid, placeDelegate, data, player);
		pause();
	}
	
	@Override
	public void politicalActions()
	{
		pause();
	}
	
	@Override
	protected void endTurn(final IAbstractForumPosterDelegate endTurnForumPosterDelegate, final GameData data, final PlayerID player)
	{
		// destroy whatever we have
		final ResourceCollection resourceCollection = player.getResources();
		final Change removeChange = ChangeFactory.removeResourceCollection(player, resourceCollection);
		// shameless cheating... (do NOT do this, normally you are never supposed to access the IDelegateBridge from outside of a delegate)
		final IDelegateBridge bridge = endTurnForumPosterDelegate.getBridge();
		bridge.getHistoryWriter().startEvent(player.getName() + " removes resources: " + resourceCollection, resourceCollection);
		bridge.addChange(removeChange);
	}
	
	@Override
	public boolean shouldBomberBomb(final Territory territory)
	{
		return false;
	}
	
	@Override
	public boolean acceptAction(final PlayerID playerSendingProposal, final String acceptanceQuestion, final boolean politics)
	{
		// we are dead, just accept
		if (!getPlayerID().amNotDeadYet(getGameData()))
			return true;
		if (!politics)
			return true;
		// if (Math.random() < .5)
		// return true;
		return false;
	}
}
