package games.strategy.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public final class Md5CryptTest {
  @Test
  public void cryptWithoutSalt_ShouldReturnEncryptedPassword() {
    Arrays.asList(
        "",
        "password",
        "the quick brown fox",
        "ABYZabyz0189",
        " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u007F")
        .forEach(password -> {
          final String encryptedPassword = Md5Crypt.crypt(password);
          final String salt = Md5Crypt.getSalt(encryptedPassword);
          assertThat(
              String.format("wrong encrypted password for '%s'", password),
              Md5Crypt.crypt(password, salt),
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
              Md5Crypt.crypt(password, salt),
              is(encryptedPassword));
        });
  }

  @Test
  public void cryptWithSalt_ShouldUseNewRandomSaltWhenSaltIsEmpty() {
    assertThat(Md5Crypt.getSalt(Md5Crypt.crypt("password", "")).length(), is(8));
  }

  @Test
  public void cryptWithSalt_ShouldIgnoreLeadingMagicInSalt() {
    assertThat(Md5Crypt.crypt("password", "$1$Eim8FgMk"), is(Md5Crypt.crypt("password", "Eim8FgMk")));
  }

  @Test
  public void cryptWithSalt_ShouldIgnoreTrailingHashInSalt() {
    assertThat(Md5Crypt.crypt("password", "Z$IGNOREME"), is(Md5Crypt.crypt("password", "Z")));
  }

  @Test
  public void cryptWithSalt_ShouldUseNoMoreThanEightCharactersFromSalt() {
    assertThat(Md5Crypt.crypt("password", "123456789"), is(Md5Crypt.crypt("password", "12345678")));
  }

  @Test
  public void cryptWithSalt_ShouldSilentlyReplaceIllegalCharactersInSaltWithPeriod() {
    assertThat(Md5Crypt.crypt("password", "ABC!@DEF"), is(Md5Crypt.crypt("password", "ABC..DEF")));
  }

  @Test
  public void getHash_ShouldReturnHashWhenEncryptedPasswordIsLegal() {
    assertThat(Md5Crypt.getHash("$1$ll5ESPtE$KsXRew.PuhVQTNMKSXQZx0"), is("KsXRew.PuhVQTNMKSXQZx0"));
  }

  @Test
  public void getHash_ShouldThrowExceptionWhenEncryptedPasswordIsIllegal() {
    assertThrows(IllegalArgumentException.class, () -> Md5Crypt.getHash("1$A$KnCRC85Rudn6P3cpfe3LR/"));
  }

  @Test
  public void getSalt_ShouldReturnSaltWhenEncryptedPasswordIsLegal() {
    assertThat(Md5Crypt.getSalt("$1$A$KnCRC85Rudn6P3cpfe3LR/"), is("A"));
    assertThat(Md5Crypt.getSalt("$1$ABCDEFGH$hGGndps75hhROKqu/zh9q1"), is("ABCDEFGH"));
  }

  @Test
  public void getSalt_ShouldThrowExceptionWhenEncryptedPasswordIsIllegal() {
    assertThrows(IllegalArgumentException.class, () -> Md5Crypt.getSalt("1$A$KnCRC85Rudn6P3cpfe3LR/"));
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
              Md5Crypt.isLegalEncryptedPassword(value),
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
              Md5Crypt.isLegalEncryptedPassword(value),
              is(false));
        });
  }

  @Test
  public void newSalt_ShouldReturnSaltOfLengthEight() {
    assertThat(Md5Crypt.newSalt().length(), is(8));
  }

  @Test
  public void newSalt_ShouldReturnDifferentSuccessiveSalts() {
    assertThat(Md5Crypt.newSalt(), is(not(Md5Crypt.newSalt())));
  }
}
