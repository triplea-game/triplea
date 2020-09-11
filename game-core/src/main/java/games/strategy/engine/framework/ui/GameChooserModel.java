package games.strategy.engine.framework.ui;

import games.strategy.engine.framework.map.file.system.loader.AvailableGamesFileSystemReader;
import games.strategy.engine.framework.map.file.system.loader.AvailableGamesList;
import java.net.URI;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.swing.DefaultListModel;
import lombok.extern.java.Log;

/** The model for a {@link GameChooser} dialog. */
@Log
public final class GameChooserModel extends DefaultListModel<String> {
  private static final long serialVersionUID = -2044689419834812524L;
  private final AvailableGamesList availableGamesList;
  /**
   * Initializes a new {@code GameChooserModel} using all available maps installed in the user's
   * maps folder. This method will block until all maps are parsed and should not be called from the
   * EDT.
   */
  public GameChooserModel() {
    this(AvailableGamesFileSystemReader.parseMapFiles());
  }

  public GameChooserModel(final AvailableGamesList availableGamesList) {
    this.availableGamesList = availableGamesList;
    availableGamesList.getSortedGameList().forEach(this::addElement);
  }

  @Override
  public String get(final int i) {
    return super.get(i);
  }

  /** Searches for a GameChooserEntry whose gameName matches the input parameter. */
  public Optional<String> findByName(final String name) {
    return IntStream.range(0, size()).mapToObj(this::get).filter(name::equals).findAny();
  }

  Optional<URI> lookupGameUriByName(final String selectedValue) {
    return availableGamesList.findGameUriByName(selectedValue);
  }
}
