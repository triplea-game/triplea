package games.strategy.common.delegate;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.delegate.IPersistentDelegate;
import games.strategy.triplea.TripleA;
import games.strategy.triplea.ai.weakAI.WeakAI;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.display.ITripleaDisplay;

/**
 * Base class designed to make writing custom persistent delegates simpler.
 * Code common to all persistent delegates is implemented here.
 * 
 * Do NOT combine this class with "BaseTripleADelegate.java"
 * It is supposed to be separate, as Persistent Delegates do not do many things that normal delegates do, like Triggers, etc.
 * Persistent Delegates are active all the time.
 * 
 * @author Chris Duncan
 */
public abstract class BasePersistentDelegate extends AbstractDelegate implements IDelegate, IPersistentDelegate
{
	public BasePersistentDelegate()
	{
		super();
	}
	
	public void initialize(final String name)
	{
		initialize(name, name);
	}
	
	/**
	 * Called before the delegate will run.
	 * All classes should call super.start if they override this.
	 */
	@Override
	public void start()
	{
		super.start();
	}
	
	/**
	 * Called before the delegate will stop running.
	 * All classes should call super.end if they override this.
	 */
	@Override
	public void end()
	{
		super.end();
	}
	
	protected ITripleaDisplay getDisplay()
	{
		return getDisplay(m_bridge);
	}
	
	protected static ITripleaDisplay getDisplay(final IDelegateBridge bridge)
	{
		return (ITripleaDisplay) bridge.getDisplayChannelBroadcaster();
	}
	
	protected ITripleaPlayer getRemotePlayer()
	{
		return getRemotePlayer(m_bridge);
	}
	
	protected static ITripleaPlayer getRemotePlayer(final IDelegateBridge bridge)
	{
		return (ITripleaPlayer) bridge.getRemote();
	}
	
	protected ITripleaPlayer getRemotePlayer(final PlayerID player)
	{
		return getRemotePlayer(player, m_bridge);
	}
	
	protected static ITripleaPlayer getRemotePlayer(final PlayerID player, final IDelegateBridge bridge)
	{
		// if its the null player, return a do nothing proxy
		if (player.isNull())
			return new WeakAI(player.getName(), TripleA.WEAK_COMPUTER_PLAYER_TYPE);
		return (ITripleaPlayer) bridge.getRemote(player);
	}
}
