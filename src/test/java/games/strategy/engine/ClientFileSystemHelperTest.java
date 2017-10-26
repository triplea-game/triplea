package games.strategy.engine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import games.strategy.triplea.settings.GameSetting;

public final class ClientFileSystemHelperTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  public final class GetUserMapsFolderPathTest {
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
