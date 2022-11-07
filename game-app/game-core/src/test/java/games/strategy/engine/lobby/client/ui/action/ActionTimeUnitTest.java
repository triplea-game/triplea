package games.strategy.engine.lobby.client.ui.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ActionTimeUnitTest {
  @Test
  void testIntervalSizesBecomeBigger() {
    final long minute = ActionTimeUnit.MINUTES.toMinutes(1);
    final long hour = ActionTimeUnit.HOURS.toMinutes(1);
    final long day = ActionTimeUnit.DAYS.toMinutes(1);
    final long week = ActionTimeUnit.WEEKS.toMinutes(1);
    final long month = ActionTimeUnit.MONTHS.toMinutes(1);

    assertTrue(minute < hour);
    assertTrue(hour < day);
    assertTrue(day < week);
    assertTrue(week < month);
  }

  @Test
  void verifyMinutes() {
    assertEquals(1, ActionTimeUnit.MINUTES.toMinutes(1));
    assertEquals(2, ActionTimeUnit.MINUTES.toMinutes(2));
    assertEquals(10, ActionTimeUnit.MINUTES.toMinutes(10));
  }

  @Test
  void verifyHours() {
    assertEquals(60, ActionTimeUnit.HOURS.toMinutes(1));
    assertEquals(120, ActionTimeUnit.HOURS.toMinutes(2));
    assertEquals(600, ActionTimeUnit.HOURS.toMinutes(10));
  }

  @Test
  void verifyDays() {
    assertEquals(24 * 60, ActionTimeUnit.DAYS.toMinutes(1));
    assertEquals(24 * 60 * 2, ActionTimeUnit.DAYS.toMinutes(2));
    assertEquals(24 * 60 * 10, ActionTimeUnit.DAYS.toMinutes(10));
  }

  @Test
  void verifyWeeks() {
    assertEquals(7 * 24 * 60, ActionTimeUnit.WEEKS.toMinutes(1));
    assertEquals(7 * 24 * 60 * 2, ActionTimeUnit.WEEKS.toMinutes(2));
    assertEquals(7 * 24 * 60 * 10, ActionTimeUnit.WEEKS.toMinutes(10));
  }

  @Test
  void verifyMonths() {
    assertEquals(30 * 24 * 60, ActionTimeUnit.MONTHS.toMinutes(1));
    assertEquals(30 * 24 * 60 * 2, ActionTimeUnit.MONTHS.toMinutes(2));
    assertEquals(30 * 24 * 60 * 10, ActionTimeUnit.MONTHS.toMinutes(10));
  }
}
