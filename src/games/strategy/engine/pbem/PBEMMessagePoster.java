package games.strategy.engine.pbem;

import games.strategy.common.ui.MainGameFrame;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.triplea.ui.history.HistoryLog;
import games.strategy.ui.ProgressWindow;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * This class is responsible for posting turn summary and email at the end of each round in a PBEM game.
 * A new instance is created at end of turn, based on the Email and a forum poster stored in the game data.
 * The needs to be serialized since it is invoked through the IAbstractEndTurnDelegate which require all objects to be serializable
 * although the PBEM games will always be local
 * 
 * @author unascribed
 * @author Klaus Groenbaek
 * 
 */
public class PBEMMessagePoster implements Serializable
{
	// -----------------------------------------------------------------------
	// class fields
	// -----------------------------------------------------------------------
	public static final String FORUM_POSTER_PROP_NAME = "games.strategy.engine.pbem.IForumPoster";
	public static final String EMAIL_SENDER_PROP_NAME = "games.strategy.engine.pbem.IEmailSender";
	public static final String WEB_POSTER_PROP_NAME = "games.strategy.engine.pbem.IWebPoster";
	private static final long serialVersionUID = 2256265436928530566L;
	
	// -----------------------------------------------------------------------
	// instance fields
	// -----------------------------------------------------------------------
	private final IForumPoster m_forumPoster;
	private final IEmailSender m_emailSender;
	private final IWebPoster m_webSitePoster;
	private transient File m_saveGameFile = null;
	private transient String m_turnSummary = null;
	private transient String m_saveGameRef = null;
	private transient String m_turnSummaryRef = null;
	private transient String m_emailSendStatus;
	private transient String m_webPostStatus;
	private transient PlayerID m_currentPlayer;
	private transient final GameData m_gameData;
	private transient int m_roundNumber;
	private transient String m_gameNameAndInfo;
	
	// -----------------------------------------------------------------------
	// Constructors
	// -----------------------------------------------------------------------
	
	public PBEMMessagePoster(final GameData gameData, final PlayerID currentPlayer, final int roundNumber, final String title)
	{
		m_gameData = gameData;
		m_currentPlayer = currentPlayer;
		m_roundNumber = roundNumber;
		m_forumPoster = (IForumPoster) gameData.getProperties().get(FORUM_POSTER_PROP_NAME);
		m_emailSender = (IEmailSender) gameData.getProperties().get(EMAIL_SENDER_PROP_NAME);
		m_webSitePoster = (IWebPoster) gameData.getProperties().get(WEB_POSTER_PROP_NAME);
		m_gameNameAndInfo = "TripleA " + title + " for game: " + gameData.getGameName() + ", version: " + gameData.getGameVersion();
	}
	
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	public boolean hasMessengers()
	{
		return (m_forumPoster != null || m_emailSender != null || m_webSitePoster != null);
	}
	
	public static boolean GameDataHasPlayByEmailOrForumMessengers(final GameData gameData)
	{
		final IForumPoster forumPoster = (IForumPoster) gameData.getProperties().get(FORUM_POSTER_PROP_NAME);
		final IEmailSender emailSender = (IEmailSender) gameData.getProperties().get(EMAIL_SENDER_PROP_NAME);
		final IWebPoster webPoster = (IWebPoster) gameData.getProperties().get(WEB_POSTER_PROP_NAME);
		return (forumPoster != null || emailSender != null || webPoster != null);
	}
	
	/* public void setForumPoster(final IForumPoster msgr)
	{
		m_forumPoster = msgr;
	}*/

	public IForumPoster getForumPoster()
	{
		return m_forumPoster;
	}
	
	public IWebPoster getWebPoster()
	{
		return m_webSitePoster;
	}
	
	public void setTurnSummary(final String turnSummary)
	{
		m_turnSummary = turnSummary;
	}
	
	public void setSaveGame(final File saveGameFile) throws FileNotFoundException
	{
		m_saveGameFile = saveGameFile;
	}
	
	public String getTurnSummaryRef()
	{
		return m_turnSummaryRef;
	}
	
	public String getSaveGameRef()
	{
		return m_saveGameRef;
	}
	
