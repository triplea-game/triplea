package games.strategy.triplea.ui;

import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.LocalPlayers;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public abstract class MainGameFrame extends Stage {
  protected LocalPlayers localPlayers;

  public MainGameFrame(final String name, final LocalPlayers players) {
    super();
    this.setTitle(name);
    localPlayers = players;
    this.getIcons().add(new Image(GameRunner.class.getResourceAsStream("ta_icon.png")));
  }

  public abstract IGame getGame();

  public abstract void leaveGame();

  public abstract void stopGame();

  public abstract void shutdown();

  public abstract void notifyError(String error);

  public abstract Node getMainPanel();

  public abstract void setShowChatTime(final boolean showTime);

  public LocalPlayers getLocalPlayers() {
    return localPlayers;
  }
}
