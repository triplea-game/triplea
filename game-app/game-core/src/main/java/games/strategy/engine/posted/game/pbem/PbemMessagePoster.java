package games.strategy.engine.posted.game.pbem;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.posted.game.pbf.IForumPoster;
import games.strategy.engine.posted.game.pbf.NodeBbForumPoster;
import games.strategy.engine.posted.game.pbf.NodeBbForumPoster.SaveGameParameter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is responsible for posting turn summary and email at the end of each round in a PBEM
 * game. A new instance is created at end of turn, based on the Email and a forum poster stored in
 * the game data. This class does only implement {@link Serializable} because otherwise the delegate
 * would reject it, even though this class is for local use only.
 */
@Slf4j
public class PbemMessagePoster implements Serializable {
  private static final long serialVersionUID = -1L;
  private final GameProperties gameProperties;
  private Path saveGameFile = null;
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
    gameNameAndInfo = "TripleA " + title + " for game: " + gameData.getGameName();
  }

  public boolean hasMessengers() {
    return gameProperties.get(IForumPoster.NAME) != null
        || gameProperties.get(IEmailSender.SUBJECT) != null;
  }

  public static boolean gameDataHasPlayByEmailOrForumMessengers(final GameState gameData) {
    return gameData != null
        && (gameData.getProperties().get(IForumPoster.NAME) != null
            || gameData.getProperties().get(IEmailSender.SUBJECT) != null);
  }

  public void setSaveGame(final Path saveGameFile) {
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
                    SaveGameParameter.builder()
                        .path(saveGameFile)
                        .displayName(saveGameName)
                        .build());
        final AtomicBoolean success = new AtomicBoolean(false);
        turnSummaryRef =
            forumSuccess
                .exceptionally(
                    e -> {
                      onForumPostError(e);
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
        log.error("Failed to post game to forum", e);
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
                    log.error("Failed to send game via email", e);
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
   * Called from {@link #post} when the forum post fails; the post continues with email. Logs by
   * default; a UI subclass may show the error to the user.
   */
  protected void onForumPostError(final Throwable error) {
    log.error("Error Posting to Forum", error);
  }

  protected GameProperties getGameProperties() {
    return gameProperties;
  }

  protected void setTurnSummary(final String turnSummary) {
    this.turnSummary = turnSummary;
  }

  protected String getTurnSummaryRef() {
    return turnSummaryRef;
  }

  protected String getEmailSendStatus() {
    return emailSendStatus;
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
