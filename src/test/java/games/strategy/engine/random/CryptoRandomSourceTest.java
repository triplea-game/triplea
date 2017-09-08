package games.strategy.engine.random;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class CryptoRandomSourceTest {

  @Test
  public void testIntToRandom() {
    final byte[] bytes = CryptoRandomSource.intsToBytes(new int[] {0xDDCCBBAA});
    assertEquals(bytes.length, 4);
    assertEquals(bytes[0], (byte) 0xAA);
    assertEquals(bytes[1], (byte) 0xBB);
    assertEquals(bytes[2], (byte) 0xCC);
    assertEquals(bytes[3], (byte) 0xDD);
  }

  @Test
  public void testBytes() {
    assertEquals(CryptoRandomSource.byteToIntUnsigned((byte) 0), 0);
    assertEquals(CryptoRandomSource.byteToIntUnsigned((byte) 1), 1);
    assertEquals(CryptoRandomSource.byteToIntUnsigned(((byte) 0xFF)), 0xFF);
  }

  @Test
  public void testMix() {
    final int[] val1 = {0, 0, 0, 1, 1, 1, 2, 2, 2};
    final int[] val2 = {0, 1, 2, 0, 1, 2, 0, 1, 2};
    final int max = 3;

    final int[] mixedValues = CryptoRandomSource.mix(val1, val2, max);

    assertThat(mixedValues, is(new int[] {0, 1, 2, 1, 2, 0, 2, 0, 1}));
  }

  @Test
  public void testThereAndBackAgain() {
    final int[] ints = new int[] {0, 1, 12, 123, 0xFF, 0x100, -1, 124152, 532153, 123121, 0xABCDEF12, 0xFF00DD00,
        Integer.MAX_VALUE, Integer.MIN_VALUE};
    final int[] thereAndBack = CryptoRandomSource.bytesToInts(CryptoRandomSource.intsToBytes(ints));
    for (int i = 0; i < ints.length; i++) {
      assertEquals("at " + i, ints[i], thereAndBack[i]);
    }
  }
}
