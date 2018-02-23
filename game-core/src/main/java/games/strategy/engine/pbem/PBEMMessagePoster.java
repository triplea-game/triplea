package games.strategy.engine.pbem;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.triplea.ui.MainGameFrame;
import games.strategy.triplea.ui.history.HistoryLog;
import games.strategy.ui.ProgressWindow;

/**
 * This class is responsible for posting turn summary and email at the end of each round in a PBEM game.
 * A new instance is created at end of turn, based on the Email and a forum poster stored in the game data.
 * The needs to be serialized since it is invoked through the IAbstractEndTurnDelegate which require all objects to be
 * serializable
 * although the PBEM games will always be local
 */
public class PBEMMessagePoster implements Serializable {
  public static final String FORUM_POSTER_PROP_NAME = "games.strategy.engine.pbem.IForumPoster";
  public static final String EMAIL_SENDER_PROP_NAME = "games.strategy.engine.pbem.IEmailSender";
  public static final String PBEM_GAME_PROP_NAME = "games.strategy.engine.pbem.PBEMMessagePoster";
  private static final long serialVersionUID = 2256265436928530566L;
  private final IForumPoster m_forumPoster;
  private final IEmailSender m_emailSender;
  private transient File saveGameFile = null;
  private transient String turnSummary = null;
  private final transient String saveGameRef = null;
  private transient String turnSummaryRef = null;
  private transient String emailSendStatus;
  private final transient PlayerID currentPlayer;
  private final transient int roundNumber;
  private final transient String gameNameAndInfo;

  public PBEMMessagePoster(final GameData gameData, final PlayerID currentPlayer, final int roundNumber,
      final String title) {
    this.currentPlayer = currentPlayer;
    this.roundNumber = roundNumber;
    m_forumPoster = (IForumPoster) gameData.getProperties().get(FORUM_POSTER_PROP_NAME);
    m_emailSender = (IEmailSender) gameData.getProperties().get(EMAIL_SENDER_PROP_NAME);
    gameNameAndInfo =
        "TripleA " + title + " for game: " + gameData.getGameName() + ", version: " + gameData.getGameVersion();
  }

  public boolean hasMessengers() {
    return (m_forumPoster != null) || (m_emailSender != null);
  }

  public static boolean gameDataHasPlayByEmailOrForumMessengers(final GameData gameData) {
    if (gameData == null) {
      return false;
    }
    final IForumPoster forumPoster = (IForumPoster) gameData.getProperties().get(FORUM_POSTER_PROP_NAME);
    final IEmailSender emailSender = (IEmailSender) gameData.getProperties().get(EMAIL_SENDER_PROP_NAME);
    final boolean isPbem = gameData.getProperties().get(PBEM_GAME_PROP_NAME, false);
    return isPbem && ((forumPoster != null) || (emailSender != null));
  }

