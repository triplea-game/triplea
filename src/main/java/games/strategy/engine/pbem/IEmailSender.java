package games.strategy.engine.pbem;

import java.io.File;
import java.io.IOException;

import games.strategy.engine.framework.startup.ui.editors.IBean;

/**
 * An interface for sending emails from a PBEM (play by email ) game.
 * Implementers must be serialized, as the sender is stored as part of the save game.
 * It is also the job of the implementer to store the to address, host/port, credentials etc.
 */
public interface IEmailSender extends IBean {
  /**
   * Sends an email with the given subject, optionally attaches a save game file.
   * The address, and credentials must be stored by the implementing class
   *
   * @param subject
   *        the subject of the email
   * @param htmlMessage
   *        the html email body
   * @param saveGame
   *        the savegame or null
   * @throws IOException
   *         if an error occurs
   */
  void sendEmail(String subject, String htmlMessage, File saveGame, String saveGameName) throws IOException;

  /**
   * Get the to email addresses configured for this sender.
   * May contain multiple email addresses separated by space ' '
   *
   * @return the to addresses
   */
  String getToAddress();

  /**
   * Remove any sensitive information, like passwords before this object is saved in as a game properties.
   */
  void clearSensitiveInfo();

  /**
   * Clones this instance.
   *
   * @return return a clone
   */
  IEmailSender doClone();

  String getUserName();

  String getPassword();

  void setUserName(String userName);

  void setPassword(String password);

  /**
   * Should we also post at the end of combat move.
   *
   * @return true if the save game should be included in the summary
   */
  boolean getAlsoPostAfterCombatMove();

  /**
   * Configure if we should also post at the end of combat move.
   *
   * @param postAlso
   *        true if the save game should be included
   */
  void setAlsoPostAfterCombatMove(boolean postAlso);
}
