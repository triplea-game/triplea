package games.strategy.triplea.settings;

import java.util.Arrays;

import javax.annotation.Nullable;

import games.strategy.security.CredentialManager;
import games.strategy.security.CredentialManagerException;

/**
 * ClientSetting to store encrypted versions of potentially sensitive Strings.
 */
class ProtectedStringClientSetting extends ClientSetting<String> {

  private final boolean sensitive;

  ProtectedStringClientSetting(final String name, final boolean sensitive) {
    super(String.class, name);
    this.sensitive = sensitive;
  }

  @Nullable
  @Override
  protected String formatValue(final @Nullable String value) {
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

  @Override
  public String transformToDisplayValue(final Object object) {
    if (sensitive && object instanceof String) {
      final String string = (String) object;
      final char[] array = new char[string.length()];
      Arrays.fill(array, '*');
      return new String(array);
    }
    return super.transformToDisplayValue(object);
  }
}
