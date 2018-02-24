package games.strategy.triplea.attachments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameParseException;

public class TechAbilityAttachmentTest {

  private final TechAbilityAttachment attachment = spy(new TechAbilityAttachment("", null, null));
  private final String name = "Test Name";
  private final String customToString = "CustomToString";

  @BeforeEach
  public void setup() {
    when(attachment.toString()).thenReturn(customToString);
  }

  @Test
  public void testSplitAndValidate_emptyString() {
    final Exception e = assertThrows(GameParseException.class, () -> attachment.splitAndValidate(name, ""));
    assertTrue(e.getMessage().contains(name));
    assertTrue(e.getMessage().contains(customToString));
  }

  @Test
  public void testSplitAndValidate_invalidLength() {
    final Exception e = assertThrows(GameParseException.class, () -> attachment.splitAndValidate(name, "a:b:c"));
    assertTrue(e.getMessage().contains(name));
    assertTrue(e.getMessage().contains(customToString));
  }

  @Test
  public void testSplitAndValidate_oneValue() throws Exception {
    final String[] result = attachment.splitAndValidate(name, "a");
    assertEquals(1, result.length);
    assertEquals("a", result[0]);
  }


  @Test
  public void testSplitAndValidate_twoValues() throws Exception {
    final String[] result = attachment.splitAndValidate(name, "a:b");
    assertEquals(2, result.length);
    assertEquals("a", result[0]);
    assertEquals("b", result[1]);
  }


  @Test
  public void testGetIntInRange_noInt() {
    final Exception e1 = assertThrows(IllegalArgumentException.class,
        () -> attachment.getIntInRange(name, "NaN", 10, false));
    final Exception e2 = assertThrows(IllegalArgumentException.class,
        () -> attachment.getIntInRange(name, "NaN", -10, true));
    assertTrue(e1.getCause() instanceof NumberFormatException);
    assertTrue(e2.getCause() instanceof NumberFormatException);
    assertTrue(e1.getMessage().contains("NaN"));
    assertTrue(e2.getMessage().contains("NaN"));
  }

  @Test
  public void testGetIntInRange_tooHigh() {
    final Exception e = assertThrows(GameParseException.class, () -> attachment.getIntInRange(name, "20", 10, false));
    assertTrue(e.getMessage().contains("20"));
    assertTrue(e.getMessage().contains("10"));
    assertTrue(e.getMessage().contains(name));
    assertTrue(e.getMessage().contains(customToString));
  }

  @Test
  public void testGetIntInRange_negativeNoUndefined() {
    final Exception e = assertThrows(GameParseException.class, () -> attachment.getIntInRange(name, "-1", 10, false));
    assertTrue(e.getMessage().contains("-1"));
    assertTrue(e.getMessage().contains("10"));
    assertTrue(e.getMessage().contains(name));
    assertTrue(e.getMessage().contains(customToString));
  }

  @Test
  public void testGetIntInRange_negativeUndefined() {
    final Exception e = assertThrows(GameParseException.class, () -> attachment.getIntInRange(name, "-2", 10, true));
    assertTrue(e.getMessage().contains("-2"));
    assertTrue(e.getMessage().contains("10"));
    assertTrue(e.getMessage().contains(name));
    assertTrue(e.getMessage().contains(customToString));
  }


  @Test
  public void testGetIntInRange_validValues() throws Exception {
    assertEquals(-1, attachment.getIntInRange(name, "-1", 10, true));
    assertEquals(10, attachment.getIntInRange(name, "10", 10, true));
    assertEquals(0, attachment.getIntInRange(name, "0", 10, false));
    assertEquals(10, attachment.getIntInRange(name, "10", 10, false));
  }
}
