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
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.pbem.ForumPosterComponent;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * 
 * @author Tony Clayton, but abstracted by Veqryn
 * @version 1.0
 */
public abstract class AbstractForumPosterPanel extends ActionPanel
{
	private static final long serialVersionUID = -5084680807785728744L;
	protected JLabel m_actionLabel;
	protected IPlayerBridge m_bridge;
	protected PBEMMessagePoster m_poster;
	protected TripleAFrame m_frame;
	protected Action m_doneAction;
	protected ForumPosterComponent m_forumPosterComponent;
	
	public AbstractForumPosterPanel(final GameData data, final MapPanel map)
	{
		super(data, map);
		m_actionLabel = new JLabel();
		m_doneAction = new AbstractAction("Done")
		{
			private static final long serialVersionUID = -3658752576117043053L;
			
			public void actionPerformed(final ActionEvent event)
			{
				release();
			}
		};
		m_forumPosterComponent = new ForumPosterComponent(getData(), m_doneAction, getTitle());
	}
	
	private int getRound()
	{
		int round = 0;
		final Object pathFromRoot[] = getData().getHistory().getLastNode().getPath();
		final Object arr$[] = pathFromRoot;
		final int len$ = arr$.length;
		int i$ = 0;
		do
		{
			if (i$ >= len$)
				break;
			final Object pathNode = arr$[i$];
			final HistoryNode curNode = (HistoryNode) pathNode;
			if (curNode instanceof Round)
			{
				round = ((Round) curNode).getRoundNo();
				break;
			}
			i$++;
		} while (true);
		return round;
	}
	
	@Override
	public void display(final PlayerID id)
	{
		super.display(id);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_actionLabel.setText(id.getName() + " " + getTitle());
				// defer componenet layout until waitForEndTurn()
			}
		});
	}
	
	abstract protected boolean allowIncludeTerritorySummary();
	
	abstract protected boolean allowIncludeProductionSummary();
	
	abstract protected boolean allowDiceBattleDetails();
	
	abstract protected boolean allowDiceStatistics();
	
	abstract protected IAbstractForumPosterDelegate getForumPosterDelegate();
	
	abstract protected boolean postTurnSummary(final PBEMMessagePoster poster, final boolean includeSaveGame);
	
	abstract protected boolean getHasPostedTurnSummary();
	
	abstract protected void setHasPostedTurnSummary(boolean posted);
	
	abstract protected boolean skipPosting();
	
	abstract protected String getTitle();
	
	@Override
	abstract public String toString();
	
	protected void waitForDone(final TripleAFrame frame, final IPlayerBridge bridge)
	{
		m_frame = frame;
		m_bridge = bridge;
		// Nothing to do if there are no PBEM messengers
		m_poster = new PBEMMessagePoster(getData(), getCurrentPlayer(), getRound(), getTitle());
		if (!m_poster.hasMessengers())
			return;
		if (skipPosting() || Boolean.parseBoolean(m_bridge.getStepProperties().getProperty(GameStep.PROPERTY_skipPosting, "false")))
			return;
		
		final boolean hasPosted = getHasPostedTurnSummary();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				removeAll();
				add(m_actionLabel);
				add(m_forumPosterComponent.layoutComponents(m_poster, getForumPosterDelegate(), m_bridge, m_frame, hasPosted,
							allowIncludeTerritorySummary(), allowIncludeProductionSummary(), allowDiceBattleDetails(), allowDiceStatistics()));
				validate();
			}
		});
		waitForRelease();
	}
}
