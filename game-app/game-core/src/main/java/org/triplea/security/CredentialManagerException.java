package org.triplea.security;

/**
 * A checked exception that indicates an error occurred while using a {@link CredentialManager}
 * (e.g. the failure to protect/unprotect a credential).
 */
public final class CredentialManagerException extends Exception {
  private static final long serialVersionUID = -110629801418732489L;

  public CredentialManagerException(final String message) {
    super(message);
  }

  public CredentialManagerException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
