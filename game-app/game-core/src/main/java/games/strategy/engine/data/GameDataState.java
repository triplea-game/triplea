package games.strategy.engine.data;

import games.strategy.triplea.delegate.TechTracker;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/** Part of the {@link GameData} with changeable data for the game. */
public class GameDataState implements Serializable {

  private static final long serialVersionUID = 9172897940709617550L;

  @Getter @Setter private transient TechTracker techTracker;

  public GameDataState(GameData gameData) {
    techTracker = new TechTracker(gameData);
  }
}
