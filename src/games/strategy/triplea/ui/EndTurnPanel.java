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
 * EndTurnPanel.java
 * 
 * Created on December 2, 2006, 10:04 AM
 */
package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;

/**
 * 
 * @author Tony Clayton, but abstracted by Veqryn
 * @version 1.0
 */
public class EndTurnPanel extends AbstractForumPosterPanel
{
	private static final long serialVersionUID = -6282316384529504341L;
	
	public EndTurnPanel(final GameData data, final MapPanel map)
	{
		super(data, map);
	}
	
	@Override
	protected String getTitle()
	{
		return "Turn Summary";
	}
	
	@Override
	public String toString()
	{
		return "EndTurnPanel";
	}
	
	@Override
	protected boolean allowIncludeTerritorySummary()
	{
		return true;
	}
	
	@Override
	protected boolean allowIncludeTerritoryAllPlayersSummary()
	{
		return false;
	}
	
	@Override
	protected boolean allowIncludeProductionSummary()
	{
		return true;
	}
	
	@Override
	protected boolean allowDiceBattleDetails()
	{
		return true;
	}
	
	@Override
	protected boolean allowDiceStatistics()
	{
		return true;
	}
	
	@Override
	protected IAbstractForumPosterDelegate getForumPosterDelegate()
	{
		return (IAbstractForumPosterDelegate) m_bridge.getRemoteDelegate();
	}
	
	@Override
	protected boolean getHasPostedTurnSummary()
	{
		final IAbstractForumPosterDelegate delegate = (IAbstractForumPosterDelegate) m_bridge.getRemoteDelegate();
		return delegate.getHasPostedTurnSummary();
	}
	
	@Override
	protected void setHasPostedTurnSummary(final boolean posted)
	{
		final IAbstractForumPosterDelegate delegate = (IAbstractForumPosterDelegate) m_bridge.getRemoteDelegate();
		delegate.setHasPostedTurnSummary(posted);
	}
	
	@Override
	protected boolean postTurnSummary(final PBEMMessagePoster poster, final boolean includeSaveGame)
	{
		final IAbstractForumPosterDelegate delegate = (IAbstractForumPosterDelegate) m_bridge.getRemoteDelegate();
		return delegate.postTurnSummary(poster, getTitle(), includeSaveGame);
	}
	
	@Override
	protected boolean skipPosting()
	{
		return false;
	}
	
	public void waitForEndTurn(final TripleAFrame frame, final IPlayerBridge bridge)
	{
		super.waitForDone(frame, bridge);
	}
}
