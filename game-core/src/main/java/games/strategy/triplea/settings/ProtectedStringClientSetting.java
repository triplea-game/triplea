package games.strategy.triplea.settings;

import java.util.Optional;

import games.strategy.security.CredentialManager;
import games.strategy.security.CredentialManagerException;

class ProtectedStringClientSetting extends StringClientSetting {

  ProtectedStringClientSetting(final String name, final String defaultValue) {
    super(name, protect(defaultValue));
  }

  private static String protect(String value) {
    try (CredentialManager manager = CredentialManager.newInstance()) {
      return manager.protect(value);
    } catch (final CredentialManagerException e) {
      throw new IllegalStateException("CredentialManager needs to be available in order to protect strings.", e);
    }
  }

  private static String unprotect(String encodedValue) {
    try (CredentialManager manager = CredentialManager.newInstance()) {
      return manager.unprotectToString(encodedValue);
    } catch (final CredentialManagerException e) {
      throw new IllegalStateException("CredentialManager needs to be available in order to unprotect strings.", e);
    }
  }

  @Override
  public void setValue(final String value) {
    super.setValue(protect(value));
  }

  @Override
  public Optional<String> getValue() {
    return super.getValue().map(ProtectedStringClientSetting::unprotect);
  }

  @Override
  public Optional<String> getDefaultValue() {
    return super.getDefaultValue().map(ProtectedStringClientSetting::unprotect);
  }
}
