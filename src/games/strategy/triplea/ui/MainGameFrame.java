package games.strategy.triplea.ui;

import javax.swing.JComponent;
import javax.swing.JFrame;

import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.LocalPlayers;

public abstract class MainGameFrame extends JFrame {
  private static final long serialVersionUID = 7433347393639606647L;
  protected LocalPlayers localPlayers;

  public MainGameFrame(final String name, final LocalPlayers players) {
    super(name);
    localPlayers = players;
    setIconImage(GameRunner2.getGameIcon(this));
  }

  public abstract IGame getGame();

  public abstract void leaveGame();

  public abstract void stopGame();

  public abstract void shutdown();

  public abstract void notifyError(String error);

  public abstract JComponent getMainPanel();

  public abstract void setShowChatTime(final boolean showTime);

  public LocalPlayers getLocalPlayers() {
    return localPlayers;
  }
}
