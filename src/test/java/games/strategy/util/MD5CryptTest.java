package games.strategy.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public final class MD5CryptTest {
  private static String crypt(final String password) {
    return MD5Crypt.crypt(password);
  }

  private static String crypt(final String password, final String salt) {
    return MD5Crypt.crypt(password, salt);
  }

  private static String getSalt(final String encryptedPassword) {
    return MD5Crypt.getSalt(encryptedPassword);
  }

  private static String newSalt() {
    return MD5Crypt.newSalt();
  }

  @Test
  public void cryptWithoutSalt_ShouldReturnEncryptedPassword() {
    Arrays.asList(
        "",
        "password",
        "the quick brown fox",
        "ABYZabyz0189",
        " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u007F")
        .forEach(password -> {
          final String encryptedPassword = crypt(password);
          final String salt = getSalt(encryptedPassword);
          assertThat(
              String.format("wrong encrypted password for '%s'", password),
              crypt(password, salt),
              is(encryptedPassword));
        });
  }

  @Test
  public void cryptWithSalt_ShouldReturnEncryptedPassword() {
    Arrays.asList(
        Triple.of("", "ll5ESPtE", "$1$ll5ESPtE$KsXRew.PuhVQTNMKSXQZx0"),
        Triple.of("password", "Eim8FgMk", "$1$Eim8FgMk$Y7Rv7y5WCc7rARI/g7xgH1"),
        Triple.of("the quick brown fox", "XlnQ6h98", "$1$XlnQ6h98$iIDgBB73DNCK/RwmzU0kv."),
        Triple.of("ABYZabyz0189", "3lvJqBhy", "$1$3lvJqBhy$ZjNcN3vfMfRdNcDyzQbQq."),
        Triple.of(" !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u007F", "wwmV2glD", "$1$wwmV2glD$J5dZUS3L8DAMUim4wdL/11"))
        .forEach(t -> {
          final String password = t.getFirst();
          final String salt = t.getSecond();
          final String encryptedPassword = t.getThird();
          assertThat(
              String.format("wrong encrypted password for '%s'", password),
              crypt(password, salt),
              is(encryptedPassword));
        });
  }

  @Test
  public void cryptWithSalt_ShouldUseNewRandomSaltWhenSaltIsEmpty() {
    assertThat(getSalt(crypt("password", "")).length(), is(8));
  }

  @Test
  public void cryptWithSalt_ShouldIgnoreLeadingMagicInSalt() {
    assertThat(crypt("password", "$1$Eim8FgMk"), is(crypt("password", "Eim8FgMk")));
  }

  @Test
  public void cryptWithSalt_ShouldIgnoreTrailingHashInSalt() {
    assertThat(crypt("password", "Z$IGNOREME"), is(crypt("password", "Z")));
  }

  @Test
  public void cryptWithSalt_ShouldUseNoMoreThanEightCharactersFromSalt() {
    assertThat(crypt("password", "123456789"), is(crypt("password", "12345678")));
  }

  @Test
  public void cryptWithSalt_ShouldSilentlyReplaceIllegalCharactersInSaltWithPeriod() {
    assertThat(crypt("password", "ABC!@DEF"), is(crypt("password", "ABC..DEF")));
  }

  @Test
  public void getSalt_ShouldReturnSaltWhenEncryptedPasswordIsLegal() {
    Arrays.asList(
        Tuple.of("$1$A$KnCRC85Rudn6P3cpfe3LR/", "A"),
        Tuple.of("$1$AB$4jo772pXjQ9qCwNdBde3d1", "AB"),
        Tuple.of("$1$ABC$3tP1DHUbEbG4bd67/3fFu/", "ABC"),
        Tuple.of("$1$ABCD$dQqZjlWf5HeY7rWTLu23s.", "ABCD"),
        Tuple.of("$1$ABCDE$ACfvKmv8y/KjlzX1R.tBw.", "ABCDE"),
        Tuple.of("$1$ABCDEF$f.URqvCLElutKgCndKcMI1", "ABCDEF"),
        Tuple.of("$1$ABCDEFG$mN7iGIbBXdAAjJtrJG2ia1", "ABCDEFG"),
        Tuple.of("$1$ABCDEFGH$hGGndps75hhROKqu/zh9q1", "ABCDEFGH"))
        .forEach(t -> {
          final String encryptedPassword = t.getFirst();
          final String salt = t.getSecond();
          assertThat(
              String.format("wrong salt for '%s'", encryptedPassword),
              getSalt(encryptedPassword),
              is(salt));
        });
  }

  @Test
  public void getSalt_ShouldThrowExceptionWhenEncryptedPasswordIsIllegal() {
    Arrays.asList(
        "1$A$KnCRC85Rudn6P3cpfe3LR/",
        "$$AB$4jo772pXjQ9qCwNdBde3d1",
        "$1ABC$3tP1DHUbEbG4bd67/3fFu/",
        "$1$$dQqZjlWf5HeY7rWTLu23s.",
        "$1$ABCDEFGHI$ACfvKmv8y/KjlzX1R.tBw.")
        .forEach(encryptedPassword -> {
          assertThrows(
              IllegalArgumentException.class,
              () -> getSalt(encryptedPassword),
              () -> String.format("expected exception for illegal encrypted password '%s'", encryptedPassword));
        });
  }

  @Test
  public void newSalt_ShouldReturnSaltOfLengthEight() {
    assertThat(newSalt().length(), is(8));
  }

  @Test
  public void newSalt_ShouldReturnDifferentSuccessiveSalts() {
    assertThat(newSalt(), is(not(newSalt())));
  }
}
