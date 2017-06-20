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
public final class PasswordManagerTest {
  @Mock
  private Preferences preferences;

  private PasswordManager passwordManager;

  @Before
  public void setUp() throws Exception {
    passwordManager = PasswordManager.newInstance(PasswordManager.newMasterPassword());
  }

  @After
  public void tearDown() {
    passwordManager.close();
  }

  @Test
  public void shouldBeAbleToRoundTripPassword() throws Exception {
    final String expected = "123$%^ ABCdef←↑→↓";

    final String protectedPassword = passwordManager.protect(expected);
    final String actual = passwordManager.unprotectToString(protectedPassword);

    assertThat(actual, is(expected));
  }

  @Test
  public void getMasterPassword_ShouldCreateAndSaveMasterPasswordWhenMasterPasswordDoesNotExist() throws Exception {
    givenMasterPasswordDoesNotExist();

    final char[] masterPassword = PasswordManager.getMasterPassword(preferences);

    thenMasterPasswordExistsAndIs(masterPassword);
  }

  private void givenMasterPasswordDoesNotExist() {
    when(preferences.getByteArray(eq(PasswordManager.PREFERENCE_KEY_MASTER_PASSWORD), any())).then(returnsSecondArg());
  }

  private void thenMasterPasswordExistsAndIs(final char[] masterPassword) {
    verify(preferences).putByteArray(PasswordManager.PREFERENCE_KEY_MASTER_PASSWORD,
        PasswordManager.encodeMasterPassword(masterPassword));
  }

  @Test
  public void getMasterPassword_ShouldLoadMasterPasswordWhenMasterPasswordExists() throws Exception {
    final char[] expected = PasswordManager.newMasterPassword();
    givenMasterPasswordExists(expected);

    final char[] actual = PasswordManager.getMasterPassword(preferences);

    assertThat(actual, is(expected));
  }

  private void givenMasterPasswordExists(final char[] masterPassword) {
    when(preferences.getByteArray(eq(PasswordManager.PREFERENCE_KEY_MASTER_PASSWORD), any()))
        .thenReturn(PasswordManager.encodeMasterPassword(masterPassword));
  }

  @Test
  public void unprotect_ShouldThrowExceptionWhenProtectedPasswordContainsLessThanOnePeriod() throws Exception {
    catchException(() -> passwordManager.unprotect("AAAABBBB"));

    assertThat(caughtException(), allOf(
        is(instanceOf(PasswordManagerException.class)),
        hasMessageThat(containsString("malformed protected password"))));
  }

  @Test
  public void unprotect_ShouldThrowExceptionWhenProtectedPasswordContainsMoreThanOnePeriod() throws Exception {
    catchException(() -> passwordManager.unprotect("AAAA.BBBB.CCCC"));

    assertThat(caughtException(), allOf(
        is(instanceOf(PasswordManagerException.class)),
        hasMessageThat(containsString("malformed protected password"))));
  }
}
