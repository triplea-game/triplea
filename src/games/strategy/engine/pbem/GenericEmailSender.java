package games.strategy.engine.pbem;

import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.EmailSenderEditor;
import games.strategy.engine.framework.startup.ui.editors.IBean;
import games.strategy.triplea.help.HelpSupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

/**
 * A PBEM (play by email) sender that will email turn summary and save game
 * This class is saved as a property as part of a save game
 * This class has two password fields, one is transitive and used while the game is running, the other is 'cleared' when
 * the game starts. This is done for security reasons so save games will not include passwords.
 * The non-transitive password is used when the object is stored in the local cache
 * 
 * @author Klaus Groenbaek
 */
public class GenericEmailSender implements IEmailSender
{
	// -----------------------------------------------------------------------
	// class fields
	// -----------------------------------------------------------------------
	private static final long serialVersionUID = 4644748856027574157L;
	/**
	 * a value to assign to the non-transitive password, as we can see that is was cleared
	 */
	private static final String USE_TRANSITIVE_PASSWORD = "d0a11f0f-96d3-4303-8875-4965aefb2ce4";
	
	
	// -----------------------------------------------------------------------
	// instance fields
	// -----------------------------------------------------------------------
	
	/**
	 * Currently only message encryption is allowed. Later connect based encryption through SSL may be implementes
	 */
	public enum Encryption
	{
		NONE, TLS
	}
	
	// -----------------------------------------------------------------------
	// constructors
	// -----------------------------------------------------------------------
	
	private long m_timeout = TimeUnit.SECONDS.toMillis(60);
	private String m_subjectPrefix;
	private String m_userName;
	private String m_password;
	private transient String m_transPassword;
	private String m_toAddress;
	private String m_host = "smptserver.example.com";
	private int m_port = 25;
	private Encryption m_encryption;
	private boolean m_alsoPostAfterCombatMove = false;
	
	// -----------------------------------------------------------------------
	// instance methods
	// -----------------------------------------------------------------------
	
