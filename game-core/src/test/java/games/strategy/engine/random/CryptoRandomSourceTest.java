package games.strategy.engine.random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CryptoRandomSourceTest {

  @Test
  void testIntsToBytes() {
    final byte[] bytes = CryptoRandomSource.intsToBytes(new int[] {0xD1C2B3A4, 0x1A2B3C4D});
    assertEquals(8, bytes.length);
    assertEquals((byte) 0xA4, bytes[0]);
    assertEquals((byte) 0xB3, bytes[1]);
    assertEquals((byte) 0xC2, bytes[2]);
    assertEquals((byte) 0xD1, bytes[3]);
    assertEquals((byte) 0x4D, bytes[4]);
    assertEquals((byte) 0x3C, bytes[5]);
    assertEquals((byte) 0x2B, bytes[6]);
    assertEquals((byte) 0x1A, bytes[7]);
  }

  @Test
  void testBytesToInts() {
    final int[] ints =
        CryptoRandomSource.bytesToInts(
            new byte[] {
              (byte) 0xA4, (byte) 0xB3, (byte) 0xC2, (byte) 0xD1,
              (byte) 0x4D, (byte) 0x3C, (byte) 0x2B, (byte) 0x1A
            });
    assertEquals(2, ints.length);
    assertEquals(0xD1C2B3A4, ints[0]);
    assertEquals(0x1A2B3C4D, ints[1]);
  }

  @Test
  void testMix() {
    final int[] val1 = {0, 0, 0, 1, 1, 1, 2, 2, 2};
    final int[] val2 = {0, 1, 2, 0, 1, 2, 0, 1, 2};
    final int max = 3;

    final int[] mixedValues = CryptoRandomSource.mix(val1, val2, max);

    assertThat(mixedValues, is(new int[] {0, 1, 2, 1, 2, 0, 2, 0, 1}));
  }

  @Test
  void testThereAndBackAgain() {
    final int[] ints =
        new int[] {
          0,
          1,
          12,
          123,
          0xFF,
          0x100,
          -1,
          124152,
          532153,
          123121,
          0xABCDEF12,
          0xFF00DD00,
          Integer.MAX_VALUE,
          Integer.MIN_VALUE
        };
    final int[] thereAndBack = CryptoRandomSource.bytesToInts(CryptoRandomSource.intsToBytes(ints));
    for (int i = 0; i < ints.length; i++) {
      assertEquals(ints[i], thereAndBack[i], "at " + i);
    }
  }
}
