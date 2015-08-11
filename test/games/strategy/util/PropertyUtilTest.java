package games.strategy.util;

import games.strategy.triplea.attatchments.RulesAttachment;
import junit.framework.TestCase;

public class PropertyUtilTest extends TestCase {
  /*
   * public void testGet()
   * {
   * final JLabel label = new JLabel("TestCase");
   * assertEquals("TestCase", PropertyUtil.get("text", label));
   * }
   * public void testSet()
   * {
   * final JLabel label = new JLabel("TestCase");
   * PropertyUtil.set("text", "changed", label);
   * assertEquals("changed", PropertyUtil.get("text", label));
   * }
   * public void testSetInt()
   * {
   * final JLabel label = new JLabel();
   * PropertyUtil.set("IconTextGap", 10, label);
   * PropertyUtil.get("IconTextGap", label);
   * }
   */
  public void testGetFieldObject() {
    final RulesAttachment at = new RulesAttachment("test", null, null);
    int uses = (Integer) PropertyUtil.getPropertyFieldObject("uses", at);
    // default value should be -1
    assertEquals(-1, uses);
    PropertyUtil.set("uses", "3", at);
    uses = (Integer) PropertyUtil.getPropertyFieldObject("uses", at);
    assertEquals(3, uses);
    final IntegerMap<String> unitPresence = new IntegerMap<String>();
    unitPresence.add("Blah", 3);
    PropertyUtil.set("unitPresence", unitPresence, at);
    assertEquals(unitPresence, PropertyUtil.getPropertyFieldObject("unitPresence", at));
  }
}
