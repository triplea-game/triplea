package games.strategy.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.junit.Test;

public class SHA512CryptTest {
  @Test
  public void testCrypt() {
    assertEquals(
        "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
        SHA512Crypt.crypt(""));
    assertEquals(
        "8275652479d5643e5c88f7a6d07a8815e48b7a4d4708a6bec5ca3edacae9c0e96800bfd0b22d1c676d2085b71c8cc214e9e1692ca00abdf3b1be824d6a5e456a",
        SHA512Crypt.crypt("tripleA"));
    assertEquals(
        "ceea8c7d9d9d0235a0f02b6ded9a09674a529fbac049cbf69135b323101d6cad2e6fe1cffa61719e92281eaeb45e6d814d5006bf9826c7219ec10b7269f5a493",
        SHA512Crypt.crypt("Map"));
    assertEquals(
        "cc61da77033ab014504d902b4fa8eaad18b53bca94cc4145f23d7d2f27df2a39c59830fc251a33f2eb5d1c8416fb532a83339fce7b1266d059b4ecd8e4ff5901",
        SHA512Crypt.crypt("Strategy"));
  }

  @Test
  public void testSaltCrypt() {
    assertEquals(
        "213ccebae3a80eaf983d7f025476ba8af249c50bcfdf25c29ec509bda08ac10080558988c73eac80df9bb120920cc806ab48b61ba71c6711d21b7c6fb7cf0031",
        SHA512Crypt.crypt("Axis&Allies", "AAA"));
    assertEquals(
        "81016670b69f1d5a8bca4cbf21885dec4733d7d13b46a746b0aa947b1f1f119099e5a2d3ef6cf3b474854acdba61446f6ea4a536c9ff5a69ee792dc85df5f71d",
        SHA512Crypt.crypt("MapXMLCreator", "tripleA"));
    assertEquals(
        "93a7d383f8406c972e45b6a95c4f1c9a9b35db79e18e21cd027493eaf0dcd1a859b67e71e03ee9000c889dde95a59847d8a1de45f4ec54abf516881c4577c8a7",
        SHA512Crypt.crypt("MapCreator", "Random"));
    assertEquals(
        "33ef24a097ceb53a1ab529475902b327b1af425adcda7a2d66d02c2fb92005ee662bce17e469681a8503d3fabda14d7974792e32477104bd98574a298cf7868",
        SHA512Crypt.crypt("Tanks", "LOL"));
  }

  @Test
  public void testSaltCryptPassed() {
    for (int i = 0; i < 10; i++) {
      final String randomText = new BigInteger(130, new SecureRandom()).toString(32);
      final String randomSalt = new BigInteger(130, new SecureRandom()).toString(32);
      assertTrue(SHA512Crypt.cryptPassSalt(randomText, randomSalt).startsWith("$" + randomSalt));
    }
  }
}
