package games.strategy.engine.pbem;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.IBean;

import java.io.File;
import java.util.Vector;

public class NullWebPoster implements IWebPoster
{
	private static final long serialVersionUID = 1871745918801353205L;
	
	public boolean postTurnSummary(final GameData gameData, final String turnSummary, final String player, final int round)
	{
		return true;
	}
	
	public boolean getMailSaveGame()
	{
		return false;
	}
	
	public void setMailSaveGame(final boolean mail)
	{
		
	}
	
	public void addSaveGame(final File saveGame, final String fileName)
	{
		
	}
	
	public String getDisplayName()
	{
		return "disabled";
	}
	
	public EditorPanel getEditor()
	{
		return null;
	}
	
	public boolean sameType(final IBean other)
	{
		return other.getClass() == NullWebPoster.class;
	}
	
	public String getHelpText()
	{
		return "Will never be called";
	}
	
	public String getSiteId()
	{
		return null;
	}
	
	public String getHost()
	{
		return null;
	}
	
	public Vector<String> getAllHosts()
	{
		return new Vector<String>();
	}
	
	public String getGameName()
	{
		return null;
	}
	
	public void setSiteId(final String siteId)
	{
	}
	
	public void setGameName(final String gameName)
	{
	}
	
	public void setHost(final String host)
	{
	}
	
	public void setAllHosts(final Vector<String> hosts)
	{
	}
	
	public void addToAllHosts(final String host)
	{
	}
	
	public void viewSite()
	{
	}
	
	public String getTestMessage()
	{
		return "You should not be able to test a Null Poster";
	}
	
	public String getServerMessage()
	{
		return "Success";
	}
	
	public IWebPoster doClone()
	{
		return null;
	}
	
	public void clearSensitiveInfo()
	{
	}
}
