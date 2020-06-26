package games.strategy.engine.framework.startup.ui.posted.game.pbf;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.npathai.hamcrestopt.OptionalMatchers;
import com.google.common.base.Preconditions;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.posted.game.pbf.IForumPoster;
import games.strategy.engine.posted.game.pbf.NodeBbForumPoster;
import games.strategy.engine.posted.game.pbf.NodeBbTokenGenerator;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;
import java.util.List;
import java.util.function.BiConsumer;
import javax.swing.SwingUtilities;
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
class ForumPosterEditorViewModelTest extends AbstractClientSettingTestCase {

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
  @ValueSource(strings = {"0.0", "-1", "2311111111111", "not a number", "NaN"})
  void viewForumPostNoOpOnInvalidTopicId(final String topicId) {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(() -> {});
    viewModel.setTopicId(topicId);
    viewModel.setViewForumPostAction(viewForumPostAction);

    viewModel.viewForumButtonClicked();

    verify(viewForumPostAction, never()).accept(any(), any());
  }

  private ForumPosterEditorViewModel givenViewModelWithInvalidFieldSettings(
      final String topicId, final String username, final char[] password) {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(() -> {});
    viewModel.setTopicId(topicId);
    viewModel.setForumUsername(username);
    viewModel.setForumPassword(password);
    return viewModel;
  }

  @SuppressWarnings("unused")
  private static List<Arguments> invalidForumSettings() {
    return List.of(
        // invalid topic IDs
        Arguments.of("not a number", "user", new char[] {'a'}),
        Arguments.of("", "user", new char[] {'a'}),
        Arguments.of("0.0", "user", new char[] {'a'}),
        Arguments.of("0", "user", new char[] {'a'}),
        Arguments.of("-1", "user", new char[] {'a'}),
        // missing user
        Arguments.of("1", "", new char[] {'a'}),
        // missing password
        Arguments.of("1", "", new char[] {}));
  }

  @DisplayName("Ensure fields are not valid if forum selection or topic id are not valid")
  @ParameterizedTest
  @MethodSource("invalidForumSettings")
  void invalidForumSettings(final String topicId, final String username, final char[] password) {
    final ForumPosterEditorViewModel viewModel =
        givenViewModelWithInvalidFieldSettings(topicId, username, password);

    assertThat(viewModel.areFieldsValid(), is(false));
  }

  @DisplayName("Ensure test post button is no-op if fields are not valid")
  @ParameterizedTest
  @MethodSource("invalidForumSettings")
  void forumPostTestButtonIsNoOpOnInvalidData(
      final String topicId, final String username, final char[] password) {
    final ForumPosterEditorViewModel viewModel =
        givenViewModelWithInvalidFieldSettings(topicId, username, password);
    viewModel.setTestPostAction(testPostAction);

    viewModel.testPostButtonClicked();
    verify(testPostAction, never()).accept(any(), any());
  }

  @Test
  void forumPostButtonIsActiveWithValidData() throws Exception {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(() -> {});
    viewModel.setTestPostAction(testPostAction);
    viewModel.setForumSelection("forumSelection");
    viewModel.setTopicId("20");
    Preconditions.checkState(
        viewModel.isTopicIdValid(), "Test makes sense only if topic id is valid");
    viewModel.setForumUsername("username");
    viewModel.setForumPassword(new char[] {'a', 'b'});
    final NodeBbTokenGenerator tokenGenerator = mock(NodeBbTokenGenerator.class);
    viewModel.setTokenGeneratorSupplier(() -> tokenGenerator);
    final NodeBbTokenGenerator.TokenInfo tokenInfo = mock(NodeBbTokenGenerator.TokenInfo.class);
    when(tokenGenerator.generateToken(any(), any(), any())).thenReturn(tokenInfo);
    when(tokenInfo.getToken()).thenReturn("");

    SwingUtilities.invokeAndWait(viewModel::testPostButtonClicked);

    verify(testPostAction).accept("forumSelection", 20);
    // No Token to revoke
    verify(tokenGenerator, times(0)).revokeToken(any(), anyInt());
  }

