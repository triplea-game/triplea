package games.strategy.net;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public final class MacFinderTest {
  @Test
  public void getHashedMacAddressForBytes_ShouldThrowExceptionWhenMacAddressLengthInvalid() {
    assertThrows(IllegalArgumentException.class, () -> MacFinder.getHashedMacAddress(new byte[5]));
    assertThrows(IllegalArgumentException.class, () -> MacFinder.getHashedMacAddress(new byte[7]));
  }

  @Test
  public void getHashedMacAddressForBytes_ShouldReturnValidHashedMacAddress() {
    final String hashedMacAddress = MacFinder.getHashedMacAddress(new byte[6]);

    assertThat(hashedMacAddress.length(), is(28));
    assertThat(hashedMacAddress, startsWith("$1$MH$"));
  }

  @Test
  public void isValidHashedMacAddress_ShouldReturnTrueWhenValid() {
    assertThat(MacFinder.isValidHashedMacAddress("$1$MH$ABCDWXYZabcdwxyz0189./"), is(true));
  }

  @Test
  public void isValidHashedMacAddress_ShouldReturnFalseWhenNotValidMd5CryptedValue() {
    Arrays.asList(
        "$1$MH$ABCDWXYZabcdwxyz0189.",
        "$1$MH$ABCDWXYZabcdwxyz0189./1",
        "1$MH$ABCDWXYZabcdwxyz0189./1",
        "$1$MH$ABCDWXYZabcdwxyz0189._")
        .forEach(hashedMacAddress -> assertThat(MacFinder.isValidHashedMacAddress(hashedMacAddress), is(false)));
  }

  @Test
  public void isValidHashedMacAddress_ShouldReturnFalseWhenDoesNotHaveExpectedSalt() {
    assertThat(MacFinder.isValidHashedMacAddress("$1$SALT$ABCDWXYZabcdwxyz0189./"), is(false));
  }

  @Test
  public void trimHashedMacAddressPrefix_ShouldReturnHashedMacAddressWithoutPrefixWhenValid() {
    assertThat(MacFinder.trimHashedMacAddressPrefix("$1$MH$ABCDWXYZabcdwxyz0189./"), is("ABCDWXYZabcdwxyz0189./"));
  }

  @Test
  public void trimHashedMacAddressPrefix_ShouldThrowExceptionWhenInvalid() {
    assertThrows(IllegalArgumentException.class, () -> MacFinder.trimHashedMacAddressPrefix("invalid"));
  }
}
