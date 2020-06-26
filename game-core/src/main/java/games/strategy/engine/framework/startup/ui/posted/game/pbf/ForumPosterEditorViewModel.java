package games.strategy.engine.framework.startup.ui.posted.game.pbf;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.startup.ui.posted.game.HelpTexts;
import games.strategy.engine.framework.startup.ui.posted.game.pbf.test.post.SwingTestPostProgressDisplayFactory;
import games.strategy.engine.framework.startup.ui.posted.game.pbf.test.post.TestPostAction;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.engine.posted.game.pbf.IForumPoster;
import games.strategy.engine.posted.game.pbf.NodeBbForumPoster;
import games.strategy.engine.posted.game.pbf.NodeBbTokenGenerator;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import java.nio.CharBuffer;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.Setter;
import org.triplea.java.Interruptibles;
import org.triplea.java.Postconditions;
import org.triplea.java.StringUtils;
import org.triplea.java.ViewModelListener;

class ForumPosterEditorViewModel {
  private static final int DUMMY_PASSWORD_LENGTH = 4;

  private final Runnable readyCallback;

  @Setter(onMethod_ = @VisibleForTesting)
  private BiConsumer<String, Integer> testPostAction =
      new TestPostAction(new SwingTestPostProgressDisplayFactory());

  @Setter private ViewModelListener<ForumPosterEditorViewModel> view;

  @Setter(onMethod_ = @VisibleForTesting)
  private BiConsumer<String, Integer> viewForumPostAction =
      (forumName, topicId) -> NodeBbForumPoster.newInstanceByName(forumName, topicId).viewPosted();

  private String forumSelection;
  @Getter private String topicId = "";
  @Setter @Getter private boolean attachSaveGameToSummary = true;
  @Setter @Getter private boolean alsoPostAfterCombatMove;
  @Getter private String forumUsername;
  private boolean forumTokenExists;
  @Setter private boolean rememberPassword;

  ForumPosterEditorViewModel(final Runnable readyCallback) {
    this.readyCallback = readyCallback;
    rememberPassword = ClientSetting.rememberForumPassword.getValue().orElse(false);
    setForumSelection("");
  }

  ForumPosterEditorViewModel(final Runnable readyCallback, final GameProperties properties) {
    this.readyCallback = readyCallback;
    rememberPassword = ClientSetting.rememberForumPassword.getValue().orElse(false);
    populateFromGameProperties(properties);
  }

  void setForumPassword(final char[] password) {
    // Check if the password is a dummy. Document model events are only supposed
    // to fire if a user types and enters a valid password, in case an event from UI
    // was thrown from UI for another reason and the password field still has the
    // dummy password, then ignore it.
    if (isDummyPassword(password)) {
      readyCallback.run();
      return;
    }
    final ClientSetting<char[]> tokenSetting = getTokenSetting();
    final ClientSetting<Integer> uidSetting = getUidSetting();

    Interruptibles.awaitResult(
            () ->
                BackgroundTaskRunner.runInBackgroundAndReturn(
                    "Logging in...",
                    () -> {
                      revokeToken();

                      final var nodeBbTokenGenerator = new NodeBbTokenGenerator(getForumUrl());

                      return nodeBbTokenGenerator.generateToken(
                          forumUsername, new String(password), null);
                    }))
        .result
        .ifPresent(
            tokenInfo -> {
              tokenSetting.setValueAndFlush(tokenInfo.getToken().toCharArray());
              uidSetting.setValueAndFlush(tokenInfo.getUserId());

              forumTokenExists =
                  tokenSetting.getValue().map(token -> token.length > 0).orElse(false);
              readyCallback.run();
            });
  }

  private ClientSetting<char[]> getTokenSetting() {
    return forumSelection.equals(NodeBbForumPoster.TRIPLEA_FORUM_DISPLAY_NAME)
        ? ClientSetting.tripleaForumToken
        : ClientSetting.aaForumToken;
  }

  private ClientSetting<Integer> getUidSetting() {
    return forumSelection.equals(NodeBbForumPoster.TRIPLEA_FORUM_DISPLAY_NAME)
        ? ClientSetting.tripleaForumUserId
        : ClientSetting.aaForumUserId;
  }

  private String getForumUrl() {
    return forumSelection.equals(NodeBbForumPoster.TRIPLEA_FORUM_DISPLAY_NAME)
        ? UrlConstants.TRIPLEA_FORUM
        : UrlConstants.AXIS_AND_ALLIES_FORUM;
  }

  private boolean isDummyPassword(final char[] password) {
    return password.length > 0 && CharBuffer.wrap(password).chars().allMatch(c -> c == '*');
  }

