package games.strategy.triplea.settings;

import games.strategy.security.CredentialManager;
import games.strategy.security.CredentialManagerException;

public class ProtectedStringClientSetting extends ClientSetting<String> {

  ProtectedStringClientSetting(final String name, final String defaultValue) {
    super(String.class, name, defaultValue);
  }


  @Override
  protected String formatValue(String value) {
    try (CredentialManager manager = CredentialManager.newInstance()) {
      return manager.protect(value);
    } catch (final CredentialManagerException e) {
      throw new IllegalStateException("CredentialManager needs to be available in order to protect strings.", e);
    }
  }

  @Override
  protected String parseValue(String encodedValue) {
    try (CredentialManager manager = CredentialManager.newInstance()) {
      return manager.unprotectToString(encodedValue);
    } catch (final CredentialManagerException e) {
      throw new IllegalStateException("CredentialManager needs to be available in order to unprotect strings.", e);
    }
  }
}
