package games.strategy.engine;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import games.strategy.triplea.settings.GameSetting;

@RunWith(Enclosed.class)
public final class ClientFileSystemHelperTests {
  @RunWith(MockitoJUnitRunner.StrictStubs.class)
  public static final class GetUserMapsFolderPathTest {
    @Mock
    private GameSetting currentSetting;

    @Mock
    private GameSetting overrideSetting;

    private String getUserMapsFolderPath() {
      return ClientFileSystemHelper.getUserMapsFolderPath(currentSetting, overrideSetting);
    }

    @Test
    public void shouldReturnCurrentPathWhenOverridePathNotSet() {
      when(overrideSetting.isSet()).thenReturn(false);
      final String currentPath = "/path/to/current";
      when(currentSetting.value()).thenReturn(currentPath);

      assertThat(getUserMapsFolderPath(), is(currentPath));
    }

    @Test
    public void shouldReturnOverridePathWhenOverridePathSet() {
      when(overrideSetting.isSet()).thenReturn(true);
      final String overridePath = "/path/to/override";
      when(overrideSetting.value()).thenReturn(overridePath);

      assertThat(getUserMapsFolderPath(), is(overridePath));
    }
  }
}
