package games.strategy.triplea.settings;

import static games.strategy.triplea.settings.SaveFunction.toDisplayString;
import static games.strategy.triplea.settings.SelectionComponent.SaveContext.ValueSensitivity.INSENSITIVE;
import static games.strategy.triplea.settings.SelectionComponent.SaveContext.ValueSensitivity.SENSITIVE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.Runnables;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

final class SaveFunctionTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  final class SaveSettingsTest {
    @Mock private SelectionComponent<JComponent> mockSelectionComponent;
    @Mock private SelectionComponent<JComponent> mockSelectionComponent2;
    @Mock private GameSetting<String> mockSetting;

    @Test
    void messageOnValidIsInformation() {
      givenValidationResults(true, true);

      final SaveFunction.SaveResult result =
          SaveFunction.saveSettings(
              List.of(mockSelectionComponent, mockSelectionComponent2), Runnables.doNothing());

      assertThat(
          "There will always be a message back to the user",
          result.message,
          is(not(emptyString())));
      assertThat(
          "All valid, message type should informational",
          result.dialogType,
          is(JOptionPane.INFORMATION_MESSAGE));
    }

    private void givenValidationResults(final boolean first, final boolean second) {
      whenSelectionComponentSave(
          mockSelectionComponent,
          context -> {
            if (first) {
              context.setValue(mockSetting, TestData.fakeValue);
            } else {
              context.reportError(mockSetting, "first failed", TestData.fakeValue);
            }
          });

      whenSelectionComponentSave(
          mockSelectionComponent2,
          context -> {
            if (second) {
              context.setValue(mockSetting, "abc");
            } else {
              context.reportError(mockSetting, "second failed", "abc");
            }
          });
    }

    private void whenSelectionComponentSave(
        final SelectionComponent<?> selectionComponent,
        final Consumer<SelectionComponent.SaveContext> action) {
      doAnswer(
              invocation -> {
                final SelectionComponent.SaveContext context = invocation.getArgument(0);
                action.accept(context);
                return null;
              })
          .when(selectionComponent)
          .save(any(SelectionComponent.SaveContext.class));
    }

    @Test
    void messageOnNotValidResultIsWarning() {
      givenValidationResults(false, false);

      final SaveFunction.SaveResult result =
          SaveFunction.saveSettings(
              List.of(mockSelectionComponent, mockSelectionComponent2), Runnables.doNothing());

      assertThat(result.message, is(not(emptyString())));
      assertThat(result.dialogType, is(JOptionPane.WARNING_MESSAGE));
    }

    @Test
    void messageOnMixedResultIsWarning() {
      givenValidationResults(true, false);

      final SaveFunction.SaveResult result =
          SaveFunction.saveSettings(
              List.of(mockSelectionComponent, mockSelectionComponent2), Runnables.doNothing());

      assertThat(result.message, is(not(emptyString())));
      assertThat(
          "At least one value was not updated, should be warning message type",
          result.dialogType,
          is(JOptionPane.WARNING_MESSAGE));
    }

    @Test
    void valueSavedWhenValid(@Mock final Runnable flushSettingsAction) {
      whenSelectionComponentSave(
          mockSelectionComponent, context -> context.setValue(mockSetting, TestData.fakeValue));

      SaveFunction.saveSettings(List.of(mockSelectionComponent), flushSettingsAction);

      verify(flushSettingsAction).run();
      verify(mockSetting).setValue(TestData.fakeValue);
    }

    @Test
    void noSettingsSavedIfAllInvalid(@Mock final Runnable flushSettingsAction) {
      whenSelectionComponentSave(
          mockSelectionComponent,
          context -> context.reportError(mockSetting, "failed", TestData.fakeValue));

      SaveFunction.saveSettings(List.of(mockSelectionComponent), flushSettingsAction);

      verify(flushSettingsAction, never()).run();
      verify(mockSetting, never()).setValue(TestData.fakeValue);
    }
  }

  @Nested
  final class ToDisplayStringTest {
    @Nested
    final class WhenValueIsNull {
      @Test
      void shouldReturnDefaultValueWhenInsensitiveAndDefaultValueIsNotNull() {
        assertThat(toDisplayString(null, "value", INSENSITIVE), is("<default> (value)"));
      }

      @Test
      void shouldReturnUnsetWhenInsensitiveAndDefaultValueIsNull() {
        assertThat(toDisplayString(null, null, INSENSITIVE), is("<unset>"));
      }

      @Test
      void shouldReturnMaskedDefaultValueWhenSenstiveAndDefaultValueIsNotNull() {
        assertThat(toDisplayString(null, "value", SENSITIVE), is("<default> (*****)"));
      }

      @Test
      void shouldReturnUnsetWhenSensitiveAndDefaultValueIsNull() {
        assertThat(toDisplayString(null, null, SENSITIVE), is("<unset>"));
      }
    }

    @Nested
    final class WhenValueEqualsDefaultValue {
      @Test
      void shouldReturnDefaultValueWhenInsensitive() {
        assertThat(toDisplayString("value", "value", INSENSITIVE), is("<default> (value)"));
      }

      @Test
      void shouldReturnMaskedDefaultValueWhenSenstive() {
        assertThat(toDisplayString("value", "value", SENSITIVE), is("<default> (*****)"));
      }
    }

    @Nested
    final class WhenValueTypeIsString {
      @Test
      void shouldReturnValueWhenInsensitive() {
        assertThat(toDisplayString("value", null, INSENSITIVE), is("value"));
      }

      @Test
      void shouldReturnMaskedValueWhenSenstive() {
        assertThat(toDisplayString("value", null, SENSITIVE), is("*****"));
      }
    }

    @Nested
    final class WhenValueTypeIsCharArray {
      @Test
      void shouldReturnValueWhenInsensitive() {
        assertThat(
            toDisplayString(new char[] {'v', 'a', 'l', 'u', 'e'}, null, INSENSITIVE), is("value"));
      }

      @Test
      void shouldReturnMaskedValueWhenSenstive() {
        assertThat(
            toDisplayString(new char[] {'v', 'a', 'l', 'u', 'e'}, null, SENSITIVE), is("*****"));
      }
    }

    @Nested
    final class WhenValueTypeIsOther {
      @Test
      void shouldReturnValueWhenInsensitive() {
        assertThat(toDisplayString(42, null, INSENSITIVE), is("42"));
      }

      @Test
      void shouldReturnMaskedValueWhenSenstive() {
        assertThat(toDisplayString(42, null, SENSITIVE), is("**"));
      }
    }
  }

  private interface TestData {
    String fakeValue = "testing fake";
  }
}