  @DisplayName("Ensure fields are valid if forum selection, topic id and credentials are set")
  @Test
  void validForumSettings() {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(() -> {});
    viewModel.setForumSelection("forumSelection");
    viewModel.setTopicId("20");
    viewModel.setForumUsername("username");
    viewModel.setForumPassword(new char[] {'a', 'b'});
    Preconditions.checkState(
        viewModel.isTopicIdValid(), "Test makes sense only if topic id is valid");

    assertThat(viewModel.areFieldsValid(), is(true));
  }

  @Test
  void topicId() {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(readyCallback);
    viewModel.setTopicId("");

    verify(readyCallback).run();
  }

  @Test
  void readyCallbackIsInvokedOnConstructionWhenForumIsSetToDefaultValue() {
    new ForumPosterEditorViewModel(readyCallback);

    verify(readyCallback).run();
  }

  @Test
  void updatingUsernameInvokesReadyCallback() {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(readyCallback);
    viewModel.setForumUsername("");

    verify(readyCallback, times(2)).run();
  }

  @Test
  void updatingPasswordInvokesReadyCallback() {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(readyCallback);
    viewModel.setForumPassword(new char[] {'a'});

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

  @Test
  void initiallyUsernameIsInvalidDueToBeingBlank() {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(readyCallback);
    assertThat(viewModel.isForumUsernameValid(), is(false));
  }

  @Test
  void nonBlankUsernameIsValid() {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(readyCallback);
    viewModel.setForumUsername("name");
    assertThat(viewModel.isForumUsernameValid(), is(true));
  }

  @Test
  void initiallyPasswordIsInvalidDueToBeingBlank() {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(readyCallback);
    assertThat(viewModel.isForumPasswordValid(), is(false));
  }

  @Test
  void nonBlankPasswordIsValid() {
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(readyCallback);
    viewModel.setForumPassword(new char[] {'a'});
    assertThat(viewModel.isForumPasswordValid(), is(true));
  }

  @Test
  @DisplayName("If password form UI is the dummy password, then it should not be set")
  void doNotSetADummyPassword() {
    ClientSetting.aaForumUsername.setValueAndFlush(new char[] {'a'});
    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(readyCallback);
    viewModel.setForumSelection(NodeBbForumPoster.AXIS_AND_ALLIES_ORG_DISPLAY_NAME);
    viewModel.setForumPassword(new char[] {'*', '*'});

    assertThat(
        "No password shoudl be set on the model, the dummy password is rejected",
        viewModel.getForumPassword(),
        is(emptyString()));
    assertThat(
        ClientSetting.aaForumUsername.getValue(),
        OptionalMatchers.isPresentAndIs(new char[] {'a'}));
  }

  @Test
  void changingForumSelectionToAxisAndAlliesOrgTogglesUsernameAndPassword() {
    ClientSetting.aaForumUsername.setValueAndFlush(new char[] {'a'});
    ClientSetting.aaForumToken.setValueAndFlush(new char[] {'b'});

    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(readyCallback);
    viewModel.setForumSelection(NodeBbForumPoster.AXIS_AND_ALLIES_ORG_DISPLAY_NAME);

    assertThat(viewModel.getForumUsername(), is("a"));
    assertThat(viewModel.getForumPassword().length(), is(4));
    assertThat(
        "we do not store the actual password, we'll set a dummy password in the text field "
            + "to represent it being set, only a token is stored in ClientSettings.",
        String.valueOf(viewModel.getForumPassword()),
        is(not("b")));
  }

  @Test
  void changingForumSelectionToTripleATogglesUsernameAndPassword() {
    ClientSetting.tripleaForumUsername.setValueAndFlush(new char[] {'c'});

    final ForumPosterEditorViewModel viewModel = new ForumPosterEditorViewModel(readyCallback);
    viewModel.setForumSelection(NodeBbForumPoster.TRIPLEA_FORUM_DISPLAY_NAME);

    assertThat(viewModel.getForumUsername(), is("c"));
    assertThat(viewModel.getForumPassword().length(), is(0));
  }
}
