package games.strategy.engine.framework.startup.ui.pbem;

import com.google.common.annotations.VisibleForTesting;
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
  private String forumSelection = "";
  @Getter private boolean forumSelectionValid;
  @Getter private String topicId = "";
  @Getter private boolean topicIdValid;
  @Setter @Getter private boolean attachSaveGameToSummary;
  @Setter @Getter private boolean alsoPostAfterCombatMove;

  ForumPosterEditorViewModel(final Runnable readyCallback) {
    this.readyCallback = readyCallback;
  }

  ForumPosterEditorViewModel(final Runnable readyCallback, final GameProperties properties) {
    this.readyCallback = readyCallback;
    setForumSelection((String) properties.get(IForumPoster.NAME));
    setTopicId(properties.get(IForumPoster.TOPIC_ID, ""));
    this.alsoPostAfterCombatMove = properties.get(IForumPoster.POST_AFTER_COMBAT, false);
    this.attachSaveGameToSummary = properties.get(IForumPoster.INCLUDE_SAVEGAME, false);
  }

  synchronized void setForumSelection(final String forumSelection) {
    this.forumSelection = forumSelection;
    forumSelectionValid = !forumSelection.isBlank();
    viewForumPostButtonEnabled = areFieldsValid();
    testForumPostButtonEnabled = areFieldsValid();
    readyCallback.run();
  }

  synchronized void setTopicId(final String topicId) {
    this.topicId = topicId;
    final Integer numericTopicId = Ints.tryParse(topicId);
    topicIdValid = numericTopicId != null && numericTopicId > 0;
    viewForumPostButtonEnabled = areFieldsValid();
    testForumPostButtonEnabled = areFieldsValid();
    readyCallback.run();
  }

  String getForumSelection() {
    return Optional.ofNullable(forumSelection) //
        .orElse(getForumSelectionOptions().iterator().next());
  }

  Iterable<String> getForumSelectionOptions() {
    final Iterable<String> selections = NodeBbForumPoster.availablePosters();
    Postconditions.assertState(selections.iterator().hasNext());
    return selections;
  }

  synchronized boolean areFieldsValid() {
    return forumSelectionValid && topicIdValid;
  }

  public void populateFromGameProperties(final GameProperties properties) {
    this.forumSelection = (String) properties.get(IForumPoster.NAME);
    this.topicId = properties.get(IForumPoster.TOPIC_ID, "");
    this.alsoPostAfterCombatMove = properties.get(IForumPoster.POST_AFTER_COMBAT, false);
    this.attachSaveGameToSummary = properties.get(IForumPoster.INCLUDE_SAVEGAME, true);
    view.viewModelChanged(this);
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
