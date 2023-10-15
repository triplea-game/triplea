package games.strategy.triplea.ui.menubar.help;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.triplea.ui.UiContext;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingAction;

@UtilityClass
class UnitHelpMenu {
  private static final String unitHelpTitle = "Unit Help";

  Action buildMenu(final GameData gameData, final UiContext uiContext) {
    return SwingAction.of(
        unitHelpTitle,
        event -> {
          try {
            BackgroundTaskRunner.runInBackgroundAndReturn(
                UnitHelpMenu::showDialog,
                "Calculating Data",
                () -> UnitStatsTable.getUnitStatsTable(gameData, uiContext));
          } catch (InterruptedException e) {
            // Nothing to do.
          }
        });
  }

  private static void showDialog(String text) {
    final JEditorPane editorPane = new JEditorPane();
    editorPane.setContentType("text/html");
    editorPane.setEditable(false);
    final JScrollPane scroll = new JScrollPane(editorPane);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    editorPane.setText(text);
    editorPane.setCaretPosition(0);
    InformationDialog.createDialog(scroll, unitHelpTitle).setVisible(true);
  }
}
