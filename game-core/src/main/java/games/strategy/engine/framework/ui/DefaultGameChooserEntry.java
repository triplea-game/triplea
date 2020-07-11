package games.strategy.engine.framework.ui;

import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.GameParser;
import games.strategy.triplea.Constants;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import lombok.extern.java.Log;
import org.triplea.java.UrlStreams;

@Log
final class DefaultGameChooserEntry implements GameChooserEntry {
  private final URI url;
  @Nonnull private GameData gameData;
  private boolean gameDataFullyLoaded = false;
  private final String gameNameAndMapNameProperty;

  DefaultGameChooserEntry(final URI uri)
      throws IOException, GameParseException, EngineVersionException {
    url = uri;

    final Optional<InputStream> inputStream = UrlStreams.openStream(uri);
    if (inputStream.isEmpty()) {
      gameNameAndMapNameProperty = "";
      // this means the map was deleted out from under us.
      return;
    }

    try (InputStream input = inputStream.get()) {
      gameData = GameParser.parseShallow(uri.toString(), input);
      gameNameAndMapNameProperty = gameData.getGameName() + ":" + getMapNameProperty();
    }
  }

  @Override
  public void fullyParseGameData() throws GameParseException {
    // TODO: We should be setting this in the the constructor. At this point, you have to call
    // methods in the
    // correct order for things to work, and that is bads.
    getCompleteGameData()
        .ifPresent(
            data -> {
              gameData = data;
              gameDataFullyLoaded = true;
            });
  }

  @Override
  public Optional<GameData> getCompleteGameData() throws GameParseException {
    final Optional<InputStream> inputStream = UrlStreams.openStream(url);
    if (inputStream.isEmpty()) {
      return Optional.empty();
    }

    try (InputStream input = inputStream.get()) {
      return Optional.of(GameParser.parse(url.toString(), input));
    } catch (final EngineVersionException e) {
      log.log(Level.SEVERE, "Game engine not compatible with: " + url, e);
      throw new GameParseException(e);
    } catch (final GameParseException e) {
      log.log(Level.SEVERE, "Could not parse:" + url, e);
      throw e;
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Could not parse:" + url, e);
      throw new GameParseException(e);
    }
  }

  @Override
  public boolean isGameDataLoaded() {
    return gameDataFullyLoaded;
  }

  // the user may have selected a map skin instead of this map folder, so don't use this for
  // anything except our
  // equals/hashcode below
  private String getMapNameProperty() {
    final String mapName = (String) gameData.getProperties().get(Constants.MAP_NAME);
    if (mapName == null || mapName.isBlank()) {
      throw new IllegalStateException("Map name property not set on game");
    }
    return mapName;
  }

  @Override
  public String toString() {
    return gameData.getGameName();
  }

  @Nonnull
  @Override
  public GameData getGameData() {
    return gameData;
  }

  @Override
  public URI getUri() {
    return url;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(gameNameAndMapNameProperty);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof DefaultGameChooserEntry)) {
      return false;
    }

    final DefaultGameChooserEntry other = (DefaultGameChooserEntry) obj;
    return this.gameNameAndMapNameProperty.equals(other.gameNameAndMapNameProperty);
  }

  @Override
  public int compareTo(final GameChooserEntry o) {
    return gameData.getGameName().compareToIgnoreCase(o.getGameData().getGameName());
  }
}
