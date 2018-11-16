package games.strategy.triplea.settings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonatype.goodies.prefs.memory.MemoryPreferences;

import games.strategy.security.CredentialManager;

final class ProtectedStringClientSettingTest {
  private final ProtectedStringClientSetting clientSetting = new ProtectedStringClientSetting("name", false);

  @BeforeAll
  static void setup() {
    ClientSetting.setPreferences(new MemoryPreferences());
  }

  @Test
  void testDisplayValue() {
    clientSetting.setValue("$eCrEt".toCharArray());
    assertThat(clientSetting.getDisplayValue(), is("$eCrEt"));
    final ProtectedStringClientSetting sensitive = new ProtectedStringClientSetting("name", true);
    sensitive.setValue("$eCrEt".toCharArray());
    assertThat(sensitive.getDisplayValue(), is("******"));
  }

  @Nested
  final class FormatValueTest {
    @Test
    void shouldReturnValueUnchanged() throws Exception {
      try (CredentialManager manager = CredentialManager.newInstance()) {
        final char[] sensitiveValue = "value".toCharArray();
        assertThat(manager.unprotectToString(clientSetting.formatValue(sensitiveValue)), is("value"));
        assertThat(sensitiveValue, is(new char[]{0, 0, 0, 0, 0}));
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
