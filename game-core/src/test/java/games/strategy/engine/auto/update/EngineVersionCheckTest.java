package games.strategy.engine.auto.update;

import static games.strategy.engine.auto.update.EngineVersionCheck.formatUpdateCheckDate;
import static games.strategy.engine.auto.update.EngineVersionCheck.parseUpdateCheckDate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.triplea.settings.GameSetting;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

final class EngineVersionCheckTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  final class IsEngineUpdateCheckRequiredTest {
    private final LocalDate now = LocalDate.of(2008, 6, 1);
    @Mock private GameSetting<Boolean> firstRunSetting;
    @Mock private GameSetting<String> updateCheckDateSetting;
    @Mock private Runnable flushSetting;

    private void givenFirstRun() {
      when(firstRunSetting.getValueOrThrow()).thenReturn(true);
    }

    private void givenNotFirstRun() {
      when(firstRunSetting.getValueOrThrow()).thenReturn(false);
    }

    private void givenEngineUpdateCheckNeverRun() {
      when(updateCheckDateSetting.getValue()).thenReturn(Optional.empty());
    }

    private void givenEngineUpdateCheckLastRunRelativeToNow(
        final long amountToAdd, final TemporalUnit unit) {
      when(updateCheckDateSetting.getValue())
          .thenReturn(Optional.of(formatUpdateCheckDate(now.plus(amountToAdd, unit))));
    }

    private boolean whenIsEngineUpdateCheckRequired() {
      return EngineVersionCheck.isEngineUpdateCheckRequired(
          now, firstRunSetting, updateCheckDateSetting, flushSetting);
    }

    @Test
    void shouldReturnTrueWhenFirstRun() {
      givenFirstRun();

      assertThat(whenIsEngineUpdateCheckRequired(), is(true));
    }

    @Test
    void shouldReturnTrueWhenNotFirstRunAndEngineUpdateCheckLastRunOneYearAgo() {
      givenNotFirstRun();
      givenEngineUpdateCheckLastRunRelativeToNow(-1, ChronoUnit.YEARS);

      assertThat(whenIsEngineUpdateCheckRequired(), is(true));
    }

    @Test
    void shouldReturnTrueWhenNotFirstRunAndEngineUpdateCheckLastRunTwoDaysAgo() {
      givenNotFirstRun();
      givenEngineUpdateCheckLastRunRelativeToNow(-2, ChronoUnit.DAYS);

      assertThat(whenIsEngineUpdateCheckRequired(), is(true));
    }

    @Test
    void shouldReturnTrueWhenNotFirstRunAndEngineUpdateCheckNeverRun() {
      givenNotFirstRun();
      givenEngineUpdateCheckNeverRun();

      assertThat(whenIsEngineUpdateCheckRequired(), is(true));
    }

    @Test
    void shouldSaveAndFlushLastUpdateCheckDateSettingWhenReturnsTrue() {
      givenFirstRun();

      assertThat(whenIsEngineUpdateCheckRequired(), is(true));
      verify(updateCheckDateSetting).setValue(formatUpdateCheckDate(now));
      verify(flushSetting).run();
    }

    @Test
    void shouldReturnFalseWhenNotFirstRunAndEngineUpdateCheckLastRunOneDayAgo() {
      givenNotFirstRun();
      givenEngineUpdateCheckLastRunRelativeToNow(-1, ChronoUnit.DAYS);

      assertThat(whenIsEngineUpdateCheckRequired(), is(false));
    }

    @Test
    void shouldReturnFalseWhenNotFirstRunAndEngineUpdateCheckLastRunToday() {
      givenNotFirstRun();
      givenEngineUpdateCheckLastRunRelativeToNow(0, ChronoUnit.DAYS);

      assertThat(whenIsEngineUpdateCheckRequired(), is(false));
    }

    @Test
    void shouldReturnFalseWhenNotFirstRunAndEngineUpdateCheckLastRunOneDayHence() {
      givenNotFirstRun();
      givenEngineUpdateCheckLastRunRelativeToNow(1, ChronoUnit.DAYS);

      assertThat(whenIsEngineUpdateCheckRequired(), is(false));
    }

    @Test
    void shouldNotSaveAndFlushLastUpdateCheckDateSettingWhenReturnsFalse() {
      givenNotFirstRun();
      givenEngineUpdateCheckLastRunRelativeToNow(0, ChronoUnit.DAYS);

      assertThat(whenIsEngineUpdateCheckRequired(), is(false));
      verify(updateCheckDateSetting, never()).setValue(anyString());
      verify(flushSetting, never()).run();
    }
  }

  @Nested
  final class ParseUpdateCheckDateTest {
    @Test
    void shouldParseStringAsDate() {
      assertThat(parseUpdateCheckDate("2018:1"), is(LocalDate.ofYearDay(2018, 1)));
      assertThat(parseUpdateCheckDate("1941:365"), is(LocalDate.ofYearDay(1941, 365)));
    }
  }

  @Nested
  final class FormatUpdateCheckDateTest {
    @Test
    void shouldFormatDateAsString() {
      assertThat(formatUpdateCheckDate(LocalDate.ofYearDay(2018, 1)), is("2018:1"));
      assertThat(formatUpdateCheckDate(LocalDate.ofYearDay(1941, 365)), is("1941:365"));
    }
  }
}
