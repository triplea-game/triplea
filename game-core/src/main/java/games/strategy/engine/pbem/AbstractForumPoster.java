package games.strategy.engine.pbem;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.ForumPosterEditor;
import games.strategy.security.CredentialManager;
import games.strategy.security.CredentialManagerException;

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
public abstract class AbstractForumPoster implements IForumPoster {
  /**
   * The value assigned to a persistent credential that indicates it was cleared and the associated transient credential
   * should be used instead.
   */
  private static final String USE_TRANSIENT_CREDENTIAL = "d0a11f0f-96d3-4303-8875-4965aefb2ce4";

  private static final long serialVersionUID = -734015230309508040L;
  protected String m_username = null;
  private transient String transientUsername;
  protected String m_password = null;
  private transient String transientPassword;
  protected String m_topicId = null;
  protected boolean m_includeSaveGame = true;
  protected boolean m_alsoPostAfterCombatMove = false;
  protected transient File saveGameFile = null;
  protected transient String turnSummaryRef = null;
  protected transient String saveGameFileName = null;
  private boolean credentialsSaved = false;
  private boolean credentialsProtected = false;

  private void writeObject(final ObjectOutputStream out) throws IOException {
    final String username = m_username;
    final String password = m_password;
    try {
      protectCredentials();
      out.defaultWriteObject();
    } finally {
      m_username = username;
      m_password = password;
    }
  }

  private void protectCredentials() {
    if (credentialsSaved) {
      credentialsProtected = true;
      try (CredentialManager credentialManager = CredentialManager.newInstance()) {
        m_username = credentialManager.protect(m_username);
        m_password = credentialManager.protect(m_password);
      } catch (final CredentialManagerException e) {
        ClientLogger.logQuietly("failed to protect PBF credentials", e);
        m_username = "";
        m_password = "";
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
        m_username = credentialManager.unprotectToString(m_username);
        m_password = credentialManager.unprotectToString(m_password);
      } catch (final CredentialManagerException e) {
        ClientLogger.logQuietly("failed to unprotect PBF credentials", e);
        m_username = "";
        m_password = "";
      }
    }
  }

  @Override
  public String getTurnSummaryRef() {
    return turnSummaryRef;
  }

  @Override
  public boolean getIncludeSaveGame() {
    return m_includeSaveGame;
  }

  @Override
  public void setIncludeSaveGame(final boolean include) {
    m_includeSaveGame = include;
  }

  @Override
  public boolean getAlsoPostAfterCombatMove() {
    return m_alsoPostAfterCombatMove;
  }

  @Override
  public void setAlsoPostAfterCombatMove(final boolean post) {
    m_alsoPostAfterCombatMove = post;
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
    m_topicId = topicId;
  }

  @Override
  public String getTopicId() {
    return m_topicId;
  }

  @Override
  public void setUsername(final String username) {
    m_username = credentialsSaved ? username : USE_TRANSIENT_CREDENTIAL;
    transientUsername = username;
  }

  @Override
  public String getUsername() {
    return USE_TRANSIENT_CREDENTIAL.equals(m_username) ? transientUsername : m_username;
  }

  @Override
  public void setPassword(final String password) {
    m_password = credentialsSaved ? password : USE_TRANSIENT_CREDENTIAL;
    transientPassword = password;
  }

  @Override
  public String getPassword() {
    return USE_TRANSIENT_CREDENTIAL.equals(m_password) ? transientPassword : m_password;
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
    m_username = m_password = USE_TRANSIENT_CREDENTIAL;
  }

  @Override
  public EditorPanel getEditor() {
    return new ForumPosterEditor(this);
  }

  @Override
  public boolean equals(final Object other) {
    return (other != null) && getClass().equals(other.getClass());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getClass());
  }
}
