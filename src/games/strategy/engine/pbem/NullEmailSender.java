package games.strategy.engine.pbem;

import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.IBean;

import java.io.File;
import java.io.IOException;

/**
 * A dummy Email sender, to use when Email sending is disabled
 * 
 * @author Klaus Groenbaek
 */
public class NullEmailSender implements IEmailSender
{
	// -----------------------------------------------------------------------
	// class fields
	// -----------------------------------------------------------------------
	private static final long serialVersionUID = 9138507282128548506L;
	
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	
	public String getDisplayName()
	{
		return "disabled";
	}
	
	public void sendEmail(final String subject, final String htmlMessage, final File saveGame, final String fileName) throws IOException
	{
	}
	
	public String getToAddress()
	{
		return null;
	}
	
	public void clearSensitiveInfo()
	{
		
	}
	
	public IEmailSender doClone()
	{
		return new NullEmailSender();
	}
	
	public EditorPanel getEditor()
	{
		return null;
	}
	
	public boolean sameType(final IBean other)
	{
		return other.getClass() == NullEmailSender.class;
	}
	
	public String getUserName()
	{
		return null;
	}
	
	public String getPassword()
	{
		return null;
	}
	
	public void setUserName(final String userName)
	{
		
	}
	
	public void setPassword(final String password)
	{
		
	}
	
	public String getHelpText()
	{
		return "will never be called";
	}
	
	public boolean getAlsoPostAfterCombatMove()
	{
		return false;
	}
	
	public void setAlsoPostAfterCombatMove(final boolean postAlso)
	{
	}
}
