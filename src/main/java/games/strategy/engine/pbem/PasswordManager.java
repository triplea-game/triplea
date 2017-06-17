package games.strategy.engine.pbem;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import com.google.common.annotations.VisibleForTesting;

/**
 * Provides a facility to protect a password before writing it to storage and to subsequently unprotect a protected
 * password after reading it from storage.
 *
 * <p>
 * Instances of this class are immutable.
 * </p>
 */
final class PasswordManager {
  private static final Charset PLAINTEXT_ENCODING_CHARSET = StandardCharsets.UTF_8;

  private final CipherFactory cipherFactory;

  private final SecretKey key;

  @VisibleForTesting
  PasswordManager(final CipherFactory cipherFactory, final SecretKey key) {
    this.cipherFactory = cipherFactory;
    this.key = key;
  }

  private Cipher newCipher(final int opmode) throws GeneralSecurityException {
    final Cipher cipher = cipherFactory.create();
    cipher.init(opmode, key);
    return cipher;
  }

  /**
   * Protects the specified unprotected password.
   *
   * @param unprotectedPassword The unprotected password; must not be {@code null}.
   *
   * @return The protected password; never {@code null}.
   *
   * @throws GeneralSecurityException If the unprotected password cannot be protected.
   *
   * @see #unprotect(String)
   */
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

  private static String encodeCiphertext(final byte[] ciphertext) {
    return Base64.getEncoder().encodeToString(ciphertext);
  }

  /**
   * Unprotects the specified protected password.
   *
   * @param protectedPassword The protected password previously created by {@link #protect(String)}; must not be
   *        {@code null}.
   *
   * @return The unprotected password; never {@code null}.
   *
   * @throws GeneralSecurityException If the protected password cannot be unprotected.
   *
   * @see #protect(String)
   */
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
