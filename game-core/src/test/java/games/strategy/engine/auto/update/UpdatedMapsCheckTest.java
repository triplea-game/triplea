package games.strategy.engine.auto.update;

import static games.strategy.engine.auto.update.UpdatedMapsCheck.formatUpdateCheckDate;
import static games.strategy.engine.auto.update.UpdatedMapsCheck.parseUpdateCheckDate;
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

final class UpdatedMapsCheckTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  final class IsMapUpdateCheckRequiredTest {
    private final LocalDate now = LocalDate.of(2008, 6, 1);
    @Mock private GameSetting<String> updateCheckDateSetting;
    @Mock private Runnable flushSetting;

    private void givenMapUpdateCheckNeverRun() {
      when(updateCheckDateSetting.getValue()).thenReturn(Optional.empty());
    }

    private void givenMapUpdateCheckLastRunRelativeToNow(
        final long amountToAdd, final TemporalUnit unit) {
      when(updateCheckDateSetting.getValue())
          .thenReturn(Optional.of(formatUpdateCheckDate(now.plus(amountToAdd, unit))));
    }

    private boolean whenIsMapUpdateCheckRequired() {
      return UpdatedMapsCheck.isMapUpdateCheckRequired(now, updateCheckDateSetting, flushSetting);
    }

    @Test
    void shouldReturnTrueWhenMapUpdateCheckLastRunOneYearAgo() {
      givenMapUpdateCheckLastRunRelativeToNow(-1, ChronoUnit.YEARS);

      assertThat(whenIsMapUpdateCheckRequired(), is(true));
    }

    @Test
    void shouldReturnTrueWhenMapUpdateCheckLastRunOneMonthAgo() {
      givenMapUpdateCheckLastRunRelativeToNow(-1, ChronoUnit.MONTHS);

      assertThat(whenIsMapUpdateCheckRequired(), is(true));
    }

    @Test
    void shouldReturnTrueWhenMapUpdateCheckNeverRun() {
      givenMapUpdateCheckNeverRun();

      assertThat(whenIsMapUpdateCheckRequired(), is(true));
    }

    @Test
    void shouldSaveAndFlushLastUpdateCheckDateSettingWhenReturnsTrue() {
      givenMapUpdateCheckLastRunRelativeToNow(-1, ChronoUnit.YEARS);

      assertThat(whenIsMapUpdateCheckRequired(), is(true));
      verify(updateCheckDateSetting).setValue(formatUpdateCheckDate(now));
      verify(flushSetting).run();
    }

    @Test
    void shouldReturnFalseWhenMapUpdateCheckLastRunThisMonth() {
      givenMapUpdateCheckLastRunRelativeToNow(0, ChronoUnit.MONTHS);

      assertThat(whenIsMapUpdateCheckRequired(), is(false));
    }

    @Test
    void shouldReturnFalseWhenMapUpdateCheckLastRunOneMonthHence() {
      givenMapUpdateCheckLastRunRelativeToNow(1, ChronoUnit.MONTHS);

      assertThat(whenIsMapUpdateCheckRequired(), is(false));
    }

    @Test
    void shouldNotSaveAndFlushLastUpdateCheckDateSettingWhenReturnsFalse() {
      givenMapUpdateCheckLastRunRelativeToNow(0, ChronoUnit.MONTHS);

      assertThat(whenIsMapUpdateCheckRequired(), is(false));
      verify(updateCheckDateSetting, never()).setValue(anyString());
      verify(flushSetting, never()).run();
    }
  }

  @Nested
  final class ParseUpdateCheckDateTest {
    @Test
    void shouldParseStringAsDate() {
      assertThat(parseUpdateCheckDate("2018:1"), is(LocalDate.of(2018, 1, 1)));
      assertThat(parseUpdateCheckDate("1941:12"), is(LocalDate.of(1941, 12, 1)));
    }
  }

  @Nested
  final class FormatUpdateCheckDateTest {
    @Test
    void shouldFormatDateAsString() {
      assertThat(formatUpdateCheckDate(LocalDate.of(2018, 1, 1)), is("2018:1"));
      assertThat(formatUpdateCheckDate(LocalDate.of(1941, 12, 1)), is("1941:12"));
    }
  }
}
