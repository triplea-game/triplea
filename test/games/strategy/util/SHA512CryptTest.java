package games.strategy.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SHA512CryptTest {
  @Test
  public void testCrypt(){
    assertEquals(SHA512Crypt.crypt(""),
        "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e");
    assertEquals(SHA512Crypt.crypt("tripleA"),
        "8275652479d5643e5c88f7a6d07a8815e48b7a4d4708a6bec5ca3edacae9c0e96800bfd0b22d1c676d2085b71c8cc214e9e1692ca00abdf3b1be824d6a5e456a");
    assertEquals(SHA512Crypt.crypt("Map"),
        "ceea8c7d9d9d0235a0f02b6ded9a09674a529fbac049cbf69135b323101d6cad2e6fe1cffa61719e92281eaeb45e6d814d5006bf9826c7219ec10b7269f5a493");
    assertEquals(SHA512Crypt.crypt("Strategy"),
        "cc61da77033ab014504d902b4fa8eaad18b53bca94cc4145f23d7d2f27df2a39c59830fc251a33f2eb5d1c8416fb532a83339fce7b1266d059b4ecd8e4ff5901");
  }
}
