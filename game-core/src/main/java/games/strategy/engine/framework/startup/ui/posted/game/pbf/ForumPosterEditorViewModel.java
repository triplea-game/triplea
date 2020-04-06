package games.strategy.engine.framework.startup.ui.posted.game.pbf;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.startup.ui.posted.game.pbf.test.post.SwingTestPostProgressDisplayFactory;
import games.strategy.engine.framework.startup.ui.posted.game.pbf.test.post.TestPostAction;
import games.strategy.engine.posted.game.pbf.IForumPoster;
import games.strategy.engine.posted.game.pbf.NodeBbForumPoster;
import games.strategy.triplea.settings.ClientSetting;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.Setter;
import org.triplea.java.Postconditions;
import org.triplea.java.StringUtils;
import org.triplea.java.ViewModelListener;

class ForumPosterEditorViewModel {
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
  private boolean forumPasswordIsSet;
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
    final ClientSetting<char[]> passwordSetting =
        forumSelection.equals(NodeBbForumPoster.TRIPLEA_FORUM_DISPLAY_NAME)
            ? ClientSetting.tripleaForumPassword
            : ClientSetting.aaForumPassword;

    passwordSetting.setValueAndFlush(password);
    forumPasswordIsSet = password.length > 0;
    readyCallback.run();
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
    forumPasswordIsSet =
        this.forumSelection.equals(NodeBbForumPoster.TRIPLEA_FORUM_DISPLAY_NAME)
            ? ClientSetting.tripleaForumPassword.getValue().orElse(new char[0]).length > 0
            : ClientSetting.aaForumPassword.getValue().orElse(new char[0]).length > 0;
    readyCallback.run();
  }

  String getForumPassword() {
    return forumPasswordIsSet ? "********" : "";
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

  boolean isForumProviderTripleA() {
    return forumSelection.equals(NodeBbForumPoster.TRIPLEA_FORUM_DISPLAY_NAME);
  }

  boolean isForumPasswordValid() {
    return forumPasswordIsSet;
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

  boolean isForgetPasswordOnShutdown() {
    return !rememberPassword;
  }
}
