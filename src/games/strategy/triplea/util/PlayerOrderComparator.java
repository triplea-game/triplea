package games.strategy.triplea.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameSequence;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegate;

import java.util.Comparator;

public class PlayerOrderComparator implements Comparator<PlayerID>
{
	private final GameData m_data;
	
	public PlayerOrderComparator(final GameData data)
	{
		m_data = data;
	}
	
	/**
	 * sort based on first step that isn't a bid related step.
	 */
	public int compare(final PlayerID p1, final PlayerID p2)
	{
		m_data.acquireReadLock(); // TODO: see is needed
		final GameSequence sequence = m_data.getSequence();
		m_data.releaseReadLock();
		for (final GameStep s : sequence)
		{
			if (s.getPlayerID() == null)
				continue;
			m_data.acquireReadLock(); // TODO: see is needed
			final IDelegate delegate = s.getDelegate();
			m_data.releaseReadLock();
			if (delegate != null && delegate.getClass() != null)
			{
				final String delegateClassName = delegate.getClass().getName();
				if (delegateClassName.equals("games.strategy.triplea.delegate.InitializationDelegate") || delegateClassName.equals("games.strategy.triplea.delegate.BidPurchaseDelegate")
							|| delegateClassName.equals("games.strategy.triplea.delegate.BidPlaceDelegate") || delegateClassName.equals("games.strategy.triplea.delegate.EndRoundDelegate"))
					continue;
			}
			else if (s.getName() != null && (s.getName().endsWith("Bid") || s.getName().endsWith("BidPlace")))
				continue;
			if (s.getPlayerID().equals(p1))
				return -1;
			else if (s.getPlayerID().equals(p2))
				return 1;
		}
		return 0;
	}
}
