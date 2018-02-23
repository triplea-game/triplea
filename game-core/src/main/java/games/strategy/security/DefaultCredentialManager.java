package games.strategy.security;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.prefs.Preferences;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.common.annotations.VisibleForTesting;

/**
 * Default implementation of {@link CredentialManager}.
 *
 * <p>
 * This implementation stores the user's master password unencrypted in the user's preference tree. Thus, the master
 * password is in an area of the user's system that has the required permissions to ensure access only by the user. The
 * master password will automatically be created if it does not exist.
 * </p>
 */
final class DefaultCredentialManager implements CredentialManager {
  private static final String CIPHER_ALGORITHM = "AES";

  @VisibleForTesting
  static final String PREFERENCE_KEY_MASTER_PASSWORD = "DEFAULT_CREDENTIAL_MANAGER_MASTER_PASSWORD";

  private final char[] masterPassword;

  private DefaultCredentialManager(final char[] masterPassword) {
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
   * @return A new credential manager.
   *
   * @throws CredentialManagerException If no saved master password exists and a new master password cannot be created.
   */
  static DefaultCredentialManager newInstance() throws CredentialManagerException {
    try {
      return newInstance(getMasterPassword());
    } catch (final GeneralSecurityException e) {
      throw new CredentialManagerException("failed to create credential manager", e);
    }
  }

  @VisibleForTesting
  static DefaultCredentialManager newInstance(final char[] masterPassword) {
    return new DefaultCredentialManager(masterPassword);
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

  private static @Nullable char[] loadMasterPassword(final Preferences preferences) {
    final byte[] encodedMasterPassword = preferences.getByteArray(PREFERENCE_KEY_MASTER_PASSWORD, null);
    if (encodedMasterPassword == null) {
      return null;
    }

    final char[] masterPassword = decodeBytesToChars(encodedMasterPassword);
    scrub(encodedMasterPassword);
    return masterPassword;
  }

  private static char[] decodeBytesToChars(final byte[] bytes) {
    final CharBuffer cb = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes));
    final char[] chars = new char[cb.length()];
    cb.get(chars);
    scrub(cb);
    return chars;
  }

  private static char[] newMasterPassword() throws GeneralSecurityException {
    // https://en.wikipedia.org/wiki/Password_strength#Random_passwords
    // Base64 ~~ Case sensitive alphanumeric
    final int base64DigitCountFor160BitEntropy = 27;
    final int dataBitsPerBase64Digit = 6;
    final int totalBitsPerBase64Digit = 8;
    final int byteLengthForDesiredEntropy =
        ((base64DigitCountFor160BitEntropy * dataBitsPerBase64Digit) / totalBitsPerBase64Digit) + 1;

    final byte[] randomBytes = new byte[byteLengthForDesiredEntropy];
    fillRandom(randomBytes);
    final byte[] encodedMasterPassword = Base64.getEncoder().encode(randomBytes);
    scrub(randomBytes);
    final char[] masterPassword = decodeBytesToChars(encodedMasterPassword);
    scrub(encodedMasterPassword);
    return masterPassword;
  }

  private static void fillRandom(final byte[] bytes) throws GeneralSecurityException {
    SecureRandom.getInstance("SHA1PRNG").nextBytes(bytes);
  }

  private static void saveMasterPassword(final Preferences preferences, final char[] masterPassword) {
    preferences.putByteArray(PREFERENCE_KEY_MASTER_PASSWORD, encodeCharsToBytes(masterPassword));
  }

  @VisibleForTesting
  static byte[] encodeCharsToBytes(final char[] chars) {
    final ByteBuffer bb = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
    final byte[] bytes = new byte[bb.remaining()];
    bb.get(bytes);
    scrub(bb);
    return bytes;
  }

  @Override
  public void close() {
    scrub(masterPassword);
  }

  @Override
  public String protect(final String unprotectedCredentialAsString) throws CredentialManagerException {
    checkNotNull(unprotectedCredentialAsString);

    final char[] unprotectedCredential = unprotectedCredentialAsString.toCharArray();
    try {
      return protect(unprotectedCredential);
    } finally {
      scrub(unprotectedCredential);
    }
  }

  @Override
  public String protect(final char[] unprotectedCredential) throws CredentialManagerException {
    checkNotNull(unprotectedCredential);

    try {
      final byte[] plaintext = encodeCharsToBytes(unprotectedCredential);
      final byte[] salt = newSalt();
      final byte[] ciphertext = encrypt(plaintext, salt);
      scrub(plaintext);
      return formatProtectedCredential(ciphertext, salt);
    } catch (final GeneralSecurityException e) {
      throw new CredentialManagerException("failed to protect credential", e);
    }
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
    final Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
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

  private static String formatProtectedCredential(final byte[] ciphertext, final byte[] salt) {
    final String encodedCiphertext = Base64.getEncoder().encodeToString(ciphertext);
    final String encodedSalt = Base64.getEncoder().encodeToString(salt);
    return encodedSalt + "." + encodedCiphertext;
  }

  @Override
  public String unprotectToString(final String protectedCredential) throws CredentialManagerException {
    checkNotNull(protectedCredential);

    final char[] unprotectedCredential = unprotect(protectedCredential);
    final String unprotectedCredentialAsString = new String(unprotectedCredential);
    scrub(unprotectedCredential);
    return unprotectedCredentialAsString;
  }

  @Override
  public char[] unprotect(final String protectedCredential) throws CredentialManagerException {
    checkNotNull(protectedCredential);

    try {
      final CiphertextAndSalt ciphertextAndSalt = parseProtectedCredential(protectedCredential);
      final byte[] plaintext = decrypt(ciphertextAndSalt.ciphertext, ciphertextAndSalt.salt);
      final char[] unprotectedCredential = decodeBytesToChars(plaintext);
      scrub(plaintext);
      return unprotectedCredential;
    } catch (final GeneralSecurityException e) {
      throw new CredentialManagerException("failed to unprotect credential", e);
    }
  }

  private static CiphertextAndSalt parseProtectedCredential(final String protectedCredential)
      throws CredentialManagerException {
    final String[] components = protectedCredential.split("\\.");
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
}
