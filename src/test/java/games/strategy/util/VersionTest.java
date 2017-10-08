package games.strategy.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
  public void testRead4() {
    assertEquals("1.2.3.dev", new Version("1.2.3.dev").toString());
  }

  @Test
  public void getExactVersion() {
    assertEquals("1.2.3.4", new Version(1, 2, 3, 4).getExactVersion());
    assertEquals("1.2.3.4.5", new Version("1.2.3.4.5").getExactVersion());
  }

  @Test
  public void testCompatible() {
    assertTrue(new Version(1, 9).isCompatible(new Version(1, 9)));
    assertTrue(new Version(1, 9).isCompatible(new Version(1, 9, 1, 2)));
    assertTrue(new Version(1, 9, 1, 2).isCompatible(new Version(1, 9, 3, 4)));
    assertTrue(new Version(1, 9, 1).isCompatible(new Version(1, 9, 2, 3)));
    assertTrue(new Version(1, 9, 1, 2).isCompatible(new Version(1, 9, 3)));
    assertFalse(new Version(2, 9).isCompatible(new Version(1, 9)));
    assertFalse(new Version(1, 10).isCompatible(new Version(1, 9)));
    assertFalse(new Version(2, 10).isCompatible(new Version(1, 9)));
  }
}
