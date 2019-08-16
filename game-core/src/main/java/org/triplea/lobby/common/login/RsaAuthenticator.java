package org.triplea.lobby.common.login;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * A class which implements the TripleA-Lobby-Login authentication system using RSA encryption for
 * passwords.
 */
public final class RsaAuthenticator {
  private static final String RSA = "RSA";
  private static final String RSA_ECB_OAEPP = RSA + "/ECB/OAEPPadding";
  private static final String PSEUDO_SALT = "TripleA";
  private static final String SHA_512 = "SHA-512";

  private final KeyPair keyPair;

  public RsaAuthenticator() {
    this(generateKeyPair());
  }

  @VisibleForTesting
  public RsaAuthenticator(final KeyPair keyPair) {
    this.keyPair = keyPair;
  }

  /** Returns true if the specified map contains the required values. */
  public static boolean canProcessResponse(final Map<String, String> response) {
    return response.containsKey(LobbyLoginResponseKeys.RSA_ENCRYPTED_PASSWORD);
  }

  /**
   * Creates a new challenge for the lobby server to send to the lobby client.
   *
   * @return The challenge as a collection of properties to be added to the message the lobby server
   *     sends the lobby client.
   */
  public Map<String, String> newChallenge() {
    return Collections.singletonMap(
        LobbyLoginChallengeKeys.RSA_PUBLIC_KEY,
        Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
  }

  private static KeyPair generateKeyPair() {
    try {
      final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA);
      keyGen.initialize(4096);
      return keyGen.generateKeyPair();
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException(RSA + " is an invalid algorithm!", e);
    }
  }

  /**
   * Decrypts the password contained in the specified response and provides it to the specified
   * action for further processing.
   *
   * @param response The response map containing the encrypted password.
   * @param action A {@link Function} which is executed if the password is successfully decrypted.
   * @return The result of {@code action} if the password is decrypted successfully; otherwise a
   *     message describing the error that occurred during decryption.
   * @throws IllegalStateException If the encryption cipher is not available.
   */
  public String decryptPasswordForAction(
      final Map<String, String> response, final Function<String, String> action) {
    final String encryptedPassword = response.get(LobbyLoginResponseKeys.RSA_ENCRYPTED_PASSWORD);
    try {
      final Cipher cipher = Cipher.getInstance(RSA_ECB_OAEPP);
      cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
      return action.apply(
          new String(
              cipher.doFinal(Base64.getDecoder().decode(encryptedPassword)),
              StandardCharsets.UTF_8));
    } catch (final NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new IllegalStateException(e);
    } catch (final InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
      return e.getMessage();
    }
  }

  /**
   * Returns UTF-8 encoded bytes of a "salted" SHA-512 hash of the given input string. See {@link
   * #hashPasswordWithSalt(String)} for more information.
   */
  private static byte[] getHashedBytes(final String input) {
    return hashPasswordWithSalt(input).getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Creates a SHA-512 hash of a given String with a salt. <br>
   * The server doesn't need to know the actual password, so this hash essentially replaces the real
   * password. In case any other server authentication system SHA-512 hashes passwords before
   * sending them, we are applying a 'TripleA' prefix to the given String before hashing. This way
   * the hash cannot be used on other websites even if the password and the authentication system is
   * the same.
   *
   * @param password The input String to hash.
   * @return A hashed hexadecimal String of the input.
   */
  public static String hashPasswordWithSalt(final String password) {
    Preconditions.checkNotNull(password);
    return sha512(PSEUDO_SALT + password);
  }

  /** Creates a SHA-512 hash of the given String. */
  @VisibleForTesting
  static String sha512(final String input) {
    try {
      return BaseEncoding.base16()
          .encode(MessageDigest.getInstance(SHA_512).digest(input.getBytes(StandardCharsets.UTF_8)))
          .toLowerCase();
    } catch (final NoSuchAlgorithmException e) {
      throw new IllegalStateException(SHA_512 + " is not supported!", e);
    }
  }

  /**
   * Creates a response to the specified challenge for the lobby client to send to the lobby server.
   *
   * @param rsaPublicKey Public key string sent from server.
   * @param password The lobby client's password.
   * @return Encrypted password using provided rsa public key.
   */
  public static String encrpytPassword(final String rsaPublicKey, final String password) {
    try {
      final PublicKey publicKey =
          KeyFactory.getInstance(RSA)
              .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(rsaPublicKey)));
      final Cipher cipher = Cipher.getInstance(RSA_ECB_OAEPP);
      cipher.init(Cipher.ENCRYPT_MODE, publicKey);
      return Base64.getEncoder().encodeToString(cipher.doFinal(getHashedBytes(password)));
    } catch (final GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }
}
