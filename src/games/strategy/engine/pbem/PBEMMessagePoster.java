package games.strategy.engine.pbem;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.history.IDelegateHistoryWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;

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
	private static final long serialVersionUID = 2256265436928530566L;
	
	// -----------------------------------------------------------------------
	// instance fields
	// -----------------------------------------------------------------------
	private IForumPoster m_forumPoster = null;
	private final IEmailSender m_emailSender;
	private transient File m_saveGameFile = null;
	private transient String m_turnSummary = null;
	private transient String m_saveGameRef = null;
	private transient String m_turnSummaryRef = null;
	private transient String m_emailSendStatus;
	private transient PlayerID m_currentPlayer;
	private transient int m_roundNumber;
	
	// -----------------------------------------------------------------------
	// Constructors
	// -----------------------------------------------------------------------
	
	public PBEMMessagePoster(final GameData gameData, final PlayerID currentPlayer, final int roundNumber)
	{
		m_currentPlayer = currentPlayer;
		m_roundNumber = roundNumber;
		m_forumPoster = (IForumPoster) gameData.getProperties().get(FORUM_POSTER_PROP_NAME);
		m_emailSender = (IEmailSender) gameData.getProperties().get(EMAIL_SENDER_PROP_NAME);
	}
	
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	public boolean hasMessengers()
	{
		return (m_forumPoster != null || m_emailSender != null);
	}
	
	public void setForumPoster(final IForumPoster msgr)
	{
		m_forumPoster = msgr;
	}
	
	public IForumPoster getForumPoster()
	{
		return m_forumPoster;
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
	public boolean post(final IDelegateHistoryWriter historyWriter)
	{
		boolean forumSuccess = true;
		
		final StringBuilder saveGameSb = new StringBuilder().append("triplea_");
		if (m_forumPoster != null)
		{
			saveGameSb.append(m_forumPoster.getTopicId()).append("_");
		}
		saveGameSb.append(m_currentPlayer.getName().substring(0, 1)).append(m_roundNumber).append(".tsvg");
		
		final String saveGameName = saveGameSb.toString();
		if (m_forumPoster != null)
		{
			if (m_forumPoster.getIncludeSaveGame())
			{
				m_forumPoster.addSaveGame(m_saveGameFile, saveGameName);
			}
			try
			{
				forumSuccess = m_forumPoster.postTurnSummary(m_turnSummary, "TripleA Turn Summary: " + m_currentPlayer.getName() + " round " + m_roundNumber);
				m_turnSummaryRef = m_forumPoster.getTurnSummaryRef();
				if (m_turnSummaryRef != null && historyWriter != null)
				{
					historyWriter.addChildToEvent("Turn Summary: " + m_turnSummaryRef, null);
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
				m_emailSender.sendEmail(subjectPostFix.toString(), convertToHtml(m_turnSummary), m_saveGameFile, saveGameName);
				m_emailSendStatus = "Success, sent to " + m_emailSender.getToAddress();
				
			} catch (final IOException e)
			{
				emailSuccess = false;
				m_emailSendStatus = "Failed! Error " + e.getMessage();
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
		
		return forumSuccess && emailSuccess;
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
		return string.replaceAll("\n", "<br/>");
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
}
