package games.strategy.engine.framework.ui;

import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.gameparser.GameParser;
import games.strategy.engine.data.gameparser.ShallowGameParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
import java.util.logging.Level;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.java.Log;
import org.triplea.java.UrlStreams;

@Log
@EqualsAndHashCode(of = "uri")
@Getter
public class DefaultGameChooserEntry implements Comparable<DefaultGameChooserEntry> {

  private final URI uri;
  private String gameName;

  public DefaultGameChooserEntry(final URI uri)
      throws IOException, GameParseException, EngineVersionException {
    this.uri = uri;

    final Optional<InputStream> inputStream = UrlStreams.openStream(uri);
    if (inputStream.isEmpty()) {
      // this means the map was deleted out from under us.
      return;
    }

    try (InputStream input = inputStream.get()) {
      gameName = ShallowGameParser.readGameName(uri.toString(), input);
    }
  }

  public GameData fullyParseGameData() throws GameParseException {
    final Optional<InputStream> inputStream = UrlStreams.openStream(uri);
    if (inputStream.isEmpty()) {
      return null;
    }

    try (InputStream input = inputStream.get()) {
      return GameParser.parse(uri.toString(), input);
    } catch (final EngineVersionException e) {
      log.log(Level.WARNING, "Game engine not compatible with: " + uri, e);
      throw new GameParseException(e);
    } catch (final GameParseException e) {
      log.log(Level.WARNING, "Could not parse:" + uri, e);
      throw e;
    } catch (final Exception e) {
      log.log(Level.WARNING, "Could not parse:" + uri, e);
      throw new GameParseException(e);
    }
  }

  @Override
  public String toString() {
    return gameName;
  }

  @Override
  public int compareTo(final DefaultGameChooserEntry o) {
    return getGameName().compareToIgnoreCase(o.getGameName());
  }
}
