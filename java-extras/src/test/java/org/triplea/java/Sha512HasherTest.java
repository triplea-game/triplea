package org.triplea.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class Sha512HasherTest {

  @Test
  void testSha512() {
    assertEquals(
        "317167dd20761e90ab62f85af48985716bd2df129a0c36e5403841a861c01e51d8786c33e3"
            + "4dedeba6cb969aa9a957ba1079b9a48a66ceec668af39b91446ec5",
        Sha512Hasher.sha512("TripleA"));
    assertEquals(
        "1fdeebdbd3363f2d3f14f10e4cc85bc8115f564ba85a179f19b2d5b3da7ec3f7"
            + "9484cd4e59c6103ff4c8dd1cf37a82da13ed185f2e64725e113b0fb448c87fcb",
        Sha512Hasher.sha512("triplea"));
    assertEquals(
        "8d708d18b54df3962d696f069ad42dad7762b5d4d3c97ee5fa2dae0673ed4654"
            + "5164c078b8db3d59c4b96020e4316f17bb3d91bf1f6bc0896bbe75416eb8c385",
        Sha512Hasher.sha512("AAA"));
    assertEquals(
        "6bed2b94f7204211ba1ce66869096a59898688088d482e12c95a9778d2adf2ab"
            + "aee05890f97f73e4274742c69adf51406c0535452c9ec2e2adbf98048526b30c",
        Sha512Hasher.sha512("WWII"));

    assertThrows(NullPointerException.class, () -> Sha512Hasher.hashPasswordWithSalt(null));
  }
}
