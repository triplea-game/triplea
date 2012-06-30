package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.triplea.delegate.DelegateFinder;

/**
 * 
 * @author veqryn
 * 
 */
public class MoveForumPosterPanel extends AbstractForumPosterPanel
{
	private static final long serialVersionUID = -533962696697230277L;
	
	public MoveForumPosterPanel(final GameData data, final MapPanel map)
	{
		super(data, map);
	}
	
	@Override
	protected String getTitle()
	{
		return "Move Summary";
	}
	
	@Override
	protected boolean allowIncludeTerritorySummary()
	{
		return false;
	}
	
	@Override
	protected boolean allowIncludeProductionSummary()
	{
		return false;
	}
	
	@Override
	protected boolean allowDiceBattleDetails()
	{
		return false;
	}
	
	@Override
	public String toString()
	{
		return "MoveForumPosterPanel";
	}
	
	@Override
	protected boolean getHasPostedTurnSummary()
	{
		return false;
	}
	
	@Override
	protected void setHasPostedTurnSummary(final boolean posted)
	{
		// nothing
	}
	
	@Override
	public void waitForDone(final TripleAFrame frame, final IPlayerBridge bridge)
	{
		super.waitForDone(frame, bridge);
	}
	
	@Override
	protected boolean postTurnSummary(final PBEMMessagePoster poster)
	{
		return DelegateFinder.moveDelegate(getData()).postTurnSummary(poster, getTitle());
	}
	
	@Override
	protected boolean skipPosting()
	{
		return !m_poster.alsoPostMoveSummary();
	}
}
