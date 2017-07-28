package games.strategy.triplea.settings;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JOptionPane;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class SaveFunctionTest {

  @Mock
  private UiBinding mockBinding;

  @Mock
  private UiBinding mockBinding2;

  @Mock
  private GameSetting mockSetting;

  @Test
  public void messageOnValidIsInformation() throws Exception {
    givenValidationResults(true, true);
    final SaveFunction.SaveResult result = SaveFunction.saveSettings(
        Arrays.asList(mockBinding, mockBinding2), () -> {
        });

    MatcherAssert.assertThat("There will always be a message back to the user",
        result.message.isEmpty(), Is.is(false));
    MatcherAssert.assertThat("All valid, message type should informational",
        result.dialogType, Is.is(JOptionPane.INFORMATION_MESSAGE));
  }

  private void givenValidationResults(final boolean first, final boolean second) {
    Mockito.when(mockBinding.isValid()).thenReturn(first);
    Mockito.when(mockBinding.readValues()).thenReturn(ImmutableMap.of(mockSetting, TestData.fakeValue));
    Mockito.when(mockSetting.value()).thenReturn("");
    Mockito.when(mockBinding2.isValid()).thenReturn(second);
    Mockito.when(mockBinding2.readValues()).thenReturn(ImmutableMap.of());
  }

  @Test
  public void messageOnNotValidResultIsWarning() throws Exception {
    givenValidationResults(false, false);

    final SaveFunction.SaveResult result = SaveFunction.saveSettings(
        Arrays.asList(mockBinding, mockBinding2), () -> {
        });

    MatcherAssert.assertThat(result.message.isEmpty(), Is.is(false));
    MatcherAssert.assertThat(result.dialogType, Is.is(JOptionPane.WARNING_MESSAGE));
  }

  @Test
  public void messageOnMixedResultIsWarning() throws Exception {
    givenValidationResults(true, false);

    final SaveFunction.SaveResult result = SaveFunction.saveSettings(
        Arrays.asList(mockBinding, mockBinding2), () -> {
        });

    MatcherAssert.assertThat(result.message.isEmpty(), Is.is(false));
    MatcherAssert.assertThat("At least one value was not updated, should be warning message type",
        result.dialogType, Is.is(JOptionPane.WARNING_MESSAGE));
  }

  @Test
  public void valueSavedWhenValid() throws Exception {
    final AtomicInteger callCount = new AtomicInteger(0);

    Mockito.when(mockBinding.isValid()).thenReturn(true);
    Mockito.when(mockBinding.readValues()).thenReturn(ImmutableMap.of(mockSetting, TestData.fakeValue));
    Mockito.when(mockSetting.value()).thenReturn("");

    Mockito.when(mockBinding2.isValid()).thenReturn(false);

    SaveFunction.saveSettings(Arrays.asList(mockBinding, mockBinding2), callCount::incrementAndGet);

    MatcherAssert.assertThat("Make sure we flushed! A call count of '1' means our runnable was called,"
        + "which should only happen when settings were successfully saved.",
        callCount.get(), Is.is(1));

    Mockito.verify(mockSetting, Mockito.times(1)).save(TestData.fakeValue);
  }

  @Test
  public void noSettingsSavedIfAllInvalid() throws Exception {
    final AtomicInteger callCount = new AtomicInteger(0);

    Mockito.when(mockBinding.isValid()).thenReturn(false);

    SaveFunction.saveSettings(Collections.singletonList(mockBinding), callCount::incrementAndGet);

    MatcherAssert.assertThat("The one setting value was not valid, nothing should be saved, our "
        + "increment value runnable should not have been called, callCount should still be at zero.",
        callCount.get(), Is.is(0));
  }

  private interface TestData {
    String fakeValue = "testing fake";
  }
}
