package games.strategy.engine.pbem;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.prefs.Preferences;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.common.annotations.VisibleForTesting;

/**
 * Provides a facility to protect a credential before writing it to storage and to subsequently unprotect a protected
 * credential after reading it from storage.
 *
 * <p>
 * The credential manager uses a user-specific master password to protect individual credentials. The master password is
 * stored unencrypted in the user's preference tree. Thus, the master password is in an area of the user's system that
 * has the required permissions to ensure access only by the user. The master password will automatically be created if
 * it does not exist.
 * </p>
 *
 * <p>
 * Instances of this class are not thread safe.
 * </p>
 */
final class CredentialManager implements AutoCloseable {
  private static final String CIPHER_ALGORITHM = "AES";
  private static final Charset MASTER_PASSWORD_CHARSET = StandardCharsets.ISO_8859_1;
  private static final Charset PLAINTEXT_CHARSET = StandardCharsets.UTF_8;

  @VisibleForTesting
  static final String PREFERENCE_KEY_MASTER_PASSWORD = "CREDENTIAL_MANAGER_MASTER_PASSWORD";

  private final CipherFactory cipherFactory;
  private final char[] masterPassword;

  private CredentialManager(final CipherFactory cipherFactory, final char[] masterPassword) {
    this.cipherFactory = cipherFactory;
    this.masterPassword = masterPassword;
  }

  /**
   * Creates a new instance of the {@code CredentialManager} class using the default master password for the user.
   *
   * <p>
   * If the user has a saved master password, it will be used. Otherwise, a new master password will be created for the
   * user and saved.
   * </p>
   *
   * @return A new credential manager; never {@code null}.
   *
   * @throws CredentialManagerException If no saved master password exists and a new master password cannot be created.
   */
  static CredentialManager newInstance() throws CredentialManagerException {
    try {
      return newInstance(getMasterPassword());
    } catch (final GeneralSecurityException e) {
      throw new CredentialManagerException("failed to create credential manager", e);
    }
  }

  @VisibleForTesting
  static CredentialManager newInstance(final char[] masterPassword) {
    return new CredentialManager(() -> Cipher.getInstance(CIPHER_ALGORITHM), masterPassword);
  }

  private static char[] getMasterPassword() throws GeneralSecurityException {
    return getMasterPassword(Preferences.userNodeForPackage(CredentialManager.class));
  }

  @VisibleForTesting
  static char[] getMasterPassword(final Preferences preferences) throws GeneralSecurityException {
    char[] masterPassword = loadMasterPassword(preferences);
    if (masterPassword == null) {
      masterPassword = newMasterPassword();
      saveMasterPassword(preferences, masterPassword);
    }
    return masterPassword;
  }

  private static char[] loadMasterPassword(final Preferences preferences) {
    final byte[] encodedMasterPassword = preferences.getByteArray(PREFERENCE_KEY_MASTER_PASSWORD, null);
    if (encodedMasterPassword == null) {
      return null;
    }

    final char[] masterPassword = decodeMasterPassword(encodedMasterPassword);
    scrub(encodedMasterPassword);
    return masterPassword;
  }

  private static char[] decodeMasterPassword(final byte[] encodedMasterPassword) {
    return decodeChars(encodedMasterPassword, MASTER_PASSWORD_CHARSET);
  }

  private static char[] decodeChars(final byte[] bytes, final Charset charset) {
    final CharBuffer cb = charset.decode(ByteBuffer.wrap(bytes));
    final char[] chars = new char[cb.length()];
    cb.get(chars);
    scrub(cb);
    return chars;
  }

  @VisibleForTesting
  static char[] newMasterPassword() throws GeneralSecurityException {
    // https://en.wikipedia.org/wiki/Password_strength#Random_passwords
    // Base64 ~~ Case sensitive alphanumeric
    final int base64DigitCountFor160BitEntropy = 27;
    final int dataBitsPerBase64Digit = 6;
    final int totalBitsPerBase64Digit = 8;
    final int byteLengthForDesiredEntropy =
        (base64DigitCountFor160BitEntropy * dataBitsPerBase64Digit / totalBitsPerBase64Digit) + 1;

    final byte[] randomBytes = new byte[byteLengthForDesiredEntropy];
    fillRandom(randomBytes);
    final byte[] encodedMasterPassword = Base64.getEncoder().encode(randomBytes);
    scrub(randomBytes);
    final char[] masterPassword = decodeMasterPassword(encodedMasterPassword);
    scrub(encodedMasterPassword);
    return masterPassword;
  }

