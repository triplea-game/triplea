package org.triplea.modules.forgot.password;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.function.BiConsumer;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.triplea.db.dao.TempPasswordDao;
import org.triplea.http.AppConfig;

/** Sends a temporary password to a target user. */
@AllArgsConstructor
@Log
class PasswordEmailSender implements BiConsumer<String, String> {
  private static final String FROM = "no-reply@triplea-game.org";

  private final AppConfig appConfig;

  @Override
  public void accept(final String email, final String generatedPassword) {
    if (!appConfig.isProd()) {
      // do not send emails if not on prod
      log.info(
          String.format(
              "Non-prod forgot password, email: %s, generated temp passsword: %s",
              email, generatedPassword));
      return;
    }

    final Properties props = new Properties();
    final Session session = Session.getDefaultInstance(props, null);

    try {
      final Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(FROM, "no-reply"));
      message.addRecipient(Message.RecipientType.TO, new InternetAddress(email, email));
      message.setSubject("Temporary Password");
      message.setText(createMailBody(generatedPassword));
      Transport.send(message);
    } catch (final MessagingException e) {
      throw new EmailSendFailure(e);
    } catch (final UnsupportedEncodingException e) {
      throw new AssertionError(e);
    }
  }

  private static String createMailBody(final String generatedPassword) {
    return "Your TripleA temporary password is: "
        + generatedPassword
        + "\nUse this password to login to the TripleA lobby."
        + "\nThis password may only be used once and will expire in "
        + TempPasswordDao.TEMP_PASSWORD_EXPIRATION
        + "\nAfter login you will be prompted to create a new password";
  }

  private static class EmailSendFailure extends RuntimeException {
    private static final long serialVersionUID = -7190784731567635418L;

    EmailSendFailure(final Exception e) {
      super("Failed to send email", e);
    }
  }
}
