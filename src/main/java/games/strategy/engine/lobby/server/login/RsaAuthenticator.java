package games.strategy.engine.lobby.server.login;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import games.strategy.util.Util;

/**
 * A class which implements the TripleA-Lobby-Login authentication system using RSA encryption
 * for passwords.
 */
public class RsaAuthenticator {
  private static final Cache<String, PrivateKey> rsaKeyMap = buildCache();
  private static final String RSA = "RSA";
  private static final String RSA_ECB_OAEPP = RSA + "/ECB/OAEPPadding";

  @VisibleForTesting
  static final String ENCRYPTED_PASSWORD_KEY = "RSAPWD";

  @VisibleForTesting
  static final String RSA_PUBLIC_KEY = "RSAPUBLICKEY";

  private static Cache<String, PrivateKey> buildCache() {
    return CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build();
  }

  /**
   * Returns true if the specified map contains the required values.
   */
  static boolean canProcessResponse(final Map<String, String> response) {
    return response.get(ENCRYPTED_PASSWORD_KEY) != null;
  }

  /**
   * Returns true if the specified map contains the required values.
   */
  public static boolean canProcessChallenge(final Map<String, String> challenge) {
    return challenge.get(RSA_PUBLIC_KEY) != null;
  }

  /**
   * Adds public key of a generated key-pair to the challenge map
   * and stores the private key in a map.
   */
  static void appendPublicKey(final Map<String, String> challenge) {
    challenge.put(RSA_PUBLIC_KEY, storeKeypair());
  }

  private static String storeKeypair() {
    KeyPair keyPair;
    String publicKey;
    do {
      keyPair = generateKeypair();
      publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    } while (rsaKeyMap.getIfPresent(publicKey) != null);
    rsaKeyMap.put(publicKey, keyPair.getPrivate());
    return publicKey;
  }

  private static KeyPair generateKeypair() {
    try {
      final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA);
      keyGen.initialize(4096);
      return keyGen.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(RSA + " is an invalid algorithm!", e);
    }
  }

  private static String decryptPassword(final String encryptedPassword, final PrivateKey privateKey,
      final Function<String, String> successfullDecryptionAction) {
    Preconditions.checkNotNull(encryptedPassword);
    Preconditions.checkNotNull(privateKey);
    try {
      final Cipher cipher = Cipher.getInstance(RSA_ECB_OAEPP);
      cipher.init(Cipher.DECRYPT_MODE, privateKey);
      return successfullDecryptionAction
          .apply(new String(cipher.doFinal(Base64.getDecoder().decode(encryptedPassword)), StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new IllegalStateException(e);
    } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
      return e.getMessage();
    }
  }

  /**
   * Attempts to decrypt the given password using the challenge and response parameters.
   */
  public static String decryptPasswordForAction(final Map<String, String> challenge, final Map<String, String> response,
      final Function<String, String> successFullDecryptionAction) {
    final String publicKey = challenge.get(RSA_PUBLIC_KEY);
    final PrivateKey privateKey = rsaKeyMap.getIfPresent(publicKey);
    if (privateKey == null) {
      return "Login timeout, try again!";
    } else {
      rsaKeyMap.invalidate(publicKey);
    }
    return decryptPassword(response.get(ENCRYPTED_PASSWORD_KEY), privateKey, successFullDecryptionAction);
  }


  @VisibleForTesting
  static String encryptPassword(final String publicKeyString, final String password) {
    try {
      final PublicKey publicKey =
          KeyFactory.getInstance(RSA)
              .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyString)));
      final Cipher cipher = Cipher.getInstance(RSA_ECB_OAEPP);
      cipher.init(Cipher.ENCRYPT_MODE, publicKey);
      return Base64.getEncoder().encodeToString(cipher.doFinal(Util.sha512(password).getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * This method adds the encrypted password (using the specified public key and password)
   * to the specified response map.
   */
  public static void appendEncryptedPassword(
      final Map<String, String> response,
      final Map<String, String> challenge,
      final String password) {
    response.put(ENCRYPTED_PASSWORD_KEY, encryptPassword(challenge.get(RSA_PUBLIC_KEY), password));
  }

  @VisibleForTesting
  static void invalidateAll() {
    rsaKeyMap.invalidateAll();
  }
}