  private static void fillRandom(final byte[] bytes) throws GeneralSecurityException {
    SecureRandom.getInstance("SHA1PRNG").nextBytes(bytes);
  }

  private static void saveMasterPassword(final Preferences preferences, final char[] masterPassword) {
    preferences.putByteArray(PREFERENCE_KEY_MASTER_PASSWORD, encodeMasterPassword(masterPassword));
  }

  @VisibleForTesting
  static byte[] encodeMasterPassword(final char[] masterPassword) {
    return encodeChars(masterPassword, MASTER_PASSWORD_CHARSET);
  }

  private static byte[] encodeChars(final char[] chars, final Charset charset) {
    final ByteBuffer bb = charset.encode(CharBuffer.wrap(chars));
    final byte[] bytes = new byte[bb.remaining()];
    bb.get(bytes);
    scrub(bb);
    return bytes;
  }

  @Override
  public void close() {
    scrub(masterPassword);
  }

  /**
   * Protects the unprotected credential contained in the specified string.
   *
   * <p>
   * <strong>IT IS STRONGLY RECOMMENDED TO USE {@link #protect(char[])} INSTEAD!</strong> Strings are immutable and
   * the secret data contained in the argument cannot be scrubbed. This data may then be leaked outside of this
   * process (e.g. if memory is paged to disk).
   * </p>
   *
   * @param unprotectedCredentialAsString The unprotected credential as a string; must not be {@code null}.
   *
   * @return The protected credential; never {@code null}.
   *
   * @throws CredentialManagerException If the unprotected credential cannot be protected.
   *
   * @see #unprotectToString(String)
   */
  String protect(final String unprotectedCredentialAsString) throws CredentialManagerException {
    assert unprotectedCredentialAsString != null;

    final char[] unprotectedCredential = unprotectedCredentialAsString.toCharArray();
    try {
      return protect(unprotectedCredential);
    } finally {
      scrub(unprotectedCredential);
    }
  }

  /**
   * Protects the unprotected credential contained in the specified character array.
   *
   * @param unprotectedCredential The unprotected credential as a character array; must not be {@code null}.
   *
   * @return The protected credential; never {@code null}.
   *
   * @throws CredentialManagerException If the unprotected credential cannot be protected.
   *
   * @see #unprotect(String)
   */
  String protect(final char[] unprotectedCredential) throws CredentialManagerException {
    assert unprotectedCredential != null;

    try {
      final byte[] plaintext = decodePlaintext(unprotectedCredential);
      final byte[] salt = newSalt();
      final byte[] ciphertext = encrypt(plaintext, salt);
      scrub(plaintext);
      return encodeCiphertextAndSalt(ciphertext, salt);
    } catch (final GeneralSecurityException e) {
      throw new CredentialManagerException("failed to protect credential", e);
    }
  }

  private static byte[] decodePlaintext(final char[] encodedPlaintext) {
    return encodeChars(encodedPlaintext, PLAINTEXT_CHARSET);
  }

  private static byte[] newSalt() throws GeneralSecurityException {
    // https://crackstation.net/hashing-security.htm#salt
    final int saltLengthForSha512InBytes = 64;

    final byte[] salt = new byte[saltLengthForSha512InBytes];
    fillRandom(salt);
    return salt;
  }

  private byte[] encrypt(final byte[] plaintext, final byte[] salt) throws GeneralSecurityException {
    return newCipher(Cipher.ENCRYPT_MODE, salt).doFinal(plaintext);
  }

  private Cipher newCipher(final int opmode, final byte[] salt) throws GeneralSecurityException {
    final Cipher cipher = cipherFactory.create();
    cipher.init(opmode, newSecretKey(salt));
    return cipher;
  }

  private SecretKey newSecretKey(final byte[] salt) throws GeneralSecurityException {
    // https://security.stackexchange.com/questions/3959/recommended-of-iterations-when-using-pkbdf2-sha256
    final int iterationCount = 20_000;

    // Oracle Java 8 limited to AES-128 without JCE Unlimited Strength Jurisdiction Policy installed on end-user
    // machine. Oracle Java 9 will not have this limitation. OpenJDK does not have this limitation.
    final int keyLengthInBits = 128;

    final SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
    final KeySpec keySpec = new PBEKeySpec(masterPassword, salt, iterationCount, keyLengthInBits);
    final SecretKey key = secretKeyFactory.generateSecret(keySpec);
    return new SecretKeySpec(key.getEncoded(), CIPHER_ALGORITHM);
  }

