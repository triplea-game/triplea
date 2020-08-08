package games.strategy.engine.posted.game.pbem;

import com.google.common.base.Splitter;
import games.strategy.triplea.settings.ClientSetting;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Properties;
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
 * A PBEM (play by email) sender that will email turn summary and save game.
 *
 * <p>Instances of this class are saved as a property as part of a save game.
 *
 * <p>This class has two fields per credential. One is transient and used while the game is running.
 * The other is persistent and "cleared" when the game starts. This is done for security reasons so
 * save games will not include credentials. The persistent password is used when the object is
 * stored in the local cache.
 */
public class DefaultEmailSender implements IEmailSender {
  private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(60);
  private final String subjectPrefix;
  private final String username;
  private final String password;
  private final String toAddress;
  private final String emailServerHost;
  private final int emailServerPort;
  private final boolean encrypted;

  DefaultEmailSender(
      final String username,
      final String password,
      final String subjectPrefix,
      final String toAddress) {
    this.username = username;
    this.password = password;
    this.subjectPrefix = subjectPrefix;
    this.toAddress = toAddress;

    emailServerHost = ClientSetting.emailServerHost.getValueOrThrow();
    emailServerPort = ClientSetting.emailServerPort.getValueOrThrow();
    encrypted = ClientSetting.emailServerSecurity.getValue().orElse(true);
  }

  @Override
  public void sendEmail(
      final String subject,
      final String htmlMessage,
      final File saveGame,
      final String saveGameName)
      throws IOException {
    // this is the last step and we create the email to send
    final Properties props = new Properties();
    props.put("mail.smtp.auth", "true");
    if (encrypted) {
      props.put("mail.smtp.starttls.enable", "true");
      props.put("mail.smtp.starttls.required", "true");
    }
    props.put("mail.smtp.host", emailServerHost);
    props.put("mail.smtp.port", emailServerPort);
    props.put("mail.smtp.connectiontimeout", TIMEOUT);
    props.put("mail.smtp.timeout", TIMEOUT);
    // todo get the turn and player number from the game data
    try {
      final Session session = Session.getInstance(props, null);
      final MimeMessage mimeMessage = new MimeMessage(session);
      // Build the message fields one by one:
      // priority
      mimeMessage.setHeader("X-Priority", "3 (Normal)");
      // from
      mimeMessage.setFrom(new InternetAddress(username));
      // to address
      for (final String token :
          Splitter.on(' ').omitEmptyStrings().trimResults().split(toAddress)) {
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
        try (FileInputStream fin = new FileInputStream(saveGame)) {
          final DataSource source = new ByteArrayDataSource(fin, "application/triplea");
          final BodyPart messageBodyPart = new MimeBodyPart();
          messageBodyPart.setDataHandler(new DataHandler(source));
          messageBodyPart.setFileName(saveGameName);
          multipart.addBodyPart(messageBodyPart);
        }
        mimeMessage.setContent(multipart);
      }
      // date
      try {
        mimeMessage.setSentDate(Date.from(Instant.now()));
      } catch (final Exception e) {
        // NoOp - the Date field is simply ignored in this case
      }

      try (Transport transport = session.getTransport("smtp")) {
        transport.connect(emailServerHost, emailServerPort, username, password);
        mimeMessage.saveChanges();
        transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
      }
    } catch (final MessagingException e) {
      throw new IOException(e);
    }
  }
}
