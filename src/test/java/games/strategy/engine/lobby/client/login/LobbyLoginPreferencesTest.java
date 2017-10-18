package games.strategy.engine.lobby.client.login;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.prefs.Preferences;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Stubber;

import games.strategy.engine.lobby.client.login.LobbyLoginPreferences.PreferenceKeys;
import games.strategy.security.CredentialManager;
import games.strategy.security.CredentialManagerException;
import nl.jqno.equalsverifier.EqualsVerifier;

@ExtendWith(MockitoExtension.class)
public final class LobbyLoginPreferencesTest {
  private static final String PASSWORD = "password";
  private static final String PROTECTED_PASSWORD = String.format("PROTECTED(%s)", PASSWORD);
  private static final String USER_NAME = "userName";
  private static final String PROTECTED_USER_NAME = String.format("PROTECTED(%s)", USER_NAME);

  @Mock
  private CredentialManager credentialManager;

  @Mock
  private Preferences preferences;

  @Test
  public void shouldBeEquatableAndHashable() {
    EqualsVerifier.forClass(LobbyLoginPreferences.class).verify();
  }

  private LobbyLoginPreferences whenLoad() {
    return LobbyLoginPreferences.load(preferences, () -> credentialManager);
  }

  @Test
  public void load_ShouldMigrateLegacyPreferences() {
    givenPreferenceNode()
        .withLegacyAnonymousLogin(true)
        .withLegacyUserName(USER_NAME)
        .create();

    assertThat(whenLoad(), is(new LobbyLoginPreferences(USER_NAME, "", false, true)));
  }

  @Test
  public void load_ShouldLoadUnsavedCredentials() {
    givenPreferenceNode()
        .withAnonymousLogin(false)
        .withCredentialsProtected(false)
        .withCredentialsSaved(false)
        .create();

    assertThat(whenLoad(), is(new LobbyLoginPreferences("", "", false, false)));
  }

  @Test
  public void load_ShouldLoadUnprotectedSavedCredentials() {
    givenPreferenceNode()
        .withAnonymousLogin(false)
        .withCredentialsProtected(false)
        .withCredentialsSaved(true)
        .withPassword(PASSWORD)
        .withUserName(USER_NAME)
        .create();

    assertThat(whenLoad(), is(new LobbyLoginPreferences(USER_NAME, PASSWORD, true, false)));
  }

  @Test
  public void load_ShouldLoadProtectedSavedCredentials() throws Exception {
    givenPreferenceNode()
        .withAnonymousLogin(false)
        .withCredentialsProtected(true)
        .withCredentialsSaved(true)
        .withPassword(PROTECTED_PASSWORD)
        .withUserName(PROTECTED_USER_NAME)
        .create();
    givenCredentialManagerWillUnprotect(PROTECTED_PASSWORD, PASSWORD);
    givenCredentialManagerWillUnprotect(PROTECTED_USER_NAME, USER_NAME);

    assertThat(whenLoad(), is(new LobbyLoginPreferences(USER_NAME, PASSWORD, true, false)));
  }

  @Test
  public void load_ShouldLoadAnonymousCredentials() throws Exception {
    givenPreferenceNode()
        .withAnonymousLogin(true)
        .withCredentialsProtected(true)
        .withCredentialsSaved(true)
        .withUserName(PROTECTED_USER_NAME)
        .create();
    givenCredentialManagerWillUnprotect(PROTECTED_USER_NAME, USER_NAME);

    assertThat(whenLoad(), is(new LobbyLoginPreferences(USER_NAME, "", true, true)));
  }

  @Test
  public void load_ShouldReturnEmptyUserNameWhenProtectedUserNameIsAbsent() throws Exception {
    givenPreferenceNode()
        .withAnonymousLogin(false)
        .withCredentialsProtected(true)
        .withCredentialsSaved(true)
        .withPassword(PROTECTED_PASSWORD)
        .create();
    givenCredentialManagerWillUnprotect(PROTECTED_PASSWORD, PASSWORD);

    assertThat(whenLoad(), is(new LobbyLoginPreferences("", PASSWORD, true, false)));
  }

  @Test
  public void load_ShouldReturnDefaultPreferencesWhenCredentialManagerThrowsException() throws Exception {
    givenPreferenceNode()
        .withAnonymousLogin(false)
        .withCredentialsProtected(true)
        .withCredentialsSaved(true)
        .withPassword(PROTECTED_PASSWORD)
        .withUserName(PROTECTED_USER_NAME)
        .create();
    givenCredentialManagerWillThrowWhenUnprotecting();

    assertThat(whenLoad(), is(new LobbyLoginPreferences("", "", true, false)));
  }

