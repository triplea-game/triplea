package games.strategy.engine.pbem;

import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.EmailSenderEditor;
import games.strategy.engine.framework.startup.ui.editors.IBean;
import games.strategy.triplea.help.HelpSupport;

/**
 * @author Klaus Groenbaek
 */
public class GmailEmailSender extends GenericEmailSender
{
	private static final long serialVersionUID = 3511375113962472063L;
	
	// -----------------------------------------------------------------------
	// constructors
	// -----------------------------------------------------------------------
	
	public GmailEmailSender()
	{
		setHost("smtp.gmail.com");
		setPort(587);
		setEncryption(Encryption.TLS);
	}
	
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	
	@Override
	public EditorPanel getEditor()
	{
		return new EmailSenderEditor(this, new EmailSenderEditor.EditorConfiguration());
	}
	
	@Override
	public String getDisplayName()
	{
		return "Gmail";
	}
	
	@Override
	public boolean sameType(final IBean other)
	{
		return other.getClass() == GmailEmailSender.class;
	}
	
	@Override
	public IEmailSender doClone()
	{
		final GenericEmailSender sender = new GmailEmailSender();
		sender.setSubjectPrefix(getSubjectPrefix());
		sender.setPassword(getPassword());
		sender.setToAddress(getToAddress());
		sender.setUserName(getUserName());
		return sender;
	}
	
	@Override
	public String getHelpText()
	{
		return HelpSupport.loadHelp("gmailEmailSender.html");
	}
}