  private static String encodeCiphertextAndSalt(final byte[] ciphertext, final byte[] salt) {
    final String encodedCiphertext = Base64.getEncoder().encodeToString(ciphertext);
    final String encodedSalt = Base64.getEncoder().encodeToString(salt);
    return encodedSalt + "." + encodedCiphertext;
  }

  /**
   * Unprotects the specified protected credential into a string.
   *
   * <p>
   * <strong>IT IS STRONGLY RECOMMENDED TO USE {@link #unprotect(String)} INSTEAD!</strong> Strings are immutable and
   * the secret data contained in the return value cannot be scrubbed. This data may then be leaked outside of this
   * process (e.g. if memory is paged to disk).
   * </p>
   *
   * @param protectedCredential The protected credential previously created by {@link #protect(String)}; must not be
   *        {@code null}.
   *
   * @return The unprotected credential as a string; never {@code null}.
   *
   * @throws CredentialManagerException If the protected credential cannot be unprotected.
   *
   * @see #protect(String)
   */
  String unprotectToString(final String protectedCredential) throws CredentialManagerException {
    assert protectedCredential != null;

    final char[] unprotectedCredential = unprotect(protectedCredential);
    final String unprotectedCredentialAsString = new String(unprotectedCredential);
    scrub(unprotectedCredential);
    return unprotectedCredentialAsString;
  }

  /**
   * Unprotects the specified protected credential into a character array.
   *
   * @param protectedCredential The protected credential previously created by {@link #protect(char[])}; must not be
   *        {@code null}.
   *
   * @return The unprotected credential as a character array; never {@code null}.
   *
   * @throws CredentialManagerException If the protected credential cannot be unprotected.
   *
   * @see #protect(char[])
   */
  char[] unprotect(final String protectedCredential) throws CredentialManagerException {
    assert protectedCredential != null;

    try {
      final CiphertextAndSalt ciphertextAndSalt = decodeCiphertextAndSalt(protectedCredential);
      final byte[] plaintext = decrypt(ciphertextAndSalt.ciphertext, ciphertextAndSalt.salt);
      final char[] unprotectedCredential = encodePlaintext(plaintext);
      scrub(plaintext);
      return unprotectedCredential;
    } catch (final GeneralSecurityException e) {
      throw new CredentialManagerException("failed to unprotect credential", e);
    }
  }

  private static CiphertextAndSalt decodeCiphertextAndSalt(final String encodedCiphertextAndSalt)
      throws CredentialManagerException {
    final String[] components = encodedCiphertextAndSalt.split("\\.");
    if (components.length != 2) {
      throw new CredentialManagerException("malformed protected credential");
    }

    final String encodedSalt = components[0];
    final String encodedCiphertext = components[1];
    final byte[] ciphertext = Base64.getDecoder().decode(encodedCiphertext);
    final byte[] salt = Base64.getDecoder().decode(encodedSalt);
    return new CiphertextAndSalt(ciphertext, salt);
  }

  private byte[] decrypt(final byte[] ciphertext, final byte[] salt) throws GeneralSecurityException {
    return newCipher(Cipher.DECRYPT_MODE, salt).doFinal(ciphertext);
  }

  private static char[] encodePlaintext(final byte[] plaintext) {
    return decodeChars(plaintext, PLAINTEXT_CHARSET);
  }

  private static void scrub(final byte[] bytes) {
    Arrays.fill(bytes, (byte) 0);
  }

  private static void scrub(final char[] chars) {
    Arrays.fill(chars, '\0');
  }

  private static void scrub(final ByteBuffer bb) {
    if (bb.hasArray()) {
      scrub(bb.array());
    }
  }

  private static void scrub(final CharBuffer cb) {
    if (cb.hasArray()) {
      scrub(cb.array());
    }
  }

  private static final class CiphertextAndSalt {
    final byte[] ciphertext;
    final byte[] salt;

    CiphertextAndSalt(final byte[] ciphertext, final byte[] salt) {
      this.ciphertext = ciphertext;
      this.salt = salt;
    }
  }

  @FunctionalInterface
  @VisibleForTesting
  interface CipherFactory {
    Cipher create() throws GeneralSecurityException;
  }
}