  private void whenSave(final LobbyLoginPreferences lobbyLoginPreferences) {
    lobbyLoginPreferences.save(preferences, () -> credentialManager);
  }

  @Test
  public void save_ShouldRemoveLegacyPreferences() {
    whenSave(new LobbyLoginPreferences(USER_NAME, PASSWORD, false, false));

    thenRemovesValueFromPreferenceNode(PreferenceKeys.LEGACY_ANONYMOUS_LOGIN);
    thenRemovesValueFromPreferenceNode(PreferenceKeys.LEGACY_USER_NAME);
  }

  @Test
  public void save_ShouldSaveUnsavedCredentials() {
    whenSave(new LobbyLoginPreferences(USER_NAME, PASSWORD, false, false));

    thenPutsBooleanValueInPreferenceNode(PreferenceKeys.ANONYMOUS_LOGIN, false);
    thenRemovesValueFromPreferenceNode(PreferenceKeys.CREDENTIALS_PROTECTED);
    thenPutsBooleanValueInPreferenceNode(PreferenceKeys.CREDENTIALS_SAVED, false);
    thenRemovesValueFromPreferenceNode(PreferenceKeys.PASSWORD);
    thenRemovesValueFromPreferenceNode(PreferenceKeys.USER_NAME);
  }

  @Test
  public void save_ShouldSaveSavedCredentials() throws Exception {
    givenCredentialManagerWillProtect(PASSWORD, PROTECTED_PASSWORD);
    givenCredentialManagerWillProtect(USER_NAME, PROTECTED_USER_NAME);

    whenSave(new LobbyLoginPreferences(USER_NAME, PASSWORD, true, false));

    thenPutsBooleanValueInPreferenceNode(PreferenceKeys.ANONYMOUS_LOGIN, false);
    thenPutsBooleanValueInPreferenceNode(PreferenceKeys.CREDENTIALS_PROTECTED, true);
    thenPutsBooleanValueInPreferenceNode(PreferenceKeys.CREDENTIALS_SAVED, true);
    thenPutsStringValueInPreferenceNode(PreferenceKeys.PASSWORD, PROTECTED_PASSWORD);
    thenPutsStringValueInPreferenceNode(PreferenceKeys.USER_NAME, PROTECTED_USER_NAME);
  }

  @Test
  public void save_ShouldSaveAnonymousSavedCredentials() throws Exception {
    givenCredentialManagerWillProtect(USER_NAME, PROTECTED_USER_NAME);

    whenSave(new LobbyLoginPreferences(USER_NAME, PASSWORD, true, true));

    thenPutsBooleanValueInPreferenceNode(PreferenceKeys.ANONYMOUS_LOGIN, true);
    thenPutsBooleanValueInPreferenceNode(PreferenceKeys.CREDENTIALS_PROTECTED, true);
    thenPutsBooleanValueInPreferenceNode(PreferenceKeys.CREDENTIALS_SAVED, true);
    thenRemovesValueFromPreferenceNode(PreferenceKeys.PASSWORD);
    thenPutsStringValueInPreferenceNode(PreferenceKeys.USER_NAME, PROTECTED_USER_NAME);
  }

  @Test
  public void save_ShouldSaveAnonymousUnsavedCredentials() {
    whenSave(new LobbyLoginPreferences(USER_NAME, PASSWORD, false, true));

    thenPutsBooleanValueInPreferenceNode(PreferenceKeys.ANONYMOUS_LOGIN, true);
    thenRemovesValueFromPreferenceNode(PreferenceKeys.CREDENTIALS_PROTECTED);
    thenPutsBooleanValueInPreferenceNode(PreferenceKeys.CREDENTIALS_SAVED, false);
    thenRemovesValueFromPreferenceNode(PreferenceKeys.PASSWORD);
    thenRemovesValueFromPreferenceNode(PreferenceKeys.USER_NAME);
  }

  @Test
  public void save_ShouldSaveEmptyCredentialsWhenCredentialManagerThrowsException() throws Exception {
    givenCredentialManagerWillThrowWhenProtecting();

    whenSave(new LobbyLoginPreferences(USER_NAME, PASSWORD, true, false));

    thenPutsBooleanValueInPreferenceNode(PreferenceKeys.ANONYMOUS_LOGIN, false);
    thenPutsBooleanValueInPreferenceNode(PreferenceKeys.CREDENTIALS_PROTECTED, false);
    thenPutsBooleanValueInPreferenceNode(PreferenceKeys.CREDENTIALS_SAVED, true);
    thenPutsStringValueInPreferenceNode(PreferenceKeys.PASSWORD, "");
    thenPutsStringValueInPreferenceNode(PreferenceKeys.USER_NAME, "");
  }

