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
import games.strategy.triplea.delegate.remote.IAbstractEndTurnDelegate;
import games.strategy.triplea.ui.history.HistoryLog;
import games.strategy.ui.ProgressWindow;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

/**
 * 
 * @author Tony Clayton
 * @version 1.0
 */
public class EndTurnPanel extends ActionPanel
{
	private JLabel m_actionLabel;
	private IPlayerBridge m_bridge;
	private PBEMMessagePoster m_poster;
	private TripleAFrame m_frame;
	private HistoryLog m_historyLog;
	private JButton m_postButton;
	private JCheckBox m_includeTerritoryCheckbox;
	private JCheckBox m_includeProductionCheckbox;
	private JCheckBox m_showDetailsCheckbox;
	private Action m_viewAction;
	private Action m_postAction;
	private Action m_includeTerritoryAction;
	private Action m_includeProductionAction;
	private Action m_showDetailsAction;
	private Action m_doneAction;
	
	public EndTurnPanel(final GameData data, final MapPanel map)
	{
		super(data, map);
		m_actionLabel = new JLabel();
		m_viewAction = new AbstractAction("View Turn Summary")
		{
			public void actionPerformed(final ActionEvent event)
			{
				m_historyLog.setVisible(true);
			}
		};
		m_postAction = new AbstractAction("Post Turn Summary")
		{
			public void actionPerformed(final ActionEvent event)
			{
				String message = "";
				final IForumPoster turnSummaryMsgr = m_poster.getForumPoster();
				
				final StringBuilder sb = new StringBuilder();
				if (turnSummaryMsgr != null)
				{
					sb.append(message).append("Post turn summary ");
					if (turnSummaryMsgr.getIncludeSaveGame())
					{
						sb.append(" and save game ");
					}
					sb.append("to ").append(turnSummaryMsgr.getDisplayName()).append("?\n");
				}
				
				final IEmailSender emailSender = m_poster.getEmailSender();
				if (emailSender != null)
				{
					sb.append("Send email to ").append(emailSender.getToAddress()).append("?\n");
				}
				
				message = sb.toString();
				final int choice = JOptionPane.showConfirmDialog(getTopLevelAncestor(), message, "Post Turn Summary?", 2, -1, null);
				if (choice != 0)
				{
					return;
				}
				else
				{
					m_postButton.setEnabled(false);
					final ProgressWindow progressWindow = new ProgressWindow(m_frame, "Posting Turn Summary...");
					progressWindow.setVisible(true);
					final Runnable t = new Runnable()
					{
						public void run()
						{
							boolean postOk = true;
							
							final IAbstractEndTurnDelegate delegate = (IAbstractEndTurnDelegate) m_bridge.getRemote();
							delegate.setHasPostedTurnSummary(true);
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
								if (!delegate.postTurnSummary(m_poster))
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
							delegate.setHasPostedTurnSummary(postOk);
							
							final boolean finalPostOk = postOk;
							final String finalMessage = sb.toString();
							delegate.setHasPostedTurnSummary(postOk);
							final Runnable runnable = new Runnable()
							{
								public void run()
								{
									m_postButton.setEnabled(!finalPostOk);
									if (finalPostOk)
										JOptionPane.showMessageDialog(m_frame, finalMessage, "Turn Summary Posted", JOptionPane.INFORMATION_MESSAGE);
									else
										JOptionPane.showMessageDialog(m_frame, finalMessage, "Turn Summary Posted", JOptionPane.ERROR_MESSAGE);
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
			public void actionPerformed(final ActionEvent event)
			{
				updateHistoryLog();
			}
		};
		m_includeProductionAction = new AbstractAction("Include production summary")
		{
			public void actionPerformed(final ActionEvent event)
			{
				updateHistoryLog();
			}
		};
		m_showDetailsAction = new AbstractAction("Show dice/battle details")
		{
			public void actionPerformed(final ActionEvent event)
			{
				updateHistoryLog();
			}
		};
		m_doneAction = new AbstractAction("Done")
		{
			public void actionPerformed(final ActionEvent event)
			{
				release();
			}
		};
		m_includeTerritoryCheckbox = new JCheckBox(m_includeTerritoryAction);
		m_includeProductionCheckbox = new JCheckBox(m_includeProductionAction);
		m_showDetailsCheckbox = new JCheckBox(m_showDetailsAction);
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
				m_actionLabel.setText(id.getName() + " Turn Summary");
				// defer componenet layout until waitForEndTurn()
			}
		});
	}
	
	@Override
	public String toString()
	{
		return "EndTurnPanel";
	}
	
	private void updateHistoryLog()
	{
		m_historyLog.clear();
		m_historyLog.printFullTurn(getData().getHistory().getLastNode(), m_showDetailsCheckbox.isSelected());
		if (m_includeTerritoryCheckbox.isSelected())
			m_historyLog.printTerritorySummary(getData());
		if (m_includeProductionCheckbox.isSelected())
			m_historyLog.printProductionSummary(getData());
		m_historyLog.requestFocus();
	}
	
	public void waitForEndTurn(final TripleAFrame frame, final IPlayerBridge bridge)
	{
		m_frame = frame;
		m_bridge = bridge;
		// Nothing to do if there are no PBEM messengers
		m_poster = new PBEMMessagePoster(getData(), getCurrentPlayer(), getRound());
		if (!m_poster.hasMessengers())
			return;
		
		final IAbstractEndTurnDelegate delegate = (IAbstractEndTurnDelegate) m_bridge.getRemote();
		final boolean hasPosted = delegate.getHasPostedTurnSummary();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				m_historyLog = new HistoryLog();
				updateHistoryLog();
				// only show widgets if there are PBEM messengers
				removeAll();
				add(m_actionLabel);
				add(m_includeTerritoryCheckbox);
				add(m_includeProductionCheckbox);
				add(m_showDetailsCheckbox);
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
