package games.strategy.triplea.settings;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

class GameSettingTest {

  @Test
  @SuppressWarnings("unchecked")
  void testGetFromSetting() {
    final GameSetting<char[]> setting = mock(GameSetting.class);
    final char[] testData = new char[]{'a', 'b', 'c'};
    when(setting.getValueOrThrow()).thenThrow(NoSuchElementException.class).thenReturn(testData);

    assertThrows(NoSuchElementException.class, () -> GameSetting.getFromSetting(setting));
    assertArrayEquals(new char[]{'a', 'b', 'c'}, testData);
    verify(setting).getValueOrThrow();

    assertEquals("abc", GameSetting.getFromSetting(setting));
    assertArrayEquals(new char[]{0, 0, 0}, testData);
    verify(setting, times(2)).getValueOrThrow();
  }
}
