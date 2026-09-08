package games.strategy.triplea.settings;

import static games.strategy.triplea.settings.SaveFunction.toDisplayString;
import static games.strategy.triplea.settings.SelectionComponent.SaveContext.ValueSensitivity.INSENSITIVE;
import static games.strategy.triplea.settings.SelectionComponent.SaveContext.ValueSensitivity.SENSITIVE;
import static org.assertj.core.api.Assertions.assertThat;
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

      assertThat(result.message).as("There will always be a message back to the user").isNotEmpty();
      assertThat(result.dialogType)
          .as("All valid, message type should informational")
          .isEqualTo(JOptionPane.INFORMATION_MESSAGE);
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

      assertThat(result.message).isNotEmpty();
      assertThat(result.dialogType).isEqualTo(JOptionPane.WARNING_MESSAGE);
    }

    @Test
    void messageOnMixedResultIsWarning() {
      givenValidationResults(true, false);

      final SaveFunction.SaveResult result =
          SaveFunction.saveSettings(
              List.of(mockSelectionComponent, mockSelectionComponent2), Runnables.doNothing());

      assertThat(result.message).isNotEmpty();
      assertThat(result.dialogType)
          .as("At least one value was not updated, should be warning message type")
          .isEqualTo(JOptionPane.WARNING_MESSAGE);
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
        assertThat(toDisplayString(null, "value", INSENSITIVE)).isEqualTo("<default> (value)");
      }

      @Test
      void shouldReturnUnsetWhenInsensitiveAndDefaultValueIsNull() {
        assertThat(toDisplayString(null, null, INSENSITIVE)).isEqualTo("<unset>");
      }

      @Test
      void shouldReturnMaskedDefaultValueWhenSenstiveAndDefaultValueIsNotNull() {
        assertThat(toDisplayString(null, "value", SENSITIVE)).isEqualTo("<default> (*****)");
      }

      @Test
      void shouldReturnUnsetWhenSensitiveAndDefaultValueIsNull() {
        assertThat(toDisplayString(null, null, SENSITIVE)).isEqualTo("<unset>");
      }
    }

    @Nested
    final class WhenValueEqualsDefaultValue {
      @Test
      void shouldReturnDefaultValueWhenInsensitive() {
        assertThat(toDisplayString("value", "value", INSENSITIVE)).isEqualTo("<default> (value)");
      }

      @Test
      void shouldReturnMaskedDefaultValueWhenSenstive() {
        assertThat(toDisplayString("value", "value", SENSITIVE)).isEqualTo("<default> (*****)");
      }
    }

    @Nested
    final class WhenValueTypeIsString {
      @Test
      void shouldReturnValueWhenInsensitive() {
        assertThat(toDisplayString("value", null, INSENSITIVE)).isEqualTo("value");
      }

      @Test
      void shouldReturnMaskedValueWhenSenstive() {
        assertThat(toDisplayString("value", null, SENSITIVE)).isEqualTo("*****");
      }
    }

    @Nested
    final class WhenValueTypeIsCharArray {
      @Test
      void shouldReturnValueWhenInsensitive() {
        assertThat(toDisplayString(new char[] {'v', 'a', 'l', 'u', 'e'}, null, INSENSITIVE))
            .isEqualTo("value");
      }

      @Test
      void shouldReturnMaskedValueWhenSenstive() {
        assertThat(toDisplayString(new char[] {'v', 'a', 'l', 'u', 'e'}, null, SENSITIVE))
            .isEqualTo("*****");
      }
    }

    @Nested
    final class WhenValueTypeIsOther {
      @Test
      void shouldReturnValueWhenInsensitive() {
        assertThat(toDisplayString(42, null, INSENSITIVE)).isEqualTo("42");
      }

      @Test
      void shouldReturnMaskedValueWhenSenstive() {
        assertThat(toDisplayString(42, null, SENSITIVE)).isEqualTo("**");
      }
    }
  }

  private interface TestData {
    String fakeValue = "testing fake";
  }
}
