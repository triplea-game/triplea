package games.strategy.triplea.settings;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.core.Is;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResetFunctionTest {

  @Mock
  private UiBinding mockBinding;

  @Mock
  private UiBinding mockBinding2;

  @Test
  public void resetSettings() throws Exception {

    final AtomicInteger callCount = new AtomicInteger(0);

    ResetFunction.resetSettings(Arrays.asList(mockBinding, mockBinding2), callCount::incrementAndGet);

    assertThat(callCount.get(), Is.is(1));

    Mockito.verify(mockBinding, Mockito.times(1)).resetToDefault();
    Mockito.verify(mockBinding2, Mockito.times(1)).resetToDefault();
  }
}
