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
import java.util.function.Function;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import games.strategy.util.Util;

public class RsaAuthenticator {
  private static final Map<String, PrivateKey> rsaKeyMap = new HashMap<>();
  private static final String RSA = "RSA";
  private static final String RSA_ECB_OAEPP = RSA + "/ECB/OAEPPadding";
  static final String ENCRYPTED_PASSWORD_KEY = "RSAPWD";
  static final String RSA_PUBLIC_KEY = "RSAPUBLICKEY";

  static boolean canProcessResponse(final Map<String, String> response) {
    return response.get(ENCRYPTED_PASSWORD_KEY) != null;
  }

  public static boolean canProcessChallenge(final Map<String, String> challenge) {
    return challenge.get(RSA_PUBLIC_KEY) != null;
  }

  static void appendPublicKey(final Map<String, String> challenge) {
    challenge.put(RSA_PUBLIC_KEY, generateKeypair());
  }

  private static String generateKeypair() {
    try {
      final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA);
      keyGen.initialize(4096);
      final KeyPair keyPair = keyGen.generateKeyPair();
      final String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
      rsaKeyMap.put(publicKey, keyPair.getPrivate());
      return publicKey;
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(RSA + " is an invalid algorithm!", e);
    }
  }

  private static String decryptPassword(final String base64, final String publicKey,
      final Function<String, String> function) {
    Preconditions.checkNotNull(base64);
    try {
      final Cipher cipher = Cipher.getInstance(RSA_ECB_OAEPP);
      cipher.init(Cipher.DECRYPT_MODE, rsaKeyMap.get(publicKey));
      return function.apply(new String(cipher.doFinal(Base64.getDecoder().decode(base64)), StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      throw new IllegalStateException(e);
    } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
      return e.getMessage();
    } finally {
      rsaKeyMap.remove(publicKey);
    }
  }

  public static String authenticate(final Map<String, String> challenge, final Map<String, String> response,
      final Function<String, String> successFullDecryptionAction) {
    return decryptPassword(response.get(RSA_PUBLIC_KEY), challenge.get(RSA_PUBLIC_KEY), successFullDecryptionAction);
  }


  @VisibleForTesting
  static String encryptPassword(final String base64, final String password) {
    try {
      final PublicKey publicKey =
          KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64)));
      final Cipher cipher = Cipher.getInstance(RSA_ECB_OAEPP);
      cipher.init(Cipher.ENCRYPT_MODE, publicKey);
      return Base64.getEncoder().encodeToString(cipher.doFinal(Util.sha512(password).getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  public static void appendEncryptedPassword(
      final Map<String, String> response,
      final Map<String, String> challenge,
      final String password) {
    response.put(ENCRYPTED_PASSWORD_KEY, encryptPassword(challenge.get(RSA_PUBLIC_KEY), password));
  }
}
