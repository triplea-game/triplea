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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import games.strategy.util.Util;

/**
 * A class which implements the TripleA-Lobby-Login authentication system using RSA encryption
 * for passwords.
 */
public class RsaAuthenticator {
  private static final TimeoutKeyMap rsaKeyMap = new TimeoutKeyMap();
  private static final String RSA = "RSA";
  private static final String RSA_ECB_OAEPP = RSA + "/ECB/OAEPPadding";
  static final String ENCRYPTED_PASSWORD_KEY = "RSAPWD";
  static final String RSA_PUBLIC_KEY = "RSAPUBLICKEY";

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
    } while (rsaKeyMap.containsKey(publicKey));
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

  private static String decryptPassword(final String encryptedPassword, final String publicKey,
      final Function<String, String> function) {
    Preconditions.checkNotNull(encryptedPassword);
    final PrivateKey privateKey = rsaKeyMap.get(publicKey);
    if (privateKey == null) {
      return "Login timeout, try again!";
    }
    try {
      final Cipher cipher = Cipher.getInstance(RSA_ECB_OAEPP);
      cipher.init(Cipher.DECRYPT_MODE, privateKey);
      return function
          .apply(new String(cipher.doFinal(Base64.getDecoder().decode(encryptedPassword)), StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new IllegalStateException(e);
    } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
      return e.getMessage();
    } finally {
      rsaKeyMap.remove(publicKey);
    }
  }

  /**
   * Attempts to decrypt the given password using the challenge and response parameters.
   */
  public static String decryptPassword(final Map<String, String> challenge, final Map<String, String> response,
      final Function<String, String> successFullDecryptionAction) {
    return decryptPassword(response.get(ENCRYPTED_PASSWORD_KEY), challenge.get(RSA_PUBLIC_KEY),
        successFullDecryptionAction);
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
  static void setTimeout(final long amount, final TimeUnit timeUnit) {
    rsaKeyMap.setTimeout(amount, timeUnit);
  }

  private static class TimeoutKeyMap extends HashMap<String, PrivateKey> {
    private static final long serialVersionUID = 3788873489149542052L;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, ScheduledFuture<?>> scheduledTaks = new HashMap<>();
    private long amount;
    private TimeUnit timeUnit;

    private TimeoutKeyMap() {
      setTimeout(10, TimeUnit.MINUTES);
    }

    @Override
    public PrivateKey put(final String string, final PrivateKey key) {
      scheduledTaks.put(string, scheduler.schedule(() -> {
        super.remove(string);
      }, amount, timeUnit));
      return super.put(string, key);
    }

    @Override
    public PrivateKey remove(final Object object) {
      if (object instanceof String) {
        final ScheduledFuture<?> future = scheduledTaks.get((String) object);
        if (future != null) {
          future.cancel(false);
        }
        scheduledTaks.remove(object);
      }
      return super.remove(object);
    }

    private void setTimeout(final long amount, final TimeUnit timeUnit) {
      this.amount = amount;
      this.timeUnit = timeUnit;
    }
  }
}
