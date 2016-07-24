package games.strategy.engine.data;

import com.google.common.base.Joiner;

public class GameParseException extends Exception {
  private static final long serialVersionUID = 4015574053053781872L;

  public GameParseException(final GameData gameData, final String error) {
    this(gameData.getGameName(), error);
  }

  public GameParseException(final String mapName, final String error) {
    super(Joiner.on(':').join(mapName, error));
  }
}
