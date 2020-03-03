package games.strategy.engine.framework.ui;

import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import javax.annotation.Nonnull;

/** An installed game (map) that is selectable by the user from the Game Chooser dialog. */
public interface GameChooserEntry extends Comparable<GameChooserEntry> {

  void fullyParseGameData() throws GameParseException;

  Optional<GameData> getCompleteGameData() throws GameParseException;

  boolean isGameDataLoaded();

  String getGameName();

  @Nonnull
  GameData getGameData();

  URI getUri();

  /**
   * Returns the location of the game file.
   *
   * <p>The "location" is actually a URI in string form.
   *
   * @return The location of the game file.
   */
  String getLocation();

  static GameChooserEntry newInstance(final URI uri)
      throws IOException, GameParseException, EngineVersionException {
    return new DefaultGameChooserEntry(uri);
  }
}
