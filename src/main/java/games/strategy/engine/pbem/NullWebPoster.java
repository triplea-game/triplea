package games.strategy.engine.pbem;

import java.io.File;
import java.util.Vector;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.startup.ui.editors.EditorPanel;
import games.strategy.engine.framework.startup.ui.editors.IBean;

public class NullWebPoster implements IWebPoster {
  private static final long serialVersionUID = 1871745918801353205L;

  @Override
  public boolean postTurnSummary(final GameData gameData, final String turnSummary, final String player,
      final int round) {
    return true;
  }

  @Override
  public boolean getMailSaveGame() {
    return false;
  }

  @Override
  public void setMailSaveGame(final boolean mail) {}

  @Override
  public void addSaveGame(final File saveGame, final String fileName) {}

  @Override
  public String getDisplayName() {
    return "disabled";
  }

  @Override
  public EditorPanel getEditor() {
    return null;
  }

  @Override
  public boolean sameType(final IBean other) {
    return other.getClass() == NullWebPoster.class;
  }

  @Override
  public String getHelpText() {
    return "Will never be called";
  }

  @Override
  public String getSiteId() {
    return null;
  }

  @Override
  public String getHost() {
    return null;
  }

  @Override
  public Vector<String> getAllHosts() {
    return new Vector<>();
  }

  @Override
  public String getGameName() {
    return null;
  }

  @Override
  public void setSiteId(final String siteId) {}

  @Override
  public void setGameName(final String gameName) {}

  @Override
  public void setHost(final String host) {}

  @Override
  public void setAllHosts(final Vector<String> hosts) {}

  @Override
  public void addToAllHosts(final String host) {}

  @Override
  public void viewSite() {}

  @Override
  public String getTestMessage() {
    return "You should not be able to test a Null Poster";
  }

  @Override
  public String getServerMessage() {
    return "Success";
  }

  @Override
  public IWebPoster doClone() {
    return null;
  }

  @Override
  public void clearSensitiveInfo() {}
}
