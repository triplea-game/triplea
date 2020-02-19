package games.strategy.engine.framework.startup.ui.posted.game.pbem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.startup.ui.posted.game.pbf.ForumPosterEditorViewModel;
import games.strategy.engine.posted.game.pbf.IForumPoster;
import games.strategy.engine.posted.game.pbf.NodeBbForumPoster;
import java.util.List;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.ViewModelListener;

@ExtendWith(MockitoExtension.class)
class ForumPosterEditorViewModelTest {

  @Mock private GameData gameData;
  @Mock private ViewModelListener<ForumPosterEditorViewModel> viewModelListener;
  @Mock private BiConsumer<String, Integer> viewForumPostAction;
  @Mock private BiConsumer<String, Integer> testPostAction;
  @Mock private Runnable readyCallback;

  @Test
  @DisplayName("Ensure forum selection is set when loading wtihout game properties")
  void initializationSetsForumSelection() {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(() -> {});

    assertThat(viewModel.getForumSelection(), is(notNullValue()));
  }

  @Test
  @DisplayName("Ensure we can use GameProperties to set initial values")
  void verifyInitializationWithGameProperties() {
    final GameProperties gameProperties = new GameProperties(gameData);
    gameProperties.set(IForumPoster.NAME, "forumName");
    gameProperties.set(IForumPoster.TOPIC_ID, "topicId");
    gameProperties.set(IForumPoster.POST_AFTER_COMBAT, true);
    gameProperties.set(IForumPoster.INCLUDE_SAVEGAME, false);

    final ForumPosterEditorViewModel viewModel =
        new ForumPosterEditorViewModel(() -> {}, gameProperties);

    assertThat(viewModel.getForumSelection(), is("forumName"));
    assertThat(viewModel.getTopicId(), is("topicId"));
    assertThat("topic id is not numeric => not valid", viewModel.isTopicIdValid(), is(false));
    assertThat(viewModel.isAlsoPostAfterCombatMove(), is(true));
    assertThat(viewModel.isAttachSaveGameToSummary(), is(false));
  }

  @Test
  @DisplayName("Ensure we can use partial GameProperties to set initial values")
  void verifyInitializeWithPartialGameProperties() {
    final GameProperties gameProperties = new GameProperties(gameData);
    gameProperties.set(IForumPoster.POST_AFTER_COMBAT, true);

    final ForumPosterEditorViewModel viewModel =
        new ForumPosterEditorViewModel(() -> {}, gameProperties);

    assertThat(
        "Default forum should be first NodeBB forum when not otherwise specified",
        viewModel.getForumSelection(),
        is(NodeBbForumPoster.availablePosters().iterator().next()));
    assertThat(
        "Expecting default empty topic id string when not specified in game properties",
        viewModel.getTopicId(),
        is(emptyString()));
    assertThat(
        "We specified this value in game properties, should be true",
        viewModel.isAlsoPostAfterCombatMove(),
        is(true));
    assertThat(
        "Attach save game summary defaults to true, when not specified in game properties, "
            + "we expect the default value",
        viewModel.isAttachSaveGameToSummary(),
        is(true));
  }

  @DisplayName("Topic ID should only be valid for positive numbers")
  @ParameterizedTest
  @ValueSource(strings = {"", "nAn", "0.0", "0.", "-1", "0", "zero", "1,000"})
  void topicIdNotValidOnNonPositiveOrNonNumbers(final String invalidValue) {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(() -> {});
    viewModel.setForumSelection(invalidValue);

    assertThat(viewModel.isTopicIdValid(), is(false));
  }

  @Test
  void viewForumPostCallsPostActionWhenDataIsValid() {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(() -> {});
    viewModel.setForumSelection("forumSelection");
    viewModel.setTopicId("100");
    Preconditions.checkState(
        viewModel.isTopicIdValid(), "Test makes sense only if topic id is valid");
    viewModel.setViewForumPostAction(viewForumPostAction);

    viewModel.viewForumButtonClicked();

    verify(viewForumPostAction).accept("forumSelection", 100);
  }

