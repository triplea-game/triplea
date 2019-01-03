package games.strategy.net;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class MacFinderTest {
  @Nested
  final class GetHashedMacAddressForBytesTest {
    @Test
    void shouldThrowExceptionWhenMacAddressLengthInvalid() {
      assertThrows(
          IllegalArgumentException.class, () -> MacFinder.getHashedMacAddress(new byte[5]));
      assertThrows(
          IllegalArgumentException.class, () -> MacFinder.getHashedMacAddress(new byte[7]));
    }

    @Test
    void shouldReturnValidHashedMacAddress() {
      final String hashedMacAddress = MacFinder.getHashedMacAddress(new byte[6]);

      assertThat(hashedMacAddress.length(), is(28));
      assertThat(hashedMacAddress, startsWith("$1$MH$"));
    }
  }

  @Nested
  final class IsValidHashedMacAddressTest {
    @Test
    void shouldReturnTrueWhenValid() {
      assertThat(MacFinder.isValidHashedMacAddress("$1$MH$ABCDWXYZabcdwxyz0189./"), is(true));
    }

    @Test
    void shouldReturnFalseWhenNotValidMd5CryptedValue() {
      Arrays.asList(
              "$1$MH$ABCDWXYZabcdwxyz0189.",
              "$1$MH$ABCDWXYZabcdwxyz0189./1",
              "1$MH$ABCDWXYZabcdwxyz0189./1",
              "$1$MH$ABCDWXYZabcdwxyz0189._")
          .forEach(
              hashedMacAddress ->
                  assertThat(MacFinder.isValidHashedMacAddress(hashedMacAddress), is(false)));
    }

    @Test
    void shouldReturnFalseWhenDoesNotHaveExpectedSalt() {
      assertThat(MacFinder.isValidHashedMacAddress("$1$SALT$ABCDWXYZabcdwxyz0189./"), is(false));
    }
  }

  @Nested
  final class TrimHashedMacAddressPrefixTest {
    @Test
    void shouldReturnHashedMacAddressWithoutPrefixWhenValid() {
      assertThat(
          MacFinder.trimHashedMacAddressPrefix("$1$MH$ABCDWXYZabcdwxyz0189./"),
          is("ABCDWXYZabcdwxyz0189./"));
    }

    @Test
    void shouldThrowExceptionWhenInvalid() {
      assertThrows(
          IllegalArgumentException.class, () -> MacFinder.trimHashedMacAddressPrefix("invalid"));
    }
  }
}
