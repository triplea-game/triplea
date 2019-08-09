package games.strategy.engine.lobby.moderator.toolbox.tabs.moderators;

import games.strategy.engine.lobby.moderator.toolbox.MessagePopup;
import games.strategy.engine.lobby.moderator.toolbox.tabs.ShowApiKeyDialog;
import java.awt.Component;
import java.util.function.BiConsumer;
import javax.annotation.Nonnull;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import lombok.Builder;
import org.triplea.swing.JTableBuilder;
import org.triplea.swing.SwingComponents;

/** Tab with a table showing list of moderators. */
@Builder
final class ModeratorsTabActions {
  private static final String GENERATE_KEY_LABEL =
      "<html>Copy/Paste the key below and provide it to the moderator. "
          + "This is the only time the key will be shown.";

  @Nonnull private final JFrame parentFrame;
  @Nonnull private final ModeratorsTabModel moderatorsTabModel;

  // TODO: test?
  BiConsumer<Integer, DefaultTableModel> generateApiKeyAction() {
    return (rowNum, tableModel) -> {
      final String user = extractUserName(rowNum, tableModel);
      SwingComponents.promptUser(
          "Generate API Key?",
          "Generate a single-use API key for " + user + "?",
          () -> {
            final String newKey = moderatorsTabModel.generateApiKey(user);
            ShowApiKeyDialog.showKey(parentFrame, GENERATE_KEY_LABEL, newKey);
          });
    };
  }

  private static String extractUserName(final int rowNum, final DefaultTableModel tableModel) {
    return (String) tableModel.getValueAt(rowNum, 0);
  }

  BiConsumer<Integer, DefaultTableModel> removeModAction(final JFrame parentFrame) {
    return (rowNum, tableModel) -> {
      final String user = extractUserName(rowNum, tableModel);

      SwingComponents.promptUser(
          "Remove Moderator Status?",
          "Are you sure you want to remove moderator from: " + user + "?",
          () -> {
            moderatorsTabModel.removeMod(user);
            MessagePopup.showMessage(parentFrame, user + " is no longer a moderator.");

            tableModel.removeRow(rowNum);
          });
    };
  }

  BiConsumer<Integer, DefaultTableModel> addSuperModAction(final JFrame parentFrame) {
    return (rowNum, tableModel) -> {
      final String user = extractUserName(rowNum, tableModel);

      SwingComponents.promptUser(
          "Add Super-Moderator Status?",
          "Are you sure you want to add super-moderator to: " + user + "?",
          () -> {
            moderatorsTabModel.addSuperMod(user);
            MessagePopup.showMessage(parentFrame, user + " is now a super-moderator.");
          });
    };
  }

  void addModerator(final JTextField addField, final Component button, final JTable table) {
    moderatorsTabModel.addModerator(addField.getText().trim());
    addField.setText("");
    button.setEnabled(false);
  }

  void refreshTableData(final JTable table) {
    JTableBuilder.setData(table, moderatorsTabModel.fetchTableData());
  }
}
