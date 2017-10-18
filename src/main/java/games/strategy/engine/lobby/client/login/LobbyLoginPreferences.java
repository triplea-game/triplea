package games.strategy.engine.lobby.client.login;

import java.util.Objects;
import java.util.prefs.Preferences;

import javax.annotation.concurrent.Immutable;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;

import games.strategy.debug.ClientLogger;
import games.strategy.security.CredentialManager;
import games.strategy.security.CredentialManagerException;

/**
 * The login preferences for a lobby user.
 */
@Immutable
public final class LobbyLoginPreferences {
  final String userName;
  final String password;
  final boolean credentialsSaved;
  final boolean anonymousLogin;

  LobbyLoginPreferences(
      final String userName,
      final String password,
      final boolean credentialsSaved,
      final boolean anonymousLogin) {
    this.userName = userName;
    this.password = password;
    this.credentialsSaved = credentialsSaved;
    this.anonymousLogin = anonymousLogin;
  }

  /**
   * Loads the user's current lobby login preferences.
   *
   * @return The user's current lobby login preferences.
   */
  public static LobbyLoginPreferences load() {
    return load(getPreferenceNode(), () -> CredentialManager.newInstance());
  }

  @VisibleForTesting
  static LobbyLoginPreferences load(
      final Preferences preferences,
      final CredentialManagerFactory credentialManagerFactory) {
    final String legacyUserName = preferences.get(PreferenceKeys.LEGACY_USER_NAME, "");
    final Credentials credentials = unprotectCredentials(
        new Credentials(
            preferences.get(PreferenceKeys.USER_NAME, legacyUserName),
            preferences.get(PreferenceKeys.PASSWORD, ""),
            preferences.getBoolean(PreferenceKeys.CREDENTIALS_PROTECTED, false)),
        credentialManagerFactory);
    final boolean credentialsSaved = preferences.getBoolean(PreferenceKeys.CREDENTIALS_SAVED, false);
    final boolean legacyAnonymousLogin = preferences.getBoolean(PreferenceKeys.LEGACY_ANONYMOUS_LOGIN, false);
    final boolean anonymousLogin = preferences.getBoolean(PreferenceKeys.ANONYMOUS_LOGIN, legacyAnonymousLogin);
    return new LobbyLoginPreferences(credentials.userName, credentials.password, credentialsSaved, anonymousLogin);
  }

  private static Preferences getPreferenceNode() {
    return Preferences.userNodeForPackage(LobbyLoginPreferences.class);
  }

  private static Credentials unprotectCredentials(
      final Credentials credentials,
      final CredentialManagerFactory credentialManagerFactory) {
    if (!credentials.isProtected) {
      return credentials;
    }

    try (CredentialManager credentialManager = credentialManagerFactory.create()) {
      return new Credentials(
          credentials.userName.isEmpty() ? "" : credentialManager.unprotectToString(credentials.userName),
          credentials.password.isEmpty() ? "" : credentialManager.unprotectToString(credentials.password),
          false);
    } catch (final CredentialManagerException e) {
      ClientLogger.logQuietly("failed to unprotect lobby login credentials", e);
      return new Credentials("", "", false);
    }
  }

  /**
   * Saves this instance as the user's current lobby login preferences.
   */
  public void save() {
    save(getPreferenceNode(), () -> CredentialManager.newInstance());
  }

  @VisibleForTesting
  void save(final Preferences preferences, final CredentialManagerFactory credentialManagerFactory) {
    final Credentials credentials = protectCredentials(
        new Credentials(userName, password, false),
        credentialManagerFactory);
    if (credentialsSaved) {
      preferences.put(PreferenceKeys.USER_NAME, credentials.userName);
      preferences.putBoolean(PreferenceKeys.CREDENTIALS_PROTECTED, credentials.isProtected);
    } else {
      preferences.remove(PreferenceKeys.USER_NAME);
      preferences.remove(PreferenceKeys.CREDENTIALS_PROTECTED);
    }
    if (credentialsSaved && !anonymousLogin) {
      preferences.put(PreferenceKeys.PASSWORD, credentials.password);
    } else {
      preferences.remove(PreferenceKeys.PASSWORD);
    }

    preferences.putBoolean(PreferenceKeys.CREDENTIALS_SAVED, credentialsSaved);
    preferences.putBoolean(PreferenceKeys.ANONYMOUS_LOGIN, anonymousLogin);

    preferences.remove(PreferenceKeys.LEGACY_ANONYMOUS_LOGIN);
    preferences.remove(PreferenceKeys.LEGACY_USER_NAME);
  }

  private static Credentials protectCredentials(
      final Credentials credentials,
      final CredentialManagerFactory credentialManagerFactory) {
    assert !credentials.isProtected;

    try (CredentialManager credentialManager = credentialManagerFactory.create()) {
      return new Credentials(
          credentialManager.protect(credentials.userName),
          credentialManager.protect(credentials.password),
          true);
    } catch (final CredentialManagerException e) {
      ClientLogger.logQuietly("failed to protect lobby login credentials", e);
      return new Credentials("", "", false);
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    } else if (!(obj instanceof LobbyLoginPreferences)) {
      return false;
    }

    final LobbyLoginPreferences other = (LobbyLoginPreferences) obj;
    return (anonymousLogin == other.anonymousLogin)
        && (credentialsSaved == other.credentialsSaved)
        && Objects.equals(password, other.password)
        && Objects.equals(userName, other.userName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(anonymousLogin, credentialsSaved, password, userName);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("anonymousLogin", anonymousLogin)
        .add("credentialsSaved", credentialsSaved)
        .add("password", password)
        .add("userName", userName)
        .toString();
  }

  @VisibleForTesting
  interface PreferenceKeys {
    // TODO: "LEGACY_*" keys can be removed in the second release after 1.9.0.0.3635
    String LEGACY_ANONYMOUS_LOGIN = "ANONYMOUS_LOGIN_PREF";
    String LEGACY_USER_NAME = "LAST_LOGIN_NAME_PREF";

    String ANONYMOUS_LOGIN = "LOBBY_ANONYMOUS_LOGIN";
    String CREDENTIALS_PROTECTED = "LOBBY_CREDENTIALS_PROTECTED";
    String CREDENTIALS_SAVED = "LOBBY_CREDENTIALS_SAVED";
    String PASSWORD = "LOBBY_PASSWORD";
    String USER_NAME = "LOBBY_USER_NAME";
  }

  @FunctionalInterface
  @VisibleForTesting
  interface CredentialManagerFactory {
    CredentialManager create() throws CredentialManagerException;
  }

  @Immutable
  private static final class Credentials {
    final String userName;
    final String password;
    final boolean isProtected;

    Credentials(final String userName, final String password, final boolean isProtected) {
      this.userName = userName;
      this.password = password;
      this.isProtected = isProtected;
    }
  }
}
