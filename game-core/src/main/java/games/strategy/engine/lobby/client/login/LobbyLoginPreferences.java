package games.strategy.engine.lobby.client.login;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import games.strategy.security.CredentialManager;
import games.strategy.security.CredentialManagerException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import javax.annotation.concurrent.Immutable;
import lombok.extern.java.Log;
import org.triplea.java.function.ThrowingSupplier;

/** The login preferences for a lobby user. */
@Immutable
@Log
public final class LobbyLoginPreferences {
  final String username;
  final String password;
  final boolean credentialsSaved;
  final boolean anonymousLogin;

  LobbyLoginPreferences(
      final String username,
      final String password,
      final boolean credentialsSaved,
      final boolean anonymousLogin) {
    this.username = username;
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
    return load(getPreferenceNode(), CredentialManager::newInstance);
  }

  @VisibleForTesting
  static LobbyLoginPreferences load(
      final Preferences preferences,
      final ThrowingSupplier<CredentialManager, CredentialManagerException>
          credentialManagerFactory) {
    final String legacyUserName = preferences.get(PreferenceKeys.LEGACY_USER_NAME, "");
    final Credentials credentials =
        unprotectCredentials(
            new Credentials(
                preferences.get(PreferenceKeys.USER_NAME, legacyUserName),
                preferences.get(PreferenceKeys.PASSWORD, ""),
                preferences.getBoolean(PreferenceKeys.CREDENTIALS_PROTECTED, false)),
            credentialManagerFactory);
    final boolean credentialsSaved =
        preferences.getBoolean(PreferenceKeys.CREDENTIALS_SAVED, false);
    final boolean legacyAnonymousLogin =
        preferences.getBoolean(PreferenceKeys.LEGACY_ANONYMOUS_LOGIN, false);
    final boolean anonymousLogin =
        preferences.getBoolean(PreferenceKeys.ANONYMOUS_LOGIN, legacyAnonymousLogin);
    return new LobbyLoginPreferences(
        credentials.username, credentials.password, credentialsSaved, anonymousLogin);
  }

  private static Preferences getPreferenceNode() {
    return Preferences.userNodeForPackage(LobbyLoginPreferences.class);
  }

  private static Credentials unprotectCredentials(
      final Credentials credentials,
      final ThrowingSupplier<CredentialManager, CredentialManagerException>
          credentialManagerFactory) {
    if (!credentials.isProtected) {
      return credentials;
    }

    try (CredentialManager credentialManager = credentialManagerFactory.get()) {
      return new Credentials(
          credentials.username.isEmpty()
              ? ""
              : credentialManager.unprotectToString(credentials.username),
          credentials.password.isEmpty()
              ? ""
              : credentialManager.unprotectToString(credentials.password),
          false);
    } catch (final CredentialManagerException e) {
      log.log(Level.SEVERE, "failed to unprotect lobby login credentials", e);
      return new Credentials("", "", false);
    }
  }

  /** Saves this instance as the user's current lobby login preferences. */
  public void save() {
    save(getPreferenceNode(), CredentialManager::newInstance);
  }

  @VisibleForTesting
  void save(
      final Preferences preferences,
      final ThrowingSupplier<CredentialManager, CredentialManagerException>
          credentialManagerFactory) {
    final Credentials credentials =
        protectCredentials(new Credentials(username, password, false), credentialManagerFactory);
    if (credentialsSaved) {
      preferences.put(PreferenceKeys.USER_NAME, credentials.username);
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
      final ThrowingSupplier<CredentialManager, CredentialManagerException>
          credentialManagerFactory) {
    assert !credentials.isProtected;

    try (CredentialManager credentialManager = credentialManagerFactory.get()) {
      return new Credentials(
          credentialManager.protect(credentials.username),
          credentialManager.protect(credentials.password),
          true);
    } catch (final CredentialManagerException e) {
      log.log(Level.SEVERE, "failed to protect lobby login credentials", e);
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
        && Objects.equals(username, other.username);
  }

  @Override
  public int hashCode() {
    return Objects.hash(anonymousLogin, credentialsSaved, password, username);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("anonymousLogin", anonymousLogin)
        .add("credentialsSaved", credentialsSaved)
        .add("password", password)
        .add("username", username)
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

  @Immutable
  private static final class Credentials {
    final String username;
    final String password;
    final boolean isProtected;

    Credentials(final String username, final String password, final boolean isProtected) {
      this.username = username;
      this.password = password;
      this.isProtected = isProtected;
    }
  }
}
