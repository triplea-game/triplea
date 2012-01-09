package games.strategy.engine.pbem;

import java.io.*;

/**
 * Abstract Forum poster that takes care of storing the username, password, and other common properties
 * 
 * @author Klaus Groenbaek
 */
public abstract class AbstractForumPoster implements IForumPoster
{
	// -----------------------------------------------------------------------
	// constants
	// -----------------------------------------------------------------------
	private static final String USE_TRANSITIVE_PASSWORD = "d0a11f0f-96d3-4303-8875-4965aefb2ce4";
	private static final long serialVersionUID = -734015230309508040L;
	
	// -----------------------------------------------------------------------
	// class methods
	// -----------------------------------------------------------------------
	
	// -----------------------------------------------------------------------
	// instance fields
	// -----------------------------------------------------------------------
	protected String m_username = null;
	protected String m_password = null;
	protected transient String m_transPassword;
	protected String m_forumId = null;
	protected boolean m_includeSaveGame = true;
	
	// -----------------------------------------------------------------------
	// transitive fields
	// -----------------------------------------------------------------------
	protected transient File m_saveGameFile = null;
	protected transient String m_turnSummaryRef = null;
	protected transient String m_saveGameFileName = null;
	
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	
	public String getTurnSummaryRef()
	{
		return m_turnSummaryRef;
	}
	
	public boolean getIncludeSaveGame()
	{
		return m_includeSaveGame;
	}
	
	public void setIncludeSaveGame(final boolean include)
	{
		m_includeSaveGame = include;
	}
	
	public void addSaveGame(final File saveGame, final String fileName)
	{
		m_saveGameFile = saveGame;
		m_saveGameFileName = fileName;
	}
	
	public boolean getCanViewPosted()
	{
		return true;
	}
	
	public void setForumId(final String forumId)
	{
		m_forumId = forumId;
	}
	
	public String getForumId()
	{
		return m_forumId;
	}
	
	public void setUsername(final String username)
	{
		m_username = username;
	}
	
	public String getUsername()
	{
		return m_username;
	}
	
	public void setPassword(final String password)
	{
		m_password = password;
		m_transPassword = password;
	}
	
	public String getPassword()
	{
		if (USE_TRANSITIVE_PASSWORD.equals(m_password))
		{
			return m_transPassword;
		}
		return m_password;
	}
	
	public void clearSensitiveInfo()
	{
		m_password = USE_TRANSITIVE_PASSWORD;
	}
}
