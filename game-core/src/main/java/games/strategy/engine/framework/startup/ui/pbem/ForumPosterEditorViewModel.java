package games.strategy.engine.framework.startup.ui.pbem;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.primitives.Ints;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.startup.ui.pbem.test.post.SwingTestPostProgressDisplayFactory;
import games.strategy.engine.framework.startup.ui.pbem.test.post.TestPostAction;
import games.strategy.engine.posted.game.pbf.IForumPoster;
import games.strategy.engine.posted.game.pbf.NodeBbForumPoster;
import java.util.Optional;
import java.util.function.BiConsumer;
import lombok.Getter;
import lombok.Setter;
import org.triplea.java.Postconditions;
import org.triplea.java.ViewModelListener;

@SuppressWarnings("UnstableApiUsage")
class ForumPosterEditorViewModel {
  private final Runnable readyCallback;

  @Setter(onMethod_ = @VisibleForTesting)
  private BiConsumer<String, Integer> testPostAction =
      new TestPostAction(new SwingTestPostProgressDisplayFactory());

  @Setter private ViewModelListener<ForumPosterEditorViewModel> view;

  @Setter(onMethod_ = @VisibleForTesting)
  private BiConsumer<String, Integer> viewForumPostAction =
      (forumName, topicId) -> NodeBbForumPoster.newInstanceByName(forumName, topicId).viewPosted();

  @Getter private boolean viewForumPostButtonEnabled;
  @Getter private boolean testForumPostButtonEnabled;
  private String forumSelection;
  @Getter private String topicId = "";
  @Getter private boolean topicIdValid;
  @Setter @Getter private boolean attachSaveGameToSummary = true;
  @Setter @Getter private boolean alsoPostAfterCombatMove;

  ForumPosterEditorViewModel(final Runnable readyCallback) {
    this.readyCallback = readyCallback;
    setForumSelection("");
  }

  ForumPosterEditorViewModel(final Runnable readyCallback, final GameProperties properties) {
    this.readyCallback = readyCallback;
    populateFromGameProperties(properties);
  }

  @SuppressWarnings("Guava")
  synchronized void setForumSelection(final String forumSelection) {
    // if forumSelect is null or blank, default it to first forum selection option
    this.forumSelection =
        Optional.ofNullable(forumSelection)
            .filter(Predicates.not(String::isBlank))
            .orElse(getForumSelectionOptions().iterator().next());
    Postconditions.assertState(
        this.forumSelection != null && !this.forumSelection.isBlank(),
        "Forum selection is driven by a drop-down to ensure user can never set it to null, "
            + "if setting to null from a game properties, we default to the first available "
            + "selection entry, forum selection should never be null or empty.");
    viewForumPostButtonEnabled = areFieldsValid();
    testForumPostButtonEnabled = areFieldsValid();
    readyCallback.run();
    Optional.ofNullable(view).ifPresent(v -> v.viewModelChanged(this));
  }

  public String getForumSelection() {
    Preconditions.checkNotNull(forumSelection, "Forum selection should never be null");
    return forumSelection;
  }

  synchronized void setTopicId(final String topicId) {
    this.topicId = topicId;
    final Integer numericTopicId = Ints.tryParse(topicId);
    topicIdValid = numericTopicId != null && numericTopicId > 0;
    viewForumPostButtonEnabled = areFieldsValid();
    testForumPostButtonEnabled = areFieldsValid();
    readyCallback.run();
    Optional.ofNullable(view).ifPresent(v -> v.viewModelChanged(this));
  }

  Iterable<String> getForumSelectionOptions() {
    final Iterable<String> selections = NodeBbForumPoster.availablePosters();
    Postconditions.assertState(selections.iterator().hasNext());
    return selections;
  }

  synchronized boolean areFieldsValid() {
    return topicIdValid;
  }

  public void populateFromGameProperties(final GameProperties properties) {
    setForumSelection((String) properties.get(IForumPoster.NAME));
    setTopicId(properties.get(IForumPoster.TOPIC_ID, ""));
    this.alsoPostAfterCombatMove = properties.get(IForumPoster.POST_AFTER_COMBAT, false);
    this.attachSaveGameToSummary = properties.get(IForumPoster.INCLUDE_SAVEGAME, true);
    Optional.ofNullable(view).ifPresent(v -> v.viewModelChanged(this));
  }

  synchronized void viewForumButtonClicked() {
    if (areFieldsValid()) {
      viewForumPostAction.accept(forumSelection, Integer.parseInt(topicId));
    }
  }

  synchronized void testPostButtonClicked() {
    if (areFieldsValid()) {
      testPostAction.accept(forumSelection, Integer.parseInt(topicId));
    }
  }
}
