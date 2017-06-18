package games.strategy.engine.pbem;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.prefs.Preferences;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.google.common.annotations.VisibleForTesting;

/**
 * Provides a facility to protect a password before writing it to storage and to subsequently unprotect a protected
 * password after reading it from storage.
 *
 * <p>
 * The password manager uses a user-specific key to protect passwords. The key is stored unencrypted in the user's
 * preference tree. Thus, the key is in an area of the user's system that has the required permissions to ensure access
 * only by the user. The key will automatically be created if it does not exist.
 * </p>
 *
 * <p>
 * Instances of this class are immutable.
 * </p>
 */
final class PasswordManager {
  private static final String CIPHER_ALGORITHM = "AES";
  private static final int CIPHER_KEY_SIZE_IN_BITS = 128;
  private static final Charset PLAINTEXT_ENCODING_CHARSET = StandardCharsets.UTF_8;

  @VisibleForTesting
  static final String PREFERENCE_KEY_ENCRYPTION_KEY = "PASSWORD_MANAGER_ENCRYPTION_KEY";

  private final CipherFactory cipherFactory;
  private final SecretKey key;

  private PasswordManager(final CipherFactory cipherFactory, final SecretKey key) {
    this.cipherFactory = cipherFactory;
    this.key = key;
  }

  static PasswordManager newInstance() throws GeneralSecurityException {
    return newInstance(getKey());
  }

  @VisibleForTesting
  static PasswordManager newInstance(final SecretKey key) {
    return new PasswordManager(() -> Cipher.getInstance(CIPHER_ALGORITHM), key);
  }

  private static SecretKey getKey() throws GeneralSecurityException {
    return getKey(Preferences.userNodeForPackage(PasswordManager.class));
  }

  @VisibleForTesting
  static SecretKey getKey(final Preferences preferences) throws GeneralSecurityException {
    final byte[] encodedKey = preferences.getByteArray(PREFERENCE_KEY_ENCRYPTION_KEY, null);
    if (encodedKey != null) {
      return decodeKey(encodedKey);
    }

    final SecretKey key = newKey();
    preferences.putByteArray(PREFERENCE_KEY_ENCRYPTION_KEY, encodeKey(key));
    return key;
  }

  private static SecretKey decodeKey(final byte[] encodedKey) {
    return new SecretKeySpec(encodedKey, CIPHER_ALGORITHM);
  }

  @VisibleForTesting
  static SecretKey newKey() throws GeneralSecurityException {
    final KeyGenerator keyGenerator = KeyGenerator.getInstance(CIPHER_ALGORITHM);
    keyGenerator.init(CIPHER_KEY_SIZE_IN_BITS);
    return keyGenerator.generateKey();
  }

  private static byte[] encodeKey(final SecretKey key) {
    return key.getEncoded();
  }

  String protect(final String unprotectedPassword) throws GeneralSecurityException {
    assert unprotectedPassword != null;

    return encodeCiphertext(encrypt(decodePlaintext(unprotectedPassword)));
  }

  private static byte[] decodePlaintext(final String encodedPlaintext) {
    return encodedPlaintext.getBytes(PLAINTEXT_ENCODING_CHARSET);
  }

  private byte[] encrypt(final byte[] plaintext) throws GeneralSecurityException {
    return newCipher(Cipher.ENCRYPT_MODE).doFinal(plaintext);
  }

  private Cipher newCipher(final int opmode) throws GeneralSecurityException {
    final Cipher cipher = cipherFactory.create();
    cipher.init(opmode, key);
    return cipher;
  }

  private static String encodeCiphertext(final byte[] ciphertext) {
    return Base64.getEncoder().encodeToString(ciphertext);
  }

  String unprotect(final String protectedPassword) throws GeneralSecurityException {
    assert protectedPassword != null;

    return encodePlaintext(decrypt(decodeCiphertext(protectedPassword)));
  }

  private static byte[] decodeCiphertext(final String encodedCiphertext) {
    return Base64.getDecoder().decode(encodedCiphertext);
  }

  private byte[] decrypt(final byte[] ciphertext) throws GeneralSecurityException {
    return newCipher(Cipher.DECRYPT_MODE).doFinal(ciphertext);
  }

  private static String encodePlaintext(final byte[] plaintext) {
    return new String(plaintext, PLAINTEXT_ENCODING_CHARSET);
  }

  @FunctionalInterface
  @VisibleForTesting
  interface CipherFactory {
    Cipher create() throws GeneralSecurityException;
  }
}
