package games.strategy.engine.framework.startup.ui.posted.game;

import lombok.experimental.UtilityClass;

/**
 * Stores help text constants and helper methods to create help texts used in the PBF and PBEM
 * screens.
 */
@UtilityClass
public class HelpTexts {

  public static final String TRIPLEA_FORUM =
      "<html><p style='width: 400px;'>"
          + "Posts to forums.triplea-game.org<br/>"
          + "You can play PBEM/PBF games via forums.triplea-game.org.<br/>"
          + "Instructions:<br/>"
          + "Create a new Forum post in the Play by Forum category "
          + "https://forums.triplea-game.org/category/6/<br/>"
          + "Copy the topic id from the URL displayed in the address bar of you "
          + "browser. "
          + "If the URL is https://forums.triplea-game.org/topic/24/ put the topic "
          + "number (24) into the Topic ID field in TripleA<br/>"
          + "Put your username and password for the forums.triplea-game.org forum "
          + "into the username and password fields<br/>"
          + "Click the Test Post button, to check that TripleA can reply to your "
          + "forum post<br/>"
          + "<i>Note:</i> Your forums.triplea-game.org username and password are not stored "
          + "as part of the save game, but they are stored encrypted in the local "
          + "file system if you select the option to remember your credentials. You "
          + "may have to enter your username and password again if you open the save "
          + "game on another computer."
          + "</p></html>";
  public static final String AXIS_AND_ALLIES_DOT_ORG_FORUM =
      "<html><p style='width: 400px;'>"
          + "Posts to www.AxisAndAllies.org<br/>"
          + "This poster is build for PBEM games via AxisAndAllies.org. Instructions:"
          + "<br/>"
          + "Create a new Forum post in the Play Boardgames section "
          + "http://www.axisandallies.org/forums/index.php?board=40.0<br/>"
          + "Copy the topic id from the URL displayed in the address bar of you "
          + "browser. If the URL is "
          + "http://www.axisandallies.org/forums/index.php?topic=25252.0, "
          + "put this 5 digit number (25252) into the Topic ID field in TripleA<br/>"
          + "Put your username and password for the AxisAndAllies.org forum into the "
          + "username and password fields<br/>"
          + "Click the Test Post button, to check that TripleA can reply to your forum "
          + "post<br/>"
          + "<i>Note:</i> Your AxisAndAllies.org username and password are not stored as part "
          + "of the save game, but they are stored encrypted in the local file system "
          + "if you select the option to remember your credentials. You may have to "
          + "enter your username and password again if you open the save game on "
          + "another computer."
          + "</p></html>";
  public static final String SMTP_DISABLED =
      "<html><p style='width: 400px;'>"
          + "Email sender<br/>"
          + "An email sender can email the turn summary and save game to multiple "
          + "recipients at the end of each players turn. This allows two or more "
          + "players to play a game where you don't have to be online at the same "
          + "time.<br/>"
          + "Each email sender may require custom configuration, to learn more "
          + "click help again after selecting a specific email sender."
          + "</p></html>";

  public static final String GENERIC_SMTP_SERVER =
      "<html><p style='width: 400px;'>"
          + "Email through SMTP<br/>"
          + "This email sends email via any generic SMTP service. Configuration:<br/>"
          + "<b>Subject:</b> This will be the subject of the email. In addition to the text "
          + "entered, the player and round number will be appended<br/>"
          + "<b>To:</b> A list of email addresses separated by space. the email will be sent "
          + "to all these users<br/>"
          + "<b>Email Username:</b>: Your login to the smtp server<br/>"
          + "<b>Email Password:</b> Your password smtp server<br/>"
          + "<b>SMTP Server:</b> The host address (or ip) of the SMTP server<br/>"
          + "<b>Port:</b> The port of the smtp server (typically 25 for unencrypted, and 587 "
          + "for TLS encrypted servers)<br/>"
          + "Use TLS encryption: check this if yor server supports the TLS protocol for "
          + "email encryption<br/>"
          + "<i>Note:</i> Your SMTP login and password are not stored as part of the save game, "
          + "but they are stored encrypted in the local file system if you select the "
          + "option to remember your credentials.<br/>"
          + "You may have to enter your login and password again if you open the save "
          + "game on another computer."
          + "</p></html>";

  public static String gmailHelpText() {
    return emailProviderHelpText("Gmail", "Gmails");
  }

  public static String hotmailHelpText() {
    return emailProviderHelpText("Hotmail", "Hotmails (live.com)");
  }

  private static String emailProviderHelpText(final String name, final String possisiveName) {
    return String.format(
        "<html><p style='width: 400px;'>"
            + "Email through %s<br/>"
            + "This email sends email via %s SMTP service. To use this "
            + "you must have a %s account. Configuration:<br/>"
            + "<b>Subject:</b> This will be the subject of the email. In addition to the text "
            + "entered, the player and round number will be appended<br/>"
            + "<b>To:</b> A list of email addresses separated by space. the email will be sent to "
            + "all these users<br/>"
            + "<b>Email Username:</b> Your %s login used to authenticate against the %s smtp "
            + "service<br/>"
            + "<b>Email Password:</b> Your %s password used to authenticate against the %s "
            + "smtp service<br/>"
            + "<i>Note:</i> All communication with the %s service uses TLS encryption. Your "
            + "%s login and password are not stored as part of the save game, but "
            + "they are stored encrypted in the local file system if you select the option "
            + "to remember your credentials.<br/>"
            + "You may have to enter your login and password again if you open the save "
            + "game on another computer."
            + "</p></html>",
        name, possisiveName, name, name, name, name, name, name, name);
  }

  public static String rememberPlayByForumPassword() {
    return rememberPassword("play-by-forum");
  }

  public static String rememberPlayByEmailPassword() {
    return rememberPassword("play-by-email");
  }

  private static String rememberPassword(final String playTypeLabel) {
    return String.format(
        "<html>Remembering your password will store your password on your<br>"
            + "computer in an encrypted format. Otherwise you will need to enter your password<br>"
            + "whenever you start a %s game. While difficult, a person or virus<br>"
            + "with full access to your system could decrypt a remembered password.",
        playTypeLabel);
  }
}
