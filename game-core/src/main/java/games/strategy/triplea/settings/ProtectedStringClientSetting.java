package games.strategy.triplea.settings;

import java.util.Arrays;

import javax.annotation.Nullable;

import games.strategy.security.CredentialManager;
import games.strategy.security.CredentialManagerException;

/**
 * ClientSetting to store encrypted versions of potentially sensitive Strings.
 */
class ProtectedStringClientSetting extends ClientSetting<char[]> {

  private final boolean sensitive;

  ProtectedStringClientSetting(final String name, final boolean sensitive) {
    super(char[].class, name);
    this.sensitive = sensitive;
  }

  /**
   * Protects the given char array and cleans the content afterwards.
   */
  @Nullable
  @Override
  protected String formatValue(final @Nullable char[] value) {
    if (value == null) {
      return null;
    }
    try (CredentialManager manager = CredentialManager.newInstance()) {
      return manager.protect(value);
    } catch (final CredentialManagerException e) {
      throw new IllegalStateException("Error while trying to protect String.", e);
    } finally {
      Arrays.fill(value, '\0');
    }
  }

  @Nullable
  @Override
  protected char[] parseValue(final @Nullable String encodedValue) {
    if (encodedValue == null) {
      return null;
    }
    try (CredentialManager manager = CredentialManager.newInstance()) {
      return manager.unprotect(encodedValue);
    } catch (final CredentialManagerException e) {
      throw new IllegalStateException("Error while trying to unprotect string '" + encodedValue + "'.", e);
    }
  }

  @Override
  public String getDisplayValue() {
    final char[] string = getValue().orElse(new char[0]);
    if (sensitive) {
      Arrays.fill(string, '*');
    }
    return new String(string);
  }
}
