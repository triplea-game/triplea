package games.strategy.engine.lobby.client.ui;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import games.strategy.engine.lobby.client.ui.TimespanDialog.TimeUnit;
import games.strategy.engine.lobby.client.ui.TimespanDialog.Timespan;

public class TimespanDialogTest {

  @Test
  public void testForeverReturnsNull() {
    assertNull(TimeUnit.FOREVER.getInstant(Integer.MAX_VALUE));
    assertNull(TimeUnit.FOREVER.getInstant(0));
    assertNull(TimeUnit.FOREVER.getInstant(Integer.MIN_VALUE));
  }

  @Test
  public void testIntervalSizesBecomeBigger() {
    final Instant minute = TimeUnit.MINUTES.getInstant(1);
    final Instant hour = TimeUnit.HOURS.getInstant(1);
    final Instant day = TimeUnit.DAYS.getInstant(1);
    final Instant week = TimeUnit.WEEKS.getInstant(1);
    final Instant month = TimeUnit.MONTHS.getInstant(1);
    final Instant year = TimeUnit.YEARS.getInstant(1);

    assertTrue(minute.isBefore(hour));
    assertTrue(hour.isBefore(day));
    assertTrue(day.isBefore(week));
    assertTrue(week.isBefore(month));
    assertTrue(month.isBefore(year));
  }

  @Test
  public void testPositiveIntIsInFuture() {
    assertTrue(Instant.now().isBefore(TimeUnit.MINUTES.getInstant(1)));
  }

  @Test
  public void testCancelDoesExecuteNothing() {
    TimespanDialog.runAction(d -> fail("Operation was not cancelled!"), Optional.empty());
  }

  @Test
  public void testNonNullDateInTheFuture() {
    // We can't use Integer#MAX_VALUE for years, because this will result in a long-overflow
    // So we just limit the amount for every time unit
    Arrays.asList(
        TimeUnit.MINUTES,
        TimeUnit.HOURS,
        TimeUnit.DAYS,
        TimeUnit.WEEKS,
        TimeUnit.MONTHS,
        TimeUnit.YEARS)
        .forEach(timeUnit -> {
          TimespanDialog.runAction(
              d -> assertTrue(d.after(new Date())),
              Optional.of(new Timespan(TimespanDialog.MAX_DURATION, timeUnit)));
        });
  }

  @Test
  public void testForeverPassesNull() {
    TimespanDialog.runAction(d -> assertNull(d), Optional.of(new Timespan(0, TimeUnit.FOREVER)));
  }
}
