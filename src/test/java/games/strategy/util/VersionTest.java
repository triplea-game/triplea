package games.strategy.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VersionTest {

  @Test
  public void testCompare() {
    final Version v1 = new Version(0, 0, 0);
    final Version v2 = new Version(1, 0, 0);
    assertTrue(!v1.equals(v2));
    assertTrue(!v2.equals(v1));
  }

  @Test
  public void testCompare2() {
    final Version v1 = new Version(0, 0, 0);
    final Version v2 = new Version(1, 1, 0);
    assertTrue(!v1.equals(v2));
    assertTrue(!v2.equals(v1));
  }

  @Test
  public void testCompare3() {
    final Version v1 = new Version(0, 0, 0);
    final Version v2 = new Version(0, 1, 0);
    assertTrue(!v1.equals(v2));
    assertTrue(!v2.equals(v1));
  }

  @Test
  public void testCompare4() {
    final Version v1 = new Version(0, 0, 0);
    final Version v2 = new Version(0, 0, 1);
    assertTrue(!v1.equals(v2));
    assertTrue(!v2.equals(v1));
  }

  @Test
  public void testCompare5() {
    // micro differences should have no difference
    final Version v1 = new Version(0, 0, 0, 0);
    final Version v2 = new Version(0, 0, 0, 1);
    assertTrue(v1.equals(v2, true));
    assertTrue(v2.equals(v1, true));
  }

  @Test
  public void testRead1() {
    assertTrue("1.2.3".equals(new Version("1.2.3").toString()));
  }

  @Test
  public void testRead2() {
    assertTrue("1.2".equals(new Version("1.2").toString()));
  }

  @Test
  public void testRead3() {
    assertTrue("1.2".equals(new Version("1.2.0").toString()));
  }
}
