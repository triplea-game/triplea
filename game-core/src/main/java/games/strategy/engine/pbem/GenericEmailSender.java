package games.strategy.engine.pbem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Instant;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.annotation.Nullable;
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

import com.google.common.base.Splitter;

import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.EmailSenderEditor;
import games.strategy.engine.framework.startup.ui.editors.IBean;
import games.strategy.security.CredentialManager;
import games.strategy.security.CredentialManagerException;
import games.strategy.triplea.help.HelpSupport;
import lombok.extern.java.Log;

/**
 * A PBEM (play by email) sender that will email turn summary and save game.
 *
 * <p>
 * Instances of this class are saved as a property as part of a save game.
 * </p>
 *
 * <p>
 * This class has two fields per credential. One is transient and used while the game is running. The other is
 * persistent and "cleared" when the game starts. This is done for security reasons so save games will not include
 * credentials. The persistent password is used when the object is stored in the local cache.
 * </p>
 */
@Log
public class GenericEmailSender implements IEmailSender {
  private static final long serialVersionUID = 4644748856027574157L;

  /**
   * The value assigned to a persistent credential that indicates it was cleared and the associated transient credential
   * should be used instead.
   */
  private static final String USE_TRANSIENT_CREDENTIAL = "d0a11f0f-96d3-4303-8875-4965aefb2ce4";

  /**
   * Currently only message encryption is allowed. Later connect based encryption through SSL may be implemented.
   */
  public enum Encryption {
    NONE, TLS
  }

  private long timeout = TimeUnit.SECONDS.toMillis(60);
  private String subjectPrefix;
  private String username;
  private transient String transientUsername;
  private String password;
  private transient String transientPassword;
  private String toAddress;
  private String host = "smptserver.example.com";
  private int port = 25;
  private Encryption encryption;
  private boolean alsoPostAfterCombatMove = false;
  private boolean credentialsSaved = false;
  private boolean credentialsProtected = false;

  private void writeObject(final ObjectOutputStream out) throws IOException {
    final String username = this.username;
    final String password = this.password;
    try {
      protectCredentials();
      out.defaultWriteObject();
    } finally {
      this.username = username;
      this.password = password;
    }
  }

  private void protectCredentials() {
    if (credentialsSaved) {
      credentialsProtected = true;
      try (CredentialManager credentialManager = CredentialManager.newInstance()) {
        username = credentialManager.protect(username);
        password = credentialManager.protect(password);
      } catch (final CredentialManagerException e) {
        log.log(Level.SEVERE, "failed to protect PBEM credentials", e);
        username = "";
        password = "";
      }
    } else {
      credentialsProtected = false;
    }
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    unprotectCredentials();
  }

  private void unprotectCredentials() {
    if (credentialsProtected) {
      try (CredentialManager credentialManager = CredentialManager.newInstance()) {
        username = credentialManager.unprotectToString(username);
        password = credentialManager.unprotectToString(password);
      } catch (final CredentialManagerException e) {
        log.log(Level.SEVERE, "failed to unprotect PBEM credentials", e);
        username = "";
        password = "";
      }
    }
  }

  @Override
  public void sendEmail(final String subject, final String htmlMessage, final File saveGame, final String saveGameName)
      throws IOException {
    // this is the last step and we create the email to send
    if (toAddress == null) {
      throw new IOException("Could not send email, no To address configured");
    }
    final Properties props = new Properties();
    if (getUserName() != null) {
      props.put("mail.smtp.auth", "true");
    }
    if (encryption == Encryption.TLS) {
      props.put("mail.smtp.starttls.enable", "true");
      props.put("mail.smtp.starttls.required", "true");
    }
    props.put("mail.smtp.host", getHost());
    props.put("mail.smtp.port", getPort());
    props.put("mail.smtp.connectiontimeout", timeout);
    props.put("mail.smtp.timeout", timeout);
    final String to = toAddress;
    final String from = getUserName();
    // todo get the turn and player number from the game data
    try {
      final Session session = Session.getInstance(props, null);
      final MimeMessage mimeMessage = new MimeMessage(session);
      // Build the message fields one by one:
      // priority
      mimeMessage.setHeader("X-Priority", "3 (Normal)");
      // from
      mimeMessage.setFrom(new InternetAddress(from));
      // to address
      for (final String token : Splitter.on(' ').omitEmptyStrings().trimResults().split(to)) {
        mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(token));
      }
      // subject
      mimeMessage.setSubject(subjectPrefix + " " + subject);
      final MimeBodyPart bodypart = new MimeBodyPart();
      bodypart.setText(htmlMessage, "UTF-8");
      bodypart.setHeader("Content-Type", "text/html");
      if (saveGame != null) {
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
      try {
        mimeMessage.setSentDate(Date.from(Instant.now()));
      } catch (final Exception e) {
        // NoOp - the Date field is simply ignored in this case
      }

      try (Transport transport = session.getTransport("smtp")) {
        if (getUserName() != null) {
          transport.connect(getHost(), getPort(), getUserName(), getPassword());
        } else {
          transport.connect();
        }
        mimeMessage.saveChanges();
        transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
      }
    } catch (final MessagingException e) {
      throw new IOException(e);
    }
  }

