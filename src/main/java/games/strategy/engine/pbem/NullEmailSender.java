package games.strategy.engine.pbem;

import java.io.File;
import java.io.IOException;

import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.IBean;

/**
 * A dummy Email sender, to use when Email sending is disabled.
 */
public class NullEmailSender implements IEmailSender {
  private static final long serialVersionUID = 9138507282128548506L;

  @Override
  public String getDisplayName() {
    return "disabled";
  }

  @Override
  public void sendEmail(final String subject, final String htmlMessage, final File saveGame, final String fileName)
      throws IOException {}

  @Override
  public String getToAddress() {
    return null;
  }

  @Override
  public void clearSensitiveInfo() {}

  @Override
  public IEmailSender doClone() {
    return new NullEmailSender();
  }

  @Override
  public EditorPanel getEditor() {
    return null;
  }

  @Override
  public boolean sameType(final IBean other) {
    return other.getClass() == NullEmailSender.class;
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
  public boolean isPasswordSaved() {
    return false;
  }

  @Override
  public void setUserName(final String userName) {}

  @Override
  public void setPassword(final String password) {}

  @Override
  public void setPasswordSaved(final boolean passwordSaved) {}

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
}
