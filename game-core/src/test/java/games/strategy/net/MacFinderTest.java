package games.strategy.net;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@SuppressWarnings("InnerClassMayBeStatic")
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

    @ParameterizedTest
    @ValueSource(
        strings = {
          "$1$MH$ABCDWXYZabcdwxyz0189.",
          "$1$MH$ABCDWXYZabcdwxyz0189./1",
          "1$MH$ABCDWXYZabcdwxyz0189./1",
          "$1$MH$ABCDWXYZabcdwxyz0189._"
        })
    void shouldReturnFalseWhenNotValidMd5CryptedValue(final String hashedMacAddress) {
      assertThat(MacFinder.isValidHashedMacAddress(hashedMacAddress), is(false));
    }

    @Test
    void shouldReturnFalseWhenDoesNotHaveExpectedSalt() {
      assertThat(MacFinder.isValidHashedMacAddress("$1$SALT$ABCDWXYZabcdwxyz0189./"), is(false));
    }
  }

  @Nested
  final class WithPrefixTest {
    @Test
    void shouldReturnValueUnchangedWhenPrefixPresent() {
      assertThat(
          MacFinder.withPrefix("$1$MH$ABCDWXYZabcdwxyz0189./"), is("$1$MH$ABCDWXYZabcdwxyz0189./"));
    }

    @Test
    void shouldReturnValueWithPrefixWhenPrefixAbsent() {
      assertThat(
          MacFinder.withPrefix("ABCDWXYZabcdwxyz0189./"), is("$1$MH$ABCDWXYZabcdwxyz0189./"));
    }
  }
}
