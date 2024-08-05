package games.strategy.engine.posted.game.pbem;

import com.google.common.base.Preconditions;
import games.strategy.triplea.settings.ClientSetting;
import java.io.IOException;
import java.nio.file.Path;
import org.triplea.util.Arrays;

/**
 * An interface for sending emails from a PBEM (play by email ) game. Implementers must be
 * serialized, as the sender is stored as part of the save game. It is also the job of the
 * implementer to store the to address, host/port, credentials etc.
 */
public interface IEmailSender {

  String SUBJECT = "PBEM_SUBJECT";
  String RECIPIENTS = "PBEM_RECIPIENTS";
  String POST_AFTER_COMBAT = "PBEM_POST_AFTER_COMBAT";

  /**
   * Sends an email with the given subject, optionally attaches a save game file. The address, and
   * credentials must be stored by the implementing class
   *
   * @param subject the subject of the email
   * @param htmlMessage the html email body
   * @param saveGame the save game or null
   * @throws IOException if an error occurs
   */
  void sendEmail(String subject, String htmlMessage, Path saveGame, String saveGameName)
      throws IOException;

  /**
   * Creates an {@link IEmailSender} instance based on the given arguments and the configured
   * settings.
   */
  static IEmailSender newInstance(final String subjectPrefix, final String toAddress) {
    Preconditions.checkNotNull(subjectPrefix);
    Preconditions.checkNotNull(toAddress);

    return new DefaultEmailSender(
        Arrays.withSensitiveArrayAndReturn(
            ClientSetting.emailUsername::getValueOrThrow, String::new),
        Arrays.withSensitiveArrayAndReturn(
            ClientSetting.emailPassword::getValueOrThrow, String::new),
        subjectPrefix,
        toAddress);
  }
}