  void setForumUsername(final String username) {
    final ClientSetting<char[]> usernameSetting =
        forumSelection.equals(NodeBbForumPoster.TRIPLEA_FORUM_DISPLAY_NAME)
            ? ClientSetting.tripleaForumUsername
            : ClientSetting.aaForumUsername;

    usernameSetting.setValueAndFlush(username.toCharArray());
    forumUsername = username;
    readyCallback.run();
  }

  synchronized void setForumSelection(final String forumSelection) {
    // if forumSelect is null or blank, default it to first forum selection option
    this.forumSelection =
        Optional.ofNullable(forumSelection)
            .filter(Predicate.not(String::isBlank))
            .orElse(getForumSelectionOptions().iterator().next());
    Postconditions.assertState(
        this.forumSelection != null && !this.forumSelection.isBlank(),
        "Forum selection is driven by a drop-down to ensure user can never set it to null, "
            + "if setting to null from a game properties, we default to the first available "
            + "selection entry, forum selection should never be null or empty.");
    forumUsername =
        this.forumSelection.equals(NodeBbForumPoster.TRIPLEA_FORUM_DISPLAY_NAME)
            ? ClientSetting.tripleaForumUsername.getValue().map(String::valueOf).orElse("")
            : ClientSetting.aaForumUsername.getValue().map(String::valueOf).orElse("");
    forumTokenExists = false;
    readyCallback.run();
  }

  /**
   * Returns a dummy password value that has the value of {@link #DUMMY_PASSWORD_LENGTH}. This
   * should only be used to set the UI text value and never used as the actual users password.
   */
  String getForumPassword() {
    return forumTokenExists ? Strings.repeat("*", DUMMY_PASSWORD_LENGTH) : "";
  }

  public String getForumSelection() {
    Preconditions.checkNotNull(forumSelection, "Forum selection should never be null");
    return forumSelection;
  }

  synchronized void setTopicId(final String topicId) {
    if (this.topicId.equals(topicId)) {
      return;
    }

    this.topicId = topicId;
    readyCallback.run();
  }

  Iterable<String> getForumSelectionOptions() {
    final Iterable<String> selections = NodeBbForumPoster.availablePosters();
    Postconditions.assertState(selections.iterator().hasNext());
    return selections;
  }

  synchronized boolean areFieldsValid() {
    return isTopicIdValid() && isForumUsernameValid() && isForumPasswordValid();
  }

  String getForumProviderHelpText() {
    return forumSelection.equals(NodeBbForumPoster.TRIPLEA_FORUM_DISPLAY_NAME)
        ? HelpTexts.TRIPLEA_FORUM
        : HelpTexts.AXIS_AND_ALLIES_DOT_ORG_FORUM;
  }

  boolean isForumPasswordValid() {
    return forumTokenExists;
  }

  boolean isForumUsernameValid() {
    return forumUsername != null && !forumUsername.isBlank();
  }

  boolean isViewForumPostButtonEnabled() {
    return isTopicIdValid();
  }

  boolean isTestForumPostButtonEnabled() {
    return areFieldsValid();
  }

  boolean isTopicIdValid() {
    return StringUtils.isPositiveInt(topicId);
  }

  public void populateFromGameProperties(final GameProperties properties) {
    setForumSelection((String) properties.get(IForumPoster.NAME));
    setTopicId(properties.get(IForumPoster.TOPIC_ID, ""));
    this.alsoPostAfterCombatMove = properties.get(IForumPoster.POST_AFTER_COMBAT, false);
    this.attachSaveGameToSummary = properties.get(IForumPoster.INCLUDE_SAVEGAME, true);
    Optional.ofNullable(view).ifPresent(v -> v.viewModelChanged(this));
  }

  synchronized void viewForumButtonClicked() {
    if (isTopicIdValid()) {
      viewForumPostAction.accept(forumSelection, Integer.parseInt(topicId));
    }
  }

  synchronized void testPostButtonClicked() {
    if (areFieldsValid()) {
      testPostAction.accept(forumSelection, Integer.parseInt(topicId));
    }
  }

  boolean shouldRevokeTokenOnShutdown() {
    return !rememberPassword;
  }

  void revokeToken() {
    final ClientSetting<char[]> tokenSetting = getTokenSetting();
    final ClientSetting<Integer> uidSetting = getUidSetting();

    final var nodeBbTokenGenerator = new NodeBbTokenGenerator(getForumUrl());

    tokenSetting
        .getValue()
        .ifPresent(
            token ->
                uidSetting
                    .getValue()
                    .ifPresent(uid -> nodeBbTokenGenerator.revokeToken(new String(token), uid)));
  }
}
