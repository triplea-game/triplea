package org.triplea.security;

/**
 * Provides a facility to protect a credential before writing it to storage and to subsequently
 * unprotect a protected credential after reading it from storage.
 *
 * <p>The credential manager uses a user-specific master password to protect individual credentials.
 * An implementation may automatically create and manager a master password instead of requiring the
 * user to specify one explicitly. In such a case, the implementation must ensure the master
 * password is appropriately protected such that it can only be accessed by the user.
 */
public interface CredentialManager extends AutoCloseable {
  /** Scrubs the master password from memory. */
  @Override
  void close();

  /**
   * Protects the unprotected credential contained in the specified string.
   *
   * <p><strong>IT IS STRONGLY RECOMMENDED TO USE {@link #protect(char[])} INSTEAD!</strong> Strings
   * are immutable and the secret data contained in the argument cannot be scrubbed. This data may
   * then be leaked outside of this process (e.g. if memory is paged to disk).
   *
   * @param unprotectedCredentialAsString The unprotected credential as a string.
   * @return The protected credential.
   * @throws CredentialManagerException If the unprotected credential cannot be protected.
   * @see #unprotectToString(String)
   */
  String protect(String unprotectedCredentialAsString) throws CredentialManagerException;

  /**
   * Protects the unprotected credential contained in the specified character array.
   *
   * @param unprotectedCredential The unprotected credential as a character array.
   * @return The protected credential.
   * @throws CredentialManagerException If the unprotected credential cannot be protected.
   * @see #unprotect(String)
   */
  String protect(char[] unprotectedCredential) throws CredentialManagerException;

  /**
   * Unprotects the specified protected credential into a string.
   *
   * <p><strong>IT IS STRONGLY RECOMMENDED TO USE {@link #unprotect(String)} INSTEAD!</strong>
   * Strings are immutable and the secret data contained in the return value cannot be scrubbed.
   * This data may then be leaked outside of this process (e.g. if memory is paged to disk).
   *
   * @param protectedCredential The protected credential previously created by {@link
   *     #protect(String)}.
   * @return The unprotected credential as a string.
   * @throws CredentialManagerException If the protected credential cannot be unprotected.
   * @see #protect(String)
   */
  String unprotectToString(String protectedCredential) throws CredentialManagerException;

  /**
   * Unprotects the specified protected credential into a character array.
   *
   * @param protectedCredential The protected credential previously created by {@link
   *     #protect(char[])}.
   * @return The unprotected credential as a character array.
   * @throws CredentialManagerException If the protected credential cannot be unprotected.
   * @see #protect(char[])
   */
  char[] unprotect(String protectedCredential) throws CredentialManagerException;

  /**
   * Creates a new credential manager using the default implementation with the default master
   * password for the user.
   *
   * <p>If the user has a saved master password, it will be used. Otherwise, a new master password
   * will be created for the user and saved.
   *
   * @return A new credential manager.
   * @throws CredentialManagerException If no saved master password exists and a new master password
   *     cannot be created.
   */
  static CredentialManager newInstance() throws CredentialManagerException {
    return DefaultCredentialManager.newInstance();
  }
}
