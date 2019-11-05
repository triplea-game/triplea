package games.strategy.engine.lobby.client.ui.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BanTimeUnitTest {
  @Test
  void testIntervalSizesBecomeBigger() {
    final long minute = BanTimeUnit.MINUTES.toMinutes(1);
    final long hour = BanTimeUnit.HOURS.toMinutes(1);
    final long day = BanTimeUnit.DAYS.toMinutes(1);
    final long week = BanTimeUnit.WEEKS.toMinutes(1);
    final long month = BanTimeUnit.MONTHS.toMinutes(1);
    final long forever = BanTimeUnit.FOREVER.toMinutes(1);

    assertTrue(minute < hour);
    assertTrue(hour < day);
    assertTrue(day < week);
    assertTrue(week < month);
    assertTrue(month < forever);
  }

  @Test
  void verifyMinutes() {
    assertEquals(1, BanTimeUnit.MINUTES.toMinutes(1));
    assertEquals(2, BanTimeUnit.MINUTES.toMinutes(2));
    assertEquals(10, BanTimeUnit.MINUTES.toMinutes(10));
  }

  @Test
  void verifyHours() {
    assertEquals(60, BanTimeUnit.HOURS.toMinutes(1));
    assertEquals(120, BanTimeUnit.HOURS.toMinutes(2));
    assertEquals(600, BanTimeUnit.HOURS.toMinutes(10));
  }

  @Test
  void verifyDays() {
    assertEquals(24 * 60, BanTimeUnit.DAYS.toMinutes(1));
    assertEquals(24 * 60 * 2, BanTimeUnit.DAYS.toMinutes(2));
    assertEquals(24 * 60 * 10, BanTimeUnit.DAYS.toMinutes(10));
  }

  @Test
  void verifyWeeks() {
    assertEquals(7 * 24 * 60, BanTimeUnit.WEEKS.toMinutes(1));
    assertEquals(7 * 24 * 60 * 2, BanTimeUnit.WEEKS.toMinutes(2));
    assertEquals(7 * 24 * 60 * 10, BanTimeUnit.WEEKS.toMinutes(10));
  }

  @Test
  void verifyMonths() {
    assertEquals(30 * 24 * 60, BanTimeUnit.MONTHS.toMinutes(1));
    assertEquals(30 * 24 * 60 * 2, BanTimeUnit.MONTHS.toMinutes(2));
    assertEquals(30 * 24 * 60 * 10, BanTimeUnit.MONTHS.toMinutes(10));
  }

  @Test
  void verifyForever() {
    assertEquals(BanDurationDialog.MAX_DURATION, BanTimeUnit.FOREVER.toMinutes(1));
    assertEquals(BanDurationDialog.MAX_DURATION, BanTimeUnit.FOREVER.toMinutes(2));
    assertEquals(BanDurationDialog.MAX_DURATION, BanTimeUnit.FOREVER.toMinutes(10));
  }
}
