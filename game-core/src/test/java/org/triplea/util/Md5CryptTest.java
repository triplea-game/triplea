package org.triplea.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.triplea.util.Md5Crypt.fromSaltAndHash;
import static org.triplea.util.Md5Crypt.getSalt;
import static org.triplea.util.Md5Crypt.hash;
import static org.triplea.util.Md5Crypt.hashPassword;
import static org.triplea.util.Md5Crypt.isLegalHashedValue;
import static org.triplea.util.Md5Crypt.newSalt;

import java.util.Arrays;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class Md5CryptTest {
  @Nested
  @SuppressWarnings("deprecation") // required for testing; remove upon next lobby-incompatible release
  final class HashPasswordTest {
    @Test
    void shouldReturnHashedPassword() {
      final String password = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ"
          + "[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u007F";
      final String salt = "wwmV2glD";
      assertThat(hashPassword(password, salt), is("$1$wwmV2glD$J5dZUS3L8DAMUim4wdL/11"));
    }
  }

  @Nested
  final class HashTest {
    @Test
    void shouldReturnHashedValue() {
      Arrays.asList(
          Triple.of("", "ll5ESPtE", "$1$ll5ESPtE$KsXRew.PuhVQTNMKSXQZx0"),
          Triple.of("value", "Eim8FgMk", "$1$Eim8FgMk$TYixIMiLc1BA6XHJBw66y0"),
          Triple.of("the quick brown fox", "XlnQ6h98", "$1$XlnQ6h98$iIDgBB73DNCK/RwmzU0kv."),
          Triple.of("ABYZabyz0189", "3lvJqBhy", "$1$3lvJqBhy$ZjNcN3vfMfRdNcDyzQbQq."),
          Triple.of(" !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ"
              + "[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u007F", "wwmV2glD", "$1$wwmV2glD$J5dZUS3L8DAMUim4wdL/11"))
          .forEach(t -> {
            final String value = t.getFirst();
            final String salt = t.getSecond();
            final String hashedValue = t.getThird();
            assertThat(String.format("wrong hashed value for '%s'", value), hash(value, salt), is(hashedValue));
          });
    }

    @Test
    void shouldUseNewRandomSaltWhenSaltIsEmpty() {
      assertThat(getSalt(hash("value", "")).length(), is(8));
    }

    @Test
    void shouldIgnoreLeadingMagicInSalt() {
      assertThat(hash("value", "$1$Eim8FgMk"), is(hash("value", "Eim8FgMk")));
    }

    @Test
    void shouldIgnoreTrailingHashInSalt() {
      assertThat(hash("value", "Z$IGNOREME"), is(hash("value", "Z")));
    }

    @Test
    void shouldUseNoMoreThanEightCharactersFromSalt() {
      assertThat(hash("value", "123456789"), is(hash("value", "12345678")));
    }

    @Test
    void shouldSilentlyReplaceIllegalCharactersInSaltWithPeriod() {
      assertThat(hash("value", "ABC!@DEF"), is(hash("value", "ABC..DEF")));
    }
  }

  @Nested
  final class GetSaltTest {
    @Test
    void shouldReturnSaltWhenHashedValueIsLegal() {
      assertThat(getSalt("$1$A$KnCRC85Rudn6P3cpfe3LR/"), is("A"));
      assertThat(getSalt("$1$ABCDEFGH$hGGndps75hhROKqu/zh9q1"), is("ABCDEFGH"));
    }

    @Test
    void shouldThrowExceptionWhenHashedValueIsIllegal() {
      assertThrows(IllegalArgumentException.class, () -> getSalt("1$A$KnCRC85Rudn6P3cpfe3LR/"));
    }
  }

  @Nested
  final class IsLegalHashedValueTest {
    @Test
    void shouldReturnTrueWhenHashedValueIsLegal() {
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
                String.format("expected legal hashed value '%s'", value),
                isLegalHashedValue(value),
                is(true));
          });
    }

    @Test
    void shouldReturnFalseWhenHashedValueIsIlegal() {
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
                String.format("expected illegal hashed value '%s'", value),
                isLegalHashedValue(value),
                is(false));
          });
    }
  }

  @Nested
  final class FromSaltAndHashTest {
    @Test
    void shouldReturnHashedValue() {
      assertThat(fromSaltAndHash("salt", "hash"), is("$1$salt$hash"));
    }
  }

  @Nested
  final class NewSaltTest {
    @Test
    void shouldReturnSaltOfLengthEight() {
      assertThat(newSalt().length(), is(8));
    }

    @Test
    void shouldReturnDifferentSuccessiveSalts() {
      assertThat(newSalt(), is(not(newSalt())));
    }
  }
}
