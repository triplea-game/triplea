package org.triplea.test.common.security;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import org.jetbrains.annotations.NonNls;

/** A collection of useful methods for writing tests that involve security functions. */
public final class TestSecurityUtils {
  @NonNls private static final String RSA_ALGORITHM = "RSA";

  private TestSecurityUtils() {}

  /**
   * Loads the global RSA key pair.
   *
   * @return The global RSA key pair.
   * @throws IOException If an I/O error occurs while reading the RSA key pair.
   * @throws GeneralSecurityException If the RSA key pair cannot be created from the persistent
   *     data.
   */
  public static KeyPair loadRsaKeyPair() throws IOException, GeneralSecurityException {
    return loadRsaKeyPair(TestSecurityUtils.class);
  }

  /**
   * Loads the RSA key pair for the specified type.
   *
   * @param type The type whose RSA key pair is to be loaded.
   * @return The RSA key pair for the specified type.
   * @throws IOException If an I/O error occurs while reading the RSA key pair.
   * @throws GeneralSecurityException If the RSA key pair cannot be created from the persistent
   *     data.
   */
  public static KeyPair loadRsaKeyPair(final Class<?> type)
      throws IOException, GeneralSecurityException {
    checkNotNull(type);

    final KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);

    final X509EncodedKeySpec publicKeySpec;
    try (InputStream is = type.getResourceAsStream(getPublicKeyFileName(type))) {
      publicKeySpec = new X509EncodedKeySpec(ByteStreams.toByteArray(is));
    }
    final PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

    final PKCS8EncodedKeySpec privateKeySpec;
    try (InputStream is = type.getResourceAsStream(getPrivateKeyFileName(type))) {
      privateKeySpec = new PKCS8EncodedKeySpec(ByteStreams.toByteArray(is));
    }
    final PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

    return new KeyPair(publicKey, privateKey);
  }

  private static String getPublicKeyFileName(final Class<?> type) {
    return type.getSimpleName() + "-public.key";
  }

  private static String getPrivateKeyFileName(final Class<?> type) {
    return type.getSimpleName() + "-private.key";
  }
}
