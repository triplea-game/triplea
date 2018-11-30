package games.strategy.triplea.settings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.util.concurrent.Runnables;

@ExtendWith(MockitoExtension.class)
final class SaveFunctionTest {
  @Mock
  private SelectionComponent<JComponent> mockSelectionComponent;
  @Mock
  private SelectionComponent<JComponent> mockSelectionComponent2;
  @Mock
  private GameSetting<String> mockSetting;

  @Test
  void messageOnValidIsInformation() {
    givenValidationResults(true, true);

    final SaveFunction.SaveResult result = SaveFunction.saveSettings(
        Arrays.asList(mockSelectionComponent, mockSelectionComponent2), Runnables.doNothing());

    assertThat("There will always be a message back to the user", result.message, is(not(emptyString())));
    assertThat("All valid, message type should informational", result.dialogType, is(JOptionPane.INFORMATION_MESSAGE));
  }

  private void givenValidationResults(final boolean first, final boolean second) {
    when(mockSelectionComponent.isValid()).thenReturn(first);
    whenSelectionComponentSave(mockSelectionComponent, context -> context.setValue(mockSetting, TestData.fakeValue));
    if (first) {
      when(mockSetting.getValue()).thenReturn(Optional.empty());
    }

    when(mockSelectionComponent2.isValid()).thenReturn(second);
    whenSelectionComponentSave(mockSelectionComponent2, context -> context.setValue(mockSetting, "abc"));
  }

  private static void whenSelectionComponentSave(
      final SelectionComponent<?> selectionComponent,
      final Consumer<SelectionComponent.SaveContext> action) {
    doAnswer(invocation -> {
      final SelectionComponent.SaveContext context = (SelectionComponent.SaveContext) invocation.getArgument(0);
      action.accept(context);
      return null;
    }).when(selectionComponent).save(any(SelectionComponent.SaveContext.class));
  }

  @Test
  void messageOnNotValidResultIsWarning() {
    givenValidationResults(false, false);

    final SaveFunction.SaveResult result = SaveFunction.saveSettings(
        Arrays.asList(mockSelectionComponent, mockSelectionComponent2), Runnables.doNothing());

    assertThat(result.message, is(not(emptyString())));
    assertThat(result.dialogType, is(JOptionPane.WARNING_MESSAGE));
  }

  @Test
  void messageOnMixedResultIsWarning() {
    givenValidationResults(true, false);

    final SaveFunction.SaveResult result = SaveFunction.saveSettings(
        Arrays.asList(mockSelectionComponent, mockSelectionComponent2), Runnables.doNothing());

    assertThat(result.message, is(not(emptyString())));
    assertThat("At least one value was not updated, should be warning message type",
        result.dialogType, is(JOptionPane.WARNING_MESSAGE));
  }

  @Test
  void valueSavedWhenValid(@Mock final Runnable flushSettingsAction) {
    when(mockSelectionComponent.isValid()).thenReturn(true);
    whenSelectionComponentSave(mockSelectionComponent, context -> context.setValue(mockSetting, TestData.fakeValue));
    when(mockSetting.getValue()).thenReturn(Optional.empty());
    when(mockSelectionComponent2.isValid()).thenReturn(false);

    SaveFunction.saveSettings(Arrays.asList(mockSelectionComponent, mockSelectionComponent2), flushSettingsAction);

    verify(flushSettingsAction).run();
    verify(mockSetting).setValue(TestData.fakeValue);
  }

  @Test
  void noSettingsSavedIfAllInvalid(@Mock final Runnable flushSettingsAction) {
    when(mockSelectionComponent.isValid()).thenReturn(false);

    SaveFunction.saveSettings(Collections.singletonList(mockSelectionComponent), flushSettingsAction);

    verify(flushSettingsAction, never()).run();
  }

  private interface TestData {
    String fakeValue = "testing fake";
  }
}
