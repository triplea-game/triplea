package games.strategy.engine.framework.ui;

import java.nio.file.Path;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Builder
@EqualsAndHashCode(of = "gameFilePath")
@Getter
public class DefaultGameChooserEntry implements Comparable<DefaultGameChooserEntry> {

  private final Path gameFilePath;
  private final String gameName;

  @Override
  public String toString() {
    return gameName;
  }

  @Override
  public int compareTo(final DefaultGameChooserEntry o) {
    return getGameName().compareToIgnoreCase(o.getGameName());
  }
}
