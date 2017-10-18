package games.strategy.security;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import com.google.common.io.ByteStreams;

/**
 * A collection of useful methods for writing tests that involve security functions.
 */
public final class TestSecurityUtils {
  private static final String RSA_ALGORITHM = "RSA";

  private TestSecurityUtils() {}

  /**
   * Generates a new RSA key pair with the specified key size.
   *
   * @param keySizeInBits The key size in bits.
   *
   * @return A new RSA key pair.
   *
   * @throws GeneralSecurityException If an error occurs while generating the RSA key pair.
   */
  public static KeyPair generateRsaKeyPair(final int keySizeInBits) throws GeneralSecurityException {
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
    keyPairGenerator.initialize(keySizeInBits);
    return keyPairGenerator.generateKeyPair();
  }

  /**
   * Loads the global RSA key pair.
   *
   * @return The global RSA key pair.
   *
   * @throws IOException If an I/O error occurs while reading the RSA key pair.
   * @throws GeneralSecurityException If the RSA key pair cannot be created from the persistent data.
   */
  public static KeyPair loadRsaKeyPair() throws IOException, GeneralSecurityException {
    return loadRsaKeyPair(TestSecurityUtils.class);
  }

  /**
   * Loads the RSA key pair for the specified type.
   *
   * @param type The type whose RSA key pair is to be loaded.
   *
   * @return The RSA key pair for the specified type.
   *
   * @throws IOException If an I/O error occurs while reading the RSA key pair.
   * @throws GeneralSecurityException If the RSA key pair cannot be created from the persistent data.
   */
  public static KeyPair loadRsaKeyPair(final Class<?> type) throws IOException, GeneralSecurityException {
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

  /**
   * Saves the global RSA key pair.
   *
   * @param keyPair The global RSA key pair to save.
   *
   * @throws IOException If an I/O error occurs while writing the RSA key pair.
   */
  public static void saveRsaKeyPair(final KeyPair keyPair) throws IOException {
    checkNotNull(keyPair);

    saveRsaKeyPair(TestSecurityUtils.class, keyPair);
  }

  /**
   * Saves the RSA key pair for the specified type.
   *
   * @param type The type whose RSA key pair is to be saved.
   * @param keyPair The RSA key pair to save.
   *
   * @throws IOException If an I/O error occurs while writing the RSA key pair.
   */
  public static void saveRsaKeyPair(final Class<?> type, final KeyPair keyPair) throws IOException {
    checkNotNull(type);
    checkNotNull(keyPair);

    final Path testResourcesPath = Paths.get("src", "test", "resources");
    final Path basePath = testResourcesPath.resolve(type.getPackage().getName().replace('.', File.separatorChar));
    Files.createDirectories(basePath);

    final Path publicKeySpecPath = basePath.resolve(getPublicKeyFileName(type));
    final X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(keyPair.getPublic().getEncoded());
    Files.write(publicKeySpecPath, publicKeySpec.getEncoded());

    final Path privateKeySpecPath = basePath.resolve(getPrivateKeyFileName(type));
    final PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(keyPair.getPrivate().getEncoded());
    Files.write(privateKeySpecPath, privateKeySpec.getEncoded());
  }
}
