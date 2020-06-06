package games.strategy.engine.posted.game.pbem;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.posted.game.pbf.IForumPoster;
import games.strategy.engine.posted.game.pbf.NodeBbForumPoster;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.history.HistoryLog;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.java.Log;
import org.triplea.swing.DialogBuilder;
import org.triplea.swing.ProgressWindow;

/**
 * This class is responsible for posting turn summary and email at the end of each round in a PBEM
 * game. A new instance is created at end of turn, based on the Email and a forum poster stored in
 * the game data. This class does only implement {@link Serializable} because otherwise the delegate
 * would reject it, even though this class is for local use only.
 */
@Log
public class PbemMessagePoster implements Serializable {
  private static final long serialVersionUID = -1L;
  private final GameProperties gameProperties;
  private File saveGameFile = null;
  private String turnSummary = null;
  private String turnSummaryRef = null;
  private String emailSendStatus;
  private final GamePlayer currentPlayer;
  private final int roundNumber;
  private final String gameNameAndInfo;

  public PbemMessagePoster(
      final GameData gameData,
      final GamePlayer currentPlayer,
      final int roundNumber,
      final String title) {
    this.currentPlayer = currentPlayer;
    this.roundNumber = roundNumber;
    gameProperties = gameData.getProperties();
    gameNameAndInfo =
        "TripleA "
            + title
            + " for game: "
            + gameData.getGameName()
            + ", version: "
            + gameData.getGameVersion();
  }

  public boolean hasMessengers() {
    return gameProperties.get(IForumPoster.NAME) != null
        || gameProperties.get(IEmailSender.SUBJECT) != null;
  }

  public static boolean gameDataHasPlayByEmailOrForumMessengers(final GameData gameData) {
    return gameData != null
        && (gameData.getProperties().get(IForumPoster.NAME) != null
            || gameData.getProperties().get(IEmailSender.SUBJECT) != null);
  }

  public void setSaveGame(final File saveGameFile) {
    this.saveGameFile = saveGameFile;
  }

  /**
   * Post summary to form and/or email, and writes the action performed to the history writer.
   *
   * @param historyWriter the history writer (which has no effect since save game has already be
   *     generated)
   * @return true if all posts were successful
   */
  public boolean post(final IDelegateHistoryWriter historyWriter, final String title) {
    final Optional<NodeBbForumPoster> forumPoster = newForumPoster();
    final StringBuilder saveGameSb = new StringBuilder().append("triplea_");

    if (forumPoster.isPresent()) {
      saveGameSb.append(gameProperties.get(IForumPoster.TOPIC_ID)).append("_");
    }
    saveGameSb
        .append(roundNumber)
        .append(currentPlayer.getName(), 0, Math.min(3, currentPlayer.getName().length() - 1));
    final String saveGameName = GameDataFileUtils.addExtension(saveGameSb.toString());
    CompletableFuture<String> forumSuccess = null;
    if (forumPoster.isPresent()) {
      try {
        forumSuccess =
            forumPoster
                .get()
                .postTurnSummary(
                    (gameNameAndInfo + "\n\n" + turnSummary),
                    "TripleA " + title + ": " + currentPlayer.getName() + " round " + roundNumber,
                    saveGameFile.toPath());
        final AtomicBoolean success = new AtomicBoolean(false);
        turnSummaryRef =
            forumSuccess
                .exceptionally(
                    e -> {
                      DialogBuilder.builder()
                          .parent(null)
                          .title("Error Posting to Forum")
                          .errorMessage(e.getMessage())
                          .showDialog();
                      return null;
                    })
                .whenComplete((v1, v2) -> success.set(true))
                .get();
        if (!success.get()) {
          return false;
        }

        if (turnSummaryRef != null && historyWriter != null) {
          historyWriter.startEvent("Turn Summary: " + turnSummaryRef);
        }
      } catch (final Exception e) {
        log.log(Level.SEVERE, "Failed to post game to forum", e);
      }
    }
    final Optional<IEmailSender> emailSender = newEmailSender();
    final boolean emailSuccess =
        emailSender
            .map(
                sender -> {
                  try {
                    sender.sendEmail(
                        currentPlayer.getName() + " - round " + roundNumber,
                        convertToHtml((gameNameAndInfo + "\n\n" + turnSummary)),
                        saveGameFile,
                        saveGameName);
                    emailSendStatus =
                        "Success, sent to " + gameProperties.get(IEmailSender.RECIPIENTS);
                    return true;
                  } catch (final IOException e) {
                    emailSendStatus = "Failed! Error " + e.getMessage();
                    log.log(Level.SEVERE, "Failed to send game via email", e);
                    return false;
                  }
                })
            .orElse(false);
    if (historyWriter != null) {
      final StringBuilder sb = new StringBuilder("Post Turn Summary");
      if (forumSuccess != null) {
        sb.append(" to ")
            .append(gameProperties.get(IForumPoster.NAME))
            .append(" success = ")
            .append(forumSuccess.isDone() && !forumSuccess.isCancelled());
      }
      if (emailSender.isPresent()) {
        sb.append(forumPoster.isPresent() ? " and to " : " to ");
        sb.append(gameProperties.get(IEmailSender.RECIPIENTS))
            .append(" success = ")
            .append(emailSuccess);
      }
      historyWriter.startEvent(sb.toString());
    }
    return (forumSuccess == null || !forumSuccess.isCancelled()) && emailSuccess;
  }

