package games.strategy.triplea.settings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.security.CredentialManager;

final class ProtectedStringClientSettingTest {
  private final ProtectedStringClientSetting clientSetting = new ProtectedStringClientSetting("name", false);

  @Nested
  final class FormatValueTest {
    @Test
    void shouldReturnValueUnchanged() throws Exception {
      try (CredentialManager manager = CredentialManager.newInstance()) {
        assertThat(manager.unprotectToString(clientSetting.formatValue("value".toCharArray())), is("value"));
      }
    }
  }

  @Nested
  final class ParseValueTest {
    @Test
    void shouldReturnEncodedValueUnchanged() throws Exception {
      try (CredentialManager manager = CredentialManager.newInstance()) {
        assertThat(clientSetting.parseValue(manager.protect("encodedValue")), is("encodedValue".toCharArray()));
      }
    }
  }
}