  /**
   * Returns {@code true} if the email provider requires authentication; otherwise returns {@code false}.
   *
   * <p>
   * Subclasses may override and are not required to call the superclass implementation. This implementation always
   * returns {@code false}.
   * </p>
   */
  public boolean isAuthenticationRequired() {
    return false;
  }

  @Override
  public String getUserName() {
    return USE_TRANSIENT_CREDENTIAL.equals(username) ? transientUsername : username;
  }

  @Override
  public void setUserName(final String username) {
    this.username = credentialsSaved ? username : USE_TRANSIENT_CREDENTIAL;
    transientUsername = username;
  }

  @Override
  public String getPassword() {
    return USE_TRANSIENT_CREDENTIAL.equals(password) ? transientPassword : password;
  }

  @Override
  public void setPassword(final String password) {
    this.password = credentialsSaved ? password : USE_TRANSIENT_CREDENTIAL;
    transientPassword = password;
  }

  @Override
  public boolean areCredentialsSaved() {
    return credentialsSaved;
  }

  @Override
  public void setCredentialsSaved(final boolean credentialsSaved) {
    this.credentialsSaved = credentialsSaved;
    setUserName(transientUsername);
    setPassword(transientPassword);
  }

  /**
   * Get the timeout (in milliseconds) before the send operation should be aborted.
   *
   * @return the timeout
   */
  public long getTimeout() {
    return timeout;
  }

  /**
   * Set the send timeout, after the Email sender is connected to the SMTP server this is the maximum amount of time
   * it will wait before aborting the send operation.
   *
   * @param timeout the timeout in milli seconds. The default is 60 seconds (60000 milli seconds)
   */
  public void setTimeout(final long timeout) {
    this.timeout = timeout;
  }

  /**
   * Get the SMTP host.
   *
   * @return the host to send to
   */
  public String getHost() {
    return host;
  }

  /**
   * Set the smtp server host or IP address.
   *
   * @param host the host
   */
  public void setHost(final String host) {
    this.host = host;
  }

  /**
   * Get the smtp server post.
   *
   * @return the port
   */
  public int getPort() {
    return port;
  }

  /**
   * Set the SMTP server port.
   *
   * @param port the port
   */
  public void setPort(final int port) {
    this.port = port;
  }

  /**
   * Get the message encryption.
   *
   * @return the selected encryption
   */
  public Encryption getEncryption() {
    return encryption;
  }

  /**
   * Sets the message encryption.
   *
   * @param encryption the encryption
   */
  public void setEncryption(final Encryption encryption) {
    this.encryption = encryption;
  }

  /**
   * Sets the to address field, if multiple email addresses are given they must be separated by space.
   *
   * @param to the to addresses
   */
  public void setToAddress(final String to) {
    toAddress = to;
  }

  @Override
  public String getToAddress() {
    return toAddress;
  }

  @Override
  public void clearSensitiveInfo() {
    credentialsSaved = false;
    username = password = USE_TRANSIENT_CREDENTIAL;
  }

  @Override
  public IEmailSender clone() {
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
    sender.setCredentialsSaved(areCredentialsSaved());
    return sender;
  }

  @Override
  public boolean getAlsoPostAfterCombatMove() {
    return alsoPostAfterCombatMove;
  }

  @Override
  public void setAlsoPostAfterCombatMove(final boolean postAlso) {
    alsoPostAfterCombatMove = postAlso;
  }

  public String getSubjectPrefix() {
    return subjectPrefix;
  }

  public void setSubjectPrefix(final String subjectPrefix) {
    this.subjectPrefix = subjectPrefix;
  }

  @Override
  public String getDisplayName() {
    return "Generic SMTP";
  }

  @Override
  public EditorPanel getEditor() {
    return new EmailSenderEditor(this, new EmailSenderEditor.EditorConfiguration(true, true, true));
  }

  @Override
  public String getHelpText() {
    return HelpSupport.loadHelp("genericEmailSender.html");
  }

  @Override
  public String toString() {
    return "GenericEmailSender{" + "toAddress='" + toAddress + '\'' + ", username='" + username + '\''
        + ", host='" + host + '\'' + ", port=" + port + ", encryption=" + encryption + '}';
  }

  @Override
  public final boolean isSameType(final @Nullable IBean other) {
    return other != null && getClass().equals(other.getClass());
  }
}
