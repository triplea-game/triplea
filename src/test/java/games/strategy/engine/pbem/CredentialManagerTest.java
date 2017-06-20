package games.strategy.engine.pbem;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers.hasMessageThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.prefs.Preferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class CredentialManagerTest {
  @Mock
  private Preferences preferences;

  private CredentialManager credentialManager;

  @Before
  public void setUp() throws Exception {
    credentialManager = CredentialManager.newInstance(CredentialManager.newMasterPassword());
  }

  @After
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

    final char[] masterPassword = CredentialManager.getMasterPassword(preferences);

    thenMasterPasswordExistsAndIs(masterPassword);
  }

  private void givenMasterPasswordDoesNotExist() {
    when(preferences.getByteArray(eq(CredentialManager.PREFERENCE_KEY_MASTER_PASSWORD), any()))
        .then(returnsSecondArg());
  }

  private void thenMasterPasswordExistsAndIs(final char[] masterPassword) {
    verify(preferences).putByteArray(CredentialManager.PREFERENCE_KEY_MASTER_PASSWORD,
        CredentialManager.encodeMasterPassword(masterPassword));
  }

  @Test
  public void getMasterPassword_ShouldLoadMasterPasswordWhenMasterPasswordExists() throws Exception {
    final char[] expected = CredentialManager.newMasterPassword();
    givenMasterPasswordExists(expected);

    final char[] actual = CredentialManager.getMasterPassword(preferences);

    assertThat(actual, is(expected));
  }

  private void givenMasterPasswordExists(final char[] masterPassword) {
    when(preferences.getByteArray(eq(CredentialManager.PREFERENCE_KEY_MASTER_PASSWORD), any()))
        .thenReturn(CredentialManager.encodeMasterPassword(masterPassword));
  }

  @Test
  public void unprotect_ShouldThrowExceptionWhenProtectedCredentialContainsLessThanOnePeriod() throws Exception {
    catchException(() -> credentialManager.unprotect("AAAABBBB"));

    assertThat(caughtException(), allOf(
        is(instanceOf(CredentialManagerException.class)),
        hasMessageThat(containsString("malformed protected credential"))));
  }

  @Test
  public void unprotect_ShouldThrowExceptionWhenProtectedCredentialContainsMoreThanOnePeriod() throws Exception {
    catchException(() -> credentialManager.unprotect("AAAA.BBBB.CCCC"));

    assertThat(caughtException(), allOf(
        is(instanceOf(CredentialManagerException.class)),
        hasMessageThat(containsString("malformed protected credential"))));
  }
}
