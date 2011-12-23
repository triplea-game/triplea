package games.strategy.engine.pbem;

import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.IBean;

import java.io.File;
import java.io.IOException;

/**
 * A dummy Email sender, to use when Email sending is disabled
 * @author Klaus Groenbaek
 */
public class NullEmailSender implements IEmailSender
{
	//-----------------------------------------------------------------------
	// class fields
	//-----------------------------------------------------------------------
	private static final long serialVersionUID = 9138507282128548506L;

	//-----------------------------------------------------------------------
	// instance methods
	//-----------------------------------------------------------------------

	public String getDisplayName()
	{
		return "disabled";
	}

	public void sendEmail(String subject, String htmlMessage, File saveGame, String fileName) throws IOException
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

	public boolean sameType(IBean other)
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

	public void setUserName(String userName)
	{

	}

	public void setPassword(String password)
	{

	}
}