	/**
	 * Post summary to form and/or email, and writes the action performed to the history writer
	 * 
	 * @param historyWriter
	 *            the history writer (which has no effect since save game has already be generated...) // todo (kg)
	 * @return true if all posts were successful
	 */
	public boolean post(final IDelegateHistoryWriter historyWriter, final String title, final boolean includeSaveGame)
	{
		boolean forumSuccess = true;
		
		final StringBuilder saveGameSb = new StringBuilder().append("triplea_");
		if (m_forumPoster != null)
		{
			saveGameSb.append(m_forumPoster.getTopicId()).append("_");
		}
		saveGameSb.append(m_currentPlayer.getName().substring(0, Math.min(3, m_currentPlayer.getName().length() - 1))).append(m_roundNumber).append(".tsvg");
		
		final String saveGameName = saveGameSb.toString();
		if (m_forumPoster != null)
		{
			if (includeSaveGame)
			{
				m_forumPoster.addSaveGame(m_saveGameFile, saveGameName);
			}
			try
			{
				forumSuccess = m_forumPoster.postTurnSummary((m_gameNameAndInfo + "\n\n" + m_turnSummary), "TripleA " + title + ": " + m_currentPlayer.getName() + " round " + m_roundNumber);
				m_turnSummaryRef = m_forumPoster.getTurnSummaryRef();
				if (m_turnSummaryRef != null && historyWriter != null)
				{
					historyWriter.startEvent("Turn Summary: " + m_turnSummaryRef);
				}
			} catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
		
		boolean emailSuccess = true;
		if (m_emailSender != null)
		{
			final StringBuilder subjectPostFix = new StringBuilder(m_currentPlayer.getName());
			subjectPostFix.append(" - ").append("round ").append(m_roundNumber);
			try
			{
				m_emailSender.sendEmail(subjectPostFix.toString(), convertToHtml((m_gameNameAndInfo + "\n\n" + m_turnSummary)), m_saveGameFile, saveGameName);
				m_emailSendStatus = "Success, sent to " + m_emailSender.getToAddress();
				
			} catch (final IOException e)
			{
				emailSuccess = false;
				m_emailSendStatus = "Failed! Error " + e.getMessage();
				e.printStackTrace();
			}
		}
		
		boolean webSiteSuccess = true;
		if (m_webSitePoster != null)
		{
			m_webSitePoster.addSaveGame(m_saveGameFile, saveGameName);
			try
			{
				webSiteSuccess = m_webSitePoster.postTurnSummary(m_gameData, m_turnSummary, m_currentPlayer.getName(), m_roundNumber);
				if (webSiteSuccess)
					m_webPostStatus = "Success! Sent State of Game " + m_webSitePoster.getGameName() + " to " + m_webSitePoster.getHost() + "\n" + m_webSitePoster.getServerMessage();
				else
					m_webPostStatus = "Failed! " + m_webSitePoster.getServerMessage();
			} catch (final Exception e)
			{
				webSiteSuccess = false;
				m_webPostStatus = "Failed! Error: " + e.getMessage();
				e.printStackTrace();
			}
		}
		
		if (historyWriter != null)
		{
			final StringBuilder sb = new StringBuilder("Post Turn Summary");
			if (m_forumPoster != null)
			{
				sb.append(" to ").append(m_forumPoster.getDisplayName()).append(" success = ").append(String.valueOf(forumSuccess));
			}
			if (m_emailSender != null)
			{
				if (m_forumPoster != null)
				{
					sb.append(" and to ");
				}
				else
				{
					sb.append(" to ");
				}
				sb.append(m_emailSender.getToAddress()).append(" success = ").append(String.valueOf(emailSuccess));
			}
			historyWriter.startEvent(sb.toString());
		}
		
		return forumSuccess && emailSuccess && webSiteSuccess;
	}
	
	/**
	 * Converts text to html, by transforming \n to <br/>
	 * 
	 * @param string
	 *            the string to transform
	 * @return the transformed string
	 */
	private String convertToHtml(final String string)
	{
		return "<pre><br/>" + string.replaceAll("\n", "<br/>") + "<br/></pre>";
	}
	
	/**
	 * Get the configured email sender
	 * 
	 * @return return an email sender or null
	 */
	public IEmailSender getEmailSender()
	{
		return m_emailSender;
	}
	
	/**
	 * Return the status string from sending the email.
	 * 
	 * @return a success of failure string, or null if no email sender was configured
	 */
	public String getEmailSendStatus()
	{
		return m_emailSendStatus;
	}
	
	public String getWebPostStatus()
	{
		return m_webPostStatus;
	}
	
	public boolean alsoPostMoveSummary()
	{
		if (m_forumPoster != null)
			return m_forumPoster.getAlsoPostAfterCombatMove();
		if (m_emailSender != null)
			return m_emailSender.getAlsoPostAfterCombatMove();
		return false;
	}
	
	public static void postTurn(final String title, final HistoryLog historyLog, final boolean includeSaveGame, final PBEMMessagePoster posterPBEM, final IAbstractForumPosterDelegate postingDelegate,
				final MainGameFrame mainGameFrame, final JComponent postButton)
	{
		String message = "";
		final IForumPoster turnSummaryMsgr = posterPBEM.getForumPoster();
		
		final StringBuilder sb = new StringBuilder();
		if (turnSummaryMsgr != null)
		{
			sb.append(message).append("Post " + title + " ");
			if (includeSaveGame)
			{
				sb.append("and save game ");
			}
			sb.append("to ").append(turnSummaryMsgr.getDisplayName()).append("?\n");
		}
		
		final IEmailSender emailSender = posterPBEM.getEmailSender();
		if (emailSender != null)
		{
			sb.append("Send email to ").append(emailSender.getToAddress()).append("?\n");
		}
		
		final IWebPoster webPoster = posterPBEM.getWebPoster();
		if (webPoster != null)
		{
			sb.append("Send game state of '" + webPoster.getGameName() + "' to " + webPoster.getHost() + "?\n");
		}
		
		message = sb.toString();
		final int choice = JOptionPane.showConfirmDialog(mainGameFrame, message, "Post " + title + "?", 2, -1, null);
		if (choice != 0)
		{
			return;
		}
		else
		{
			postButton.setEnabled(false);
			final ProgressWindow progressWindow = new ProgressWindow(mainGameFrame, "Posting " + title + "...");
			progressWindow.setVisible(true);
			final Runnable t = new Runnable()
			{
				public void run()
				{
					boolean postOk = true;
					
					File saveGameFile = null;
					postingDelegate.setHasPostedTurnSummary(true);
					
					try
					{
						saveGameFile = File.createTempFile("triplea", ".tsvg");
						if (saveGameFile != null)
						{
							mainGameFrame.getGame().saveGame(saveGameFile);
							posterPBEM.setSaveGame(saveGameFile);
						}
					} catch (final Exception e)
					{
						postOk = false;
						e.printStackTrace();
					}
					
					posterPBEM.setTurnSummary(historyLog.toString());
					
					try
					{
						// forward the poster to the delegate which invokes post() on the poster
						if (!postingDelegate.postTurnSummary(posterPBEM, title, includeSaveGame))
							postOk = false;
					} catch (final Exception e)
					{
						postOk = false;
						e.printStackTrace();
					}
					postingDelegate.setHasPostedTurnSummary(postOk);
					
					final StringBuilder sb = new StringBuilder();
					if (posterPBEM.getForumPoster() != null)
					{
						final String saveGameRef = posterPBEM.getSaveGameRef();
						final String turnSummaryRef = posterPBEM.getTurnSummaryRef();
						
						if (saveGameRef != null)
							sb.append("\nSave Game : ").append(saveGameRef);
						if (turnSummaryRef != null)
							sb.append("\nSummary Text: ").append(turnSummaryRef);
					}
					if (posterPBEM.getEmailSender() != null)
					{
						sb.append("\nEmails: ").append(posterPBEM.getEmailSendStatus());
					}
					if (posterPBEM.getWebPoster() != null)
					{
						sb.append("\nWeb Site Post: ").append(posterPBEM.getWebPostStatus());
					}
					
					historyLog.getWriter().println(sb.toString());
					
					if (historyLog.isVisible()) // todo(kg) I think this is a brain fart, unless is is a workaround for some bug
						historyLog.setVisible(true);
					try
					{
						if (saveGameFile != null && !saveGameFile.delete())
							System.out.println((new StringBuilder()).append("INFO TripleA PBEM/PBF poster couldn't delete temporary savegame: ").append(saveGameFile.getCanonicalPath()).toString());
					} catch (final IOException ioe)
					{
						ioe.printStackTrace();
					}
					progressWindow.setVisible(false);
					progressWindow.removeAll();
					progressWindow.dispose();
					
					final boolean finalPostOk = postOk;
					final String finalMessage = sb.toString();
					final Runnable runnable = new Runnable()
					{
						public void run()
						{
							postButton.setEnabled(!finalPostOk);
							if (finalPostOk)
								JOptionPane.showMessageDialog(mainGameFrame, finalMessage, title + " Posted", JOptionPane.INFORMATION_MESSAGE);
							else
								JOptionPane.showMessageDialog(mainGameFrame, finalMessage, title + " Posted", JOptionPane.ERROR_MESSAGE);
						}
					};
					SwingUtilities.invokeLater(runnable);
				}
			};
			// start a new thread for posting the summary.
			new Thread(t).start();
		}
	}
}
