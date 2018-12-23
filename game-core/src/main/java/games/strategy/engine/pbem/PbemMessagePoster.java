package games.strategy.engine.pbem;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.Future;
import java.util.logging.Level;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.history.HistoryLog;
import games.strategy.ui.ProgressWindow;
import lombok.extern.java.Log;

/**
 * This class is responsible for posting turn summary and email at the end of each round in a PBEM game.
 * A new instance is created at end of turn, based on the Email and a forum poster stored in the game data.
 * The needs to be serialized since it is invoked through the IAbstractEndTurnDelegate which require all objects to be
 * serializable although the PBEM games will always be local
 */
@Log
public class PbemMessagePoster {
  private final GameProperties gameProperties;
  private File saveGameFile = null;
  private String turnSummary = null;
  private final String saveGameRef = null;
  private String turnSummaryRef = null;
  private String emailSendStatus;
  private final transient PlayerId currentPlayer;
  private final transient int roundNumber;
  private final transient String gameNameAndInfo;

  public PbemMessagePoster(final GameData gameData, final PlayerId currentPlayer, final int roundNumber,
      final String title) {
    this.currentPlayer = currentPlayer;
    this.roundNumber = roundNumber;
    gameProperties = gameData.getProperties();
    gameNameAndInfo =
        "TripleA " + title + " for game: " + gameData.getGameName() + ", version: " + gameData.getGameVersion();
  }

  public boolean hasMessengers() {
    return gameProperties.get(IForumPoster.NAME) != null && gameProperties.get("EMAIL_POSTER") != null;
  }

  public static boolean gameDataHasPlayByEmailOrForumMessengers(final GameData gameData) {
    return gameData != null
        && (gameData.getProperties().get(IForumPoster.NAME) != null
        || gameData.getProperties().get(IEmailSender.SUBJECT) != null);
  }

  public void setTurnSummary(final String turnSummary) {
    this.turnSummary = turnSummary;
  }

  public void setSaveGame(final File saveGameFile) {
    this.saveGameFile = saveGameFile;
  }

  public String getTurnSummaryRef() {
    return turnSummaryRef;
  }

  public String getSaveGameRef() {
    return saveGameRef;
  }

  private IForumPoster getForumPoster() {
    final String name = gameProperties.get(IForumPoster.NAME, "");
    if (name.equals("TripleA")) { // FIXME change to actual names
      return new TripleAForumPoster(gameProperties.get(IForumPoster.TOPIC_ID, 0), "", "");
    } else if (name.equals("Axis and Allies")) {
      return new AxisAndAlliesForumPoster(gameProperties.get(IForumPoster.TOPIC_ID, 0), "", "");
    }
    return null;
  }

  /**
   * Post summary to form and/or email, and writes the action performed to the history writer.
   *
   * @param historyWriter the history writer (which has no effect since save game has already be generated) // todo (kg)
   * @return true if all posts were successful
   */
  public boolean post(final IDelegateHistoryWriter historyWriter, final String title) {
    IForumPoster forumPoster = getForumPoster();

    Future<String> forumSuccess = null;
    final StringBuilder saveGameSb = new StringBuilder().append("triplea_");
    if (forumPoster != null) {
      saveGameSb.append(gameProperties.get(IForumPoster.TOPIC_ID)).append("_");
    }
    saveGameSb.append(currentPlayer.getName(), 0, Math.min(3, currentPlayer.getName().length() - 1))
        .append(roundNumber);
    final String saveGameName = GameDataFileUtils.addExtension(saveGameSb.toString());
    if (forumPoster != null) {
      try {
        forumSuccess = forumPoster.postTurnSummary((gameNameAndInfo + "\n\n" + turnSummary),
            "TripleA " + title + ": " + currentPlayer.getName() + " round " + roundNumber, saveGameFile.toPath());
        turnSummaryRef = forumSuccess.get();
        if (turnSummaryRef != null && historyWriter != null) {
          historyWriter.startEvent("Turn Summary: " + turnSummaryRef);
        }
      } catch (final Exception e) {
        log.log(Level.SEVERE, "Failed to post game to forum", e);
      }
    }
    boolean emailSuccess = true;
    IEmailSender emailSender = null;
    if (gameProperties.get(GenericEmailSender.SUBJECT) != null) {
      final StringBuilder subjectPostFix = new StringBuilder(currentPlayer.getName());
      subjectPostFix.append(" - ").append("round ").append(roundNumber);
      try {
        emailSender.sendEmail(subjectPostFix.toString(), convertToHtml((gameNameAndInfo + "\n\n" + turnSummary)),
            saveGameFile, saveGameName);
        emailSendStatus = "Success, sent to " + gameProperties.get(IEmailSender.OPPONENT);
      } catch (final IOException e) {
        emailSuccess = false;
        emailSendStatus = "Failed! Error " + e.getMessage();
        log.log(Level.SEVERE, "Failed to send game via email", e);
      }
    }
    if (historyWriter != null) {
      final StringBuilder sb = new StringBuilder("Post Turn Summary");
      if (forumPoster != null) {
        sb.append(" to ").append(forumPoster.getDisplayName()).append(" success = ")
            .append(forumSuccess.isDone() && !forumSuccess.isCancelled());
      }
      if (emailSender != null) {
        if (forumPoster != null) {
          sb.append(" and to ");
        } else {
          sb.append(" to ");
        }
        sb.append(gameProperties.get(IEmailSender.OPPONENT)).append(" success = ").append(emailSuccess);
      }
      historyWriter.startEvent(sb.toString());
    }
    return !forumSuccess.isCancelled() && emailSuccess;
  }

