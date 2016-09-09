package games.strategy.triplea.ui;

import java.io.IOException;

import games.strategy.debug.ClientLogger;
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
    try {
      this.getIcons().add(new Image(GameRunner.class.getResource("ta_icon.png").openStream()));
    } catch (IOException e) {
      ClientLogger.logError("Could not load TripleA Icon", e);
    }
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
