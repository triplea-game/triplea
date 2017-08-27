package games.strategy.engine.lobby.client.ui;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers.hasMessageThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.time.Instant;

import org.junit.Test;

import games.strategy.engine.lobby.client.ui.LobbyFrame.TimeUnitNames;

public final class LobbyFrameTest {
  private static final long SECONDS_PER_MINUTE = 60L;
  private static final long MINUTES_PER_HOUR = 60L;
  private static final long HOURS_PER_DAY = 24L;
  private static final long SECONDS_PER_DAY = SECONDS_PER_MINUTE * MINUTES_PER_HOUR * HOURS_PER_DAY;

  @Test
  public void addDuration_ShouldBeAbleToAddDurationInMinutes() {
    final long minutes = 2L;

    final Instant end = LobbyFrame.addDuration(Instant.EPOCH, minutes, TimeUnitNames.MINUTE);

    assertThat(end, is(Instant.ofEpochSecond(SECONDS_PER_MINUTE * minutes)));
  }

  @Test
  public void addDuration_ShouldBeAbleToAddDurationInHours() {
    final long hours = 2L;

    final Instant end = LobbyFrame.addDuration(Instant.EPOCH, hours, TimeUnitNames.HOUR);

    assertThat(end, is(Instant.ofEpochSecond(SECONDS_PER_MINUTE * MINUTES_PER_HOUR * hours)));
  }

  @Test
  public void addDuration_ShouldBeAbleToAddDurationInDays() {
    final long days = 2L;

    final Instant end = LobbyFrame.addDuration(Instant.EPOCH, days, TimeUnitNames.DAY);

    assertThat(end, is(Instant.ofEpochSecond(SECONDS_PER_DAY * days)));
  }

  @Test
  public void addDuration_ShouldBeAbleToAddDurationInWeeks() {
    final long daysPerWeek = 7L;
    final long weeks = 2L;

    final Instant end = LobbyFrame.addDuration(Instant.EPOCH, weeks, TimeUnitNames.WEEK);

    assertThat(end, is(Instant.ofEpochSecond(SECONDS_PER_DAY * daysPerWeek * weeks)));
  }

  @Test
  public void addDuration_ShouldBeAbleToAddDurationInMonths() {
    final long daysInJan1970 = 31L;
    final long daysInFeb1970 = 28L;
    final long months = 2L;

    final Instant end = LobbyFrame.addDuration(Instant.EPOCH, months, TimeUnitNames.MONTH);

    assertThat(end, is(Instant.ofEpochSecond(SECONDS_PER_DAY * (daysInJan1970 + daysInFeb1970))));
  }

  @Test
  public void addDuration_ShouldBeAbleToAddDurationInYears() {
    final long daysIn1970 = 365L;
    final long daysIn1971 = 365L;
    final long years = 2L;

    final Instant end = LobbyFrame.addDuration(Instant.EPOCH, years, TimeUnitNames.YEAR);

    assertThat(end, is(Instant.ofEpochSecond(SECONDS_PER_DAY * (daysIn1970 + daysIn1971))));
  }

  @Test
  public void addDuration_ShouldThrowExceptionWhenUnitNameIsUnknown() {
    catchException(() -> LobbyFrame.addDuration(Instant.EPOCH, 1L, "unknown"));

    assertThat(caughtException(), allOf(
        is(instanceOf(IllegalArgumentException.class)),
        hasMessageThat(containsString("unknown temporal unit name"))));
  }
}
