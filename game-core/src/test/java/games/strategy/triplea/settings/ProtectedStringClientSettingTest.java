package games.strategy.triplea.settings;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.triplea.security.CredentialManager;
import org.triplea.security.CredentialManagerException;

final class ProtectedStringClientSettingTest {
  private final ProtectedStringClientSetting clientSetting =
      new ProtectedStringClientSetting("name");

  @Nested
  final class EncodeValueTest {
    @Test
    void shouldReturnProtectedValue() throws Exception {
      try (CredentialManager credentialManager = CredentialManager.newInstance()) {
        final char[] value = "value".toCharArray();

        final String encodedValue =
            ProtectedStringClientSetting.encodeValue(value, credentialManager);

        assertThat(credentialManager.unprotectToString(encodedValue), is("value"));
      }
    }

    @Test
    void shouldThrowExceptionWhenFailToProtectValue() throws Exception {
      final CredentialManager credentialManager = mock(CredentialManager.class);
      when(credentialManager.protect(any(char[].class)))
          .thenThrow(CredentialManagerException.class);

      assertThrows(
          ClientSetting.ValueEncodingException.class,
          () -> ProtectedStringClientSetting.encodeValue("value".toCharArray(), credentialManager));
    }
  }

  @Nested
  final class DecodeValueTest {
    @Test
    void shouldReturnUnprotectedValue() throws Exception {
      try (CredentialManager credentialManager = CredentialManager.newInstance()) {
        final String encodedValue = credentialManager.protect("encodedValue");

        final char[] value =
            ProtectedStringClientSetting.decodeValue(encodedValue, credentialManager);

        assertThat(value, is("encodedValue".toCharArray()));
      }
    }

    @Test
    void shouldThrowExceptionWhenFailToUnprotectValue() throws Exception {
      final CredentialManager credentialManager = mock(CredentialManager.class);
      when(credentialManager.unprotect(any())).thenThrow(CredentialManagerException.class);

      assertThrows(
          ClientSetting.ValueEncodingException.class,
          () -> ProtectedStringClientSetting.decodeValue("encodedValue", credentialManager));
    }
  }

  @Nested
  final class RoundTripTest extends AbstractClientSettingTestCase {
    @Test
    void shouldBeAbleToRoundTripValue() {
      final String value = "value";
      clientSetting.setValue(value.toCharArray());
      assertThat(clientSetting.getValue().map(String::new), isPresentAndIs(value));
    }
  }
}
