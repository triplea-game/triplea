package games.strategy.engine.pbem;

import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.EmailSenderEditor;
import games.strategy.engine.framework.startup.ui.editors.IBean;

/**
 * A pre configured Email sender that uses Hotmail's SMTP server
 *
 * @author Klaus Groenbaek
 */
public class HotmailEmailSender extends GenericEmailSender
{
	private static final long serialVersionUID = 3511375113962472063L;

	//-----------------------------------------------------------------------
	// constructors
	//-----------------------------------------------------------------------

	public HotmailEmailSender()
	{
		setHost("smtp.live.com");
		setPort(587);
		setEncryption(Encryption.TLS);
	}

	//-----------------------------------------------------------------------
	// instance methods
	//-----------------------------------------------------------------------

	@Override
	public EditorPanel getEditor()
	{
		return new EmailSenderEditor(this, new EmailSenderEditor.EditorConfiguration());
	}

	@Override
	public String getDisplayName()
	{
		return "Hotmail ";
	}

	@Override
	public boolean sameType(IBean other)
	{
		return other.getClass() == HotmailEmailSender.class;
	}

	@Override
	public IEmailSender doClone()
	{
		GenericEmailSender sender = new HotmailEmailSender();
		sender.setSubjectPrefix(getSubjectPrefix());
		sender.setPassword(getPassword());
		sender.setToAddress(getToAddress());
		sender.setUserName(getUserName());
		return sender;
	}
}
