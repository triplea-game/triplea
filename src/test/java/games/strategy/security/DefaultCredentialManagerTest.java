package games.strategy.security;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.prefs.Preferences;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public final class DefaultCredentialManagerTest {
  private static final char[] MASTER_PASSWORD = "MASTER←PASSWORD↑WITH→UNICODE↓CHARS".toCharArray();

  @Mock
  private Preferences preferences;

  private DefaultCredentialManager credentialManager;

  @BeforeEach
  public void setUp() {
    credentialManager = DefaultCredentialManager.newInstance(MASTER_PASSWORD);
  }

  @AfterEach
  public void tearDown() {
    credentialManager.close();
  }

  @Test
  public void shouldBeAbleToRoundTripCredential() throws Exception {
    final String expected = "123$%^ ABCdef←↑→↓";

    final String protectedCredential = credentialManager.protect(expected);
    final String actual = credentialManager.unprotectToString(protectedCredential);

    assertThat(actual, is(expected));
  }

  @Test
  public void getMasterPassword_ShouldCreateAndSaveMasterPasswordWhenMasterPasswordDoesNotExist() throws Exception {
    givenMasterPasswordDoesNotExist();

    final char[] masterPassword = DefaultCredentialManager.getMasterPassword(preferences);

    thenMasterPasswordExistsAndIs(masterPassword);
  }

  private void givenMasterPasswordDoesNotExist() {
    when(preferences.getByteArray(eq(DefaultCredentialManager.PREFERENCE_KEY_MASTER_PASSWORD), any()))
        .then(returnsSecondArg());
  }

  private void thenMasterPasswordExistsAndIs(final char[] masterPassword) {
    verify(preferences).putByteArray(DefaultCredentialManager.PREFERENCE_KEY_MASTER_PASSWORD,
        DefaultCredentialManager.encodeCharsToBytes(masterPassword));
  }

  @Test
  public void getMasterPassword_ShouldLoadMasterPasswordWhenMasterPasswordExists() throws Exception {
    givenMasterPasswordExists(MASTER_PASSWORD);

    final char[] actual = DefaultCredentialManager.getMasterPassword(preferences);

    assertThat(actual, is(MASTER_PASSWORD));
  }

  private void givenMasterPasswordExists(final char[] masterPassword) {
    when(preferences.getByteArray(eq(DefaultCredentialManager.PREFERENCE_KEY_MASTER_PASSWORD), any()))
        .thenReturn(DefaultCredentialManager.encodeCharsToBytes(masterPassword));
  }

  @Test
  public void unprotect_ShouldThrowExceptionWhenProtectedCredentialContainsLessThanOnePeriod() {
    final Exception e = assertThrows(CredentialManagerException.class, () -> credentialManager.unprotect("AAAABBBB"));

    assertThat(e.getMessage(), containsString("malformed protected credential"));
  }

  @Test
  public void unprotect_ShouldThrowExceptionWhenProtectedCredentialContainsMoreThanOnePeriod() {
    final Exception e =
        assertThrows(CredentialManagerException.class, () -> credentialManager.unprotect("AAAA.BBBB.CCCC"));

    assertThat(e.getMessage(), containsString("malformed protected credential"));
  }
}
