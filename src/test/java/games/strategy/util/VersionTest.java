package games.strategy.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class VersionTest {

  @Test
  public void testCompare() {
    final Version v1 = new Version(0, 0, 0);
    final Version v2 = new Version(1, 0, 0);
    assertNotEquals(v1, v2);
    assertNotEquals(v2, v1);
  }

  @Test
  public void testCompare2() {
    final Version v1 = new Version(0, 0, 0);
    final Version v2 = new Version(1, 1, 0);
    assertNotEquals(v1, v2);
    assertNotEquals(v2, v1);
  }

  @Test
  public void testCompare3() {
    final Version v1 = new Version(0, 0, 0);
    final Version v2 = new Version(0, 1, 0);
    assertNotEquals(v1, v2);
    assertNotEquals(v2, v1);
  }

  @Test
  public void testCompare4() {
    final Version v1 = new Version(0, 0, 0);
    final Version v2 = new Version(0, 0, 1);
    assertNotEquals(v1, v2);
    assertNotEquals(v2, v1);
  }

  @Test
  public void testRead1() {
    assertEquals("1.2.3", new Version("1.2.3").toString());
  }

  @Test
  public void testRead2() {
    assertEquals("1.2", new Version("1.2").toString());
  }

  @Test
  public void testRead3() {
    assertEquals("1.2", new Version("1.2.0").toString());
  }

  @Test
  public void getExactVersion() {
    assertEquals("1.2.3.4", new Version(1, 2, 3, 4).getExactVersion());
    assertEquals("1.2.3.4.5", new Version("1.2.3.4.5").getExactVersion());
  }

  @Test
  public void testIsGreaterThan() {
    assertTrue(new Version(2, 0).isGreaterThan(new Version(1, 0)));
    assertFalse(new Version(1, 0).isGreaterThan(new Version(2, 0)));
    assertFalse(new Version(1, 0).isGreaterThan(new Version(1, 0)));
  }

  @Test
  public void testIsLessThan() {
    assertFalse(new Version(2, 0).isLessThan(new Version(1, 0)));
    assertTrue(new Version(1, 0).isLessThan(new Version(2, 0)));
    assertFalse(new Version(1, 0).isLessThan(new Version(1, 0)));
  }
}
