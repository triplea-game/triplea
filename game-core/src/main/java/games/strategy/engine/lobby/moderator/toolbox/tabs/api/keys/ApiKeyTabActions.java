package games.strategy.engine.lobby.moderator.toolbox.tabs.api.keys;

import games.strategy.engine.lobby.moderator.toolbox.MessagePopup;
import games.strategy.engine.lobby.moderator.toolbox.tabs.ShowApiKeyDialog;
import java.util.function.BiConsumer;
import javax.swing.JFrame;
import javax.swing.table.DefaultTableModel;
import lombok.AllArgsConstructor;
import org.triplea.swing.SwingComponents;

@AllArgsConstructor
class ApiKeyTabActions {
  private static final String CREATE_NEW_KEY_MESSAGE =
      "<html>New key created below. Copy this key exactly, it will only be shown once.<br />"
          + "This key can be used on new machines to register that machine. You will assign a password<br/>"
          + "for the key at that time.<br/>"
          + "This key is only valid for a few days and can only be used once.";
  private final ApiKeyTabModel apiKeyTabModel;

  Runnable createSingleUseKeyAction(final JFrame parent) {
    return () -> {
      final String newKey = apiKeyTabModel.createSingleUseKey();
      ShowApiKeyDialog.showKey(parent, CREATE_NEW_KEY_MESSAGE, newKey);
    };
  }

  BiConsumer<Integer, DefaultTableModel> deleteKeyAction(final JFrame parentFrame) {
    return (rowNumber, tableModel) -> {
      final String keyId = String.valueOf(tableModel.getValueAt(rowNumber, 0));
      SwingComponents.promptUser(
          "Delete API Key?",
          "Are you sure you want to delete key " + keyId + "?",
          () -> {
            apiKeyTabModel.deleteKey(keyId);
            tableModel.removeRow(rowNumber);
            MessagePopup.showMessage(parentFrame, "Delete key " + keyId);
          });
    };
  }
}
