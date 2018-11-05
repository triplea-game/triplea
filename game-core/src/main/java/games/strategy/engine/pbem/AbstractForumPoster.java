package games.strategy.engine.pbem;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;
import java.util.logging.Level;

import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.ForumPosterEditor;
import games.strategy.security.CredentialManager;
import games.strategy.security.CredentialManagerException;
import lombok.extern.java.Log;

/**
 * Abstract Forum poster that takes care of storing the username, password, and other common properties.
 *
 * <p>
 * Instances of this class are saved as a property as part of a save game.
 * </p>
 *
 * <p>
 * This class has two fields per credential. One is transient and used while the game is running. The other is
 * persistent and "cleared" when the game starts. This is done for security reasons so save games will not include
 * credentials. The persistent password is used when the object is stored in the local cache.
 * </p>
 */
@Log
public abstract class AbstractForumPoster implements IForumPoster {
  /**
   * The value assigned to a persistent credential that indicates it was cleared and the associated transient credential
   * should be used instead.
   */
  private static final String USE_TRANSIENT_CREDENTIAL = "d0a11f0f-96d3-4303-8875-4965aefb2ce4";
  private static final long serialVersionUID = -734015230309508040L;

  protected String username = null;
  private transient String transientUsername;
  protected String password = null;
  private transient String transientPassword;
  protected String topicId = null;
  protected boolean includeSaveGame = true;
  protected boolean alsoPostAfterCombatMove = false;
  protected transient File saveGameFile = null;
  protected transient String turnSummaryRef = null;
  protected transient String saveGameFileName = null;
  private boolean credentialsSaved = false;
  private boolean credentialsProtected = false;

  private void writeObject(final ObjectOutputStream out) throws IOException {
    final String username = this.username;
    final String password = this.password;
    try {
      protectCredentials();
      out.defaultWriteObject();
    } finally {
      this.username = username;
      this.password = password;
    }
  }

  private void protectCredentials() {
    if (credentialsSaved) {
      credentialsProtected = true;
      try (CredentialManager credentialManager = CredentialManager.newInstance()) {
        username = credentialManager.protect(username);
        password = credentialManager.protect(password);
      } catch (final CredentialManagerException e) {
        log.log(Level.SEVERE, "failed to protect PBF credentials", e);
        username = "";
        password = "";
      }
    } else {
      credentialsProtected = false;
    }
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    unprotectCredentials();
  }

  private void unprotectCredentials() {
    if (credentialsProtected) {
      try (CredentialManager credentialManager = CredentialManager.newInstance()) {
        username = credentialManager.unprotectToString(username);
        password = credentialManager.unprotectToString(password);
      } catch (final CredentialManagerException e) {
        log.log(Level.SEVERE, "failed to unprotect PBF credentials", e);
        username = "";
        password = "";
      }
    }
  }

  @Override
  public String getTurnSummaryRef() {
    return turnSummaryRef;
  }

  @Override
  public boolean getIncludeSaveGame() {
    return includeSaveGame;
  }

  @Override
  public void setIncludeSaveGame(final boolean include) {
    includeSaveGame = include;
  }

  @Override
  public boolean getAlsoPostAfterCombatMove() {
    return alsoPostAfterCombatMove;
  }

  @Override
  public void setAlsoPostAfterCombatMove(final boolean post) {
    alsoPostAfterCombatMove = post;
  }

  @Override
  public void addSaveGame(final File saveGame, final String fileName) {
    saveGameFile = saveGame;
    saveGameFileName = fileName;
  }

  @Override
  public boolean getCanViewPosted() {
    return true;
  }

  @Override
  public void setTopicId(final String topicId) {
    this.topicId = topicId;
  }

  @Override
  public String getTopicId() {
    return topicId;
  }

  @Override
  public void setUsername(final String username) {
    this.username = credentialsSaved ? username : USE_TRANSIENT_CREDENTIAL;
    transientUsername = username;
  }

  @Override
  public String getUsername() {
    return USE_TRANSIENT_CREDENTIAL.equals(username) ? transientUsername : username;
  }

  @Override
  public void setPassword(final String password) {
    this.password = credentialsSaved ? password : USE_TRANSIENT_CREDENTIAL;
    transientPassword = password;
  }

  @Override
  public String getPassword() {
    return USE_TRANSIENT_CREDENTIAL.equals(password) ? transientPassword : password;
  }

  @Override
  public void setCredentialsSaved(final boolean credentialsSaved) {
    this.credentialsSaved = credentialsSaved;
    setUsername(transientUsername);
    setPassword(transientPassword);
  }

  @Override
  public boolean areCredentialsSaved() {
    return credentialsSaved;
  }

  @Override
  public void clearSensitiveInfo() {
    credentialsSaved = false;
    username = password = USE_TRANSIENT_CREDENTIAL;
  }

  @Override
  public EditorPanel getEditor() {
    return new ForumPosterEditor(this);
  }

  @Override
  public boolean equals(final Object other) {
    return other != null && getClass().equals(other.getClass());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getClass());
  }
}
