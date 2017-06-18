package games.strategy.engine.pbem;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.prefs.Preferences;

import javax.crypto.SecretKey;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class PasswordManagerTest {
  @Mock
  private Preferences preferences;

  @Test
  public void shouldBeAbleToRoundTripPassword() throws Exception {
    final String expected = "123$%^ ABCdef←↑→↓";
    final PasswordManager passwordManager = PasswordManager.newInstance(PasswordManager.newKey());

    final String protectedPassword = passwordManager.protect(expected);
    final String actual = passwordManager.unprotect(protectedPassword);

    assertThat(actual, is(expected));
  }

  @Test
  public void getKey_ShouldCreateAndSaveKeyWhenKeyDoesNotExist() throws Exception {
    givenKeyDoesNotExist();

    final SecretKey key = PasswordManager.getKey(preferences);

    thenKeyExistsAndIs(key);
  }

  private void givenKeyDoesNotExist() {
    when(preferences.getByteArray(eq(PasswordManager.PREFERENCE_KEY_ENCRYPTION_KEY), any())).then(returnsSecondArg());
  }

  private void thenKeyExistsAndIs(final SecretKey key) {
    verify(preferences).putByteArray(PasswordManager.PREFERENCE_KEY_ENCRYPTION_KEY, key.getEncoded());
  }

  @Test
  public void getKey_ShouldLoadKeyWhenKeyExists() throws Exception {
    final SecretKey expected = PasswordManager.newKey();
    givenKeyExists(expected);

    final SecretKey actual = PasswordManager.getKey(preferences);

    assertThat(actual.getEncoded(), is(expected.getEncoded()));
  }

  private void givenKeyExists(final SecretKey key) {
    when(preferences.getByteArray(eq(PasswordManager.PREFERENCE_KEY_ENCRYPTION_KEY), any()))
        .thenReturn(key.getEncoded());
  }
}
