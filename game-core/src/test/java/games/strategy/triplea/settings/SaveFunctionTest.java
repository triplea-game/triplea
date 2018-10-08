package games.strategy.triplea.settings;

import static org.hamcrest.core.Is.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Runnables;

@ExtendWith(MockitoExtension.class)
public class SaveFunctionTest {

  @Mock
  private SelectionComponent<JComponent> mockSelectionComponent;

  @Mock
  private SelectionComponent<JComponent> mockSelectionComponent2;

  @Mock
  private GameSetting<String> mockSetting;

  @Test
  public void messageOnValidIsInformation() {
    givenValidationResults(true, true);
    final SaveFunction.SaveResult result = SaveFunction.saveSettings(
        Arrays.asList(mockSelectionComponent, mockSelectionComponent2), Runnables.doNothing());

    MatcherAssert.assertThat("There will always be a message back to the user",
        result.message.isEmpty(), is(false));
    MatcherAssert.assertThat("All valid, message type should informational",
        result.dialogType, is(JOptionPane.INFORMATION_MESSAGE));
  }

  private void givenValidationResults(final boolean first, final boolean second) {
    Mockito.when(mockSelectionComponent.isValid()).thenReturn(first);
    Mockito.when(mockSelectionComponent.readValues()).thenReturn(ImmutableMap.of(mockSetting, TestData.fakeValue));
    if (first) {
      Mockito.when(mockSetting.value()).thenReturn("");
    }
    Mockito.when(mockSelectionComponent2.isValid()).thenReturn(second);
    Mockito.when(mockSelectionComponent2.readValues()).thenReturn(ImmutableMap.of(mockSetting, "abc"));
  }

  @Test
  public void messageOnNotValidResultIsWarning() {
    givenValidationResults(false, false);

    final SaveFunction.SaveResult result = SaveFunction.saveSettings(
        Arrays.asList(mockSelectionComponent, mockSelectionComponent2), Runnables.doNothing());

    MatcherAssert.assertThat(result.message.isEmpty(), is(false));
    MatcherAssert.assertThat(result.dialogType, is(JOptionPane.WARNING_MESSAGE));
  }

  @Test
  public void messageOnMixedResultIsWarning() {
    givenValidationResults(true, false);

    final SaveFunction.SaveResult result = SaveFunction.saveSettings(
        Arrays.asList(mockSelectionComponent, mockSelectionComponent2), Runnables.doNothing());

    MatcherAssert.assertThat(result.message.isEmpty(), is(false));
    MatcherAssert.assertThat("At least one value was not updated, should be warning message type",
        result.dialogType, is(JOptionPane.WARNING_MESSAGE));
  }

  @Test
  public void valueSavedWhenValid() {
    final AtomicInteger callCount = new AtomicInteger(0);

    Mockito.when(mockSelectionComponent.isValid()).thenReturn(true);
    Mockito.when(mockSelectionComponent.readValues()).thenReturn(ImmutableMap.of(mockSetting, TestData.fakeValue));
    Mockito.when(mockSetting.value()).thenReturn("");

    Mockito.when(mockSelectionComponent2.isValid()).thenReturn(false);

    SaveFunction.saveSettings(
        Arrays.asList(mockSelectionComponent, mockSelectionComponent2),
        callCount::incrementAndGet);

    MatcherAssert.assertThat("Make sure we flushed! A call count of '1' means our runnable was called,"
        + "which should only happen when settings were successfully saved.",
        callCount.get(), is(1));

    Mockito.verify(mockSetting, Mockito.times(1)).saveString(TestData.fakeValue);
  }

  @Test
  public void noSettingsSavedIfAllInvalid() {
    final AtomicInteger callCount = new AtomicInteger(0);

    Mockito.when(mockSelectionComponent.isValid()).thenReturn(false);

    SaveFunction.saveSettings(Collections.singletonList(mockSelectionComponent), callCount::incrementAndGet);

    MatcherAssert.assertThat("The one setting value was not valid, nothing should be saved, our "
        + "increment value runnable should not have been called, callCount should still be at zero.",
        callCount.get(), is(0));
  }

  private interface TestData {
    String fakeValue = "testing fake";
  }
}