  @DisplayName("View Forum button click is no-op if topic id or forum selection are not valid")
  @ParameterizedTest
  @MethodSource("invalidForumSettings")
  void viewForumPostNoOpOnInvalidData(final String forumSelection, final String topicId) {
    final ForumPosterEditorViewModel viewModel =
        givenViewModelWithInvalidFieldSettings(forumSelection, topicId);
    viewModel.setViewForumPostAction(viewForumPostAction);

    viewModel.viewForumButtonClicked();

    verify(viewForumPostAction, never()).accept(any(), any());
  }

  private ForumPosterEditorViewModel givenViewModelWithInvalidFieldSettings(
      final String forumSelection, final String topicId) {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(() -> {});
    viewModel.setForumSelection(forumSelection);
    viewModel.setTopicId(topicId);
    Preconditions.checkState(
        !viewModel.isTopicIdValid(), "Test makes sense only if topic id is invalid");
    return viewModel;
  }

  @SuppressWarnings("unused")
  private static List<Arguments> invalidForumSettings() {
    return List.of(
        Arguments.of("forumSelection", "not a number"),
        Arguments.of("forumSelection", ""),
        Arguments.of("forumSelection", "0.0"),
        Arguments.of("forumSelection", "0"),
        Arguments.of("forumSelection", "-1"));
  }

  @DisplayName("Ensure fields are not valid if forum selection or topic id are not valid")
  @ParameterizedTest
  @MethodSource("invalidForumSettings")
  void invalidForumSettings(final String forumSelection, final String topicId) {
    final ForumPosterEditorViewModel viewModel =
        givenViewModelWithInvalidFieldSettings(forumSelection, topicId);

    assertThat(viewModel.areFieldsValid(), is(false));
  }

  @DisplayName("Ensure test post button is no-op if fields are not valid")
  @ParameterizedTest
  @MethodSource("invalidForumSettings")
  void forumPostTestButtonIsNoOpOnInvalidData(final String forumSelection, final String topicId) {
    final ForumPosterEditorViewModel viewModel =
        givenViewModelWithInvalidFieldSettings(forumSelection, topicId);
    viewModel.setTestPostAction(testPostAction);

    viewModel.testPostButtonClicked();

    verify(testPostAction, never()).accept(any(), any());
  }

  @Test
  void forumPostButtonIsActiveWithValidData() {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(() -> {});
    viewModel.setTestPostAction(testPostAction);
    viewModel.setForumSelection("forumSelection");
    viewModel.setTopicId("20");
    Preconditions.checkState(
        viewModel.isTopicIdValid(), "Test makes sense only if topic id is valid");

    viewModel.testPostButtonClicked();

    verify(testPostAction).accept("forumSelection", 20);
  }

  @DisplayName("Ensure fields are valid if both forum selection and topic id are valid")
  @Test
  void validForumSettings() {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(() -> {});
    viewModel.setForumSelection("forumSelection");
    viewModel.setTopicId("20");
    Preconditions.checkState(
        viewModel.isTopicIdValid(), "Test makes sense only if topic id is valid");

    assertThat(viewModel.areFieldsValid(), is(true));
  }

  @Test
  void topicId() {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(readyCallback);
    viewModel.setTopicId("");

    // called once in construction, called again on 'setTopicId'
    verify(readyCallback, times(2)).run();
  }

  @Test
  void forumSelection() {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(readyCallback);
    viewModel.setForumSelection("");

    // called once in construction, called again on 'setForumSelection'
    verify(readyCallback, times(2)).run();
  }

  @Test
  void viewCallbackIsInvokedWhenViewModelDataIsChanged() {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(readyCallback);
    viewModel.setView(viewModelListener);
    viewModel.populateFromGameProperties(new GameProperties(gameData));

    verify(viewModelListener).viewModelChanged(viewModel);
  }
}
