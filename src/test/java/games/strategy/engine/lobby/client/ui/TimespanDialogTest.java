package games.strategy.engine.lobby.client.ui;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.util.Date;

import javax.swing.JOptionPane;

import org.junit.jupiter.api.Test;

import games.strategy.engine.lobby.client.ui.TimespanDialog.TimeUnit;

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
    TimespanDialog.runAction(d -> fail("Operation was not cancelled!"), JOptionPane.CANCEL_OPTION, TimeUnit.FOREVER, 0);
  }

  @Test
  public void testNonNullDateInTheFuture() {
    // We can't use Integer#MAX_VALUE for years, because this will result in a long-overflow
    // So we just limit the amount for every time unit
    TimespanDialog.runAction(d -> assertTrue(d.after(new Date())),
        JOptionPane.OK_OPTION, TimeUnit.MINUTES, 99999999);
    TimespanDialog.runAction(d -> assertTrue(d.after(new Date())),
        JOptionPane.OK_OPTION, TimeUnit.HOURS, 99999999);
    TimespanDialog.runAction(d -> assertTrue(d.after(new Date())),
        JOptionPane.OK_OPTION, TimeUnit.DAYS, 99999999);
    TimespanDialog.runAction(d -> assertTrue(d.after(new Date())),
        JOptionPane.OK_OPTION, TimeUnit.WEEKS, 99999999);
    TimespanDialog.runAction(d -> assertTrue(d.after(new Date())),
        JOptionPane.OK_OPTION, TimeUnit.MONTHS, 99999999);
    TimespanDialog.runAction(d -> assertTrue(d.after(new Date())),
        JOptionPane.OK_OPTION, TimeUnit.YEARS, 99999999);
  }

  @Test
  public void testForeverPassesNull() {
    TimespanDialog.runAction(d -> assertNull(d), JOptionPane.OK_OPTION, TimeUnit.FOREVER, 0);
  }
}
