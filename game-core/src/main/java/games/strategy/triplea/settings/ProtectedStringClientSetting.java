package games.strategy.triplea.settings;

import java.util.Optional;

import javax.annotation.Nullable;

import games.strategy.security.CredentialManager;
import games.strategy.security.CredentialManagerException;

  public class ProtectedStringClientSetting extends ClientSetting<String> {

  ProtectedStringClientSetting(final String name, final String defaultValue) {
    super(String.class, name, protect(defaultValue));
  }

  /**
   * Static helper method to be used in the constructor
   * to be applied before passing the string to the super constructor.
   */
  @Nullable
  private static String protect(final @Nullable String value) {
    if (value == null) {
      return null;
    }
    try (CredentialManager manager = CredentialManager.newInstance()) {
      return manager.protect(value);
    } catch (final CredentialManagerException e) {
      throw new IllegalStateException("Error while trying to protect String.", e);
    }
  }


  @Nullable
  @Override
  protected String formatValue(final @Nullable String value) {
    return protect(value);
  }

  @Nullable
  @Override
  protected String parseValue(final @Nullable String encodedValue) {
    if (encodedValue == null) {
      return null;
    }
    try (CredentialManager manager = CredentialManager.newInstance()) {
      return manager.unprotectToString(encodedValue);
    } catch (final CredentialManagerException e) {
      throw new IllegalStateException("Error while trying to unprotect string '" + encodedValue + "'.", e);
    }
  }

  @Nullable
  @Override
  public Optional<String> getDefaultValue() {
    return super.getDefaultValue().map(this::parseValue);
  }
}
