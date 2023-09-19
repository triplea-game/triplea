package games.strategy.triplea.ui.menubar.help;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.triplea.ui.UiContext;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import lombok.experimental.UtilityClass;
import org.triplea.java.Interruptibles;
import org.triplea.java.Interruptibles.Result;
import org.triplea.swing.SwingAction;

@UtilityClass
class UnitHelpMenu {
  private static final String unitHelpTitle = "Unit Help";

  Action buildMenu(final GameData gameData, final UiContext uiContext) {
    return SwingAction.of(
        unitHelpTitle,
        e -> {
          final Result<JDialog> result =
              Interruptibles.awaitResult(
                  () ->
                      BackgroundTaskRunner.runInBackgroundAndReturn(
                          "Calculating Data",
                          () -> {
                            String text = UnitStatsTable.getUnitStatsTable(gameData, uiContext);
                            JEditorPane editorPane = new JEditorPane("text/html", text);
                            editorPane.setEditable(false);
                            JScrollPane scroll = new JScrollPane(editorPane);
                            scroll.setBorder(BorderFactory.createEmptyBorder());
                            return InformationDialog.createDialog(scroll, unitHelpTitle);
                          }));
          result.result.orElseThrow().setVisible(true);
        });
  }
}
