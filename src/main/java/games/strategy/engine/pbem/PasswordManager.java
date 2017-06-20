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
 * Provides a facility to protect a password before writing it to storage and to subsequently unprotect a protected
 * password after reading it from storage.
 *
 * <p>
 * The password manager uses a user-specific master password to protect individual passwords. The master password is
 * stored unencrypted in the user's preference tree. Thus, the master password is in an area of the user's system that
 * has the required permissions to ensure access only by the user. The master password will automatically be created if
 * it does not exist.
 * </p>
 *
 * <p>
 * Instances of this class are immutable.
 * </p>
 */
final class PasswordManager {
  private static final String CIPHER_ALGORITHM = "AES";
  private static final Charset MASTER_PASSWORD_CHARSET = StandardCharsets.ISO_8859_1;
  private static final Charset PLAINTEXT_CHARSET = StandardCharsets.UTF_8;

  @VisibleForTesting
  static final String PREFERENCE_KEY_MASTER_PASSWORD = "PASSWORD_MANAGER_MASTER_PASSWORD";

  private final CipherFactory cipherFactory;
  private final char[] masterPassword;

  private PasswordManager(final CipherFactory cipherFactory, final char[] masterPassword) {
    this.cipherFactory = cipherFactory;
    this.masterPassword = masterPassword;
  }

  /**
   * Creates a new instance of the {@code PasswordManager} class using the default master password for the user.
   *
   * <p>
   * If the user has a saved master password, it will be used. Otherwise, a new master password will be created for the
   * user and saved.
   * </p>
   *
   * @return A new password manager; never {@code null}.
   *
   * @throws GeneralSecurityException If no saved master password exists and a new master password cannot be created.
   */
  static PasswordManager newInstance() throws GeneralSecurityException {
    return newInstance(getMasterPassword());
  }

  @VisibleForTesting
  static PasswordManager newInstance(final char[] masterPassword) {
    return new PasswordManager(() -> Cipher.getInstance(CIPHER_ALGORITHM), masterPassword);
  }

  private static char[] getMasterPassword() throws GeneralSecurityException {
    return getMasterPassword(Preferences.userNodeForPackage(PasswordManager.class));
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
    final CharBuffer cb = MASTER_PASSWORD_CHARSET.decode(ByteBuffer.wrap(encodedMasterPassword));
    final char[] masterPassword = new char[cb.length()];
    cb.get(masterPassword);
    scrub(cb);
    return masterPassword;
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
    final ByteBuffer bb = MASTER_PASSWORD_CHARSET.encode(CharBuffer.wrap(masterPassword));
    final byte[] encodedMasterPassword = new byte[bb.remaining()];
    bb.get(encodedMasterPassword);
    scrub(bb);
    return encodedMasterPassword;
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

    final byte[] plaintext = decodePlaintext(unprotectedPassword);
    final byte[] salt = newSalt();
    final byte[] ciphertext = encrypt(plaintext, salt);
    scrub(plaintext);
    return encodeCiphertextAndSalt(ciphertext, salt);
  }

  private static byte[] decodePlaintext(final String encodedPlaintext) {
    return encodedPlaintext.getBytes(PLAINTEXT_CHARSET);
  }

  private static byte[] newSalt() throws GeneralSecurityException {
    // https://crackstation.net/hashing-security.htm#salt
    final int saltLengthForSha512InBytes = 64;

    final byte[] salt = new byte[saltLengthForSha512InBytes];
    fillRandom(salt);
    return salt;
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

  private byte[] encrypt(final byte[] plaintext, final byte[] salt) throws GeneralSecurityException {
    return newCipher(Cipher.ENCRYPT_MODE, salt).doFinal(plaintext);
  }

  private Cipher newCipher(final int opmode, final byte[] salt) throws GeneralSecurityException {
    final Cipher cipher = cipherFactory.create();
    cipher.init(opmode, newSecretKey(salt));
    return cipher;
  }

  private static String encodeCiphertextAndSalt(final byte[] ciphertext, final byte[] salt) {
    final String encodedCiphertext = Base64.getEncoder().encodeToString(ciphertext);
    final String encodedSalt = Base64.getEncoder().encodeToString(salt);
    return encodedSalt + "." + encodedCiphertext;
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

    final CiphertextAndSalt ciphertextAndSalt = decodeCiphertextAndSalt(protectedPassword);
    final byte[] plaintext = decrypt(ciphertextAndSalt.ciphertext, ciphertextAndSalt.salt);
    final String unprotectedPassword = encodePlaintext(plaintext);
    scrub(plaintext);
    return unprotectedPassword;
  }

  private static CiphertextAndSalt decodeCiphertextAndSalt(final String encodedCiphertextAndSalt)
      throws GeneralSecurityException {
    final String[] components = encodedCiphertextAndSalt.split("\\.");
    if (components.length != 2) {
      throw new GeneralSecurityException("malformed protected password");
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

  private static String encodePlaintext(final byte[] plaintext) {
    return new String(plaintext, PLAINTEXT_CHARSET);
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
