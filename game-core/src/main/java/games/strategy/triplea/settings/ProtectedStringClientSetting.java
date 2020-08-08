package games.strategy.triplea.settings;

import com.google.common.annotations.VisibleForTesting;
import org.triplea.java.function.ThrowingBiFunction;
import org.triplea.security.CredentialManager;
import org.triplea.security.CredentialManagerException;

/** ClientSetting to store encrypted versions of potentially sensitive Strings. */
final class ProtectedStringClientSetting extends ClientSetting<char[]> {
  ProtectedStringClientSetting(final String name) {
    super(char[].class, name);
  }

  @Override
  protected String encodeValue(final char[] value) throws ValueEncodingException {
    return withCredentialManager(value, ProtectedStringClientSetting::encodeValue);
  }

  @VisibleForTesting
  static String encodeValue(final char[] value, final CredentialManager credentialManager)
      throws ValueEncodingException {
    try {
      return credentialManager.protect(value);
    } catch (final CredentialManagerException e) {
      throw new ValueEncodingException("Error while trying to protect string", e);
    }
  }

  private static <T, R> R withCredentialManager(
      final T value,
      final ThrowingBiFunction<T, CredentialManager, R, ValueEncodingException> function)
      throws ValueEncodingException {
    try (CredentialManager credentialManager = CredentialManager.newInstance()) {
      return function.apply(value, credentialManager);
    } catch (final CredentialManagerException e) {
      throw new ValueEncodingException("Failed to create credential manager", e);
    }
  }

  @Override
  protected char[] decodeValue(final String encodedValue) throws ValueEncodingException {
    return withCredentialManager(encodedValue, ProtectedStringClientSetting::decodeValue);
  }

  @VisibleForTesting
  static char[] decodeValue(final String encodedValue, final CredentialManager credentialManager)
      throws ValueEncodingException {
    try {
      return credentialManager.unprotect(encodedValue);
    } catch (final CredentialManagerException e) {
      throw new ValueEncodingException(
          "Error while trying to unprotect string '" + encodedValue + "'", e);
    }
  }
}
