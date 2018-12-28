package games.strategy.engine.pbem;

import java.io.File;
import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import games.strategy.triplea.settings.ClientSetting;
import games.strategy.util.Util;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * An interface for sending emails from a PBEM (play by email ) game.
 * Implementers must be serialized, as the sender is stored as part of the save game.
 * It is also the job of the implementer to store the to address, host/port, credentials etc.
 */
public interface IEmailSender {

  String SUBJECT = "PBEM_SUBJECT";
  String OPPONENT = "PBEM_OPPONENT";
  String POST_AFTER_COMBAT = "PBEM_POST_AFTER_COMBAT";

  /**
   * Sends an email with the given subject, optionally attaches a save game file.
   * The address, and credentials must be stored by the implementing class
   *
   * @param subject the subject of the email
   * @param htmlMessage the html email body
   * @param saveGame the savegame or null
   * @throws IOException if an error occurs
   */
  void sendEmail(String subject, String htmlMessage, File saveGame, String saveGameName) throws IOException;

  String getDisplayName();

  /**
   * Data class to store a 3-tuple consisting of
   * a server host, a server port and whether or not
   * to use an encrypted connection.
   */
  @AllArgsConstructor
  @Immutable
  final class EmailProviderSetting {
    @Nonnull
    private final String displayName;
    @Getter
    @Nonnull
    private final String host;
    @Getter
    private final int port;
    @Getter
    private final boolean isEncrypted;

    @Override
    public String toString() {
      return displayName;
    }
  }

  /**
   * Creates an {@link IEmailSender} instance based on the given arguments and the configured settings.
   */
  static IEmailSender getEmailSender(final String subjectPrefix, final String toAddress) {
    return new GenericEmailSender(
        new IEmailSender.EmailProviderSetting(
            "",
            ClientSetting.emailServerHost.getValueOrThrow(),
            ClientSetting.emailServerPort.getValueOrThrow(),
            ClientSetting.emailServerSecurity.getValueOrThrow()
        ),
        Util.getFromSetting(ClientSetting.emailUsername),
        Util.getFromSetting(ClientSetting.emailPassword),
        subjectPrefix,
        toAddress);
  }
}
