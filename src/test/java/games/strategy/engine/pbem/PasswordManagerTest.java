package games.strategy.engine.pbem;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.Test;

public final class PasswordManagerTest {
  @Test
  public void shouldBeAbleToRoundTripPassword() throws Exception {
    final String expected = "123$%^ ABCdef←↑→↓";
    final PasswordManager passwordManager = newPasswordManager();

    final String protectedPassword = passwordManager.protect(expected);
    final String actual = passwordManager.unprotect(protectedPassword);

    assertThat(actual, is(expected));
  }

  private static final PasswordManager newPasswordManager() throws Exception {
    final String cipherAlgorithm = "RC4";
    final int cipherKeySizeInBits = 128;

    final PasswordManager.CipherFactory cipherFactory = () -> Cipher.getInstance(cipherAlgorithm);

    final KeyGenerator keyGenerator = KeyGenerator.getInstance(cipherAlgorithm);
    keyGenerator.init(cipherKeySizeInBits);
    final SecretKey key = keyGenerator.generateKey();

    return new PasswordManager(cipherFactory, key);
  }
}
