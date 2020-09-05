package games.strategy.engine.data.gameparser;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import java.io.InputStream;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ShallowGameParser {

  public static String readGameName(final String mapName, final InputStream stream)
      throws GameParseException, EngineVersionException {
    checkNotNull(mapName);
    checkNotNull(stream);
    return new GameParser(new GameData(), mapName).parseShallow(stream).getGameName();
  }
}
