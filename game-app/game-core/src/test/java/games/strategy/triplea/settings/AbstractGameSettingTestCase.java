package games.strategy.triplea.settings;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
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
      assertThat(
          newGameSetting(NO_VALUE, DEFAULT_VALUE).getDefaultValue(), isPresentAndIs(DEFAULT_VALUE));
    }

    @Test
    void shouldReturnEmptyWhenDefaultValueAbsent() {
      assertThat(newGameSetting(NO_VALUE, NO_VALUE).getDefaultValue(), isEmpty());
    }
  }

  @Nested
  final class GetValueTest {
    @Test
    void shouldReturnValueWhenValuePresentAndDefaultValuePresent() {
      assertThat(newGameSetting(VALUE, DEFAULT_VALUE).getValue(), isPresentAndIs(VALUE));
    }

    @Test
    void shouldReturnValueWhenValuePresentAndDefaultValueAbsent() {
      assertThat(newGameSetting(VALUE, NO_VALUE).getValue(), isPresentAndIs(VALUE));
    }

    @Test
    void shouldReturnDefaultValueWhenValueAbsentAndDefaultValuePresent() {
      assertThat(newGameSetting(NO_VALUE, DEFAULT_VALUE).getValue(), isPresentAndIs(DEFAULT_VALUE));
    }

    @Test
    void shouldReturnEmptyWhenValueAbsentAndDefaultValueAbsent() {
      assertThat(newGameSetting(NO_VALUE, NO_VALUE).getValue(), isEmpty());
    }
  }

  @Nested
  final class GetValueOrThrowTest {
    @Test
    void shouldReturnValueWhenValuePresentAndDefaultValuePresent() {
      assertThat(newGameSetting(VALUE, DEFAULT_VALUE).getValueOrThrow(), is(VALUE));
    }

    @Test
    void shouldReturnValueWhenValuePresentAndDefaultValueAbsent() {
      assertThat(newGameSetting(VALUE, NO_VALUE).getValueOrThrow(), is(VALUE));
    }

    @Test
    void shouldReturnDefaultValueWhenValueAbsentAndDefaultValuePresent() {
      assertThat(newGameSetting(NO_VALUE, DEFAULT_VALUE).getValueOrThrow(), is(DEFAULT_VALUE));
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
      assertThat(newGameSetting(VALUE, DEFAULT_VALUE).isSet(), is(true));
    }

    @Test
    void shouldReturnTrueWhenValuePresentAndDefaultValueAbsent() {
      assertThat(newGameSetting(VALUE, NO_VALUE).isSet(), is(true));
    }

    @Test
    void shouldReturnTrueWhenValueAbsentAndDefaultValuePresent() {
      assertThat(newGameSetting(NO_VALUE, DEFAULT_VALUE).isSet(), is(true));
    }

    @Test
    void shouldReturnFalseWhenValueAbsentAndDefaultValueAbsent() {
      assertThat(newGameSetting(NO_VALUE, NO_VALUE).isSet(), is(false));
    }
  }

  @Nested
  final class ResetValueTest {
    @Test
    void shouldSetValueToDefaultValueWhenDefaultValuePresent() {
      final GameSetting<Integer> gameSetting = newGameSetting(VALUE, DEFAULT_VALUE);

      gameSetting.resetValue();

      assertThat(gameSetting.getValue(), isPresentAndIs(DEFAULT_VALUE));
    }

    @Test
    void shouldSetValueToEmptyWhenDefaultValueAbsent() {
      final GameSetting<Integer> gameSetting = newGameSetting(VALUE, NO_VALUE);

      gameSetting.resetValue();

      assertThat(gameSetting.getValue(), isEmpty());
    }
  }

  @Nested
  final class SetValueTest {
    @Test
    void shouldResetValueWhenValueIsNull() {
      final GameSetting<Integer> gameSetting = newGameSetting(VALUE, DEFAULT_VALUE);

      gameSetting.setValue(NO_VALUE);

      assertThat(gameSetting.getValue(), isPresentAndIs(DEFAULT_VALUE));
    }

    @Test
    void shouldSetValueWhenValueIsNonNull() {
      final GameSetting<Integer> gameSetting = newGameSetting(VALUE, DEFAULT_VALUE);

      gameSetting.setValue(OTHER_VALUE);

      assertThat(gameSetting.getValue(), isPresentAndIs(OTHER_VALUE));
    }
  }
}
