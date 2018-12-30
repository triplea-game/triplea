package games.strategy.engine.pbem;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
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
 */
@Log
public class PbemMessagePoster {
  private final GameProperties gameProperties;
  private File saveGameFile = null;
  private String turnSummary = null;
  private String turnSummaryRef = null;
  private String emailSendStatus;
  private final PlayerId currentPlayer;
  private final int roundNumber;
  private final String gameNameAndInfo;

  public PbemMessagePoster(final GameData gameData, final PlayerId currentPlayer, final int roundNumber,
      final String title) {
    this.currentPlayer = currentPlayer;
    this.roundNumber = roundNumber;
    gameProperties = gameData.getProperties();
    gameNameAndInfo =
        "TripleA " + title + " for game: " + gameData.getGameName() + ", version: " + gameData.getGameVersion();
  }

  public boolean hasMessengers() {
    return gameProperties.get(IForumPoster.NAME) != null && gameProperties.get(IEmailSender.SUBJECT) != null;
  }

  public static boolean gameDataHasPlayByEmailOrForumMessengers(final GameData gameData) {
    return gameData != null
        && (gameData.getProperties().get(IForumPoster.NAME) != null
        || gameData.getProperties().get(IEmailSender.SUBJECT) != null);
  }

  public void setSaveGame(final File saveGameFile) {
    this.saveGameFile = saveGameFile;
  }