  public IForumPoster getForumPoster() {
    return m_forumPoster;
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

  /**
   * Post summary to form and/or email, and writes the action performed to the history writer
   *
   * @param historyWriter
   *        the history writer (which has no effect since save game has already be generated...) // todo (kg)
   * @return true if all posts were successful
   */
  public boolean post(final IDelegateHistoryWriter historyWriter, final String title, final boolean includeSaveGame) {
    boolean forumSuccess = true;
    final StringBuilder saveGameSb = new StringBuilder().append("triplea_");
    if (m_forumPoster != null) {
      saveGameSb.append(m_forumPoster.getTopicId()).append("_");
    }
    saveGameSb.append(currentPlayer.getName().substring(0, Math.min(3, currentPlayer.getName().length() - 1)))
        .append(roundNumber);
    final String saveGameName = GameDataFileUtils.addExtension(saveGameSb.toString());
    if (m_forumPoster != null) {
      if (includeSaveGame) {
        m_forumPoster.addSaveGame(saveGameFile, saveGameName);
      }
      try {
        forumSuccess = m_forumPoster.postTurnSummary((gameNameAndInfo + "\n\n" + turnSummary),
            "TripleA " + title + ": " + currentPlayer.getName() + " round " + roundNumber);
        turnSummaryRef = m_forumPoster.getTurnSummaryRef();
        if ((turnSummaryRef != null) && (historyWriter != null)) {
          historyWriter.startEvent("Turn Summary: " + turnSummaryRef);
        }
      } catch (final Exception e) {
        ClientLogger.logQuietly("Failed to post game to forum", e);
      }
    }
    boolean emailSuccess = true;
    if (m_emailSender != null) {
      final StringBuilder subjectPostFix = new StringBuilder(currentPlayer.getName());
      subjectPostFix.append(" - ").append("round ").append(roundNumber);
      try {
        m_emailSender.sendEmail(subjectPostFix.toString(), convertToHtml((gameNameAndInfo + "\n\n" + turnSummary)),
            saveGameFile, saveGameName);
        emailSendStatus = "Success, sent to " + m_emailSender.getToAddress();
      } catch (final IOException e) {
        emailSuccess = false;
        emailSendStatus = "Failed! Error " + e.getMessage();
        ClientLogger.logQuietly("Failed to send game via email", e);
      }
    }
    if (historyWriter != null) {
      final StringBuilder sb = new StringBuilder("Post Turn Summary");
      if (m_forumPoster != null) {
        sb.append(" to ").append(m_forumPoster.getDisplayName()).append(" success = ")
            .append(String.valueOf(forumSuccess));
      }
      if (m_emailSender != null) {
        if (m_forumPoster != null) {
          sb.append(" and to ");
        } else {
          sb.append(" to ");
        }
        sb.append(m_emailSender.getToAddress()).append(" success = ").append(String.valueOf(emailSuccess));
      }
      historyWriter.startEvent(sb.toString());
    }
    return forumSuccess && emailSuccess;
  }

  /**
   * Converts text to html, by transforming \n to &lt;br/&gt;.
   *
   * @param string
   *        the string to transform
   * @return the transformed string
   */
  private static String convertToHtml(final String string) {
    return "<pre><br/>" + string.replaceAll("\n", "<br/>") + "<br/></pre>";
  }

  /**
   * Get the configured email sender.
   *
   * @return return an email sender or null
   */
  public IEmailSender getEmailSender() {
    return m_emailSender;
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
    if (m_forumPoster != null) {
      return m_forumPoster.getAlsoPostAfterCombatMove();
    }
    if (m_emailSender != null) {
      return m_emailSender.getAlsoPostAfterCombatMove();
    }
    return false;
  }

  public static void postTurn(final String title, final HistoryLog historyLog, final boolean includeSaveGame,
      final PBEMMessagePoster posterPbem, final IAbstractForumPosterDelegate postingDelegate,
      final MainGameFrame mainGameFrame, final JComponent postButton) {
    String message = "";
    final IForumPoster turnSummaryMsgr = posterPbem.getForumPoster();
    final StringBuilder sb = new StringBuilder();
    if (turnSummaryMsgr != null) {
      sb.append(message).append("Post ").append(title).append(" ");
      if (includeSaveGame) {
        sb.append("and save game ");
      }
      sb.append("to ").append(turnSummaryMsgr.getDisplayName()).append("?\n");
    }
    final IEmailSender emailSender = posterPbem.getEmailSender();
    if (emailSender != null) {
      sb.append("Send email to ").append(emailSender.getToAddress()).append("?\n");
    }
    message = sb.toString();
    final int choice = JOptionPane.showConfirmDialog(mainGameFrame, message, "Post " + title + "?", 2, -1, null);
    if (choice == 0) {
      if (postButton != null) {
        postButton.setEnabled(false);
      }
      final ProgressWindow progressWindow = new ProgressWindow(mainGameFrame, "Posting " + title + "...");
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
          if (saveGameFile != null) {
            mainGameFrame.getGame().saveGame(saveGameFile);
            posterPbem.setSaveGame(saveGameFile);
          }
        } catch (final Exception e) {
          postOk = false;
          ClientLogger.logQuietly("Failed to create save game", e);
        }
        posterPbem.setTurnSummary(historyLog.toString());
        try {
          // forward the poster to the delegate which invokes post() on the poster
          if (postingDelegate != null) {
            if (!postingDelegate.postTurnSummary(posterPbem, title, includeSaveGame)) {
              postOk = false;
            }
          } else {
            if (!posterPbem.post(null, title, includeSaveGame)) {
              postOk = false;
            }
          }
        } catch (final Exception e) {
          postOk = false;
          ClientLogger.logQuietly("Failed to post save game to forum", e);
        }
        if (postingDelegate != null) {
          postingDelegate.setHasPostedTurnSummary(postOk);
        }
        final StringBuilder sb1 = new StringBuilder();
        if (posterPbem.getForumPoster() != null) {
          final String saveGameRef = posterPbem.getSaveGameRef();
          final String turnSummaryRef = posterPbem.getTurnSummaryRef();
          if (saveGameRef != null) {
            sb1.append("\nSave Game : ").append(saveGameRef);
          }
          if (turnSummaryRef != null) {
            sb1.append("\nSummary Text: ").append(turnSummaryRef);
          }
        }
        if (posterPbem.getEmailSender() != null) {
          sb1.append("\nEmails: ").append(posterPbem.getEmailSendStatus());
        }
        historyLog.getWriter().println(sb1.toString());
        if (historyLog.isVisible()) {
          historyLog.setVisible(true);
        }
        try {
          if ((saveGameFile != null) && !saveGameFile.delete()) {
            System.out.println(
                (new StringBuilder()).append("INFO TripleA PBEM/PBF poster couldn't delete temporary savegame: ")
                    .append(saveGameFile.getCanonicalPath()).toString());
          }
        } catch (final IOException e) {
          ClientLogger.logQuietly("save game file = " + saveGameFile, e);
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
          if (finalPostOk) {
            JOptionPane.showMessageDialog(mainGameFrame, finalMessage, title + " Posted",
                JOptionPane.INFORMATION_MESSAGE);
          } else {
            JOptionPane.showMessageDialog(mainGameFrame, finalMessage, title + " Posted",
                JOptionPane.ERROR_MESSAGE);
          }
        });
      }).start();
    }
  }
}
