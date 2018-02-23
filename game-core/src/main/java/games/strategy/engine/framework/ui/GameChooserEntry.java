package games.strategy.engine.framework.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.GameParser;
import games.strategy.triplea.Constants;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.util.UrlStreams;

public class GameChooserEntry implements Comparable<GameChooserEntry> {
  private final URI url;
  private GameData gameData;
  private boolean gameDataFullyLoaded = false;
  private final String gameNameAndMapNameProperty;

  /**
   * Factory method, if there are any map parsing errors an exception is thrown.
   */
  public static GameChooserEntry newGameChooserEntry() {
    final URI uri = URI.create(ClientSetting.SELECTED_GAME_LOCATION.value());
    try {
      return new GameChooserEntry(uri);
    } catch (final IOException | GameParseException | EngineVersionException e) {
      throw new IllegalStateException(e);
    }
  }

  public GameChooserEntry(final URI uri) throws IOException, GameParseException, EngineVersionException {
    url = uri;

    final Optional<InputStream> inputStream = UrlStreams.openStream(uri);
    if (!inputStream.isPresent()) {
      gameNameAndMapNameProperty = "";
      // this means the map was deleted out from under us.
      return;
    }

    try (InputStream input = inputStream.get()) {
      gameData = GameParser.parseShallow(uri.toString(), input);
      gameNameAndMapNameProperty = getGameName() + ":" + getMapNameProperty();
    }
  }

  public GameData fullyParseGameData() throws GameParseException {
    // TODO: We should be setting this in the the constructor. At this point, you have to call methods in the
    // correct order for things to work, and that is bads.
    gameData = null;

    final Optional<InputStream> inputStream = UrlStreams.openStream(url);
    if (!inputStream.isPresent()) {
      return gameData;
    }

    try (InputStream input = inputStream.get()) {
      gameData = GameParser.parse(url.toString(), input);
      gameDataFullyLoaded = true;
    } catch (final EngineVersionException e) {
      ClientLogger.logQuietly("Game engine not compatible with: " + url, e);
      throw new GameParseException(e);
    } catch (final GameParseException e) {
      ClientLogger.logError("Could not parse:" + url, e);
      throw e;
    } catch (final Exception e) {
      ClientLogger.logError("Could not parse:" + url, e);
      throw new GameParseException(e);
    }
    return gameData;
  }

  public boolean isGameDataLoaded() {
    return gameDataFullyLoaded;
  }

  public String getGameName() {
    return gameData.getGameName();
  }

  // the user may have selected a map skin instead of this map folder, so don't use this for anything except our
  // equals/hashcode below
  private String getMapNameProperty() {
    final String mapName = (String) gameData.getProperties().get(Constants.MAP_NAME);
    if ((mapName == null) || (mapName.trim().length() == 0)) {
      throw new IllegalStateException("Map name property not set on game");
    }
    return mapName;
  }

  @Override
  public String toString() {
    return getGameName();
  }

  public GameData getGameData() {
    return gameData;
  }

  public URI getUri() {
    return url;
  }

  /**
   * Returns the location of the game file.
   *
   * <p>
   * The "location" is actually a URI in string form.
   * </p>
   *
   * @return The location of the game file; never {@code null}.
   */
  public String getLocation() {
    return url.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(gameNameAndMapNameProperty);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final GameChooserEntry other = (GameChooserEntry) obj;
    if ((gameData == null) && (other.gameData != null)) {
      return false;
    }
    if (other.gameData == null) {
      return false;
    }
    return this.gameNameAndMapNameProperty.equals(other.gameNameAndMapNameProperty);
  }

  @Override
  public int compareTo(final GameChooserEntry o) {
    return getGameName().compareToIgnoreCase(o.getGameName());
  }
}