  /**
   * Converts text to html, by transforming \n to &lt;br/&gt;.
   *
   * @param string the string to transform
   * @return the transformed string
   */
  private static String convertToHtml(final String string) {
    return "<pre><br/>" + string.replaceAll("\n", "<br/>") + "<br/></pre>";
  }

  /**
   * Return the status string from sending the email.
   *
   * @return a success of failure string, or null if no email sender was configured
   */
  public String getEmailSendStatus() {
    return emailSendStatus;
  }

  public boolean alsoPostMoveSummary() {
    return gameProperties.get(IForumPoster.POST_AFTER_COMBAT,
        gameProperties.get(IEmailSender.POST_AFTER_COMBAT, false));
  }

  /**
   * Posts a game turn summary (and optionally the associated save game) to the specified email service (if provided)
   * and forum (if provided). The user is first prompted to confirm they wish to perform the action before the turn is
   * posted.
   */
  public static void postTurn(final String title, final HistoryLog historyLog, final boolean includeSaveGame,
      final PbemMessagePoster posterPbem, final IAbstractForumPosterDelegate postingDelegate,
      final TripleAFrame frame, final JComponent postButton) {
    String message = "";
    final String displayName = posterPbem.gameProperties.get(IForumPoster.NAME, "");
    final StringBuilder sb = new StringBuilder();
    if (!displayName.isEmpty()) {
      sb.append(message).append("Post ").append(title).append(" ");
      if (includeSaveGame) {
        sb.append("and save game ");
      }
      sb.append("to ").append(displayName).append("?\n");
    }
    final String opponent = posterPbem.gameProperties.get(IEmailSender.OPPONENT, "");
    if (!opponent.isEmpty()) {
      sb.append("Send email to ").append(opponent).append("?\n");
    }
    message = sb.toString();
    final int choice = JOptionPane.showConfirmDialog(frame, message, "Post " + title + "?",
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
    if (choice == 0) {
      if (postButton != null) {
        postButton.setEnabled(false);
      }
      final ProgressWindow progressWindow = new ProgressWindow(frame, "Posting " + title + "...");
      progressWindow.setVisible(true);
      // start a new thread for posting the summary.
      new Thread(() -> {
        boolean postOk = true;
        File saveGameFile = null;
        if (postingDelegate != null) {
          postingDelegate.setHasPostedTurnSummary(true);
        }
        try {
          saveGameFile = File.createTempFile("triplea", GameDataFileUtils.getExtension());
          frame.getGame().saveGame(saveGameFile);
          posterPbem.setSaveGame(saveGameFile);
        } catch (final Exception e) {
          postOk = false;
          log.log(Level.SEVERE, "Failed to create save game", e);
        }
        posterPbem.setTurnSummary(historyLog.toString());
        try {
          // forward the poster to the delegate which invokes post() on the poster
          if (postingDelegate != null) {
            if (!postingDelegate.postTurnSummary(posterPbem, title)) {
              postOk = false;
            }
          } else {
            if (!posterPbem.post(null, title)) {
              postOk = false;
            }
          }
        } catch (final Exception e) {
          postOk = false;
          log.log(Level.SEVERE, "Failed to post save game to forum", e);
        }
        if (postingDelegate != null) {
          postingDelegate.setHasPostedTurnSummary(postOk);
        }
        final StringBuilder sb1 = new StringBuilder();
        if (posterPbem.gameProperties.get(IForumPoster.NAME) != null) {
          final String saveGameRef = posterPbem.getSaveGameRef();
          final String turnSummaryRef = posterPbem.getTurnSummaryRef();
          if (saveGameRef != null) {
            sb1.append("\nSave Game : ").append(saveGameRef);
          }
          if (turnSummaryRef != null) {
            sb1.append("\nSummary Text: ").append(turnSummaryRef);
          }
        }
        if (posterPbem.gameProperties.get(IEmailSender.SUBJECT) != null) {
          sb1.append("\nEmails: ").append(posterPbem.getEmailSendStatus());
        }
        historyLog.getWriter().println(sb1.toString());
        if (historyLog.isVisible()) {
          historyLog.setVisible(true);
        }
        saveGameFile.delete();
        progressWindow.setVisible(false);
        progressWindow.removeAll();
        progressWindow.dispose();
        final boolean finalPostOk = postOk;
        final String finalMessage = sb1.toString();
        SwingUtilities.invokeLater(() -> {
          if (postButton != null) {
            postButton.setEnabled(!finalPostOk);
          }
          if (finalPostOk) {
            JOptionPane.showMessageDialog(frame, finalMessage, title + " Posted",
                JOptionPane.INFORMATION_MESSAGE);
          } else {
            JOptionPane.showMessageDialog(frame, finalMessage, title + " Posted",
                JOptionPane.ERROR_MESSAGE);
          }
        });
      }).start();
    }
  }
}
