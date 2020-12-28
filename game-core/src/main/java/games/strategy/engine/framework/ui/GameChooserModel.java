package games.strategy.engine.framework.ui;

import games.strategy.engine.framework.map.file.system.loader.AvailableGamesList;
import javax.swing.DefaultListModel;
import lombok.extern.slf4j.Slf4j;

/** The model for a {@link GameChooser} dialog. */
@Slf4j
public final class GameChooserModel extends DefaultListModel<DefaultGameChooserEntry> {
  private static final long serialVersionUID = -2044689419834812524L;

  public GameChooserModel(final AvailableGamesList availableGamesList) {
    availableGamesList.getSortedGameEntries().forEach(this::addElement);
  }
}
