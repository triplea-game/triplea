package games.strategy.engine.pbem;

import java.io.File;
import java.io.IOException;

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
}
