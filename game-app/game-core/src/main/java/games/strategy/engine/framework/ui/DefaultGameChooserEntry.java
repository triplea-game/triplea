package games.strategy.engine.framework.ui;

import games.strategy.engine.framework.map.file.system.loader.InstalledMap;
import java.nio.file.Path;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DefaultGameChooserEntry implements Comparable<DefaultGameChooserEntry> {
  private final InstalledMap installedMap;
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
    if (rhs instanceof DefaultGameChooserEntry chooserEntry) {
      return chooserEntry.gameName.equals(gameName)
          && installedMap.getMapName().equals(chooserEntry.installedMap.getMapName());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return gameName.hashCode() * installedMap.getMapName().hashCode();
  }

  String readGameNotes() {
    return installedMap.readGameNotes(gameName).orElse("");
  }

  Optional<Path> getGameXmlFilePath() {
    return installedMap.getGameXmlFilePath(gameName);
  }
}
