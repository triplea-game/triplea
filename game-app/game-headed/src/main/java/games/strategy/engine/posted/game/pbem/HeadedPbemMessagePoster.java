package games.strategy.engine.posted.game.pbem;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.posted.game.pbf.IForumPoster;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.triplea.ui.history.HistoryLog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.ThreadRunner;
import org.triplea.swing.DialogBuilder;
import org.triplea.swing.ProgressWindow;

/** A {@link PbemMessagePoster} that interacts with the user through Swing dialogs. */
@Slf4j
public class HeadedPbemMessagePoster extends PbemMessagePoster {
  private static final long serialVersionUID = -1L;

  public HeadedPbemMessagePoster(
      final GameData gameData,
      final GamePlayer currentPlayer,
      final int roundNumber,
      final String title) {
    super(gameData, currentPlayer, roundNumber, title);
  }

  @Override
  protected void onForumPostError(final Throwable error) {
    DialogBuilder.builder()
        .parent(null)
        .title("Error Posting to Forum")
        .errorMessage(error.getMessage())
        .showDialog();
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
      final JFrame frame,
      final IGame game,
      final JComponent postButton) {
    final GameProperties gameProperties = getGameProperties();
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
      ThreadRunner.runInNewThread(
          () -> {
            boolean postOk = true;
            Path saveGameFile = null;
            if (postingDelegate != null) {
              postingDelegate.setHasPostedTurnSummary(true);
            }
            try {
              saveGameFile = Files.createTempFile("triplea", GameDataFileUtils.getExtension());
              game.saveGame(saveGameFile);
              setSaveGame(saveGameFile);
            } catch (final Exception e) {
              postOk = false;
              log.error("Failed to create save game", e);
            }
            setTurnSummary(historyLog.toString());
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
              log.error("Failed to post save game to forum", e);
            }
            if (postingDelegate != null) {
              postingDelegate.setHasPostedTurnSummary(postOk);
            }
            final StringBuilder sb1 = new StringBuilder();
            if (gameProperties.get(IForumPoster.NAME) != null && getTurnSummaryRef() != null) {
              sb1.append("\nSummary Text: ").append(getTurnSummaryRef());
            }
            if (gameProperties.get(IEmailSender.SUBJECT) != null) {
              sb1.append("\nEmails: ").append(getEmailSendStatus());
            }
            historyLog.append(sb1.toString());
            historyLog.append("\n");
            if (historyLog.isVisible()) {
              historyLog.setVisible(true);
            }
            if (saveGameFile != null) {
              try {
                Files.delete(saveGameFile);
              } catch (final IOException e) {
                log.warn("Could not delete file " + saveGameFile.toAbsolutePath(), e);
              }
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
                      finalPostOk ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                });
          });
    }
  }
}
