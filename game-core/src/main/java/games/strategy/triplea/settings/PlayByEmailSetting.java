package games.strategy.triplea.settings;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.github.openjson.JSONObject;

import games.strategy.security.CredentialManager;
import games.strategy.security.CredentialManagerException;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class PlayByEmailSetting extends ClientSetting<PlayByEmailSetting.UserEmailConfiguration> {

  enum KnownEmailServerConfigurations {
    GMAIL("GMail", new EmailProviderSetting("smtp.gmail.com", 587, true)),
    HOTMAIL("Hotmail", new EmailProviderSetting("smtp.live.com", 587, true));

    private final String name;
    private final EmailProviderSetting property;

    KnownEmailServerConfigurations(final String name, final EmailProviderSetting property) {
      this.name = name;
      this.property = property;
    }

    EmailProviderSetting getEmailProviderInfo() {
      return property;
    }

    @Override
    public String toString() {
      return name;
    }
  }


  PlayByEmailSetting(final String name, final UserEmailConfiguration defaultValue) {
    super(UserEmailConfiguration.class, name, defaultValue);
  }

  PlayByEmailSetting(final String name) {
    this(name, null);
  }

  @Override
  protected String formatValue(final UserEmailConfiguration value) {
    return value == null ? "" : value.toString();
  }

  @Override
  protected UserEmailConfiguration parseValue(final String encodedValue) {
    return encodedValue.isEmpty() ? null : UserEmailConfiguration.parse(encodedValue);
  }

  @Getter
  @AllArgsConstructor
  @Immutable
  public static final class EmailProviderSetting {
    @Nonnull
    private final String host;
    @Nonnull
    private final int port;
    @Nonnull
    private final boolean isEncrypted;

    static EmailProviderSetting parse(final String encodedValue) {
      final JSONObject object = new JSONObject(encodedValue);
      return new EmailProviderSetting(
          object.getString("host"),
          object.getInt("port"),
          object.getBoolean("secure"));
    }

    @Override
    public String toString() {
      final JSONObject object = new JSONObject();
      object.put("host", host);
      object.put("port", port);
      object.put("secure", isEncrypted);
      return object.toString();
    }
  }

  public static final class UserEmailConfiguration {
    private final EmailProviderSetting setting;
    private final String username;
    private final String password;

    public UserEmailConfiguration(final EmailProviderSetting setting, final String username, final String password) {
      this(setting, username, password, true);
    }

    private UserEmailConfiguration(final EmailProviderSetting setting, final String username, final String password,
        final boolean protect) {
      this.setting = setting;
      if (protect) {
        try (CredentialManager manager = CredentialManager.newInstance()) {
          this.username = manager.protect(username);
          this.password = manager.protect(password);
        } catch (final CredentialManagerException e) {
          throw new IllegalStateException("CredentialManager needs to be available to securely store passwords.", e);
        }
      } else {
        this.username = username;
        this.password = password;
      }
    }

    public EmailProviderSetting getEmailProviderSetting() {
      return setting;
    }

    public String getUsername() {
      try (CredentialManager manager = CredentialManager.newInstance()) {
        return manager.unprotectToString(username);
      } catch (CredentialManagerException e) {
        throw new IllegalStateException("CredentialManager needs to be available in order to unprotect username.", e);
      }
    }

    public String getPassword() {
      try (CredentialManager manager = CredentialManager.newInstance()) {
        return manager.unprotectToString(password);
      } catch (CredentialManagerException e) {
        throw new IllegalStateException("CredentialManager needs to be available in order to unprotect passwords.", e);
      }
    }

    static UserEmailConfiguration parse(final String encodedValue) {
      final JSONObject object = new JSONObject(encodedValue);
      return new UserEmailConfiguration(
          EmailProviderSetting.parse(object.getString("configuration")),
          object.getString("username"),
          object.getString("password"),
          false);
    }

    @Override
    public String toString() {
      final JSONObject object = new JSONObject();
      object.put("configuration", setting.toString());
      object.put("username", username);
      object.put("password", password);
      return object.toString();
    }
  }
}