  private Optional<IForumPoster> newForumPoster() {
    final String name = gameProperties.get(IForumPoster.NAME, "");
    if (name.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(IForumPoster.newInstanceByName(name, gameProperties.get(IForumPoster.TOPIC_ID, 0)));
  }

  private Optional<IEmailSender> newEmailSender() {
    final String subject = gameProperties.get(IEmailSender.SUBJECT, "");
    if (subject.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(IEmailSender.newInstance(subject, gameProperties.get(IEmailSender.OPPONENT, "")));
  }

  /**
   * Post summary to form and/or email, and writes the action performed to the history writer.
   *
   * @param historyWriter the history writer (which has no effect since save game has already be generated) // todo (kg)
   * @return true if all posts were successful
   */
  public boolean post(final IDelegateHistoryWriter historyWriter, final String title) {
    final Optional<IForumPoster> forumPoster = newForumPoster();

    final StringBuilder saveGameSb = new StringBuilder().append("triplea_");
    if (forumPoster.isPresent()) {
      saveGameSb.append(gameProperties.get(IForumPoster.TOPIC_ID)).append("_");
    }
    saveGameSb.append(currentPlayer.getName(), 0, Math.min(3, currentPlayer.getName().length() - 1))
        .append(roundNumber);
    final String saveGameName = GameDataFileUtils.addExtension(saveGameSb.toString());
    Future<String> forumSuccess = null;
    if (forumPoster.isPresent()) {
      try {
        forumSuccess = forumPoster.get().postTurnSummary((gameNameAndInfo + "\n\n" + turnSummary),
            "TripleA " + title + ": " + currentPlayer.getName() + " round " + roundNumber, saveGameFile.toPath());
        turnSummaryRef = forumSuccess.get();
        if (turnSummaryRef != null && historyWriter != null) {
          historyWriter.startEvent("Turn Summary: " + turnSummaryRef);
        }
      } catch (final Exception e) {
        log.log(Level.SEVERE, "Failed to post game to forum", e);
      }
    }
    final Optional<IEmailSender> emailSender = newEmailSender();
    boolean emailSuccess = emailSender.map(sender -> {
      try {
        sender.sendEmail(currentPlayer.getName() + " - round " + roundNumber,
            convertToHtml((gameNameAndInfo + "\n\n" + turnSummary)), saveGameFile, saveGameName);
        emailSendStatus = "Success, sent to " + gameProperties.get(IEmailSender.OPPONENT);
        return true;
      } catch (final IOException e) {
        emailSendStatus = "Failed! Error " + e.getMessage();
        log.log(Level.SEVERE, "Failed to send game via email", e);
        return false;
      }
    }).orElse(false);
    if (historyWriter != null) {
      final StringBuilder sb = new StringBuilder("Post Turn Summary");
      if (forumSuccess != null) {
        sb.append(" to ").append(forumPoster.get().getDisplayName()).append(" success = ")
            .append(forumSuccess.isDone() && !forumSuccess.isCancelled());
      }
      if (emailSender.isPresent()) {
        sb.append(forumPoster.isPresent() ? " and to " : " to ");
        sb.append(gameProperties.get(IEmailSender.OPPONENT)).append(" success = ").append(emailSuccess);
      }
      historyWriter.startEvent(sb.toString());
    }
    return (forumSuccess == null || !forumSuccess.isCancelled()) && emailSuccess;
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

  public boolean alsoPostMoveSummary() {
    return gameProperties.get(IForumPoster.POST_AFTER_COMBAT,
        gameProperties.get(IEmailSender.POST_AFTER_COMBAT, false));
  }

  /**
   * Posts a game turn summary (and optionally the associated save game) to the specified email service (if provided)
   * and forum (if provided). The user is first prompted to confirm they wish to perform the action before the turn is
   * posted.
   */
  public void postTurn(final String title, final HistoryLog historyLog, final boolean includeSaveGame,
      final IAbstractForumPosterDelegate postingDelegate,
      final TripleAFrame frame, final JComponent postButton) {
    String message = "";
    final String displayName = gameProperties.get(IForumPoster.NAME, "");
    final StringBuilder sb = new StringBuilder();
    if (!displayName.isEmpty()) {
      sb.append(message).append("Post ").append(title).append(" ");
      if (includeSaveGame) {
        sb.append("and save game ");
      }
      sb.append("to ").append(displayName).append("?\n");
    }
    final String opponent = gameProperties.get(IEmailSender.OPPONENT, "");
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
      // FIXME swap opponent here (so the opponent is always not you)
      new Thread(() -> {
        boolean postOk = true;
        File saveGameFile = null;
        if (postingDelegate != null) {
          postingDelegate.setHasPostedTurnSummary(true);
        }
        try {
          saveGameFile = File.createTempFile("triplea", GameDataFileUtils.getExtension());
          frame.getGame().saveGame(saveGameFile);
          setSaveGame(saveGameFile);
        } catch (final Exception e) {
          postOk = false;
          log.log(Level.SEVERE, "Failed to create save game", e);
        }
        turnSummary = historyLog.toString();
        try {
          // forward the poster to the delegate which invokes post() on the poster
          if (postingDelegate != null) {
            if (!postingDelegate.postTurnSummary(this, title)) {
              postOk = false;
            }
          } else {
            if (!post(null, title)) {
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
        if (gameProperties.get(IForumPoster.NAME) != null) {
          if (this.turnSummaryRef != null) {
            sb1.append("\nSummary Text: ").append(this.turnSummaryRef);
          }
        }
        if (gameProperties.get(IEmailSender.SUBJECT) != null) {
          sb1.append("\nEmails: ").append(emailSendStatus);
        }
        historyLog.getWriter().println(sb1.toString());
        if (historyLog.isVisible()) {
          historyLog.setVisible(true);
        }
        if (saveGameFile != null) {
          saveGameFile.delete();
        }
        progressWindow.setVisible(false);
        progressWindow.removeAll();
        progressWindow.dispose();
        final boolean finalPostOk = postOk;
        final String finalMessage = sb1.toString();
        SwingUtilities.invokeLater(() -> {
          if (postButton != null) {
            postButton.setEnabled(!finalPostOk);
          }
          JOptionPane.showMessageDialog(frame, finalMessage, title + " Posted",
              finalPostOk ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
        });
      }).start();
    }
  }
}
