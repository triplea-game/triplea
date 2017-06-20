package games.strategy.engine.pbem;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.ForumPosterEditor;
import games.strategy.engine.framework.startup.ui.editors.IBean;

/**
 * Abstract Forum poster that takes care of storing the username, password, and other common properties.
 */
public abstract class AbstractForumPoster implements IForumPoster {
  private static final String USE_TRANSITIVE_PASSWORD = "d0a11f0f-96d3-4303-8875-4965aefb2ce4";
  private static final long serialVersionUID = -734015230309508040L;
  protected String m_username = null;
  protected String m_password = null;
  protected transient String m_transPassword;
  protected String m_topicId = null;
  protected boolean m_includeSaveGame = true;
  protected boolean m_alsoPostAfterCombatMove = false;
  protected transient File m_saveGameFile = null;
  protected transient String m_turnSummaryRef = null;
  protected transient String m_saveGameFileName = null;
  private boolean passwordSaved = false;
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
    credentialsProtected = true;
    try (final CredentialManager credentialManager = CredentialManager.newInstance()) {
      m_username = credentialManager.protect(m_username);
      m_password = credentialManager.protect(m_password);
    } catch (final CredentialManagerException e) {
      ClientLogger.logQuietly("failed to protect PBF credentials", e);
      m_username = "";
      m_password = "";
    }
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    unprotectCredentials();
  }

  private void unprotectCredentials() {
    if (credentialsProtected) {
      try (final CredentialManager credentialManager = CredentialManager.newInstance()) {
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
    return m_turnSummaryRef;
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
    m_saveGameFile = saveGame;
    m_saveGameFileName = fileName;
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
    m_username = username;
  }

  @Override
  public String getUsername() {
    return m_username;
  }

  @Override
  public void setPassword(final String password) {
    m_password = passwordSaved ? password : USE_TRANSITIVE_PASSWORD;
    m_transPassword = password;
  }

  @Override
  public String getPassword() {
    if (USE_TRANSITIVE_PASSWORD.equals(m_password)) {
      return m_transPassword;
    }
    return m_password;
  }

  @Override
  public void setPasswordSaved(final boolean passwordSaved) {
    this.passwordSaved = passwordSaved;
    setPassword(m_transPassword);
  }

  @Override
  public boolean isPasswordSaved() {
    return passwordSaved;
  }

  @Override
  public void clearSensitiveInfo() {
    m_password = USE_TRANSITIVE_PASSWORD;
  }

  @Override
  public boolean sameType(final IBean other) {
    return getClass() == other.getClass();
  }

  @Override
  public EditorPanel getEditor() {
    return new ForumPosterEditor(this);
  }
}
