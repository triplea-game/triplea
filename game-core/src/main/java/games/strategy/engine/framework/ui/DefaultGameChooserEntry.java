package games.strategy.engine.framework.ui;

import games.strategy.engine.data.gameparser.ShallowGameParser;
import java.net.URI;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.triplea.java.UrlStreams;

@EqualsAndHashCode(of = "uri")
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultGameChooserEntry implements Comparable<DefaultGameChooserEntry> {

  private final URI uri;
  private final String gameName;

  public static Optional<DefaultGameChooserEntry> newDefaultGameChooserEntry(final URI uri) {

    return UrlStreams.openStream(
            uri,
            inputStream -> ShallowGameParser.readGameName(uri.toString(), inputStream).orElse(null))
        .map(gameName -> new DefaultGameChooserEntry(uri, gameName));
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