	public void sendEmail(final String subject, final String htmlMessage, final File saveGame, final String saveGameName) throws IOException
	{
		// this is the last step and we create the email to send
		
		if (m_toAddress == null)
		{
			throw new IOException("Could not send email, no To address configured");
		}
		
		final Properties props = new Properties();
		if (getUserName() != null)
		{
			props.put("mail.smtp.auth", "true");
		}
		if (m_encryption == Encryption.TLS)
		{
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.starttls.required", "true");
		}
		
		props.put("mail.smtp.host", getHost());
		props.put("mail.smtp.port", getPort());
		
		props.put("mail.smtp.connectiontimeout", m_timeout);
		props.put("mail.smtp.timeout", m_timeout);
		
		final String to = m_toAddress;
		final String from = "noreply@triplea.sourceforge.net";
		
		// todo get the turn and player number from the game data
		
		try
		{
			final Session session = Session.getInstance(props, null);
			
			final MimeMessage mimeMessage = new MimeMessage(session);
			// Build the message fields one by one:
			// priority
			mimeMessage.setHeader("X-Priority", "3 (Normal)");
			
			// from
			mimeMessage.setFrom(new InternetAddress(from));
			
			// to address
			final StringTokenizer toAddresses = new StringTokenizer(to, " ", false);
			while (toAddresses.hasMoreTokens())
			{
				mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(toAddresses.nextToken().trim()));
			}
			
			// subject
			mimeMessage.setSubject(m_subjectPrefix + " " + subject);
			
			final MimeBodyPart bodypart = new MimeBodyPart();
			bodypart.setText(htmlMessage, "UTF-8");
			bodypart.setHeader("Content-Type", "text/html");
			
			if (saveGame != null)
			{
				final Multipart multipart = new MimeMultipart();
				
				multipart.addBodyPart(bodypart);
				// add save game
				final FileInputStream fin = new FileInputStream(saveGame);
				final DataSource source = new ByteArrayDataSource(fin, "application/triplea");
				final BodyPart messageBodyPart = new MimeBodyPart();
				messageBodyPart.setDataHandler(new DataHandler(source));
				messageBodyPart.setFileName(saveGameName);
				multipart.addBodyPart(messageBodyPart);
				
				mimeMessage.setContent(multipart);
			}
			
			// date
			try
			{
				mimeMessage.setSentDate(new Date());
			} catch (final Exception e)
			{
				// NoOp - the Date field is simply ignored in this case
			}
			
			final Transport transport = session.getTransport("smtp");
			if (getUserName() != null)
			{
				transport.connect(getHost(), getPort(), getUserName(), getPassword());
			}
			else
			{
				transport.connect();
			}
			
			mimeMessage.saveChanges();
			transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
			transport.close();
			
		} catch (final MessagingException e)
		{
			throw new IOException(e.getMessage());
		}
	}
	
	/**
	 * Get the user name used to login to the smtp server to send the email
	 * 
	 * @return the userName or null if no authentication is required
	 */
	public String getUserName()
	{
		return m_userName;
	}
	
	/**
	 * Set the userName used for authentication with the smtp server
	 * 
	 * @param userName
	 *            the userName or null if no authentication is required
	 */
	public void setUserName(final String userName)
	{
		m_userName = userName;
	}
	
	/**
	 * Get the password used to authenticate
	 * 
	 * @return the password or null
	 */
	public String getPassword()
	{
		if (USE_TRANSITIVE_PASSWORD.equals(m_password))
		{
			return m_transPassword;
		}
		return m_password;
	}
	
	/**
	 * Set the password to authenticate with
	 * 
	 * @param password
	 *            the password or null
	 */
	public void setPassword(final String password)
	{
		m_password = password;
		m_transPassword = password;
	}
	
	/**
	 * Get the timeout (in milli seconds) before the send operation should be aborted
	 * 
	 * @return the timeout
	 */
	public long getTimeout()
	{
		return m_timeout;
	}
	
	/**
	 * Set the send timeout, after the Email sender is connected to the SMTP server this is the maximum amount of time
	 * it will wait before aborting the send operation
	 * 
	 * @param timeout
	 *            the timeout in milli seconds. The default is 60 seconds (60000 milli seconds)
	 */
	public void setTimeout(final long timeout)
	{
		m_timeout = timeout;
	}
	
	/**
	 * Get the SMTP host
	 * 
	 * @return the host to send to
	 */
	public String getHost()
	{
		return m_host;
	}
	
	/**
	 * Set the smtp server host or IP address
	 * 
	 * @param host
	 *            the host
	 */
	public void setHost(final String host)
	{
		m_host = host;
	}
	
	/**
	 * Get the smtp server post
	 * 
	 * @return the port
	 */
	public int getPort()
	{
		return m_port;
	}
	
	/**
	 * Set the SMTP servers port
	 * 
	 * @param port
	 *            the port
	 */
	public void setPort(final int port)
	{
		m_port = port;
	}
	
	/**
	 * Get the message encryption
	 * 
	 * @return the selected encryption
	 * @see Encryption
	 */
	public Encryption getEncryption()
	{
		return m_encryption;
	}
	
	/**
	 * Sets the message encryption
	 * 
	 * @param encryption
	 *            the encryption
	 */
	public void setEncryption(final Encryption encryption)
	{
		m_encryption = encryption;
	}
	
	/**
	 * Sets the to address field, if multiple email addresses are given they must be separated by space
	 * 
	 * @param to
	 *            the to addresses
	 */
	public void setToAddress(final String to)
	{
		m_toAddress = to;
	}
	
	/**
	 * Get the To address configured
	 * 
	 * @return the to address, or multiple separated by space
	 */
	public String getToAddress()
	{
		return m_toAddress;
	}
	
	public void clearSensitiveInfo()
	{
		m_password = USE_TRANSITIVE_PASSWORD;
	}
	
	public IEmailSender doClone()
	{
		final GenericEmailSender sender = new GenericEmailSender();
		sender.setSubjectPrefix(getSubjectPrefix());
		sender.setEncryption(getEncryption());
		sender.setHost(getHost());
		sender.setPassword(getPassword());
		sender.setPort(getPort());
		sender.setTimeout(getTimeout());
		sender.setToAddress(getToAddress());
		sender.setUserName(getUserName());
		sender.setAlsoPostAfterCombatMove(getAlsoPostAfterCombatMove());
		return sender;
	}
	
	public boolean getAlsoPostAfterCombatMove()
	{
		return m_alsoPostAfterCombatMove;
	}
	
	public void setAlsoPostAfterCombatMove(final boolean postAlso)
	{
		m_alsoPostAfterCombatMove = postAlso;
	}
	
	public String getSubjectPrefix()
	{
		return m_subjectPrefix;
	}
	
	public void setSubjectPrefix(final String subjectPrefix)
	{
		m_subjectPrefix = subjectPrefix;
	}
	
	public String getDisplayName()
	{
		return "Generic SMTP";
	}
	
	public EditorPanel getEditor()
	{
		return new EmailSenderEditor(this, new EmailSenderEditor.EditorConfiguration(true, true, true));
	}
	
	public boolean sameType(final IBean other)
	{
		return other.getClass() == GenericEmailSender.class;
	}
	
	public String getHelpText()
	{
		return HelpSupport.loadHelp("genericEmailSender.html");
	}
	
	@Override
	public String toString()
	{
		return "GenericEmailSender{" +
					"m_toAddress='" + m_toAddress + '\'' +
					", m_userName='" + m_userName + '\'' +
					", m_host='" + m_host + '\'' +
					", m_port=" + m_port +
					", m_encryption=" + m_encryption +
					'}';
	}
}
