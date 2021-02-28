package games.strategy.engine.framework.ui;

import games.strategy.engine.framework.map.file.system.loader.DownloadedMap;
import java.nio.file.Path;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DefaultGameChooserEntry implements Comparable<DefaultGameChooserEntry> {
  private final DownloadedMap downloadedMap;
  private final String gameName;

  @Override
  public String toString() {
    return gameName;
  }

  @Override
  public int compareTo(final DefaultGameChooserEntry o) {
    return getGameName().compareToIgnoreCase(o.getGameName());
  }

  @Override
  public boolean equals(final Object rhs) {
    if (rhs instanceof DefaultGameChooserEntry) {
      final var chooserEntry = (DefaultGameChooserEntry) rhs;
      return chooserEntry.gameName.equals(gameName)
          && downloadedMap.getMapName().equals(chooserEntry.downloadedMap.getMapName());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return gameName.hashCode() * downloadedMap.getMapName().hashCode();
  }

  String readGameNotes() {
    return downloadedMap.readGameNotes(gameName).orElse("");
  }

  Optional<Path> getGameXmlFilePath() {
    return downloadedMap.getGameXmlFilePath(gameName);
  }
}