  private GivenPreferenceNode givenPreferenceNode() {
    return new GivenPreferenceNode();
  }

  private final class GivenPreferenceNode {
    private Optional<Boolean> anonymousLogin = Optional.empty();
    private Optional<Boolean> credentialsProtected = Optional.empty();
    private Optional<Boolean> credentialsSaved = Optional.empty();
    private Optional<String> password = Optional.empty();
    private Optional<String> legacyUserName = Optional.empty();
    private Optional<Boolean> legacyAnonymousLogin = Optional.empty();
    private Optional<String> userName = Optional.empty();

    GivenPreferenceNode withAnonymousLogin(final boolean anonymousLogin) {
      this.anonymousLogin = Optional.of(anonymousLogin);
      return this;
    }

    GivenPreferenceNode withCredentialsProtected(final boolean credentialsProtected) {
      this.credentialsProtected = Optional.of(credentialsProtected);
      return this;
    }

    GivenPreferenceNode withCredentialsSaved(final boolean credentialsSaved) {
      this.credentialsSaved = Optional.of(credentialsSaved);
      return this;
    }

    GivenPreferenceNode withLegacyUserName(final String legacyUserName) {
      this.legacyUserName = Optional.of(legacyUserName);
      return this;
    }

    GivenPreferenceNode withLegacyAnonymousLogin(final boolean legacyAnonymousLogin) {
      this.legacyAnonymousLogin = Optional.of(legacyAnonymousLogin);
      return this;
    }

    GivenPreferenceNode withPassword(final String password) {
      this.password = Optional.of(password);
      return this;
    }

    GivenPreferenceNode withUserName(final String userName) {
      this.userName = Optional.of(userName);
      return this;
    }

    void create() {
      doReturnValueOrSecondArg(anonymousLogin)
          .when(preferences).getBoolean(eq(PreferenceKeys.ANONYMOUS_LOGIN), anyBoolean());
      doReturnValueOrSecondArg(credentialsProtected)
          .when(preferences).getBoolean(eq(PreferenceKeys.CREDENTIALS_PROTECTED), anyBoolean());
      doReturnValueOrSecondArg(credentialsSaved)
          .when(preferences).getBoolean(eq(PreferenceKeys.CREDENTIALS_SAVED), anyBoolean());
      doReturnValueOrSecondArg(legacyAnonymousLogin)
          .when(preferences).getBoolean(eq(PreferenceKeys.LEGACY_ANONYMOUS_LOGIN), anyBoolean());
      doReturnValueOrSecondArg(legacyUserName)
          .when(preferences).get(eq(PreferenceKeys.LEGACY_USER_NAME), anyString());
      doReturnValueOrSecondArg(password)
          .when(preferences).get(eq(PreferenceKeys.PASSWORD), anyString());
      doReturnValueOrSecondArg(userName)
          .when(preferences).get(eq(PreferenceKeys.USER_NAME), anyString());
    }

    private <T> Stubber doReturnValueOrSecondArg(final Optional<T> optional) {
      return optional.map(Mockito::doReturn).orElseGet(() -> doAnswer(returnsSecondArg()));
    }
  }

  private void givenCredentialManagerWillProtect(
      final String credential,
      final String protectedCredential)
      throws Exception {
    doReturn(protectedCredential).when(credentialManager).protect(credential);
  }

  private void givenCredentialManagerWillUnprotect(
      final String protectedCredential,
      final String credential)
      throws Exception {
    doReturn(credential).when(credentialManager).unprotectToString(protectedCredential);
  }

  private void givenCredentialManagerWillThrowWhenProtecting() throws Exception {
    doThrow(new CredentialManagerException("")).when(credentialManager).protect(anyString());
  }

  private void givenCredentialManagerWillThrowWhenUnprotecting() throws Exception {
    doThrow(new CredentialManagerException("")).when(credentialManager).unprotectToString(anyString());
  }

  private void thenRemovesValueFromPreferenceNode(final String key) {
    verify(preferences).remove(key);
  }

  private void thenPutsBooleanValueInPreferenceNode(final String key, final boolean value) {
    verify(preferences).putBoolean(key, value);
  }

  private void thenPutsStringValueInPreferenceNode(final String key, final String value) {
    verify(preferences).put(key, value);
  }
}
