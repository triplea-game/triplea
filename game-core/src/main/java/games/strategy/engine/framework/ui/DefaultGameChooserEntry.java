package games.strategy.engine.framework.ui;

import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.gameparser.ShallowGameParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;
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

  public DefaultGameChooserEntry(final URI uri) throws IOException, GameParseException {
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

  @Override
  public String toString() {
    return gameName;
  }

  @Override
  public int compareTo(final DefaultGameChooserEntry o) {
    return getGameName().compareToIgnoreCase(o.getGameName());
  }
}
