package games.strategy.util;

import static games.strategy.util.Md5Crypt.cryptInsensitive;
import static games.strategy.util.Md5Crypt.cryptSensitive;
import static games.strategy.util.Md5Crypt.getHash;
import static games.strategy.util.Md5Crypt.getSalt;
import static games.strategy.util.Md5Crypt.isLegalEncryptedPassword;
import static games.strategy.util.Md5Crypt.newSalt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public final class Md5CryptTest {
  @Test
  public void cryptSensitive_ShouldReturnEncryptedPassword() {
    final String password = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        + "[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u007F";
    final String salt = "wwmV2glD";
    assertThat(cryptSensitive(password, salt), is("$1$wwmV2glD$J5dZUS3L8DAMUim4wdL/11"));
  }

  @Test
  public void cryptInsensitive_ShouldReturnEncryptedPassword() {
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
              cryptInsensitive(password, salt),
              is(encryptedPassword));
        });
  }

  @Test
  public void cryptInsensitive_ShouldUseNewRandomSaltWhenSaltIsEmpty() {
    assertThat(getSalt(cryptInsensitive("password", "")).length(), is(8));
  }

  @Test
  public void cryptInsensitive_ShouldIgnoreLeadingMagicInSalt() {
    assertThat(cryptInsensitive("password", "$1$Eim8FgMk"), is(cryptInsensitive("password", "Eim8FgMk")));
  }

  @Test
  public void cryptInsensitive_ShouldIgnoreTrailingHashInSalt() {
    assertThat(cryptInsensitive("password", "Z$IGNOREME"), is(cryptInsensitive("password", "Z")));
  }

  @Test
  public void cryptInsensitive_ShouldUseNoMoreThanEightCharactersFromSalt() {
    assertThat(cryptInsensitive("password", "123456789"), is(cryptInsensitive("password", "12345678")));
  }

  @Test
  public void cryptInsensitive_ShouldSilentlyReplaceIllegalCharactersInSaltWithPeriod() {
    assertThat(cryptInsensitive("password", "ABC!@DEF"), is(cryptInsensitive("password", "ABC..DEF")));
  }

  @Test
  public void getHash_ShouldReturnHashWhenEncryptedPasswordIsLegal() {
    assertThat(getHash("$1$ll5ESPtE$KsXRew.PuhVQTNMKSXQZx0"), is("KsXRew.PuhVQTNMKSXQZx0"));
  }

  @Test
  public void getHash_ShouldThrowExceptionWhenEncryptedPasswordIsIllegal() {
    assertThrows(IllegalArgumentException.class, () -> getHash("1$A$KnCRC85Rudn6P3cpfe3LR/"));
  }

  @Test
  public void getSalt_ShouldReturnSaltWhenEncryptedPasswordIsLegal() {
    assertThat(getSalt("$1$A$KnCRC85Rudn6P3cpfe3LR/"), is("A"));
    assertThat(getSalt("$1$ABCDEFGH$hGGndps75hhROKqu/zh9q1"), is("ABCDEFGH"));
  }

  @Test
  public void getSalt_ShouldThrowExceptionWhenEncryptedPasswordIsIllegal() {
    assertThrows(IllegalArgumentException.class, () -> getSalt("1$A$KnCRC85Rudn6P3cpfe3LR/"));
  }

  @Test
  public void isLegalEncryptedPassword_ShouldReturnTrueWhenEncryptedPasswordIsLegal() {
    Arrays.asList(
        "$1$ll5ESPtE$KsXRew.PuhVQTNMKSXQZx0",
        "$1$Eim8FgMk$Y7Rv7y5WCc7rARI/g7xgH1",
        "$1$XlnQ6h98$iIDgBB73DNCK/RwmzU0kv.",
        "$1$3lvJqBhy$ZjNcN3vfMfRdNcDyzQbQq.",
        "$1$wwmV2glD$J5dZUS3L8DAMUim4wdL/11",
        "$1$A$KnCRC85Rudn6P3cpfe3LR/",
        "$1$AB$4jo772pXjQ9qCwNdBde3d1",
        "$1$ABC$3tP1DHUbEbG4bd67/3fFu/",
        "$1$ABCD$dQqZjlWf5HeY7rWTLu23s.",
        "$1$ABCDE$ACfvKmv8y/KjlzX1R.tBw.",
        "$1$ABCDEF$f.URqvCLElutKgCndKcMI1",
        "$1$ABCDEFG$mN7iGIbBXdAAjJtrJG2ia1",
        "$1$ABCDEFGH$hGGndps75hhROKqu/zh9q1")
        .forEach(value -> {
          assertThat(
              String.format("expected legal encrypted password '%s'", value),
              isLegalEncryptedPassword(value),
              is(true));
        });
  }

  @Test
  public void isLegalEncryptedPassword_ShouldReturnFalseWhenEncryptedPasswordIsIlegal() {
    Arrays.asList(
        "1$A$KnCRC85Rudn6P3cpfe3LR/",
        "$$AB$4jo772pXjQ9qCwNdBde3d1",
        "$1ABC$3tP1DHUbEbG4bd67/3fFu/",
        "$1$$dQqZjlWf5HeY7rWTLu23s.",
        "$1$ABCDEFGHI$ACfvKmv8y/KjlzX1R.tBw.",
        "$1$ABCDEFGH$ACfvKmv8y/KjlzX1R.tBw_",
        "$1$ABCDEFGH$ACfvKmv8y/KjlzX1R.tBw",
        "$1$ABCDEFGH$ACfvKmv8y/KjlzX1R.tBw..")
        .forEach(value -> {
          assertThat(
              String.format("expected illegal encrypted password '%s'", value),
              isLegalEncryptedPassword(value),
              is(false));
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
