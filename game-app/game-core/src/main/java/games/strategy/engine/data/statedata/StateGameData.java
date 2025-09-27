package games.strategy.engine.data.statedata;

import games.strategy.engine.data.GameData;
import games.strategy.engine.history.History;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

/** Part of the {@link GameData} with changeable data for the game. */
public class StateGameData implements Serializable {

  private static final long serialVersionUID = 9172897940709617550L;

  @Getter @Setter private History gameHistory;

  public StateGameData(GameData gameData) {
    setGameHistory(new History(gameData));
  }
}
