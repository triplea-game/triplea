package games.strategy.triplea.attachments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameParseException;

public class TechAbilityAttachmentTest {

  private final TechAbilityAttachment attachment = spy(new TechAbilityAttachment("", null, null));
  private final String name = "Test Name";
  private final String customToString = "CustomToString";

  @Test
  public void testSplitAndValidate_emptyString() {
    when(attachment.toString()).thenReturn(customToString);
    final Exception e = assertThrows(GameParseException.class, () -> attachment.splitAndValidate(name, ""));
    assertTrue(e.getMessage().contains(name));
    assertTrue(e.getMessage().contains(customToString));
  }

  @Test
  public void testSplitAndValidate_invalidLength() {
    when(attachment.toString()).thenReturn(customToString);
    final Exception e = assertThrows(GameParseException.class, () -> attachment.splitAndValidate(name, "a:b:c"));
    assertTrue(e.getMessage().contains(name));
    assertTrue(e.getMessage().contains(customToString));
  }

  @Test
  public void testSplitAndValidate_oneValue() throws Exception {
    final String[] result = attachment.splitAndValidate(name, "a");
    assertEquals("a", result[0]);
    assertEquals(1, result.length);
  }


  @Test
  public void testSplitAndValidate_twoValues() throws Exception {
    final String[] result = attachment.splitAndValidate(name, "a:b");
    assertEquals("a", result[0]);
    assertEquals("b", result[1]);
    assertEquals(2, result.length);
  }
}
