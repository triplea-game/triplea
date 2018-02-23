package games.strategy.engine.pbem;

import java.io.File;
import java.util.Objects;

import games.strategy.engine.framework.startup.ui.editors.EditorPanel;

/**
 * A dummy Email sender, to use when Email sending is disabled.
 */
public class NullEmailSender implements IEmailSender {
  private static final long serialVersionUID = 9138507282128548506L;

  @Override
  public String getDisplayName() {
    return "Disabled";
  }

  @Override
  public void sendEmail(final String subject, final String htmlMessage, final File saveGame, final String fileName) {}

  @Override
  public String getToAddress() {
    return null;
  }

  @Override
  public void clearSensitiveInfo() {}

  @Override
  public IEmailSender clone() {
    return new NullEmailSender();
  }

  @Override
  public EditorPanel getEditor() {
    return null;
  }

  @Override
  public String getUserName() {
    return null;
  }

  @Override
  public String getPassword() {
    return null;
  }

  @Override
  public boolean areCredentialsSaved() {
    return false;
  }

  @Override
  public void setUserName(final String userName) {}

  @Override
  public void setPassword(final String password) {}

  @Override
  public void setCredentialsSaved(final boolean credentialsSaved) {}

  @Override
  public String getHelpText() {
    return "will never be called";
  }

  @Override
  public boolean getAlsoPostAfterCombatMove() {
    return false;
  }

  @Override
  public void setAlsoPostAfterCombatMove(final boolean postAlso) {}

  @Override
  public boolean equals(final Object other) {
    return (other != null) && getClass().equals(other.getClass());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getClass());
  }
}
