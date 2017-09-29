package games.strategy.triplea.ui;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;

import org.junit.Test;

import games.strategy.triplea.ui.TimespanDialog.TimeUnit;

public class TimespanDialogTest {

  @Test
  public void testForeverReturnsNull() {
    assertNull(TimeUnit.FOREVER.getInstant((int) (Math.random() * Integer.MAX_VALUE)));
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
}
