package games.strategy.triplea.ui.menubar.help;

import games.strategy.engine.framework.ui.GameNotesView;
import java.awt.Dimension;
import java.nio.file.Path;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;
import org.triplea.util.LocalizeHtml;

@UtilityClass
class GameNotesMenu {
  private static final String gameNotesTitle = "Game Notes";

  Action buildMenu(final String gameNotes, final Path mapLocation) {
    return SwingAction.of(
        gameNotesTitle,
        e ->
            SwingUtilities.invokeLater(
                () -> {
                  final JDialog dialog =
                      InformationDialog.createDialog(
                          notesPanel(gameNotes, mapLocation), gameNotesTitle);
                  Dimension size = dialog.getSize();
                  size.width = Math.min(size.width, 400);
                  size.height = Math.min(size.height, 300);
                  size.width = Math.max(size.width, 800);
                  size.height = Math.max(size.height, 600);
                  dialog.setSize(size);
                  dialog.setVisible(true);
                }));
  }

  private static JComponent notesPanel(final String gameNotes, final Path mapLocation) {
    final String localizedHtml = LocalizeHtml.localizeImgLinksInHtml(gameNotes.trim(), mapLocation);

    final GameNotesView gameNotesPane = new GameNotesView();
    gameNotesPane.setText(localizedHtml);
    return SwingComponents.newJScrollPane(gameNotesPane);
  }
}
