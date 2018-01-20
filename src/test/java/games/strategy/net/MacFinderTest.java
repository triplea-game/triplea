package games.strategy.net;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
