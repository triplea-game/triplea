package games.strategy.triplea.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test fixture that verifies implementations of {@link GameSetting} obey the general contract of
 * the interface.
 */
public abstract class AbstractGameSettingTestCase {
  private static final @Nullable Integer NO_VALUE = null;
  private static final Integer DEFAULT_VALUE = 0;
  private static final Integer VALUE = 42;
  private static final Integer OTHER_VALUE = 2112;

  protected AbstractGameSettingTestCase() {}

  /**
   * Returns a new game setting with the specified current and default values.
   *
   * @param value The current value or {@code null} if no current value.
   * @param defaultValue The default value or {@code null} if no default value.
   */
  protected abstract GameSetting<Integer> newGameSetting(
      @Nullable Integer value, @Nullable Integer defaultValue);

  @Nested
  final class GetDefaultValueTest {
    @Test
    void shouldReturnDefaultValueWhenDefaultValuePresent() {
      assertThat(newGameSetting(NO_VALUE, DEFAULT_VALUE).getDefaultValue()).contains(DEFAULT_VALUE);
    }

    @Test
    void shouldReturnEmptyWhenDefaultValueAbsent() {
      assertThat(newGameSetting(NO_VALUE, NO_VALUE).getDefaultValue()).isEmpty();
    }
  }

  @Nested
  final class GetValueTest {
    @Test
    void shouldReturnValueWhenValuePresentAndDefaultValuePresent() {
      assertThat(newGameSetting(VALUE, DEFAULT_VALUE).getValue()).contains(VALUE);
    }

    @Test
    void shouldReturnValueWhenValuePresentAndDefaultValueAbsent() {
      assertThat(newGameSetting(VALUE, NO_VALUE).getValue()).contains(VALUE);
    }

    @Test
    void shouldReturnDefaultValueWhenValueAbsentAndDefaultValuePresent() {
      assertThat(newGameSetting(NO_VALUE, DEFAULT_VALUE).getValue()).contains(DEFAULT_VALUE);
    }

    @Test
    void shouldReturnEmptyWhenValueAbsentAndDefaultValueAbsent() {
      assertThat(newGameSetting(NO_VALUE, NO_VALUE).getValue()).isEmpty();
    }
  }

  @Nested
  final class GetValueOrThrowTest {
    @Test
    void shouldReturnValueWhenValuePresentAndDefaultValuePresent() {
      assertThat(newGameSetting(VALUE, DEFAULT_VALUE).getValueOrThrow()).isEqualTo(VALUE);
    }

    @Test
    void shouldReturnValueWhenValuePresentAndDefaultValueAbsent() {
      assertThat(newGameSetting(VALUE, NO_VALUE).getValueOrThrow()).isEqualTo(VALUE);
    }

    @Test
    void shouldReturnDefaultValueWhenValueAbsentAndDefaultValuePresent() {
      assertThat(newGameSetting(NO_VALUE, DEFAULT_VALUE).getValueOrThrow())
          .isEqualTo(DEFAULT_VALUE);
    }

    @Test
    void shouldThrowExceptionWhenValueAbsentAndDefaultValueAbsent() {
      assertThrows(
          NoSuchElementException.class, () -> newGameSetting(NO_VALUE, NO_VALUE).getValueOrThrow());
    }
  }

  @Nested
  final class IsSetTest {
    @Test
    void shouldReturnTrueWhenValuePresentAndDefaultValuePresent() {
      assertThat(newGameSetting(VALUE, DEFAULT_VALUE).isSet()).isTrue();
    }

    @Test
    void shouldReturnTrueWhenValuePresentAndDefaultValueAbsent() {
      assertThat(newGameSetting(VALUE, NO_VALUE).isSet()).isTrue();
    }

    @Test
    void shouldReturnTrueWhenValueAbsentAndDefaultValuePresent() {
      assertThat(newGameSetting(NO_VALUE, DEFAULT_VALUE).isSet()).isTrue();
    }

    @Test
    void shouldReturnFalseWhenValueAbsentAndDefaultValueAbsent() {
      assertThat(newGameSetting(NO_VALUE, NO_VALUE).isSet()).isFalse();
    }
  }

  @Nested
  final class ResetValueTest {
    @Test
    void shouldSetValueToDefaultValueWhenDefaultValuePresent() {
      final GameSetting<Integer> gameSetting = newGameSetting(VALUE, DEFAULT_VALUE);

      gameSetting.resetValue();

      assertThat(gameSetting.getValue()).contains(DEFAULT_VALUE);
    }

    @Test
    void shouldSetValueToEmptyWhenDefaultValueAbsent() {
      final GameSetting<Integer> gameSetting = newGameSetting(VALUE, NO_VALUE);

      gameSetting.resetValue();

      assertThat(gameSetting.getValue()).isEmpty();
    }
  }

  @Nested
  final class SetValueTest {
    @Test
    void shouldResetValueWhenValueIsNull() {
      final GameSetting<Integer> gameSetting = newGameSetting(VALUE, DEFAULT_VALUE);

      gameSetting.setValue(NO_VALUE);

      assertThat(gameSetting.getValue()).contains(DEFAULT_VALUE);
    }

    @Test
    void shouldSetValueWhenValueIsNonNull() {
      final GameSetting<Integer> gameSetting = newGameSetting(VALUE, DEFAULT_VALUE);

      gameSetting.setValue(OTHER_VALUE);

      assertThat(gameSetting.getValue()).contains(OTHER_VALUE);
    }
  }
}
