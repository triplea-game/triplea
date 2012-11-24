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
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.pbem.IEmailSender;
import games.strategy.engine.pbem.IForumPoster;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.engine.random.IRandomStats;
import games.strategy.triplea.ui.history.HistoryLog;
import games.strategy.ui.ProgressWindow;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
	protected HistoryLog m_historyLog;
	protected JButton m_postButton;
	protected JCheckBox m_includeTerritoryCheckbox;
	protected JCheckBox m_includeProductionCheckbox;
	protected JCheckBox m_showDetailsCheckbox;
	protected JCheckBox m_showDiceStatisticsCheckbox;
	protected Action m_viewAction;
	protected Action m_postAction;
	protected Action m_includeTerritoryAction;
	protected Action m_includeProductionAction;
	protected Action m_showDetailsAction;
	protected Action m_showDiceStatisticsAction;
	protected Action m_doneAction;
	
	abstract protected String getTitle();
	
	public AbstractForumPosterPanel(final GameData data, final MapPanel map)
	{
		super(data, map);
		m_actionLabel = new JLabel();
		m_viewAction = new AbstractAction("View " + getTitle())
		{
			private static final long serialVersionUID = -2619980789206699839L;
			
			public void actionPerformed(final ActionEvent event)
			{
				m_historyLog.setVisible(true);
			}
		};
		m_postAction = new AbstractAction("Post " + getTitle())
		{
			private static final long serialVersionUID = 8317441736305744524L;
			
			public void actionPerformed(final ActionEvent event)
			{
				updateHistoryLog();
				String message = "";
				final IForumPoster turnSummaryMsgr = m_poster.getForumPoster();
				
				final StringBuilder sb = new StringBuilder();
				if (turnSummaryMsgr != null)
				{
					sb.append(message).append("Post " + getTitle() + " ");
					if (turnSummaryMsgr.getIncludeSaveGame())
					{
						sb.append("and save game ");
					}
					sb.append("to ").append(turnSummaryMsgr.getDisplayName()).append("?\n");
				}
				
				final IEmailSender emailSender = m_poster.getEmailSender();
				if (emailSender != null)
				{
					sb.append("Send email to ").append(emailSender.getToAddress()).append("?\n");
				}
				
				message = sb.toString();
				final int choice = JOptionPane.showConfirmDialog(getTopLevelAncestor(), message, "Post " + getTitle() + "?", 2, -1, null);
				if (choice != 0)
				{
					return;
				}
				else
				{
					m_postButton.setEnabled(false);
					final ProgressWindow progressWindow = new ProgressWindow(m_frame, "Posting " + getTitle() + "...");
					progressWindow.setVisible(true);
					final Runnable t = new Runnable()
					{
						public void run()
						{
							boolean postOk = true;
							
							File saveGameFile = null;
							
							try
							{
								saveGameFile = File.createTempFile("triplea", ".tsvg");
								if (saveGameFile != null)
								{
									m_frame.getGame().saveGame(saveGameFile);
									m_poster.setSaveGame(saveGameFile);
								}
							} catch (final Exception e)
							{
								postOk = false;
								e.printStackTrace();
							}
							
							m_poster.setTurnSummary(m_historyLog.toString());
							
							try
							{
								// forward the poster to the delegate which invokes post() on the poster
								if (!postTurnSummary(m_poster))
									postOk = false;
							} catch (final Exception e)
							{
								postOk = false;
								e.printStackTrace();
							}
							
							final StringBuilder sb = new StringBuilder();
							if (m_poster.getForumPoster() != null)
							{
								final String saveGameRef = m_poster.getSaveGameRef();
								final String turnSummaryRef = m_poster.getTurnSummaryRef();
								
								if (saveGameRef != null)
									sb.append("\nSave Game : ").append(saveGameRef);
								if (turnSummaryRef != null)
									sb.append("\nSummary Text: ").append(turnSummaryRef);
							}
							if (m_poster.getEmailSender() != null)
							{
								sb.append("\nEmails: ").append(m_poster.getEmailSendStatus());
							}
							
							m_historyLog.getWriter().println(sb.toString());
							
							if (m_historyLog.isVisible()) // todo(kg) I think this is a brain fart, unless is is a workaround for some bug
								m_historyLog.setVisible(true);
							try
							{
								if (saveGameFile != null && !saveGameFile.delete())
									System.err.println((new StringBuilder()).append("couldn't delete ").append(saveGameFile.getCanonicalPath()).toString());
							} catch (final IOException ioe)
							{
								ioe.printStackTrace();
							}
							progressWindow.setVisible(false);
							progressWindow.removeAll();
							progressWindow.dispose();
							setHasPostedTurnSummary(postOk);
							
							final boolean finalPostOk = postOk;
							final String finalMessage = sb.toString();
							final Runnable runnable = new Runnable()
							{
								public void run()
								{
									m_postButton.setEnabled(!finalPostOk);
									if (finalPostOk)
										JOptionPane.showMessageDialog(m_frame, finalMessage, getTitle() + " Posted", JOptionPane.INFORMATION_MESSAGE);
									else
										JOptionPane.showMessageDialog(m_frame, finalMessage, getTitle() + " Posted", JOptionPane.ERROR_MESSAGE);
								}
							};
							SwingUtilities.invokeLater(runnable);
						}
					};
					// start a new thread for posting the summary.
					new Thread(t).start();
				}
			}
		};
		m_includeTerritoryAction = new AbstractAction("Include territory summary")
		{
			private static final long serialVersionUID = 207279881318712095L;
			
			public void actionPerformed(final ActionEvent event)
			{
				updateHistoryLog();
			}
		};
		m_includeProductionAction = new AbstractAction("Include production summary")
		{
			private static final long serialVersionUID = 2298448099326090293L;
			
			public void actionPerformed(final ActionEvent event)
			{
				updateHistoryLog();
			}
		};
		m_showDetailsAction = new AbstractAction("Show dice/battle details")
		{
			private static final long serialVersionUID = -4248518090232071926L;
			
			public void actionPerformed(final ActionEvent event)
			{
				updateHistoryLog();
			}
		};
		m_showDiceStatisticsAction = new AbstractAction("Include overall dice statistics")
		{
			private static final long serialVersionUID = 1431745626173286692L;
			
			public void actionPerformed(final ActionEvent event)
			{
				updateHistoryLog();
			}
		};
		m_doneAction = new AbstractAction("Done")
		{
			private static final long serialVersionUID = -3658752576117043053L;
			
			public void actionPerformed(final ActionEvent event)
			{
				release();
			}
		};
		m_includeTerritoryCheckbox = new JCheckBox(m_includeTerritoryAction);
		m_includeProductionCheckbox = new JCheckBox(m_includeProductionAction);
		m_showDetailsCheckbox = new JCheckBox(m_showDetailsAction);
		m_showDiceStatisticsCheckbox = new JCheckBox(m_showDiceStatisticsAction);
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
	
	abstract protected boolean postTurnSummary(final PBEMMessagePoster poster);
	
	@Override
	abstract public String toString();
	
	private void updateHistoryLog()
	{
		m_historyLog.clear();
		m_historyLog.printFullTurn(getData(), m_showDetailsCheckbox.isSelected());
		if (m_includeTerritoryCheckbox.isSelected())
			m_historyLog.printTerritorySummary(getData());
		if (m_includeProductionCheckbox.isSelected())
			m_historyLog.printProductionSummary(getData());
		if (m_showDiceStatisticsCheckbox.isSelected())
			m_historyLog.printDiceStatistics(getData(), (IRandomStats) m_frame.getGame().getRemoteMessenger().getRemote(IRandomStats.RANDOM_STATS_REMOTE_NAME));
		m_historyLog.requestFocus();
	}
	
	abstract protected boolean getHasPostedTurnSummary();
	
	abstract protected void setHasPostedTurnSummary(boolean posted);
	
	abstract protected boolean skipPosting();
	
	protected void waitForDone(final TripleAFrame frame, final IPlayerBridge bridge)
	{
		m_frame = frame;
		m_bridge = bridge;
		// Nothing to do if there are no PBEM messengers
		m_poster = new PBEMMessagePoster(getData(), getCurrentPlayer(), getRound(), getTitle());
		if (!m_poster.hasMessengers())
			return;
		if (skipPosting())
			return;
		
		final boolean hasPosted = getHasPostedTurnSummary();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_historyLog = new HistoryLog();
				updateHistoryLog();
				// only show widgets if there are PBEM messengers
				removeAll();
				add(m_actionLabel);
				if (allowIncludeTerritorySummary())
					add(m_includeTerritoryCheckbox);
				if (allowIncludeProductionSummary())
					add(m_includeProductionCheckbox);
				if (allowDiceBattleDetails())
					add(m_showDetailsCheckbox);
				if (allowDiceStatistics())
					add(m_showDiceStatisticsCheckbox);
				add(new JButton(m_viewAction));
				m_postButton = new JButton(m_postAction);
				m_postButton.setEnabled(!hasPosted);
				add(m_postButton);
				add(new JButton(m_doneAction));
				validate();
			}
		});
		waitForRelease();
	}
}
