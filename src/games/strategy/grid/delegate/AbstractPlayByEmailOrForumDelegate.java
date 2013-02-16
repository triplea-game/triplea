package games.strategy.grid.delegate;

import games.strategy.common.delegate.AbstractDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;

import java.io.Serializable;

public abstract class AbstractPlayByEmailOrForumDelegate extends AbstractDelegate implements IAbstractForumPosterDelegate
{
	private boolean m_needToInitialize = true;
	private boolean m_hasPostedTurnSummary = false;
	
	@Override
	public void start()
	{
		super.start();
		if (!m_needToInitialize)
			return;
		m_hasPostedTurnSummary = false;
		
		m_needToInitialize = false;// at the very end
	}
	
	@Override
	public void end()
	{
		super.end();
		m_needToInitialize = true;
	}
	
	@Override
	public Serializable saveState()
	{
		final AbstractPlayByEmailOrForumExtendedDelegateState state = new AbstractPlayByEmailOrForumExtendedDelegateState();
		state.superState = super.saveState();
		// add other variables to state here:
		state.m_needToInitialize = m_needToInitialize;
		state.m_hasPostedTurnSummary = m_hasPostedTurnSummary;
		return state;
	}
	
	@Override
	public void loadState(final Serializable state)
	{
		final AbstractPlayByEmailOrForumExtendedDelegateState s = (AbstractPlayByEmailOrForumExtendedDelegateState) state;
		super.loadState(s.superState);
		// load other variables from state here:
		m_needToInitialize = s.m_needToInitialize;
		m_hasPostedTurnSummary = s.m_hasPostedTurnSummary;
	}
	
	public boolean stuffToDoInThisDelegate()
	{
		// we could have a pbem/forum post to do
		return PBEMMessagePoster.GameDataHasPlayByEmailOrForumMessengers(getData());
	}
	
	public void setHasPostedTurnSummary(final boolean hasPostedTurnSummary)
	{
		m_hasPostedTurnSummary = hasPostedTurnSummary;
	}
	
	public boolean getHasPostedTurnSummary()
	{
		return m_hasPostedTurnSummary;
	}
	
	public boolean postTurnSummary(final PBEMMessagePoster poster, final String title, final boolean includeSaveGame)
	{
		m_hasPostedTurnSummary = poster.post(m_bridge.getHistoryWriter(), title, includeSaveGame);
		return m_hasPostedTurnSummary;
	}
	
	@Override
	public Class<? extends IRemote> getRemoteType()
	{
		return IAbstractForumPosterDelegate.class;
	}
}


class AbstractPlayByEmailOrForumExtendedDelegateState implements Serializable
{
	private static final long serialVersionUID = 6566058639880616720L;
	Serializable superState;
	// add other variables here:
	public boolean m_needToInitialize;
	public boolean m_hasPostedTurnSummary;
}
