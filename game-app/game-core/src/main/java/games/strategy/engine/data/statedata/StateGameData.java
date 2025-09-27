package games.strategy.engine.data.statedata;

import games.strategy.engine.data.GameData;
import games.strategy.engine.history.History;
import lombok.Getter;
import lombok.Setter;

/** Part of the {@link GameData} with changeable data for the game. */
public class StateGameData {

  @Getter @Setter private History gameHistory;

  public StateGameData(GameData gameData) {
    gameHistory = new History(gameData);
  }
}