  private Optional<NodeBbForumPoster> newForumPoster() {
    final String name = gameProperties.get(IForumPoster.NAME, "");
    if (name.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        NodeBbForumPoster.newInstanceByName(name, gameProperties.get(IForumPoster.TOPIC_ID, 0)));
  }

  private Optional<IEmailSender> newEmailSender() {
    final String subject = gameProperties.get(IEmailSender.SUBJECT, "");
    if (subject.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        IEmailSender.newInstance(subject, gameProperties.get(IEmailSender.RECIPIENTS, "")));
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
    return gameProperties.get(
        IForumPoster.POST_AFTER_COMBAT, gameProperties.get(IEmailSender.POST_AFTER_COMBAT, false));
  }

  /**
   * Posts a game turn summary (and optionally the associated save game) to the specified email
   * service (if provided) and forum (if provided). The user is first prompted to confirm they wish
   * to perform the action before the turn is posted.
   */
  public void postTurn(
      final String title,
      final HistoryLog historyLog,
      final boolean includeSaveGame,
      final IAbstractForumPosterDelegate postingDelegate,
      final TripleAFrame frame,
      final JComponent postButton) {
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
    final String opponent = gameProperties.get(IEmailSender.RECIPIENTS, "");
    if (!opponent.isEmpty()) {
      sb.append("Send email to ").append(opponent).append("?\n");
    }
    message = sb.toString();
    final int choice =
        JOptionPane.showConfirmDialog(
            frame,
            message,
            "Post " + title + "?",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null);
    if (choice == 0) {
      if (postButton != null) {
        postButton.setEnabled(false);
      }
      final ProgressWindow progressWindow = new ProgressWindow(frame, "Posting " + title + "...");
      progressWindow.setVisible(true);
      // start a new thread for posting the summary.
      new Thread(
              () -> {
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
                if (gameProperties.get(IForumPoster.NAME) != null && this.turnSummaryRef != null) {
                  sb1.append("\nSummary Text: ").append(this.turnSummaryRef);
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
                SwingUtilities.invokeLater(
                    () -> {
                      if (postButton != null) {
                        postButton.setEnabled(!finalPostOk);
                      }
                      JOptionPane.showMessageDialog(
                          frame,
                          finalMessage,
                          title + " Posted",
                          finalPostOk
                              ? JOptionPane.INFORMATION_MESSAGE
                              : JOptionPane.ERROR_MESSAGE);
                    });
              })
          .start();
    }
  }

  @SuppressWarnings("static-method")
  private void readObject(@SuppressWarnings("unused") final ObjectInputStream stream) {
    throw new UnsupportedOperationException("This class shouldn't get de-serialized!");
  }

  @SuppressWarnings("static-method")
  private void writeObject(@SuppressWarnings("unused") final ObjectOutputStream stream) {
    throw new UnsupportedOperationException("This class shouldn't get serialized!");
  }
}
